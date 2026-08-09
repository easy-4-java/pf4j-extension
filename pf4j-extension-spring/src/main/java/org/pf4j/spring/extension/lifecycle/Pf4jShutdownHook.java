package org.pf4j.spring.extension.lifecycle;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.pf4j.core.extension.PluginLifecycleManager;

/**
 * PF4J 插件管理器的 JVM 关闭钩子。
 *
 * <p>JVM 关闭时依次停止并卸载全部插件，避免插件线程、类加载器和文件句柄残留。
 * 清理异常只记录日志，不阻止 JVM 继续关闭。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
/**
 * JVM shutdown hook that ensures graceful plugin shutdown when the application terminates.
 */
public class Pf4jShutdownHook extends Thread {
	
	/**
	 * 关闭阶段需要清理的 PF4J 插件管理器。
	 */
	private final PluginManager pluginManager;
	
	/**
	 * 创建 PF4J 关闭钩子。
	 *
	 * @param pluginManager 需要在 JVM 关闭时停止并卸载插件的管理器
	 * @throws NullPointerException 当 {@code pluginManager} 为 {@code null} 时抛出
	 */
	public Pf4jShutdownHook(PluginManager pluginManager) {
		this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
	}
	
	/**
	 * 停止并卸载插件管理器中的全部插件。
	 */
	@Override
	public void run() {
		try {
			new PluginLifecycleManager(pluginManager).unloadAllSafely();
		} catch (RuntimeException e) {
			log.error("Failed to stop PF4J plugins during shutdown", e);
		}
	}
	
}
