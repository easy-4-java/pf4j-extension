package org.pf4j.core.extension.health;

import org.pf4j.ExtensionPoint;

/**
 * 插件就绪检查扩展点。
 *
 * <p>健康表示插件内部没有故障，就绪表示插件已经满足接收新业务请求的全部前置条件。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public interface PluginReadinessCheck extends ExtensionPoint {

    /**
     * 执行插件就绪检查。
     *
     * @return 当前插件就绪结果
     */
    PluginHealth checkReadiness();
}
