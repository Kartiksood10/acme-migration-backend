package com.acme.migration.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Since the frontend and backend run on different ports, we need to whitelist the front end URLs
    // So that the UI can access data from the backend with no CORS error
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Apply only to the API endpoints
                .allowedOrigins("http://localhost:3000", "http://localhost:4200") // Whitelist frontend URLs
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}

