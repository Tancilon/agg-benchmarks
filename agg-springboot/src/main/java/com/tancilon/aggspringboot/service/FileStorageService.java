package com.tancilon.aggspringboot.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import com.tancilon.aggspringboot.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.util.FileSystemUtils;
import com.tancilon.aggspringboot.dto.DownloadResultsRequest;

@Service
public class FileStorageService {

    private Path fileStorageLocation;

    @Autowired
    private StorageProperties storageProperties;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(storageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            // 创建子目录
            Files.createDirectories(this.fileStorageLocation.resolve("bib"));
            Files.createDirectories(this.fileStorageLocation.resolve("algorithmImp"));
            Files.createDirectories(this.fileStorageLocation.resolve("metricImp"));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public Path getFileStorageLocation() {
        return this.fileStorageLocation;
    }

    public String storeFile(MultipartFile file) {
        try {
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // 保存文件
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load file: " + fileName, e);
        }
    }
}