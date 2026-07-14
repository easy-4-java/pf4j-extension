package org.pf4j.core.extension.exception;

import org.pf4j.PluginRuntimeException;

/**
 * 插件加载或生命周期状态迁移失败时抛出的异常。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public class PluginLifecycleException extends PluginRuntimeException {

    /**
     * 创建不包含底层原因的插件生命周期异常。
     *
     * @param message 插件加载、启动、停止或卸载失败的描述
     */
    public PluginLifecycleException(String message) {
        super(message);
    }

    /**
     * 创建包含底层原因的插件生命周期异常。
     *
     * @param message 插件生命周期操作失败的描述
     * @param cause 导致生命周期操作失败的原始异常
     */
    public PluginLifecycleException(String message, Throwable cause) {
        super(cause, message);
    }

}
