package com.stockguard.order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages = "com.stockguard")
@EntityScan("com.stockguard")
@EnableJpaRepositories("com.stockguard")
@EnableScheduling
public class OrderApplication { public static void main(String[] args) { SpringApplication.run(OrderApplication.class, args); } }
