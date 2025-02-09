package com.minecraft.craft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CraftApplication {

    public static void main(String[] args) {
        SpringApplication.run(CraftApplication.class, args);
    }

}
