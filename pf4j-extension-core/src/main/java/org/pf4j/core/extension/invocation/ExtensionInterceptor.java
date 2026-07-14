package org.pf4j.core.extension.invocation;

/**
 * 扩展调用拦截器。
 *
 * <p>实现方可以在调用前后添加日志、指标、追踪、超时或容错逻辑。拦截器必须调用
 * {@link ExtensionInvocationChain#proceed(ExtensionInvocationContext)} 才会继续执行后续链路。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public interface ExtensionInterceptor {

    /**
     * 拦截扩展方法调用。
     *
     * @param context 当前调用上下文
     * @param chain 后续调用链
     * @return 扩展方法返回值
     * @throws Throwable 当拦截器或扩展方法失败时抛出
     */
    Object invoke(ExtensionInvocationContext context, ExtensionInvocationChain chain) throws Throwable;
}
