package com.tancilon.aggspringboot.dto;

import lombok.Data;
import java.util.List;

@Data
public class DownloadResultsRequest {
    private String metric;
    private boolean includePDF;
    private boolean includeCSV;
    private List<String> selectedAlgorithms;
    private List<String> selectedDatasets;
}