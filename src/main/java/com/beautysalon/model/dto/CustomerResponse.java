package com.beautysalon.model.dto;

import com.beautysalon.model.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 顾客信息返回体
 *
 * <p>⚠️ 不包含 passwordHash，密码哈希永远不能返回给前端。</p>
 */
@Data
@Builder
public class CustomerResponse {

    /** 顾客 id */
    private Long id;

    /** 手机号（登录账号） */
    private String phone;

    /** 姓名/昵称 */
    private String nickname;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 实体 → 返回 DTO */
    public static CustomerResponse of(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .phone(customer.getPhone())
                .nickname(customer.getNickname())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
