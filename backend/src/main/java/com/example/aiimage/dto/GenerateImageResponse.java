package com.example.aiimage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenerateImageResponse {
    private Long id;
    private String imageUrl;
    private long elapsedMillis;
}
