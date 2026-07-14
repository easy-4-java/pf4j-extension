package org.pf4j.core.extension;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.core.extension.exception.PluginLifecycleException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginLifecycleManagerTest {

    @Test
    void shouldRollbackStartedPluginsWhenBatchStartFails() {
        List<String> stopped = new ArrayList<String>();
        List<String> unloaded = new ArrayList<String>();
        PluginLifecycleManager lifecycleManager = new PluginLifecycleManager(pluginManager(stopped, unloaded));

        assertThrows(PluginLifecycleException.class, () -> lifecycleManager.loadAndStartAll(Arrays.asList(
                Paths.get("plugin-one.zip"), Paths.get("plugin-two.zip"))));

        assertEquals(Arrays.asList("plugin-two.zip", "plugin-one.zip"), stopped);
        assertEquals(Arrays.asList("plugin-two.zip", "plugin-one.zip"), unloaded);
    }

    private static PluginManager pluginManager(List<String> stopped, List<String> unloaded) {
        return (PluginManager) Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("loadPlugin".equals(methodName)) {
                        return ((Path) args[0]).getFileName().toString();
                    }
                    if ("startPlugin".equals(methodName)) {
                        return "plugin-two.zip".equals(args[0]) ? PluginState.FAILED : PluginState.STARTED;
                    }
                    if ("stopPlugin".equals(methodName)) {
                        stopped.add((String) args[0]);
                        return PluginState.STOPPED;
                    }
                    if ("unloadPlugin".equals(methodName)) {
                        unloaded.add((String) args[0]);
                        return true;
                    }
                    throw new UnsupportedOperationException(methodName);
                });
    }

}
