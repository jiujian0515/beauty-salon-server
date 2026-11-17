package com.beautysalon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动入口（类比前端的 main.ts / index.js）
 *
 * <p>两个核心注解：</p>
 * <ul>
 *   <li>
 *     {@code @SpringBootApplication}：组合注解，作用是
 *     <b>开启自动配置</b> + <b>扫描当前包(com.beautysalon)及其子包下所有组件</b>。
 *     所以本项目所有 Java 类都要放在 com.beautysalon 包下，才能被 Spring 找到。
 *   </li>
 *   <li>
 *     {@code @MapperScan}：告诉 MyBatis-Plus 去 com.beautysalon.mapper 包找 Mapper 接口，
 *     并自动为其生成实现类（动态代理）。
 *   </li>
 * </ul>
 *
 * <p><b>启动流程（重要，理解了这个就理解了 Spring Boot）：</b></p>
 * <ol>
 *   <li>执行 main 方法，调用 SpringApplication.run。</li>
 *   <li>Spring 扫描 com.beautysalon 包下所有带注解的类，把它们「装配」成一个个 Bean（对象）。</li>
 *   <li>读取 application.yml 配置，初始化数据源、MyBatis-Plus、Sa-Token、Flyway 等组件。</li>
 *   <li>Flyway 自动执行 db/migration 目录下的 SQL，完成建表。</li>
 *   <li>启动内嵌 Tomcat，监听 8080 端口，对外提供 HTTP 接口。</li>
 * </ol>
 */
@SpringBootApplication
@MapperScan("com.beautysalon.mapper")
public class BeautySalonApplication {

    public static void main(String[] args) {
        // 这一行会启动整个 Spring 容器（类比 npm run dev 启动整个应用）
        SpringApplication.run(BeautySalonApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  美容院启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
