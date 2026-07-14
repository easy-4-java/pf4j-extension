package org.pf4j.spring.extension;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtendedSpringPluginManagerTest {

    @TempDir
    Path pluginsRoot;

    @Test
    void shouldInitializeAndDestroyIdempotently() {
        ExtendedSpringPluginManager pluginManager = new ExtendedSpringPluginManager(
                pluginsRoot, false, false, false);

        pluginManager.afterPropertiesSet();
        pluginManager.afterPropertiesSet();
        assertTrue(pluginManager.isInitialized());

        pluginManager.destroy();
        pluginManager.destroy();
        assertFalse(pluginManager.isInitialized());
    }

}
