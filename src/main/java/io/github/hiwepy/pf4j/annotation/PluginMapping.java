package io.github.hiwepy.pf4j.annotation;

import java.lang.annotation.*;

/**
 * 插件注解：用于标记插件的信息
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface PluginMapping {

    /**
     * 插件标题
     */
    public String title() default "";

    /**
     * 插件详情说明
     */
    public String detail() default "";

}
