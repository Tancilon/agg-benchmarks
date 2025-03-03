package com.tancilon.aggspringboot.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.tancilon.aggspringboot.dto.ResultSubmitDTO;
import com.tancilon.aggspringboot.entity.Result;
import com.tancilon.aggspringboot.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class ResultService {
    private static final Logger logger = LoggerFactory.getLogger(ResultService.class);

    @Autowired
    private ResultRepository resultRepository;

    @Transactional
    public void saveResults(ResultSubmitDTO submitData) {
        List<Result> results = new ArrayList<>();

        // 判断是否为批量数据
        if (submitData instanceof List) {
            // 处理批量数据
            @SuppressWarnings("unchecked")
            List<ResultSubmitDTO> batchData = (List<ResultSubmitDTO>) submitData;

            for (ResultSubmitDTO data : batchData) {
                processResult(data, results);
            }

            logger.info("Processing batch upload with {} entries", batchData.size());
        } else {
            // 处理单个数据
            processResult(submitData, results);
            logger.info("Processing single upload for algorithm: {}, dataset: {}",
                    submitData.getAlgorithm(), submitData.getDataset());
        }

        // 批量保存结果
        if (!results.isEmpty()) {
            resultRepository.saveAll(results);
            logger.info("Successfully saved {} results", results.size());
        } else {
            logger.warn("No results to save");
        }
    }

    private void processResult(ResultSubmitDTO data, List<Result> results) {
        // 遍历所有指标
        data.getMetrics().forEach((metricName, metricValue) -> {
            if (metricValue instanceof Map) {
                // 处理 @k 类型的指标
                @SuppressWarnings("unchecked")
                Map<String, Double> kValues = (Map<String, Double>) metricValue;
                kValues.forEach((k, value) -> {
                    Result result = new Result();
                    result.setAlgorithm(data.getAlgorithm());
                    result.setDataset(data.getDataset());
                    result.setMetricName(metricName);
                    result.setKValue(Integer.parseInt(k));
                    result.setValue(value);
                    results.add(result);

                    logger.debug("Created @k result: algorithm={}, dataset={}, metric={}, k={}, value={}",
                            data.getAlgorithm(), data.getDataset(), metricName, k, value);
                });
            } else {
                // 处理固定类型的指标
                Result result = new Result();
                result.setAlgorithm(data.getAlgorithm());
                result.setDataset(data.getDataset());
                result.setMetricName(metricName);
                result.setValue(((Number) metricValue).doubleValue());
                results.add(result);

                logger.debug("Created fixed result: algorithm={}, dataset={}, metric={}, value={}",
                        data.getAlgorithm(), data.getDataset(), metricName, metricValue);
            }
        });
    }

    // 获取数据集的可用指标列表
    public List<String> getAvailableMetrics(String datasetId) {
        // 从结果表中查询该数据集的所有不同指标
        return resultRepository.findDistinctMetricsByDataset(datasetId);
    }

    // 获取数据集在特定指标下的性能数据
    public Map<String, Object> getDatasetMetricPerformance(String datasetId, String metricName) {
        // 获取该数据集在特定指标下的所有结果
        List<Result> results = resultRepository.findByDatasetAndMetricName(datasetId, metricName);

        // 构建性能数据响应
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> series = new ArrayList<>();
        Set<Integer> kValues = new TreeSet<>(); // 使用TreeSet自动排序

        // 按算法分组处理数据
        Map<String, List<Result>> algorithmResults = results.stream()
                .collect(Collectors.groupingBy(Result::getAlgorithm));

        // 处理每个算法的数据
        algorithmResults.forEach((algorithm, algorithmData) -> {
            Map<String, Object> seriesItem = new HashMap<>();
            seriesItem.put("name", algorithm);

            // 收集该算法的所有数据点
            List<Double> data = new ArrayList<>();
            Map<Integer, Double> kValueMap = algorithmData.stream()
                    .collect(Collectors.toMap(
                            r -> r.getKValue() != null ? r.getKValue() : 0,
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

        // 构建x轴数据（k值）
        List<Integer> xAxis = new ArrayList<>(kValues);

        response.put("series", series);
        response.put("xAxis", xAxis);

        return response;
    }
}