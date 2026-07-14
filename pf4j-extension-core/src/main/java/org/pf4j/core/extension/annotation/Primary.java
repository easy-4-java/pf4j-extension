package org.pf4j.core.extension.annotation;

import java.lang.annotation.*;


/**
 * PF4J 扩展主要实现标记。
 *
 * <p>当同一扩展类型存在多个候选实现时，可在默认实现类型或工厂方法上添加该注解，
 * 由 {@link org.pf4j.core.extension.ExtensionResolver#getPrimary(Class, String)} 选择。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {

}
