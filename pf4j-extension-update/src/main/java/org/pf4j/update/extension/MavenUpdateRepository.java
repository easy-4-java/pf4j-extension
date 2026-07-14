package org.pf4j.update.extension;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.pf4j.update.FileDownloader;
import org.pf4j.update.FileVerifier;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.UpdateRepository;
import org.pf4j.update.verifier.CompoundVerifier;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.util.CollectionUtils;

public class MavenUpdateRepository implements UpdateRepository {

	private static final String MAVEN_REPOSITORY = "maven";

	private volatile Map<String, PluginInfo> plugins;
	private final MavenProperties mavenProperties;
	private final PluginInfoProvider pluginInfoProvider;

	public MavenUpdateRepository(MavenProperties mavenProperties, PluginInfoProvider pluginInfoProvider) {
		this.mavenProperties = Objects.requireNonNull(mavenProperties, "mavenProperties must not be null");
		this.pluginInfoProvider = Objects.requireNonNull(pluginInfoProvider, "pluginInfoProvider must not be null");
	}

	@Override
	public String getId() {
		return MAVEN_REPOSITORY;
	}

	@Override
	public URL getUrl() {
		return null;
	}

	@Override
	public Map<String, PluginInfo> getPlugins() {
		Map<String, PluginInfo> currentPlugins = plugins;
		if (Objects.isNull(currentPlugins)) {
			synchronized (this) {
				currentPlugins = plugins;
				if (Objects.isNull(currentPlugins)) {
					Map<String, PluginInfo> providedPlugins = getPluginInfoProvider().plugins();
					if (CollectionUtils.isEmpty(providedPlugins)) {
						currentPlugins = Collections.emptyMap();
					} else {
						Map<String, PluginInfo> repositoryPlugins = new LinkedHashMap<String, PluginInfo>(providedPlugins);
						for (PluginInfo info : repositoryPlugins.values()) {
							if (Objects.nonNull(info)) {
								info.setRepositoryId(MAVEN_REPOSITORY);
							}
						}
						currentPlugins = Collections.unmodifiableMap(repositoryPlugins);
					}
					plugins = currentPlugins;
				}
			}
		}
		return currentPlugins;
	}

	@Override
	public PluginInfo getPlugin(String coordinates) {
		return getPlugins().get(coordinates);
	}

	@Override
	public synchronized void refresh() {
		plugins = null;
	}

	@Override
	public FileDownloader getFileDownloader() {
		return new MavenFileDownloader(mavenProperties);
	}

	@Override
	public FileVerifier getFileVerifier() {
		return new CompoundVerifier();
	}

	public PluginInfoProvider getPluginInfoProvider() {
		return pluginInfoProvider;
	}

}
