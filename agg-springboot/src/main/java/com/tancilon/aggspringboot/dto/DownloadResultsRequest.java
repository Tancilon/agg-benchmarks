package com.tancilon.aggspringboot.dto;

import lombok.Data;
import java.util.List;

@Data
public class DownloadResultsRequest {
    private String metric;
    private List<String> selectedAlgorithms;
    private List<String> selectedDatasets;
    private boolean includeCSV = true; // 默认为 true
    private List<Integer> selectedKValues;
}