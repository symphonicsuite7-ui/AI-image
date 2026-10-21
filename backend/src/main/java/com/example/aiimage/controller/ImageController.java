package com.example.aiimage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aiimage.common.ApiResponse;
import com.example.aiimage.dto.EditImageRequest;
import com.example.aiimage.dto.EditImageUrlRequest;
import com.example.aiimage.dto.GenerateImageRequest;
import com.example.aiimage.dto.GenerateImageResponse;
import com.example.aiimage.entity.ImageHistory;
import com.example.aiimage.service.ImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/generate")
    public ApiResponse<GenerateImageResponse> generate(@Valid @RequestBody GenerateImageRequest request) {
        return ApiResponse.success(imageService.generate(request));
    }

    @PostMapping(value = "/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GenerateImageResponse> edit(@Valid @ModelAttribute EditImageRequest request) {
        return ApiResponse.success(imageService.edit(request));
    }

    @PostMapping(value = "/edit-by-url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<GenerateImageResponse> editByUrl(@Valid @RequestBody EditImageUrlRequest request) {
        return ApiResponse.success(imageService.editByUrl(request));
    }

    @GetMapping("/history")
    public ApiResponse<Page<ImageHistory>> history(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) long size) {
        return ApiResponse.success(imageService.getHistory(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id) {
        imageService.delete(id);
        return ApiResponse.success();
    }
}
