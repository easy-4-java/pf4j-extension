package org.pf4j.core.extension;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pf4j.DefaultPluginDescriptor;
import org.junit.jupiter.api.Test;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;
import org.pf4j.core.extension.exception.PluginLifecycleException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link PluginLifecycleManager} 批量启动失败回滚行为测试。
 */
class PluginLifecycleManagerTest {

    /**
     * 验证第二个插件启动失败时，已启动插件会停止，全部已加载插件会按逆序卸载。
     */
    @Test
    void shouldRollbackStartedPluginsWhenBatchStartFails() {
        List<String> stopped = new ArrayList<String>();
        List<String> unloaded = new ArrayList<String>();
        PluginLifecycleManager lifecycleManager = new PluginLifecycleManager(pluginManager(stopped, unloaded));

        assertThrows(PluginLifecycleException.class, () -> lifecycleManager.loadAndStartAll(Arrays.asList(
                Paths.get("plugin-one.zip"), Paths.get("plugin-two.zip"))));

        assertEquals(Arrays.asList("plugin-one.zip"), stopped);
        assertEquals(Arrays.asList("plugin-two.zip", "plugin-one.zip"), unloaded);
    }

    /**
     * 创建可记录生命周期调用顺序的 PF4J 管理器代理。
     *
     * @param stopped 接收停止插件 ID 的记录列表
     * @param unloaded 接收卸载插件 ID 的记录列表
     * @return 模拟第二个插件启动失败的 {@link PluginManager} 动态代理
     */
    private static PluginManager pluginManager(List<String> stopped, List<String> unloaded) {
        Map<String, PluginWrapper> plugins = new HashMap<String, PluginWrapper>();
        PluginManager[] manager = new PluginManager[1];
        manager[0] = (PluginManager) Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("getRuntimeMode".equals(methodName)) {
                        return RuntimeMode.DEPLOYMENT;
                    }
                    if ("loadPlugin".equals(methodName)) {
                        String pluginId = ((Path) args[0]).getFileName().toString();
                        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor(pluginId, pluginId,
                                "org.pf4j.Plugin", "1.0.0", "*", "test", "Apache-2.0");
                        PluginWrapper wrapper = new PluginWrapper(manager[0], descriptor, (Path) args[0],
                                PluginLifecycleManagerTest.class.getClassLoader());
                        wrapper.setPluginState(PluginState.RESOLVED);
                        plugins.put(pluginId, wrapper);
                        return pluginId;
                    }
                    if ("getPlugin".equals(methodName)) {
                        return plugins.get(args[0]);
                    }
                    if ("startPlugin".equals(methodName)) {
                        PluginState state = "plugin-two.zip".equals(args[0])
                                ? PluginState.FAILED : PluginState.STARTED;
                        plugins.get(args[0]).setPluginState(state);
                        return state;
                    }
                    if ("stopPlugin".equals(methodName)) {
                        stopped.add((String) args[0]);
                        plugins.get(args[0]).setPluginState(PluginState.STOPPED);
                        return PluginState.STOPPED;
                    }
                    if ("unloadPlugin".equals(methodName)) {
                        unloaded.add((String) args[0]);
                        plugins.remove(args[0]);
                        return true;
                    }
                    throw new UnsupportedOperationException(methodName);
                });
        return manager[0];
    }

}
