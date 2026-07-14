package org.pf4j.core.extension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.core.extension.exception.PluginLifecycleException;

/**
 * 提供串行化、可回滚的 PF4J 插件生命周期操作。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public final class PluginLifecycleManager {

    private final PluginManager pluginManager;
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    public PluginLifecycleManager(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
    }

    /**
     * 加载并启动单个插件。
     *
     * @param pluginPath 插件路径
     * @return 插件 ID
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
     * @return 不可变插件 ID 列表
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
            return Collections.unmodifiableList(new ArrayList<String>(startedPluginIds));
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

    private String loadAndStartInternal(Path pluginPath) {
        String pluginId = pluginManager.loadPlugin(pluginPath);
        if (Objects.isNull(pluginId)) {
            throw new PluginLifecycleException("PF4J did not return a plugin id for path '" + pluginPath + "'");
        }
        try {
            PluginState state = pluginManager.startPlugin(pluginId);
            if (state != PluginState.STARTED) {
                throw new PluginLifecycleException(
                        "Plugin '" + pluginId + "' failed to start, current state is " + state);
            }
            return pluginId;
        } catch (RuntimeException ex) {
            stopAndUnloadQuietly(pluginId);
            throw ex;
        }
    }

    private void rollback(List<String> pluginIds) {
        for (int index = pluginIds.size() - 1; index >= 0; index--) {
            stopAndUnloadQuietly(pluginIds.get(index));
        }
    }

    private void stopAndUnloadInternal(String pluginId) {
        pluginManager.stopPlugin(pluginId);
        if (!pluginManager.unloadPlugin(pluginId)) {
            throw new PluginLifecycleException("Plugin '" + pluginId + "' could not be unloaded");
        }
    }

    private void stopAndUnloadQuietly(String pluginId) {
        try {
            stopAndUnloadInternal(pluginId);
        } catch (RuntimeException rollbackException) {
            log.warn("Failed to roll back PF4J plugin '{}'", pluginId, rollbackException);
        }
    }

}
