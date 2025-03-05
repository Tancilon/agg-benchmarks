package com.tancilon.aggspringboot.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class DatasetCreateDTO {
    @NotBlank(message = "Dataset name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Description is required")
    private String description;
}