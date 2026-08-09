package org.pf4j.update.extension.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Objects;

import org.pf4j.update.FileVerifier;
import org.pf4j.update.VerifyException;

/**
 * 基于 JCA 公钥的插件分离签名验证策略。
 *
 * <p>算法由调用方指定，可使用 Ed25519、SHA256withRSA 或其他当前 JDK 支持的签名算法。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
/**
 * Policy that verifies detached PGP signatures for plugin artifact integrity.
 */
public final class DetachedSignatureVerificationPolicy implements ArtifactVerificationPolicy {

    /** 签名算法名称。 */
    private final String algorithm;

    /** 验证签名使用的公钥。 */
    private final PublicKey publicKey;

    /** 提供发布分离签名的策略。 */
    private final SignatureProvider signatureProvider;

    /**
     * 创建分离签名验证策略。
     *
     * @param algorithm JCA 签名算法
     * @param publicKey 验签公钥
     * @param signatureProvider 分离签名提供器
     */
    public DetachedSignatureVerificationPolicy(String algorithm, PublicKey publicKey,
                                                SignatureProvider signatureProvider) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm must not be null");
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey must not be null");
        this.signatureProvider = Objects.requireNonNull(signatureProvider,
                "signatureProvider must not be null");
    }

    /**
     * 验证插件文件分离签名。
     *
     * @param context PF4J 发布上下文
     * @param file 本地制品路径
     * @throws IOException 读取制品或签名失败时抛出
     * @throws VerifyException 签名算法不可用或签名不匹配时抛出
     */
    @Override
    public void verify(FileVerifier.Context context, Path file) throws IOException, VerifyException {
        try {
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(publicKey);
            byte[] buffer = new byte[8192];
            try (InputStream input = Files.newInputStream(file)) {
                int length;
                while ((length = input.read(buffer)) >= 0) {
                    verifier.update(buffer, 0, length);
                }
            }
            if (!verifier.verify(signatureProvider.getSignature(context))) {
                throw new VerifyException("Plugin artifact signature verification failed");
            }
        } catch (GeneralSecurityException e) {
            throw new VerifyException(e, "Plugin artifact signature verification failed");
        }
    }
}
