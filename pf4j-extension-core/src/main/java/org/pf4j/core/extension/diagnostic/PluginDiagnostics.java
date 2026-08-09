package org.pf4j.core.extension.diagnostic;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginDependency;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

/**
 * PF4J 插件依赖、扩展、类来源和制品重复类诊断服务。
 *
 * <p>服务只执行读取操作，不改变插件状态。制品检查支持插件 JAR、展开目录及目录内的依赖 JAR。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Service for collecting and analyzing diagnostic information about plugins and their runtime environment.
 */
class PluginDiagnostics {

    /** PF4J 插件管理器。 */
    private final PluginManager pluginManager;

    /**
     * 创建插件诊断服务。
     *
     * @param pluginManager PF4J 插件管理器
     */
    public PluginDiagnostics(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
    }

    /**
     * 生成指定插件的运行诊断报告。
     *
     * @param pluginId 插件 ID
     * @return 插件运行诊断报告
     * @throws IllegalArgumentException 当插件 ID 为空或插件不存在时抛出
     */
    public PluginDiagnosticReport diagnose(String pluginId) {
        PluginWrapper plugin = requirePlugin(pluginId);
        List<String> dependencies = new ArrayList<String>();
        for (PluginDependency dependency : plugin.getDescriptor().getDependencies()) {
            dependencies.add(dependency.getPluginId());
        }
        List<String> dependents = new ArrayList<String>();
        for (PluginWrapper candidate : pluginManager.getPlugins()) {
            for (PluginDependency dependency : candidate.getDescriptor().getDependencies()) {
                if (pluginId.equals(dependency.getPluginId())) {
                    dependents.add(candidate.getPluginId());
                    break;
                }
            }
        }
        List<String> extensionClassNames = new ArrayList<String>(pluginManager.getExtensionClassNames(pluginId));
        Collections.sort(extensionClassNames);
        Throwable failure = plugin.getFailedException();
        return new PluginDiagnosticReport(pluginId, plugin.getDescriptor().getVersion(), plugin.getPluginState(),
                plugin.getPluginPath(), plugin.getPluginClassLoader().getClass().getName(),
                Objects.isNull(failure) ? null : failure.getClass().getName(),
                Objects.isNull(failure) ? null : failure.getMessage(), dependencies, dependents,
                extensionClassNames);
    }

    /**
     * 查询插件类加载器实际返回的类资源位置。
     *
     * @param pluginId 插件 ID
     * @param className 类全限定名
     * @return 类资源 URL；不存在时返回 {@code null}
     */
    public URL findClassOrigin(String pluginId, String className) {
        PluginWrapper plugin = requirePlugin(pluginId);
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("className must not be blank");
        }
        return plugin.getPluginClassLoader().getResource(className.replace('.', '/') + ".class");
    }

    /**
     * 检查插件制品中是否重复包含指定宿主 API 类。
     *
     * @param pluginId 插件 ID
     * @param hostApiTypes 宿主 API 类型集合
     * @return 插件制品中实际存在的重复类名不可修改集合
     */
    public Set<String> findBundledHostApiClasses(String pluginId, Collection<Class<?>> hostApiTypes) {
        PluginWrapper plugin = requirePlugin(pluginId);
        Objects.requireNonNull(hostApiTypes, "hostApiTypes must not be null");
        Set<String> duplicates = new LinkedHashSet<String>();
        for (Class<?> type : hostApiTypes) {
            Objects.requireNonNull(type, "hostApiTypes must not contain null");
            String resourceName = type.getName().replace('.', '/') + ".class";
            if (containsResource(plugin.getPluginPath(), resourceName)) {
                duplicates.add(type.getName());
            }
        }
        return Collections.unmodifiableSet(duplicates);
    }

    /**
     * 检查插件路径或内部依赖 JAR 是否包含目标资源。
     *
     * @param pluginPath 插件路径
     * @param resourceName 类资源路径
     * @return 找到目标资源时返回 {@code true}
     */
    private boolean containsResource(Path pluginPath, String resourceName) {
        if (Files.isRegularFile(pluginPath)) {
            return containsZipResource(pluginPath, resourceName);
        }
        if (Files.exists(pluginPath.resolve(resourceName))
                || Files.exists(pluginPath.resolve("classes").resolve(resourceName))) {
            return true;
        }
        try (Stream<Path> paths = Files.walk(pluginPath)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                    .anyMatch(path -> containsZipResource(path, resourceName));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查 ZIP 或 JAR 中是否包含目标类资源。
     *
     * @param archive 压缩制品路径
     * @param resourceName 类资源路径
     * @return 找到目标资源时返回 {@code true}
     */
    private boolean containsZipResource(Path archive, String resourceName) {
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            if (Objects.nonNull(zipFile.getEntry(resourceName))
                    || Objects.nonNull(zipFile.getEntry("classes/" + resourceName))) {
                return true;
            }
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith("/" + resourceName)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 获取并校验插件包装器。
     *
     * @param pluginId 插件 ID
     * @return 已加载插件包装器
     */
    private PluginWrapper requirePlugin(String pluginId) {
        if (StringUtils.isBlank(pluginId)) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        PluginWrapper plugin = pluginManager.getPlugin(pluginId);
        if (Objects.isNull(plugin)) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }
        return plugin;
    }
}
