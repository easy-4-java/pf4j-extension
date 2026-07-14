package io.github.hiwepy.pf4j.annotation;

import java.lang.annotation.*;


/**
 * 多个插件实现对象，指定默认的实现
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {

}
