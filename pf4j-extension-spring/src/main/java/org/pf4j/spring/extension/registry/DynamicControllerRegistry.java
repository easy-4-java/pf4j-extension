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
package org.pf4j.spring.extension.registry;

import java.io.IOException;

/**
 * Spring MVC Controller 动态注册接口。
 *
 * <p>统一封装运行时 Controller Bean 与请求映射的注册和移除操作，供 PF4J 扩展注入器使用。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public interface DynamicControllerRegistry {

    /**
     * 将 Controller 实例及其请求映射动态注册到 Spring MVC。
     *
     * @param controllerBeanName Controller Bean 名称；为空时实现可以使用 Controller 类型名
     * @param controller 待注册的 Controller 实例
     */
	void registerController(String controllerBeanName, Object controller);
	
    /**
     * 从 Spring 容器和 Spring MVC 请求映射表中移除 Controller。
     *
     * @param controllerBeanName 待移除的 Controller Bean 名称
     * @throws IOException 当移除过程中发生 I/O 错误时抛出
     */
    void removeController(String controllerBeanName) throws IOException;
	
	
}
