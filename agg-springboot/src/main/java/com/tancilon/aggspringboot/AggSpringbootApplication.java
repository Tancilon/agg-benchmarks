package com.tancilon.aggspringboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tancilon.aggspringboot.mapper")
public class AggSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggSpringbootApplication.class, args);
    }

}
