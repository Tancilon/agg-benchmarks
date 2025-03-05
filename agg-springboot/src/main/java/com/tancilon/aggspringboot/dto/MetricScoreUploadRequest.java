package com.tancilon.aggspringboot.dto;

import lombok.Data;
import java.util.Map;

@Data
public class MetricScoreUploadRequest {
    private String algorithm;
    private String dataset;
    private Map<String, Object> metrics; // 支持固定值和@k类型的指标
}