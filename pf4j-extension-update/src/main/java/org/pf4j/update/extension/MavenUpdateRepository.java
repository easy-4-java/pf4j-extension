package org.pf4j.update.extension;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import org.pf4j.update.FileDownloader;
import org.pf4j.update.FileVerifier;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.UpdateRepository;
import org.pf4j.update.verifier.CompoundVerifier;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.util.CollectionUtils;

/**
 * 基于 Maven 坐标元数据的 PF4J 更新仓库。
 *
 * <p>插件目录由 {@link PluginInfoProvider} 提供，并在首次访问时构建不可修改缓存。仓库使用
 * {@link MavenFileDownloader} 解析插件文件，使插件发布信息可以通过 Maven 仓库分发。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public class MavenUpdateRepository implements UpdateRepository {

	/**
	 * Maven 更新仓库的固定 PF4J 仓库 ID。
	 */
	private static final String MAVEN_REPOSITORY = "maven";

	/**
	 * 延迟初始化的只读插件元数据缓存；调用 {@link #refresh()} 后重置。
	 */
	private volatile Map<String, PluginInfo> plugins;

	/**
	 * 下载 Maven 插件制品所需的仓库配置。
	 */
	private final MavenProperties mavenProperties;

	/**
	 * 提供 Maven 坐标与插件发布元数据的数据源。
	 */
	@Getter
    private final PluginInfoProvider pluginInfoProvider;

	/** 插件制品下载器。 */
	private final FileDownloader fileDownloader;

	/** 插件制品验证器。 */
	private final FileVerifier fileVerifier;

	/**
	 * 创建 Maven 更新仓库。
	 *
	 * @param mavenProperties Maven 本地和远程仓库配置
	 * @param pluginInfoProvider 插件元数据提供器
	 * @throws NullPointerException 当任一参数为 {@code null} 时抛出
	 */
	public MavenUpdateRepository(MavenProperties mavenProperties, PluginInfoProvider pluginInfoProvider) {
		this(mavenProperties, pluginInfoProvider, new MavenFileDownloader(mavenProperties), new CompoundVerifier());
	}

	/**
	 * 使用指定下载器和验证器创建 Maven 更新仓库。
	 *
	 * @param mavenProperties Maven 仓库配置
	 * @param pluginInfoProvider 插件元数据提供器
	 * @param fileDownloader 插件文件下载器
	 * @param fileVerifier 插件文件验证器
	 */
	public MavenUpdateRepository(MavenProperties mavenProperties, PluginInfoProvider pluginInfoProvider,
								 FileDownloader fileDownloader, FileVerifier fileVerifier) {
		this.mavenProperties = Objects.requireNonNull(mavenProperties, "mavenProperties must not be null");
		this.pluginInfoProvider = Objects.requireNonNull(pluginInfoProvider, "pluginInfoProvider must not be null");
		this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader must not be null");
		this.fileVerifier = Objects.requireNonNull(fileVerifier, "fileVerifier must not be null");
	}

	/**
	 * 获取仓库 ID。
	 *
	 * @return 固定值 {@code maven}
	 */
	@Override
	public String getId() {
		return MAVEN_REPOSITORY;
	}

	/**
	 * 获取仓库目录 URL。
	 *
	 * <p>Maven 仓库由 {@link MavenProperties} 描述，没有单一目录 URL。</p>
	 *
	 * @return 始终返回 {@code null}
	 */
	@Override
	public URL getUrl() {
		return null;
	}

	/**
	 * 获取仓库中的全部插件元数据。
	 *
	 * <p>首次访问使用双重检查构建缓存，并将每个非空插件的仓库 ID 设置为 {@code maven}。
	 * 后续访问直接返回同一缓存实例。</p>
	 *
	 * @return 不可修改的插件元数据映射；提供器无数据时返回空映射
	 */
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

	/**
	 * 按元数据映射键获取插件。
	 *
	 * @param coordinates 插件提供器使用的 Maven 坐标或稳定键
	 * @return 对应插件元数据；不存在时返回 {@code null}
	 */
	@Override
	public PluginInfo getPlugin(String coordinates) {
		return getPlugins().get(coordinates);
	}

	/**
	 * 使当前插件元数据缓存失效。
	 *
	 * <p>下一次调用 {@link #getPlugins()} 时会重新向提供器取数。</p>
	 */
	@Override
	public synchronized void refresh() {
		plugins = null;
	}

	/**
	 * 创建与当前仓库配置绑定的 Maven 文件下载器。
	 *
	 * @return 新的 Maven 感知文件下载器
	 */
	@Override
	public FileDownloader getFileDownloader() {
		return fileDownloader;
	}

	/**
	 * 创建插件文件校验器。
	 *
	 * @return 新的 PF4J 组合校验器
	 */
	@Override
	public FileVerifier getFileVerifier() {
		return fileVerifier;
	}

}
