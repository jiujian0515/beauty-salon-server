package com.beautysalon.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量删除排班结果
 */
@Data
@Builder
public class ScheduleBatchDeleteResponse {

    /** 成功删除条数 */
    private int deletedCount;

    /** 被跳过条数 */
    private int skippedCount;

    /** 被跳过的排班及原因 */
    private List<SkippedItem> skipped;

    /** 单条跳过信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedItem {

        /** 排班 id */
        private Long id;

        /** 跳过原因 */
        private String reason;
    }
}
