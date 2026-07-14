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

    String encrypt(String source, Object secretKey);

    String decrypt(String source, Object secretKey);

}
