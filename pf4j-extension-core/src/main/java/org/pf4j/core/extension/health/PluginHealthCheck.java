package org.pf4j.core.extension.health;

import org.pf4j.ExtensionPoint;

/**
 * 插件健康检查扩展点。
 *
 * <p>插件可以提供一个或多个实现，宿主在启动、升级提交和周期巡检时执行。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public interface PluginHealthCheck extends ExtensionPoint {

    /**
     * 执行插件健康检查。
     *
     * @return 当前插件健康结果
     */
    PluginHealth checkHealth();
}
