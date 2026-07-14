package org.pf4j.spring.extension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.ExtensionFactory;
import org.pf4j.core.extension.ExtensionResolver;
import org.pf4j.core.extension.PluginLifecycleManager;
import org.pf4j.spring.SingletonSpringExtensionFactory;
import org.pf4j.spring.SpringExtensionFactory;
import org.pf4j.spring.SpringPluginManager;
import org.pf4j.spring.extension.event.SpringPluginEventPublisher;
import org.pf4j.spring.extension.lifecycle.SpringPluginLifecycleSynchronizer;
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

	/**
	 * 是否由 Spring 扩展工厂自动装配扩展实例。
	 */
	private final boolean autowire;

	/**
	 * 是否在同一插件管理器内复用扩展实例。
	 */
	private final boolean singleton;

	/**
	 * 是否将带 Spring 组件注解的 PF4J 扩展注入应用上下文。
	 */
	private final boolean injectable;

	/**
	 * 插件管理器初始化状态，用于保证初始化和销毁操作幂等。
	 */
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * 动态 Spring MVC Controller 注册器；仅在开启扩展注入时必须提供。
	 */
	private final DynamicControllerRegistry dynamicControllerRegistry;

	/**
	 * 提供严格启动、安全停止和串行卸载的核心生命周期管理器。
	 */
	private final PluginLifecycleManager lifecycleManager;

	/**
	 * 插件状态与 Spring Bean 生命周期同步器。
	 */
	private SpringPluginLifecycleSynchronizer lifecycleSynchronizer;

	/**
	 * PF4J 状态到 Spring 应用事件的适配发布器。
	 */
	private SpringPluginEventPublisher pluginEventPublisher;

	/**
	 * 使用文件形式的插件根目录创建管理器。
	 *
	 * @param dynamicControllerRegistry 动态 Controller 注册器；未开启注入时可以为 {@code null}
	 * @param pluginsRoot PF4J 插件根目录
	 * @param autowire 是否自动装配扩展实例
	 * @param singleton 是否复用同一扩展实例
	 * @param injectable 是否将 Spring 组件扩展注入应用上下文
	 * @throws NullPointerException 当 {@code pluginsRoot} 为 {@code null} 时抛出
	 */
	public ExtendedSpringPluginManager(DynamicControllerRegistry dynamicControllerRegistry, File pluginsRoot, boolean autowire, boolean singleton, boolean injectable) {
		this(dynamicControllerRegistry,Objects.requireNonNull(pluginsRoot, "pluginsRoot must not be null").toPath(), autowire, singleton,
				injectable);
	}

	/**
	 * 使用字符串形式的插件根目录创建管理器。
	 *
	 * @param dynamicControllerRegistry 动态 Controller 注册器；未开启注入时可以为 {@code null}
	 * @param pluginsRoot PF4J 插件根目录路径
	 * @param autowire 是否自动装配扩展实例
	 * @param singleton 是否复用同一扩展实例
	 * @param injectable 是否将 Spring 组件扩展注入应用上下文
	 * @throws NullPointerException 当 {@code pluginsRoot} 为 {@code null} 时抛出
	 */
	public ExtendedSpringPluginManager(DynamicControllerRegistry dynamicControllerRegistry, String pluginsRoot, boolean autowire, boolean singleton, boolean injectable) {
		this(dynamicControllerRegistry,Paths.get(Objects.requireNonNull(pluginsRoot, "pluginsRoot must not be null")), autowire, singleton,
				injectable);
	}

	/**
	 * 使用 {@link Path} 形式的插件根目录创建管理器。
	 *
	 * @param dynamicControllerRegistry 动态 Controller 注册器；未开启注入时可以为 {@code null}
	 * @param pluginsRoot PF4J 插件根目录路径
	 * @param autowire 是否自动装配扩展实例
	 * @param singleton 是否复用同一扩展实例
	 * @param injectable 是否将 Spring 组件扩展注入应用上下文
	 */
	public ExtendedSpringPluginManager(DynamicControllerRegistry dynamicControllerRegistry, Path pluginsRoot, boolean autowire, boolean singleton, boolean injectable) {
		super(pluginsRoot);
		this.dynamicControllerRegistry = dynamicControllerRegistry;
		this.autowire = autowire;
		this.singleton = singleton;
		this.injectable = injectable;
		this.lifecycleManager = new PluginLifecycleManager(this);
	}

	/**
	 * 根据单例配置创建 Spring 扩展工厂。
	 *
	 * @return 单例或普通 Spring 扩展工厂
	 */
	@Override
	protected ExtensionFactory createExtensionFactory() {
		if (isSingleton()) {
			return new SingletonSpringExtensionFactory(this, isAutowire());
		}
		return new SpringExtensionFactory(this, isAutowire());
	}

	/**
	 * 在 Spring 属性装配完成后加载、启动并按需注入全部插件扩展。
	 *
	 * <p>该操作通过原子状态保证幂等。初始化失败时会恢复未初始化状态，并尽力停止和卸载
	 * 已处理插件；停止或卸载异常会作为受抑制异常附加到原始初始化异常。</p>
	 *
	 * @throws NullPointerException 当开启注入但未提供动态 Controller 注册器时抛出
	 * @throws RuntimeException 当插件加载、启动或扩展注入失败时继续抛出
	 */
	@Override
	public void afterPropertiesSet() {
		if (!initialized.compareAndSet(false, true)) {
			return;
		}
		try {
			configureSpringLifecycleIntegration();
			lifecycleManager.loadAllAndStartStrictly();
			if (isInjectable()) {
				lifecycleSynchronizer.removeAll();
				for (org.pf4j.PluginWrapper plugin : getStartedPlugins()) {
					lifecycleSynchronizer.pluginStateChanged(new org.pf4j.PluginStateEvent(this, plugin,
							org.pf4j.PluginState.RESOLVED));
				}
			}
		} catch (RuntimeException ex) {
			initialized.set(false);
			stopAndUnloadPlugins(ex);
			removeSpringLifecycleIntegration();
			throw ex;
		}
	}

	/**
	 * 在 Spring 容器销毁时停止并卸载全部插件。
	 *
	 * <p>重复销毁不会再次执行 PF4J 生命周期操作。</p>
	 */
	@Override
	public void destroy() {
		if (!initialized.compareAndSet(true, false)) {
			return;
		}
		try {
			lifecycleManager.unloadAllSafely();
		} finally {
			removeSpringLifecycleIntegration();
		}
	}

	/**
	 * 判断扩展实例是否启用 Spring 自动装配。
	 *
	 * @return 启用自动装配时返回 {@code true}
	 */
	public boolean isAutowire() {
		return autowire;
	}

	/**
	 * 判断扩展实例是否按单例方式复用。
	 *
	 * @return 启用单例扩展工厂时返回 {@code true}
	 */
	public boolean isSingleton() {
		return singleton;
	}

	/**
	 * 判断是否将 Spring 组件扩展注入应用上下文。
	 *
	 * @return 启用扩展注入时返回 {@code true}
	 */
	public boolean isInjectable() {
		return injectable;
	}

	/**
	 * 获取当前初始化状态。
	 *
	 * @return 已成功初始化且尚未销毁时返回 {@code true}
	 */
	public boolean isInitialized() {
		return initialized.get();
	}

	/**
	 * 创建绑定当前插件管理器的类型安全扩展解析器。
	 *
	 * @return 绑定当前插件管理器的新扩展解析器
	 */
	public ExtensionResolver getExtensionResolver() {
		return new ExtensionResolver(this);
	}

	/**
	 * 获取受控插件生命周期管理器。
	 *
	 * @return 当前 Spring 插件管理器使用的生命周期管理器
	 */
	public PluginLifecycleManager getLifecycleManager() {
		return lifecycleManager;
	}

	/**
	 * 初始化失败后尽力停止并卸载插件，同时保留全部清理异常。
	 *
	 * @param initializationException 触发清理的原始初始化异常，清理异常将附加到该异常
	 */
	private void stopAndUnloadPlugins(RuntimeException initializationException) {
		try {
			lifecycleManager.unloadAllSafely();
		} catch (RuntimeException unloadException) {
			initializationException.addSuppressed(unloadException);
			log.warn("Failed to unload PF4J plugins after initialization failure", unloadException);
		}
	}

	/**
	 * 配置插件 Bean 同步器和 Spring 状态事件发布器。
	 */
	private void configureSpringLifecycleIntegration() {
		if (Objects.isNull(pluginEventPublisher) && Objects.nonNull(getApplicationContext())) {
			pluginEventPublisher = new SpringPluginEventPublisher(getApplicationContext());
			addPluginStateListener(pluginEventPublisher);
		}
		if (isInjectable() && Objects.isNull(lifecycleSynchronizer)) {
			Objects.requireNonNull(dynamicControllerRegistry,
					"dynamicControllerRegistry is required when injectable is enabled");
			Objects.requireNonNull(getApplicationContext(),
					"applicationContext is required when injectable is enabled");
			ExtendedExtensionsInjector extensionsInjector = new ExtendedExtensionsInjector(this,
					dynamicControllerRegistry, getApplicationContext());
			lifecycleSynchronizer = new SpringPluginLifecycleSynchronizer(extensionsInjector);
			addPluginStateListener(lifecycleSynchronizer);
		}
	}

	/**
	 * 注销 Spring 生命周期集成并移除残留插件 Bean。
	 */
	private void removeSpringLifecycleIntegration() {
		if (Objects.nonNull(lifecycleSynchronizer)) {
			lifecycleSynchronizer.removeAll();
			removePluginStateListener(lifecycleSynchronizer);
			lifecycleSynchronizer = null;
		}
		if (Objects.nonNull(pluginEventPublisher)) {
			removePluginStateListener(pluginEventPublisher);
			pluginEventPublisher = null;
		}
	}

}
