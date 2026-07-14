package org.pf4j.spring.boot.ext.registry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

@Slf4j
public class Pf4jDynamicControllerRegistry implements DynamicControllerRegistry, ApplicationContextAware {

	private static final Method DETECT_HANDLER_METHODS_METHOD = ReflectionUtils
			.findMethod(RequestMappingHandlerMapping.class, "detectHandlerMethods", Object.class);
	@Autowired(required = false)
	private RequestMappingHandlerMapping requestMappingHandlerMapping;
	private AbstractAutowireCapableBeanFactory beanFactory;
	private ApplicationContext applicationContext;

	static {
		Objects.requireNonNull(DETECT_HANDLER_METHODS_METHOD).setAccessible(true);
	}

	public Pf4jDynamicControllerRegistry() {
	}

	public Pf4jDynamicControllerRegistry(AbstractAutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext must not be null");
		this.beanFactory = (AbstractAutowireCapableBeanFactory) this.applicationContext.getAutowireCapableBeanFactory();
	}

	@Override
	public void registerController(String beanName, Object controller) {
		Objects.requireNonNull(controller, "controller must not be null");
		String controllerBeanName = StringUtils.hasText(beanName) ? beanName : controller.getClass().getName();
		removeRequestMappingIfNecessary(controllerBeanName);
		getBeanFactory().registerSingleton(controllerBeanName, controller);
		registerRequestMappingIfNecessary(controllerBeanName);
	}

	@Override
	public void removeController(String controllerBeanName) throws IOException {
		removeRequestMappingIfNecessary(controllerBeanName);
	}

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

	private void registerRequestMappingIfNecessary(String controllerBeanName) {
		ReflectionUtils.invokeMethod(DETECT_HANDLER_METHODS_METHOD, getRequestMappingHandlerMapping(),
				controllerBeanName);
		log.debug("Registered dynamic PF4J controller '{}'", controllerBeanName);
	}

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

	public AbstractAutowireCapableBeanFactory getBeanFactory() {
		return Objects.requireNonNull(beanFactory, "beanFactory must not be null");
	}

	public void setBeanFactory(AbstractAutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public ApplicationContext getApplicationContext() {
		return Objects.requireNonNull(applicationContext, "applicationContext must not be null");
	}

}
