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
}