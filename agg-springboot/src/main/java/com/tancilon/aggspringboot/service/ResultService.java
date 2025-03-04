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
            List<Double> data = new ArrayList<>();
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
        Path tempDir = Files.createTempDirectory("results_");

        try {
            // 1. 如果需要,生成PDF文件
            if (request.isIncludePDF()) {
                generatePDFChart(request.getMetric(),
                        request.getSelectedAlgorithms(),
                        request.getSelectedDatasets(),
                        tempDir.resolve("chart.pdf"));
            }

            // 2. 如果需要,生成CSV文件
            if (request.isIncludeCSV()) {
                generateCSVData(request.getMetric(),
                        request.getSelectedAlgorithms(),
                        request.getSelectedDatasets(),
                        tempDir.resolve("results.csv"));
            }

            // 3. 获取并复制所选算法的BIB文件
            for (String algorithmName : request.getSelectedAlgorithms()) {
                Optional<Algorithm> algorithm = algorithmRepository.findByName(algorithmName);
                if (algorithm.isPresent() && algorithm.get().getBibFilePath() != null) {
                    Path bibSource = Paths.get(storageProperties.getUploadDir())
                            .toAbsolutePath().normalize()
                            .resolve(algorithm.get().getBibFilePath());
                    Path bibTarget = tempDir.resolve(algorithmName + ".bib");
                    Files.copy(bibSource, bibTarget, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 4. 创建ZIP文件
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
                                throw new RuntimeException("Failed to add file to ZIP: " + path, e);
                            }
                        });
            }

            return new UrlResource(zipFile.toUri());
        } finally {
            // 清理临时文件
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    public void generateCSVData(String metric, List<String> algorithms, List<String> datasets, Path outputPath)
            throws IOException {
        List<Result> results = resultRepository.findByAlgorithmInAndDatasetInAndMetricName(
                algorithms, datasets, metric);

        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(outputPath),
                CSVFormat.Builder.create().setHeader("Algorithm", "Dataset", "Metric", "K", "Value").build())) {
            for (Result result : results) {
                printer.printRecord(
                        result.getAlgorithm(),
                        result.getDataset(),
                        result.getMetricName(),
                        result.getKValue(),
                        result.getValue());
            }
        }
    }

    public void generatePDFChart(String metric, List<String> algorithms, List<String> datasets, Path outputPath)
            throws IOException {
        // 1. 获取数据并准备图表数据集
        XYSeriesCollection dataset = new XYSeriesCollection();

        // 获取所有结果数据
        List<Result> results = resultRepository.findByAlgorithmInAndDatasetInAndMetricName(
                algorithms, datasets, metric);

        // 按算法和数据集分组
        Map<String, Map<String, List<Result>>> groupedResults = results.stream()
                .collect(Collectors.groupingBy(Result::getAlgorithm,
                        Collectors.groupingBy(Result::getDataset)));

        // 为每个算法-数据集组合创建一个数据系列
        for (String algorithm : algorithms) {
            for (String datasetName : datasets) {
                XYSeries series = new XYSeries(algorithm + " - " + datasetName);

                List<Result> seriesResults = groupedResults
                        .getOrDefault(algorithm, Collections.emptyMap())
                        .getOrDefault(datasetName, Collections.emptyList());

                // 添加数据点
                seriesResults.stream()
                        .sorted(Comparator.comparing(r -> r.getKValue() != null ? r.getKValue() : 0))
                        .forEach(r -> series.add(
                                r.getKValue() != null ? r.getKValue() : 0,
                                r.getValue()));

                dataset.addSeries(series);
            }
        }

        // 2. 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Performance Comparison - " + metric, // 标题
                metric.contains("@") ? "@k" : "Dataset", // X轴标签
                metric, // Y轴标签
                dataset,
                PlotOrientation.VERTICAL,
                true, // 显示图例
                true, // 显示工具提示
                false // 不显示URL
        );

        // 3. 自定义图表样式
        chart.setBackgroundPaint(Color.WHITE);
        var plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.getRenderer().setDefaultStroke(new BasicStroke(2.0f));

        // 4. 创建临时图片文件
        Path tempImagePath = Files.createTempFile("chart_", ".png");
        ChartUtils.saveChartAsPNG(tempImagePath.toFile(), chart, 800, 600);

        // 5. 创建PDF文档
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // 计算图片在PDF中的位置和大小
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float imageWidth = pageWidth - 2 * margin;
            float imageHeight = imageWidth * 0.75f; // 保持4:3比例
            float yPosition = pageHeight - margin - imageHeight;

            // 将图片添加到PDF
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDImageXObject image = PDImageXObject.createFromFile(tempImagePath.toString(), document);
                contentStream.drawImage(image, margin, yPosition, imageWidth, imageHeight);
            }

            // 保存PDF
            document.save(outputPath.toFile());
        } finally {
            // 删除临时图片文件
            Files.deleteIfExists(tempImagePath);
        }
    }
}