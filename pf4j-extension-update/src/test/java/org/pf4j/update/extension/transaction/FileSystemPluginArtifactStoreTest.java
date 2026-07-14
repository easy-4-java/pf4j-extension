package org.pf4j.update.extension.transaction;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FileSystemPluginArtifactStore} 原子激活与备份恢复测试。
 */
class FileSystemPluginArtifactStoreTest {

    /** JUnit 为测试创建的临时目录。 */
    @TempDir
    Path temporaryDirectory;

    /**
     * 验证更新制品激活后可以从备份恢复旧内容。
     *
     * @throws Exception 文件操作失败时抛出
     */
    @Test
    void shouldBackupActivateAndRestoreArtifact() throws Exception {
        Path pluginsRoot = temporaryDirectory.resolve("plugins");
        Path backupRoot = temporaryDirectory.resolve("backup");
        Files.createDirectories(pluginsRoot);
        Path current = Files.write(pluginsRoot.resolve("demo.jar"), "old".getBytes(StandardCharsets.UTF_8));
        Path update = Files.write(temporaryDirectory.resolve("update.jar"), "new".getBytes(StandardCharsets.UTF_8));
        FileSystemPluginArtifactStore store = new FileSystemPluginArtifactStore(pluginsRoot, backupRoot);

        Path backup = store.backup(current, "demo-plugin", "1.0.0");
        Path active = store.activate(update, "demo.jar");
        assertEquals("new", new String(Files.readAllBytes(active), StandardCharsets.UTF_8));

        store.restore(backup, current);
        store.deleteBackup(backup);

        assertEquals("old", new String(Files.readAllBytes(current), StandardCharsets.UTF_8));
        assertFalse(Files.exists(backup));
        assertTrue(Files.isRegularFile(current));
    }
}
