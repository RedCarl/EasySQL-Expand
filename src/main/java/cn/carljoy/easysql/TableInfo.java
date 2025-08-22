package cn.carljoy.easysql;


import cn.carljoy.easysql.annotation.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class TableInfo {
    public final String name;   // 表名
    public final String comment; // 表注释
    public final String pk;     // 主键列名
    public final Class<?> type; // 对应实体类
    public final List<ColumnInfo> columns; // 字段信息
    
    // 缓存已解析的 TableInfo
    private static final ConcurrentHashMap<Class<?>, TableInfo> CACHE = new ConcurrentHashMap<>();

    private TableInfo(Class<?> clazz) {
        this.type = clazz;
        // 1. 表名和注释
        Table t = clazz.getAnnotation(Table.class);
        this.name = (t == null || t.value().isEmpty())
                   ? camelToUnder(clazz.getSimpleName())
                   : t.value();
        this.comment = (t == null || t.comment().isEmpty()) ? null : t.comment();
        // 2. 主键列名
        this.pk = findPk(clazz);
        // 3. 字段信息
        this.columns = collectColumns(clazz);
    }

    public static TableInfo of(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, TableInfo::new);
    }

    /* ---------- 内部工具 ---------- */
    private static String findPk(Class<?> clazz) {
        for (Field f : clazz.getFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                return camelToUnder(f.getName());
            }
        }
        return "id"; // 默认
    }

    private static List<ColumnInfo> collectColumns(Class<?> clazz) {
        List<ColumnInfo> columns = new ArrayList<>();
        for (Field field : clazz.getFields()) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null || 
                field.isAnnotationPresent(Id.class) ||
                field.isAnnotationPresent(CreatedAt.class) ||
                field.isAnnotationPresent(UpdatedAt.class)) {
                columns.add(new ColumnInfo(field, columnAnnotation));
            }
        }
        return columns;
    }

    private static String camelToUnder(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /* ---------- 字段信息类 ---------- */
    public static class ColumnInfo {
        public final String name;           // 列名
        public final String type;           // 数据类型
        public final boolean nullable;      // 是否可为空
        public final String defaultValue;   // 默认值
        public final boolean autoIncrement; // 是否自增
        public final boolean primaryKey;    // 是否主键
        public final boolean unique;        // 是否唯一
        public final String index;          // 索引类型
        public final String comment;        // 字段注释
        public final boolean isCreatedAt;   // 是否为创建时间字段
        public final boolean isUpdatedAt;   // 是否为更新时间字段
        public final Field field;           // 对应的字段

        public ColumnInfo(Field field, Column column) {
            this.field = field;
            this.name = (column != null && !column.name().isEmpty()) 
                       ? column.name() 
                       : camelToUnder(field.getName());
            
            // 如果有 @Column 注解，使用注解信息；否则根据字段类型推断
            if (column != null) {
                this.type = column.type().isEmpty() ? inferType(field.getType()) : column.type();
                this.nullable = column.nullable();
                this.defaultValue = column.defaultValue().isEmpty() ? null : column.defaultValue();
                this.autoIncrement = column.autoIncrement();
                this.unique = column.unique();
                this.index = column.index().isEmpty() ? null : column.index();
                this.comment = column.comment().isEmpty() ? null : column.comment();
            } else {
                this.type = inferType(field.getType());
                this.nullable = true;
                this.defaultValue = null;
                this.autoIncrement = false;
                this.unique = false;
                this.index = null;
                this.comment = null;
            }
            
            this.primaryKey = field.isAnnotationPresent(Id.class);
            this.isCreatedAt = field.isAnnotationPresent(CreatedAt.class);
            this.isUpdatedAt = field.isAnnotationPresent(UpdatedAt.class);
        }

        private static String inferType(Class<?> fieldType) {
            if (fieldType == int.class || fieldType == Integer.class) {
                return "INT(11)";
            } else if (fieldType == long.class || fieldType == Long.class) {
                return "BIGINT";
            } else if (fieldType == String.class) {
                return "VARCHAR(255)";
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                return "TINYINT(1)";
            } else if (fieldType == java.util.Date.class || fieldType == java.sql.Timestamp.class) {
                return "DATETIME";
            } else {
                return "TEXT";
            }
        }
    }










}