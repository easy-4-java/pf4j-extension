package org.pf4j.update.extension.transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDependency;
import org.pf4j.PluginManager;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginWrapper;
import org.pf4j.core.extension.PluginLifecycleManager;
import org.pf4j.core.extension.health.PluginHealth;
import org.pf4j.core.extension.health.PluginHealthService;
import org.pf4j.update.UpdateManager;
import org.pf4j.update.UpdateRepository;

/**
 * 支持制品备份、流量摘除、健康检查和自动回滚的 PF4J 更新管理器。
 *
 * <p>更新操作在单个管理器内串行执行。更新目标存在依赖方时，会在 PF4J 递归卸载后按依赖顺序
 * 重新加载这些插件；新版本失败时恢复旧制品并重新加载整个受影响集合。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public class TransactionalPluginUpdateManager extends UpdateManager {

    /** PF4J 插件管理器。 */
    private final PluginManager pluginManager;

    /** 串行化插件生命周期操作的管理器。 */
    private final PluginLifecycleManager lifecycleManager;

    /** 插件健康和流量摘除服务。 */
    private final PluginHealthService healthService;

    /** 插件制品备份和激活存储。 */
    private final PluginArtifactStore artifactStore;

    /** 串行化完整更新事务的互斥锁。 */
    private final ReentrantLock updateLock = new ReentrantLock();

    /** 更新完成监听器集合。 */
    private final List<PluginUpdateListener> listeners = new CopyOnWriteArrayList<PluginUpdateListener>();

    /** 更新前等待插件完成流量摘除的最大秒数。 */
    private final long drainTimeoutSeconds;

    /**
     * 创建事务式插件更新管理器。
     *
     * @param pluginManager PF4J 插件管理器
     * @param repositories 更新仓库集合
     * @param lifecycleManager 生命周期管理器
     * @param artifactStore 插件制品存储
     * @param drainTimeoutSeconds 流量摘除最大等待秒数
     */
    public TransactionalPluginUpdateManager(PluginManager pluginManager, List<UpdateRepository> repositories,
                                            PluginLifecycleManager lifecycleManager,
                                            PluginArtifactStore artifactStore, long drainTimeoutSeconds) {
        super(pluginManager, repositories);
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        this.lifecycleManager = Objects.requireNonNull(lifecycleManager, "lifecycleManager must not be null");
        this.healthService = new PluginHealthService(pluginManager);
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        if (drainTimeoutSeconds < 0) {
            throw new IllegalArgumentException("drainTimeoutSeconds must not be negative");
        }
        this.drainTimeoutSeconds = drainTimeoutSeconds;
    }

    /**
     * 注册更新完成监听器。
     *
     * @param listener 更新监听器
     */
    public void addListener(PluginUpdateListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    /**
     * 移除更新完成监听器。
     *
     * @param listener 更新监听器
     */
    public void removeListener(PluginUpdateListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通过事务流程安装插件，兼容 {@link UpdateManager} 原有调用入口。
     *
     * @param id 插件 ID
     * @param version 目标版本；为空时选择最新兼容版本
     * @return 安装和启动成功时返回 {@code true}
     */
    @Override
    public synchronized boolean installPlugin(String id, String version) {
        return installTransactional(id, version).isSuccess();
    }

    /**
     * 通过事务流程更新插件，兼容 {@link UpdateManager} 原有调用入口。
     *
     * @param id 插件 ID
     * @param version 目标版本；为空时选择最新兼容版本
     * @return 更新和健康校验成功时返回 {@code true}
     */
    @Override
    public boolean updatePlugin(String id, String version) {
        return updateTransactional(id, version).isSuccess();
    }

    /**
     * 事务式安装插件。
     *
     * @param pluginId 插件 ID
     * @param version 目标版本；为空时选择最新兼容版本
     * @return 安装结果
     */
    public PluginUpdateResult installTransactional(String pluginId, String version) {
        updateLock.lock();
        Path activeArtifact = null;
        try {
            Path downloaded = downloadPlugin(pluginId, version);
            activeArtifact = artifactStore.activate(downloaded, downloaded.getFileName().toString());
            String loadedPluginId = lifecycleManager.loadAndStart(activeArtifact);
            if (!pluginId.equals(loadedPluginId)) {
                throw new PluginRuntimeException("Expected plugin id '{}' but loaded '{}'", pluginId,
                        loadedPluginId);
            }
            assertHealthy(pluginId);
            PluginWrapper installed = pluginManager.getPlugin(pluginId);
            PluginUpdateResult result = new PluginUpdateResult(PluginUpdateResult.Operation.INSTALL, pluginId, null,
                    installed.getDescriptor().getVersion(), activeArtifact, true, false, null);
            notifyListeners(result);
            return result;
        } catch (RuntimeException | IOException ex) {
            stopAndUnloadQuietly(pluginId);
            deleteArtifactQuietly(activeArtifact);
            PluginUpdateResult result = new PluginUpdateResult(PluginUpdateResult.Operation.INSTALL, pluginId, null,
                    version, activeArtifact, false, false, ex);
            notifyListeners(result);
            return result;
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * 事务式更新已安装插件。
     *
     * @param pluginId 插件 ID
     * @param version 目标版本；为空时选择最新兼容版本
     * @return 更新及回滚结果
     */
    public PluginUpdateResult updateTransactional(String pluginId, String version) {
        updateLock.lock();
        try {
            PluginWrapper current = pluginManager.getPlugin(pluginId);
            if (Objects.isNull(current)) {
                throw new PluginRuntimeException("Plugin {} is not installed", pluginId);
            }
            String previousVersion = current.getDescriptor().getVersion();
            Path currentArtifact = current.getPluginPath();
            Path backupArtifact = null;
            Path activeArtifact = currentArtifact;
            List<PluginSnapshot> affectedPlugins = snapshotAffectedPlugins(pluginId);
            try {
                Path downloaded = downloadPlugin(pluginId, version);
                backupArtifact = artifactStore.backup(currentArtifact, pluginId, previousVersion);
                if (!healthService.drain(pluginId, drainTimeoutSeconds, TimeUnit.SECONDS)) {
                    throw new PluginRuntimeException("Plugin {} did not drain before timeout", pluginId);
                }
                lifecycleManager.stopAndUnload(pluginId);
                activeArtifact = artifactStore.activate(downloaded, currentArtifact.getFileName().toString());
                restoreRuntime(affectedPlugins, activeArtifact, pluginId);
                assertHealthy(pluginId);
                artifactStore.deleteBackup(backupArtifact);
                PluginWrapper updated = pluginManager.getPlugin(pluginId);
                PluginUpdateResult result = new PluginUpdateResult(PluginUpdateResult.Operation.UPDATE, pluginId,
                        previousVersion, updated.getDescriptor().getVersion(), activeArtifact, true, false, null);
                notifyListeners(result);
                return result;
            } catch (RuntimeException | IOException ex) {
                boolean rolledBack = rollback(pluginId, currentArtifact, backupArtifact, affectedPlugins, ex);
                PluginUpdateResult result = new PluginUpdateResult(PluginUpdateResult.Operation.UPDATE, pluginId,
                        previousVersion, rolledBack ? previousVersion : version, currentArtifact, false, rolledBack,
                        ex);
                notifyListeners(result);
                return result;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                boolean rolledBack = rollback(pluginId, currentArtifact, backupArtifact, affectedPlugins, ex);
                PluginUpdateResult result = new PluginUpdateResult(PluginUpdateResult.Operation.UPDATE, pluginId,
                        previousVersion, rolledBack ? previousVersion : version, currentArtifact, false, rolledBack,
                        ex);
                notifyListeners(result);
                return result;
            }
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * 校验插件启动后的健康和就绪状态。
     *
     * @param pluginId 插件 ID
     */
    private void assertHealthy(String pluginId) {
        PluginHealth health = healthService.checkHealth(pluginId);
        PluginHealth readiness = healthService.checkReadiness(pluginId);
        if (!health.isHealthy() || !readiness.isHealthy()) {
            throw new PluginRuntimeException("Plugin {} failed health or readiness check: {}, {}", pluginId,
                    health.getMessage(), readiness.getMessage());
        }
    }

    /**
     * 快照目标插件及其传递依赖方。
     *
     * @param pluginId 目标插件 ID
     * @return 依赖优先排列的插件快照
     */
    private List<PluginSnapshot> snapshotAffectedPlugins(String pluginId) {
        List<PluginSnapshot> snapshots = new ArrayList<PluginSnapshot>();
        Set<String> included = new LinkedHashSet<String>();
        included.add(pluginId);
        boolean changed;
        do {
            changed = false;
            for (PluginWrapper candidate : pluginManager.getPlugins()) {
                if (included.contains(candidate.getPluginId())) {
                    continue;
                }
                for (PluginDependency dependency : candidate.getDescriptor().getDependencies()) {
                    if (included.contains(dependency.getPluginId())) {
                        included.add(candidate.getPluginId());
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);
        for (String includedId : included) {
            PluginWrapper plugin = pluginManager.getPlugin(includedId);
            snapshots.add(new PluginSnapshot(plugin.getPluginId(), plugin.getPluginPath()));
        }
        return Collections.unmodifiableList(snapshots);
    }

    /**
     * 加载更新后的目标插件和受影响依赖方。
     *
     * @param snapshots 更新前插件快照
     * @param targetArtifact 目标插件当前制品
     * @param targetPluginId 目标插件 ID
     */
    private void restoreRuntime(List<PluginSnapshot> snapshots, Path targetArtifact, String targetPluginId) {
        for (PluginSnapshot snapshot : snapshots) {
            Path path = targetPluginId.equals(snapshot.pluginId) ? targetArtifact : snapshot.pluginPath;
            String loadedId = lifecycleManager.loadAndStart(path);
            if (!snapshot.pluginId.equals(loadedId)) {
                throw new PluginRuntimeException("Expected plugin id '{}' but loaded '{}'", snapshot.pluginId,
                        loadedId);
            }
        }
    }

    /**
     * 恢复旧制品及更新前运行状态。
     *
     * @param pluginId 目标插件 ID
     * @param currentArtifact 原插件路径
     * @param backupArtifact 备份路径
     * @param affectedPlugins 受影响插件快照
     * @param originalFailure 原始更新异常
     * @return 完成旧版本恢复时返回 {@code true}
     */
    private boolean rollback(String pluginId, Path currentArtifact, Path backupArtifact,
                             List<PluginSnapshot> affectedPlugins, Throwable originalFailure) {
        if (Objects.isNull(backupArtifact)) {
            return false;
        }
        try {
            stopAndUnloadQuietly(pluginId);
            artifactStore.restore(backupArtifact, currentArtifact);
            restoreRuntime(affectedPlugins, currentArtifact, pluginId);
            artifactStore.deleteBackup(backupArtifact);
            return true;
        } catch (RuntimeException | IOException rollbackFailure) {
            originalFailure.addSuppressed(rollbackFailure);
            log.error("Failed to roll back PF4J plugin '{}'", pluginId, rollbackFailure);
            return false;
        }
    }

    /**
     * 尽力停止并卸载插件。
     *
     * @param pluginId 插件 ID
     */
    private void stopAndUnloadQuietly(String pluginId) {
        if (Objects.nonNull(pluginManager.getPlugin(pluginId))) {
            try {
                lifecycleManager.stopAndUnload(pluginId);
            } catch (RuntimeException ex) {
                log.warn("Failed to stop and unload PF4J plugin '{}'", pluginId, ex);
            }
        }
    }

    /**
     * 尽力删除失败安装制品。
     *
     * @param artifact 插件制品路径
     */
    private void deleteArtifactQuietly(Path artifact) {
        try {
            artifactStore.deleteArtifact(artifact);
        } catch (IOException ex) {
            log.warn("Failed to delete PF4J artifact '{}'", artifact, ex);
        }
    }

    /**
     * 通知更新监听器并隔离监听器异常。
     *
     * @param result 更新结果
     */
    private void notifyListeners(PluginUpdateResult result) {
        for (PluginUpdateListener listener : listeners) {
            try {
                listener.onCompleted(result);
            } catch (RuntimeException ex) {
                log.warn("PF4J update listener failed for plugin '{}'", result.getPluginId(), ex);
            }
        }
    }

    /**
     * 更新前插件运行快照。
     */
    private static final class PluginSnapshot {

        /** 插件 ID。 */
        private final String pluginId;

        /** 插件制品路径。 */
        private final Path pluginPath;

        /**
         * 创建插件运行快照。
         *
         * @param pluginId 插件 ID
         * @param pluginPath 插件路径
         */
        private PluginSnapshot(String pluginId, Path pluginPath) {
            this.pluginId = pluginId;
            this.pluginPath = pluginPath;
        }
    }
}
