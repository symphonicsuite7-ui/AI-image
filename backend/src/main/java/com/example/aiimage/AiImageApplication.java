package com.example.aiimage;

import com.example.aiimage.config.CorsProperties;
import com.example.aiimage.config.ImageProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.example.aiimage.mapper")
@EnableConfigurationProperties({ImageProperties.class, CorsProperties.class})
public class AiImageApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiImageApplication.class, args);
    }
}
