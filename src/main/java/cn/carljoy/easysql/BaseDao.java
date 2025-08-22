package cn.carljoy.easysql;

import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.api.SQLQuery;
import cc.carm.lib.easysql.api.builder.TableCreateBuilder;
import cc.carm.lib.easysql.api.builder.TableQueryBuilder;
import cc.carm.lib.easysql.api.enums.IndexType;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 基础 DAO 类，提供通用的 CRUD 操作
 * 
 * @param <T> 实体类型
 */
@Slf4j
public class BaseDao<T> {
    private final SQLManager sm;
    private final Class<T> clazz;
    private final TableInfo tableInfo;

    public BaseDao(Class<T> clazz, SQLManager sm) {
        this.sm = sm;
        this.clazz = clazz;
        this.tableInfo = TableInfo.of(clazz);

        createTable();
    }

    /* 根据主键查询数据 */
    public T selectOneById(Object id) {
        return selectOneByQuery(QueryWrapper.create(clazz).eq(tableInfo.pk, id));
    }

    /* 根据查询条件来查询 1 条数据 */
    public T selectOneByQuery(QueryWrapper<T> queryWrapper) {
        // 限制查询结果为1条
        queryWrapper.limit(1);
        List<T> list = selectListByQuery(queryWrapper);
        return list.isEmpty() ? null : list.getFirst();
    }

