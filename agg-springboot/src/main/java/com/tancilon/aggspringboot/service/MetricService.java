package com.tancilon.aggspringboot.service;

import java.lang.Object;
import java.lang.String;
import java.lang.Long;
import com.tancilon.aggspringboot.entity.Metric;
import com.tancilon.aggspringboot.repository.MetricRepository;
import com.tancilon.aggspringboot.config.StorageProperties;
import com.tancilon.aggspringboot.exception.ResourceAlreadyExistsException;
import com.tancilon.aggspringboot.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import com.tancilon.aggspringboot.dto.MetricInfo;
import java.util.Optional;

@Service
public class MetricService {

    @Autowired
    private MetricRepository metricRepository;

    private final StorageProperties storageProperties;
    private final String uploadDir;
    private final String metricImpDir;

    public MetricService(MetricRepository metricRepository, StorageProperties storageProperties) {
        this.metricRepository = metricRepository;
        this.storageProperties = storageProperties;

        // 使用绝对路径，与 AlgorithmService 保持一致
        this.uploadDir = new File(storageProperties.getUploadDir()).getAbsolutePath();
        this.metricImpDir = new File(uploadDir, "metricImp").getAbsolutePath();

        // 初始化目录
        ensureDirectoryExists(uploadDir);
        ensureDirectoryExists(metricImpDir);
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

    public List<Metric> getAllMetrics() {
        return metricRepository.findAll();
    }

    public Metric createMetric(Metric metric, MultipartFile implementationFile) throws IOException {
        try {
            if (metricRepository.existsByName(metric.getName())) {
                throw new ResourceAlreadyExistsException("Metric with name " + metric.getName() + " already exists");
            }

            // 处理实现文件上传
            if (implementationFile != null && !implementationFile.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + implementationFile.getOriginalFilename();
                String relativePath = "metricImp/" + fileName;
                File filePath = new File(uploadDir, relativePath);

                // 打印详细的调试信息
                System.out.println("Implementation file details:");
                System.out.println("Original name: " + implementationFile.getOriginalFilename());
                System.out.println("Size: " + implementationFile.getSize());
                System.out.println("Content type: " + implementationFile.getContentType());
                System.out.println("Target path: " + filePath.getAbsolutePath());

                // 确保目标目录存在
                if (!filePath.getParentFile().exists()) {
                    filePath.getParentFile().mkdirs();
                }

                // 保存文件
                implementationFile.transferTo(filePath);

                // 设置相对路径
                metric.setImplementationFile(relativePath); // 存储相对路径

                System.out.println("File saved successfully at: " + filePath.getAbsolutePath());
                System.out.println("Implementation file path set to: " + metric.getImplementationFile());
            }

            return metricRepository.save(metric);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create metric: " + e.getMessage(), e);
        }
    }

    public Metric getMetricById(Long id) {
        return metricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metric not found with id: " + id));
    }

    public void deleteMetric(Long id) {
        Metric metric = getMetricById(id);

        // 删除实现文件
        if (metric.getImplementationFile() != null) {
            try {
                File file = new File(uploadDir, metric.getImplementationFile());
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        metricRepository.deleteById(id);
    }

    public boolean existsByName(String name) {
        return metricRepository.existsByName(name);
    }

    public Map<String, MetricInfo> getMetricsInfo(List<String> metricNames) {
        Map<String, MetricInfo> metricsInfo = new HashMap<>();

        for (String name : metricNames) {
            Optional<Metric> metricOpt = metricRepository.findByName(name);
            if (metricOpt.isPresent()) {
                Metric metric = metricOpt.get();
                MetricInfo info = new MetricInfo();
                info.setName(name);
                info.setType(metric.getType());

                // 解析范围字符串
                String range = metric.getRange().trim();
                if (range.equals("ℝ") || range.equalsIgnoreCase("R")) {
                    // 全体实数
                    info.setMinValue(Double.NEGATIVE_INFINITY);
                    info.setMaxValue(Double.POSITIVE_INFINITY);
                } else if (range.matches("\\[.*,.*\\]")) {
                    // 格式为 [a, b] 的范围
                    String[] bounds = range.substring(1, range.length() - 1)
                            .split(",");
                    info.setMinValue(Double.parseDouble(bounds[0].trim()));
                    info.setMaxValue(Double.parseDouble(bounds[1].trim()));
                } else if (range.contains("-")) {
                    // 格式为 a-b 的范围
                    String[] bounds = range.split("-");
                    info.setMinValue(Double.parseDouble(bounds[0].trim()));
                    info.setMaxValue(Double.parseDouble(bounds[1].trim()));
                } else {
                    // 默认为 [0,1] 范围
                    info.setMinValue(0.0);
                    info.setMaxValue(1.0);
                }

                metricsInfo.put(name, info);
            }
        }

        return metricsInfo;
    }

    public Optional<Metric> findByName(String name) {
        return metricRepository.findByName(name);
    }
}