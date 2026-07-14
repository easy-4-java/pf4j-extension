package org.pf4j.core.extension.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * 基于独立 cglib 的类代理拦截器。
 *
 * <p>通过生成目标类型的子类代理非接口类型，并将代理方法转发到内部目标实例。
 * 使用 {@code net.sf.cglib} 可保持 core 模块不依赖 Spring。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public class CglibProxy implements MethodInterceptor {

    /**
     * 接收实际方法调用的目标对象。
     */
    private Object target;

    /**
     * 创建绑定指定目标对象的 CGLIB 方法拦截器。
     *
     * @param target 接收代理方法调用的目标对象
     */
    public CglibProxy(Object target) {
        this.target = target;
    }

    /**
     * 为已有目标对象创建 CGLIB 子类代理。
     *
     * <p>代理类以目标对象的运行时类型作为父类，所有方法调用转发给传入实例。</p>
     *
     * @param objectTarget 接收实际调用的目标对象
     * @return 目标对象运行时类型的 CGLIB 代理实例
     */
    public static Object getCglibProxy(Object objectTarget) {
        CglibProxy target = new CglibProxy(objectTarget);
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(objectTarget.getClass());
        enhancer.setCallback(target);
        Object result = enhancer.create();
        return result;
    }

    /**
     * 实例化指定类型并为其创建 CGLIB 子类代理。
     *
     * @param className 需要实例化和代理的具体类型，必须具有可访问的无参构造器
     * @param <T> 目标对象类型
     * @return 指定类型的 CGLIB 代理实例
     * @throws IllegalStateException 当目标类型无法通过无参构造器实例化时抛出
     */
    public static <T> T getCglibProxy(Class<T> className) {
        CglibProxy target = new CglibProxy(instantiate(className));
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(className);
        enhancer.setCallback(target);
        Object result = enhancer.create();
        return (T) result;
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
     * 拦截代理方法并将调用转发给目标对象。
     *
     * @param obj 接收到调用的 CGLIB 代理对象
     * @param method 被调用的方法
     * @param arr 方法实参数组
     * @param proxy CGLIB 方法代理元数据
     * @return 目标方法返回值；目标方法返回 {@code void} 时为 {@code null}
     * @throws Throwable 当反射调用目标方法失败或目标方法抛出异常时继续抛出
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] arr, MethodProxy proxy) throws Throwable {
        log.debug("Cglib动态代理，监听开始");
        Object invoke = method.invoke(target, arr);
        log.debug("Cglib动态代理，监听结束");
        return invoke;
    }

}
