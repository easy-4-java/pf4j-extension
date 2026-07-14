package org.pf4j.core.extension.point.crypto;

import org.pf4j.ExtensionPoint;

/**
 * 加解密扩展点接口
 *
 * <p>定义加解密功能的扩展点，插件可以通过实现此接口提供自定义的加解密算法。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface CryptoExtensionPoint extends ExtensionPoint {

    /**
     * 使用扩展实现定义的算法加密原始文本。
     *
     * @param source 待加密的原始文本
     * @param secretKey 算法所需的密钥对象，具体类型由扩展实现约定
     * @return 加密后的文本
     */
    String encrypt(String source, Object secretKey);

    /**
     * 使用扩展实现定义的算法解密密文。
     *
     * @param source 待解密的密文
     * @param secretKey 算法所需的密钥对象，具体类型由扩展实现约定
     * @return 解密后的原始文本
     */
    String decrypt(String source, Object secretKey);

}
