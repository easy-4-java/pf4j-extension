package org.pf4j.core.extension.lifecycle;

import lombok.Getter;
import org.pf4j.PluginState;

/**
 * 插件生命周期操作结果。
 *
 * <p>对象记录插件、操作、执行前后状态、耗时及失败原因，可直接用于审计日志、指标和
 * Spring 事件发布。该对象不可变且不持有插件实例，避免阻止插件类加载器回收。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
public final class PluginOperationResult {

    /** 插件 ID；加载失败且尚未得到 ID 时可以为空。 */
    private final String pluginId;

    /** 当前生命周期操作。 */
    private final PluginOperation operation;

    /** 操作前状态；加载前可以为空。 */
    private final PluginState previousState;

    /** 操作后状态；加载失败时可以为空。 */
    private final PluginState currentState;

    /** 操作是否成功。 */
    private final boolean success;

    /** 操作耗时，单位为纳秒。 */
    private final long durationNanos;

    /** 失败原因；成功时为空。 */
    private final Throwable failure;

    /**
     * 创建生命周期操作结果。
     *
     * @param pluginId 插件 ID
     * @param operation 生命周期操作
     * @param previousState 操作前状态
     * @param currentState 操作后状态
     * @param success 是否成功
     * @param durationNanos 操作耗时，单位为纳秒
     * @param failure 失败原因
     */
    public PluginOperationResult(String pluginId, PluginOperation operation, PluginState previousState,
                                 PluginState currentState, boolean success, long durationNanos, Throwable failure) {
        this.pluginId = pluginId;
        this.operation = operation;
        this.previousState = previousState;
        this.currentState = currentState;
        this.success = success;
        this.durationNanos = durationNanos;
        this.failure = failure;
    }
}
