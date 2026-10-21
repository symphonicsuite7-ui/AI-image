package com.example.aiimage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EditImageRequest {

    @Size(max = 2000, message = "统一风格提示词不能超过 2000 个字符")
    private String stylePrompt;

    @NotBlank(message = "修改提示词不能为空")
    @Size(max = 4000, message = "提示词不能超过 4000 个字符")
    private String prompt;

    @NotNull(message = "请上传需要修改的原图")
    private MultipartFile image;

    private MultipartFile mask;

    @NotBlank(message = "图片尺寸不能为空")
    @Pattern(
            regexp = "1024x1024|1024x1536|1536x1024|1152x2048|2048x1152",
            message = "不支持该图片尺寸"
    )
    private String size = "1024x1024";

    @NotBlank(message = "图片质量不能为空")
    @Pattern(regexp = "low|medium|high", message = "图片质量仅支持 low、medium 或 high")
    private String quality = "low";
}
