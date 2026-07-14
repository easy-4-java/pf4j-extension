package org.pf4j.spring.boot.ext.update;

import java.util.Collections;
import java.util.Map;

import org.pf4j.update.PluginInfo;

public class DefaultPluginInfoProvider implements PluginInfoProvider {

	@Override
	public Map<String, PluginInfo> plugins() {
		return Collections.emptyMap();
	}

}
