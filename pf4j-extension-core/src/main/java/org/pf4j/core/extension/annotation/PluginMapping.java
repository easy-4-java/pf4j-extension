package org.pf4j.core.extension.annotation;

import java.lang.annotation.*;

/**
 * PF4J 插件展示元数据注解。
 *
 * <p>用于在插件实现类型上补充人类可读的标题和详情，供插件目录、管理页面或审计日志使用。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface PluginMapping {

    /**
     * 获取插件标题。
     *
     * @return 面向用户展示的插件名称
     */
    String title() default "";

    /**
     * 获取插件详情说明。
     *
     * @return 插件能力、用途和使用约束的描述
     */
    String detail() default "";

}
