package org.pf4j.core.extension.util;

import org.pf4j.core.extension.ExtensionResolver;
import org.pf4j.core.extension.exception.PluginInvokeException;
import org.pf4j.PluginManager;

import java.util.Optional;

/**
 * PF4J 工具类
 *
 * <p>提供获取扩展点实例的工具方法，支持按插件 ID 和扩展点 ID 精确查找。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class Pf4jKit {

    private Pf4jKit() {
    }

    public static <T> T getExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId,
                                          String extensionId) throws PluginInvokeException {
        return findExtensionPoint(pluginManager, type, pluginId, extensionId).orElse(null);
    }

    /**
     * 按插件和扩展 ID 查找扩展。
     */
    public static <T> Optional<T> findExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId,
                                                     String extensionId) {
        return new ExtensionResolver(pluginManager).find(type, pluginId, extensionId);
    }

    /**
     * 获取必需扩展，不存在时抛出扩展解析异常。
     */
    public static <T> T getRequiredExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId,
                                                   String extensionId) {
        return new ExtensionResolver(pluginManager).getRequired(type, pluginId, extensionId);
    }

    /**
     * 获取唯一实现或被 {@code @Primary} 标记的默认实现。
     */
    public static <T> T getPrimaryExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId) {
        return new ExtensionResolver(pluginManager).getPrimary(type, pluginId);
    }

}
