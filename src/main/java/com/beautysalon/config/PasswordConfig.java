package com.beautysalon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 *
 * <p>为什么需要它：密码绝不能明文存数据库。业界标准做法是存「哈希值」——
 * 把密码经过单向加密后存储，登录时再对用户输入做同样的加密并比对。</p>
 *
 * <p>BCrypt 的特点：</p>
 * <ul>
 *   <li>单向不可逆：拿到哈希也反推不出原密码。</li>
 *   <li>自带「盐」：同样的密码每次生成的哈希都不同，防止彩虹表攻击。</li>
 * </ul>
 *
 * <p>这里把 PasswordEncoder 定义成一个 Bean，之后在 Service 里直接注入使用。</p>
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder：业界标准密码哈希算法
        return new BCryptPasswordEncoder();
    }
}
