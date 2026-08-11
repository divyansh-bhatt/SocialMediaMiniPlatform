package com.connectsphere.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

/**
 * AppConfig — wires both JPA (MySQL) and Elasticsearch repositories.
 *
 * IMPORTANT: Because this service uses BOTH Spring Data JPA and
 * Spring Data Elasticsearch, we must explicitly tell Spring which
 * packages belong to which repository type. Without this, Spring
 * gets confused and tries to create JPA repositories for ES entities.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.connectsphere.search.repository",
        includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.connectsphere.search.repository.HashtagRepository.class,
                        com.connectsphere.search.repository.PostHashtagRepository.class
                }
        )
)
@EnableElasticsearchRepositories(
        basePackages = "com.connectsphere.search.repository",
        includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.connectsphere.search.repository.PostSearchRepository.class,
                        com.connectsphere.search.repository.UserSearchRepository.class
                }
        )
)
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
