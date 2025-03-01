package com.tancilon.aggspringboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.tancilon.aggspringboot.entity.Dataset;

@Entity
@Table(name = "metric_scores")
@Data
public class MetricScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    private Double value;
    private String color;
}