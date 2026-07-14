package org.pf4j.update.extension;

import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.pf4j.update.FileDownloader;
import org.pf4j.update.SimpleFileDownloader;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.util.StringUtils;

/**
 * 同时支持普通远程 URL 和 Maven 坐标伪 URL 的插件文件下载器。
 *
 * <p>HTTP 等非 {@code file} 协议委托 PF4J 默认下载器处理。对于 {@code file} 协议，优先返回
 * 已存在的本地文件；文件不存在时，将路径转换为 Maven 坐标并交由 Spring Cloud Deployer
 * 的 {@link MavenResource} 从配置的仓库解析。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public class MavenFileDownloader implements FileDownloader {

	/**
	 * Maven 本地仓库、远程仓库和认证等解析配置。
	 */
	private final MavenProperties mavenProperties;

	/**
	 * 处理 HTTP、HTTPS 等普通远程 URL 的 PF4J 下载器。
	 */
	private final FileDownloader remoteFileDownloader = new SimpleFileDownloader();

	/**
	 * 创建 Maven 感知的插件文件下载器。
	 *
	 * @param mavenProperties Maven 资源解析配置
	 * @throws NullPointerException 当 {@code mavenProperties} 为 {@code null} 时抛出
	 */
	public MavenFileDownloader(MavenProperties mavenProperties) {
		this.mavenProperties = Objects.requireNonNull(mavenProperties, "mavenProperties must not be null");
	}

	/**
	 * 下载或解析指定插件文件。
	 *
	 * @param fileUrl 插件文件 URL
	 * @return 已下载或从本地/Maven 仓库解析到的文件路径
	 * @throws IOException 当远程下载失败、Maven 坐标无效或 Maven 资源无法解析时抛出
	 * @throws NullPointerException 当 {@code fileUrl} 为 {@code null} 时抛出
	 */
	@Override
	public Path downloadFile(URL fileUrl) throws IOException {
		Objects.requireNonNull(fileUrl, "fileUrl must not be null");
		if (!"file".equalsIgnoreCase(fileUrl.getProtocol())) {
			return remoteFileDownloader.downloadFile(fileUrl);
		}
		Path localFile = getLocalFile(fileUrl);
		if (Objects.nonNull(localFile) && Files.exists(localFile)) {
			return localFile;
		}
		return downloadMavenArtifact(fileUrl);
	}

	/**
	 * 尝试把 {@code file} URL 转换为本地文件路径。
	 *
	 * @param fileUrl 文件协议 URL
	 * @return 可解析的本地路径；URL 不是合法文件 URI 时返回 {@code null}
	 */
	private Path getLocalFile(URL fileUrl) {
		try {
			return Paths.get(fileUrl.toURI());
		} catch (IllegalArgumentException | URISyntaxException e) {
			return null;
		}
	}

	/**
	 * 将文件 URL 的路径部分转换为 Maven 坐标并解析制品。
	 *
	 * @param fileUrl 使用斜杠分隔 Maven 坐标片段的文件 URL
	 * @return Maven 资源解析得到的本地制品路径
	 * @throws IOException 当坐标为空或 Maven 资源下载失败时抛出
	 */
	private Path downloadMavenArtifact(URL fileUrl) throws IOException {
		String path = fileUrl.toExternalForm().substring("file:".length()).replaceFirst("^//", "");
		String coordinates = path.replace("/", ":");
		if (!StringUtils.hasText(coordinates)) {
			throw new IOException("Maven coordinates must not be blank");
		}
		return MavenResource.parse(coordinates, mavenProperties).getFile().toPath();
	}

}
