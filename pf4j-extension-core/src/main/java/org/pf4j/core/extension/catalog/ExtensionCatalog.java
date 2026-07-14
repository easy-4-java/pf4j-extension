package org.pf4j.core.extension.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.pf4j.PluginDependency;
import org.pf4j.PluginManager;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginWrapper;
import org.pf4j.core.extension.annotation.ExtensionMapping;
import org.pf4j.core.extension.annotation.PluginMapping;
import org.pf4j.core.extension.annotation.Primary;

/**
 * 基于 PF4J 运行状态构建的扩展与插件只读目录。
 *
 * <p>目录监听插件状态变化并延迟重建不可变快照，统一校验扩展 ID 与主要实现冲突。目录只保存
 * 类名而不保存扩展实例或 {@link Class}，避免在插件卸载后继续持有插件类加载器。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final class ExtensionCatalog implements PluginStateListener, AutoCloseable {

    /** PF4J 插件管理器。 */
    private final PluginManager pluginManager;

    /** 当前不可变目录快照。 */
    private volatile Snapshot snapshot = Snapshot.empty();

    /** 插件状态变化后是否需要重建目录。 */
    private volatile boolean dirty = true;

    /**
     * 创建扩展目录并注册插件状态监听器。
     *
     * @param pluginManager PF4J 插件管理器
     * @throws NullPointerException 当 {@code pluginManager} 为 {@code null} 时抛出
     */
    public ExtensionCatalog(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        this.pluginManager.addPluginStateListener(this);
    }

    /**
     * 获取全部插件元数据。
     *
     * @return 以插件 ID 为键的不可修改元数据映射
     */
    public Map<String, PluginMetadata> getPlugins() {
        return current().plugins;
    }

    /**
     * 获取全部扩展元数据。
     *
     * @return 按插件、扩展点和排序值稳定排列的不可修改列表
     */
    public List<ExtensionMetadata> getExtensions() {
        return current().extensions;
    }

    /**
     * 获取指定扩展点的全部扩展元数据。
     *
     * @param extensionPoint 扩展点类型
     * @return 匹配扩展的不可修改列表
     */
    public List<ExtensionMetadata> getExtensions(Class<?> extensionPoint) {
        Objects.requireNonNull(extensionPoint, "extensionPoint must not be null");
        List<ExtensionMetadata> metadata = current().extensionsByPoint.get(extensionPoint.getName());
        return Objects.isNull(metadata) ? Collections.<ExtensionMetadata>emptyList() : metadata;
    }

    /**
     * 按插件、扩展点和扩展 ID 查询元数据。
     *
     * @param extensionPoint 扩展点类型
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @return 匹配元数据；不存在时返回空容器
     */
    public Optional<ExtensionMetadata> find(Class<?> extensionPoint, String pluginId, String extensionId) {
        Objects.requireNonNull(extensionPoint, "extensionPoint must not be null");
        if (StringUtils.isBlank(pluginId) || StringUtils.isBlank(extensionId)) {
            throw new IllegalArgumentException("pluginId and extensionId must not be blank");
        }
        return Optional.ofNullable(current().extensionsByKey.get(
                key(pluginId, extensionPoint.getName(), extensionId)));
    }

    /**
     * 立即重建并校验目录快照。
     */
    public synchronized void refresh() {
        Map<String, PluginMetadata> plugins = new LinkedHashMap<String, PluginMetadata>();
        List<ExtensionMetadata> extensions = new ArrayList<ExtensionMetadata>();
        Map<String, ExtensionMetadata> extensionsByKey = new HashMap<String, ExtensionMetadata>();
        Map<String, Integer> primaryCounts = new HashMap<String, Integer>();

        for (PluginWrapper plugin : new ArrayList<PluginWrapper>(pluginManager.getStartedPlugins())) {
            plugins.put(plugin.getPluginId(), pluginMetadata(plugin));
            for (Class<?> extensionClass : pluginManager.getExtensionClasses(plugin.getPluginId())) {
                for (Class<?> extensionPoint : findExtensionPoints(extensionClass)) {
                    ExtensionMetadata metadata = extensionMetadata(plugin.getPluginId(), extensionPoint,
                            extensionClass);
                    String metadataKey = key(plugin.getPluginId(), extensionPoint.getName(), metadata.getExtensionId());
                    if (extensionsByKey.containsKey(metadataKey)) {
                        throw new ExtensionConflictException("Duplicate extension id '" + metadata.getExtensionId()
                                + "' for plugin '" + plugin.getPluginId() + "' and extension point '"
                                + extensionPoint.getName() + "'");
                    }
                    extensionsByKey.put(metadataKey, metadata);
                    extensions.add(metadata);
                    if (metadata.isPrimary()) {
                        String primaryKey = plugin.getPluginId() + "|" + extensionPoint.getName();
                        int count = primaryCounts.containsKey(primaryKey) ? primaryCounts.get(primaryKey) + 1 : 1;
                        primaryCounts.put(primaryKey, count);
                        if (count > 1) {
                            throw new ExtensionConflictException("Multiple primary extensions for plugin '"
                                    + plugin.getPluginId() + "' and extension point '" + extensionPoint.getName()
                                    + "'");
                        }
                    }
                }
            }
        }

        Collections.sort(extensions, Comparator.comparing(ExtensionMetadata::getPluginId)
                .thenComparing(ExtensionMetadata::getExtensionPointClassName)
                .thenComparingInt(ExtensionMetadata::getOrdinal)
                .thenComparing(ExtensionMetadata::getExtensionId));
        snapshot = Snapshot.of(plugins, extensions, extensionsByKey);
        dirty = false;
    }

    /**
     * 标记目录为需要重建。
     *
     * @param event PF4J 插件状态事件
     */
    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        dirty = true;
    }

    /**
     * 注销插件状态监听器并清空目录快照。
     */
    @Override
    public void close() {
        pluginManager.removePluginStateListener(this);
        snapshot = Snapshot.empty();
        dirty = true;
    }

    /**
     * 获取最新目录快照。
     *
     * @return 当前不可变快照
     */
    private Snapshot current() {
        if (dirty) {
            refresh();
        }
        return snapshot;
    }

    /**
     * 从 PF4J 包装器创建插件元数据。
     *
     * @param plugin 插件包装器
     * @return 插件元数据
     */
    private PluginMetadata pluginMetadata(PluginWrapper plugin) {
        String title = plugin.getPluginId();
        String detail = plugin.getDescriptor().getPluginDescription();
        try {
            Class<?> pluginClass = plugin.getPluginClassLoader().loadClass(plugin.getDescriptor().getPluginClass());
            PluginMapping mapping = pluginClass.getAnnotation(PluginMapping.class);
            if (Objects.nonNull(mapping)) {
                title = StringUtils.defaultIfBlank(mapping.title(), title);
                detail = StringUtils.defaultIfBlank(mapping.detail(), detail);
            }
        } catch (ClassNotFoundException ignored) {
            // 描述符验证和插件启动会提供更完整的类型加载错误，此处保留描述符元数据。
        }
        List<String> dependencies = new ArrayList<String>();
        for (PluginDependency dependency : plugin.getDescriptor().getDependencies()) {
            dependencies.add(dependency.getPluginId() + "@" + dependency.getPluginVersionSupport()
                    + (dependency.isOptional() ? "?" : ""));
        }
        return new PluginMetadata(plugin.getPluginId(), plugin.getDescriptor().getVersion(),
                plugin.getDescriptor().getProvider(), title, detail, plugin.getPluginState(), plugin.getPluginPath(),
                dependencies);
    }

    /**
     * 创建单个扩展元数据。
     *
     * @param pluginId 插件 ID
     * @param extensionPoint 扩展点类型
     * @param extensionClass 扩展实现类型
     * @return 扩展元数据
     */
    private ExtensionMetadata extensionMetadata(String pluginId, Class<?> extensionPoint, Class<?> extensionClass) {
        ExtensionMapping mapping = extensionClass.getAnnotation(ExtensionMapping.class);
        Extension extension = extensionClass.getAnnotation(Extension.class);
        String extensionId = Objects.isNull(mapping) ? extensionClass.getName()
                : StringUtils.defaultIfBlank(mapping.id(), extensionClass.getName());
        String title = Objects.isNull(mapping) ? extensionClass.getSimpleName()
                : StringUtils.defaultIfBlank(mapping.title(), extensionClass.getSimpleName());
        String version = Objects.isNull(mapping) ? "1.0.0" : mapping.ver();
        String description = Objects.isNull(mapping) ? "" : mapping.desc();
        int ordinal = Objects.isNull(extension) ? 0 : extension.ordinal();
        return new ExtensionMetadata(pluginId, extensionId, title, version, description, extensionPoint.getName(),
                extensionClass.getName(), ordinal, extensionClass.isAnnotationPresent(Primary.class));
    }

    /**
     * 查找实现类型声明的全部 PF4J 扩展点。
     *
     * @param extensionClass 扩展实现类型
     * @return 扩展点类型集合
     */
    private Set<Class<?>> findExtensionPoints(Class<?> extensionClass) {
        Set<Class<?>> extensionPoints = new LinkedHashSet<Class<?>>();
        collectExtensionPoints(extensionClass, extensionPoints);
        return extensionPoints;
    }

    /**
     * 递归收集接口和父类上的扩展点。
     *
     * @param type 当前检查类型
     * @param extensionPoints 扩展点结果集合
     */
    private void collectExtensionPoints(Class<?> type, Set<Class<?>> extensionPoints) {
        if (Objects.isNull(type) || Object.class.equals(type)) {
            return;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            if (!ExtensionPoint.class.equals(interfaceType)
                    && ExtensionPoint.class.isAssignableFrom(interfaceType)) {
                extensionPoints.add(interfaceType);
            }
            collectExtensionPoints(interfaceType, extensionPoints);
        }
        collectExtensionPoints(type.getSuperclass(), extensionPoints);
    }

    /**
     * 创建扩展唯一键。
     *
     * @param pluginId 插件 ID
     * @param extensionPoint 扩展点类型名称
     * @param extensionId 扩展 ID
     * @return 扩展唯一键
     */
    private static String key(String pluginId, String extensionPoint, String extensionId) {
        return pluginId + "|" + extensionPoint + "|" + extensionId;
    }

    /**
     * 不可变扩展目录快照。
     */
    private static final class Snapshot {

        /** 插件元数据映射。 */
        private final Map<String, PluginMetadata> plugins;

        /** 全部扩展元数据。 */
        private final List<ExtensionMetadata> extensions;

        /** 扩展唯一键索引。 */
        private final Map<String, ExtensionMetadata> extensionsByKey;

        /** 按扩展点类型索引的元数据。 */
        private final Map<String, List<ExtensionMetadata>> extensionsByPoint;

        /**
         * 创建目录快照。
         *
         * @param plugins 插件元数据映射
         * @param extensions 扩展元数据列表
         * @param extensionsByKey 扩展唯一键索引
         * @param extensionsByPoint 扩展点索引
         */
        private Snapshot(Map<String, PluginMetadata> plugins, List<ExtensionMetadata> extensions,
                         Map<String, ExtensionMetadata> extensionsByKey,
                         Map<String, List<ExtensionMetadata>> extensionsByPoint) {
            this.plugins = plugins;
            this.extensions = extensions;
            this.extensionsByKey = extensionsByKey;
            this.extensionsByPoint = extensionsByPoint;
        }

        /**
         * 创建空快照。
         *
         * @return 空目录快照
         */
        private static Snapshot empty() {
            return new Snapshot(Collections.<String, PluginMetadata>emptyMap(),
                    Collections.<ExtensionMetadata>emptyList(),
                    Collections.<String, ExtensionMetadata>emptyMap(),
                    Collections.<String, List<ExtensionMetadata>>emptyMap());
        }

        /**
         * 从可变构建结果创建不可变快照。
         *
         * @param plugins 插件元数据映射
         * @param extensions 扩展元数据列表
         * @param extensionsByKey 扩展唯一键索引
         * @return 不可变目录快照
         */
        private static Snapshot of(Map<String, PluginMetadata> plugins, List<ExtensionMetadata> extensions,
                                   Map<String, ExtensionMetadata> extensionsByKey) {
            Map<String, List<ExtensionMetadata>> byPoint = new LinkedHashMap<String, List<ExtensionMetadata>>();
            for (ExtensionMetadata metadata : extensions) {
                List<ExtensionMetadata> pointExtensions = byPoint.get(metadata.getExtensionPointClassName());
                if (Objects.isNull(pointExtensions)) {
                    pointExtensions = new ArrayList<ExtensionMetadata>();
                    byPoint.put(metadata.getExtensionPointClassName(), pointExtensions);
                }
                pointExtensions.add(metadata);
            }
            for (Map.Entry<String, List<ExtensionMetadata>> entry : byPoint.entrySet()) {
                entry.setValue(Collections.unmodifiableList(entry.getValue()));
            }
            return new Snapshot(Collections.unmodifiableMap(new LinkedHashMap<String, PluginMetadata>(plugins)),
                    Collections.unmodifiableList(new ArrayList<ExtensionMetadata>(extensions)),
                    Collections.unmodifiableMap(new HashMap<String, ExtensionMetadata>(extensionsByKey)),
                    Collections.unmodifiableMap(byPoint));
        }
    }
}
