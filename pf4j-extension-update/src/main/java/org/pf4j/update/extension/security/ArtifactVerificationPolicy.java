package org.pf4j.update.extension.security;

import java.io.IOException;
import java.nio.file.Path;

import org.pf4j.update.FileVerifier;
import org.pf4j.update.VerifyException;

/**
 * 插件制品附加验证策略。
 *
 * <p>策略在 PF4J Update 基础校验之后执行，用于补充协议、大小、压缩结构、签名和企业准入规则。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public interface ArtifactVerificationPolicy {

    /**
     * 验证下载完成的插件制品。
     *
     * @param context PF4J 发布文件上下文
     * @param file 本地制品路径
     * @throws IOException 读取制品失败时抛出
     * @throws VerifyException 制品不满足策略时抛出
     */
    void verify(FileVerifier.Context context, Path file) throws IOException, VerifyException;
}
