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

    /**
     * 阻止工具类被实例化。
     */
    private Pf4jKit() {
    }

    /**
     * 按插件 ID 和扩展 ID 获取扩展，保留返回 {@code null} 的兼容行为。
     *
     * @param pluginManager PF4J 插件管理器
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param <T> 扩展类型
     * @return 匹配的扩展实例；不存在时返回 {@code null}
     * @throws PluginInvokeException 插件扩展调用失败时抛出
     * @throws IllegalArgumentException 当插件 ID 或扩展 ID 为空时抛出
     */
    public static <T> T getExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId,
                                          String extensionId) throws PluginInvokeException {
        return findExtensionPoint(pluginManager, type, pluginId, extensionId).orElse(null);
    }

    /**
     * 按插件和扩展 ID 查找扩展。
     *
     * @param pluginManager PF4J 插件管理器
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param <T> 扩展类型
     * @return 包含匹配扩展的 {@link Optional}；不存在时返回空值容器
     * @throws IllegalArgumentException 当插件 ID 或扩展 ID 为空时抛出
     */
    public static <T> Optional<T> findExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId,
                                                     String extensionId) {
        return new ExtensionResolver(pluginManager).find(type, pluginId, extensionId);
    }

    /**
     * 获取必需扩展，不存在时抛出扩展解析异常。
     *
     * @param pluginManager PF4J 插件管理器
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param <T> 扩展类型
     * @return 匹配的扩展实例
     * @throws org.pf4j.core.extension.exception.ExtensionResolutionException 当扩展不存在时抛出
     */
    public static <T> T getRequiredExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId,
                                                   String extensionId) {
        return new ExtensionResolver(pluginManager).getRequired(type, pluginId, extensionId);
    }

    /**
     * 获取唯一实现或被 {@code @Primary} 标记的默认实现。
     *
     * @param pluginManager PF4J 插件管理器
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param <T> 扩展类型
     * @return 唯一实现或标记为主要实现的扩展实例
     * @throws org.pf4j.core.extension.exception.ExtensionResolutionException 当默认实现无法唯一确定时抛出
     */
    public static <T> T getPrimaryExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId) {
        return new ExtensionResolver(pluginManager).getPrimary(type, pluginId);
    }

}
