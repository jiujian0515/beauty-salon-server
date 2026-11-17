package com.beautysalon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置（CORS，Cross-Origin Resource Sharing）
 *
 * <p>前端开发时（例如 http://localhost:5173 的 React 项目）直接请求后端 8080 端口，
 * 浏览器会因「跨域」拦截请求。这里放行跨域，方便前后端分离开发。</p>
 *
 * <p>注意：多个类都可以实现 WebMvcConfigurer，Spring 会合并它们，互不冲突。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")           // 对所有路径生效
                .allowedOriginPatterns("*")  // 允许所有来源（生产环境应收紧为具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)       // 允许携带 cookie / 认证头
                .maxAge(3600);                // 预检请求缓存时间（秒）
    }
}
