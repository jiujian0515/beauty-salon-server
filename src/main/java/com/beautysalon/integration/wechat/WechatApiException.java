package com.beautysalon.integration.wechat;

import lombok.Getter;

/**
 * 微信接口异常
 *
 * <p>封装微信返回的 errcode / errmsg，或网络调用失败。
 * 该异常只在服务端内部流转，最终由业务层转换成对用户友好的提示，
 * 不把微信原始错误直接暴露给前端。</p>
 */
@Getter
public class WechatApiException extends RuntimeException {

    /** 微信错误码；-1 表示网络/格式等非微信业务错误 */
    private final int errcode;

    public WechatApiException(int errcode, String errmsg) {
        super(errmsg);
        this.errcode = errcode;
    }

    public WechatApiException(int errcode, String errmsg, Throwable cause) {
        super(errmsg, cause);
        this.errcode = errcode;
    }

    public static WechatApiException network(Throwable cause) {
        return new WechatApiException(-1, "微信接口调用失败", cause);
    }
}
