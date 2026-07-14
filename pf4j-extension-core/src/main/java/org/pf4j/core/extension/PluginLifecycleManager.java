package org.pf4j.core.extension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.core.extension.exception.PluginLifecycleException;
import org.pf4j.core.extension.lifecycle.PluginLifecycleListener;
import org.pf4j.core.extension.lifecycle.PluginOperation;
import org.pf4j.core.extension.lifecycle.PluginOperationResult;

/**
 * 提供串行化、可回滚的 PF4J 插件生命周期操作。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public final class PluginLifecycleManager {

    /**
     * 执行底层插件加载、启动、停止和卸载操作的 PF4J 管理器。
     */
    private final PluginManager pluginManager;

    /**
     * 生命周期互斥锁，确保加载、启动、停止、卸载和回滚操作不会并发交错。
     */
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    /**
     * 生命周期操作监听器集合，使用写时复制保证通知期间可以安全增删监听器。
     */
    private final List<PluginLifecycleListener> listeners = new CopyOnWriteArrayList<PluginLifecycleListener>();

    /**
     * 创建插件生命周期管理器。
     *
     * @param pluginManager PF4J 插件管理器，不允许为 {@code null}
     * @throws NullPointerException 当 {@code pluginManager} 为 {@code null} 时抛出
     */
    public PluginLifecycleManager(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
    }

    /**
     * 注册生命周期操作监听器。
     *
     * @param listener 待注册的监听器
     * @throws NullPointerException 当 {@code listener} 为 {@code null} 时抛出
     */
    public void addListener(PluginLifecycleListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    /**
     * 移除生命周期操作监听器。
     *
     * @param listener 待移除的监听器
     */
    public void removeListener(PluginLifecycleListener listener) {
        listeners.remove(listener);
    }

    /**
     * 扫描插件目录并严格启动全部已解析插件。
     *
     * <p>方法不调用 PF4J 的批量启动方法，而是按解析顺序逐个调用
     * {@link PluginManager#startPlugin(String)}，从而保留必选依赖启动失败校验。当前批次新启动的
     * 插件发生失败时会按逆序停止并卸载。</p>
     *
     * @return 本次成功启动的插件 ID 不可修改列表
     * @throws PluginLifecycleException 当任一插件启动失败时抛出
     */
    public List<String> loadAllAndStartStrictly() {
        lifecycleLock.lock();
        try {
            pluginManager.loadPlugins();
            return startAllStrictlyInternal();
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 严格启动全部已解析但尚未启动的插件。
     *
     * @return 本次成功启动的插件 ID 不可修改列表
     * @throws PluginLifecycleException 当任一插件启动失败时抛出
     */
    public List<String> startAllStrictly() {
        lifecycleLock.lock();
        try {
            return startAllStrictlyInternal();
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 安全停止全部已启动插件。
     *
     * <p>先复制 PF4J 内部已启动列表，再按逆序逐个停止，避免 PF4J 3.15.0 批量停止时修改
     * 正在迭代的内部列表。</p>
     *
     * @return 按实际停止顺序排列的插件 ID 不可修改列表
     * @throws PluginLifecycleException 当任一插件停止失败时抛出
     */
    public List<String> stopAllSafely() {
        lifecycleLock.lock();
        try {
            return stopAllSafelyInternal();
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 安全停止并卸载当前管理器中的全部插件。
     *
     * @throws PluginLifecycleException 当插件停止或卸载失败时抛出
     */
    public void unloadAllSafely() {
        lifecycleLock.lock();
        try {
            stopAllSafelyInternal();
            List<PluginWrapper> plugins = new ArrayList<PluginWrapper>(pluginManager.getPlugins());
            Collections.reverse(plugins);
            for (PluginWrapper plugin : plugins) {
                if (Objects.nonNull(pluginManager.getPlugin(plugin.getPluginId()))) {
                    unloadInternal(plugin.getPluginId());
                }
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 加载并启动单个插件。
     *
     * @param pluginPath 插件路径
     * @return PF4J 加载后分配的插件 ID
     * @throws NullPointerException 当 {@code pluginPath} 为 {@code null} 时抛出
     * @throws PluginLifecycleException 当 PF4J 未返回插件 ID 或插件未进入已启动状态时抛出
     */
    public String loadAndStart(Path pluginPath) {
        Objects.requireNonNull(pluginPath, "pluginPath must not be null");
        lifecycleLock.lock();
        try {
            return loadAndStartInternal(pluginPath);
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 原子地加载并启动一批插件，任一插件失败时按逆序回滚本批次已启动插件。
     *
     * @param pluginPaths 插件路径集合
     * @return 按输入顺序排列的不可修改插件 ID 列表；输入为空时返回空列表
     * @throws NullPointerException 当集合中包含 {@code null} 路径时抛出
     * @throws PluginLifecycleException 当任一插件加载或启动失败时抛出
     */
    public List<String> loadAndStartAll(List<Path> pluginPaths) {
        if (Objects.isNull(pluginPaths) || pluginPaths.isEmpty()) {
            return Collections.emptyList();
        }
        lifecycleLock.lock();
        List<String> startedPluginIds = new ArrayList<String>();
        try {
            for (Path pluginPath : pluginPaths) {
                startedPluginIds.add(loadAndStartInternal(
                        Objects.requireNonNull(pluginPath, "pluginPath must not contain null")));
            }
            return Collections.unmodifiableList(new ArrayList<>(startedPluginIds));
        } catch (RuntimeException ex) {
            rollback(startedPluginIds);
            throw ex;
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 停止并卸载插件。
     *
     * @param pluginId 插件 ID
     * @throws IllegalArgumentException 当 {@code pluginId} 为空时抛出
     * @throws PluginLifecycleException 当插件无法卸载时抛出
     */
    public void stopAndUnload(String pluginId) {
        if (StringUtils.isBlank(pluginId)) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        lifecycleLock.lock();
        try {
            stopAndUnloadInternal(pluginId);
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 在调用方已经持有生命周期锁的前提下加载并启动单个插件。
     *
     * <p>启动失败时会立即尝试停止并卸载当前插件，随后继续抛出原始异常。</p>
     *
     * @param pluginPath 插件文件或目录路径
     * @return 加载成功的插件 ID
     * @throws PluginLifecycleException 当插件 ID 为空或插件未进入 {@link PluginState#STARTED} 状态时抛出
     */
    private String loadAndStartInternal(Path pluginPath) {
        long startedAt = System.nanoTime();
        String pluginId = null;
        PluginState previousState = null;
        try {
            pluginId = pluginManager.loadPlugin(pluginPath);
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.LOAD, null,
                    stateOf(pluginId), true, System.nanoTime() - startedAt, null));
        } catch (RuntimeException ex) {
            notifyListeners(new PluginOperationResult(null, PluginOperation.LOAD, null, null,
                    false, System.nanoTime() - startedAt, ex));
            throw ex;
        }
        if (Objects.isNull(pluginId)) {
            throw new PluginLifecycleException("PF4J did not return a plugin id for path '" + pluginPath + "'");
        }
        try {
            previousState = stateOf(pluginId);
            startedAt = System.nanoTime();
            PluginState state = pluginManager.startPlugin(pluginId);
            if (state != PluginState.STARTED) {
                Throwable failure = failureOf(pluginId);
                throw new PluginLifecycleException(
                        "Plugin '" + pluginId + "' failed to start, current state is " + state, failure);
            }
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.START, previousState, state,
                    true, System.nanoTime() - startedAt, null));
            return pluginId;
        } catch (RuntimeException ex) {
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.START, previousState,
                    stateOf(pluginId), false, System.nanoTime() - startedAt, ex));
            stopAndUnloadQuietly(pluginId);
            throw ex;
        }
    }

    /**
     * 在已持有生命周期锁时严格启动全部已解析插件。
     *
     * @return 本批次成功启动的插件 ID 列表
     */
    private List<String> startAllStrictlyInternal() {
        List<String> startedPluginIds = new ArrayList<String>();
        try {
            for (PluginWrapper plugin : new ArrayList<PluginWrapper>(pluginManager.getResolvedPlugins())) {
                PluginState state = plugin.getPluginState();
                if (state.isDisabled() || state.isStarted()) {
                    continue;
                }
                startInternal(plugin.getPluginId());
                startedPluginIds.add(plugin.getPluginId());
            }
            return Collections.unmodifiableList(new ArrayList<String>(startedPluginIds));
        } catch (RuntimeException ex) {
            rollback(startedPluginIds);
            throw ex;
        }
    }

    /**
     * 启动单个已加载插件并校验最终状态。
     *
     * @param pluginId 插件 ID
     */
    private void startInternal(String pluginId) {
        PluginState previousState = stateOf(pluginId);
        long startedAt = System.nanoTime();
        try {
            PluginState state = pluginManager.startPlugin(pluginId);
            if (!state.isStarted()) {
                throw new PluginLifecycleException("Plugin '" + pluginId + "' failed to start, current state is "
                        + state, failureOf(pluginId));
            }
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.START, previousState, state,
                    true, System.nanoTime() - startedAt, null));
        } catch (RuntimeException ex) {
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.START, previousState,
                    stateOf(pluginId), false, System.nanoTime() - startedAt, ex));
            throw ex;
        }
    }

    /**
     * 在已持有生命周期锁时安全停止全部插件。
     *
     * @return 实际停止的插件 ID 列表
     */
    private List<String> stopAllSafelyInternal() {
        List<PluginWrapper> startedPlugins = new ArrayList<PluginWrapper>(pluginManager.getStartedPlugins());
        Collections.reverse(startedPlugins);
        List<String> stoppedPluginIds = new ArrayList<String>();
        for (PluginWrapper plugin : startedPlugins) {
            if (Objects.nonNull(pluginManager.getPlugin(plugin.getPluginId()))
                    && pluginManager.getPlugin(plugin.getPluginId()).getPluginState().isStarted()) {
                stopInternal(plugin.getPluginId());
                stoppedPluginIds.add(plugin.getPluginId());
            }
        }
        return Collections.unmodifiableList(stoppedPluginIds);
    }

    /**
     * 按启动顺序的逆序回滚本批次插件。
     *
     * @param pluginIds 已成功启动、等待回滚的插件 ID 列表
     */
    private void rollback(List<String> pluginIds) {
        for (int index = pluginIds.size() - 1; index >= 0; index--) {
            stopAndUnloadQuietly(pluginIds.get(index));
        }
    }

    /**
     * 在调用方已经持有生命周期锁的前提下停止并卸载插件。
     *
     * @param pluginId 待停止和卸载的插件 ID
     * @throws PluginLifecycleException 当 PF4J 返回卸载失败时抛出
     */
    private void stopAndUnloadInternal(String pluginId) {
        PluginWrapper plugin = pluginManager.getPlugin(pluginId);
        if (Objects.isNull(plugin)) {
            return;
        }
        if (plugin.getPluginState().isStarted()) {
            stopInternal(pluginId);
        }
        if (Objects.nonNull(pluginManager.getPlugin(pluginId))) {
            unloadInternal(pluginId);
        }
    }

    /**
     * 停止单个插件并校验最终状态。
     *
     * @param pluginId 插件 ID
     */
    private void stopInternal(String pluginId) {
        PluginState previousState = stateOf(pluginId);
        long startedAt = System.nanoTime();
        try {
            PluginState state = pluginManager.stopPlugin(pluginId);
            if (state.isFailed() || state.isStarted()) {
                throw new PluginLifecycleException("Plugin '" + pluginId + "' failed to stop, current state is "
                        + state, failureOf(pluginId));
            }
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.STOP, previousState, state,
                    true, System.nanoTime() - startedAt, null));
        } catch (RuntimeException ex) {
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.STOP, previousState,
                    stateOf(pluginId), false, System.nanoTime() - startedAt, ex));
            throw ex;
        }
    }

    /**
     * 卸载单个插件并记录操作结果。
     *
     * @param pluginId 插件 ID
     */
    private void unloadInternal(String pluginId) {
        PluginState previousState = stateOf(pluginId);
        long startedAt = System.nanoTime();
        try {
            if (!pluginManager.unloadPlugin(pluginId)) {
                throw new PluginLifecycleException("Plugin '" + pluginId + "' could not be unloaded");
            }
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.UNLOAD, previousState,
                    PluginState.UNLOADED, true, System.nanoTime() - startedAt, null));
        } catch (RuntimeException ex) {
            notifyListeners(new PluginOperationResult(pluginId, PluginOperation.UNLOAD, previousState,
                    stateOf(pluginId), false, System.nanoTime() - startedAt, ex));
            throw ex;
        }
    }

    /**
     * 获取插件当前状态。
     *
     * @param pluginId 插件 ID
     * @return 当前状态；插件不存在时返回 {@code null}
     */
    private PluginState stateOf(String pluginId) {
        PluginWrapper plugin = Objects.isNull(pluginId) ? null : pluginManager.getPlugin(pluginId);
        return Objects.isNull(plugin) ? null : plugin.getPluginState();
    }

    /**
     * 获取插件最近一次失败原因。
     *
     * @param pluginId 插件 ID
     * @return 失败原因；插件不存在或未失败时返回 {@code null}
     */
    private Throwable failureOf(String pluginId) {
        PluginWrapper plugin = pluginManager.getPlugin(pluginId);
        return Objects.isNull(plugin) ? null : plugin.getFailedException();
    }

    /**
     * 通知全部生命周期监听器，并隔离监听器自身异常。
     *
     * @param result 生命周期操作结果
     */
    private void notifyListeners(PluginOperationResult result) {
        for (PluginLifecycleListener listener : listeners) {
            try {
                listener.onCompleted(result);
            } catch (RuntimeException listenerException) {
                log.warn("PF4J lifecycle listener failed for plugin '{}' and operation '{}'",
                        result.getPluginId(), result.getOperation(), listenerException);
            }
        }
    }

    /**
     * 尽力停止并卸载插件，将回滚失败记录为警告而不覆盖原始业务异常。
     *
     * @param pluginId 待回滚的插件 ID
     */
    private void stopAndUnloadQuietly(String pluginId) {
        try {
            stopAndUnloadInternal(pluginId);
        } catch (RuntimeException rollbackException) {
            log.warn("Failed to roll back PF4J plugin '{}'", pluginId, rollbackException);
        }
    }

}
