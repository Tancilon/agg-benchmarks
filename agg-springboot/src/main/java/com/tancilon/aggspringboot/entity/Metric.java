package com.tancilon.aggspringboot.entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "metrics")
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "`range`", nullable = false)
    private String range;

    @Column(nullable = false)
    private String type;

    private String implementationFile; // 确保这个字段名称正确
}