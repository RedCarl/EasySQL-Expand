package cn.carljoy.easysql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    String type() default "";           // 数据类型，如 "INT(11)", "BIGINT"
    boolean nullable() default false;   // 是否可为空，主键默认不为空
    boolean autoIncrement() default true; // 是否自增，主键默认自增
    boolean unique() default true;      // 是否唯一，主键默认唯一
    String comment() default "编号";    // 字段注释，默认为"编号"
}