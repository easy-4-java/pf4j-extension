package org.pf4j.core.extension.exception;

import org.pf4j.PluginRuntimeException;

/**
 * 插件调用异常
 *
 * <p>当插件或扩展点调用发生错误时抛出的运行时异常。
 * 包含插件 ID 和扩展点 ID，方便定位问题。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@SuppressWarnings("serial")
public class PluginInvokeException extends PluginRuntimeException {

    /**
     * 插件 ID
     */
    private String pluginId;

    /**
     * 扩展点 ID
     */
    private String extensionId;

    public PluginInvokeException(String pluginId, Throwable cause) {
        super("Plugin '" + pluginId + "' invoke error.", cause);
        this.pluginId = pluginId;
    }

    public PluginInvokeException(String pluginId, String extensionId, Throwable cause) {
        super("Plugin '" + pluginId + "' extensionId '" + extensionId + "' invoke error.", cause);
        this.pluginId = pluginId;
        this.extensionId = extensionId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getExtensionId() {
        return extensionId;
    }

}
