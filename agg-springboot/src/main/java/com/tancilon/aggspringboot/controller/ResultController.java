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
            List<String> metrics = resultService.getAvailableMetrics(datasetId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // 获取数据集在特定指标下的性能数据
    @GetMapping("/{datasetId}/{metricName}")
    public ResponseEntity<?> getDatasetMetricPerformance(
            @PathVariable String datasetId,
            @PathVariable String metricName) {
        try {
            Map<String, Object> performanceData = resultService.getDatasetMetricPerformance(datasetId, metricName);
            return ResponseEntity.ok(performanceData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}