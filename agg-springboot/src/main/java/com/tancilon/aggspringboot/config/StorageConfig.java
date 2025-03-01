package com.tancilon.aggspringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
public class StorageConfig implements WebMvcConfigurer {

    private static final String[] UPLOAD_DIRS = {
            "./uploads",
            "./uploads/bib",
            "./uploads/algorithmImp"
    };

    @PostConstruct
    public void init() {
        // 创建必要的目录
        for (String dir : UPLOAD_DIRS) {
            File file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}