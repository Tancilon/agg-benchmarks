package com.tancilon.aggspringboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import java.util.Map;

@Entity
@Data
@Table(name = "results")
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String algorithm;

    @Column(nullable = false)
    private String dataset;

    @Column(columnDefinition = "TEXT")
    private String metricName;

    private Double value;

    private Integer kValue;

    @CreationTimestamp
    private LocalDateTime createdAt;
}