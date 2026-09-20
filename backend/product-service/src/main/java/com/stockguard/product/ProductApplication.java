package com.stockguard.product;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages = "com.stockguard")
@EntityScan("com.stockguard")
@EnableJpaRepositories("com.stockguard")
@EnableScheduling
public class ProductApplication { public static void main(String[] args) { SpringApplication.run(ProductApplication.class, args); } }
