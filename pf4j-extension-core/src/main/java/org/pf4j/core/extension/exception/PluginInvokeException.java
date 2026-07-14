package org.pf4j.core.extension.exception;

import lombok.Getter;
import org.pf4j.PluginRuntimeException;

/**
 * 插件调用异常
 *
 * <p>当插件或扩展点调用发生错误时抛出的运行时异常。
 * 包含插件 ID 和扩展点 ID，方便定位问题。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class PluginInvokeException extends PluginRuntimeException {

    /**
     * 发生调用异常的插件 ID。
     */
    @Getter
    private String pluginId;

    /**
     * 发生调用异常的扩展 ID；异常不针对具体扩展时可以为空。
     */
    @Getter
    private String extensionId;

    /**
     * 创建插件级调用异常。
     *
     * @param pluginId 发生异常的插件 ID
     * @param cause 导致插件调用失败的原始异常
     */
    public PluginInvokeException(String pluginId, Throwable cause) {
        super(cause, "Plugin '{}' invoke error.", pluginId);
        this.pluginId = pluginId;
    }

    /**
     * 创建扩展级调用异常。
     *
     * @param pluginId 发生异常的插件 ID
     * @param extensionId 发生异常的扩展 ID
     * @param cause 导致扩展调用失败的原始异常
     */
    public PluginInvokeException(String pluginId, String extensionId, Throwable cause) {
        super(cause, "Plugin '{}' extensionId '{}' invoke error.", pluginId, extensionId);
        this.pluginId = pluginId;
        this.extensionId = extensionId;
    }

}
