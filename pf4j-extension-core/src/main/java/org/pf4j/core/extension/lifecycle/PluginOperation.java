package org.pf4j.core.extension.lifecycle;

/**
 * PF4J 插件生命周期操作类型。
 *
 * <p>用于统一描述加载、启动、停止、卸载、替换和回滚操作，便于监听器、审计和指标系统
 * 使用稳定的操作标识。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public enum PluginOperation {

    /** 加载插件。 */
    LOAD,

    /** 启动插件。 */
    START,

    /** 停止插件。 */
    STOP,

    /** 卸载插件。 */
    UNLOAD,

    /** 替换插件制品。 */
    REPLACE,

    /** 回滚插件制品和状态。 */
    ROLLBACK
}
