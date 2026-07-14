package org.pf4j.spring.extension;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ExtendedSpringPluginManager} 容器生命周期幂等性测试。
 */
class ExtendedSpringPluginManagerTest {

    /**
     * 为每个测试创建的空插件根目录。
     */
    @TempDir
    Path pluginsRoot;

    /**
     * 验证重复初始化和重复销毁只会发生一次有效状态迁移。
     */
    @Test
    void shouldInitializeAndDestroyIdempotently() {
        ExtendedSpringPluginManager pluginManager = new ExtendedSpringPluginManager(null,
                pluginsRoot, false, false, false);

        pluginManager.afterPropertiesSet();
        pluginManager.afterPropertiesSet();
        assertTrue(pluginManager.isInitialized());

        pluginManager.destroy();
        pluginManager.destroy();
        assertFalse(pluginManager.isInitialized());
    }

}
