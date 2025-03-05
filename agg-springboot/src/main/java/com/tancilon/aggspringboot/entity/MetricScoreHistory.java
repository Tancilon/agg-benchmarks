package com.tancilon.aggspringboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.tancilon.aggspringboot.enums.ChangeType;
import com.tancilon.aggspringboot.entity.MetricScore;

@Entity
@Table(name = "metric_score_history")
@Data
public class MetricScoreHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "metric_score_id")
    private MetricScore metricScore;

    private Double oldValue;
    private Double newValue;

    @Column(name = "change_type")
    @Enumerated(EnumType.STRING)
    private ChangeType changeType; // CREATE, UPDATE, DEPRECATE

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;
}