package org.pf4j.spring.boot.ext.utils;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjectorUtilsTest {

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

    @Component("namedExtension")
    private static class NamedComponent {
    }

    @Service
    private static class ServiceExtension {
    }

    @RestController("restExtension")
    private static class RestExtension {
    }

    private static class PlainExtension {
    }
}
