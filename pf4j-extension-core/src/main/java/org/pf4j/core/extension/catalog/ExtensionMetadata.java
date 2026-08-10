package org.pf4j.core.extension.catalog;

import lombok.Getter;

/**
 * PF4J 扩展目录元数据。
 *
 * <p>描述一个已启动插件贡献的扩展实现，不持有扩展实例，从而允许插件停止后及时释放实例和
 * 类加载器引用。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public final class ExtensionMetadata {

    /** 提供扩展的插件 ID。 */
    private final String pluginId;

    /** 扩展稳定 ID。 */
    private final String extensionId;

    /** 扩展展示标题。 */
    private final String title;

    /** 扩展版本。 */
    private final String version;

    /** 扩展描述。 */
    private final String description;

    /** 扩展点类型名称。 */
    private final String extensionPointClassName;

    /** 扩展实现类型名称。 */
    private final String implementationClassName;

    /** PF4J 扩展排序值。 */
    private final int ordinal;

    /** 是否为主要实现。 */
    private final boolean primary;

    /**
     * 创建扩展元数据。
     *
     * @param pluginId 插件 ID
     * @param extensionId 扩展稳定 ID
     * @param title 展示标题
     * @param version 扩展版本
     * @param description 扩展描述
     * @param extensionPointClassName 扩展点类型名称
     * @param implementationClassName 实现类型名称
     * @param ordinal PF4J 排序值
     * @param primary 是否为主要实现
     */
    public ExtensionMetadata(String pluginId, String extensionId, String title, String version, String description,
                             String extensionPointClassName, String implementationClassName, int ordinal,
                             boolean primary) {
        this.pluginId = pluginId;
        this.extensionId = extensionId;
        this.title = title;
        this.version = version;
        this.description = description;
        this.extensionPointClassName = extensionPointClassName;
        this.implementationClassName = implementationClassName;
        this.ordinal = ordinal;
        this.primary = primary;
    }
}
