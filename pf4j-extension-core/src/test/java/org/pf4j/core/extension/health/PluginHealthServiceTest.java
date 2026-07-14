package org.pf4j.core.extension.health;

import java.lang.reflect.Proxy;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PluginHealthService} 健康、就绪与摘流聚合测试。
 */
class PluginHealthServiceTest {

    /**
     * 验证最严重健康结果优先，并按顺序执行摘流钩子。
     *
     * @throws InterruptedException 摘流等待被中断时抛出
     */
    @Test
    void shouldAggregateHealthAndDrainPlugin() throws InterruptedException {
        AtomicBoolean draining = new AtomicBoolean(false);
        PluginHealthCheck healthy = () -> PluginHealth.up("ok");
        PluginHealthCheck degraded = () -> new PluginHealth(PluginHealth.Status.DEGRADED, "slow", null);
        PluginReadinessCheck notReady = () -> PluginHealth.down("warming");
        PluginDrainHook drainHook = new PluginDrainHook() {
            @Override
            public void beginDrain() {
                draining.set(true);
            }

            @Override
            public boolean awaitDrained(long timeout, TimeUnit unit) {
                return draining.get() && timeout > 0;
            }
        };
        PluginHealthService service = new PluginHealthService(pluginManager(
                Arrays.asList(healthy, degraded), Collections.singletonList(notReady),
                Collections.singletonList(drainHook)));

        assertEquals(PluginHealth.Status.DEGRADED, service.checkHealth("demo-plugin").getStatus());
        assertEquals(PluginHealth.Status.DOWN, service.checkReadiness("demo-plugin").getStatus());
        assertTrue(service.drain("demo-plugin", 1, TimeUnit.SECONDS));
        assertTrue(draining.get());
    }

    /**
     * 验证未启动插件不会被判定为健康。
     */
    @Test
    void shouldReportStoppedPluginAsDown() {
        PluginManager manager = pluginManager(Collections.<PluginHealthCheck>emptyList(),
                Collections.<PluginReadinessCheck>emptyList(), Collections.<PluginDrainHook>emptyList());
        manager.getPlugin("demo-plugin").setPluginState(PluginState.STOPPED);

        assertFalse(new PluginHealthService(manager).checkHealth("demo-plugin").isHealthy());
    }

    /**
     * 创建能够返回治理扩展的 PF4J 管理器代理。
     *
     * @param healthChecks 健康检查集合
     * @param readinessChecks 就绪检查集合
     * @param drainHooks 摘流钩子集合
     * @return PF4J 管理器代理
     */
    private static PluginManager pluginManager(List<PluginHealthCheck> healthChecks,
                                               List<PluginReadinessCheck> readinessChecks,
                                               List<PluginDrainHook> drainHooks) {
        PluginManager[] manager = new PluginManager[1];
        PluginWrapper[] wrapper = new PluginWrapper[1];
        manager[0] = (PluginManager) Proxy.newProxyInstance(PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class}, (proxy, method, args) -> {
                    if ("getRuntimeMode".equals(method.getName())) {
                        return RuntimeMode.DEPLOYMENT;
                    }
                    if ("getPlugin".equals(method.getName())) {
                        return wrapper[0];
                    }
                    if ("getExtensions".equals(method.getName())) {
                        if (PluginHealthCheck.class.equals(args[0])) {
                            return healthChecks;
                        }
                        if (PluginReadinessCheck.class.equals(args[0])) {
                            return readinessChecks;
                        }
                        if (PluginDrainHook.class.equals(args[0])) {
                            return drainHooks;
                        }
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor("demo-plugin", "demo",
                "org.pf4j.Plugin", "1.0.0", "*", "test", "Apache-2.0");
        wrapper[0] = new PluginWrapper(manager[0], descriptor, Paths.get("demo-plugin.jar"),
                PluginHealthServiceTest.class.getClassLoader());
        wrapper[0].setPluginState(PluginState.STARTED);
        return manager[0];
    }
}
