package com.example.aiimage.utils;

import com.example.aiimage.config.ImageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ImageFileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldConvertToPngAndDeleteBase64File() throws Exception {
        ImageProperties properties = new ImageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicPath("/uploads/images");
        ImageFileUtils utils = new ImageFileUtils(properties);

        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, Color.MAGENTA.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(source, "jpg", output);
        String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
        String publicUrl = utils.saveBase64Png(base64);

        assertThat(publicUrl).startsWith("/uploads/images/").endsWith(".png");
        String relative = publicUrl.substring("/uploads/images/".length());
        Path saved = tempDir.resolve(relative);
        assertThat(Files.exists(saved)).isTrue();
        assertThat(ImageIO.read(saved.toFile())).isNotNull();
        assertThat(Files.readAllBytes(saved))
                .startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);

        utils.deleteByPublicUrl(publicUrl);
        assertThat(Files.exists(saved)).isFalse();
    }
}
