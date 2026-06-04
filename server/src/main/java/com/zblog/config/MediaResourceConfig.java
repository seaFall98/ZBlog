package com.zblog.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadsDir = ensureUploadsDir();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsDir.toUri().toString());
    }

    private Path ensureUploadsDir() {
        Path uploadsDir = Path.of("uploads").toAbsolutePath().normalize();
        try {
            return Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create uploads directory: " + uploadsDir, e);
        }
    }
}
