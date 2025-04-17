package com.tancilon.aggspringboot.controller;

import com.tancilon.aggspringboot.entity.Metric;
import com.tancilon.aggspringboot.service.MetricService;
import com.tancilon.aggspringboot.service.FileStorageService;
import com.tancilon.aggspringboot.dto.ValidationResponse;
import com.tancilon.aggspringboot.dto.MetricInfo;
import com.tancilon.aggspringboot.dto.ErrorResponse;
import com.tancilon.aggspringboot.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {
    private static final Logger logger = LoggerFactory.getLogger(MetricController.class);

    @Autowired
    private MetricService metricService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<List<Metric>> getAllMetrics() {
        try {
            List<Metric> metrics = metricService.getAllMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Metric> createMetric(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam("range") String range,
            @RequestPart(value = "implementationFile", required = false) MultipartFile implementationFile) {
        try {
            // 打印接收到的参数
            System.out.println("Received metric creation request:");
            System.out.println("Name: " + name);
            System.out.println("Description: " + description);
            System.out.println("Type: " + type);
            System.out.println("Range: " + range);
            System.out.println(
                    "File: " + (implementationFile != null ? implementationFile.getOriginalFilename() : "null"));

            Metric metric = new Metric();
            metric.setName(name);
            metric.setDescription(description);
            metric.setType(type);
            metric.setRange(range);

            Metric createdMetric = metricService.createMetric(metric, implementationFile);
            return ResponseEntity.ok(createdMetric);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Metric> getMetricById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(metricService.getMetricById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMetric(@PathVariable Long id) {
        try {
            metricService.deleteMetric(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/check-name")
    public ResponseEntity<?> checkMetricName(@RequestParam String name) {
        if (metricService.existsByName(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Metric name already exists");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateMetrics(@RequestBody List<String> metricNames) {
        try {
            List<String> invalidMetrics = metricNames.stream()
                    .filter(name -> !metricService.existsByName(name))
                    .collect(Collectors.toList());

            if (invalidMetrics.isEmpty()) {
                return ResponseEntity.ok(ValidationResponse.success());
            } else {
                return ResponseEntity.ok(ValidationResponse.error(invalidMetrics));
            }
        } catch (Exception e) {
            logger.error("Error validating metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/info")
    public ResponseEntity<Map<String, MetricInfo>> getMetricsInfo(@RequestBody List<String> metricNames) {
        try {
            Map<String, MetricInfo> metricsInfo = metricService.getMetricsInfo(metricNames);
            return ResponseEntity.ok(metricsInfo);
        } catch (Exception e) {
            logger.error("Error getting metrics info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<Metric> getMetricByName(@PathVariable String name) {
        logger.info("接收到获取指标请求，指标名称: {}", name);
        try {
            Metric metric = metricService.getMetricByName(name);
            logger.info("成功获取指标信息: {}", metric);
            return ResponseEntity.ok(metric);
        } catch (ResourceNotFoundException e) {
            logger.warn("指标未找到: {}", name);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("获取指标信息时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/{name}/download")
    public ResponseEntity<Resource> downloadImplementation(@PathVariable String name) {
        try {
            Optional<Metric> metricOpt = metricService.findByName(name);
            logger.info("Attempting to download metric implementation for: {}", name);

            if (metricOpt.isEmpty() || metricOpt.get().getImplementationFile() == null) {
                logger.warn("Metric not found or implementation file is null for: {}", name);
                return ResponseEntity.notFound().build();
            }

            Metric metric = metricOpt.get();
            logger.info("Found metric: {}, implementation file: {}", metric.getName(), metric.getImplementationFile());

            try {
                Resource resource = fileStorageService.loadFileAsResource(metric.getImplementationFile());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } catch (RuntimeException e) {
                logger.error("Error loading implementation file: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        } catch (Exception e) {
            logger.error("Error downloading metric implementation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

}