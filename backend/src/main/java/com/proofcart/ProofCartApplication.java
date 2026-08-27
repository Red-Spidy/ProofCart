package com.proofcart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class ProofCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProofCartApplication.class, args);
    }

}
