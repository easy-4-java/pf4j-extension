package org.pf4j.spring.extension.lifecycle;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;

@Slf4j
public class Pf4jShutdownHook extends Thread {
	
	private final PluginManager pluginManager;
	
	public Pf4jShutdownHook(PluginManager pluginManager) {
		this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager must not be null");
	}
	
	@Override
	public void run() {
		try {
			pluginManager.stopPlugins();
			pluginManager.unloadPlugins();
		} catch (RuntimeException e) {
			log.error("Failed to stop PF4J plugins during shutdown", e);
		}
	}
	
}
