package org.pf4j.spring.extension.registry;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.pf4j.PluginRuntimeException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PluginBeanRegistry} 插件 Bean 所有权与动态清理测试。
 */
class PluginBeanRegistryTest {

    /**
     * 验证普通 Bean 和 Controller 都会按插件登记并在停止时精确移除。
     */
    @Test
    void shouldRegisterAndRemoveAllPluginBeans() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        RecordingControllerRegistry controllers = new RecordingControllerRegistry();
        PluginBeanRegistry registry = new PluginBeanRegistry(beanFactory, controllers);

        registry.register("demo-plugin", "demoService", new Object(), false);
        registry.register("demo-plugin", "demoController", new Object(), true);

        assertTrue(beanFactory.containsSingleton("demoService"));
        assertEquals(2, registry.getBeanNames("demo-plugin").size());
        assertEquals("demoController", controllers.registered.get(0));

        registry.removePlugin("demo-plugin");

        assertFalse(beanFactory.containsSingleton("demoService"));
        assertEquals("demoController", controllers.removed.get(0));
        assertTrue(registry.getBeanNames("demo-plugin").isEmpty());
    }

    /**
     * 验证不同插件不能复用同一 Spring Bean 名称。
     */
    @Test
    void shouldRejectBeanNameOwnedByAnotherPlugin() {
        PluginBeanRegistry registry = new PluginBeanRegistry(new DefaultListableBeanFactory(),
                new RecordingControllerRegistry());
        registry.register("plugin-one", "sharedBean", new Object(), false);

        assertThrows(PluginRuntimeException.class,
                () -> registry.register("plugin-two", "sharedBean", new Object(), false));
    }

    /**
     * 记录动态 Controller 注册和移除操作的测试实现。
     */
    private static final class RecordingControllerRegistry implements DynamicControllerRegistry {

        /** 已注册 Controller 名称。 */
        private final List<String> registered = new ArrayList<String>();

        /** 已移除 Controller 名称。 */
        private final List<String> removed = new ArrayList<String>();

        /**
         * 记录 Controller 注册。
         *
         * @param controllerBeanName Controller Bean 名称
         * @param controller Controller 实例
         */
        @Override
        public void registerController(String controllerBeanName, Object controller) {
            registered.add(controllerBeanName);
        }

        /**
         * 记录 Controller 移除。
         *
         * @param controllerBeanName Controller Bean 名称
         */
        @Override
        public void removeController(String controllerBeanName) {
            removed.add(controllerBeanName);
        }
    }
}
