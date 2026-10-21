package com.example.aiimage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExternalImageResponse(long created, List<ImageData> data) {
    public record ImageData(@JsonProperty("b64_json") String base64Json) {
    }
}
