package com.beautysalon.controller;

import com.beautysalon.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 健康检查接口：用于验证项目能否正常启动。
 *
 * <p>启动成功后浏览器访问 http://localhost:8080/api/v1/ping，
 * 若返回 {"code":0,"message":"success","data":"pong - ..."} 说明后端已就绪。</p>
 *
 * <p>注解说明：</p>
 * <ul>
 *   <li>@RestController：表示这是一个处理 HTTP 请求的控制器，返回 JSON。</li>
 *   <li>@RequestMapping("/api/v1")：本类下所有接口的统一前缀。</li>
 *   <li>@GetMapping("/ping")：处理 GET 请求，完整路径为 /api/v1/ping。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong - " + LocalDateTime.now());
    }
}
