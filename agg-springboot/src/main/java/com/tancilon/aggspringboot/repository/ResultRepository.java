package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    // 可以添加自定义查询方法

    @Query("SELECT DISTINCT r.metricName FROM Result r WHERE r.dataset = :datasetId")
    List<String> findDistinctMetricsByDataset(String datasetId);

    List<Result> findByDatasetAndMetricName(String dataset, String metricName);

    List<Result> findByAlgorithmAndMetricName(String algorithm, String metricName);

    @Query("SELECT DISTINCT r.metricName FROM Result r WHERE r.algorithm = :algorithm")
    List<String> findDistinctMetricNamesByAlgorithm(String algorithm);
}