package com.beautysalon.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量删除排班请求
 *
 * <p>逐条删除：有预约占用或预约历史的排班会被跳过并返回原因，其余正常删除。</p>
 */
@Data
public class ScheduleBatchDeleteRequest {

    /** 要删除的排班 id 列表 */
    @NotEmpty(message = "请选择要删除的排班")
    @Size(max = 100, message = "一次最多删除 100 条排班")
    private List<@NotNull(message = "排班 id 不能为空") Long> ids;
}
