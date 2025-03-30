package com.tancilon.aggspringboot.controller;

import com.tancilon.aggspringboot.entity.Algorithm;
import com.tancilon.aggspringboot.service.AlgorithmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import com.tancilon.aggspringboot.dto.ValidationResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import com.tancilon.aggspringboot.dto.ErrorResponse;
import com.tancilon.aggspringboot.service.FileStorageService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import com.tancilon.aggspringboot.service.ResultService;
import com.tancilon.aggspringboot.dto.CategoryStats;
import com.tancilon.aggspringboot.repository.AlgorithmRepository;

@RestController
@RequestMapping("/api/algorithms")
public class AlgorithmController {

    private final AlgorithmService algorithmService;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private static final Logger logger = LoggerFactory.getLogger(AlgorithmController.class);

    @Autowired
    private ResultService resultService;

    @Autowired
    private AlgorithmRepository algorithmRepository;

    public AlgorithmController(AlgorithmService algorithmService, ObjectMapper objectMapper,
            FileStorageService fileStorageService) {
        this.algorithmService = algorithmService;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<?> createAlgorithm(
            @RequestPart("algorithm") String algorithmJson,
            @RequestPart("bibFile") MultipartFile bibFile,
            @RequestPart(value = "implementationFile", required = false) MultipartFile implementationFile) {
        try {
            System.out.println("Received request to create algorithm");
            System.out.println("Algorithm JSON: " + algorithmJson);
            System.out.println("Bib file name: " + bibFile.getOriginalFilename());
            System.out.println("Bib file size: " + bibFile.getSize());
            if (implementationFile != null) {
                System.out.println("Implementation file name: " + implementationFile.getOriginalFilename());
                System.out.println("Implementation file size: " + implementationFile.getSize());
            }

            // 验证文件
            if (bibFile == null || bibFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Bibliography file is required");
            }

            Algorithm algorithm = objectMapper.readValue(algorithmJson, Algorithm.class);
            Algorithm savedAlgorithm = algorithmService.createAlgorithm(algorithm, bibFile, implementationFile);

            System.out.println("Successfully created algorithm: " + savedAlgorithm);
            return ResponseEntity.ok(savedAlgorithm);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "Error creating algorithm: " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    @GetMapping
    public ResponseEntity<List<Algorithm>> getAlgorithms(
            @RequestParam(required = false) String category) {
        List<Algorithm> algorithms = category == null ? algorithmService.getAllAlgorithms()
                : algorithmService.getAlgorithmsByCategory(category);
        return ResponseEntity.ok(algorithms);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(algorithmService.getAllCategories());
    }

    @GetMapping("/check-name")
    public ResponseEntity<?> checkAlgorithmName(@RequestParam String name) {
        if (algorithmService.existsByName(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Algorithm name already exists");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateAlgorithms(@RequestBody List<String> algorithmNames) {
        try {
            List<String> invalidAlgorithms = algorithmNames.stream()
                    .filter(name -> !algorithmService.existsByName(name))
                    .collect(Collectors.toList());

            if (invalidAlgorithms.isEmpty()) {
                return ResponseEntity.ok(ValidationResponse.success());
            } else {
                return ResponseEntity.ok(ValidationResponse.error(invalidAlgorithms));
            }
        } catch (Exception e) {
            logger.error("Error validating algorithms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAlgorithmById(@PathVariable String id) {
        try {
            Algorithm algorithm = algorithmService.getAlgorithmById(id);
            return ResponseEntity.ok(algorithm);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadAlgorithm(@PathVariable String id) {
        try {
            Algorithm algorithm = algorithmService.getAlgorithmById(id);
            if (algorithm.getImplementationFilePath() == null || algorithm.getImplementationFilePath().isEmpty()) {
                throw new RuntimeException("No implementation file available");
            }

            Resource resource = fileStorageService.loadFileAsResource(algorithm.getImplementationFilePath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}/performance/{metricName}")
    public ResponseEntity<?> getAlgorithmPerformance(
            @PathVariable String id,
            @PathVariable String metricName) {
        try {
            logger.info("Fetching performance data for algorithm: {}, metric: {}", id, metricName);
            Map<String, Object> performanceData = algorithmService.getAlgorithmPerformance(id, metricName);
            logger.info("Successfully retrieved performance data");
            return ResponseEntity.ok(performanceData);
        } catch (Exception e) {
            logger.error("Error fetching performance data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<?> getAvailableMetrics(@PathVariable String id) {
        try {
            logger.info("Fetching metrics for algorithm: {}", id);
            List<String> metrics = resultService.findDistinctMetricNamesByAlgorithm(id);
            logger.info("Found {} metrics for algorithm {}", metrics.size(), id);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error fetching metrics for algorithm {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/categories/stats")
    public ResponseEntity<List<CategoryStats>> getCategoryStats() {
        try {
            // 从算法表中获取所有不同的类别及其计数
            List<CategoryStats> stats = algorithmRepository.findAllCategoriesWithCount();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting algorithm categories stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/sources")
    public ResponseEntity<List<String>> getSources() {
        try {
            // 从算法表中获取所有不同的来源
            List<String> sources = algorithmRepository.findDistinctSources();
            return ResponseEntity.ok(sources);
        } catch (Exception e) {
            logger.error("Error getting algorithm sources", e);
            return ResponseEntity.badRequest().build();
        }
    }
}