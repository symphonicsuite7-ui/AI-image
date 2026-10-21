package com.example.aiimage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aiimage.dto.EditImageRequest;
import com.example.aiimage.dto.EditImageUrlRequest;
import com.example.aiimage.dto.GenerateImageRequest;
import com.example.aiimage.dto.GenerateImageResponse;
import com.example.aiimage.entity.ImageHistory;

public interface ImageService {
    GenerateImageResponse generate(GenerateImageRequest request);

    GenerateImageResponse edit(EditImageRequest request);

    GenerateImageResponse editByUrl(EditImageUrlRequest request);

    Page<ImageHistory> getHistory(long page, long size);

    void delete(Long id);
}
