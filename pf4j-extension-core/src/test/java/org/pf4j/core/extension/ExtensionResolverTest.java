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

class ExtensionResolverTest {

    @Test
    void shouldResolveMappedAndPrimaryExtensions() {
        GreetingExtension first = new FirstGreetingExtension();
        GreetingExtension primary = new PrimaryGreetingExtension();
        ExtensionResolver resolver = new ExtensionResolver(pluginManager(Arrays.asList(first, primary)));

        assertSame(first, resolver.getRequired(GreetingExtension.class, "sample-plugin", "first"));
        assertSame(primary, resolver.getPrimary(GreetingExtension.class, "sample-plugin"));
        assertEquals(2, resolver.getExtensions(GreetingExtension.class, "sample-plugin").size());
    }

    @Test
    void shouldExposeImmutableResults() {
        ExtensionResolver resolver = new ExtensionResolver(
                pluginManager(Arrays.<GreetingExtension>asList(new FirstGreetingExtension())));

        List<GreetingExtension> extensions = resolver.getExtensions(GreetingExtension.class, "sample-plugin");

        assertThrows(UnsupportedOperationException.class,
                () -> extensions.add(new PrimaryGreetingExtension()));
    }

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

    private interface GreetingExtension {
    }

    @ExtensionMapping(id = "first")
    private static final class FirstGreetingExtension implements GreetingExtension {
    }

    @Primary
    @ExtensionMapping(id = "primary")
    private static final class PrimaryGreetingExtension implements GreetingExtension {
    }

}
