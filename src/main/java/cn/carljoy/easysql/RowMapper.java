package cn.carljoy.easysql;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
                        f.set(obj, val);
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
}