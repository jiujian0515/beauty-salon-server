package com.beautysalon.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * <p>核心是注册「分页插件」：MyBatis-Plus 默认不带分页能力，需要手动挂上拦截器。
 * 之后在 Service 层调用 {@code page(new Page<>(页码, 每页条数), 查询条件)}
 * 即可自动生成 MySQL 的 LIMIT 分页 SQL。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册分页插件
     *
     * <p>{@code @Bean} 表示：把这个方法返回的对象交给 Spring 容器管理
     * （类比前端把工具函数挂到全局，其他地方可以直接注入使用）。</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 指定数据库类型为 MySQL，分页插件会生成 MySQL 对应的 LIMIT 语法
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
