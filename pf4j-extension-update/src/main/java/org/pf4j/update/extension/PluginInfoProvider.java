package org.pf4j.update.extension;

import java.util.Map;

import org.pf4j.update.PluginInfo;

public interface PluginInfoProvider {

	Map<String, PluginInfo> plugins();

}
