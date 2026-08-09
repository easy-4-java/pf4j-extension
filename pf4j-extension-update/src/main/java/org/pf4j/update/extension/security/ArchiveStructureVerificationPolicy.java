package org.pf4j.update.extension.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.pf4j.update.FileVerifier;
import org.pf4j.update.VerifyException;

/**
 * ZIP/JAR 插件结构、解压规模和禁止类前缀验证策略。
 *
 * <p>策略拒绝路径穿越、过多条目、过大声明解压量，以及插件制品中重复携带 PF4J 核心或
 * 宿主明确禁止的 API 包。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Policy that verifies the internal structure of plugin archive files for integrity.
 */
class ArchiveStructureVerificationPolicy implements ArtifactVerificationPolicy {

    /** 下载资源限制策略。 */
    private final DownloadPolicy policy;

    /** 禁止出现在插件制品中的类资源前缀。 */
    private final List<String> forbiddenClassPrefixes;

    /**
     * 创建默认压缩结构策略。
     *
     * @param policy 下载资源限制策略
     */
    public ArchiveStructureVerificationPolicy(DownloadPolicy policy) {
        this(policy, Collections.singletonList("org/pf4j/"));
    }

    /**
     * 创建包含宿主禁止包前缀的压缩结构策略。
     *
     * @param policy 下载资源限制策略
     * @param forbiddenClassPrefixes 禁止类资源前缀
     */
    public ArchiveStructureVerificationPolicy(DownloadPolicy policy, List<String> forbiddenClassPrefixes) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.forbiddenClassPrefixes = Collections.unmodifiableList(
                new ArrayList<String>(Objects.requireNonNull(forbiddenClassPrefixes,
                        "forbiddenClassPrefixes must not be null")));
    }

    /**
     * 验证 ZIP/JAR 条目结构和资源限制。
     *
     * @param context PF4J 发布上下文
     * @param file 本地制品路径
     * @throws IOException 读取压缩文件失败时抛出
     * @throws VerifyException 压缩结构不满足策略时抛出
     */
    @Override
    public void verify(FileVerifier.Context context, Path file) throws IOException, VerifyException {
        String lowerName = file.getFileName().toString().toLowerCase();
        if (!lowerName.endsWith(".zip") && !lowerName.endsWith(".jar")) {
            throw new VerifyException("Plugin artifact must be a ZIP or JAR file");
        }
        int entries = 0;
        long uncompressedSize = 0;
        byte[] buffer = new byte[8192];
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            java.util.Enumeration<? extends ZipEntry> values = zipFile.entries();
            while (values.hasMoreElements()) {
                ZipEntry entry = values.nextElement();
                entries++;
                if (entries > policy.getMaximumArchiveEntries()) {
                    throw new VerifyException("Plugin archive contains too many entries: " + entries);
                }
                String normalized = entry.getName().replace('\\', '/');
                if (isUnsafePath(normalized)) {
                    throw new VerifyException("Plugin archive contains unsafe entry: " + entry.getName());
                }
                if (!entry.isDirectory()) {
                    try (InputStream input = zipFile.getInputStream(entry)) {
                        int length;
                        while ((length = input.read(buffer)) >= 0) {
                            uncompressedSize += length;
                            if (uncompressedSize > policy.getMaximumUncompressedSize()) {
                                throw new VerifyException("Plugin archive exceeds maximum uncompressed size");
                            }
                        }
                    }
                }
                String classResource = normalized.startsWith("classes/")
                        ? normalized.substring("classes/".length()) : normalized;
                if (classResource.endsWith(".class") && isForbidden(classResource)) {
                    throw new VerifyException("Plugin archive contains forbidden class: " + classResource);
                }
            }
        }
    }

    /**
     * 判断类资源是否命中禁止前缀。
     *
     * @param classResource 类资源路径
     * @return 命中禁止前缀时返回 {@code true}
     */
    private boolean isForbidden(String classResource) {
        for (String prefix : forbiddenClassPrefixes) {
            if (classResource.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断压缩条目是否可能逃逸解压根目录。
     *
     * @param normalized 使用正斜杠分隔的条目路径
     * @return 绝对路径、盘符路径或包含父目录跳转时返回 {@code true}
     */
    private boolean isUnsafePath(String normalized) {
        return normalized.startsWith("/") || normalized.startsWith("../") || normalized.contains("/../")
                || "..".equals(normalized) || normalized.matches("^[A-Za-z]:/.*");
    }
}
