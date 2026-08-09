package org.pf4j.core.extension.catalog;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import org.pf4j.PluginState;

/**
 * 插件目录元数据。
 *
 * <p>聚合 PF4J 描述符和 {@code @PluginMapping} 展示信息，为管理界面、审计和诊断提供稳定的
 * 只读模型。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
public final /**
 * Data class holding metadata about a plugin including its ID, version, description, and dependencies.
 */
class PluginMetadata {

    /** 插件 ID。 */
    private final String pluginId;

    /** 插件版本。 */
    private final String version;

    /** 插件提供者。 */
    private final String provider;

    /** 展示标题。 */
    private final String title;

    /** 展示详情。 */
    private final String detail;

    /** 插件当前状态。 */
    private final PluginState state;

    /** 插件制品路径。 */
    private final Path pluginPath;

    /** 必选及可选依赖的字符串表示。 */
    private final List<String> dependencies;

    /**
     * 创建插件目录元数据。
     *
     * @param pluginId 插件 ID
     * @param version 插件版本
     * @param provider 插件提供者
     * @param title 展示标题
     * @param detail 展示详情
     * @param state 当前状态
     * @param pluginPath 插件路径
     * @param dependencies 插件依赖
     */
    public PluginMetadata(String pluginId, String version, String provider, String title, String detail,
                          PluginState state, Path pluginPath, List<String> dependencies) {
        this.pluginId = pluginId;
        this.version = version;
        this.provider = provider;
        this.title = title;
        this.detail = detail;
        this.state = state;
        this.pluginPath = pluginPath;
        this.dependencies = Collections.unmodifiableList(dependencies);
    }
}
