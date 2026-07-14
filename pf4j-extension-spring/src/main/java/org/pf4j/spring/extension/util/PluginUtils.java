package org.pf4j.spring.extension.util;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
public final class PluginUtils {

	private PluginUtils() {
	}

	public static void loadAndStartPlugins(PluginManager pluginManager, List<String> plugins) {
		Objects.requireNonNull(pluginManager, "pluginManager must not be null");
		if (CollectionUtils.isEmpty(plugins)) {
			return;
		}
		List<String> pluginIds = new ArrayList<String>();
		for (String path : plugins) {
			try {
				String pluginId = pluginManager.loadPlugin(Paths.get(path));
				if (StringUtils.hasText(pluginId)) {
					pluginIds.add(pluginId);
				}
			} catch (Exception e) {
				log.error("Failed to load PF4J plugin from path '{}'", path, e);
			}
		}
		for (String pluginId : pluginIds) {
			try {
				if (StringUtils.hasText(pluginId)) {
					pluginManager.startPlugin(pluginId);
				}
			} catch (Exception e) {
				log.error("Failed to start PF4J plugin '{}'", pluginId, e);
			}
		}
	}

}
