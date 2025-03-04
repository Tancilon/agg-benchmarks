package com.tancilon.aggspringboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tancilon.aggspringboot.service.ResultService;
import com.tancilon.aggspringboot.dto.ResultSubmitDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import com.tancilon.aggspringboot.dto.ErrorResponse;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.tancilon.aggspringboot.dto.DownloadResultsRequest;

@RestController
@RequestMapping("/api/results")
public class ResultController {
    private static final Logger logger = LoggerFactory.getLogger(ResultController.class);

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> submitResults(@RequestBody ResultSubmitDTO[] results) {
        try {
            for (ResultSubmitDTO result : results) {
                resultService.saveResults(result);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // 获取数据集的可用指标列表
    @GetMapping("/metrics/{datasetId}")
    public ResponseEntity<?> getAvailableMetrics(@PathVariable String datasetId) {
        try {
            logger.info("Fetching metrics for dataset: {}", datasetId);
            List<String> metrics = resultService.findDistinctMetricsByDataset(datasetId);
            logger.info("Found {} metrics for dataset {}", metrics.size(), datasetId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error fetching metrics for dataset {}: {}", datasetId, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // 获取数据集在特定指标下的性能数据
    @GetMapping("/{datasetId}/{metricName}")
    public ResponseEntity<?> getDatasetMetricPerformance(
            @PathVariable String datasetId,
            @PathVariable String metricName) {
        try {
            logger.info("Fetching performance data for dataset: {}, metric: {}", datasetId, metricName);
            Map<String, Object> performanceData = resultService.getDatasetMetricPerformance(datasetId, metricName);
            logger.info("Successfully retrieved performance data");
            return ResponseEntity.ok(performanceData);
        } catch (Exception e) {
            logger.error("Error fetching performance data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadResults(@RequestBody DownloadResultsRequest request) {
        try {
            // 生成ZIP文件
            Resource resource = resultService.generateResultsZip(request);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"results.zip\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}