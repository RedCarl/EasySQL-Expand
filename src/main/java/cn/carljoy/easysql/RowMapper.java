package cn.carljoy.easysql;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class RowMapper {
    public static <T> List<T> toList(ResultSet rs, Class<T> clazz) throws SQLException {
        List<T> list = new ArrayList<>();
        while (rs.next()) {
            list.add(toBean(rs, clazz));
        }
        return list;
    }

    private static <T> T toBean(ResultSet rs, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (Field f : clazz.getFields()) {
                String columnName = camelToUnder(f.getName());
                try {
                    Object val = rs.getObject(columnName);
                    if (val != null) {
                        // 处理类型转换
                        Object convertedVal = convertValue(val, f.getType());
                        f.set(obj, convertedVal);
                    }
                } catch (SQLException e) {
                    // 如果列不存在，跳过该字段
                    continue;
                }
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map ResultSet to " + clazz.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static String camelToUnder(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 转换值类型以匹配目标字段类型
     */
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 如果类型已经匹配，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // LocalDateTime -> Date 转换
        if (value instanceof LocalDateTime && targetType == Date.class) {
            return Timestamp.valueOf((LocalDateTime) value);
        }

        // Timestamp -> Date 转换
        if (value instanceof Timestamp && targetType == Date.class) {
            return new Date(((Timestamp) value).getTime());
        }

        // Date -> LocalDateTime 转换
        if (value instanceof Date && targetType == LocalDateTime.class) {
            return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }

        // Timestamp -> LocalDateTime 转换
        if (value instanceof Timestamp && targetType == LocalDateTime.class) {
            return ((Timestamp) value).toLocalDateTime();
        }

        // Float -> Double 转换
        if (value instanceof Float && targetType == Double.class) {
            return ((Float) value).doubleValue();
        }

        // Double -> Float 转换
        if (value instanceof Double && targetType == Float.class) {
            return ((Double) value).floatValue();
        }

        // 其他类型直接返回，让反射处理
        return value;
    }
}