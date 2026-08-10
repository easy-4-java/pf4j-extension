package org.pf4j.update.extension.transaction;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 事务式插件更新使用的制品存储接口。
 *
 * <p>实现负责备份当前制品、将已验证制品原子激活到插件目录，以及在失败时恢复备份。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public interface PluginArtifactStore {

    /**
     * 备份当前插件制品。
     *
     * @param currentArtifact 当前制品路径
     * @param pluginId 插件 ID
     * @param version 当前版本
     * @return 备份制品路径
     * @throws IOException 备份失败时抛出
     */
    Path backup(Path currentArtifact, String pluginId, String version) throws IOException;

    /**
     * 将已验证制品激活到插件目录。
     *
     * @param verifiedArtifact 已验证制品路径
     * @param targetFileName 激活后的文件名
     * @return 插件目录中的激活路径
     * @throws IOException 激活失败时抛出
     */
    Path activate(Path verifiedArtifact, String targetFileName) throws IOException;

    /**
     * 将备份制品恢复到目标路径。
     *
     * @param backupArtifact 备份制品路径
     * @param targetArtifact 目标插件路径
     * @throws IOException 恢复失败时抛出
     */
    void restore(Path backupArtifact, Path targetArtifact) throws IOException;

    /**
     * 删除不再需要的备份制品。
     *
     * @param backupArtifact 备份制品路径
     * @throws IOException 删除失败时抛出
     */
    void deleteBackup(Path backupArtifact) throws IOException;

    /**
     * 删除失败安装留下的激活制品。
     *
     * @param artifact 激活制品路径
     * @throws IOException 删除失败时抛出
     */
    void deleteArtifact(Path artifact) throws IOException;
}
