package org.pf4j.core.extension.invocation;

/**
 * 扩展调用完成监听器。
 *
 * <p>用于接入指标、追踪或审计系统，监听器不应持久保存扩展实例或调用上下文。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public interface ExtensionInvocationListener {

    /**
     * 处理一次完成的扩展调用。
     *
     * @param context 调用上下文
     * @param durationNanos 调用耗时，单位为纳秒
     * @param failure 失败原因；成功时为空
     */
    void onCompleted(ExtensionInvocationContext context, long durationNanos, Throwable failure);
}
