package cn.carljoy.easysql;

import cc.carm.lib.easysql.api.builder.TableQueryBuilder;

import java.util.ArrayList;
import java.util.List;

public class QueryWrapper<T> {

    private final TableInfo table;
    private final Class<T> entityClass;

    // 存储条件信息的内部类
    private static class Condition {
        final String column;
        final String operator;
        final Object value;
        final boolean hasValue;

        Condition(String column, String operator, Object value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
            this.hasValue = true;
        }

        Condition(String column, String operator) {
            this.column = column;
            this.operator = operator;
            this.value = null;
            this.hasValue = false;
        }
    }

    private final List<Condition> conditions = new ArrayList<>();

    private String orderColumn;
    private boolean orderAsc = true;
    private Integer limit;
    private Integer offset;

    public QueryWrapper(Class<T> clazz) {
        this.table = TableInfo.of(clazz);
        this.entityClass = clazz;
    }

    /* 静态工厂方法 */
    public static <T> QueryWrapper<T> create(Class<T> clazz) {
        return new QueryWrapper<>(clazz);
    }

    /* ---------- 条件构造 ---------- */
    public QueryWrapper<T> eq(String column, Object val) {
        conditions.add(new Condition(column, "=", val));
        return this;
    }

    public QueryWrapper<T> eq(LambdaUtils.SFunction<T, ?> column, Object val) {
        return eq(LambdaUtils.getColumnName(column), val);
    }

    public QueryWrapper<T> ne(String column, Object val) {
        return op(column, "!=", val);
    }

    public QueryWrapper<T> ne(LambdaUtils.SFunction<T, ?> column, Object val) {
        return ne(LambdaUtils.getColumnName(column), val);
    }

    public QueryWrapper<T> like(String column, String val) {
        // 自动添加 % 通配符
        String likeValue = val != null && !val.startsWith("%") && !val.endsWith("%") 
            ? "%" + val + "%" 
            : val;
        return op(column, "LIKE", likeValue);
    }

    public QueryWrapper<T> like(LambdaUtils.SFunction<T, ?> column, String val) {
        return like(LambdaUtils.getColumnName(column), val);
    }

    /**
     * 左匹配查询（以指定值开头）
     * 例如：likeLeft("name", "admin") 会匹配 "admin123"
     */
    public QueryWrapper<T> likeLeft(String column, String val) {
        String likeValue = val != null ? val + "%" : null;
        return op(column, "LIKE", likeValue);
    }

    public QueryWrapper<T> likeLeft(LambdaUtils.SFunction<T, ?> column, String val) {
        return likeLeft(LambdaUtils.getColumnName(column), val);
    }

    /**
     * 右匹配查询（以指定值结尾）
     * 例如：likeRight("name", "admin") 会匹配 "123admin"
     */
    public QueryWrapper<T> likeRight(String column, String val) {
        String likeValue = val != null ? "%" + val : null;
        return op(column, "LIKE", likeValue);
    }

    public QueryWrapper<T> likeRight(LambdaUtils.SFunction<T, ?> column, String val) {
        return likeRight(LambdaUtils.getColumnName(column), val);
    }

    public QueryWrapper<T> gt(String column, Object val) {
        return op(column, ">", val);
    }

    public QueryWrapper<T> gt(LambdaUtils.SFunction<T, ?> column, Object val) {
        return gt(LambdaUtils.getColumnName(column), val);
    }

    public QueryWrapper<T> ge(String column, Object val) {
        return op(column, ">=", val);
    }

    public QueryWrapper<T> ge(LambdaUtils.SFunction<T, ?> column, Object val) {
        return ge(LambdaUtils.getColumnName(column), val);
    }

    public QueryWrapper<T> lt(String column, Object val) {
        return op(column, "<", val);
    }

    public QueryWrapper<T> lt(LambdaUtils.SFunction<T, ?> column, Object val) {
        return lt(LambdaUtils.getColumnName(column), val);
    }

    public QueryWrapper<T> le(String column, Object val) {
        return op(column, "<=", val);
    }

    public QueryWrapper<T> le(LambdaUtils.SFunction<T, ?> column, Object val) {
        return le(LambdaUtils.getColumnName(column), val);
    }

    public QueryWrapper<T> in(String column, Object... values) {
        if (values.length == 0)
            return this;
        // IN 操作需要特殊处理，传递数组作为值
        conditions.add(new Condition(column, "IN", values));
        return this;
    }

    public QueryWrapper<T> in(LambdaUtils.SFunction<T, ?> column, Object... values) {
        return in(LambdaUtils.getColumnName(column), values);
    }

    public QueryWrapper<T> notIn(String column, Object... values) {
        if (values.length == 0)
            return this;
        conditions.add(new Condition(column, "NOT IN", values));
        return this;
    }

    public QueryWrapper<T> notIn(LambdaUtils.SFunction<T, ?> column, Object... values) {
        return notIn(LambdaUtils.getColumnName(column), values);
    }

    public QueryWrapper<T> isNull(String column) {
        conditions.add(new Condition(column, "IS NULL"));
        return this;
    }

