package org.pf4j.spring.extension;

import java.util.Objects;

import org.pf4j.spring.ExtensionsInjector;
import org.pf4j.spring.SpringPluginManager;
import org.pf4j.spring.extension.registry.DynamicControllerRegistry;
import org.pf4j.spring.extension.util.InjectorUtils;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

public class ExtendedExtensionsInjector extends ExtensionsInjector {

	private final DynamicControllerRegistry dynamicControllerRegistry;

	public ExtendedExtensionsInjector(SpringPluginManager pluginManager, DynamicControllerRegistry dynamicControllerRegistry,
			ApplicationContext applicationContext) {
		super(Objects.requireNonNull(pluginManager, "pluginManager must not be null"),
				(AbstractAutowireCapableBeanFactory) Objects
						.requireNonNull(applicationContext, "applicationContext must not be null")
						.getAutowireCapableBeanFactory());
		this.dynamicControllerRegistry = Objects.requireNonNull(dynamicControllerRegistry,
				"dynamicControllerRegistry must not be null");
	}

	@Override
	protected void registerExtension(Class<?> extensionClass) {
		if (!InjectorUtils.isInjectNecessary(extensionClass)) {
			return;
		}
		Object extension = Objects.requireNonNull(springPluginManager.getExtensionFactory().create(extensionClass),
				"PF4J extension factory returned null for " + extensionClass.getName());
		String beanName = InjectorUtils.getBeanName(extensionClass, extension.getClass().getName());
		if (InjectorUtils.isController(extensionClass)) {
			dynamicControllerRegistry.registerController(beanName, extension);
			return;
		}
		if (beanFactory.containsSingleton(beanName)) {
			beanFactory.destroySingleton(beanName);
		}
		beanFactory.registerSingleton(beanName, extension);
	}

}
