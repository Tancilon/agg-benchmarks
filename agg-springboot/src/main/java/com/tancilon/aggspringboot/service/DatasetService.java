package com.tancilon.aggspringboot.service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.io.IOException;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.tancilon.aggspringboot.entity.Dataset;
import com.tancilon.aggspringboot.entity.MetricScore;
import com.tancilon.aggspringboot.repository.DatasetRepository;
import com.tancilon.aggspringboot.vo.CategoryStatsVO;
import com.tancilon.aggspringboot.vo.DatasetVO;
import com.tancilon.aggspringboot.vo.MetricScoreVO;
import com.tancilon.aggspringboot.dto.DatasetCreateDTO;
import com.tancilon.aggspringboot.service.FileStorageService;

@Service
@Transactional(readOnly = true)
public class DatasetService {

    private static final Logger logger = LoggerFactory.getLogger(DatasetService.class);

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Page<DatasetVO> getDatasets(Pageable pageable, String category) {
        logger.info("Fetching datasets: page={}, size={}, category={}", pageable.getPageNumber(),
                pageable.getPageSize(), category);
        try {
            Page<Dataset> datasets;
            if (category != null && !category.equals("all")) {
                datasets = datasetRepository.findByCategoryIgnoreCase(category, pageable);
            } else {
                datasets = datasetRepository.findAll(pageable);
            }
            logger.info("Found {} datasets", datasets.getTotalElements());
            return datasets.map(this::convertToVO);
        } catch (Exception e) {
            logger.error("Error fetching datasets", e);
            throw e;
        }
    }

    public List<CategoryStatsVO> getCategoryStats() {
        List<Object[]> rawStats = datasetRepository.getCategoryStatsRaw();
        return rawStats.stream()
                .map(row -> new CategoryStatsVO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    @Transactional
    public DatasetVO createDataset(DatasetCreateDTO dto, MultipartFile file) {
        try {
            // 保存文件
            String fileUrl = fileStorageService.storeFile(file);

            // 创建数据集实体
            Dataset dataset = new Dataset();
            dataset.setName(dto.getName());
            dataset.setCategory(dto.getCategory());
            dataset.setDescription(dto.getDescription());
            dataset.setFileUrl(fileUrl);
            // createdAt 字段会由 @CreationTimestamp 自动填充

            // 保存到数据库
            dataset = datasetRepository.save(dataset);
            logger.info("Dataset created with id: {}", dataset.getId());

            return convertToVO(dataset);
        } catch (Exception e) {
            logger.error("Error creating dataset", e);
            throw new RuntimeException("Failed to create dataset", e);
        }
    }

    private DatasetVO convertToVO(Dataset dataset) {
        DatasetVO vo = new DatasetVO();
        BeanUtils.copyProperties(dataset, vo);

        // 转换性能指标
        if (dataset.getMetrics() != null) {
            vo.setMetrics(dataset.getMetrics().stream()
                    .map(this::convertToMetricVO)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    private MetricScoreVO convertToMetricVO(MetricScore score) {
        MetricScoreVO vo = new MetricScoreVO();
        BeanUtils.copyProperties(score, vo);
        return vo;
    }

    /**
     * 获取所有数据集分类
     * 
     * @return 分类列表
     */
    public List<String> getAllCategories() {
        try {
            // 直接使用 Repository 的查询方法
            List<String> categories = datasetRepository.findDistinctCategories();
            logger.info("Found {} distinct categories", categories.size());
            return categories;
        } catch (Exception e) {
            logger.error("Error fetching categories", e);
            // 返回空列表而不是抛出异常，这样前端至少能显示 'Other' 选项
            return new ArrayList<>();
        }
    }

    public Dataset createDataset(Dataset dataset, MultipartFile file) throws IOException {
        try {
            // 检查数据集名称是否已存在
            if (datasetRepository.existsByName(dataset.getName())) {
                throw new RuntimeException("Dataset with name '" + dataset.getName() + "' already exists");
            }

            // 处理文件上传
            if (file != null && !file.isEmpty()) {
                String fileUrl = fileStorageService.storeFile(file);
                dataset.setFileUrl(fileUrl);
            }

            return datasetRepository.save(dataset);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create dataset: " + e.getMessage(), e);
        }
    }

    public boolean existsByName(String name) {
        return datasetRepository.existsByName(name);
    }

    // 获取单个数据集详情
    public Dataset getDatasetById(String id) {
        try {
            // 尝试通过名称查找数据集（因为前端传来的是数据集名称）
            Dataset dataset = datasetRepository.findByName(id);
            if (dataset == null) {
                // 如果通过名称找不到，尝试通过ID查找
                try {
                    Long datasetId = Long.parseLong(id);
                    dataset = datasetRepository.findById(datasetId).orElse(null);
                } catch (NumberFormatException e) {
                    // ID 不是数字格式，忽略这个异常
                }
            }

            if (dataset == null) {
                logger.warn("Dataset not found with id/name: {}", id);
                return null;
            }

            logger.info("Found dataset: {}", dataset.getName());
            return dataset;
        } catch (Exception e) {
            logger.error("Error fetching dataset by id: " + id, e);
            throw new RuntimeException("Failed to fetch dataset: " + e.getMessage(), e);
        }
    }
}