package com.deanrobin.yx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class YxApplication {
    public static void main(String[] args) {
        SpringApplication.run(YxApplication.class, args);
    }
}
