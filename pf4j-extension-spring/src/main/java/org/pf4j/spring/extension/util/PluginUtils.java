package org.pf4j.spring.extension.util;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.pf4j.core.extension.PluginLifecycleManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 面向 Spring 应用的 PF4J 插件批量加载工具。
 *
 * <p>先逐个加载配置的插件路径，再启动成功获得 ID 的插件。单个插件失败只记录错误，
 * 不会中断后续插件处理，适合需要尽可能启动可用插件的非事务性场景。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
public final class PluginUtils {

	/**
	 * 阻止工具类被实例化。
	 */
	private PluginUtils() {
	}

	/**
	 * 尽可能加载并启动指定路径列表中的全部插件。
	 *
	 * <p>路径加载失败或插件启动失败会记录日志并继续处理其他条目。本方法不提供批次回滚语义；
	 * 需要原子启动时应使用 {@link org.pf4j.core.extension.PluginLifecycleManager}。</p>
	 *
	 * @param pluginManager 执行插件加载和启动操作的 PF4J 管理器
	 * @param plugins 插件文件或目录路径字符串列表；为空时不执行任何操作
	 * @throws NullPointerException 当 {@code pluginManager} 为 {@code null} 时抛出
	 */
	public static void loadAndStartPlugins(PluginManager pluginManager, List<String> plugins) {
		Objects.requireNonNull(pluginManager, "pluginManager must not be null");
		if (CollectionUtils.isEmpty(plugins)) {
			return;
		}
		PluginLifecycleManager lifecycleManager = new PluginLifecycleManager(pluginManager);
		for (String path : plugins) {
			try {
				lifecycleManager.loadAndStart(Paths.get(path));
			} catch (Exception e) {
				log.error("Failed to load and start PF4J plugin from path '{}'", path, e);
			}
		}
	}

}
