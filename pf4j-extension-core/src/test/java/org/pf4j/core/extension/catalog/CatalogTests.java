package org.pf4j.core.extension.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.pf4j.PluginState;

/**
 * Tests for catalog classes in the pf4j extension framework.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 */
class CatalogTests {

    @Test
    void extensionMetadataShouldHoldAllFields() {
        ExtensionMetadata metadata = new ExtensionMetadata(
            "plugin-1", "ext-1", "Test Extension", "1.0.0", "A test extension",
            "com.example.ExtensionPoint", "com.example.ExtensionImpl", 1, true);
        assertThat(metadata.getPluginId()).isEqualTo("plugin-1");
        assertThat(metadata.getExtensionId()).isEqualTo("ext-1");
        assertThat(metadata.getTitle()).isEqualTo("Test Extension");
        assertThat(metadata.getVersion()).isEqualTo("1.0.0");
        assertThat(metadata.getDescription()).isEqualTo("A test extension");
        assertThat(metadata.getExtensionPointClassName()).isEqualTo("com.example.ExtensionPoint");
        assertThat(metadata.getImplementationClassName()).isEqualTo("com.example.ExtensionImpl");
        assertThat(metadata.getOrdinal()).isEqualTo(1);
        assertThat(metadata.isPrimary()).isTrue();
    }

    @Test
    void pluginMetadataShouldHoldAllFields() {
        PluginMetadata metadata = new PluginMetadata("plugin-1", "1.0.0", "TestProvider", "Test Plugin",
            "A test plugin", PluginState.STARTED, Paths.get("/tmp/plugin.jar"), Collections.emptyList());
        assertThat(metadata.getPluginId()).isEqualTo("plugin-1");
        assertThat(metadata.getVersion()).isEqualTo("1.0.0");
        assertThat(metadata.getProvider()).isEqualTo("TestProvider");
        assertThat(metadata.getTitle()).isEqualTo("Test Plugin");
        assertThat(metadata.getDetail()).isEqualTo("A test plugin");
        assertThat(metadata.getState()).isEqualTo(PluginState.STARTED);
    }

    @Test
    void pluginMetadataShouldHandleDependencies() {
        PluginMetadata metadata = new PluginMetadata("plugin-1", "1.0.0", "provider", "title",
            "detail", PluginState.STOPPED, null, Arrays.asList("dep-1", "dep-2"));
        assertThat(metadata.getDependencies()).containsExactly("dep-1", "dep-2");
    }
}
