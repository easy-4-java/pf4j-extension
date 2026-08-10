package org.pf4j.spring.extension.event;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;

/**
 * Spring 插件状态事件中的依赖快照。
 *
 * <p>快照只保存插件依赖的基础属性，不持有 PF4J {@code PluginDependency} 对象，适合事件异步
 * 传递、序列化和审计记录。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public final class SpringPluginDependencyInfo implements Serializable {

    /** 被依赖插件的唯一标识。 */
    private final String pluginId;

    /** 被依赖插件需要满足的版本约束。 */
    private final String versionSupport;

    /** 当前依赖是否为可选依赖。 */
    private final boolean optional;

    /**
     * 创建插件依赖快照。
     *
     * @param pluginId 被依赖插件 ID
     * @param versionSupport 版本约束
     * @param optional 是否为可选依赖
     */
    public SpringPluginDependencyInfo(String pluginId, String versionSupport, boolean optional) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId must not be null");
        this.versionSupport = Objects.requireNonNull(versionSupport, "versionSupport must not be null");
        this.optional = optional;
    }
}
