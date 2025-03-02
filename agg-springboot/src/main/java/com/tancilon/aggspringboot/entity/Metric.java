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

    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String type;
    @Column(name = "`range`")
    private String range;
    private String implementationFile; // 确保这个字段名称正确
}