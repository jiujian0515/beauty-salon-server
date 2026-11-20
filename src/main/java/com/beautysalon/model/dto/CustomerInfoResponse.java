package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 顾客端「当前登录顾客信息」（GET /api/v1/customer/auth/me 的返回体）
 *
 * <p>顾客没有后台角色，所以这里没有 role / roles 字段——
 * 顾客端只要登录成功，身份就固定是「顾客」。</p>
 */
@Data
@Builder
public class CustomerInfoResponse {

    /** 顾客 id */
    private Long customerId;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 状态：1启用 0禁用 */
    private Integer status;
}
