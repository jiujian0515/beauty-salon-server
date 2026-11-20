package com.beautysalon.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权配置
 *
 * <p>这里注册一个「拦截器」（类比前端路由守卫），对所有请求做登录校验：</p>
 * <ul>
 *   <li>放行（无需登录）：登录接口、公开的服务列表、可售时段、健康检查。</li>
 *   <li>其余 /api/v1/** 路径统一要求登录。</li>
 * </ul>
 *
 * <p>更细粒度的「角色控制」用注解在 Controller 方法上声明，
 * 例如 {@code @SaCheckRole("ADMIN")}，二者配合使用。</p>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 匹配 /api/v1/** 下所有请求，但排除下面这些「公开路径」
            SaRouter.match("/api/v1/**")
                    .notMatch(
                            "/api/v1/auth/login",              // 后台登录（公开）；logout/me 等需要登录
                            "/api/v1/customer/auth/register",  // 顾客自助注册（公开）
                            "/api/v1/customer/auth/login",     // 顾客登录（公开）
                            "/api/v1/services",                // 服务项目列表（公开）
                            "/api/v1/slots",                   // 可售时段查询（公开）
                            "/api/v1/ping"                     // 健康检查
                    )
                    .check(r -> StpUtil.checkLogin()); // 其余路径校验是否登录
        })).addPathPatterns("/**");
    }
}
