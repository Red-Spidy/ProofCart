package com.proofcart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ProofCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProofCartApplication.class, args);
    }

}
