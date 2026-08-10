package org.pf4j.update.extension.transaction;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * 基于本地文件系统的插件制品存储。
 *
 * <p>激活和恢复先复制到目标目录临时文件，再优先使用原子移动替换正式制品，避免宿主观察到
 * 部分写入文件。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public final class FileSystemPluginArtifactStore implements PluginArtifactStore {

    /** PF4J 正式插件目录。 */
    private final Path pluginsRoot;

    /** 插件备份目录。 */
    private final Path backupRoot;

    /**
     * 创建文件系统插件制品存储。
     *
     * @param pluginsRoot PF4J 插件目录
     * @param backupRoot 插件备份目录
     */
    public FileSystemPluginArtifactStore(Path pluginsRoot, Path backupRoot) {
        this.pluginsRoot = Objects.requireNonNull(pluginsRoot, "pluginsRoot must not be null");
        this.backupRoot = Objects.requireNonNull(backupRoot, "backupRoot must not be null");
    }

    /**
     * 复制当前制品到版本化备份文件。
     *
     * @param currentArtifact 当前制品路径
     * @param pluginId 插件 ID
     * @param version 当前版本
     * @return 备份制品路径
     * @throws IOException 当前制品不是普通文件或复制失败时抛出
     */
    @Override
    public Path backup(Path currentArtifact, String pluginId, String version) throws IOException {
        requireRegularFile(currentArtifact);
        Files.createDirectories(backupRoot);
        String fileName = pluginId + "-" + version + "-" + System.currentTimeMillis() + "-"
                + currentArtifact.getFileName();
        return Files.copy(currentArtifact, backupRoot.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 将已验证制品原子激活到插件目录。
     *
     * @param verifiedArtifact 已验证制品路径
     * @param targetFileName 正式文件名
     * @return 正式插件路径
     * @throws IOException 制品无效、复制或移动失败时抛出
     */
    @Override
    public Path activate(Path verifiedArtifact, String targetFileName) throws IOException {
        requireRegularFile(verifiedArtifact);
        Files.createDirectories(pluginsRoot);
        Path target = pluginsRoot.resolve(targetFileName);
        copyAndReplace(verifiedArtifact, target);
        return target;
    }

    /**
     * 将备份制品原子恢复到原插件路径。
     *
     * @param backupArtifact 备份制品路径
     * @param targetArtifact 原插件路径
     * @throws IOException 恢复失败时抛出
     */
    @Override
    public void restore(Path backupArtifact, Path targetArtifact) throws IOException {
        requireRegularFile(backupArtifact);
        copyAndReplace(backupArtifact, targetArtifact);
    }

    /**
     * 删除备份制品。
     *
     * @param backupArtifact 备份制品路径
     * @throws IOException 删除失败时抛出
     */
    @Override
    public void deleteBackup(Path backupArtifact) throws IOException {
        if (Objects.nonNull(backupArtifact)) {
            Files.deleteIfExists(backupArtifact);
        }
    }

    /**
     * 删除激活制品。
     *
     * @param artifact 激活制品路径
     * @throws IOException 删除失败时抛出
     */
    @Override
    public void deleteArtifact(Path artifact) throws IOException {
        if (Objects.nonNull(artifact)) {
            Files.deleteIfExists(artifact);
        }
    }

    /**
     * 复制源文件并原子替换目标文件。
     *
     * @param source 源文件
     * @param target 目标文件
     * @throws IOException 复制或替换失败时抛出
     */
    private void copyAndReplace(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".staging");
        boolean moved = false;
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    /**
     * 校验路径表示普通文件。
     *
     * @param file 待校验文件
     * @throws IOException 文件不存在或不是普通文件时抛出
     */
    private void requireRegularFile(Path file) throws IOException {
        if (Objects.isNull(file) || !Files.isRegularFile(file)) {
            throw new IOException("Plugin artifact must be a regular file: " + file);
        }
    }
}
