package org.pf4j.core.extension.point.crypto;

/**
 * 数据源加解密扩展点接口
 *
 * <p>继承 {@link CryptoExtensionPoint}，用于数据源连接信息的加解密。
 * 插件可以实现此接口对数据库密码等敏感信息进行加解密处理。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface DatasourceCryptoExtensionPoint extends CryptoExtensionPoint {

}
