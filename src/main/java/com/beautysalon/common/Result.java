package com.beautysalon.common;

import lombok.Data;

/**
 * 统一响应结构（类比前端 axios 响应拦截器统一包装的返回值）
 *
 * <p>所有接口都返回这个结构，前端只需要判断 code 是否为 0：</p>
 * <pre>
 * { "code": 0, "message": "success", "data": { ... } }
 * </pre>
 *
 * <p>用法示例（在 Controller 里）：</p>
 * <pre>
 * return Result.ok(data);        // 成功
 * return Result.fail(ErrorCode.SLOT_FULL); // 业务失败
 * </pre>
 *
 * @param <T> data 字段的类型，可以是任意对象或 null
 */
@Data
public class Result<T> {

    /** 业务码：0 成功，非 0 失败（具体含义见 {@link ErrorCode}） */
    private int code;

    /** 提示信息：成功为 "success"，失败为具体错误描述 */
    private String message;

    /** 返回数据：成功时为业务数据，失败时通常为 null */
    private T data;

    /** 私有构造器：不对外暴露，统一通过下面的静态方法创建 */
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    /** 成功（带数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /** 失败（按错误码） */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 失败（自定义提示信息，错误码固定为系统异常） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ErrorCode.SYSTEM_ERROR.getCode(), message, null);
    }

    /** 失败（自定义错误码 + 提示信息） */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
