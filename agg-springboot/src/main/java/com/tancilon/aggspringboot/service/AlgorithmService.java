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
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import com.tancilon.aggspringboot.entity.Result;
import com.tancilon.aggspringboot.repository.ResultRepository;

@Service
public class AlgorithmService {

    private final AlgorithmRepository algorithmRepository;
    private final StorageProperties storageProperties;
    private final String uploadDir;
    private final String bibDir;
    private final String algorithmImpDir;

    @Autowired
    private ResultRepository resultRepository;

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
            // 检查算法名称是否已存在
            if (existsByName(algorithm.getName())) {
                throw new RuntimeException("Algorithm with name '" + algorithm.getName() + "' already exists");
            }

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

    public boolean existsByName(String name) {
        return algorithmRepository.existsByName(name);
    }

    public Algorithm getAlgorithmById(String id) {
        return algorithmRepository.findByName(id)
                .orElseThrow(() -> new RuntimeException("Algorithm not found with id: " + id));
    }

    public Map<String, Object> getAlgorithmPerformance(String algorithmId, String metricName) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> series = new ArrayList<>();
        Set<Integer> kValues = new TreeSet<>(); // 使用TreeSet自动排序

        // 从数据库获取该算法在指定指标下的所有结果
        List<Result> results = resultRepository.findByAlgorithmAndMetricName(algorithmId, metricName);

        // 按数据集分组处理数据
        Map<String, List<Result>> datasetResults = results.stream()
                .collect(Collectors.groupingBy(Result::getDataset));

        // 处理每个数据集的数据
        datasetResults.forEach((dataset, datasetData) -> {
            Map<String, Object> seriesItem = new HashMap<>();
            seriesItem.put("name", dataset);

            // 收集该数据集的所有数据点
            Map<Integer, Double> kValueMap = datasetData.stream()
                    .collect(Collectors.toMap(
                            Result::getKValue,
                            Result::getValue,
                            (v1, v2) -> v1 // 如果有重复的k值，保留第一个
            ));

            // 收集所有k值
            kValues.addAll(kValueMap.keySet());

            // 按k值排序添加数据点
            List<Double> sortedData = kValueMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            seriesItem.put("data", sortedData);
            series.add(seriesItem);
        });

        response.put("series", series);
        response.put("xAxis", new ArrayList<>(kValues));

        return response;
    }
}