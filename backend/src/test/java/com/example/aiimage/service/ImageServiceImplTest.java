package com.example.aiimage.service;

import com.example.aiimage.config.ImageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aiimage.dto.EditImageRequest;
import com.example.aiimage.dto.EditImageUrlRequest;
import com.example.aiimage.dto.GenerateImageRequest;
import com.example.aiimage.dto.GenerateImageResponse;
import com.example.aiimage.entity.ImageHistory;
import com.example.aiimage.mapper.ImageHistoryMapper;
import com.example.aiimage.service.impl.ImageServiceImpl;
import com.example.aiimage.utils.ImageFileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private ImageHistoryMapper imageHistoryMapper;

    @Mock
    private ImageFileUtils imageFileUtils;

    private MockRestServiceServer server;
    private ImageServiceImpl imageService;
    private ImageProperties properties;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        properties = new ImageProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://example.test/v1/images/generations");
        properties.setEditUrl("https://example.test/v1/images/edits");
        properties.setModel("gpt-image-2");

        when(imageFileUtils.saveBase64Png("ZmFrZS1pbWFnZQ=="))
                .thenReturn("/uploads/images/result.png");
        doAnswer(invocation -> {
            ImageHistory history = invocation.getArgument(0);
            history.setId(100L);
            return 1;
        }).when(imageHistoryMapper).insert(any(ImageHistory.class));

        imageService = new ImageServiceImpl(
                imageHistoryMapper,
                properties,
                builder.build(),
                imageFileUtils,
                new ObjectMapper()
        );
    }

    @Test
    void shouldCombineStylePromptForTextToImage() {
        server.expect(once(), requestTo(properties.getBaseUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "gpt-image-2",
                          "prompt": "统一风格要求：\n电影级概念艺术，冷色调，高细节\n\n本次图片内容：\n漂浮在天空中的机械城市",
                          "size": "1024x1024",
                          "quality": "low",
                          "response_format": "b64_json"
                        }
                        """))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        GenerateImageRequest request = new GenerateImageRequest();
        request.setStylePrompt("电影级概念艺术，冷色调，高细节");
        request.setPrompt("漂浮在天空中的机械城市");

        GenerateImageResponse response = imageService.generate(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getImageUrl()).isEqualTo("/uploads/images/result.png");
        server.verify();
    }

    @Test
    void shouldCallMultipartEditApi() {
        server.expect(once(), requestTo(properties.getEditUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.startsWith("multipart/form-data")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("统一风格要求")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("复古胶片，暖色调")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("改成国潮海报")))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        EditImageRequest request = new EditImageRequest();
        request.setStylePrompt("复古胶片，暖色调");
        request.setPrompt("改成国潮海报");
        request.setImage(new MockMultipartFile(
                "image", "input.png", MediaType.IMAGE_PNG_VALUE, "source".getBytes(StandardCharsets.UTF_8)
        ));

        GenerateImageResponse response = imageService.edit(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getImageUrl()).isEqualTo("/uploads/images/result.png");
        server.verify();
    }

    @Test
    void shouldCallJsonEditApiForPublicImageUrl() {
        server.expect(once(), requestTo(properties.getEditUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "gpt-image-2",
                          "prompt": "统一风格要求：\n清新水彩插画，柔和配色\n\n本次图片内容：\n保留主体，改成水彩风格",
                          "images": [{"image_url": "https://example.com/input.png"}],
                          "size": "1024x1024",
                          "quality": "low",
                          "response_format": "b64_json"
                        }
                        """))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        EditImageUrlRequest request = new EditImageUrlRequest();
        request.setStylePrompt("清新水彩插画，柔和配色");
        request.setPrompt("保留主体，改成水彩风格");
        request.setImageUrl("https://example.com/input.png");

        GenerateImageResponse response = imageService.editByUrl(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getImageUrl()).isEqualTo("/uploads/images/result.png");
        server.verify();
    }

    private String successResponse() {
        return """
                {
                  "created": 1783007786,
                  "data": [{"b64_json": "ZmFrZS1pbWFnZQ=="}]
                }
                """;
    }
}
