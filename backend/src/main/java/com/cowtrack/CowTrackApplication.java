package com.cowtrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CowTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CowTrackApplication.class, args);
    }
}
