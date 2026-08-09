package org.pf4j.core.extension.invocation;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

/**
 * 扩展方法调用上下文。
 *
 * <p>上下文携带插件、扩展、目标对象、方法和参数，并提供线程安全属性表供拦截器之间传递
 * 低生命周期的调用数据。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Getter
public final /**
 * Context object carrying information about an extension method invocation including method, arguments, and result.
 */
class ExtensionInvocationContext {

    /** 插件 ID。 */
    private final String pluginId;

    /** 扩展稳定 ID。 */
    private final String extensionId;

    /** 接收实际调用的扩展实例。 */
    private final Object target;

    /** 当前被调用的方法。 */
    private final Method method;

    /** 方法参数副本。 */
    private final Object[] arguments;

    /** 拦截器共享属性。 */
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    /**
     * 创建扩展调用上下文。
     *
     * @param pluginId 插件 ID
     * @param extensionId 扩展 ID
     * @param target 扩展实例
     * @param method 被调用方法
     * @param arguments 方法参数
     */
    public ExtensionInvocationContext(String pluginId, String extensionId, Object target, Method method,
                                      Object[] arguments) {
        this.pluginId = pluginId;
        this.extensionId = extensionId;
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.arguments = Objects.isNull(arguments) ? new Object[0] : arguments.clone();
    }

    /**
     * 写入拦截器共享属性。
     *
     * @param name 属性名称
     * @param value 属性值
     * @throws NullPointerException 当名称或值为空时抛出
     */
    public void setAttribute(String name, Object value) {
        attributes.put(Objects.requireNonNull(name, "name must not be null"),
                Objects.requireNonNull(value, "value must not be null"));
    }

    /**
     * 获取拦截器共享属性。
     *
     * @param name 属性名称
     * @param <T> 属性值类型
     * @return 属性值；不存在时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    /**
     * 获取不可修改的属性视图。
     *
     * @return 当前属性不可修改视图
     */
    public Map<String, Object> getAttributesView() {
        return Collections.unmodifiableMap(attributes);
    }
}
