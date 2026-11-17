package com.beautysalon.common;

import lombok.Getter;

/**
 * 业务错误码（与 README-BTD.md 第 4 节约定保持一致）
 *
 * <p>前端拿到 code 后可做统一处理，例如：</p>
 * <ul>
 *   <li>40101：跳转登录页</li>
 *   <li>40901：提示「时段已满，请选择其他时段」</li>
 * </ul>
 *
 * <p>编码约定：5 位数字，前两位为 HTTP 语义（40 客户端错误 / 50 服务端错误），
 * 后三位为业务细分。</p>
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40001, "参数错误"),
    NOT_LOGIN(40101, "未登录 / 登录失效"),
    NO_PERMISSION(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    SLOT_FULL(40901, "时段已满"),
    STATUS_INVALID(40902, "预约状态不允许该操作"),
    CANCEL_TOO_LATE(40903, "距离服务开始不足 24 小时，不可取消"),
    SYSTEM_ERROR(50000, "系统异常");

    /** 错误码 */
    private final int code;
    /** 错误信息 */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
