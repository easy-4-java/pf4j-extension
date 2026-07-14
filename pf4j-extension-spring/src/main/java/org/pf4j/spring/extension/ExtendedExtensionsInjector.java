package org.pf4j.spring.extension;

import java.util.Objects;

import org.pf4j.spring.ExtensionsInjector;
import org.pf4j.spring.SpringPluginManager;
import org.pf4j.spring.extension.registry.DynamicControllerRegistry;
import org.pf4j.spring.extension.registry.PluginBeanRegistry;
import org.pf4j.spring.extension.util.InjectorUtils;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * 支持 Spring Bean 和 Spring MVC Controller 的 PF4J 扩展注入器。
 *
 * <p>仅处理带有 Spring 组件注解的扩展类型。普通组件注册为单例 Bean，Controller 则委托
 * {@link DynamicControllerRegistry} 同时完成 Bean 注册和请求映射注册。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public class ExtendedExtensionsInjector extends ExtensionsInjector {

	/**
	 * 负责动态维护 Spring MVC Controller 及其请求映射的注册器。
	 */
	private final DynamicControllerRegistry dynamicControllerRegistry;

	/**
	 * 按插件跟踪已注册 Spring Bean 的注册表。
	 */
	private final PluginBeanRegistry pluginBeanRegistry;

	/**
	 * 创建扩展注入器。
	 *
	 * @param pluginManager 提供扩展类型和扩展实例工厂的 Spring PF4J 管理器
	 * @param dynamicControllerRegistry 动态 Controller 注册器
	 * @param applicationContext 目标 Spring 应用上下文，其 BeanFactory 必须支持单例注册
	 * @throws NullPointerException 当任一参数为 {@code null} 时抛出
	 * @throws ClassCastException 当应用上下文的 BeanFactory 不是
	 *         {@link AbstractAutowireCapableBeanFactory} 时抛出
	 */
	public ExtendedExtensionsInjector(SpringPluginManager pluginManager, DynamicControllerRegistry dynamicControllerRegistry,
			ApplicationContext applicationContext) {
		super(Objects.requireNonNull(pluginManager, "pluginManager must not be null"),
				(AbstractAutowireCapableBeanFactory) Objects
						.requireNonNull(applicationContext, "applicationContext must not be null")
						.getAutowireCapableBeanFactory());
		this.dynamicControllerRegistry = Objects.requireNonNull(dynamicControllerRegistry,
				"dynamicControllerRegistry must not be null");
		this.pluginBeanRegistry = new PluginBeanRegistry(beanFactory, this.dynamicControllerRegistry);
	}

	/**
	 * 注入指定插件贡献的全部 Spring 扩展。
	 *
	 * @param pluginId 插件 ID
	 */
	public void injectExtensions(String pluginId) {
		for (Class<?> extensionClass : springPluginManager.getExtensionClasses(pluginId)) {
			registerExtension(pluginId, extensionClass);
		}
	}

	/**
	 * 移除指定插件贡献的全部 Spring 扩展。
	 *
	 * @param pluginId 插件 ID
	 */
	public void removeExtensions(String pluginId) {
		pluginBeanRegistry.removePlugin(pluginId);
	}

	/**
	 * 移除注入器当前跟踪的全部插件扩展。
	 */
	public void removeAllExtensions() {
		pluginBeanRegistry.removeAll();
	}

	/**
	 * 将单个 PF4J 扩展注册到 Spring 容器。
	 *
	 * <p>没有 Spring 组件注解的扩展会被忽略；同名普通单例会先销毁再替换，Controller 的替换
	 * 交由动态注册器处理。</p>
	 *
	 * @param extensionClass 待检查和注册的扩展实现类型
	 * @throws NullPointerException 当扩展工厂返回 {@code null} 时抛出
	 */
	@Override
	protected void registerExtension(Class<?> extensionClass) {
		org.pf4j.PluginWrapper plugin = springPluginManager.whichPlugin(extensionClass);
		String pluginId = Objects.nonNull(plugin) ? plugin.getPluginId() : "classpath";
		registerExtension(pluginId, extensionClass);
	}

	/**
	 * 将指定插件的单个扩展注册到 Spring 容器。
	 *
	 * @param pluginId 插件 ID
	 * @param extensionClass 扩展实现类型
	 */
	private void registerExtension(String pluginId, Class<?> extensionClass) {
		if (!InjectorUtils.isInjectNecessary(extensionClass)) {
			return;
		}
		Object extension = Objects.requireNonNull(springPluginManager.getExtensionFactory().create(extensionClass),
				"PF4J extension factory returned null for " + extensionClass.getName());
		String beanName = InjectorUtils.getBeanName(extensionClass, extensionClass.getName());
		pluginBeanRegistry.register(pluginId, beanName, extension, InjectorUtils.isController(extensionClass));
	}

}
