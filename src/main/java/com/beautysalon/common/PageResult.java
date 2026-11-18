package com.beautysalon.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页返回结构（列表接口统一用它，类比前端常见的 { list, total } 响应）
 *
 * <p>返回示例：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "data": {
 *     "records": [ ... ],
 *     "total": 25,    // 总条数（前端算总页数用）
 *     "page": 1,      // 当前第几页（从 1 开始）
 *     "size": 10,     // 每页条数
 *     "pages": 3      // 总页数
 *   }
 * }
 * </pre>
 *
 * @param <T> 列表里每一项的类型
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总条数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private long page;

    /** 每页条数 */
    private long size;

    /** 总页数 */
    private long pages;

    /**
     * 由 MyBatis-Plus 的 IPage 转换而来（IPage 里带了 total/pages 等分页信息）
     *
     * @param page    分页对象（含 total、current、size、pages）
     * @param records 已经转换成 DTO 的当前页数据
     */
    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }
}
