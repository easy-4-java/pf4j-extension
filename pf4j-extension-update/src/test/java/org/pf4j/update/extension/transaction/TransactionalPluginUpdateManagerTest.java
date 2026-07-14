package org.pf4j.update.extension.transaction;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.DefaultVersionManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;
import org.pf4j.core.extension.PluginLifecycleManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TransactionalPluginUpdateManager} 更新失败自动回滚测试。
 */
class TransactionalPluginUpdateManagerTest {

    /** JUnit 为测试创建的临时目录。 */
    @TempDir
    Path temporaryDirectory;

    /**
     * 验证新版本启动失败时恢复旧制品、旧版本和运行状态。
     *
     * @throws Exception 准备或读取测试制品失败时抛出
     */
    @Test
    void shouldRestorePreviousArtifactWhenUpdatedPluginFailsToStart() throws Exception {
        Path pluginsRoot = Files.createDirectories(temporaryDirectory.resolve("plugins"));
        Path current = Files.write(pluginsRoot.resolve("demo.jar"), "old".getBytes(StandardCharsets.UTF_8));
        Path downloaded = Files.write(temporaryDirectory.resolve("downloaded.jar"),
                "new".getBytes(StandardCharsets.UTF_8));
        PluginRuntime runtime = new PluginRuntime(pluginsRoot, current);
        PluginLifecycleManager lifecycleManager = new PluginLifecycleManager(runtime.pluginManager);
        FileSystemPluginArtifactStore store = new FileSystemPluginArtifactStore(pluginsRoot,
                temporaryDirectory.resolve("backups"));
        TransactionalPluginUpdateManager manager = new TestUpdateManager(runtime.pluginManager, lifecycleManager,
                store, downloaded);

        PluginUpdateResult result = manager.updateTransactional("demo-plugin", "2.0.0");

        assertFalse(result.isSuccess());
        assertTrue(result.isRolledBack());
        assertEquals("old", new String(Files.readAllBytes(current), StandardCharsets.UTF_8));
        assertEquals("1.0.0", runtime.pluginManager.getPlugin("demo-plugin").getDescriptor().getVersion());
        assertEquals(PluginState.STARTED, runtime.pluginManager.getPlugin("demo-plugin").getPluginState());
    }

    /**
     * 使用固定下载制品的事务更新管理器。
     */
    private static final class TestUpdateManager extends TransactionalPluginUpdateManager {

        /** 测试下载制品。 */
        private final Path downloaded;

        /**
         * 创建测试更新管理器。
         *
         * @param pluginManager PF4J 管理器
         * @param lifecycleManager 生命周期管理器
         * @param artifactStore 制品存储
         * @param downloaded 固定下载制品
         */
        private TestUpdateManager(PluginManager pluginManager, PluginLifecycleManager lifecycleManager,
                                  PluginArtifactStore artifactStore, Path downloaded) {
            super(pluginManager, Collections.emptyList(), lifecycleManager, artifactStore, 1);
            this.downloaded = downloaded;
        }

        /**
         * 返回预先创建并视为已验证的测试制品。
         *
         * @param id 插件 ID
         * @param version 目标版本
         * @return 测试制品路径
         */
        @Override
        protected Path downloadPlugin(String id, String version) {
            return downloaded;
        }
    }

    /**
     * 模拟 PF4J 加载、启动、停止和卸载状态的测试运行时。
     */
    private static final class PluginRuntime {

        /** 插件运行映射。 */
        private final Map<String, PluginWrapper> plugins = new LinkedHashMap<String, PluginWrapper>();

        /** 仅让新版本第一次启动失败。 */
        private final AtomicBoolean rejectNewVersion = new AtomicBoolean(true);

        /** PF4J 管理器代理。 */
        private final PluginManager pluginManager;

        /**
         * 创建包含一个已启动旧版本插件的测试运行时。
         *
         * @param pluginsRoot 插件目录
         * @param currentArtifact 当前旧版本制品
         */
        private PluginRuntime(Path pluginsRoot, Path currentArtifact) {
            PluginManager[] manager = new PluginManager[1];
            manager[0] = (PluginManager) Proxy.newProxyInstance(PluginManager.class.getClassLoader(),
                    new Class<?>[]{PluginManager.class}, (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getRuntimeMode".equals(name)) {
                            return RuntimeMode.DEPLOYMENT;
                        }
                        if ("getVersionManager".equals(name)) {
                            return new DefaultVersionManager();
                        }
                        if ("getSystemVersion".equals(name)) {
                            return "1.0.0";
                        }
                        if ("getPluginsRoot".equals(name)) {
                            return pluginsRoot;
                        }
                        if ("getPlugin".equals(name)) {
                            return plugins.get(args[0]);
                        }
                        if ("getPlugins".equals(name)) {
                            return new ArrayList<PluginWrapper>(plugins.values());
                        }
                        if ("getExtensions".equals(name)) {
                            return Collections.emptyList();
                        }
                        if ("loadPlugin".equals(name)) {
                            Path artifact = (Path) args[0];
                            String content = new String(Files.readAllBytes(artifact), StandardCharsets.UTF_8);
                            plugins.put("demo-plugin", wrapper(manager[0], artifact,
                                    "new".equals(content) ? "2.0.0" : "1.0.0", PluginState.RESOLVED));
                            return "demo-plugin";
                        }
                        if ("startPlugin".equals(name)) {
                            PluginWrapper wrapper = plugins.get(args[0]);
                            if ("2.0.0".equals(wrapper.getDescriptor().getVersion())
                                    && rejectNewVersion.compareAndSet(true, false)) {
                                wrapper.setPluginState(PluginState.FAILED);
                                return PluginState.FAILED;
                            }
                            wrapper.setPluginState(PluginState.STARTED);
                            return PluginState.STARTED;
                        }
                        if ("stopPlugin".equals(name)) {
                            plugins.get(args[0]).setPluginState(PluginState.STOPPED);
                            return PluginState.STOPPED;
                        }
                        if ("unloadPlugin".equals(name)) {
                            plugins.remove(args[0]);
                            return true;
                        }
                        throw new UnsupportedOperationException(name);
                    });
            pluginManager = manager[0];
            plugins.put("demo-plugin", wrapper(pluginManager, currentArtifact, "1.0.0", PluginState.STARTED));
        }

        /**
         * 创建指定版本和状态的插件包装器。
         *
         * @param manager PF4J 管理器
         * @param artifact 插件制品
         * @param version 插件版本
         * @param state 插件状态
         * @return 插件包装器
         */
        private PluginWrapper wrapper(PluginManager manager, Path artifact, String version, PluginState state) {
            DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor("demo-plugin", "demo",
                    "org.pf4j.Plugin", version, "*", "test", "Apache-2.0");
            PluginWrapper wrapper = new PluginWrapper(manager, descriptor, artifact,
                    TransactionalPluginUpdateManagerTest.class.getClassLoader());
            wrapper.setPluginState(state);
            return wrapper;
        }
    }
}
