package com.beautysalon.common;

import lombok.Getter;

/**
 * 业务异常：当业务规则不满足时主动抛出（如「时段已满」「状态不允许」）。
 *
 * <p>抛出后会被 {@link GlobalExceptionHandler} 捕获，统一转成对应的 Result 返回给前端。</p>
 *
 * <p>用法示例：</p>
 * <pre>
 * if (库存不足) {
 *     throw new BusinessException(ErrorCode.SLOT_FULL);
 * }
 * </pre>
 *
 * <p>类比前端：相当于 throw new Error() 触发 catch 分支，只是这里用类型区分错误。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 关联的错误码（决定最终返回给前端的 code 和默认 message） */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 允许携带自定义错误信息（仍使用指定错误码） */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
