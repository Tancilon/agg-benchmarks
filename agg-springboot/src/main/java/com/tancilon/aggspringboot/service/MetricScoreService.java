package com.tancilon.aggspringboot.service;

import com.tancilon.aggspringboot.dto.MetricScoreUploadRequest;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.tancilon.aggspringboot.repository.MetricScoreRepository;
import com.tancilon.aggspringboot.entity.MetricScore;
import java.util.List;
import java.util.Optional;

@Service
public class MetricScoreService {

    @Autowired
    private MetricScoreRepository metricScoreRepository;

    public List<MetricScore> findByAlgorithmIdAndDatasetId(String algorithmId, String datasetId) {
        return metricScoreRepository.findByAlgorithmIdAndDatasetId(
                Long.parseLong(algorithmId),
                Long.parseLong(datasetId));
    }

    public List<MetricScore> findByDatasetIdAndMetricId(String datasetId, String metricId) {
        return metricScoreRepository.findByDatasetIdAndMetricId(
                Long.parseLong(datasetId),
                Long.parseLong(metricId));
    }

    public List<MetricScore> findByAlgorithmIdAndDatasetIdAndMetricId(
            String algorithmId, String datasetId, String metricId) {
        return metricScoreRepository.findByAlgorithmIdAndDatasetIdAndMetricId(
                Long.parseLong(algorithmId), Long.parseLong(datasetId), Long.parseLong(metricId));
    }

    public MetricScore save(MetricScore metricScore) {
        return metricScoreRepository.save(metricScore);
    }

    public List<MetricScore> findAll() {
        return metricScoreRepository.findAll();
    }

    public Optional<MetricScore> findById(Long id) {
        return metricScoreRepository.findById(id);
    }

    public void deleteById(Long id) {
        metricScoreRepository.deleteById(id);
    }

    public void saveMetricScores(MetricScoreUploadRequest request) {
        // Implementation needed
    }

    public Map<String, Object> getAlgorithmPerformance(String algorithmId, String metricName) {
        // Implementation needed
        return null;
    }

    public Map<String, Object> getDatasetPerformance(String datasetId, String metricName) {
        // Implementation needed
        return null;
    }

    public byte[] generateResultsFile(String algorithmId, String datasetId, String format) {
        // Implementation needed
        return null;
    }

    public List<MetricScore> findByAlgorithmIdOrderByScoreDesc(String algorithmId) {
        return metricScoreRepository.findByAlgorithmIdOrderByScoreDesc(Long.parseLong(algorithmId));
    }

    public List<MetricScore> findByDatasetIdOrderByScoreDesc(String datasetId) {
        return metricScoreRepository.findByDatasetIdOrderByScoreDesc(Long.parseLong(datasetId));
    }
}