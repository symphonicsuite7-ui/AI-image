package com.example.aiimage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExternalImageEditUrlRequest(
        String model,
        String prompt,
        List<ImageReference> images,
        String size,
        String quality,
        @JsonProperty("response_format") String responseFormat
) {
    public record ImageReference(@JsonProperty("image_url") String imageUrl) {
    }
}
