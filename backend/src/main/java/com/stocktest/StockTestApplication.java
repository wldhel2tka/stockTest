package com.stocktest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockTestApplication.class, args);
    }
}
