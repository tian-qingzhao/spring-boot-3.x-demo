package com.tqz.spring.boot3.x.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Spring Boot 3.x 版本的demo。
 *
 * @author tianqingzhao
 * @since 2022/12/12 12:34
 * @see org.springframework.boot.SpringApplicationAotProcessor
 */
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
