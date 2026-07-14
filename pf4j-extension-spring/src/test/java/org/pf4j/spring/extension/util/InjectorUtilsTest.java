package org.pf4j.spring.extension.util;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link InjectorUtils} 的 Spring 组件识别和 Bean 名称解析测试。
 */
class InjectorUtilsTest {

    /**
     * 验证组件、服务和 Controller 的注入判定及显式 Bean 名称解析规则。
     */
    @Test
    void shouldRecognizeSpringExtensionTypesAndBeanNames() {
        assertTrue(InjectorUtils.isInjectNecessary(NamedComponent.class));
        assertTrue(InjectorUtils.isInjectNecessary(ServiceExtension.class));
        assertTrue(InjectorUtils.isController(RestExtension.class));
        assertFalse(InjectorUtils.isInjectNecessary(PlainExtension.class));

        assertEquals("namedExtension", InjectorUtils.getBeanName(NamedComponent.class, "fallback"));
        assertEquals("fallback", InjectorUtils.getBeanName(ServiceExtension.class, "fallback"));
        assertEquals("restExtension", InjectorUtils.getBeanName(RestExtension.class, "fallback"));
    }

    /**
     * 声明显式组件名称的测试类型。
     */
    @Component("namedExtension")
    private static class NamedComponent {
    }

    /**
     * 未声明显式名称的服务测试类型。
     */
    @Service
    private static class ServiceExtension {
    }

    /**
     * 声明显式 Controller 名称的 REST 测试类型。
     */
    @RestController("restExtension")
    private static class RestExtension {
    }

    /**
     * 不包含任何 Spring 组件注解的普通测试类型。
     */
    private static class PlainExtension {
    }
}
