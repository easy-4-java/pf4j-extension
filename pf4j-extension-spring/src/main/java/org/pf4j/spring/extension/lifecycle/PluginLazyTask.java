/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.pf4j.spring.extension.lifecycle;

import java.util.TimerTask;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.pf4j.core.extension.PluginLifecycleManager;

/**
 * 延迟加载并启动全部 PF4J 插件的一次性定时任务。
 *
 * <p>任务无论成功或失败都会在本次执行结束时取消，防止同一个 {@link TimerTask} 被重复调度。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
@Slf4j
/**
 * Lazy initialization task for plugins that defers plugin loading until first access.
 */
public class PluginLazyTask extends TimerTask {

	/**
	 * 执行插件批量加载和启动操作的 PF4J 管理器。
	 */
	private final PluginManager pluginManager;
	
	/**
	 * 创建插件延迟启动任务。
	 *
	 * @param pluginManager PF4J 插件管理器
	 * @throws NullPointerException 当 {@code pluginManager} 为 {@code null} 时抛出
	 */
	public PluginLazyTask(PluginManager pluginManager) {
		this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
	}

	/**
	 * 加载并启动全部插件，执行结束后取消当前任务。
	 *
	 * @throws RuntimeException 当插件加载或启动失败时记录日志并继续抛出
	 */
	@Override
	public void run() {
		try {
			new PluginLifecycleManager(pluginManager).loadAllAndStartStrictly();
		} catch (RuntimeException e) {
			log.error("Failed to lazily load and start PF4J plugins", e);
			throw e;
		} finally {
			cancel();
		}
	}

}
