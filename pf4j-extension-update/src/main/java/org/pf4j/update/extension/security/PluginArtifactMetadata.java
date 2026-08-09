package org.pf4j.update.extension.security;

import lombok.Getter;

/**
 * 已验证插件制品的审计元数据。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
/**
 * Data class holding metadata about a plugin artifact including coordinates, checksums, and signatures.
 */
public final class PluginArtifactMetadata {

    /** 插件 ID。 */
    private final String pluginId;

    /** 插件版本。 */
    private final String version;

    /** 发布下载地址。 */
    private final String sourceUrl;

    /** 发布声明的 SHA-512 摘要。 */
    private final String sha512;

    /**
     * 创建插件制品元数据。
     *
     * @param pluginId 插件 ID
     * @param version 插件版本
     * @param sourceUrl 下载地址
     * @param sha512 SHA-512 摘要
     */
    public PluginArtifactMetadata(String pluginId, String version, String sourceUrl, String sha512) {
        this.pluginId = pluginId;
        this.version = version;
        this.sourceUrl = sourceUrl;
        this.sha512 = sha512;
    }
}
