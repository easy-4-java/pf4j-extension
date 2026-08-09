package org.pf4j.spring.extension.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

/**
 * Spring 插件状态事件中的插件描述符快照。
 *
 * <p>快照完整保留 PF4J 标准描述符字段和结构化依赖信息，但不持有插件描述符、插件实例、
 * 插件类型或类加载器。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
/**
 * Data class holding descriptor information about a Spring-managed plugin.
 */
public final class SpringPluginDescriptorInfo implements Serializable {

    /** 插件唯一标识。 */
    private final String pluginId;

    /** 插件功能描述。 */
    private final String description;

    /** 插件入口类全限定名。 */
    private final String pluginClassName;

    /** 插件版本。 */
    private final String version;

    /** 插件要求的宿主版本约束。 */
    private final String requires;

    /** 插件提供方。 */
    private final String provider;

    /** 插件许可证标识。 */
    private final String license;

    /** 插件声明的不可修改依赖快照。 */
    private final List<SpringPluginDependencyInfo> dependencies;

    /**
     * 创建插件描述符快照。
     *
     * @param pluginId 插件 ID
     * @param description 插件描述
     * @param pluginClassName 插件入口类名
     * @param version 插件版本
     * @param requires 宿主版本约束
     * @param provider 插件提供方
     * @param license 插件许可证
     * @param dependencies 插件依赖快照集合
     */
    public SpringPluginDescriptorInfo(String pluginId, String description, String pluginClassName, String version,
                                      String requires, String provider, String license,
                                      List<SpringPluginDependencyInfo> dependencies) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId must not be null");
        this.description = description;
        this.pluginClassName = pluginClassName;
        this.version = version;
        this.requires = requires;
        this.provider = provider;
        this.license = license;
        List<SpringPluginDependencyInfo> source = Objects.isNull(dependencies)
                ? Collections.<SpringPluginDependencyInfo>emptyList() : dependencies;
        this.dependencies = Collections.unmodifiableList(new ArrayList<SpringPluginDependencyInfo>(source));
    }
}
