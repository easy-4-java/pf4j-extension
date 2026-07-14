package org.pf4j.core.extension;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.pf4j.PluginManager;
import org.pf4j.core.extension.annotation.ExtensionMapping;
import org.pf4j.core.extension.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ExtensionResolver} 的扩展筛选、主要实现选择和只读结果测试。
 */
class ExtensionResolverTest {

    /**
     * 验证解析器可以同时按扩展 ID 和 {@code @Primary} 选择目标实现。
     */
    @Test
    void shouldResolveMappedAndPrimaryExtensions() {
        GreetingExtension first = new FirstGreetingExtension();
        GreetingExtension primary = new PrimaryGreetingExtension();
        ExtensionResolver resolver = new ExtensionResolver(pluginManager(Arrays.asList(first, primary)));

        assertSame(first, resolver.getRequired(GreetingExtension.class, "sample-plugin", "first"));
        assertSame(primary, resolver.getPrimary(GreetingExtension.class, "sample-plugin"));
        assertEquals(2, resolver.getExtensions(GreetingExtension.class, "sample-plugin").size());
    }

    /**
     * 验证解析器返回的扩展集合不允许调用方直接修改。
     */
    @Test
    void shouldExposeImmutableResults() {
        ExtensionResolver resolver = new ExtensionResolver(
                pluginManager(Arrays.<GreetingExtension>asList(new FirstGreetingExtension())));

        List<GreetingExtension> extensions = resolver.getExtensions(GreetingExtension.class, "sample-plugin");

        assertThrows(UnsupportedOperationException.class,
                () -> extensions.add(new PrimaryGreetingExtension()));
    }

    /**
     * 创建仅支持扩展查询的轻量 PF4J 管理器代理。
     *
     * @param extensions 查询扩展时返回的测试实现列表
     * @return 用于隔离解析器测试的 {@link PluginManager} 动态代理
     */
    private static PluginManager pluginManager(List<GreetingExtension> extensions) {
        return (PluginManager) Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class},
                (proxy, method, args) -> {
                    if ("getExtensions".equals(method.getName())) {
                        return extensions;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /**
     * 测试使用的问候扩展契约。
     */
    private interface GreetingExtension {
    }

    /**
     * 具有固定扩展 ID 的普通测试实现。
     */
    @ExtensionMapping(id = "first")
    private static final class FirstGreetingExtension implements GreetingExtension {
    }

    /**
     * 同时具有固定扩展 ID 和主要实现标记的测试实现。
     */
    @Primary
    @ExtensionMapping(id = "primary")
    private static final class PrimaryGreetingExtension implements GreetingExtension {
    }

}
