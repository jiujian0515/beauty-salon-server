package com.beautysalon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信小程序配置（app.wechat.*）
 *
 * <p>AppID / AppSecret 属于敏感信息，不写死在配置文件里，
 * 通过环境变量 WECHAT_APP_ID / WECHAT_APP_SECRET 注入。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.wechat")
public class WechatProperties {

    /** 小程序 AppID */
    private String appId;

    /** 小程序 AppSecret（切勿返回给前端或写入日志） */
    private String appSecret;

    /** 微信接口域名（测试时可替换为本地桩服务） */
    private String apiBaseUrl = "https://api.weixin.qq.com";

    /** 是否已配置真实微信凭据 */
    public boolean isConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret);
    }
}
