package org.pf4j.spring.extension.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.pf4j.PluginRuntimeException;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.util.StringUtils;

/**
 * 按插件跟踪 Spring 单例 Bean 和 MVC Controller 的注册表。
 *
 * <p>注册表维护插件与 Bean 的所有权关系，在插件停止、失败或卸载时可以精确销毁其贡献的
 * Spring 对象，防止宿主容器继续持有插件类加载器。</p>
 *
 * @author <a href="https://github.com/hiwepy">hiwepy</a>
 */
public final /**
 * Registry for managing Spring beans provided by plugins, supporting dynamic registration and lookup.
 */
class PluginBeanRegistry {

    /** 支持动态注册和销毁单例的 BeanFactory。 */
    private final AbstractAutowireCapableBeanFactory beanFactory;

    /** 动态 MVC Controller 注册器。 */
    private final DynamicControllerRegistry controllerRegistry;

    /** 插件 ID 到已注册 Bean 的映射。 */
    private final Map<String, Map<String, Registration>> registrations =
            new LinkedHashMap<String, Map<String, Registration>>();

    /** Bean 名称到所属插件的映射。 */
    private final Map<String, String> owners = new LinkedHashMap<String, String>();

    /**
     * 创建插件 Bean 注册表。
     *
     * @param beanFactory Spring BeanFactory
     * @param controllerRegistry 动态 Controller 注册器
     */
    public PluginBeanRegistry(AbstractAutowireCapableBeanFactory beanFactory,
                              DynamicControllerRegistry controllerRegistry) {
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory must not be null");
        this.controllerRegistry = Objects.requireNonNull(controllerRegistry,
                "controllerRegistry must not be null");
    }

    /**
     * 注册插件贡献的 Spring Bean。
     *
     * @param pluginId 插件 ID
     * @param beanName Bean 名称
     * @param bean Bean 实例
     * @param controller 是否为 MVC Controller
     * @throws PluginRuntimeException 当 Bean 名称已被其他插件占用时抛出
     */
    public synchronized void register(String pluginId, String beanName, Object bean, boolean controller) {
        if (!StringUtils.hasText(pluginId) || !StringUtils.hasText(beanName)) {
            throw new IllegalArgumentException("pluginId and beanName must not be blank");
        }
        Objects.requireNonNull(bean, "bean must not be null");
        String owner = owners.get(beanName);
        if (Objects.nonNull(owner) && !pluginId.equals(owner)) {
            throw new PluginRuntimeException("Spring bean '{}' is already owned by plugin '{}'", beanName, owner);
        }
        removeRegistration(pluginId, beanName);
        if (controller) {
            controllerRegistry.registerController(beanName, bean);
        } else {
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            beanFactory.registerSingleton(beanName, bean);
        }
        Map<String, Registration> pluginRegistrations = registrations.get(pluginId);
        if (Objects.isNull(pluginRegistrations)) {
            pluginRegistrations = new LinkedHashMap<String, Registration>();
            registrations.put(pluginId, pluginRegistrations);
        }
        pluginRegistrations.put(beanName, new Registration(beanName, controller));
        owners.put(beanName, pluginId);
    }

    /**
     * 移除指定插件贡献的全部 Bean。
     *
     * @param pluginId 插件 ID
     */
    public synchronized void removePlugin(String pluginId) {
        Map<String, Registration> pluginRegistrations = registrations.remove(pluginId);
        if (Objects.isNull(pluginRegistrations)) {
            return;
        }
        List<Registration> values = new ArrayList<Registration>(pluginRegistrations.values());
        Collections.reverse(values);
        for (Registration registration : values) {
            removeBean(registration);
            owners.remove(registration.beanName);
        }
    }

    /**
     * 移除注册表中的全部插件 Bean。
     */
    public synchronized void removeAll() {
        List<String> pluginIds = new ArrayList<String>(registrations.keySet());
        Collections.reverse(pluginIds);
        for (String pluginId : pluginIds) {
            removePlugin(pluginId);
        }
    }

    /**
     * 获取指定插件当前注册的 Bean 名称。
     *
     * @param pluginId 插件 ID
     * @return Bean 名称不可修改集合
     */
    public synchronized Set<String> getBeanNames(String pluginId) {
        Map<String, Registration> pluginRegistrations = registrations.get(pluginId);
        if (Objects.isNull(pluginRegistrations)) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<String>(pluginRegistrations.keySet()));
    }

    /**
     * 移除同一插件中指定 Bean 的旧注册。
     *
     * @param pluginId 插件 ID
     * @param beanName Bean 名称
     */
    private void removeRegistration(String pluginId, String beanName) {
        Map<String, Registration> pluginRegistrations = registrations.get(pluginId);
        if (Objects.isNull(pluginRegistrations)) {
            return;
        }
        Registration registration = pluginRegistrations.remove(beanName);
        if (Objects.nonNull(registration)) {
            removeBean(registration);
            owners.remove(beanName);
        }
        if (pluginRegistrations.isEmpty()) {
            registrations.remove(pluginId);
        }
    }

    /**
     * 从 Spring 容器移除单个注册对象。
     *
     * @param registration Bean 注册信息
     */
    private void removeBean(Registration registration) {
        if (registration.controller) {
            try {
                controllerRegistry.removeController(registration.beanName);
            } catch (IOException e) {
                throw new PluginRuntimeException(e, "Failed to remove PF4J controller '{}'", registration.beanName);
            }
        } else if (beanFactory.containsSingleton(registration.beanName)) {
            beanFactory.destroySingleton(registration.beanName);
        }
    }

    /**
     * 单个插件 Bean 注册信息。
     */
    private static final class Registration {

        /** Bean 名称。 */
        private final String beanName;

        /** 是否为 MVC Controller。 */
        private final boolean controller;

        /**
         * 创建 Bean 注册信息。
         *
         * @param beanName Bean 名称
         * @param controller 是否为 MVC Controller
         */
        private Registration(String beanName, boolean controller) {
            this.beanName = beanName;
            this.controller = controller;
        }
    }
}
