package com.beautysalon.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户核销预约返回体
 */
@Data
@Builder
public class ReservationVerifyResponse {

    /** 预约单 id */
    private Long reservationId;

    /** 预约业务号 */
    private String reservationNo;

    /** 核销码 */
    private String verifyCode;

    /** 核销后的状态：COMPLETED */
    private String status;

    /** 顾客 id */
    private Long customerId;

    /** 顾客昵称 */
    private String customerNickname;

    /** 顾客手机号 */
    private String customerPhone;

    /** 预约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentTime;

    /** 服务项目名称 */
    private String serviceItemName;

    /** 资源名称（技师名） */
    private String resourceName;

    /** 核销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifiedAt;

    /** 核销人（后台用户 id） */
    private Long verifiedBy;
}
