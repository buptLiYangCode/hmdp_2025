package com.example.hmdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@EnableConfigurationProperties
public class HmdpApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmdpApplication.class, args);
    }

}
