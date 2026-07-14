package io.github.hiwepy.pf4j.util;

import io.github.hiwepy.pf4j.annotation.ExtensionMapping;
import io.github.hiwepy.pf4j.exception.PluginInvokeException;
import org.pf4j.PluginManager;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * PF4J 工具类
 *
 * <p>提供获取扩展点实例的工具方法，支持按插件 ID 和扩展点 ID 精确查找。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Pf4jKit {

    public static <T> T getExtensionPoint(PluginManager pluginManager, Class<T> type, String pluginId,
                                          String extensionId) throws PluginInvokeException {
        if (StringUtils.isNotBlank(pluginId) && StringUtils.isNotBlank(extensionId)) {
            List<T> extensions = pluginManager.getExtensions(type, pluginId);
            for (T extension : extensions) {
                ExtensionMapping em = extension.getClass().getAnnotation(ExtensionMapping.class);
                if (Objects.nonNull(em) && StringUtils.isNotBlank(em.id()) && em.id().equals(extensionId)) {
                    return extension;
                }
            }
        }
        return null;
    }

}
