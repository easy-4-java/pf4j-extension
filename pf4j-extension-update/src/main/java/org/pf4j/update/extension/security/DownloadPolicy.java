package org.pf4j.update.extension.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;

/**
 * 插件下载和压缩制品资源限制策略。
 *
 * <p>默认允许 HTTPS 与本地 file 协议，限制单文件大小、压缩条目数和声明的解压总量。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public final class DownloadPolicy {

    /** 允许的 URL 协议集合。 */
    private final Set<String> allowedProtocols;

    /** 下载文件最大字节数。 */
    private final long maximumFileSize;

    /** 压缩文件最大条目数。 */
    private final int maximumArchiveEntries;

    /** 压缩文件声明的最大解压字节数。 */
    private final long maximumUncompressedSize;

    /** 是否要求发布元数据包含 SHA-512 摘要。 */
    private final boolean checksumRequired;

    /**
     * 创建下载策略。
     *
     * @param allowedProtocols 允许的 URL 协议
     * @param maximumFileSize 最大文件大小
     * @param maximumArchiveEntries 最大压缩条目数
     * @param maximumUncompressedSize 最大解压字节数
     * @param checksumRequired 是否要求 SHA-512 摘要
     */
    public DownloadPolicy(Set<String> allowedProtocols, long maximumFileSize, int maximumArchiveEntries,
                          long maximumUncompressedSize, boolean checksumRequired) {
        Objects.requireNonNull(allowedProtocols, "allowedProtocols must not be null");
        if (allowedProtocols.isEmpty() || maximumFileSize <= 0 || maximumArchiveEntries <= 0
                || maximumUncompressedSize <= 0) {
            throw new IllegalArgumentException("Download policy limits and protocols must be positive");
        }
        Set<String> protocols = new LinkedHashSet<String>();
        for (String protocol : allowedProtocols) {
            protocols.add(protocol.toLowerCase());
        }
        this.allowedProtocols = Collections.unmodifiableSet(protocols);
        this.maximumFileSize = maximumFileSize;
        this.maximumArchiveEntries = maximumArchiveEntries;
        this.maximumUncompressedSize = maximumUncompressedSize;
        this.checksumRequired = checksumRequired;
    }

    /**
     * 创建适合生产默认值的下载策略。
     *
     * @return 允许 HTTPS/file、最大 256 MiB、最多 10000 条目、最大解压 1 GiB且要求摘要的策略
     */
    public static DownloadPolicy secureDefaults() {
        return new DownloadPolicy(new LinkedHashSet<String>(Arrays.asList("https", "file")),
                256L * 1024L * 1024L, 10000, 1024L * 1024L * 1024L, true);
    }
}
