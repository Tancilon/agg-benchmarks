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
}