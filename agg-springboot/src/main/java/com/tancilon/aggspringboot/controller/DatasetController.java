package com.tancilon.aggspringboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tancilon.aggspringboot.service.DatasetService;
import com.tancilon.aggspringboot.vo.DatasetVO;
import com.tancilon.aggspringboot.vo.CategoryStatsVO;
import org.springframework.web.multipart.MultipartFile;
import com.tancilon.aggspringboot.dto.DatasetCreateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tancilon.aggspringboot.dto.ValidationResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.tancilon.aggspringboot.dto.ErrorResponse;
import com.tancilon.aggspringboot.entity.Dataset;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.tancilon.aggspringboot.service.FileStorageService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private static final Logger logger = LoggerFactory.getLogger(DatasetController.class);

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 获取数据集列表(支持分页和分类筛选)
     */
    @GetMapping
    public ResponseEntity<Page<DatasetVO>> getDatasets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DatasetVO> datasets = datasetService.getDatasets(pageable, category);
            return ResponseEntity.ok(datasets);
        } catch (Exception e) {
            logger.error("Error getting datasets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 获取数据集分类统计
     */
    @GetMapping("/categories/stats")
    public ResponseEntity<List<CategoryStatsVO>> getCategoryStats() {
        try {
            return ResponseEntity.ok(datasetService.getCategoryStats());
        } catch (Exception e) {
            logger.error("Error getting category stats", e);
            throw e;
        }
    }

    /**
     * 获取数据集分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        try {
            List<String> categories = datasetService.getAllCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            logger.error("Error getting categories", e);
            // 返回空列表而不是错误状态
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * 上传新数据集
     */
    @PostMapping
    public ResponseEntity<DatasetVO> uploadDataset(
            @RequestPart(value = "dataset", required = true) String datasetJson,
            @RequestPart(value = "file", required = true) MultipartFile file) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            DatasetCreateDTO datasetDTO = mapper.readValue(datasetJson, DatasetCreateDTO.class);
            return ResponseEntity.ok(datasetService.createDataset(datasetDTO, file));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid dataset data", e);
        }
    }

    @GetMapping("/check-name")
    public ResponseEntity<?> checkDatasetName(@RequestParam String name) {
        if (datasetService.existsByName(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Dataset name already exists");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateDatasets(@RequestBody List<String> datasetNames) {
        try {
            List<String> invalidDatasets = datasetNames.stream()
                    .filter(name -> !datasetService.existsByName(name))
                    .collect(Collectors.toList());

            if (invalidDatasets.isEmpty()) {
                return ResponseEntity.ok(ValidationResponse.success());
            } else {
                return ResponseEntity.ok(ValidationResponse.error(invalidDatasets));
            }
        } catch (Exception e) {
            logger.error("Error validating datasets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 获取单个数据集详情
    @GetMapping("/{id}")
    public ResponseEntity<?> getDatasetById(@PathVariable String id) {
        try {
            Dataset dataset = datasetService.getDatasetById(id);
            if (dataset == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(dataset);
        } catch (Exception e) {
            logger.error("Error fetching dataset by id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch dataset: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDataset(@PathVariable String id) {
        try {
            Dataset dataset = datasetService.getDatasetById(id);
            if (dataset == null || dataset.getFileUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            // 获取文件资源
            Resource resource = fileStorageService.loadFileAsResource(dataset.getFileUrl());

            // 从原始文件URL中获取文件扩展名
            String originalExt = dataset.getFileUrl().substring(dataset.getFileUrl().lastIndexOf("."));
            // 构建下载文件名：数据集名称 + 原始扩展名
            String downloadFilename = dataset.getName() + originalExt;

            // 设置响应头，处理文件名中的特殊字符
            String encodedFilename = URLEncoder.encode(downloadFilename, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading dataset: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to download dataset: " + e.getMessage()));
        }
    }
}