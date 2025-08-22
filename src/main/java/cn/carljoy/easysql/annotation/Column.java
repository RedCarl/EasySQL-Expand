package cn.carljoy.easysql.annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name() default "";           // 列名
    String type() default "";           // 数据类型，如 "VARCHAR(32)"
    boolean nullable() default true;    // 是否可为空
    String defaultValue() default "";   // 默认值
    boolean autoIncrement() default false; // 是否自增
    boolean unique() default false;     // 是否唯一
    String index() default "";          // 索引类型：INDEX, UNIQUE_KEY 等
    String comment() default "";        // 字段注释
}