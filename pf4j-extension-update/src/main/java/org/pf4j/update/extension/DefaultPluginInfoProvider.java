package org.pf4j.update.extension;

import java.util.Collections;
import java.util.Map;

import org.pf4j.update.PluginInfo;

/**
 * 返回空插件目录的默认元数据提供器。
 *
 * <p>适合作为未配置外部插件目录时的安全默认实现，调用方可以通过自定义
 * {@link PluginInfoProvider} 提供实际 Maven 插件坐标和版本信息。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public class DefaultPluginInfoProvider implements PluginInfoProvider {

	/**
	 * 创建返回空插件目录的默认提供器。
	 */
	public DefaultPluginInfoProvider() {
	}

	/**
	 * 获取空的插件元数据映射。
	 *
	 * @return 不可修改的空插件映射
	 */
	@Override
	public Map<String, PluginInfo> plugins() {
		return Collections.emptyMap();
	}

}
