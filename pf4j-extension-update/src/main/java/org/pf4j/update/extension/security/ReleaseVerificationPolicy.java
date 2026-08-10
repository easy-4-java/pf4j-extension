package org.pf4j.update.extension.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.pf4j.update.FileVerifier;
import org.pf4j.update.VerifyException;

/**
 * 插件发布协议、摘要和文件大小验证策略。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public final class ReleaseVerificationPolicy implements ArtifactVerificationPolicy {

    /** 下载资源限制策略。 */
    private final DownloadPolicy policy;

    /**
     * 创建发布验证策略。
     *
     * @param policy 下载资源限制策略
     */
    public ReleaseVerificationPolicy(DownloadPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    /**
     * 验证发布 URL、摘要声明和本地文件大小。
     *
     * @param context PF4J 发布上下文
     * @param file 本地制品路径
     * @throws IOException 读取文件大小失败时抛出
     * @throws VerifyException 发布不满足策略时抛出
     */
    @Override
    public void verify(FileVerifier.Context context, Path file) throws IOException, VerifyException {
        try {
            URL releaseUrl = new URL(context.url);
            if (!policy.getAllowedProtocols().contains(releaseUrl.getProtocol().toLowerCase())) {
                throw new VerifyException("Plugin download protocol is not allowed: " + releaseUrl.getProtocol());
            }
        } catch (MalformedURLException e) {
            throw new VerifyException(e, "Invalid plugin release URL {}", context.url);
        }
        if (policy.isChecksumRequired() && (Objects.isNull(context.sha512sum)
                || context.sha512sum.trim().isEmpty())) {
            throw new VerifyException("Plugin release must declare a SHA-512 checksum");
        }
        long size = Files.size(file);
        if (size > policy.getMaximumFileSize()) {
            throw new VerifyException("Plugin artifact exceeds maximum size: " + size);
        }
    }
}
