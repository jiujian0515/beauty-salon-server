package com.beautysalon.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（类比前端 axios 的 response 拦截器 + 全局错误兜底）
 *
 * <p>所有 Controller 抛出的异常都会经过这里，统一转换成 Result 结构返回，
 * 保证前端拿到的永远是 { code, message, data }，而不是 500 堆栈或裸错误。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：返回对应错误码（若抛异常时传了自定义信息，则用自定义信息） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    /** 参数校验失败（Controller 方法参数上用了 @Valid + @NotNull 等注解时触发） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        // 取第一个校验失败的字段及其提示，拼成可读信息
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getField() + " " + fieldError.getDefaultMessage()
                : "参数错误";
        log.warn("参数校验失败：{}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 未登录（Sa-Token 校验失败时抛出） */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(ErrorCode.NOT_LOGIN);
    }

    /** 无权限 / 无角色（Sa-Token 校验失败时抛出） */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handleNoPermission(Exception e) {
        return Result.fail(ErrorCode.NO_PERMISSION);
    }

    /** 兜底：其余所有未知异常，避免把内部堆栈直接暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 数据库唯一约束冲突（比如插入重复的手机号）
     *
     * <p>为什么要有这个兜底？因为「先查询再插入」存在并发窗口：
     * 两个请求同时查到「手机号不存在」，然后同时插入，就会有一个撞上唯一索引。
     * 这种异常如果落到通用兜底，前端只会看到「系统异常」，很难排查。</p>
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return Result.fail(ErrorCode.DUPLICATE_DATA.getCode(), "数据已存在（唯一字段冲突）");
    }
}
