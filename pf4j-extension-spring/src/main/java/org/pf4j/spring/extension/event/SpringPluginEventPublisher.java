package org.pf4j.spring.extension.event;

import java.util.Objects;

import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 将 PF4J 插件状态事件转换为 Spring 应用事件的发布器。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final class SpringPluginEventPublisher implements PluginStateListener {

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
     * 发布不持有插件对象的 Spring 状态事件。
     *
     * @param event PF4J 插件状态事件
     */
    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        Throwable failure = event.getPlugin().getFailedException();
        eventPublisher.publishEvent(new SpringPluginStateChangedEvent(event.getSource(),
                event.getPlugin().getPluginId(), event.getPlugin().getDescriptor().getVersion(),
                event.getOldState(), event.getPluginState(),
                Objects.isNull(failure) ? null : failure.getClass().getName(),
                Objects.isNull(failure) ? null : failure.getMessage()));
    }
}
