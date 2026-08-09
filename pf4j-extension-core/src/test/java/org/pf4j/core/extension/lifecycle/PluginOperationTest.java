package org.pf4j.core.extension.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PluginOperation}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class PluginOperationTest {

    @Test
    void loadShouldExist() {
        assertThat(PluginOperation.LOAD).isNotNull();
    }

    @Test
    void startShouldExist() {
        assertThat(PluginOperation.START).isNotNull();
    }

    @Test
    void stopShouldExist() {
        assertThat(PluginOperation.STOP).isNotNull();
    }

    @Test
    void unloadShouldExist() {
        assertThat(PluginOperation.UNLOAD).isNotNull();
    }

    @Test
    void replaceShouldExist() {
        assertThat(PluginOperation.REPLACE).isNotNull();
    }

    @Test
    void rollbackShouldExist() {
        assertThat(PluginOperation.ROLLBACK).isNotNull();
    }

    @Test
    void valuesShouldContainAllOperations() {
        assertThat(PluginOperation.values()).hasSize(6);
    }

    @Test
    void valueOfShouldReturnCorrectEnum() {
        assertThat(PluginOperation.valueOf("LOAD")).isEqualTo(PluginOperation.LOAD);
        assertThat(PluginOperation.valueOf("START")).isEqualTo(PluginOperation.START);
        assertThat(PluginOperation.valueOf("STOP")).isEqualTo(PluginOperation.STOP);
    }
}
