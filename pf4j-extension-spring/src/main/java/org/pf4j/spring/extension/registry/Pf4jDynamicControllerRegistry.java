package org.pf4j.spring.extension.registry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 基于 Spring MVC {@link RequestMappingHandlerMapping} 的动态 Controller 注册器。
 *
 * <p>注册时先清理同名 Bean 的旧请求映射，再注册新的单例并触发 Spring MVC 的处理器方法检测；
 * 移除时注销该 Bean 对应的全部请求映射并销毁单例。处理器映射可通过构造器提供，也可从
 * {@link ApplicationContext} 中按类型和顺序自动选择。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public class Pf4jDynamicControllerRegistry implements DynamicControllerRegistry, ApplicationContextAware {

	/**
	 * Spring MVC 内部处理器检测方法，用于为运行时注册的 Controller 建立请求映射。
	 */
	private static final Method DETECT_HANDLER_METHODS_METHOD = ReflectionUtils
			.findMethod(RequestMappingHandlerMapping.class, "detectHandlerMethods", Object.class);

	/**
	 * 当前使用的 Spring MVC 请求映射管理器；未显式提供时延迟从应用上下文解析。
	 */
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	/**
	 * 支持动态注册和销毁单例 Bean 的 Spring BeanFactory。
	 */
	@Setter
    private AbstractAutowireCapableBeanFactory beanFactory;

	/**
	 * 用于发现请求映射管理器和获取 BeanFactory 的 Spring 应用上下文。
	 */
	private ApplicationContext applicationContext;

	/**
	 * 开放 Spring MVC 内部处理器检测方法的反射访问权限。
	 */
	static {
		Objects.requireNonNull(DETECT_HANDLER_METHODS_METHOD).setAccessible(true);
	}

	/**
	 * 使用指定请求映射管理器创建动态注册器。
	 *
	 * @param requestMappingHandlerMapping Spring MVC 请求映射管理器
	 */
	public Pf4jDynamicControllerRegistry(RequestMappingHandlerMapping requestMappingHandlerMapping) {
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
	}

	/**
	 * 使用指定请求映射管理器和 BeanFactory 创建动态注册器。
	 *
	 * @param requestMappingHandlerMapping Spring MVC 请求映射管理器
	 * @param beanFactory 支持动态单例注册和销毁的 BeanFactory
	 */
	public Pf4jDynamicControllerRegistry(RequestMappingHandlerMapping requestMappingHandlerMapping,
										 AbstractAutowireCapableBeanFactory beanFactory) {
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
		this.beanFactory = beanFactory;
	}

	/**
	 * 保存 Spring 应用上下文，并同步获取其可自动装配 BeanFactory。
	 *
	 * @param applicationContext 当前 Spring 应用上下文
	 * @throws NullPointerException 当 {@code applicationContext} 为 {@code null} 时抛出
	 * @throws ClassCastException 当上下文 BeanFactory 不是
	 *         {@link AbstractAutowireCapableBeanFactory} 时抛出
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext must not be null");
		this.beanFactory = (AbstractAutowireCapableBeanFactory) this.applicationContext.getAutowireCapableBeanFactory();
	}

	/**
	 * 注册或替换动态 Controller 及其请求映射。
	 *
	 * @param beanName Controller Bean 名称；无文本时使用 Controller 运行时类型名
	 * @param controller 待注册的 Controller 实例
	 * @throws NullPointerException 当 {@code controller} 或必需的 BeanFactory 为空时抛出
	 * @throws IllegalArgumentException 当无法解析 Spring MVC 请求映射管理器时抛出
	 */
	@Override
	public void registerController(String beanName, Object controller) {
		Objects.requireNonNull(controller, "controller must not be null");
		String controllerBeanName = StringUtils.hasText(beanName) ? beanName : controller.getClass().getName();
		removeRequestMappingIfNecessary(controllerBeanName);
		getBeanFactory().registerSingleton(controllerBeanName, controller);
		registerRequestMappingIfNecessary(controllerBeanName);
	}

	/**
	 * 移除指定 Controller 的请求映射和单例 Bean。
	 *
	 * @param controllerBeanName 待移除的 Controller Bean 名称
	 * @throws IOException 保留接口异常契约；当前实现不执行 I/O 操作
	 */
	@Override
	public void removeController(String controllerBeanName) throws IOException {
		removeRequestMappingIfNecessary(controllerBeanName);
	}

	/**
	 * 当目标 Bean 已注册时，注销其全部请求映射并销毁单例。
	 *
	 * @param controllerBeanName Controller Bean 名称
	 */
	private void removeRequestMappingIfNecessary(String controllerBeanName) {
		if (!getBeanFactory().containsBean(controllerBeanName)) {
			return;
		}

		RequestMappingHandlerMapping handlerMapping = getRequestMappingHandlerMapping();
		Set<RequestMappingInfo> mappings = new LinkedHashSet<RequestMappingInfo>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
			if (controllerBeanName.equals(entry.getValue().getBean())) {
				mappings.add(entry.getKey());
			}
		}
		for (RequestMappingInfo mapping : mappings) {
			handlerMapping.unregisterMapping(mapping);
		}
		getBeanFactory().destroySingleton(controllerBeanName);
		log.debug("Removed dynamic PF4J controller '{}' with {} request mappings", controllerBeanName,
				mappings.size());
	}

	/**
	 * 调用 Spring MVC 内部检测方法，为动态 Bean 注册处理器方法。
	 *
	 * @param controllerBeanName 已注册到 BeanFactory 的 Controller Bean 名称
	 */
	private void registerRequestMappingIfNecessary(String controllerBeanName) {
		ReflectionUtils.invokeMethod(DETECT_HANDLER_METHODS_METHOD, getRequestMappingHandlerMapping(), controllerBeanName);
		log.debug("Registered dynamic PF4J controller '{}'", controllerBeanName);
	}

	/**
	 * 获取或从应用上下文中解析请求映射管理器。
	 *
	 * <p>存在多个候选时优先选择框架原生类型，否则按 Spring 顺序规则选择第一个候选。</p>
	 *
	 * @return 可用于动态注册请求映射的管理器
	 * @throws IllegalArgumentException 当应用上下文中不存在可用映射管理器时抛出
	 */
	private RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
		try {
			if (Objects.nonNull(requestMappingHandlerMapping)) {
				return requestMappingHandlerMapping;
			}
			Map<String, RequestMappingHandlerMapping> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
					getApplicationContext(), RequestMappingHandlerMapping.class, true, false);
			if (!CollectionUtils.isEmpty(beans)) {
				List<RequestMappingHandlerMapping> mappings = new ArrayList<>(beans.values());
				for (RequestMappingHandlerMapping handlerMapping : beans.values()) {
					if (handlerMapping.getClass().getName().equals(RequestMappingHandlerMapping.class.getName())) {
						requestMappingHandlerMapping = handlerMapping;
						return handlerMapping;
					}
				}
				AnnotationAwareOrderComparator.sort(mappings);
				requestMappingHandlerMapping = mappings.get(0);
				return requestMappingHandlerMapping;
			}
			requestMappingHandlerMapping = getApplicationContext().getBean(RequestMappingHandlerMapping.class);
			return requestMappingHandlerMapping;
		} catch (Exception e) {
			throw new IllegalArgumentException("applicationContext must has RequestMappingHandlerMapping", e);
		}
	}

	/**
	 * 获取用于动态管理单例的 BeanFactory。
	 *
	 * @return 已配置或从应用上下文解析的 BeanFactory
	 * @throws NullPointerException 当 BeanFactory 尚未初始化时抛出
	 */
	public AbstractAutowireCapableBeanFactory getBeanFactory() {
		return Objects.requireNonNull(beanFactory, "beanFactory must not be null");
	}

	/**
	 * 获取当前 Spring 应用上下文。
	 *
	 * @return 已由 Spring 回调设置的应用上下文
	 * @throws NullPointerException 当应用上下文尚未初始化时抛出
	 */
    public ApplicationContext getApplicationContext() {
		return Objects.requireNonNull(applicationContext, "applicationContext must not be null");
	}

}
