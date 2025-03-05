package com.tancilon.aggspringboot.vo;

import lombok.Data;

@Data
public class CategoryStatsVO {
    private String category;
    private Long count;

    public CategoryStatsVO() {
    }

    public CategoryStatsVO(String category, Long count) {
        this.category = category;
        this.count = count;
    }
}