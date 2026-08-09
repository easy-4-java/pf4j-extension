package org.pf4j.core.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.pf4j.core.extension.annotation.ExtensionMapping;
import org.pf4j.core.extension.annotation.Primary;
import org.pf4j.core.extension.exception.ExtensionResolutionException;

/**
 * PF4J 扩展实现的类型安全解析器。
 *
 * <p>支持按插件、扩展 ID 和 {@link Primary} 默认实现解析扩展，避免业务侧重复编写遍历与异常处理逻辑。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Interface for resolving extension instances from the plugin framework by extension type.
 */
class ExtensionResolver {

    /**
     * PF4J 插件管理器，用于查询已发现并实例化的扩展实现。
     */
    private final PluginManager pluginManager;

    /**
     * 创建扩展解析器。
     *
     * @param pluginManager PF4J 插件管理器，不允许为 {@code null}
     * @throws NullPointerException 当 {@code pluginManager} 为 {@code null} 时抛出
     */
    public ExtensionResolver(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
    }

    /**
     * 获取指定类型的全部扩展。
     *
     * @param type 扩展类型
     * @param <T> 扩展类型
     * @return 指定类型的不可修改扩展列表；没有匹配实现时返回空列表
     * @throws NullPointerException 当 {@code type} 为 {@code null} 时抛出
     */
    public <T> List<T> getExtensions(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return immutableCopy(pluginManager.getExtensions(type));
    }

    /**
     * 获取指定插件中的全部扩展。
     *
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param <T> 扩展类型
     * @return 指定插件中匹配类型的不可修改扩展列表；没有匹配实现时返回空列表
     * @throws NullPointerException 当 {@code type} 为 {@code null} 时抛出
     * @throws IllegalArgumentException 当 {@code pluginId} 为空时抛出
     */
    public <T> List<T> getExtensions(Class<T> type, String pluginId) {
        Objects.requireNonNull(type, "type must not be null");
        if (StringUtils.isBlank(pluginId)) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        return immutableCopy(pluginManager.getExtensions(type, pluginId));
    }

    /**
     * 按插件 ID 和 {@link ExtensionMapping#id()} 查找扩展。
     *
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param <T> 扩展类型
     * @return 包含匹配扩展的 {@link Optional}；未匹配到扩展时返回空值容器
     * @throws NullPointerException 当 {@code type} 为 {@code null} 时抛出
     * @throws IllegalArgumentException 当 {@code pluginId} 或 {@code extensionId} 为空时抛出
     */
    public <T> Optional<T> find(Class<T> type, String pluginId, String extensionId) {
        if (StringUtils.isBlank(extensionId)) {
            throw new IllegalArgumentException("extensionId must not be blank");
        }
        for (T extension : getExtensions(type, pluginId)) {
            ExtensionMapping mapping = extension.getClass().getAnnotation(ExtensionMapping.class);
            if (Objects.nonNull(mapping) && extensionId.equals(mapping.id())) {
                return Optional.of(extension);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取指定插件中具有目标扩展 ID 的必需扩展。
     *
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param <T> 扩展类型
     * @return 唯一匹配的扩展实例
     * @throws ExtensionResolutionException 未找到扩展时抛出
     * @throws NullPointerException 当 {@code type} 为 {@code null} 时抛出
     * @throws IllegalArgumentException 当 {@code pluginId} 或 {@code extensionId} 为空时抛出
     */
    public <T> T getRequired(Class<T> type, String pluginId, String extensionId) {
        return find(type, pluginId, extensionId).orElseThrow(() -> new ExtensionResolutionException(
                "No extension of type '" + type.getName() + "' found for plugin '" + pluginId
                        + "' and extension id '" + extensionId + "'"));
    }

    /**
     * 获取唯一实现或 {@link Primary} 默认实现。
     *
     * <p>只有一个实现时直接返回该实现；存在多个实现时，要求其中恰好一个实现标记了
     * {@link Primary}，否则无法确定默认实现。</p>
     *
     * @param type 扩展类型
     * @param pluginId 插件 ID
     * @param <T> 扩展类型
     * @return 唯一实现或标记为主要实现的扩展实例
     * @throws ExtensionResolutionException 不存在实现或无法唯一确定默认实现时抛出
     * @throws NullPointerException 当 {@code type} 为 {@code null} 时抛出
     * @throws IllegalArgumentException 当 {@code pluginId} 为空时抛出
     */
    public <T> T getPrimary(Class<T> type, String pluginId) {
        List<T> extensions = getExtensions(type, pluginId);
        if (extensions.isEmpty()) {
            throw new ExtensionResolutionException(
                    "No extension of type '" + type.getName() + "' found for plugin '" + pluginId + "'");
        }
        if (extensions.size() == 1) {
            return extensions.get(0);
        }
        List<T> primaryExtensions = new ArrayList<T>();
        for (T extension : extensions) {
            if (Objects.nonNull(extension.getClass().getAnnotation(Primary.class))) {
                primaryExtensions.add(extension);
            }
        }
        if (primaryExtensions.size() == 1) {
            return primaryExtensions.get(0);
        }
        throw new ExtensionResolutionException("Expected exactly one primary extension of type '" + type.getName()
                + "' for plugin '" + pluginId + "', but found " + primaryExtensions.size());
    }

    /**
     * 将 PF4J 返回的扩展集合转换为不可修改视图。
     *
     * @param extensions PF4J 返回的扩展列表，可以为 {@code null}
     * @param <T> 扩展类型
     * @return 不可修改的扩展列表；源列表为空时返回共享空列表
     */
    private static <T> List<T> immutableCopy(List<T> extensions) {
        if (Objects.isNull(extensions) || extensions.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(extensions);
    }

}
