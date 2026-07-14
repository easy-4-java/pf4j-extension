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
public class JdkProxy implements InvocationHandler {

    /**
     * 需要代理的目标对象
     */
    private Object target;

    public JdkProxy(Object target) {
        this.target = target;
    }

    //定义获取代理对象方法
    public static Object getJDKProxy(Object targetObject) {
        //JDK动态代理只能针对实现了接口的类进行代理，newProxyInstance 函数所需参数就可看出
        return Proxy.newProxyInstance(targetObject.getClass().getClassLoader(), targetObject.getClass().getInterfaces(), new JdkProxy(targetObject));
    }

    // 定义获取代理对象方法
    public static <T> T getJDKProxy(Class<T> className) {
        JdkProxy target = new JdkProxy(instantiate(className));
        //JDK动态代理只能针对实现了接口的类进行代理，newProxyInstance 函数所需参数就可看出
        return (T) Proxy.newProxyInstance(className.getClassLoader(), className.getInterfaces(), target);
    }

    /**
     * 无参构造实例化（替代 Spring BeanUtils.instantiateClass）。
     */
    private static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot instantiate class: " + clazz.getName() + " (requires public no-arg constructor)", e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.debug("JDK动态代理，监听开始");
        Object result = method.invoke(target, args);
        log.debug("JDK动态代理，监听结束");
        return result;
    }

}
