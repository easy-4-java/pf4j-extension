package org.pf4j.update.extension.security;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.pf4j.update.FileDownloader;

/**
 * 带协议和下载后文件大小限制的 PF4J 文件下载器装饰器。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final class SecureFileDownloader implements FileDownloader {

    /** 实际执行下载的文件下载器。 */
    private final FileDownloader delegate;

    /** 下载资源限制策略。 */
    private final DownloadPolicy policy;

    /**
     * 创建安全文件下载器。
     *
     * @param delegate 实际文件下载器
     * @param policy 下载资源限制策略
     */
    public SecureFileDownloader(FileDownloader delegate, DownloadPolicy policy) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    /**
     * 校验协议，委托下载并检查最终文件大小。
     *
     * @param fileUrl 插件发布 URL
     * @return 下载后的本地文件
     * @throws IOException 协议不允许、下载失败或文件过大时抛出
     */
    @Override
    public Path downloadFile(URL fileUrl) throws IOException {
        Objects.requireNonNull(fileUrl, "fileUrl must not be null");
        if (!policy.getAllowedProtocols().contains(fileUrl.getProtocol().toLowerCase())) {
            throw new IOException("Plugin download protocol is not allowed: " + fileUrl.getProtocol());
        }
        Path file = delegate.downloadFile(fileUrl);
        if (Files.size(file) > policy.getMaximumFileSize()) {
            throw new IOException("Downloaded plugin exceeds maximum size");
        }
        return file;
    }
}
