package org.pf4j.core.extension.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

/**
 * 插件健康检查结果。
 *
 * <p>结果只包含宿主类加载器可见的基础类型，避免健康状态缓存持有插件对象。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public final class PluginHealth {

    /** 插件健康状态。 */
    public enum Status {
        /** 插件健康且可以接收请求。 */
        UP,
        /** 插件可用但能力降级。 */
        DEGRADED,
        /** 插件不可用。 */
        DOWN,
        /** 插件没有提供可判定状态。 */
        UNKNOWN
    }

    /** 当前健康状态。 */
    private final Status status;

    /** 面向运维的简短说明。 */
    private final String message;

    /** 不包含敏感信息的健康详情。 */
    private final Map<String, String> details;

    /**
     * 创建插件健康结果。
     *
     * @param status 健康状态
     * @param message 健康说明
     * @param details 健康详情
     */
    public PluginHealth(Status status, String message, Map<String, String> details) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.message = message;
        Map<String, String> source = Objects.isNull(details) ? Collections.<String, String>emptyMap() : details;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<String, String>(source));
    }

    /**
     * 创建健康结果。
     *
     * @param message 健康说明
     * @return 健康结果
     */
    public static PluginHealth up(String message) {
        return new PluginHealth(Status.UP, message, Collections.<String, String>emptyMap());
    }

    /**
     * 创建不可用结果。
     *
     * @param message 不可用说明
     * @return 不可用结果
     */
    public static PluginHealth down(String message) {
        return new PluginHealth(Status.DOWN, message, Collections.<String, String>emptyMap());
    }

    /**
     * 判断插件是否完全健康。
     *
     * @return 状态为 {@link Status#UP} 时返回 {@code true}
     */
    public boolean isHealthy() {
        return Status.UP.equals(status);
    }
}
