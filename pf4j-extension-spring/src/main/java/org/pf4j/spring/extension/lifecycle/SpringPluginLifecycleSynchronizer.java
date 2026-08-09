package org.pf4j.spring.extension.lifecycle;

import java.util.Objects;

import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.pf4j.spring.extension.ExtendedExtensionsInjector;

/**
 * PF4J 插件状态与 Spring Bean 生命周期同步器。
 *
 * <p>插件启动时注入其 Spring 扩展，插件离开启动状态时移除其全部 Bean 和 Controller。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Synchronizer that coordinates plugin lifecycle with Spring application context lifecycle events.
 */
class SpringPluginLifecycleSynchronizer implements PluginStateListener {

    /** 执行插件扩展注册和移除的注入器。 */
    private final ExtendedExtensionsInjector extensionsInjector;

    /**
     * 创建 Spring 插件生命周期同步器。
     *
     * @param extensionsInjector 扩展注入器
     */
    public SpringPluginLifecycleSynchronizer(ExtendedExtensionsInjector extensionsInjector) {
        this.extensionsInjector = Objects.requireNonNull(extensionsInjector,
                "extensionsInjector must not be null");
    }

    /**
     * 根据插件新状态注册或移除 Spring 扩展。
     *
     * @param event PF4J 插件状态事件
     */
    @Override
    public void pluginStateChanged(PluginStateEvent event) {
        String pluginId = event.getPlugin().getPluginId();
        if (event.getPluginState().isStarted()) {
            extensionsInjector.injectExtensions(pluginId);
        } else {
            extensionsInjector.removeExtensions(pluginId);
        }
    }

    /**
     * 移除当前注册的全部插件扩展。
     */
    public void removeAll() {
        extensionsInjector.removeAllExtensions();
    }
}
