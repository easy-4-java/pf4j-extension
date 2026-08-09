package org.pf4j.core.extension.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for exception classes in the pf4j extension framework.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class ExceptionTests {

    @Test
    void extensionConflictExceptionShouldHoldMessage() {
        ExtensionConflictException ex = new ExtensionConflictException("duplicate extension");
        assertThat(ex.getMessage()).isEqualTo("duplicate extension");
    }

    @Test
    void extensionResolutionExceptionShouldHoldMessage() {
        ExtensionResolutionException ex = new ExtensionResolutionException("extension not found");
        assertThat(ex.getMessage()).isEqualTo("extension not found");
    }

    @Test
    void pluginInvokeExceptionShouldHoldPluginId() {
        RuntimeException cause = new RuntimeException("root cause");
        PluginInvokeException ex = new PluginInvokeException("plugin-1", cause);
        assertThat(ex.getPluginId()).isEqualTo("plugin-1");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void pluginInvokeExceptionShouldHoldPluginIdAndExtensionId() {
        RuntimeException cause = new RuntimeException("root cause");
        PluginInvokeException ex = new PluginInvokeException("plugin-1", "ext-1", cause);
        assertThat(ex.getPluginId()).isEqualTo("plugin-1");
        assertThat(ex.getExtensionId()).isEqualTo("ext-1");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void pluginLifecycleExceptionShouldHoldMessage() {
        PluginLifecycleException ex = new PluginLifecycleException("lifecycle error");
        assertThat(ex.getMessage()).isEqualTo("lifecycle error");
    }

    @Test
    void exceptionsShouldExtendPluginRuntimeException() {
        assertThat(new ExtensionConflictException("test")).isInstanceOf(org.pf4j.PluginRuntimeException.class);
        assertThat(new ExtensionResolutionException("test")).isInstanceOf(org.pf4j.PluginRuntimeException.class);
        assertThat(new PluginInvokeException("p", new RuntimeException())).isInstanceOf(org.pf4j.PluginRuntimeException.class);
        assertThat(new PluginLifecycleException("test")).isInstanceOf(org.pf4j.PluginRuntimeException.class);
    }
}
