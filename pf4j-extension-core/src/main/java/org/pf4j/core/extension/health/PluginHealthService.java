package org.pf4j.core.extension.health;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

/**
 * 插件健康、就绪和流量摘除协调服务。
 *
 * <p>服务按插件查询 PF4J 扩展并聚合结果，不缓存插件扩展实例。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Service for managing and executing plugin health checks across the plugin ecosystem.
 */
class PluginHealthService {

    /** PF4J 插件管理器。 */
    private final PluginManager pluginManager;

    /**
     * 创建插件健康服务。
     *
     * @param pluginManager PF4J 插件管理器
     */
    public PluginHealthService(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
    }

    /**
     * 聚合指定插件的健康检查结果。
     *
     * @param pluginId 插件 ID
     * @return 聚合健康结果；插件未启动时返回不可用
     */
    public PluginHealth checkHealth(String pluginId) {
        PluginWrapper plugin = requireStartedPlugin(pluginId);
        if (Objects.isNull(plugin)) {
            return PluginHealth.down("Plugin is not started: " + pluginId);
        }
        List<PluginHealthCheck> checks = pluginManager.getExtensions(PluginHealthCheck.class, pluginId);
        if (checks.isEmpty()) {
            return PluginHealth.up("No health check declared");
        }
        return aggregateHealth(checks);
    }

    /**
     * 聚合指定插件的就绪检查结果。
     *
     * @param pluginId 插件 ID
     * @return 聚合就绪结果；插件未启动时返回不可用
     */
    public PluginHealth checkReadiness(String pluginId) {
        PluginWrapper plugin = requireStartedPlugin(pluginId);
        if (Objects.isNull(plugin)) {
            return PluginHealth.down("Plugin is not started: " + pluginId);
        }
        List<PluginReadinessCheck> checks = pluginManager.getExtensions(PluginReadinessCheck.class, pluginId);
        if (checks.isEmpty()) {
            return PluginHealth.up("No readiness check declared");
        }
        PluginHealth.Status status = PluginHealth.Status.UP;
        String message = "Plugin is ready";
        for (PluginReadinessCheck check : checks) {
            PluginHealth result = Objects.requireNonNull(check.checkReadiness(),
                    "Plugin readiness check returned null");
            if (PluginHealth.Status.DOWN.equals(result.getStatus())) {
                return result;
            }
            if (PluginHealth.Status.DEGRADED.equals(result.getStatus())) {
                status = PluginHealth.Status.DEGRADED;
                message = result.getMessage();
            }
        }
        return new PluginHealth(status, message, null);
    }

    /**
     * 开始流量摘除并等待指定插件的全部流量摘除钩子完成。
     *
     * @param pluginId 插件 ID
     * @param timeout 最大等待时间
     * @param unit 时间单位
     * @return 所有流量摘除钩子在超时前完成时返回 {@code true}
     * @throws InterruptedException 当前线程等待期间被中断时抛出
     */
    public boolean drain(String pluginId, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit, "unit must not be null");
        List<PluginDrainHook> hooks = pluginManager.getExtensions(PluginDrainHook.class, pluginId);
        for (PluginDrainHook hook : hooks) {
            hook.beginDrain();
        }
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (PluginDrainHook hook : hooks) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0 || !hook.awaitDrained(remaining, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查插件是否存在且已启动。
     *
     * @param pluginId 插件 ID
     * @return 已启动插件；不存在或未启动时返回 {@code null}
     */
    private PluginWrapper requireStartedPlugin(String pluginId) {
        if (StringUtils.isBlank(pluginId)) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        PluginWrapper plugin = pluginManager.getPlugin(pluginId);
        return Objects.nonNull(plugin) && plugin.getPluginState().isStarted() ? plugin : null;
    }

    /**
     * 聚合健康检查结果。
     *
     * @param checks 健康检查集合
     * @return 聚合健康结果
     */
    private PluginHealth aggregateHealth(List<PluginHealthCheck> checks) {
        PluginHealth.Status status = PluginHealth.Status.UP;
        String message = "Plugin is healthy";
        for (PluginHealthCheck check : checks) {
            PluginHealth result = Objects.requireNonNull(check.checkHealth(), "Plugin health check returned null");
            if (PluginHealth.Status.DOWN.equals(result.getStatus())) {
                return result;
            }
            if (PluginHealth.Status.DEGRADED.equals(result.getStatus())
                    || PluginHealth.Status.UNKNOWN.equals(result.getStatus())) {
                status = result.getStatus();
                message = result.getMessage();
            }
        }
        return new PluginHealth(status, message, null);
    }
}
