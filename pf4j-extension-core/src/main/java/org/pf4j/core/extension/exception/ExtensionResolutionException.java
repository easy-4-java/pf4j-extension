package org.pf4j.core.extension.exception;

import org.pf4j.PluginRuntimeException;

/**
 * 扩展实现无法唯一解析时抛出的异常。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@SuppressWarnings("serial")
public class ExtensionResolutionException extends PluginRuntimeException {

    public ExtensionResolutionException(String message) {
        super(message);
    }

}
