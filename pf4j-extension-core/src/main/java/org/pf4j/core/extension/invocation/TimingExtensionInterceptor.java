package org.pf4j.core.extension.invocation;

import java.util.Objects;

/**
 * 扩展调用耗时拦截器。
 *
 * <p>无论调用成功或失败都会通知监听器；监听器异常不会覆盖扩展原始异常。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final class TimingExtensionInterceptor implements ExtensionInterceptor {

    /** 接收调用耗时和失败信息的监听器。 */
    private final ExtensionInvocationListener listener;

    /**
     * 创建耗时拦截器。
     *
     * @param listener 调用完成监听器
     */
    public TimingExtensionInterceptor(ExtensionInvocationListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
    }

    /**
     * 统计后续链路耗时并通知监听器。
     *
     * @param context 调用上下文
     * @param chain 后续调用链
     * @return 扩展方法返回值
     * @throws Throwable 当扩展调用失败时抛出
     */
    @Override
    public Object invoke(ExtensionInvocationContext context, ExtensionInvocationChain chain) throws Throwable {
        long startedAt = System.nanoTime();
        Throwable failure = null;
        try {
            return chain.proceed(context);
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            try {
                listener.onCompleted(context, System.nanoTime() - startedAt, failure);
            } catch (RuntimeException ignored) {
                // 观测监听器不能覆盖扩展调用结果。
            }
        }
    }
}
