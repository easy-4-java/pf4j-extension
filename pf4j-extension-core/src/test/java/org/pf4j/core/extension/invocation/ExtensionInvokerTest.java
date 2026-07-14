package org.pf4j.core.extension.invocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.pf4j.core.extension.exception.PluginInvokeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ExtensionInvoker} 责任链与异常边界测试。
 */
class ExtensionInvokerTest {

    /**
     * 验证拦截器按嵌套顺序执行且能够共享调用属性。
     */
    @Test
    void shouldInvokeExtensionThroughOrderedInterceptorChain() {
        List<String> calls = new ArrayList<String>();
        ExtensionInterceptor outer = (context, chain) -> {
            calls.add("outer-before");
            context.setAttribute("traceId", "trace-1");
            Object result = chain.proceed(context);
            calls.add("outer-after");
            return result;
        };
        ExtensionInterceptor inner = (context, chain) -> {
            calls.add("inner-" + context.<String>getAttribute("traceId"));
            return chain.proceed(context);
        };
        GreetingExtension proxy = new ExtensionInvoker(Arrays.asList(outer, inner))
                .createProxy(GreetingExtension.class, name -> "hello " + name, "demo-plugin", "greeting");

        assertEquals("hello PF4J", proxy.greet("PF4J"));
        assertEquals(Arrays.asList("outer-before", "inner-trace-1", "outer-after"), calls);
    }

    /**
     * 验证扩展实现异常被解包并携带插件与扩展定位信息。
     */
    @Test
    void shouldWrapTargetFailureWithPluginContext() {
        IllegalStateException cause = new IllegalStateException("broken");
        GreetingExtension proxy = new ExtensionInvoker().createProxy(GreetingExtension.class, name -> {
            throw cause;
        }, "demo-plugin", "greeting");

        PluginInvokeException failure = assertThrows(PluginInvokeException.class, () -> proxy.greet("PF4J"));

        assertEquals("demo-plugin", failure.getPluginId());
        assertEquals("greeting", failure.getExtensionId());
        assertSame(cause, failure.getCause());
    }

    /**
     * 测试用问候扩展点。
     */
    private interface GreetingExtension {

        /**
         * 生成问候语。
         *
         * @param name 被问候名称
         * @return 问候文本
         */
        String greet(String name);
    }
}
