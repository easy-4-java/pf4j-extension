package org.pf4j.core.extension.invocation;

/**
 * 扩展调用拦截器链。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public interface ExtensionInvocationChain {

    /**
     * 继续执行下一个拦截器或最终扩展方法。
     *
     * @param context 当前扩展调用上下文
     * @return 扩展方法返回值
     * @throws Throwable 当拦截器或扩展方法失败时抛出
     */
    Object proceed(ExtensionInvocationContext context) throws Throwable;
}
