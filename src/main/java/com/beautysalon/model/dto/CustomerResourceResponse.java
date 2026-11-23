package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 顾客端资源（美容师）返回体
 *
 * <p>只暴露顾客选择所需的最小字段，不返回绑定的用户 id / 手机号等内部信息。</p>
 */
@Data
@Builder
public class CustomerResourceResponse {

    /** 资源 id（预约时作为 resourceId 传入） */
    private Long id;

    /** 所属商户 id */
    private Long merchantId;

    /** 类型：STAFF 技师 / ROOM 床位 */
    private String type;

    /** 资源名称（美容师姓名） */
    private String name;
}
