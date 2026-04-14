package com.connectsphere.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class PostServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostServiceApplication.class, args);
    }

    // RestTemplate bean — used for inter-service HTTP calls to auth-service
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
