package com.example.aiimage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "image")
public class ImageProperties {

    /** Third-party image API key, preferably injected through IMAGE_API_KEY. */
    private String apiKey;

    private String baseUrl = "https://api.likeai520.cc/v1/images/generations";

    private String editUrl = "https://api.likeai520.cc/v1/images/edits";

    /** Optional separate API key for the edit provider; falls back to apiKey when blank. */
    private String editApiKey;

    private String model = "gpt-image-2";

    /** Local storage root. Relative paths are resolved from the backend working directory. */
    private String uploadDir = "uploads/images";

    private String publicPath = "/uploads/images";

    private Duration connectTimeout = Duration.ofSeconds(15);

    private Duration readTimeout = Duration.ofMinutes(3);

    /** Reject unexpectedly large upstream responses before writing them to disk. */
    private long maxImageBytes = 25L * 1024 * 1024;

    /** Maximum size of each source image or mask uploaded for image editing. */
    private long maxUploadBytes = 20L * 1024 * 1024;
}
