package org.pf4j.update.extension.security;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.update.FileVerifier;
import org.pf4j.update.VerifyException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 插件下载准入与压缩制品安全策略测试。
 */
class PluginArtifactSecurityTest {

    /** JUnit 为每个测试创建的临时目录。 */
    @TempDir
    Path temporaryDirectory;

    /**
     * 验证不允许的协议会在调用实际下载器前被拒绝。
     *
     * @throws Exception URL 创建失败时抛出
     */
    @Test
    void shouldRejectProtocolBeforeDownload() throws Exception {
        AtomicBoolean delegated = new AtomicBoolean(false);
        SecureFileDownloader downloader = new SecureFileDownloader(url -> {
            delegated.set(true);
            return temporaryDirectory.resolve("plugin.jar");
        }, DownloadPolicy.secureDefaults());

        assertThrows(IOException.class, () -> downloader.downloadFile(new URL("http://example.com/plugin.jar")));
        assertFalse(delegated.get());
    }

    /**
     * 验证尺寸拒绝不会删除委托下载器返回的本地 Maven 仓库文件。
     *
     * @throws Exception 测试文件或 URL 创建失败时抛出
     */
    @Test
    void shouldPreserveDelegateFileWhenSizeLimitIsExceeded() throws Exception {
        Path localArtifact = Files.write(temporaryDirectory.resolve("plugin.jar"), new byte[]{1, 2});
        DownloadPolicy policy = new DownloadPolicy(Collections.singleton("file"), 1, 10, 10, false);
        SecureFileDownloader downloader = new SecureFileDownloader(url -> localArtifact, policy);

        assertThrows(IOException.class, () -> downloader.downloadFile(localArtifact.toUri().toURL()));
        assertTrue(Files.exists(localArtifact));
    }

    /**
     * 验证路径穿越和插件自带 PF4J 核心类都会被拒绝。
     *
     * @throws Exception 创建或读取测试压缩包失败时抛出
     */
    @Test
    void shouldRejectUnsafeArchiveEntriesAndBundledPf4jClasses() throws Exception {
        DownloadPolicy policy = new DownloadPolicy(new LinkedHashSet<String>(Collections.singleton("file")),
                1024, 10, 1024, false);
        ArchiveStructureVerificationPolicy verifier = new ArchiveStructureVerificationPolicy(policy);
        FileVerifier.Context context = context("file:/plugin.jar", null);
        Path traversal = archive("traversal.jar", "../outside.class");
        Path bundledApi = archive("bundled.jar", "classes/org/pf4j/PluginManager.class");

        assertThrows(VerifyException.class, () -> verifier.verify(context, traversal));
        assertThrows(VerifyException.class, () -> verifier.verify(context, bundledApi));
    }

    /**
     * 验证生产发布策略要求仓库元数据声明 SHA-512 摘要。
     *
     * @throws Exception 创建测试文件失败时抛出
     */
    @Test
    void shouldRequireChecksumInReleaseMetadata() throws Exception {
        Path artifact = Files.write(temporaryDirectory.resolve("release.jar"), new byte[]{1});
        ReleaseVerificationPolicy verifier = new ReleaseVerificationPolicy(DownloadPolicy.secureDefaults());

        assertThrows(VerifyException.class,
                () -> verifier.verify(context(artifact.toUri().toString(), null), artifact));
    }

    /**
     * 创建只包含一个条目的测试压缩包。
     *
     * @param fileName 压缩包文件名
     * @param entryName 压缩条目名称
     * @return 测试压缩包路径
     * @throws IOException 写入失败时抛出
     */
    private Path archive(String fileName, String entryName) throws IOException {
        Path archive = temporaryDirectory.resolve(fileName);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new ZipEntry(entryName));
            output.write("class".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return archive;
    }

    /**
     * 创建 PF4J 文件验证上下文。
     *
     * @param url 发布 URL
     * @param sha512sum SHA-512 摘要
     * @return 文件验证上下文
     */
    private FileVerifier.Context context(String url, String sha512sum) {
        return new FileVerifier.Context("demo-plugin", new Date(), "1.0.0", "*", url, sha512sum);
    }
}
