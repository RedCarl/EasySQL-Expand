package cn.carljoy.easysql;

import lombok.Getter;

import java.util.List;

/**
 * 分页查询结果类
 * 
 * @param <T> 实体类型
 */
@Getter
public class PageResult<T> {
    /**
     * -- GETTER --
     *  获取当前页数据
     */
    private final List<T> records;
    /**
     * -- GETTER --
     *  获取总记录数
     */
    private final long total;
    /**
     * -- GETTER --
     *  获取当前页码
     */
    private final int pageNumber;
    /**
     * -- GETTER --
     *  获取每页大小
     */
    private final int pageSize;
    /**
     * -- GETTER --
     *  获取总页数
     */
    private final int totalPages;
    
    public PageResult(List<T> records, long total, int pageNumber, int pageSize) {
        this.records = records;
        this.total = total;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return pageNumber > 1;
    }
    
    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return pageNumber < totalPages;
    }
    
    /**
     * 是否为第一页
     */
    public boolean isFirst() {
        return pageNumber == 1;
    }
    
    /**
     * 是否为最后一页
     */
    public boolean isLast() {
        return pageNumber == totalPages;
    }
    
    @Override
    public String toString() {
        return "PageResult{" +
                "records=" + records.size() + " items" +
                ", total=" + total +
                ", pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", totalPages=" + totalPages +
                '}';
    }
}