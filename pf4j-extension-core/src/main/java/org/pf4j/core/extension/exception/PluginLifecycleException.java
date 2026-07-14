package org.pf4j.core.extension.exception;

import org.pf4j.PluginRuntimeException;

/**
 * 插件加载或生命周期状态迁移失败时抛出的异常。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@SuppressWarnings("serial")
public class PluginLifecycleException extends PluginRuntimeException {

    public PluginLifecycleException(String message) {
        super(message);
    }

    public PluginLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

}
