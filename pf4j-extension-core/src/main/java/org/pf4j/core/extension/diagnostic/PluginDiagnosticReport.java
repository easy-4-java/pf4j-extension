package org.pf4j.core.extension.diagnostic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import org.pf4j.PluginState;

/**
 * 插件运行诊断报告。
 *
 * <p>报告仅保存字符串、路径和枚举，不持有插件类、扩展实例或插件类加载器。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
/**
 * Report containing diagnostic information about plugin health, dependencies, and runtime state.
 */
public final class PluginDiagnosticReport {

    /** 插件 ID。 */
    private final String pluginId;

    /** 插件版本。 */
    private final String version;

    /** 插件状态。 */
    private final PluginState state;

    /** 插件制品路径。 */
    private final Path pluginPath;

    /** 插件类加载器类型名称。 */
    private final String classLoaderClassName;

    /** 最近失败异常类型。 */
    private final String failureClassName;

    /** 最近失败异常消息。 */
    private final String failureMessage;

    /** 插件直接依赖。 */
    private final List<String> dependencies;

    /** 直接依赖当前插件的插件。 */
    private final List<String> dependents;

    /** 当前插件声明的扩展实现类名。 */
    private final List<String> extensionClassNames;

    /**
     * 创建插件诊断报告。
     *
     * @param pluginId 插件 ID
     * @param version 插件版本
     * @param state 插件状态
     * @param pluginPath 插件路径
     * @param classLoaderClassName 类加载器类型名称
     * @param failureClassName 失败异常类型
     * @param failureMessage 失败异常消息
     * @param dependencies 直接依赖
     * @param dependents 直接依赖方
     * @param extensionClassNames 扩展实现类名
     */
    public PluginDiagnosticReport(String pluginId, String version, PluginState state, Path pluginPath,
                                  String classLoaderClassName, String failureClassName, String failureMessage,
                                  List<String> dependencies, List<String> dependents,
                                  List<String> extensionClassNames) {
        this.pluginId = pluginId;
        this.version = version;
        this.state = state;
        this.pluginPath = pluginPath;
        this.classLoaderClassName = classLoaderClassName;
        this.failureClassName = failureClassName;
        this.failureMessage = failureMessage;
        this.dependencies = immutableCopy(dependencies);
        this.dependents = immutableCopy(dependents);
        this.extensionClassNames = immutableCopy(extensionClassNames);
    }

    /**
     * 创建字符串列表的不可修改副本。
     *
     * @param values 源列表
     * @return 不可修改副本
     */
    private static List<String> immutableCopy(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }
}
