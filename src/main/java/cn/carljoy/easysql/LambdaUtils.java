package cn.carljoy.easysql;

import cc.carm.lib.easysql.annotation.Column;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Lambda 表达式工具类
 * 用于将方法引用转换为对应的数据库列名
 */
public class LambdaUtils {
    
    private static final ConcurrentHashMap<String, String> COLUMN_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 函数式接口，用于获取属性
     */
    @FunctionalInterface
    public interface SFunction<T, R> extends Function<T, R>, Serializable {
    }
    
    /**
     * 获取 lambda 表达式对应的列名
     * 
     * @param func lambda 表达式
     * @return 数据库列名
     */
    public static <T> String getColumnName(SFunction<T, ?> func) {
        try {
            // 获取 SerializedLambda
            Method writeReplace = func.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(func);
            
            // 获取方法名
            String methodName = serializedLambda.getImplMethodName();
            
            // 生成缓存键
            String cacheKey = serializedLambda.getImplClass() + "#" + methodName;
            
            // 从缓存中获取
            return COLUMN_CACHE.computeIfAbsent(cacheKey, key -> {
                try {
                    // 获取实现类
                    String implClass = serializedLambda.getImplClass().replace("/", ".");
                    Class<?> clazz = Class.forName(implClass);
                    
                    // 从方法名推断字段名
                    String fieldName = getFieldNameFromMethod(methodName);
                    
                    // 获取字段
                    Field field = clazz.getDeclaredField(fieldName);
                    
                    // 检查是否有 @Column 注解
                    Column column = field.getAnnotation(Column.class);
                    if (column != null && !column.name().isEmpty()) {
                        return column.name();
                    }
                    
                    // 如果没有注解或注解中没有指定名称，使用字段名转换为下划线格式
                    return camelToUnderscore(fieldName);
                    
                } catch (Exception e) {
                    throw new RuntimeException("无法解析 lambda 表达式对应的列名: " + key, e);
                }
            });
            
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("无法解析 lambda 表达式", e);
        }
    }
    
    /**
     * 从方法名推断字段名
     * 支持 getXxx、isXxx 格式
     */
    private static String getFieldNameFromMethod(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
            // 如果不是标准的 getter 方法，直接返回方法名
            return methodName;
        }
    }
    
    /**
     * 将驼峰命名转换为下划线命名
     */
    private static String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        
        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}