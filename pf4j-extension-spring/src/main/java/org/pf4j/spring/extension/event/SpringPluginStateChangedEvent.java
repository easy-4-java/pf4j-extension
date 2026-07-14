package org.pf4j.spring.extension.event;

import lombok.Getter;
import org.pf4j.PluginState;
import org.springframework.context.ApplicationEvent;

/**
 * Spring 应用内的 PF4J 插件状态变化事件。
 *
 * <p>事件只保存宿主基础类型，不暴露 {@code PluginWrapper}，避免异步监听器阻止插件卸载。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
public final class SpringPluginStateChangedEvent extends ApplicationEvent {

    /** 插件 ID。 */
    private final String pluginId;

    /** 插件版本。 */
    private final String pluginVersion;

    /** 变化前状态。 */
    private final PluginState previousState;

    /** 变化后状态。 */
    private final PluginState currentState;

    /** 最近失败原因类型。 */
    private final String failureClassName;

    /** 最近失败原因消息。 */
    private final String failureMessage;

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
        super(source);
        this.pluginId = pluginId;
        this.pluginVersion = pluginVersion;
        this.previousState = previousState;
        this.currentState = currentState;
        this.failureClassName = failureClassName;
        this.failureMessage = failureMessage;
    }
}
