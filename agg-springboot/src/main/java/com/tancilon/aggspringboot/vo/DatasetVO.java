package com.tancilon.aggspringboot.vo;

import lombok.Data;
import java.util.List;

@Data
public class DatasetVO {
    private Long id;
    private String name;
    private String category;
    private String description;
    private List<MetricScoreVO> metrics;
}