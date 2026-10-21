package com.example.aiimage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExternalImageRequest(
        String model,
        String prompt,
        String size,
        String quality,
        @JsonProperty("response_format") String responseFormat
) {
}
