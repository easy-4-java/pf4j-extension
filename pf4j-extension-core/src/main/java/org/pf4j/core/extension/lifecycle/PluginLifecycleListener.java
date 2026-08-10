package org.pf4j.core.extension.lifecycle;

/**
 * 插件生命周期操作监听器。
 *
 * <p>监听器在操作完成后同步调用。实现方应快速返回并自行隔离慢速 I/O；监听器异常会被
 * 生命周期管理器记录并忽略，不会反向改变插件状态。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public interface PluginLifecycleListener {

    /**
     * 处理生命周期操作结果。
     *
     * @param result 已完成的插件生命周期操作结果
     */
    void onCompleted(PluginOperationResult result);
}
