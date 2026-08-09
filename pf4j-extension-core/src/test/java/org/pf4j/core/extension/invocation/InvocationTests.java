package org.pf4j.core.extension.invocation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * Tests for invocation classes in the pf4j extension framework.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class InvocationTests {

    @Test
    void extensionInvocationContextShouldHoldMethod() throws Exception {
        Method method = String.class.getMethod("length");
        Object target = "test";
        Object[] args = new Object[0];
        ExtensionInvocationContext context = new ExtensionInvocationContext("plugin-1", "ext-1", target, method, args);
        assertThat(context.getMethod()).isEqualTo(method);
        assertThat(context.getTarget()).isEqualTo(target);
        assertThat(context.getArguments()).isEmpty();
        assertThat(context.getPluginId()).isEqualTo("plugin-1");
        assertThat(context.getExtensionId()).isEqualTo("ext-1");
    }

    @Test
    void extensionInvocationContextShouldHandleNullArguments() throws Exception {
        Method method = String.class.getMethod("length");
        Object target = "test";
        ExtensionInvocationContext context = new ExtensionInvocationContext("plugin-1", "ext-1", target, method, null);
        assertThat(context.getArguments()).isEmpty();
    }

    @Test
    void extensionInvocationContextShouldProvideAttributes() throws Exception {
        Method method = String.class.getMethod("length");
        Object target = "test";
        ExtensionInvocationContext context = new ExtensionInvocationContext("plugin-1", "ext-1", target, method, new Object[0]);
        context.setAttribute("key", "value");
        assertThat(context.<String>getAttribute("key")).isEqualTo("value");
    }

    @Test
    void loggingExtensionInterceptorShouldImplementInterface() {
        LoggingExtensionInterceptor interceptor = new LoggingExtensionInterceptor();
        assertThat(interceptor).isInstanceOf(ExtensionInterceptor.class);
    }

    @Test
    void timingExtensionInterceptorShouldImplementInterface() {
        ExtensionInvocationListener listener = (context, durationNanos, failure) -> { };
        TimingExtensionInterceptor interceptor = new TimingExtensionInterceptor(listener);
        assertThat(interceptor).isInstanceOf(ExtensionInterceptor.class);
    }
}
