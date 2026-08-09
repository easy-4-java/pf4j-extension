package org.pf4j.core.extension.invocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.pf4j.core.extension.exception.PluginInvokeException;

/**
 * 支持责任链拦截的扩展调用器。
 *
 * <p>调用器以宿主扩展接口创建 JDK 动态代理，按注册顺序执行拦截器，并将最终异常统一包装为
 * {@link PluginInvokeException}。调用器本身不可变且线程安全。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Invoker that wraps extension instances with interceptor chains for proxied method calls.
 */
class ExtensionInvoker {

    /** 按调用顺序排列的不可修改拦截器列表。 */
    private final List<ExtensionInterceptor> interceptors;

    /**
     * 创建不包含拦截器的扩展调用器。
     */
    public ExtensionInvoker() {
        this(Collections.<ExtensionInterceptor>emptyList());
    }

    /**
     * 创建包含指定拦截器的扩展调用器。
     *
     * @param interceptors 按执行顺序排列的拦截器
     */
    public ExtensionInvoker(List<ExtensionInterceptor> interceptors) {
        Objects.requireNonNull(interceptors, "interceptors must not be null");
        this.interceptors = Collections.unmodifiableList(new ArrayList<ExtensionInterceptor>(interceptors));
    }

    /**
     * 为扩展实例创建类型安全调用代理。
     *
     * @param extensionPoint 宿主扩展点接口
     * @param target 扩展实例
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param <T> 扩展点类型
     * @return 实现宿主扩展点接口的调用代理
     * @throws IllegalArgumentException 当扩展点不是接口或目标不兼容时抛出
     */
    public <T> T createProxy(Class<T> extensionPoint, T target, String pluginId, String extensionId) {
        Objects.requireNonNull(extensionPoint, "extensionPoint must not be null");
        Objects.requireNonNull(target, "target must not be null");
        if (!extensionPoint.isInterface()) {
            throw new IllegalArgumentException("extensionPoint must be an interface");
        }
        if (!extensionPoint.isInstance(target)) {
            throw new IllegalArgumentException("target does not implement " + extensionPoint.getName());
        }
        Object proxy = Proxy.newProxyInstance(extensionPoint.getClassLoader(), new Class<?>[]{extensionPoint},
                (instance, method, arguments) -> invoke(pluginId, extensionId, target, method, arguments));
        return extensionPoint.cast(proxy);
    }

    /**
     * 通过拦截器链调用扩展方法。
     *
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param target 扩展实例
     * @param method 被调用方法
     * @param arguments 方法参数
     * @return 扩展方法返回值
     * @throws PluginInvokeException 当调用失败时抛出
     */
    public Object invoke(String pluginId, String extensionId, Object target, Method method, Object[] arguments) {
        ExtensionInvocationContext context = new ExtensionInvocationContext(pluginId, extensionId, target, method,
                arguments);
        try {
            return new DefaultChain(0).proceed(context);
        } catch (Throwable ex) {
            Throwable cause = unwrap(ex);
            if (cause instanceof PluginInvokeException) {
                throw (PluginInvokeException) cause;
            }
            throw new PluginInvokeException(pluginId, extensionId, cause);
        }
    }

    /**
     * 解包反射调用异常。
     *
     * @param failure 原始异常
     * @return 扩展实现抛出的真实异常
     */
    private Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof InvocationTargetException
                && Objects.nonNull(((InvocationTargetException) current).getTargetException())) {
            current = ((InvocationTargetException) current).getTargetException();
        }
        return current;
    }

    /**
     * 默认不可变索引调用链。
     */
    private final class DefaultChain implements ExtensionInvocationChain {

        /** 当前待执行拦截器索引。 */
        private final int index;

        /**
         * 创建指定索引的调用链节点。
         *
         * @param index 当前拦截器索引
         */
        private DefaultChain(int index) {
            this.index = index;
        }

        /**
         * 执行下一个拦截器或目标方法。
         *
         * @param context 当前调用上下文
         * @return 扩展方法返回值
         * @throws Throwable 当调用失败时抛出
         */
        @Override
        public Object proceed(ExtensionInvocationContext context) throws Throwable {
            if (index < interceptors.size()) {
                return interceptors.get(index).invoke(context, new DefaultChain(index + 1));
            }
            return context.getMethod().invoke(context.getTarget(), context.getArguments());
        }
    }
}
