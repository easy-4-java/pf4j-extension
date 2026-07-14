package org.pf4j.core.extension.health;

import java.util.concurrent.TimeUnit;

import org.pf4j.ExtensionPoint;

/**
 * 插件流量摘除扩展点。
 *
 * <p>更新或停止插件前，宿主先调用 {@link #beginDrain()} 阻止新请求，再等待已有请求完成。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public interface PluginDrainHook extends ExtensionPoint {

    /**
     * 将插件切换到拒绝新请求的流量摘除状态。
     */
    void beginDrain();

    /**
     * 等待插件在途调用完成。
     *
     * @param timeout 最大等待时间
     * @param unit 时间单位
     * @return 在超时前完成流量摘除时返回 {@code true}
     * @throws InterruptedException 当前线程等待期间被中断时抛出
     */
    boolean awaitDrained(long timeout, TimeUnit unit) throws InterruptedException;
}