    /* 根据查询条件查询数据列表 */
    public List<T> selectListByQuery(QueryWrapper<T> queryWrapper) {
        TableQueryBuilder q = sm.createQuery().inTable(queryWrapper.getTableInfo().name);
        
        // 应用条件
        queryWrapper.applyConditions(q);
        
        // 应用排序
        queryWrapper.applyOrdering(q);
        
        // 应用分页
        queryWrapper.applyPaging(q);
        
        // 执行查询
        try (SQLQuery query = q.build().execute()) {
            return RowMapper.toList(query.getResultSet(), queryWrapper.getEntityClass());
        } catch (SQLException e) {
            log.error("查询数据列表失败: {}", e.getMessage());
            
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /* 查询全部数据 */
    public List<T> selectAll() {
        return selectListByQuery(QueryWrapper.create(clazz));
    }

    /* 查询数据量 */
    public long selectCountByQuery(QueryWrapper<T> queryWrapper) {
        try {
            TableQueryBuilder q = sm.createQuery()
                    .inTable(queryWrapper.getTableInfo().name)
                    .selectColumns("COUNT(1)");
            
            queryWrapper.applyConditions(q);
            
            try (SQLQuery query = q.build().execute()) {
                return query.getResultSet().next()
                        ? query.getResultSet().getLong(1)
                        : 0L;
            }
        } catch (SQLException e) {
            log.error("查询数据量失败: {}", e.getMessage());
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 分页查询，自动计算总记录数
     * 
     * @param pageNumber 页码（从1开始）
     * @param pageSize 每页大小
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public PageResult<T> paginate(int pageNumber, int pageSize, QueryWrapper<T> queryWrapper) {
        try {
            // 先查询总记录数
            long total = selectCountByQuery(queryWrapper);
            
            // 设置分页参数
            queryWrapper.page(pageNumber, pageSize);
            
            // 查询当前页数据
            List<T> records = selectListByQuery(queryWrapper);
            
            return new PageResult<>(records, total, pageNumber, pageSize);
        } catch (Exception e) {
            log.error("分页查询失败: {}", e.getMessage());
            e.printStackTrace();
            return new PageResult<>(new ArrayList<>(), 0L, pageNumber, pageSize);
        }
    }
    
    /**
     * 分页查询，使用指定的总记录数（避免重复查询总数）
     * 
     * @param pageNumber 页码（从1开始）
     * @param pageSize 每页大小
     * @param totalRow 总记录数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public PageResult<T> paginate(int pageNumber, int pageSize, long totalRow, QueryWrapper<T> queryWrapper) {
        try {
            // 设置分页参数
            queryWrapper.page(pageNumber, pageSize);
            
            // 查询当前页数据
            List<T> records = selectListByQuery(queryWrapper);
            
            return new PageResult<>(records, totalRow, pageNumber, pageSize);
        } catch (Exception e) {
            log.error("分页查询失败: {}", e.getMessage());
            e.printStackTrace();
            return new PageResult<>(new ArrayList<>(), totalRow, pageNumber, pageSize);
        }
    }

    /* 根据主键删除 */
    public boolean deleteById(Object id) {
        try {
            Integer result = sm.createDelete(tableInfo.name)
                    .addCondition(tableInfo.pk, id)
                    .build()
                    .execute();
            return result > 0;
        } catch (SQLException e) {
            log.error("删除数据失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /* 插入实体（简单实现，可根据需要扩展） */
    public boolean insert(T entity) {
        try {
            // 自动设置时间戳
            setTimestamps(entity, true);
            
            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            
            for (Field field : clazz.getFields()) {
                String columnName = camelToUnder(field.getName());
                Object value = field.get(entity);
                if (value != null) {
                    columns.add(columnName);
                    values.add(value);
                }
            }
            
            if (columns.isEmpty()) {
                return false;
            }
            
            Integer result = sm.createInsert(tableInfo.name)
                    .setColumnNames(columns.toArray(new String[0]))
                    .setParams(values.toArray())
                    .execute();
            
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("插入数据失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /* 根据主键更新实体 */
    public boolean updateById(T entity) {
        try {
            // 自动设置更新时间戳
            setTimestamps(entity, false);
            
            List<String> setParts = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            Object pkValue = null;
            
            for (Field field : clazz.getFields()) {
                String columnName = camelToUnder(field.getName());
                Object value = field.get(entity);
                
                // 检查是否为主键字段
                boolean isPrimaryKey = false;
                for (TableInfo.ColumnInfo column : tableInfo.columns) {
                    if (column.field.equals(field) && column.primaryKey) {
                        isPrimaryKey = true;
                        pkValue = value;
                        break;
                    }
                }
                
                // 非主键字段且有值的才加入更新列表
                if (!isPrimaryKey && value != null) {
                    setParts.add(columnName + " = ?");
                    values.add(value);
                }
            }
            
            if (setParts.isEmpty() || pkValue == null) {
                return false;
            }
            
            // 添加主键值到参数列表末尾
            values.add(pkValue);
            
            String sql = "UPDATE " + tableInfo.name + " SET " + 
                        String.join(", ", setParts) + " WHERE " + tableInfo.pk + " = ?";
            
            Integer result = sm.executeSQL(sql, values.toArray());
            
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("更新数据失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== 新增的插入操作方法 ====================
    
    /**
     * 插入实体类数据，但是忽略 null 的数据，只对有值的内容进行插入
     */
    public boolean insertSelective(T entity) {
        return insert(entity, true);
    }
    
    /**
     * 插入实体类数据
     */
    public boolean insert(T entity, boolean ignoreNulls) {
        try {
            // 自动设置时间戳
            setTimestamps(entity, true);
            
            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            
            for (Field field : clazz.getFields()) {
                String columnName = camelToUnder(field.getName());
                Object value = field.get(entity);
                if (!ignoreNulls || value != null) {
                    columns.add(columnName);
                    values.add(value);
                }
            }
            
            if (columns.isEmpty()) {
                return false;
            }
            
            Integer result = sm.createInsert(tableInfo.name)
                    .setColumnNames(columns.toArray(new String[0]))
                    .setParams(values.toArray())
                    .execute();
            
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("插入数据失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 批量插入实体类数据，只会根据第一条数据来构建插入的字段内容
     */
    public int insertBatch(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            // 根据第一条数据构建字段列表
            T firstEntity = entities.get(0);
            setTimestamps(firstEntity, true);
            
            List<String> columns = new ArrayList<>();
            for (Field field : clazz.getFields()) {
                String columnName = camelToUnder(field.getName());
                Object value = field.get(firstEntity);
                if (value != null) {
                    columns.add(columnName);
                }
            }
            
            if (columns.isEmpty()) {
                return 0;
            }
            
            // 构建所有实体的值
            List<Object[]> allValues = new ArrayList<>();
            for (T entity : entities) {
                setTimestamps(entity, true);
                List<Object> values = new ArrayList<>();
                for (Field field : clazz.getFields()) {
                    String columnName = camelToUnder(field.getName());
                    if (columns.contains(columnName)) {
                        values.add(field.get(entity));
                    }
                }
                allValues.add(values.toArray());
            }
            
            Integer result = sm.createInsert(tableInfo.name)
                    .setColumnNames(columns.toArray(new String[0]))
                    .setParams(allValues.toArray(new Object[0][]))
                    .execute();
            
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("批量插入数据失败: {}", e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 批量插入实体类数据，按 size 切分
     */
    public int insertBatch(List<T> entities, int size) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        int totalInserted = 0;
        for (int i = 0; i < entities.size(); i += size) {
            int end = Math.min(i + size, entities.size());
            List<T> batch = entities.subList(i, end);
            totalInserted += insertBatch(batch);
        }
        
        return totalInserted;
    }
    
    /**
     * 批量插入实体类数据，忽略 null 值
     */
    public int insertBatchSelective(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        int totalInserted = 0;
        for (T entity : entities) {
            if (insertSelective(entity)) {
                totalInserted++;
            }
        }
        
        return totalInserted;
    }
    
    /**
     * 插入或者更新，若主键有值，则更新，若没有主键值，则插入，插入或者更新都不会忽略 null 值
     */
    public boolean insertOrUpdate(T entity) {
        return insertOrUpdate(entity, false);
    }
    
    /**
     * 插入或者更新，若主键有值，则更新，若没有主键值，则插入，插入或者更新都会忽略 null 值
     */
    public boolean insertOrUpdateSelective(T entity) {
        return insertOrUpdate(entity, true);
    }
    
    /**
     * 插入或者更新，若主键有值，则更新，若没有主键值，则插入
     */
    public boolean insertOrUpdate(T entity, boolean ignoreNulls) {
        try {
            // 获取主键值
            Object pkValue = null;
            for (Field field : clazz.getFields()) {
                for (TableInfo.ColumnInfo column : tableInfo.columns) {
                    if (column.field.equals(field) && column.primaryKey) {
                        pkValue = field.get(entity);
                        break;
                    }
                }
                if (pkValue != null) {
                    break;
                }
            }
            
            // 如果主键有值，尝试更新
            if (pkValue != null) {
                T existing = selectOneById(pkValue);
                if (existing != null) {
                    return updateById(entity, ignoreNulls);
                }
            }
            
            // 否则插入
            return insert(entity, ignoreNulls);
        } catch (Exception e) {
            log.error("插入或更新数据失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ==================== 新增的删除操作方法 ====================
    
    /**
     * 根据多个主键批量删除数据
     */
    public int deleteBatchByIds(List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        int totalDeleted = 0;
        for (Object id : ids) {
            if (deleteById(id)) {
                totalDeleted++;
            }
        }
        
        return totalDeleted;
    }
    
    /**
     * 根据查询条件来删除数据
     */
    public int deleteByQuery(QueryWrapper<T> queryWrapper) {
        try {
            StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableInfo.name);
            List<Object> params = new ArrayList<>();
            
            if (queryWrapper.hasConditions()) {
                sql.append(" WHERE ").append(queryWrapper.buildWhereClause(params));
            }
            
            Integer result = sm.executeSQL(sql.toString(), params.toArray());
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("根据条件删除数据失败: {}", e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    // ==================== 新增的更新操作方法 ====================
    
    /**
     * 根据主键来更新数据到数据库
     */
    public boolean updateById(T entity, boolean ignoreNulls) {
        try {
            // 自动设置更新时间戳
            setTimestamps(entity, false);
            
            List<String> setParts = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            Object pkValue = null;
            
            for (Field field : clazz.getFields()) {
                String columnName = camelToUnder(field.getName());
                Object value = field.get(entity);
                
                // 检查是否为主键字段
                boolean isPrimaryKey = false;
                for (TableInfo.ColumnInfo column : tableInfo.columns) {
                    if (column.field.equals(field) && column.primaryKey) {
                        isPrimaryKey = true;
                        pkValue = value;
                        break;
                    }
                }
                
                // 非主键字段且符合条件的才加入更新列表
                if (!isPrimaryKey && (!ignoreNulls || value != null)) {
                    setParts.add(columnName + " = ?");
                    values.add(value);
                }
            }
            
            if (setParts.isEmpty() || pkValue == null) {
                return false;
            }
            
            // 添加主键值到参数列表末尾
            values.add(pkValue);
            
            String sql = "UPDATE " + tableInfo.name + " SET " + 
                        String.join(", ", setParts) + " WHERE " + tableInfo.pk + " = ?";
            
            Integer result = sm.executeSQL(sql, values.toArray());
            
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("更新数据失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 根据查询条件来更新数据
     */
    public int updateByQuery(T entity, QueryWrapper<T> queryWrapper) {
        return updateByQuery(entity, true, queryWrapper);
    }
    
    /**
     * 根据查询条件来更新数据
     */
    public int updateByQuery(T entity, boolean ignoreNulls, QueryWrapper<T> queryWrapper) {
        try {
            // 自动设置更新时间戳
            setTimestamps(entity, false);
            
            List<String> setParts = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            
            for (Field field : clazz.getFields()) {
                String columnName = camelToUnder(field.getName());
                Object value = field.get(entity);
                
                // 检查是否为主键字段
                boolean isPrimaryKey = false;
                for (TableInfo.ColumnInfo column : tableInfo.columns) {
                    if (column.field.equals(field) && column.primaryKey) {
                        isPrimaryKey = true;
                        break;
                    }
                }
                
                // 非主键字段且符合条件的才加入更新列表
                if (!isPrimaryKey && (!ignoreNulls || value != null)) {
                    setParts.add(columnName + " = ?");
                    values.add(value);
                }
            }
            
            if (setParts.isEmpty()) {
                return 0;
            }
            
            StringBuilder sql = new StringBuilder("UPDATE ").append(tableInfo.name)
                    .append(" SET ").append(String.join(", ", setParts));
            
            if (queryWrapper.hasConditions()) {
                sql.append(" WHERE ").append(queryWrapper.buildWhereClause(values));
            }
            
            Integer result = sm.executeSQL(sql.toString(), values.toArray());
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("根据条件更新数据失败: {}", e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private static String camelToUnder(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /* 自动设置时间戳 */
    private void setTimestamps(T entity, boolean isInsert) {
        Date now = new Date();
        
        try {
            for (TableInfo.ColumnInfo column : tableInfo.columns) {
                if (column.isCreatedAt && isInsert) {
                    // 只在插入时设置创建时间
                    setFieldValue(entity, column.field, now);
                } else if (column.isUpdatedAt) {
                    // 插入和更新时都设置更新时间
                    setFieldValue(entity, column.field, now);
                }
            }
        } catch (IllegalAccessException e) {
            log.error("设置时间戳失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /* 设置字段值，支持不同的时间类型 */
    private void setFieldValue(T entity, Field field, Date date) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        
        if (fieldType == Date.class) {
            field.set(entity, date);
        } else if (fieldType == Timestamp.class) {
            field.set(entity, new Timestamp(date.getTime()));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(entity, date.getTime());
        } else if (fieldType == String.class) {
            field.set(entity, date.toString());
        }
        // 可以根据需要添加更多类型支持
    }

    /* 创建表 */
    public void createTable() {
        createTable(false);
    }

    /* 创建表（可选择是否覆盖已存在的表） */
    public void createTable(boolean dropIfExists) {
        TableCreateBuilder builder = sm.createTable(tableInfo.name);

        // 如果需要删除已存在的表
        if (dropIfExists) {
            try {
                sm.executeSQL("DROP TABLE IF EXISTS " + tableInfo.name);
            } catch (Exception ignored) {
                // 忽略表不存在的错误
            }
        }

        // 添加字段
        for (TableInfo.ColumnInfo column : tableInfo.columns) {
            // 统一使用 addColumn 方法以支持注释
            String columnDef = buildColumnDefinition(column);
            builder.addColumn(column.name, columnDef);
        }

        // 添加索引
        for (TableInfo.ColumnInfo column : tableInfo.columns) {
            if (column.index != null && !column.index.isEmpty()) {
                IndexType indexType = parseIndexType(column.index);
                if (indexType != null) {
                    builder.setIndex(column.name, indexType);
                }
            }
        }

        // 执行创建表
        builder.build().execute(null);

        // 添加表注释（如果有的话）
        if (tableInfo.comment != null && !tableInfo.comment.isEmpty()) {
            try {
                String commentSql = "ALTER TABLE " + tableInfo.name +
                                  " COMMENT '" + tableInfo.comment.replace("'", "''") + "'";
                sm.executeSQL(commentSql);
            } catch (Exception e) {
                // 某些数据库可能不支持表注释，忽略错误
                System.err.println("Warning: Failed to add table comment: " + e.getMessage());
            }
        }
    }

    /* 构建字段定义 */
    private String buildColumnDefinition(TableInfo.ColumnInfo column) {
        StringBuilder def = new StringBuilder(column.type);
        
        if (!column.nullable) {
            def.append(" NOT NULL");
        }
        
        if (column.autoIncrement) {
            def.append(" AUTO_INCREMENT");
        }
        
        if (column.defaultValue != null && !column.autoIncrement) {
            def.append(" DEFAULT ").append(column.defaultValue);
        }
        
        if (column.primaryKey) {
            def.append(" PRIMARY KEY");
        }
        
        if (column.unique) {
            def.append(" UNIQUE KEY");
        }
        
        if (column.comment != null && !column.comment.isEmpty()) {
            def.append(" COMMENT '").append(column.comment.replace("'", "''")).append("'");
        }
        
        return def.toString();
    }

    /* 解析索引类型 */
    private IndexType parseIndexType(String index) {
        switch (index.toUpperCase()) {
            case "INDEX":
                return IndexType.INDEX;
            case "UNIQUE_KEY":
            case "UNIQUE":
                return IndexType.UNIQUE_KEY;
            default:
                return IndexType.INDEX; // 默认为普通索引
        }
    }
}