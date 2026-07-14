package org.pf4j.update.extension;

import java.util.Map;

import org.pf4j.update.PluginInfo;

/**
 * Maven 更新仓库的插件元数据提供接口。
 *
 * <p>实现方负责以 Maven 坐标或其他稳定键组织 {@link PluginInfo}，仓库会为返回的插件信息
 * 补充自身仓库 ID 并构建只读缓存。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public interface PluginInfoProvider {

	/**
	 * 提供当前可用插件的元数据。
	 *
	 * @return 插件标识到插件元数据的映射；没有插件时可以返回空映射
	 */
	Map<String, PluginInfo> plugins();

}
