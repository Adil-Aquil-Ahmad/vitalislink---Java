package com.vitalislink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VitalisLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitalisLinkApplication.class, args);
    }
}