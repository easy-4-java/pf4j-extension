package org.pf4j.spring.extension.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 将 PF4J 插件状态事件转换为 Spring 应用事件的发布器。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public final class SpringPluginEventPublisher implements PluginStateListener {

    /** 为防止异常链异常构造导致无限遍历而设置的最大层数。 */
    private static final int MAXIMUM_FAILURE_DEPTH = 32;

    /** Spring 应用事件发布器。 */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建插件事件发布器。
     *
     * @param eventPublisher Spring 应用事件发布器
     */
    public SpringPluginEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    /**
     * 发布不持有插件对象的完整 Spring 状态事件快照。
     *
     * @param event PF4J 插件状态事件
     */
    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        try {
            eventPublisher.publishEvent(createSpringEvent(event));
        } catch (RuntimeException ex) {
            log.error("Failed to publish Spring state event for PF4J plugin '{}'",
                    event.getPlugin().getPluginId(), ex);
        }
    }

    /**
     * 将 PF4J 状态事件转换为不持有插件运行对象的 Spring 事件快照。
     *
     * @param event PF4J 插件状态事件
     * @return 包含描述符、运行环境、扩展和失败信息的 Spring 事件
     */
    private SpringPluginStateChangedEvent createSpringEvent(PluginStateEvent event) {
        PluginWrapper plugin = event.getPlugin();
        PluginDescriptor descriptor = plugin.getDescriptor();
        FailureDetails failure = failureDetails(plugin.getFailedException());
        return new SpringPluginStateChangedEvent(event.getSource(), descriptorInfo(descriptor),
                Objects.isNull(plugin.getPluginPath()) ? null : plugin.getPluginPath().toAbsolutePath()
                        .normalize().toString(),
                Objects.isNull(plugin.getRuntimeMode()) ? null : plugin.getRuntimeMode().toString(),
                event.getSource().getSystemVersion(), event.getSource().getClass().getName(),
                Objects.isNull(plugin.getPluginClassLoader()) ? null
                        : plugin.getPluginClassLoader().getClass().getName(),
                extensionClassNames(event), event.getOldState(), event.getPluginState(), failure.className,
                failure.message, failure.rootClassName, failure.rootMessage, failure.causeChain);
    }

    /**
     * 将 PF4J 插件描述符转换为不持有源对象的事件快照。
     *
     * @param descriptor PF4J 插件描述符
     * @return Spring 插件描述符快照
     */
    private SpringPluginDescriptorInfo descriptorInfo(PluginDescriptor descriptor) {
        List<SpringPluginDependencyInfo> dependencies = new ArrayList<SpringPluginDependencyInfo>();
        for (PluginDependency dependency : descriptor.getDependencies()) {
            dependencies.add(new SpringPluginDependencyInfo(dependency.getPluginId(),
                    dependency.getPluginVersionSupport(), dependency.isOptional()));
        }
        return new SpringPluginDescriptorInfo(descriptor.getPluginId(), descriptor.getPluginDescription(),
                descriptor.getPluginClass(), descriptor.getVersion(), descriptor.getRequires(),
                descriptor.getProvider(), descriptor.getLicense(), dependencies);
    }

    /**
     * 安全读取插件声明的扩展类名。
     *
     * <p>插件进入失败或卸载状态时，扩展查找器可能已经无法读取索引。此时事件仍然发布，并将
     * 扩展类名降级为空列表。</p>
     *
     * @param event PF4J 插件状态事件
     * @return 稳定排序的扩展类名列表
     */
    private List<String> extensionClassNames(PluginStateEvent event) {
        try {
            List<String> classNames = new ArrayList<String>(
                    event.getSource().getExtensionClassNames(event.getPlugin().getPluginId()));
            Collections.sort(classNames);
            return classNames;
        } catch (RuntimeException ex) {
            log.debug("Failed to snapshot extension class names for PF4J plugin '{}'",
                    event.getPlugin().getPluginId(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * 将异常对象转换为不持有插件异常类型实例的字符串快照。
     *
     * @param failure 插件最近失败原因
     * @return 异常类型、消息、根异常和异常链摘要
     */
    private FailureDetails failureDetails(Throwable failure) {
        if (Objects.isNull(failure)) {
            return FailureDetails.empty();
        }
        List<String> causeChain = new ArrayList<String>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        Throwable current = failure;
        Throwable root = failure;
        int depth = 0;
        while (Objects.nonNull(current) && visited.add(current) && depth < MAXIMUM_FAILURE_DEPTH) {
            root = current;
            causeChain.add(failureSummary(current));
            current = current.getCause();
            depth++;
        }
        return new FailureDetails(failure.getClass().getName(), failure.getMessage(), root.getClass().getName(),
                root.getMessage(), causeChain);
    }

    /**
     * 创建单层异常摘要。
     *
     * @param failure 当前异常
     * @return “异常类型: 异常消息”形式的摘要
     */
    private String failureSummary(Throwable failure) {
        return Objects.isNull(failure.getMessage()) ? failure.getClass().getName()
                : failure.getClass().getName() + ": " + failure.getMessage();
    }

    /**
     * 插件失败信息的内部字符串快照。
     */
    private static final class FailureDetails {

        /** 最外层异常类型名称。 */
        private final String className;

        /** 最外层异常消息。 */
        private final String message;

        /** 根异常类型名称。 */
        private final String rootClassName;

        /** 根异常消息。 */
        private final String rootMessage;

        /** 从最外层异常到根异常排列的摘要列表。 */
        private final List<String> causeChain;

        /**
         * 创建失败信息快照。
         *
         * @param className 最外层异常类型名称
         * @param message 最外层异常消息
         * @param rootClassName 根异常类型名称
         * @param rootMessage 根异常消息
         * @param causeChain 异常链摘要
         */
        private FailureDetails(String className, String message, String rootClassName, String rootMessage,
                               List<String> causeChain) {
            this.className = className;
            this.message = message;
            this.rootClassName = rootClassName;
            this.rootMessage = rootMessage;
            this.causeChain = causeChain;
        }

        /**
         * 创建不包含失败原因的空快照。
         *
         * @return 空失败信息
         */
        private static FailureDetails empty() {
            return new FailureDetails(null, null, null, null, Collections.<String>emptyList());
        }
    }
}
