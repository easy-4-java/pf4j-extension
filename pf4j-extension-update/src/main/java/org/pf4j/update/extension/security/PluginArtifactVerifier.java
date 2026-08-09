package org.pf4j.update.extension.security;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.pf4j.update.FileVerifier;
import org.pf4j.update.VerifyException;
import org.pf4j.update.verifier.CompoundVerifier;

/**
 * 组合 PF4J 基础校验和生产制品策略的文件验证器。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
/**
 * Verifier for plugin artifacts that applies multiple verification policies for security.
 */
public final class PluginArtifactVerifier implements FileVerifier {

    /** PF4J 基础文件和 SHA-512 校验器。 */
    private final FileVerifier delegate;

    /** 按顺序执行的附加验证策略。 */
    private final List<ArtifactVerificationPolicy> policies;

    /**
     * 使用生产默认下载与压缩策略创建验证器。
     */
    public PluginArtifactVerifier() {
        this(DownloadPolicy.secureDefaults());
    }

    /**
     * 使用指定下载策略创建验证器。
     *
     * @param policy 下载与压缩资源限制策略
     */
    public PluginArtifactVerifier(DownloadPolicy policy) {
        this(new CompoundVerifier(), Arrays.<ArtifactVerificationPolicy>asList(
                new ReleaseVerificationPolicy(policy), new ArchiveStructureVerificationPolicy(policy)));
    }

    /**
     * 创建自定义组合验证器。
     *
     * @param delegate PF4J 基础验证器
     * @param policies 附加验证策略
     */
    public PluginArtifactVerifier(FileVerifier delegate, List<ArtifactVerificationPolicy> policies) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.policies = Collections.unmodifiableList(new ArrayList<ArtifactVerificationPolicy>(
                Objects.requireNonNull(policies, "policies must not be null")));
    }

    /**
     * 依次执行 PF4J 基础校验和全部附加策略。
     *
     * @param context PF4J 发布上下文
     * @param file 本地制品路径
     * @throws IOException 读取制品失败时抛出
     * @throws VerifyException 任一验证器拒绝制品时抛出
     */
    @Override
    public void verify(Context context, Path file) throws IOException, VerifyException {
        delegate.verify(context, file);
        for (ArtifactVerificationPolicy policy : policies) {
            policy.verify(context, file);
        }
    }
}
