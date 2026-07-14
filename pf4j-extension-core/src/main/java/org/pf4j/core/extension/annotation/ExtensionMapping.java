package org.pf4j.core.extension.annotation;

import java.lang.annotation.*;

/**
 * 扩展点注解：用于标注某个功能扩展点的信息
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface ExtensionMapping {

    /**
     * 扩展点 ID
     */
    public String id() default "";

    /**
     * 扩展点标题
     */
    public String title() default "";

    /**
     * 扩展点版本号
     */
    public String ver() default "1.0.0";

    /**
     * 扩展点描述
     */
    public String desc() default "";

}
