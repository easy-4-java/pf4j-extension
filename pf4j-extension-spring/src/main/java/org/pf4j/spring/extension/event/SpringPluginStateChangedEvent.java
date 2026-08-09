package org.pf4j.spring.extension.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import org.pf4j.PluginState;
import org.springframework.context.ApplicationEvent;

/**
 * Spring 应用内的 PF4J 插件状态变化事件。
 *
 * <p>事件包含描述符、运行环境、扩展类、状态转换和异常链快照。所有内容均为宿主基础类型、
 * 枚举或不可修改集合，不暴露 {@code PluginWrapper}、插件实例、插件类型或插件类加载器，避免
 * 异步监听器阻止插件卸载。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
/**
 * Spring application event published when a plugin's state changes (started, stopped, etc.).
 */
public final class SpringPluginStateChangedEvent extends ApplicationEvent {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 当前事件的唯一标识。 */
    private final String eventId;

    /** 当前事件发生时间。 */
    private final Instant occurredAt;

    /** 插件 ID。 */
    private final String pluginId;

    /** 插件版本。 */
    private final String pluginVersion;

    /** 完整的插件描述符快照。 */
    private final SpringPluginDescriptorInfo pluginDescriptor;

    /** 插件制品或展开目录的规范化路径。 */
    private final String pluginPath;

    /** 插件运行模式，例如 {@code development} 或 {@code deployment}。 */
    private final String runtimeMode;

    /** PF4J 插件管理器配置的宿主系统版本。 */
    private final String systemVersion;

    /** 产生状态变化的插件管理器类型名称。 */
    private final String pluginManagerClassName;

    /** 插件类加载器类型名称，不持有实际类加载器。 */
    private final String pluginClassLoaderClassName;

    /** 当前插件声明的扩展实现类名不可修改列表。 */
    private final List<String> extensionClassNames;

    /** 变化前状态。 */
    private final PluginState previousState;

    /** 变化后状态。 */
    private final PluginState currentState;

    /** 最近失败原因类型。 */
    private final String failureClassName;

    /** 最近失败原因消息。 */
    private final String failureMessage;

    /** 异常链最深层根异常类型。 */
    private final String rootFailureClassName;

    /** 异常链最深层根异常消息。 */
    private final String rootFailureMessage;

    /** 从外层异常到根异常排列的不可修改异常摘要列表。 */
    private final List<String> failureCauseChain;

    /**
     * 创建 Spring 插件状态事件。
     *
     * @param source 事件来源
     * @param pluginId 插件 ID
     * @param pluginVersion 插件版本
     * @param previousState 变化前状态
     * @param currentState 变化后状态
     * @param failureClassName 失败原因类型
     * @param failureMessage 失败原因消息
     */
    public SpringPluginStateChangedEvent(Object source, String pluginId, String pluginVersion,
                                         PluginState previousState, PluginState currentState,
                                         String failureClassName, String failureMessage) {
        this(source, new SpringPluginDescriptorInfo(pluginId, null, null, pluginVersion, null, null, null,
                        Collections.<SpringPluginDependencyInfo>emptyList()),
                null, null, null, null, null, Collections.<String>emptyList(), previousState, currentState,
                failureClassName, failureMessage, failureClassName, failureMessage,
                Objects.isNull(failureClassName) ? Collections.<String>emptyList()
                        : Collections.singletonList(failureSummary(failureClassName, failureMessage)));
    }

    /**
     * 创建包含插件描述符、运行环境、扩展和异常链快照的 Spring 插件状态事件。
     *
     * @param source 事件来源
     * @param pluginDescriptor 插件描述符快照
     * @param pluginPath 插件制品或展开目录路径
     * @param runtimeMode 插件运行模式
     * @param systemVersion 宿主系统版本
     * @param pluginManagerClassName 插件管理器类型名称
     * @param pluginClassLoaderClassName 插件类加载器类型名称
     * @param extensionClassNames 扩展实现类名集合
     * @param previousState 变化前状态
     * @param currentState 变化后状态
     * @param failureClassName 最外层失败原因类型
     * @param failureMessage 最外层失败原因消息
     * @param rootFailureClassName 根失败原因类型
     * @param rootFailureMessage 根失败原因消息
     * @param failureCauseChain 异常链摘要集合
     */
    public SpringPluginStateChangedEvent(Object source, SpringPluginDescriptorInfo pluginDescriptor,
                                         String pluginPath, String runtimeMode, String systemVersion,
                                         String pluginManagerClassName, String pluginClassLoaderClassName,
                                         List<String> extensionClassNames, PluginState previousState,
                                         PluginState currentState, String failureClassName, String failureMessage,
                                         String rootFailureClassName, String rootFailureMessage,
                                         List<String> failureCauseChain) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.ofEpochMilli(getTimestamp());
        this.pluginDescriptor = Objects.requireNonNull(pluginDescriptor, "pluginDescriptor must not be null");
        this.pluginId = pluginDescriptor.getPluginId();
        this.pluginVersion = pluginDescriptor.getVersion();
        this.pluginPath = pluginPath;
        this.runtimeMode = runtimeMode;
        this.systemVersion = systemVersion;
        this.pluginManagerClassName = pluginManagerClassName;
        this.pluginClassLoaderClassName = pluginClassLoaderClassName;
        List<String> extensionSource = Objects.isNull(extensionClassNames)
                ? Collections.<String>emptyList() : extensionClassNames;
        this.extensionClassNames = Collections.unmodifiableList(new ArrayList<String>(extensionSource));
        this.previousState = previousState;
        this.currentState = currentState;
        this.failureClassName = failureClassName;
        this.failureMessage = failureMessage;
        this.rootFailureClassName = rootFailureClassName;
        this.rootFailureMessage = rootFailureMessage;
        List<String> failureSource = Objects.isNull(failureCauseChain)
                ? Collections.<String>emptyList() : failureCauseChain;
        this.failureCauseChain = Collections.unmodifiableList(new ArrayList<String>(failureSource));
    }

    /**
     * 获取便于日志、指标和审计展示的状态转换文本。
     *
     * @return 格式为“旧状态 -&gt; 新状态”的转换文本
     */
    public String getStateTransition() {
        return stateName(previousState) + " -> " + stateName(currentState);
    }

    /**
     * 判断本次事件是否表示插件失败。
     *
     * @return 当前状态为失败或包含失败原因时返回 {@code true}
     */
    public boolean isFailure() {
        return PluginState.FAILED.equals(currentState) || Objects.nonNull(failureClassName);
    }

    /**
     * 将可空插件状态转换为稳定文本。
     *
     * @param state 插件状态
     * @return 状态名称；状态为空时返回 {@code UNKNOWN}
     */
    private String stateName(PluginState state) {
        return Objects.isNull(state) ? "UNKNOWN" : state.name();
    }

    /**
     * 创建单层异常摘要。
     *
     * @param className 异常类型名称
     * @param message 异常消息
     * @return 不包含插件异常对象的异常摘要
     */
    private static String failureSummary(String className, String message) {
        return Objects.isNull(message) ? className : className + ": " + message;
    }
}
