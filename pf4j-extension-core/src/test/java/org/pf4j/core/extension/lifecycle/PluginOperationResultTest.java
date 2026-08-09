package org.pf4j.core.extension.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.pf4j.PluginState;

/**
 * Tests for {@link PluginOperationResult}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class PluginOperationResultTest {

    @Test
    void constructorShouldSetFields() {
        PluginOperationResult result = new PluginOperationResult(
            "plugin-1", PluginOperation.START, PluginState.STOPPED, PluginState.STARTED, true, 1000L, null);
        assertThat(result.getPluginId()).isEqualTo("plugin-1");
        assertThat(result.getOperation()).isEqualTo(PluginOperation.START);
        assertThat(result.getPreviousState()).isEqualTo(PluginState.STOPPED);
        assertThat(result.getCurrentState()).isEqualTo(PluginState.STARTED);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDurationNanos()).isEqualTo(1000L);
        assertThat(result.getFailure()).isNull();
    }

    @Test
    void constructorShouldHandleFailure() {
        RuntimeException error = new RuntimeException("test error");
        PluginOperationResult result = new PluginOperationResult(
            "plugin-1", PluginOperation.STOP, PluginState.STARTED, PluginState.STOPPED, false, 500L, error);
        assertThat(result.getPluginId()).isEqualTo("plugin-1");
        assertThat(result.getOperation()).isEqualTo(PluginOperation.STOP);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure()).isEqualTo(error);
    }
}
