package org.pf4j.spring.boot.ext.utils;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

public final class InjectorUtils {

	private InjectorUtils() {
	}

	public static boolean isController(Class<?> extensionClass) {
		return Objects.nonNull(AnnotatedElementUtils.findMergedAnnotation(extensionClass, RestController.class))
				|| Objects.nonNull(AnnotatedElementUtils.findMergedAnnotation(extensionClass, Controller.class));
	}

	public static boolean isInjectNecessary(Class<?> extensionClass) {
		if (isController(extensionClass)) {
			return true;
		}
		return Objects.nonNull(AnnotatedElementUtils.findMergedAnnotation(extensionClass, Component.class));
	}

	public static String getBeanName(Class<?> extensionClass, String defaultName) {
		RestController restController = AnnotatedElementUtils.findMergedAnnotation(extensionClass, RestController.class);
		if (Objects.nonNull(restController) && StringUtils.hasText(restController.value())) {
			return restController.value();
		}
		Controller controller = AnnotatedElementUtils.findMergedAnnotation(extensionClass, Controller.class);
		if (Objects.nonNull(controller) && StringUtils.hasText(controller.value())) {
			return controller.value();
		}
		Component component = AnnotatedElementUtils.findMergedAnnotation(extensionClass, Component.class);
		if (Objects.nonNull(component) && StringUtils.hasText(component.value())) {
			return component.value();
		}
		Service service = AnnotatedElementUtils.findMergedAnnotation(extensionClass, Service.class);
		if (Objects.nonNull(service) && StringUtils.hasText(service.value())) {
			return service.value();
		}
		Repository repository = AnnotatedElementUtils.findMergedAnnotation(extensionClass, Repository.class);
		if (Objects.nonNull(repository) && StringUtils.hasText(repository.value())) {
			return repository.value();
		}
		return defaultName;
	}

}
