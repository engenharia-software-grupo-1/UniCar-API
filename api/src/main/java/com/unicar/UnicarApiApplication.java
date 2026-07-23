package com.unicar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnicarApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnicarApiApplication.class, args);
    }
}
