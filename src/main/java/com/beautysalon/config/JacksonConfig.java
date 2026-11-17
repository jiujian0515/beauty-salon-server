package com.beautysalon.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 序列化配置（前后端数据交互的关键细节，前端同学务必理解）
 *
 * <p><b>问题背景：</b>数据库主键是 BIGINT（64 位），Java 里用 Long 表示。
 * 但前端 JavaScript 的 Number 类型只能精确表示 2^53 以内的整数，
 * 超过这个范围的 Long 会在序列化给前端时「丢失精度」，出现 id 对不上的诡异 bug。</p>
 *
 * <p><b>解决：</b>把 Long 类型统一序列化成字符串返回给前端，前端按字符串处理。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            // Long 类型 → 字符串（解决前端 JS 精度丢失问题）
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
