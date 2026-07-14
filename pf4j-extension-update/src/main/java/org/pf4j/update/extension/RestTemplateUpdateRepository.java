package org.pf4j.update.extension;

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
import lombok.Getter;
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

/**
 * 基于 Spring {@link RestTemplate} 的 PF4J JSON 更新仓库。
 *
 * <p>首次访问时从远程地址读取 PF4J 插件元数据数组，将相对发布地址解析为绝对 URL，
 * 设置仓库 ID 后构建不可修改缓存。网络、反序列化或数据处理失败会记录日志并降级为空仓库，
 * 避免更新服务故障影响应用主流程。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public class RestTemplateUpdateRepository implements UpdateRepository {

	/**
	 * 未显式指定仓库 ID 时使用的默认值。
	 */
	private static final String REST_REPOSITORY = "rest";

	/**
	 * 当前更新仓库的唯一标识。
	 */
	private final String id;

	/**
	 * 远程插件元数据 JSON 地址。
	 */
	private final String url;

	/**
	 * 执行远程元数据请求的 Spring HTTP 客户端。
	 */
	@Getter
	private final RestTemplate restTemplate;

	/**
	 * 下载插件发布制品的文件下载器。
	 */
	private final FileDownloader fileDownloader;

	/**
	 * 校验下载制品的文件验证器。
	 */
	private final FileVerifier fileVerifier;

	/**
	 * 延迟初始化的只读插件元数据缓存；调用 {@link #refresh()} 后重置。
	 */
	private volatile Map<String, PluginInfo> plugins;

	/**
	 * 使用默认仓库 ID 创建 REST 更新仓库。
	 *
	 * @param url 远程插件元数据 JSON 地址
	 * @param restTemplate 执行 HTTP 请求的客户端
	 * @throws IllegalArgumentException 当 {@code url} 为空时抛出
	 * @throws NullPointerException 当 {@code restTemplate} 为 {@code null} 时抛出
	 */
	public RestTemplateUpdateRepository(String url, RestTemplate restTemplate) {
		this(REST_REPOSITORY, url, restTemplate);
	}

	/**
	 * 使用指定仓库 ID 创建 REST 更新仓库。
	 *
	 * @param id 仓库唯一标识
	 * @param url 远程插件元数据 JSON 地址
	 * @param restTemplate 执行 HTTP 请求的客户端
	 * @throws IllegalArgumentException 当 {@code id} 或 {@code url} 为空时抛出
	 * @throws NullPointerException 当 {@code restTemplate} 为 {@code null} 时抛出
	 */
	public RestTemplateUpdateRepository(String id, String url, RestTemplate restTemplate) {
		this(id, url, restTemplate, new SimpleFileDownloader(), new CompoundVerifier());
	}

	/**
	 * 使用指定下载器和验证器创建 REST 更新仓库。
	 *
	 * @param id 仓库唯一标识
	 * @param url 远程插件元数据 JSON 地址
	 * @param restTemplate 执行 HTTP 请求的客户端
	 * @param fileDownloader 插件文件下载器
	 * @param fileVerifier 插件文件验证器
	 */
	public RestTemplateUpdateRepository(String id, String url, RestTemplate restTemplate,
										FileDownloader fileDownloader, FileVerifier fileVerifier) {
		if (!StringUtils.hasText(id)) {
			throw new IllegalArgumentException("id must not be blank");
		}
		if (!StringUtils.hasText(url)) {
			throw new IllegalArgumentException("url must not be blank");
		}
		this.id = id;
		this.url = url;
		this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");
		this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader must not be null");
		this.fileVerifier = Objects.requireNonNull(fileVerifier, "fileVerifier must not be null");
	}

	/**
	 * 获取仓库 ID。
	 *
	 * @return 构造仓库时指定的 ID
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * 将配置的仓库地址转换为 URL。
	 *
	 * @return 合法仓库 URL；配置地址无法转换时返回 {@code null}
	 */
	@Override
	public URL getUrl() {
		try {
			return URI.create(url).toURL();
		} catch (IllegalArgumentException | MalformedURLException e) {
			log.warn("Invalid PF4J update repository URL '{}'", url, e);
			return null;
		}
	}

	/**
	 * 获取仓库中的全部插件元数据。
	 *
	 * <p>首次访问使用双重检查加载远程数据，后续访问返回同一缓存实例。</p>
	 *
	 * @return 不可修改的插件元数据映射；远程加载失败时返回空映射
	 */
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

	/**
	 * 从远程仓库读取并解析插件 JSON。
	 *
	 * @return 解析后的不可修改插件映射；响应为空或发生异常时返回空映射
	 */
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

	/**
	 * 解析并规范化 PF4J 插件元数据。
	 *
	 * <p>忽略空插件和缺少 ID 的插件，为缺失的发布列表设置空列表，并尝试将相对发布地址转换为
	 * 基于仓库 URL 的绝对地址。无效发布 URL 会保留原值并记录警告。</p>
	 *
	 * @param pluginsJsonReader 包含 PF4J {@link PluginInfo} 数组的 JSON 字符流
	 * @return 以插件 ID 为键的不可修改插件映射
	 */
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

	/**
	 * 按插件 ID 获取元数据。
	 *
	 * @param id 插件 ID
	 * @return 对应插件元数据；不存在时返回 {@code null}
	 */
	@Override
	public PluginInfo getPlugin(String id) {
		return getPlugins().get(id);
	}

	/**
	 * 使当前远程插件元数据缓存失效。
	 *
	 * <p>下一次调用 {@link #getPlugins()} 时会重新请求远程地址。</p>
	 */
	@Override
	public synchronized void refresh() {
		plugins = null;
	}

	/**
	 * 创建用于下载插件发布文件的默认下载器。
	 *
	 * @return 新的 PF4J 简单文件下载器
	 */
	@Override
	public FileDownloader getFileDownloader() {
		return fileDownloader;
	}

	/**
	 * 创建用于校验插件发布文件的组合校验器。
	 *
	 * @return 新的 PF4J 组合校验器
	 */
	@Override
	public FileVerifier getFileVerifier() {
		return fileVerifier;
	}

}
