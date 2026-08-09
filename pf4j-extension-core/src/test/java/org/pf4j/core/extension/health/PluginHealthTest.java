package org.pf4j.core.extension.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PluginHealth}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class PluginHealthTest {

    @Test
    void upShouldCreateHealthyStatus() {
        PluginHealth health = PluginHealth.up("all good");
        assertThat(health.getStatus()).isEqualTo(PluginHealth.Status.UP);
        assertThat(health.getMessage()).isEqualTo("all good");
        assertThat(health.isHealthy()).isTrue();
    }

    @Test
    void downShouldCreateUnhealthyStatus() {
        PluginHealth health = PluginHealth.down("connection failed");
        assertThat(health.getStatus()).isEqualTo(PluginHealth.Status.DOWN);
        assertThat(health.getMessage()).isEqualTo("connection failed");
        assertThat(health.isHealthy()).isFalse();
    }

    @Test
    void constructorShouldSetAllFields() {
        Map<String, String> details = new HashMap<>();
        details.put("key", "value");
        PluginHealth health = new PluginHealth(PluginHealth.Status.DEGRADED, "partial", details);
        assertThat(health.getStatus()).isEqualTo(PluginHealth.Status.DEGRADED);
        assertThat(health.getMessage()).isEqualTo("partial");
        assertThat(health.getDetails()).containsEntry("key", "value");
    }

    @Test
    void constructorShouldHandleNullDetails() {
        PluginHealth health = new PluginHealth(PluginHealth.Status.UP, "ok", null);
        assertThat(health.getDetails()).isEmpty();
    }

    @Test
    void detailsShouldBeUnmodifiable() {
        Map<String, String> details = new HashMap<>();
        details.put("key", "value");
        PluginHealth health = new PluginHealth(PluginHealth.Status.UP, "ok", details);
        assertThat(health.getDetails()).isUnmodifiable();
    }

    @Test
    void statusEnumShouldHaveAllValues() {
        assertThat(PluginHealth.Status.values()).hasSize(4);
        assertThat(PluginHealth.Status.UP).isNotNull();
        assertThat(PluginHealth.Status.DEGRADED).isNotNull();
        assertThat(PluginHealth.Status.DOWN).isNotNull();
        assertThat(PluginHealth.Status.UNKNOWN).isNotNull();
    }
}
