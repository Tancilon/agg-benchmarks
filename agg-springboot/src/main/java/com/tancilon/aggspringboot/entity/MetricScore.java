package com.tancilon.aggspringboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.tancilon.aggspringboot.entity.Dataset;
import java.time.LocalDateTime;

@Entity
@Table(name = "metric_scores")
@Data
public class MetricScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "algorithm_id")
    private Algorithm algorithm;

    @ManyToOne
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(name = "metric_id")
    private Metric metric;

    @Column(name = "score")
    private Double score;

    @Column(name = "k_value")
    private Integer kValue;

    @Version
    private Long version;

    @Column(name = "commit_hash", length = 40)
    private String commitHash;

    @Column(name = "algorithm_version")
    private String algorithmVersion;

    @Column(name = "environment_info", columnDefinition = "TEXT")
    private String environmentInfo;

    @Column(name = "is_latest")
    private Boolean isLatest = true;

    @Column(name = "deprecated")
    private Boolean deprecated = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "metric_name")
    private String metricName;

    @Column(name = "algorithm_name")
    private String algorithmName;

    @Column(name = "dataset_name")
    private String datasetName;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}