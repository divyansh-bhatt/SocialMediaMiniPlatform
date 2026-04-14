package com.connectsphere.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }

    // Used to call post-service /posts/internal/{id}/comments/increment|decrement
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
