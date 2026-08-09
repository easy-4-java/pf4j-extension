package org.pf4j.core.extension.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK 动态代理实现
 *
 * <p>基于 JDK 的 {@link java.lang.reflect.Proxy} 实现动态代理，
 * 只能代理实现了接口的类。提供对目标方法的日志记录功能。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
/**
 * Utility for creating JDK dynamic proxies for extension interfaces to enable method interception.
 */
public class JdkProxy implements InvocationHandler {

    /**
     * 接收实际接口方法调用的目标对象。
     */
    private final Object target;

    /**
     * 创建绑定指定目标对象的 JDK 调用处理器。
     *
     * @param target 接收代理方法调用的目标对象
     */
    public JdkProxy(Object target) {
        this.target = target;
    }

    /**
     * 根据目标对象实现的全部接口创建 JDK 动态代理。
     *
     * @param targetObject 接收实际接口方法调用的目标对象
     * @return 实现目标对象全部接口的动态代理
     * @throws IllegalArgumentException 当目标类型没有可代理接口或代理类无法创建时抛出
     */
    public static Object getJDKProxy(Object targetObject) {
        return Proxy.newProxyInstance(targetObject.getClass().getClassLoader(), targetObject.getClass().getInterfaces(), new JdkProxy(targetObject));
    }

    /**
     * 实例化指定类型，并根据该类型实现的接口创建 JDK 动态代理。
     *
     * @param className 需要实例化的具体类型，必须具有可访问的无参构造器并实现至少一个接口
     * @param <T> 调用方期望的代理类型
     * @return 基于目标类型接口创建的动态代理
     * @throws IllegalStateException 当目标类型无法通过无参构造器实例化时抛出
     * @throws IllegalArgumentException 当目标类型没有可代理接口或代理类无法创建时抛出
     */
    public static <T> T getJDKProxy(Class<T> className) {
        JdkProxy target = new JdkProxy(instantiate(className));
        return (T) Proxy.newProxyInstance(className.getClassLoader(), className.getInterfaces(), target);
    }

    /**
     * 通过无参构造器实例化目标类型。
     *
     * @param clazz 待实例化的具体类型
     * @param <T> 目标对象类型
     * @return 新创建的目标对象
     * @throws IllegalStateException 当无参构造器不存在、不可访问或执行失败时抛出
     */
    private static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot instantiate class: " + clazz.getName() + " (requires public no-arg constructor)", e);
        }
    }

    /**
     * 将动态代理接收到的方法调用转发给目标对象。
     *
     * @param proxy 接收到调用的 JDK 动态代理对象
     * @param method 被调用的接口方法
     * @param args 方法实参数组；无参数方法调用时可以为 {@code null}
     * @return 目标方法返回值；目标方法返回 {@code void} 时为 {@code null}
     * @throws Throwable 当反射调用目标方法失败或目标方法抛出异常时继续抛出
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.debug("JDK动态代理，监听开始");
        Object result = method.invoke(target, args);
        log.debug("JDK动态代理，监听结束");
        return result;
    }

}
