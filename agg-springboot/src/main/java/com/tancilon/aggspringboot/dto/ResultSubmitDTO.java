package com.tancilon.aggspringboot.dto;

import java.util.Map;
import lombok.Data;

@Data
public class ResultSubmitDTO {
    private String algorithm;
    private String dataset;
    private Map<String, Object> metrics;
}