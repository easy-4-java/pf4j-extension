package org.pf4j.spring.extension.util;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

/**
 * PF4J 扩展注入 Spring 容器时使用的注解解析工具。
 *
 * <p>通过 Spring 合并注解模型识别组合注解和元注解，判断扩展是否需要注入、是否为
 * Controller，并按照 Spring 组件注解提取显式 Bean 名称。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Utility class for dependency injection operations in the PF4J Spring integration.
 */
class InjectorUtils {

	/**
	 * 阻止工具类被实例化。
	 */
	private InjectorUtils() {
	}

	/**
	 * 判断扩展类型是否为 Spring MVC Controller。
	 *
	 * @param extensionClass 待检查的扩展实现类型
	 * @return 存在 {@link RestController} 或 {@link Controller} 合并注解时返回 {@code true}
	 */
	public static boolean isController(Class<?> extensionClass) {
		return Objects.nonNull(AnnotatedElementUtils.findMergedAnnotation(extensionClass, RestController.class))
				|| Objects.nonNull(AnnotatedElementUtils.findMergedAnnotation(extensionClass, Controller.class));
	}

	/**
	 * 判断扩展类型是否需要注入 Spring 容器。
	 *
	 * @param extensionClass 待检查的扩展实现类型
	 * @return 扩展是 Controller 或带有 {@link Component} 合并注解时返回 {@code true}
	 */
	public static boolean isInjectNecessary(Class<?> extensionClass) {
		if (isController(extensionClass)) {
			return true;
		}
		return Objects.nonNull(AnnotatedElementUtils.findMergedAnnotation(extensionClass, Component.class));
	}

	/**
	 * 按 Spring 组件注解优先级解析扩展 Bean 名称。
	 *
	 * <p>依次检查 {@link RestController}、{@link Controller}、{@link Component}、
	 * {@link Service} 和 {@link Repository} 的显式名称；均未声明名称时返回默认值。</p>
	 *
	 * @param extensionClass 待解析的扩展实现类型
	 * @param defaultName 注解未指定名称时使用的默认 Bean 名称
	 * @return 注解声明的 Bean 名称或 {@code defaultName}
	 */
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
