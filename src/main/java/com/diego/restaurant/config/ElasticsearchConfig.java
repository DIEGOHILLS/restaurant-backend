package com.diego.restaurant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.diego.restaurant.repositories")
public class ElasticsearchConfig {
}
