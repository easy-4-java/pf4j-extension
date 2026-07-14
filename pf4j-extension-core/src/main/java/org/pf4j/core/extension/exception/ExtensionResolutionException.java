package org.pf4j.core.extension.exception;

import org.pf4j.PluginRuntimeException;

/**
 * 扩展实现无法唯一解析时抛出的异常。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public class ExtensionResolutionException extends PluginRuntimeException {

    /**
     * 创建扩展解析异常。
     *
     * @param message 描述扩展缺失、候选冲突或主要实现不唯一的错误信息
     */
    public ExtensionResolutionException(String message) {
        super(message);
    }

}
