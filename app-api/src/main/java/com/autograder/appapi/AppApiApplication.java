package com.autograder.appapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Modular monolith: scan the domain modules (com.autograder.*), not just this package.
@SpringBootApplication(scanBasePackages = "com.autograder")
@EntityScan("com.autograder")
@EnableJpaRepositories("com.autograder")
public class AppApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApiApplication.class, args);
    }
}
