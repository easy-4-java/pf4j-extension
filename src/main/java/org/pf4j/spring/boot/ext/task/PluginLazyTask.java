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
package org.pf4j.spring.boot.ext.task;

import java.util.TimerTask;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;

@Slf4j
public class PluginLazyTask extends TimerTask {

	private final PluginManager pluginManager;
	
	public PluginLazyTask(PluginManager pluginManager) {
		this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
	}

	@Override
	public void run() {
		try {
			pluginManager.loadPlugins();
			pluginManager.startPlugins();
		} catch (RuntimeException e) {
			log.error("Failed to lazily load and start PF4J plugins", e);
			throw e;
		} finally {
			cancel();
		}
	}

}
