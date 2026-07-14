package org.pf4j.core.extension.diagnostic;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PluginDiagnostics} 运行报告与宿主 API 重复打包检测测试。
 */
class PluginDiagnosticsTest {

    /** JUnit 为测试创建的临时目录。 */
    @TempDir
    Path temporaryDirectory;

    /**
     * 验证诊断报告不持有扩展实例，并能发现制品重复携带的宿主 API 类。
     *
     * @throws Exception 创建测试插件制品失败时抛出
     */
    @Test
    void shouldReportRuntimeAndFindBundledHostApiClass() throws Exception {
        String resourceName = HostApi.class.getName().replace('.', '/') + ".class";
        Path pluginArtifact = temporaryDirectory.resolve("demo-plugin.jar");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(pluginArtifact))) {
            output.putNextEntry(new ZipEntry(resourceName));
            output.write(new byte[]{1});
            output.closeEntry();
        }
        PluginManager manager = pluginManager(pluginArtifact);
        PluginDiagnostics diagnostics = new PluginDiagnostics(manager);

        PluginDiagnosticReport report = diagnostics.diagnose("demo-plugin");

        assertEquals(PluginState.STARTED, report.getState());
        assertEquals(Collections.singletonList("com.acme.DemoExtension"), report.getExtensionClassNames());
        assertTrue(diagnostics.findBundledHostApiClasses("demo-plugin",
                Collections.<Class<?>>singletonList(HostApi.class)).contains(HostApi.class.getName()));
    }

    /**
     * 创建用于诊断的 PF4J 管理器代理。
     *
     * @param pluginArtifact 插件制品路径
     * @return PF4J 管理器代理
     */
    private static PluginManager pluginManager(Path pluginArtifact) {
        PluginManager[] manager = new PluginManager[1];
        PluginWrapper[] wrapper = new PluginWrapper[1];
        manager[0] = (PluginManager) Proxy.newProxyInstance(PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class}, (proxy, method, args) -> {
                    if ("getRuntimeMode".equals(method.getName())) {
                        return RuntimeMode.DEPLOYMENT;
                    }
                    if ("getPlugin".equals(method.getName())) {
                        return wrapper[0];
                    }
                    if ("getPlugins".equals(method.getName())) {
                        return Collections.singletonList(wrapper[0]);
                    }
                    if ("getExtensionClassNames".equals(method.getName())) {
                        return Collections.singleton("com.acme.DemoExtension");
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        DefaultPluginDescriptor descriptor = new DefaultPluginDescriptor("demo-plugin", "demo",
                "org.pf4j.Plugin", "1.0.0", "*", "test", "Apache-2.0");
        wrapper[0] = new PluginWrapper(manager[0], descriptor, pluginArtifact,
                PluginDiagnosticsTest.class.getClassLoader());
        wrapper[0].setPluginState(PluginState.STARTED);
        return manager[0];
    }

    /**
     * 用于验证重复打包检测的宿主 API 类型。
     */
    private interface HostApi {
    }
}
