package org.pf4j.spring.boot.ext.update;

import java.util.Map;

import org.pf4j.update.PluginInfo;

public interface PluginInfoProvider {

	Map<String, PluginInfo> plugins();

}
