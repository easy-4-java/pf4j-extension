package org.pf4j.core.extension.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

//Cglib动态代理，实现MethodInterceptor接口

/**
 * 基于独立 cglib（net.sf.cglib）实现的动态代理，去除对 spring-core 的依赖。
 * <p>
 * 原实现使用 {@code org.springframework.cglib}，但 Spring 6.x 已移除该内部包，
 * 故改用 cglib 独立 jar（保持 ddd4j-extension-pf4j 模块零 Spring 依赖）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public class CglibProxy implements MethodInterceptor {

    /**
     * 需要代理的目标对象
     */
    private Object target;

    public CglibProxy(Object target) {
        this.target = target;
    }

    // 定义获取代理对象方法
    public static Object getCglibProxy(Object objectTarget) {
        //为目标对象target赋值
        CglibProxy target = new CglibProxy(objectTarget);
        Enhancer enhancer = new Enhancer();
        //设置父类,因为Cglib是针对指定的类生成一个子类，所以需要指定父类
        enhancer.setSuperclass(objectTarget.getClass());
        enhancer.setCallback(target);// 设置回调
        Object result = enhancer.create();//创建并返回代理对象
        return result;
    }

    // 定义获取代理对象方法
    public static <T> T getCglibProxy(Class<T> className) {
        CglibProxy target = new CglibProxy(instantiate(className));
        Enhancer enhancer = new Enhancer();
        //设置父类,因为Cglib是针对指定的类生成一个子类，所以需要指定父类
        enhancer.setSuperclass(className);
        enhancer.setCallback(target);// 设置回调
        Object result = enhancer.create();//创建并返回代理对象
        return (T) result;
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

    //重写拦截方法
    @Override
    public Object intercept(Object obj, Method method, Object[] arr, MethodProxy proxy) throws Throwable {
        log.debug("Cglib动态代理，监听开始");
        Object invoke = method.invoke(target, arr);//方法执行，参数：target 目标对象 arr参数数组
        log.debug("Cglib动态代理，监听结束");
        return invoke;
    }

}
