package com.example.aiimage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aiimage.config.ImageProperties;
import com.example.aiimage.dto.EditImageRequest;
import com.example.aiimage.dto.EditImageUrlRequest;
import com.example.aiimage.dto.ExternalImageEditUrlRequest;
import com.example.aiimage.dto.ExternalImageRequest;
import com.example.aiimage.dto.ExternalImageResponse;
import com.example.aiimage.dto.GenerateImageRequest;
import com.example.aiimage.dto.GenerateImageResponse;
import com.example.aiimage.entity.ImageHistory;
import com.example.aiimage.exception.BusinessException;
import com.example.aiimage.mapper.ImageHistoryMapper;
import com.example.aiimage.service.ImageService;
import com.example.aiimage.utils.ImageFileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    private final ImageHistoryMapper imageHistoryMapper;
    private final ImageProperties imageProperties;
    private final RestClient imageRestClient;
    private final ImageFileUtils imageFileUtils;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateImageResponse generate(GenerateImageRequest request) {
        validateApiConfiguration(imageProperties.getApiKey(), "文生图");
        long startedAt = System.currentTimeMillis();
        log.info("Start text-to-image request: model={}, size={}, quality={}",
                imageProperties.getModel(), request.getSize(), request.getQuality());

        ExternalImageRequest externalRequest = new ExternalImageRequest(
                imageProperties.getModel(),
                buildFinalPrompt(request.getStylePrompt(), request.getPrompt()),
                request.getSize(),
                request.getQuality(),
                "b64_json"
        );

        ExternalImageResponse externalResponse = callJsonApi(
                imageProperties.getBaseUrl(), externalRequest, "文生图", imageProperties.getApiKey()
        );
        return saveResult(request.getPrompt(), request.getSize(), request.getQuality(), startedAt, externalResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateImageResponse edit(EditImageRequest request) {
        String editApiKey = resolveEditApiKey();
        validateApiConfiguration(editApiKey, "改图");
        validateUpload(request.getImage(), "原图");
        if (request.getMask() != null && !request.getMask().isEmpty()) {
            validateUpload(request.getMask(), "遮罩图");
        }
        long startedAt = System.currentTimeMillis();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", imageProperties.getModel());
        body.add("prompt", buildFinalPrompt(request.getStylePrompt(), request.getPrompt()));
        addFilePart(body, "image", request.getImage());
        if (request.getMask() != null && !request.getMask().isEmpty()) {
            addFilePart(body, "mask", request.getMask());
        }
        body.add("size", request.getSize());
        body.add("quality", request.getQuality());
        body.add("response_format", "b64_json");

        ExternalImageResponse externalResponse;
        try {
            externalResponse = imageRestClient.post()
                    .uri(imageProperties.getEditUrl())
                    .headers(headers -> headers.setBearerAuth(editApiKey))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(ExternalImageResponse.class);
        } catch (RestClientResponseException ex) {
            throw upstreamException("改图", ex);
        } catch (RestClientException ex) {
            log.error("Image edit API request failed", ex);
            throw new BusinessException(502, "改图服务调用失败，请稍后重试", ex);
        }

        return saveResult(request.getPrompt(), request.getSize(), request.getQuality(), startedAt, externalResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateImageResponse editByUrl(EditImageUrlRequest request) {
        String editApiKey = resolveEditApiKey();
        validateApiConfiguration(editApiKey, "改图");
        long startedAt = System.currentTimeMillis();
        ExternalImageEditUrlRequest externalRequest = new ExternalImageEditUrlRequest(
                imageProperties.getModel(),
                buildFinalPrompt(request.getStylePrompt(), request.getPrompt()),
                List.of(new ExternalImageEditUrlRequest.ImageReference(request.getImageUrl().trim())),
                request.getSize(),
                request.getQuality(),
                "b64_json"
        );
        ExternalImageResponse externalResponse = callJsonApi(
                imageProperties.getEditUrl(), externalRequest, "图片 URL 改图", editApiKey
        );
        return saveResult(request.getPrompt(), request.getSize(), request.getQuality(), startedAt, externalResponse);
    }

    private String buildFinalPrompt(String stylePrompt, String prompt) {
        String contentPrompt = prompt.trim();
        if (!StringUtils.hasText(stylePrompt)) {
            return contentPrompt;
        }
        return "Unified Style Requirements:\n"
                + stylePrompt.trim()
                + "\n\nContent of This Image:\n"
                + contentPrompt;
    }

    private GenerateImageResponse saveResult(
            String prompt,
            String size,
            String quality,
            long startedAt,
            ExternalImageResponse externalResponse
    ) {
        String base64 = extractBase64(externalResponse);
        String imageUrl = imageFileUtils.saveBase64Png(base64);

        ImageHistory history = new ImageHistory();
        history.setPrompt(prompt.trim());
        history.setImageUrl(imageUrl);
        history.setModel(imageProperties.getModel());
        history.setSize(size);
        history.setQuality(quality);
        history.setCreateTime(LocalDateTime.now());

        try {
            imageHistoryMapper.insert(history);
        } catch (RuntimeException ex) {
            imageFileUtils.deleteByPublicUrl(imageUrl);
            throw ex;
        }

        return new GenerateImageResponse(
                history.getId(),
                imageUrl,
                System.currentTimeMillis() - startedAt
        );
    }

    private ExternalImageResponse callJsonApi(String url, Object request, String operationName, String apiKey) {
        try {
            return imageRestClient.post()
                    .uri(url)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExternalImageResponse.class);
        } catch (RestClientResponseException ex) {
            throw upstreamException(operationName, ex);
        } catch (RestClientException ex) {
            log.error("{} API request failed", operationName, ex);
            throw new BusinessException(502, operationName + "服务调用失败，请稍后重试", ex);
        }
    }

    private BusinessException upstreamException(String operationName, RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        String upstreamMessage = extractUpstreamMessage(responseBody);
        log.error("{} upstream request failed: status={}, response={}",
                operationName, ex.getStatusCode(), abbreviate(responseBody));

        String message = operationName + "服务拒绝了请求（上游 " + ex.getStatusCode().value() + "）";
        if (StringUtils.hasText(upstreamMessage)) {
            message += "：" + upstreamMessage;
        }
        return new BusinessException(502, message, ex);
    }

    private String extractUpstreamMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText();
            return StringUtils.hasText(message) ? message : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String abbreviate(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500) + "...";
    }

    private void addFilePart(MultiValueMap<String, Object> body, String partName, MultipartFile file) {
        try {
            String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                    ? file.getOriginalFilename()
                    : partName + ".png";
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(resolveMediaType(file));
            body.add(partName, new HttpEntity<>(resource, headers));
        } catch (IOException ex) {
            throw new BusinessException(400, "读取上传图片失败", ex);
        }
    }

    private void validateUpload(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传" + label);
        }
        if (file.getSize() > imageProperties.getMaxUploadBytes()) {
            throw new BusinessException(400, label + "不能超过 20MB");
        }
        String contentType = file.getContentType();
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        boolean allowedType = StringUtils.hasText(contentType)
                && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean allowedExtension = StringUtils.hasText(extension)
                && Set.of("png", "jpg", "jpeg", "webp").contains(extension.toLowerCase(Locale.ROOT));
        if (!allowedType && !allowedExtension) {
            throw new BusinessException(400, label + "仅支持 PNG、JPG、JPEG 或 WebP 格式");
        }
    }

    private MediaType resolveMediaType(MultipartFile file) {
        if (StringUtils.hasText(file.getContentType())) {
            return MediaType.parseMediaType(file.getContentType());
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_PNG;
        };
    }

    @Override
    public Page<ImageHistory> getHistory(long page, long size) {
        return imageHistoryMapper.selectPage(
                Page.of(page, size),
                Wrappers.<ImageHistory>lambdaQuery().orderByDesc(ImageHistory::getCreateTime)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ImageHistory history = imageHistoryMapper.selectById(id);
        if (history == null) {
            throw new BusinessException(404, "历史记录不存在");
        }
        imageHistoryMapper.deleteById(id);
        imageFileUtils.deleteByPublicUrl(history.getImageUrl());
    }

    private void validateApiConfiguration(String apiKey, String operationName) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(500, "服务端未配置" + operationName + " API Key");
        }
    }

    private String resolveEditApiKey() {
        return StringUtils.hasText(imageProperties.getEditApiKey())
                ? imageProperties.getEditApiKey()
                : imageProperties.getApiKey();
    }

    private String extractBase64(ExternalImageResponse response) {
        if (response == null) {
            throw new BusinessException(502, "图片生成服务返回空响应");
        }
        List<ExternalImageResponse.ImageData> data = response.data();
        if (data == null || data.isEmpty() || !StringUtils.hasText(data.get(0).base64Json())) {
            throw new BusinessException(502, "图片生成服务未返回有效图片数据");
        }
        return data.get(0).base64Json();
    }
}
