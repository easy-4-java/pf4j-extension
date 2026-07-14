package org.pf4j.core.extension.annotation;

import java.lang.annotation.*;

/**
 * PF4J 扩展实现元数据注解。
 *
 * <p>用于为扩展实现声明稳定 ID、展示标题、版本和描述信息。运行时可通过
 * {@link org.pf4j.core.extension.ExtensionResolver} 根据 {@link #id()} 精确选择扩展。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface ExtensionMapping {

    /**
     * 获取扩展 ID。
     *
     * @return 用于在同一插件内唯一标识扩展实现的字符串
     */
    String id() default "";

    /**
     * 获取扩展展示标题。
     *
     * @return 面向管理界面或日志展示的扩展标题
     */
    String title() default "";

    /**
     * 获取扩展版本号。
     *
     * @return 扩展实现版本，默认值为 {@code 1.0.0}
     */
    String ver() default "1.0.0";

    /**
     * 获取扩展描述。
     *
     * @return 扩展功能、适用范围或约束说明
     */
    String desc() default "";

}
