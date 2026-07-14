package org.pf4j.spring.boot.ext.update;

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

public class MavenFileDownloader implements FileDownloader {

	private final MavenProperties mavenProperties;
	private final FileDownloader remoteFileDownloader = new SimpleFileDownloader();

	public MavenFileDownloader(MavenProperties mavenProperties) {
		this.mavenProperties = Objects.requireNonNull(mavenProperties, "mavenProperties must not be null");
	}

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

	private Path getLocalFile(URL fileUrl) {
		try {
			return Paths.get(fileUrl.toURI());
		} catch (IllegalArgumentException | URISyntaxException e) {
			return null;
		}
	}

	private Path downloadMavenArtifact(URL fileUrl) throws IOException {
		String path = fileUrl.toExternalForm().substring("file:".length()).replaceFirst("^//", "");
		String coordinates = path.replace("/", ":");
		if (!StringUtils.hasText(coordinates)) {
			throw new IOException("Maven coordinates must not be blank");
		}
		return MavenResource.parse(coordinates, mavenProperties).getFile().toPath();
	}

}
