package org.pf4j.update.extension.security;

import java.io.IOException;

import org.pf4j.update.FileVerifier;

/**
 * 插件发布分离签名提供器。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public interface SignatureProvider {

    /**
     * 获取当前插件发布的分离签名字节。
     *
     * @param context PF4J 发布上下文
     * @return 分离签名字节
     * @throws IOException 读取签名失败时抛出
     */
    byte[] getSignature(FileVerifier.Context context) throws IOException;
}
