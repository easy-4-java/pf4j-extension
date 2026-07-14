package org.pf4j.core.extension.catalog;

import java.lang.reflect.Proxy;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;
import org.pf4j.core.extension.annotation.ExtensionMapping;
import org.pf4j.core.extension.annotation.Primary;
import org.pf4j.core.extension.exception.ExtensionConflictException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ExtensionCatalog} 元数据快照与冲突校验测试。
 */
class ExtensionCatalogTest {

    /**
     * 验证目录提取稳定扩展 ID、版本与主要实现标记。
     */
    @Test
    void shouldBuildExtensionMetadataSnapshot() {
        ExtensionCatalog catalog = new ExtensionCatalog(pluginManager(
                Collections.<Class<?>>singletonList(PrimaryGreeting.class)));

        List<ExtensionMetadata> extensions = catalog.getExtensions(GreetingPoint.class);

        assertEquals(1, extensions.size());
        assertEquals("greeting", extensions.get(0).getExtensionId());
        assertEquals("2.0.0", extensions.get(0).getVersion());
        assertTrue(extensions.get(0).isPrimary());
        assertEquals("1.0.0", catalog.getPlugins().get("demo-plugin").getVersion());
        catalog.close();
    }

    /**
     * 验证同一插件和扩展点不允许声明重复扩展 ID。
     */
    @Test
    void shouldRejectDuplicateExtensionId() {
        ExtensionCatalog catalog = new ExtensionCatalog(pluginManager(
                Arrays.<Class<?>>asList(PrimaryGreeting.class, DuplicateGreeting.class)));

        assertThrows(ExtensionConflictException.class, catalog::refresh);
        catalog.close();
    }

    /**
     * 创建包含指定扩展类型的 PF4J 管理器代理。
     *
     * @param extensionClasses 插件声明的扩展类型
     * @return PF4J 管理器代理
     */
    private static PluginManager pluginManager(List<Class<?>> extensionClasses) {
        PluginManager[] manager = new PluginManager[1];
        PluginWrapper[] wrapper = new PluginWrapper[1];
        manager[0] = (PluginManager) Proxy.newProxyInstance(PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class}, (proxy, method, args) -> {
                    if ("getRuntimeMode".equals(method.getName())) {
                        return RuntimeMode.DEPLOYMENT;
                    }
                    if ("getStartedPlugins".equals(method.getName())) {
                        return Collections.singletonList(wrapper[0]);
                    }
                    if ("getExtensionClasses".equals(method.getName())) {
                        return extensionClasses;
                    }
                    if ("addPluginStateListener".equals(method.getName())
                            || "removePluginStateListener".equals(method.getName())) {
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor("demo-plugin", "demo",
                ExtensionCatalogTest.class.getName(), "1.0.0", "*", "test", "Apache-2.0");
        wrapper[0] = new PluginWrapper(manager[0], descriptor, Paths.get("demo-plugin.jar"),
                ExtensionCatalogTest.class.getClassLoader());
        wrapper[0].setPluginState(PluginState.STARTED);
        return manager[0];
    }

    /**
     * 测试用问候扩展点。
     */
    private interface GreetingPoint extends ExtensionPoint {
    }

    /**
     * 主要问候扩展实现。
     */
    @Extension(ordinal = 10)
    @Primary
    @ExtensionMapping(id = "greeting", title = "Greeting", ver = "2.0.0", desc = "primary")
    private static final class PrimaryGreeting implements GreetingPoint {
    }

    /**
     * 声明重复 ID 的问候扩展实现。
     */
    @Extension
    @ExtensionMapping(id = "greeting")
    private static final class DuplicateGreeting implements GreetingPoint {
    }
}
