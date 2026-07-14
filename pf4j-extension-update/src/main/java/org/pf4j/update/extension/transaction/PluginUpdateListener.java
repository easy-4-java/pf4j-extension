package org.pf4j.update.extension.transaction;

/**
 * 事务式插件更新完成监听器。
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public interface PluginUpdateListener {

    /**
     * 处理插件更新结果。
     *
     * @param result 插件更新结果
     */
    void onCompleted(PluginUpdateResult result);
}
