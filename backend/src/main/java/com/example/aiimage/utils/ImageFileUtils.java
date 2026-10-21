package com.example.aiimage.utils;

import com.example.aiimage.config.ImageProperties;
import com.example.aiimage.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageFileUtils {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final ImageProperties imageProperties;

    public String saveBase64Png(String base64Content) {
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(stripDataUrlPrefix(base64Content));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(502, "图片数据 Base64 解码失败", ex);
        }
        if (imageBytes.length == 0 || imageBytes.length > imageProperties.getMaxImageBytes()) {
            throw new BusinessException(502, "图片数据大小异常");
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException ex) {
            throw new BusinessException(502, "图片数据解析失败", ex);
        }
        if (image == null) {
            throw new BusinessException(502, "图片生成服务返回了无效图片");
        }

        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".png";
        Path root = getUploadRoot();
        Path target = root.resolve(datePath).resolve(fileName).normalize();
        ensureInsideRoot(root, target);

        try {
            Files.createDirectories(target.getParent());
            try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                if (!ImageIO.write(image, "png", output)) {
                    throw new IOException("No PNG writer is available");
                }
            }
        } catch (IOException ex) {
            deleteQuietly(target);
            throw new BusinessException(500, "图片保存失败", ex);
        }

        return normalizePublicPath() + "/" + datePath + "/" + fileName;
    }

    public void deleteByPublicUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(normalizePublicPath() + "/")) {
            log.warn("Skip deleting invalid image url: {}", imageUrl);
            return;
        }

        String relativePath = imageUrl.substring(normalizePublicPath().length() + 1);
        Path root = getUploadRoot();
        Path target = root.resolve(relativePath).normalize();
        ensureInsideRoot(root, target);

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            // Database deletion is more important; orphan files can be cleaned asynchronously later.
            log.warn("Failed to delete image file: {}", target, ex);
        }
    }

    private String stripDataUrlPrefix(String value) {
        int commaIndex = value.indexOf(',');
        return value.startsWith("data:") && commaIndex >= 0 ? value.substring(commaIndex + 1) : value;
    }

    private Path getUploadRoot() {
        return Path.of(imageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    private void ensureInsideRoot(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new BusinessException(400, "非法图片路径");
        }
    }

    private void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException cleanupError) {
            log.warn("Failed to clean incomplete image file: {}", target, cleanupError);
        }
    }

    private String normalizePublicPath() {
        String path = imageProperties.getPublicPath();
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
