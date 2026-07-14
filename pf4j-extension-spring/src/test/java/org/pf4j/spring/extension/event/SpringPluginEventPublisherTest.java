package org.pf4j.spring.extension.event;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginDependency;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SpringPluginEventPublisher} 完整插件状态快照映射测试。
 */
class SpringPluginEventPublisherTest {

    /** JUnit 为测试插件创建的临时制品目录。 */
    @TempDir
    Path temporaryDirectory;

    /**
     * 验证事件包含描述符、运行环境、扩展类和完整异常链快照。
     */
    @Test
    void shouldPublishCompletePluginStateSnapshot() {
        AtomicReference<SpringPluginStateChangedEvent> published =
                new AtomicReference<SpringPluginStateChangedEvent>();
        ApplicationEventPublisher applicationEventPublisher = new ApplicationEventPublisher() {
            @Override
            public void publishEvent(Object event) {
                published.set((SpringPluginStateChangedEvent) event);
            }
        };
        PluginManager manager = pluginManager();
        DefaultPluginDescriptor descriptor = descriptor();
        PluginWrapper wrapper = new PluginWrapper(manager, descriptor, temporaryDirectory.resolve("demo.jar"),
                SpringPluginEventPublisherTest.class.getClassLoader());
        wrapper.setPluginState(PluginState.FAILED);
        wrapper.setFailedException(new IllegalStateException("start failed",
                new IllegalArgumentException("bad configuration")));

        new SpringPluginEventPublisher(applicationEventPublisher)
                .pluginStateChanged(new PluginStateEvent(manager, wrapper, PluginState.STARTED));

        SpringPluginStateChangedEvent event = published.get();
        assertNotNull(event);
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals("demo-plugin", event.getPluginId());
        assertEquals("1.2.0", event.getPluginVersion());
        assertEquals("Demo plugin", event.getPluginDescriptor().getDescription());
        assertEquals("com.acme.DemoPlugin", event.getPluginDescriptor().getPluginClassName());
        assertEquals("host@>=2.0.0", event.getPluginDescriptor().getRequires());
        assertEquals("hiwepy", event.getPluginDescriptor().getProvider());
        assertEquals("Apache-2.0", event.getPluginDescriptor().getLicense());
        assertEquals(2, event.getPluginDescriptor().getDependencies().size());
        assertFalse(event.getPluginDescriptor().getDependencies().get(0).isOptional());
        assertTrue(event.getPluginDescriptor().getDependencies().get(1).isOptional());
        assertEquals("deployment", event.getRuntimeMode());
        assertEquals("2.0.0", event.getSystemVersion());
        assertNotNull(event.getPluginManagerClassName());
        assertEquals(SpringPluginEventPublisherTest.class.getClassLoader().getClass().getName(),
                event.getPluginClassLoaderClassName());
        assertEquals(Arrays.asList("com.acme.FirstExtension", "com.acme.SecondExtension"),
                event.getExtensionClassNames());
        assertEquals("STARTED -> FAILED", event.getStateTransition());
        assertTrue(event.isFailure());
        assertEquals(IllegalStateException.class.getName(), event.getFailureClassName());
        assertEquals(IllegalArgumentException.class.getName(), event.getRootFailureClassName());
        assertEquals("bad configuration", event.getRootFailureMessage());
        assertEquals(2, event.getFailureCauseChain().size());
        assertThrows(UnsupportedOperationException.class,
                () -> event.getExtensionClassNames().add("com.acme.UnsafeExtension"));
    }

    /**
     * 验证 Spring 事件消费者异常不会反向中断 PF4J 生命周期线程。
     */
    @Test
    void shouldIsolateSpringEventConsumerFailure() {
        ApplicationEventPublisher failingPublisher = new ApplicationEventPublisher() {
            @Override
            public void publishEvent(Object event) {
                throw new IllegalStateException("consumer failed");
            }
        };
        PluginManager manager = pluginManager();
        PluginWrapper wrapper = new PluginWrapper(manager, descriptor(), temporaryDirectory.resolve("demo.jar"),
                SpringPluginEventPublisherTest.class.getClassLoader());
        wrapper.setPluginState(PluginState.STARTED);

        assertDoesNotThrow(() -> new SpringPluginEventPublisher(failingPublisher)
                .pluginStateChanged(new PluginStateEvent(manager, wrapper, PluginState.RESOLVED)));
    }

    /**
     * 创建包含完整描述符字段和必选、可选依赖的测试插件描述符。
     *
     * @return 测试插件描述符
     */
    private DefaultPluginDescriptor descriptor() {
        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor("demo-plugin", "Demo plugin",
                "com.acme.DemoPlugin", "1.2.0", "host@>=2.0.0", "hiwepy", "Apache-2.0");
        descriptor.addDependency(new PluginDependency("required-plugin@>=1.0.0"));
        descriptor.addDependency(new PluginDependency("optional-plugin?@2.0.0"));
        return descriptor;
    }

    /**
     * 创建能够提供运行环境和扩展类名的 PF4J 管理器代理。
     *
     * @return PF4J 管理器代理
     */
    private PluginManager pluginManager() {
        return (PluginManager) Proxy.newProxyInstance(PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class}, (proxy, method, args) -> {
                    if ("getRuntimeMode".equals(method.getName())) {
                        return RuntimeMode.DEPLOYMENT;
                    }
                    if ("getSystemVersion".equals(method.getName())) {
                        return "2.0.0";
                    }
                    if ("getExtensionClassNames".equals(method.getName())) {
                        return new LinkedHashSet<String>(Arrays.asList(
                                "com.acme.SecondExtension", "com.acme.FirstExtension"));
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
