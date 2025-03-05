package com.tancilon.aggspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EntityScan("com.tancilon.aggspringboot.entity")
@EnableJpaRepositories("com.tancilon.aggspringboot.repository")
@EnableTransactionManagement
public class AggSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggSpringbootApplication.class, args);
    }

}
