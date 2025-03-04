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
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileSystemUtils;
import org.springframework.core.io.ResourceLoader;
import com.tancilon.aggspringboot.dto.DownloadResultsRequest;
import com.tancilon.aggspringboot.entity.Algorithm;
import com.tancilon.aggspringboot.repository.AlgorithmRepository;
import org.springframework.core.io.ResourceLoader;
import com.tancilon.aggspringboot.config.StorageProperties;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ChartUtils;
import java.awt.Color;
import java.awt.BasicStroke;
import java.nio.file.Paths;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileSystemUtils;

@Service
public class ResultService {
    private static final Logger logger = LoggerFactory.getLogger(ResultService.class);

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private AlgorithmRepository algorithmRepository;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private ResourceLoader resourceLoader;

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
        logger.info("Getting performance data for dataset: {}, metric: {}", datasetId, metricName);

        // 获取该数据集在特定指标下的所有结果
        List<Result> results = resultRepository.findByDatasetAndMetricName(datasetId, metricName);
        logger.info("Found {} results", results.size());

        // 构建性能数据响应
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> series = new ArrayList<>();
        Set<Integer> kValues = new TreeSet<>(); // 使用TreeSet自动排序

        // 按算法分组处理数据
        Map<String, List<Result>> algorithmResults = results.stream()
                .collect(Collectors.groupingBy(Result::getAlgorithm));

        logger.info("Processing {} algorithms", algorithmResults.size());

        // 处理每个算法的数据
        algorithmResults.forEach((algorithm, algorithmData) -> {
            Map<String, Object> seriesItem = new HashMap<>();
            seriesItem.put("name", algorithm);

            // 收集该算法的所有数据点
            Map<Integer, Double> kValueMap = algorithmData.stream()
                    .collect(Collectors.toMap(
                            r -> r.getKValue() != null ? r.getKValue() : 0,
                            Result::getValue,
                            (v1, v2) -> v1));

            // 收集所有k值
            kValues.addAll(kValueMap.keySet());

            // 按k值排序添加数据点
            List<Double> sortedData = kValueMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            seriesItem.put("data", sortedData);
            series.add(seriesItem);

            logger.info("Processed algorithm: {}, data points: {}", algorithm, sortedData.size());
        });

        response.put("series", series);
        response.put("xAxis", new ArrayList<>(kValues));

        logger.info("Final response: series count={}, xAxis values={}", series.size(), kValues.size());
        return response;
    }

    public List<String> findDistinctMetricNamesByAlgorithm(String algorithm) {
        return resultRepository.findDistinctMetricNamesByAlgorithm(algorithm);
    }

    public List<String> findDistinctMetricsByDataset(String datasetId) {
        logger.info("Finding distinct metrics for dataset: {}", datasetId);
        List<String> metrics = resultRepository.findDistinctMetricsByDataset(datasetId);
        logger.info("Found metrics: {}", metrics);
        return metrics;
    }

    public Resource generateResultsZip(DownloadResultsRequest request) throws IOException {
        logger.info("Generating results zip with config: {}", request);

        if (request.getSelectedKValues() == null) {
            request.setSelectedKValues(new ArrayList<>());
        }

        Path tempDir = Files.createTempDirectory("results_");
        try {
            // 生成 CSV 文件
            if (request.isIncludeCSV()) {
                generateCSVData(request.getMetric(),
                        request.getSelectedAlgorithms(),
                        request.getSelectedDatasets(),
                        request.getSelectedKValues(),
                        tempDir.resolve("results.csv"));
            }

            // 添加选中算法的 bib 文件
            Path bibDir = tempDir.resolve("references");
            Files.createDirectories(bibDir);
            for (String algorithmName : request.getSelectedAlgorithms()) {
                Algorithm algorithm = algorithmRepository.findByName(algorithmName)
                        .orElseThrow(() -> new RuntimeException("Algorithm not found: " + algorithmName));

                if (algorithm.getBibFilePath() != null && !algorithm.getBibFilePath().isEmpty()) {
                    // 从存储路径读取 BibTeX 文件内容
                    Path sourceBibPath = Paths.get(storageProperties.getUploadDir(), algorithm.getBibFilePath());
                    if (Files.exists(sourceBibPath)) {
                        Path bibFile = bibDir.resolve(algorithmName + ".bib");
                        Files.copy(sourceBibPath, bibFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            // 创建 ZIP 文件
            Path zipFile = Files.createTempFile("results_", ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                Files.walk(tempDir)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(tempDir.relativize(path).toString());
                            try {
                                zos.putNextEntry(zipEntry);
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }

            return new FileSystemResource(zipFile.toFile());
        } catch (Exception e) {
            logger.error("Error generating results zip", e);
            throw e;
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    public void generateCSVData(String metric, List<String> algorithms, List<String> datasets,
            List<Integer> selectedKValues, Path outputPath)
            throws IOException {
        // 获取基础数据
        List<Result> results = resultRepository.findByAlgorithmInAndDatasetInAndMetricName(
                algorithms, datasets, metric);

        // 根据是否是@k指标过滤数据
        boolean isAtKMetric = metric.equals("mAP") || metric.equals("NDCG");
        List<Result> filteredResults = results.stream()
                .filter(result -> {
                    // 如果是@k指标，只返回选中的K值的数据
                    if (isAtKMetric && result.getKValue() != null) {
                        return selectedKValues.contains(result.getKValue());
                    }
                    // 非@k指标返回所有数据
                    return true;
                })
                .collect(Collectors.toList());

        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(outputPath),
                CSVFormat.Builder.create().setHeader("Algorithm", "Dataset", "Metric", "K", "Value").build())) {
            for (Result result : filteredResults) {
                printer.printRecord(
                        result.getAlgorithm(),
                        result.getDataset(),
                        result.getMetricName(),
                        isAtKMetric ? result.getKValue() : "NULL", // 非@k指标时输出"NULL"
                        result.getValue());
            }
        }
    }
}