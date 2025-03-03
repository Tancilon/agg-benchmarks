package com.tancilon.aggspringboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import com.tancilon.aggspringboot.service.MetricScoreService;
import com.tancilon.aggspringboot.dto.MetricScoreUploadRequest;
import com.tancilon.aggspringboot.entity.MetricScore;
import java.util.List;

@RestController
@RequestMapping("/api/metric-scores")
public class MetricScoreController {

    @Autowired
    private MetricScoreService metricScoreService;

    // 上传性能指标
    @PostMapping("/batch")
    public ResponseEntity<?> uploadMetricScores(@RequestBody MetricScoreUploadRequest request) {
        try {
            metricScoreService.saveMetricScores(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 获取算法在不同数据集上的性能
    @GetMapping("/algorithm/{algorithmId}")
    public ResponseEntity<?> getAlgorithmPerformance(
            @PathVariable String algorithmId,
            @RequestParam(required = false) String metricName) {
        Map<String, Object> performance = metricScoreService.getAlgorithmPerformance(algorithmId, metricName);
        return ResponseEntity.ok(performance);
    }

    // 获取数据集上不同算法的性能
    @GetMapping("/dataset/{datasetId}")
    public ResponseEntity<?> getDatasetPerformance(
            @PathVariable String datasetId,
            @RequestParam(required = false) String metricName) {
        Map<String, Object> performance = metricScoreService.getDatasetPerformance(datasetId, metricName);
        return ResponseEntity.ok(performance);
    }

    // 下载性能结果
    @GetMapping("/download")
    public ResponseEntity<?> downloadResults(
            @RequestParam(required = false) String algorithmId,
            @RequestParam(required = false) String datasetId,
            @RequestParam(required = false) String format) {
        byte[] data = metricScoreService.generateResultsFile(algorithmId, datasetId, format);
        String filename = String.format("results_%s.%s", LocalDateTime.now(), format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    @GetMapping("/search")
    public List<MetricScore> searchMetrics(
            @RequestParam(required = false) String algorithmId,
            @RequestParam(required = false) String datasetId,
            @RequestParam(required = false) String metricId) {

        // 如果提供了所有参数，使用完整查询
        if (algorithmId != null && datasetId != null && metricId != null) {
            return metricScoreService.findByAlgorithmIdAndDatasetIdAndMetricId(
                    algorithmId, datasetId, metricId);
        }

        // 两个参数的组合查询
        if (algorithmId != null && datasetId != null) {
            return metricScoreService.findByAlgorithmIdAndDatasetId(algorithmId, datasetId);
        }
        if (datasetId != null && metricId != null) {
            return metricScoreService.findByDatasetIdAndMetricId(datasetId, metricId);
        }

        // 单参数查询，带排序
        if (algorithmId != null) {
            return metricScoreService.findByAlgorithmIdOrderByScoreDesc(algorithmId);
        }
        if (datasetId != null) {
            return metricScoreService.findByDatasetIdOrderByScoreDesc(datasetId);
        }

        // 如果没有参数，返回所有记录
        return metricScoreService.findAll();
    }

    @PostMapping
    public MetricScore createMetricScore(@RequestBody MetricScore metricScore) {
        return metricScoreService.save(metricScore);
    }

    @GetMapping("/{id}")
    public MetricScore getMetricScore(@PathVariable Long id) {
        return metricScoreService.findById(id)
                .orElseThrow(() -> new RuntimeException("MetricScore not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteMetricScore(@PathVariable Long id) {
        metricScoreService.deleteById(id);
    }
}