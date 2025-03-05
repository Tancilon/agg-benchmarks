package com.tancilon.aggspringboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "algorithms")
public class Algorithm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer year;

    @ElementCollection
    @CollectionTable(name = "algorithm_categories", joinColumns = @JoinColumn(name = "algorithm_id"))
    @Column(name = "category")
    private List<String> categories;

    @ElementCollection
    @CollectionTable(name = "algorithm_sources", joinColumns = @JoinColumn(name = "algorithm_id"))
    @Column(name = "source")
    private List<String> sources;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String paperUrl;

    @Column(name = "bib_file_path")
    private String bibFilePath;

    @Column(name = "implementation_file_path")
    private String implementationFilePath;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}