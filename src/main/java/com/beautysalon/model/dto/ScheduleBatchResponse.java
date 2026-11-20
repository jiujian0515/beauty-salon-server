package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量新增排班结果
 */
@Data
@Builder
public class ScheduleBatchResponse {

    /** 成功创建条数 */
    private int createdCount;

    /** 跳过条数 */
    private int skippedCount;

    /** 被跳过的日期及原因 */
    private List<SkippedItem> skipped;

    /** 单条跳过信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedItem {

        /** 日期 */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        /** 跳过原因 */
        private String reason;
    }
}
