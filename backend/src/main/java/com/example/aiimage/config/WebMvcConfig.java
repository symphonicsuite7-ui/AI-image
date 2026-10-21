package com.example.aiimage.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ImageProperties imageProperties;
    private final CorsProperties corsProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(imageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(normalizePublicPath() + "/**")
                .addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    private String normalizePublicPath() {
        String path = imageProperties.getPublicPath();
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