    public QueryWrapper<T> isNull(LambdaUtils.SFunction<T, ?> column) {
        return isNull(LambdaUtils.getColumnName(column));
    }

    public QueryWrapper<T> isNotNull(String column) {
        conditions.add(new Condition(column, "IS NOT NULL"));
        return this;
    }

    public QueryWrapper<T> isNotNull(LambdaUtils.SFunction<T, ?> column) {
        return isNotNull(LambdaUtils.getColumnName(column));
    }

    public QueryWrapper<T> between(String column, Object start, Object end) {
        // BETWEEN 操作传递数组 [start, end]
        conditions.add(new Condition(column, "BETWEEN", new Object[] { start, end }));
        return this;
    }

    public QueryWrapper<T> between(LambdaUtils.SFunction<T, ?> column, Object start, Object end) {
        return between(LambdaUtils.getColumnName(column), start, end);
    }

    private QueryWrapper<T> op(String column, String op, Object val) {
        conditions.add(new Condition(column, op, val));
        return this;
    }

    /* ---------- 排序 / 分页 ---------- */
    public QueryWrapper<T> orderByAsc(String column) {
        this.orderColumn = column;
        this.orderAsc = true;
        return this;
    }

    public QueryWrapper<T> orderByAsc(LambdaUtils.SFunction<T, ?> column) {
        return orderByAsc(LambdaUtils.getColumnName(column));
    }

    public QueryWrapper<T> orderByDesc(String column) {
        this.orderColumn = column;
        this.orderAsc = false;
        return this;
    }

    public QueryWrapper<T> orderByDesc(LambdaUtils.SFunction<T, ?> column) {
        return orderByDesc(LambdaUtils.getColumnName(column));
    }

    public QueryWrapper<T> limit(int rows) {
        this.limit = rows;
        return this;
    }

    public QueryWrapper<T> page(int pageNumber, int pageSize) {
        int start = (pageNumber - 1) * pageSize;
        int end = start + pageSize - 1; // EasySQL 要求闭区间 [start, end]
        return this.limit(start, end);
    }

    private QueryWrapper<T> limit(int start, int end) { // 保留原内部方法
        this.offset = start;
        this.limit = end;
        return this;
    }

    /* ---------- 内部方法，供DAO使用 ---------- */
    public void applyConditions(TableQueryBuilder q) {
        for (Condition cond : conditions) {
            if (cond.hasValue) {
                if ("IN".equals(cond.operator) || "NOT IN".equals(cond.operator)) {
                    // 处理 IN 和 NOT IN 操作
                    Object[] values = (Object[]) cond.value;
                    for (Object val : values) {
                        q.addCondition(cond.column, cond.operator, val);
                    }
                } else if ("BETWEEN".equals(cond.operator)) {
                    // 处理 BETWEEN 操作
                    Object[] range = (Object[]) cond.value;
                    q.addCondition(cond.column, ">=", range[0]);
                    q.addCondition(cond.column, "<=", range[1]);
                } else {
                    // 普通操作
                    q.addCondition(cond.column, cond.operator, cond.value);
                }
            } else {
                // 处理不需要参数的条件，如 IS NULL
                q.addCondition(cond.column, cond.operator, null);
            }
        }
    }

    public void applyOrdering(TableQueryBuilder q) {
        if (orderColumn != null) {
            q.orderBy(orderColumn, orderAsc);
        }
    }

    public void applyPaging(TableQueryBuilder q) {
        if (limit != null && offset != null) {
            q.setPageLimit(offset, limit);
        }
    }

    public TableInfo getTableInfo() {
        return table;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * 检查是否有查询条件
     * @return 如果有条件返回 true，否则返回 false
     */
    public boolean hasConditions() {
        return !conditions.isEmpty();
    }

    /**
     * 构建 WHERE 子句
     * @param params 参数列表，用于收集查询参数
     * @return WHERE 子句字符串
     */
    public String buildWhereClause(List<Object> params) {
        if (conditions.isEmpty()) {
            return "";
        }

        List<String> whereParts = new ArrayList<>();
        for (Condition cond : conditions) {
            if (cond.hasValue) {
                if ("IN".equals(cond.operator) || "NOT IN".equals(cond.operator)) {
                    // 处理 IN 和 NOT IN 操作
                    Object[] values = (Object[]) cond.value;
                    if (values.length > 0) {
                        String placeholders = String.join(", ", java.util.Collections.nCopies(values.length, "?"));
                        whereParts.add(cond.column + " " + cond.operator + " (" + placeholders + ")");
                        for (Object val : values) {
                            params.add(val);
                        }
                    }
                } else if ("BETWEEN".equals(cond.operator)) {
                    // 处理 BETWEEN 操作
                    Object[] range = (Object[]) cond.value;
                    whereParts.add(cond.column + " BETWEEN ? AND ?");
                    params.add(range[0]);
                    params.add(range[1]);
                } else {
                    // 普通操作
                    whereParts.add(cond.column + " " + cond.operator + " ?");
                    params.add(cond.value);
                }
            } else {
                // 处理不需要参数的条件，如 IS NULL
                whereParts.add(cond.column + " " + cond.operator);
            }
        }

        return String.join(" AND ", whereParts);
    }
}