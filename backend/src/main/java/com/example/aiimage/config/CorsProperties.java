package com.example.aiimage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** Frontend origins allowed to call the REST API. */
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
}
