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

import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private static final Logger logger = LoggerFactory.getLogger(DatasetController.class);

    @Autowired
    private DatasetService datasetService;

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
}