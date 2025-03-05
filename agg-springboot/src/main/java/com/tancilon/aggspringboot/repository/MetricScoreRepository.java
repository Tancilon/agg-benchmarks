package com.tancilon.aggspringboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tancilon.aggspringboot.entity.MetricScore;
import java.util.List;

@Repository
public interface MetricScoreRepository extends JpaRepository<MetricScore, Long> {
    @Query("SELECT m FROM MetricScore m WHERE m.algorithm.id = :algorithmId AND m.metricName = :metricName")
    List<MetricScore> findByAlgorithmIdAndMetricName(
            @Param("algorithmId") Long algorithmId,
            @Param("metricName") String metricName);

    @Query("SELECT m FROM MetricScore m WHERE m.dataset.name = :datasetName AND m.metricName = :metricName")
    List<MetricScore> findByDatasetNameAndMetricName(
            @Param("datasetName") String datasetName,
            @Param("metricName") String metricName);

    @Query("SELECT m FROM MetricScore m WHERE m.algorithm.id = :algorithmId AND m.dataset.id = :datasetId")
    List<MetricScore> findByAlgorithmIdAndDatasetId(
            @Param("algorithmId") Long algorithmId,
            @Param("datasetId") Long datasetId);

    @Query("SELECT m FROM MetricScore m WHERE m.algorithm.id = :algorithmId " +
            "AND m.dataset.id = :datasetId AND m.metric.id = :metricId " +
            "AND m.isLatest = true")
    MetricScore findLatestVersion(Long algorithmId, Long datasetId, Long metricId);

    @Query("SELECT m FROM MetricScore m WHERE m.id = :metricScoreId ORDER BY m.version DESC")
    List<MetricScore> findAllVersions(Long metricScoreId);

    List<MetricScore> findByAlgorithmName(String algorithmName);

    List<MetricScore> findByDatasetName(String datasetName);

    List<MetricScore> findByMetricName(String metricName);

    @Query("SELECT m FROM MetricScore m WHERE m.dataset.id = :datasetId AND m.metric.id = :metricId")
    List<MetricScore> findByDatasetIdAndMetricId(
            @Param("datasetId") Long datasetId,
            @Param("metricId") Long metricId);

    @Query("SELECT m FROM MetricScore m WHERE m.algorithm.id = :algorithmId")
    List<MetricScore> findByAlgorithmId(@Param("algorithmId") Long algorithmId);

    @Query("SELECT m FROM MetricScore m WHERE m.dataset.id = :datasetId")
    List<MetricScore> findByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT m FROM MetricScore m WHERE m.metric.id = :metricId")
    List<MetricScore> findByMetricId(@Param("metricId") Long metricId);

    @Query("SELECT m FROM MetricScore m WHERE m.algorithm.id = :algorithmId " +
            "AND m.dataset.id = :datasetId AND m.metric.id = :metricId")
    List<MetricScore> findByAlgorithmIdAndDatasetIdAndMetricId(
            @Param("algorithmId") Long algorithmId,
            @Param("datasetId") Long datasetId,
            @Param("metricId") Long metricId);

    @Query("SELECT m FROM MetricScore m WHERE m.algorithm.id = :algorithmId ORDER BY m.score DESC")
    List<MetricScore> findByAlgorithmIdOrderByScoreDesc(@Param("algorithmId") Long algorithmId);

    @Query("SELECT m FROM MetricScore m WHERE m.dataset.id = :datasetId ORDER BY m.score DESC")
    List<MetricScore> findByDatasetIdOrderByScoreDesc(@Param("datasetId") Long datasetId);
}