package com.tancilon.aggspringboot.service;

import com.tancilon.aggspringboot.entity.Algorithm;
import com.tancilon.aggspringboot.repository.AlgorithmRepository;
import com.tancilon.aggspringboot.config.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class AlgorithmService {

    private final AlgorithmRepository algorithmRepository;
    private final StorageProperties storageProperties;
    private final String uploadDir;
    private final String bibDir;
    private final String algorithmImpDir;

    public AlgorithmService(AlgorithmRepository algorithmRepository, StorageProperties storageProperties) {
        this.algorithmRepository = algorithmRepository;
        this.storageProperties = storageProperties;

        // 使用绝对路径
        this.uploadDir = new File(storageProperties.getUploadDir()).getAbsolutePath();
        this.bibDir = new File(uploadDir, "bib").getAbsolutePath();
        this.algorithmImpDir = new File(uploadDir, "algorithmImp").getAbsolutePath();

        // 初始化目录
        ensureDirectoryExists(uploadDir);
        ensureDirectoryExists(bibDir);
        ensureDirectoryExists(algorithmImpDir);
    }

    private void ensureDirectoryExists(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Created directory: " + directory.getAbsolutePath());
            } else {
                throw new RuntimeException("Failed to create directory: " + directory.getAbsolutePath());
            }
        }
    }

    public Algorithm createAlgorithm(Algorithm algorithm, MultipartFile bibFile, MultipartFile implementationFile)
            throws IOException {
        try {
            // 处理bib文件上传
            if (bibFile != null && !bibFile.isEmpty()) {
                String bibFileName = UUID.randomUUID() + "_" + bibFile.getOriginalFilename();
                File bibFilePath = new File(bibDir, bibFileName);

                System.out.println("Bib file details:");
                System.out.println("Original name: " + bibFile.getOriginalFilename());
                System.out.println("Size: " + bibFile.getSize());
                System.out.println("Content type: " + bibFile.getContentType());
                System.out.println("Target path: " + bibFilePath.getAbsolutePath());

                bibFile.transferTo(bibFilePath);
                algorithm.setBibFilePath("bib/" + bibFileName);
            }

            // 处理实现文件上传（可选）
            if (implementationFile != null && !implementationFile.isEmpty()) {
                String implFileName = UUID.randomUUID() + "_" + implementationFile.getOriginalFilename();
                File implFilePath = new File(algorithmImpDir, implFileName);

                System.out.println("Implementation file details:");
                System.out.println("Original name: " + implementationFile.getOriginalFilename());
                System.out.println("Size: " + implementationFile.getSize());
                System.out.println("Target path: " + implFilePath.getAbsolutePath());

                implementationFile.transferTo(implFilePath);
                algorithm.setImplementationFilePath("algorithmImp/" + implFileName);
            }

            return algorithmRepository.save(algorithm);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create algorithm: " + e.getMessage(), e);
        }
    }

    public List<Algorithm> getAllAlgorithms() {
        return algorithmRepository.findAll();
    }

    public List<Algorithm> getAlgorithmsByCategory(String category) {
        return algorithmRepository.findByCategories(category);
    }

    public List<String> getAllCategories() {
        return algorithmRepository.findAllCategories();
    }
}