package org.pf4j.spring.boot.ext.update;

import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.update.FileDownloader;
import org.pf4j.update.FileVerifier;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.PluginInfo.PluginRelease;
import org.pf4j.update.SimpleFileDownloader;
import org.pf4j.update.UpdateRepository;
import org.pf4j.update.util.LenientDateTypeAdapter;
import org.pf4j.update.verifier.CompoundVerifier;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

@Slf4j
public class RestTemplateUpdateRepository implements UpdateRepository {

	private static final String REST_REPOSITORY = "rest";

	private final String id;
	private final String url;
	private final RestTemplate restTemplate;
	private volatile Map<String, PluginInfo> plugins;

	public RestTemplateUpdateRepository(String url, RestTemplate restTemplate) {
		this(REST_REPOSITORY, url, restTemplate);
	}

	public RestTemplateUpdateRepository(String id, String url, RestTemplate restTemplate) {
		if (!StringUtils.hasText(id)) {
			throw new IllegalArgumentException("id must not be blank");
		}
		if (!StringUtils.hasText(url)) {
			throw new IllegalArgumentException("url must not be blank");
		}
		this.id = id;
		this.url = url;
		this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public URL getUrl() {
		try {
			return URI.create(url).toURL();
		} catch (IllegalArgumentException | MalformedURLException e) {
			log.warn("Invalid PF4J update repository URL '{}'", url, e);
			return null;
		}
	}

	@Override
	public Map<String, PluginInfo> getPlugins() {
		Map<String, PluginInfo> currentPlugins = plugins;
		if (Objects.isNull(currentPlugins)) {
			synchronized (this) {
				currentPlugins = plugins;
				if (Objects.isNull(currentPlugins)) {
					currentPlugins = loadPlugins();
					plugins = currentPlugins;
				}
			}
		}
		return currentPlugins;
	}

	private Map<String, PluginInfo> loadPlugins() {
		try {
			String json = getRestTemplate().getForObject(url, String.class);
			if (!StringUtils.hasText(json)) {
				return Collections.emptyMap();
			}
			log.debug("Read plugins of '{}' repository from '{}'", id, url);
			return parsePlugins(new StringReader(json));
		} catch (Exception e) {
			log.error("Failed to read plugins of '{}' repository from '{}'", id, url, e);
			return Collections.emptyMap();
		}
	}

	private Map<String, PluginInfo> parsePlugins(Reader pluginsJsonReader) {
		Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new LenientDateTypeAdapter()).create();
		PluginInfo[] items = gson.fromJson(pluginsJsonReader, PluginInfo[].class);
		if (Objects.isNull(items) || items.length == 0) {
			return Collections.emptyMap();
		}
		Map<String, PluginInfo> loadedPlugins = new HashMap<String, PluginInfo>(items.length);
		URL repositoryUrl = getUrl();
		for (PluginInfo pluginInfo : items) {
			if (Objects.isNull(pluginInfo) || !StringUtils.hasText(pluginInfo.id)) {
				continue;
			}
			if (Objects.isNull(pluginInfo.releases)) {
				pluginInfo.releases = Collections.emptyList();
			}
			for (PluginRelease release : pluginInfo.releases) {
				if (Objects.isNull(release)) {
					continue;
				}
				try {
					if (Objects.nonNull(repositoryUrl) && StringUtils.hasText(release.url)) {
						release.url = new URL(repositoryUrl, release.url).toString();
					}
					if (Objects.nonNull(release.date) && release.date.getTime() == 0) {
						log.warn("Illegal release date when parsing {}@{}, setting to epoch", pluginInfo.id,
								release.version);
					}
				} catch (MalformedURLException e) {
					log.warn("Skipping release {} of plugin {} due to failure to build valid absolute URL. Url was {}{}",
							release.version, pluginInfo.id, repositoryUrl, release.url);
				}
			}
			pluginInfo.setRepositoryId(getId());
			loadedPlugins.put(pluginInfo.id, pluginInfo);
		}
		log.debug("Found {} plugins in repository '{}'", loadedPlugins.size(), id);
		return Collections.unmodifiableMap(loadedPlugins);
	}

	@Override
	public PluginInfo getPlugin(String id) {
		return getPlugins().get(id);
	}

	@Override
	public synchronized void refresh() {
		plugins = null;
	}

	@Override
	public FileDownloader getFileDownloader() {
		return new SimpleFileDownloader();
	}

	@Override
	public FileVerifier getFileVerifier() {
		return new CompoundVerifier();
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

}
