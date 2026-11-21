package com.beautysalon.integration.wechat;

import com.beautysalon.config.WechatProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 微信小程序服务端接口客户端
 *
 * <p>负责三件事：</p>
 * <ol>
 *   <li>用 wx.login 的 code 换 openid（jscode2session）</li>
 *   <li>用 getPhoneNumber 的 code 换手机号（getuserphonenumber）</li>
 *   <li>维护 access_token（微信要求 2 小时刷新一次）</li>
 * </ol>
 *
 * <p>注意：微信接口返回的 Content-Type 有时是 text/plain，
 * 所以这里统一按字节读取后再用 Jackson 解析，避免消息转换器因 Content-Type 不匹配而失败。</p>
 */
@Slf4j
@Component
public class WechatMiniProgramClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final long TOKEN_REFRESH_AHEAD_MS = 5 * 60 * 1000L;

    /** access_token 失效相关错误码：刷新后重试一次 */
    private static final Set<Integer> TOKEN_INVALID_CODES = Set.of(40001, 40014, 42001);

    private final WechatProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Object tokenLock = new Object();

    private volatile String accessToken;
    private volatile long accessTokenExpiresAt;

    public WechatMiniProgramClient(WechatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        // 缓冲请求体，确保带 Content-Length，避免部分服务端不支持 chunked 请求
        requestFactory.setBufferRequestBody(true);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * 用 wx.login 的 code 换取 openid
     */
    public String getOpenid(String jsCode) {
        ensureConfigured();

        JsonNode response = get(
                "/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",
                properties.getAppId(),
                properties.getAppSecret(),
                jsCode
        );
        checkError(response, "获取微信登录凭证失败");

        String openid = text(response, "openid");
        if (!StringUtils.hasText(openid)) {
            throw new WechatApiException(-1, "微信返回的 openid 为空");
        }
        return openid;
    }

    /**
     * 用 getPhoneNumber 的 code 换取微信绑定手机号
     */
    public String getPhoneNumber(String phoneCode) {
        ensureConfigured();

        String token = getAccessToken(false);
        JsonNode response = requestPhone(token, phoneCode);

        // token 可能刚好过期，强制刷新后重试一次
        if (TOKEN_INVALID_CODES.contains(response.path("errcode").asInt(0))) {
            token = getAccessToken(true);
            response = requestPhone(token, phoneCode);
        }
        checkError(response, "获取微信手机号失败");

        JsonNode phoneInfo = response.path("phone_info");
        String phone = text(phoneInfo, "purePhoneNumber");
        if (!StringUtils.hasText(phone)) {
            phone = text(phoneInfo, "phoneNumber");
        }
        if (!StringUtils.hasText(phone)) {
            throw new WechatApiException(-1, "微信返回的手机号为空");
        }
        return phone;
    }

    /**
     * 获取 access_token（带内存缓存）
     *
     * @param forceRefresh 是否强制刷新
     */
    public String getAccessToken(boolean forceRefresh) {
        ensureConfigured();

        long now = System.currentTimeMillis();
        String cached = accessToken;
        if (!forceRefresh && cached != null && now < accessTokenExpiresAt) {
            return cached;
        }

        synchronized (tokenLock) {
            now = System.currentTimeMillis();
            cached = accessToken;
            if (!forceRefresh && cached != null && now < accessTokenExpiresAt) {
                return cached;
            }

            JsonNode response = get(
                    "/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}",
                    properties.getAppId(),
                    properties.getAppSecret()
            );
            checkError(response, "获取微信 access_token 失败");

            String token = text(response, "access_token");
            long expiresIn = response.path("expires_in").asLong(7200);
            if (!StringUtils.hasText(token)) {
                throw new WechatApiException(-1, "微信返回的 access_token 为空");
            }

            accessToken = token;
            long ttlMs = Math.max(60, expiresIn) * 1000L;
            // 至少缓存 1 分钟，避免 expires_in 很小时提前量把过期时间算成过去
            accessTokenExpiresAt = System.currentTimeMillis()
                    + Math.max(ttlMs - TOKEN_REFRESH_AHEAD_MS, 60_000L);
            log.info("微信 access_token 已刷新，有效期 {} 秒", expiresIn);
            return token;
        }
    }

    private JsonNode requestPhone(String token, String phoneCode) {
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(Map.of("code", phoneCode));
        } catch (JsonProcessingException e) {
            throw new WechatApiException(-1, "微信手机号请求参数序列化失败", e);
        }
        try {
            // 用 byte[] 收发，避免微信返回 text/plain 时消息转换器因 Content-Type 不匹配而失败
            byte[] body = restClient.post()
                    .uri(properties.getApiBaseUrl() + "/wxa/business/getuserphonenumber?access_token={token}", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(byte[].class);
            return parse(body);
        } catch (RestClientException e) {
            // 只记录异常类型，避免把带 secret / access_token 的 URL 写进日志
            log.warn("微信手机号接口请求失败：{}", e.getClass().getSimpleName());
            throw WechatApiException.network(e);
        }
    }

    private JsonNode get(String path, Object... uriVariables) {
        try {
            byte[] body = restClient.get()
                    .uri(properties.getApiBaseUrl() + path, uriVariables)
                    .retrieve()
                    .body(byte[].class);
            return parse(body);
        } catch (RestClientException e) {
            // 只记录异常类型，避免把带 secret / access_token 的 URL 写进日志
            log.warn("微信接口请求失败：{}", e.getClass().getSimpleName());
            throw WechatApiException.network(e);
        }
    }

    private JsonNode parse(byte[] body) {
        if (body == null || body.length == 0) {
            throw new WechatApiException(-1, "微信接口返回内容为空");
        }
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw new WechatApiException(-1, "微信接口返回格式异常", e);
        }
    }

    private void checkError(JsonNode response, String action) {
        int errcode = response.path("errcode").asInt(0);
        if (errcode != 0) {
            String errmsg = response.path("errmsg").asText("未知错误");
            log.warn("{}：errcode={}, errmsg={}", action, errcode, errmsg);
            throw new WechatApiException(errcode, errmsg);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new WechatApiException(-1, "微信登录未配置");
        }
    }
}
