package org.pf4j.core.extension.invocation;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于 SLF4J 的扩展调用日志拦截器。
 *
 * <p>仅记录插件、扩展和方法标识，不输出参数和返回值，避免敏感信息或大对象进入日志。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Slf4j
public final class LoggingExtensionInterceptor implements ExtensionInterceptor {

    /**
     * 创建使用 SLF4J 记录扩展调用结果的拦截器。
     */
    public LoggingExtensionInterceptor() {
    }

    /**
     * 记录扩展调用开始、结束和失败日志。
     *
     * @param context 调用上下文
     * @param chain 后续调用链
     * @return 扩展方法返回值
     * @throws Throwable 当扩展调用失败时抛出
     */
    @Override
    public Object invoke(ExtensionInvocationContext context, ExtensionInvocationChain chain) throws Throwable {
        log.debug("Invoke PF4J plugin '{}', extension '{}', method '{}'", context.getPluginId(),
                context.getExtensionId(), context.getMethod().getName());
        try {
            Object result = chain.proceed(context);
            log.debug("Completed PF4J plugin '{}', extension '{}', method '{}'", context.getPluginId(),
                    context.getExtensionId(), context.getMethod().getName());
            return result;
        } catch (Throwable ex) {
            log.warn("Failed PF4J plugin '{}', extension '{}', method '{}'", context.getPluginId(),
                    context.getExtensionId(), context.getMethod().getName(), ex);
            throw ex;
        }
    }
}
