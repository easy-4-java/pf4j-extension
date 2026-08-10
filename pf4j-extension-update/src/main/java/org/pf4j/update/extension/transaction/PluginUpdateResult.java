package org.pf4j.update.extension.transaction;

import java.nio.file.Path;

import lombok.Getter;

/**
 * 事务式插件安装或更新结果。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public final class PluginUpdateResult {

    /** 更新操作类型。 */
    public enum Operation {
        /** 首次安装插件。 */
        INSTALL,
        /** 更新已安装插件。 */
        UPDATE
    }

    /** 操作类型。 */
    private final Operation operation;

    /** 插件 ID。 */
    private final String pluginId;

    /** 操作前版本。 */
    private final String previousVersion;

    /** 操作后目标版本。 */
    private final String currentVersion;

    /** 当前激活制品路径。 */
    private final Path artifactPath;

    /** 操作是否成功。 */
    private final boolean success;

    /** 失败后是否成功恢复旧版本。 */
    private final boolean rolledBack;

    /** 操作失败原因。 */
    private final Throwable failure;

    /**
     * 创建插件更新结果。
     *
     * @param operation 操作类型
     * @param pluginId 插件 ID
     * @param previousVersion 操作前版本
     * @param currentVersion 当前版本
     * @param artifactPath 激活制品路径
     * @param success 是否成功
     * @param rolledBack 是否已回滚
     * @param failure 失败原因
     */
    public PluginUpdateResult(Operation operation, String pluginId, String previousVersion, String currentVersion,
                              Path artifactPath, boolean success, boolean rolledBack, Throwable failure) {
        this.operation = operation;
        this.pluginId = pluginId;
        this.previousVersion = previousVersion;
        this.currentVersion = currentVersion;
        this.artifactPath = artifactPath;
        this.success = success;
        this.rolledBack = rolledBack;
        this.failure = failure;
    }
}
