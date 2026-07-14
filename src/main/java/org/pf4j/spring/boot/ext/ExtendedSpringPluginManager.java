package org.pf4j.spring.boot.ext;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.pf4j.ExtensionFactory;
import org.pf4j.spring.SingletonSpringExtensionFactory;
import org.pf4j.spring.SpringExtensionFactory;
import org.pf4j.spring.SpringPluginManager;
import org.pf4j.spring.boot.ext.registry.Pf4jDynamicControllerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.InitializingBean;

public class ExtendedSpringPluginManager extends SpringPluginManager implements InitializingBean {

	private final boolean autowire;
	private final boolean singleton;
	private final boolean injectable;

	private Pf4jDynamicControllerRegistry dynamicControllerRegistry;

	public ExtendedSpringPluginManager(File pluginsRoot, boolean autowire, boolean singleton, boolean injectable) {
		this(Objects.requireNonNull(pluginsRoot, "pluginsRoot must not be null").toPath(), autowire, singleton,
				injectable);
	}

	public ExtendedSpringPluginManager(String pluginsRoot, boolean autowire, boolean singleton, boolean injectable) {
		this(Paths.get(Objects.requireNonNull(pluginsRoot, "pluginsRoot must not be null")), autowire, singleton,
				injectable);
	}

	public ExtendedSpringPluginManager(Path pluginsRoot, boolean autowire, boolean singleton, boolean injectable) {
		super(pluginsRoot);
		this.autowire = autowire;
		this.singleton = singleton;
		this.injectable = injectable;
	}

	@Override
	protected ExtensionFactory createExtensionFactory() {
		if (isSingleton()) {
			return new SingletonSpringExtensionFactory(this, isAutowire());
		}
		return new SpringExtensionFactory(this, isAutowire());
	}

	@Override
	public void afterPropertiesSet() {
		loadPlugins();
		startPlugins();
		if (isInjectable()) {
			Objects.requireNonNull(dynamicControllerRegistry,
					"dynamicControllerRegistry is required when injectable is enabled");
			ExtendedExtensionsInjector extensionsInjector = new ExtendedExtensionsInjector(this,
					dynamicControllerRegistry, getApplicationContext());
			extensionsInjector.injectExtensions();
		}
	}

	@Autowired(required = false)
	public void setDynamicControllerRegistry(Pf4jDynamicControllerRegistry dynamicControllerRegistry) {
		this.dynamicControllerRegistry = dynamicControllerRegistry;
	}

	public boolean isAutowire() {
		return autowire;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public boolean isInjectable() {
		return injectable;
	}

}
