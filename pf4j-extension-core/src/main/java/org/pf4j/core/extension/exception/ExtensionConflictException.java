package org.pf4j.core.extension.exception;

import org.pf4j.PluginRuntimeException;

/**
 * 扩展目录冲突异常。
 *
 * <p>同一插件、扩展点中出现重复扩展 ID 或多个主要实现时抛出。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public class ExtensionConflictException extends PluginRuntimeException {

    /**
     * 创建扩展目录冲突异常。
     *
     * @param message 冲突详情
     */
    public ExtensionConflictException(String message) {
        super(message);
    }
}
