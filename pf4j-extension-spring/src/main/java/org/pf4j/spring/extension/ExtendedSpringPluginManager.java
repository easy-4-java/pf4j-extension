package org.pf4j.spring.extension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.ExtensionFactory;
import org.pf4j.core.extension.ExtensionResolver;
import org.pf4j.spring.SingletonSpringExtensionFactory;
import org.pf4j.spring.SpringExtensionFactory;
import org.pf4j.spring.SpringPluginManager;
import org.pf4j.spring.extension.registry.DynamicControllerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 支持 Spring 扩展注入与容器生命周期托管的 PF4J 插件管理器。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public class ExtendedSpringPluginManager extends SpringPluginManager implements InitializingBean, DisposableBean {

	private final boolean autowire;
	private final boolean singleton;
	private final boolean injectable;
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private DynamicControllerRegistry dynamicControllerRegistry;

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
		if (!initialized.compareAndSet(false, true)) {
			return;
		}
		try {
			loadPlugins();
			startPlugins();
			if (isInjectable()) {
				Objects.requireNonNull(dynamicControllerRegistry,
						"dynamicControllerRegistry is required when injectable is enabled");
				ExtendedExtensionsInjector extensionsInjector = new ExtendedExtensionsInjector(this,
						dynamicControllerRegistry, getApplicationContext());
				extensionsInjector.injectExtensions();
			}
		} catch (RuntimeException ex) {
			initialized.set(false);
			stopAndUnloadPlugins(ex);
			throw ex;
		}
	}

	@Override
	public void destroy() {
		if (!initialized.compareAndSet(true, false)) {
			return;
		}
		stopPlugins();
		unloadPlugins();
	}

	@Autowired(required = false)
	public void setDynamicControllerRegistry(DynamicControllerRegistry dynamicControllerRegistry) {
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

	public boolean isInitialized() {
		return initialized.get();
	}

	/**
	 * 创建绑定当前插件管理器的类型安全扩展解析器。
	 *
	 * @return 扩展解析器
	 */
	public ExtensionResolver getExtensionResolver() {
		return new ExtensionResolver(this);
	}

	private void stopAndUnloadPlugins(RuntimeException initializationException) {
		try {
			stopPlugins();
		} catch (RuntimeException stopException) {
			initializationException.addSuppressed(stopException);
			log.warn("Failed to stop PF4J plugins after initialization failure", stopException);
		}
		try {
			unloadPlugins();
		} catch (RuntimeException unloadException) {
			initializationException.addSuppressed(unloadException);
			log.warn("Failed to unload PF4J plugins after initialization failure", unloadException);
		}
	}

}
