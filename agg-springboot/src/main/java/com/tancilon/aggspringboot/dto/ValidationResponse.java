package com.tancilon.aggspringboot.dto;

import lombok.Data;
import java.util.List;

@Data
public class ValidationResponse {
    private boolean valid;
    private List<String> invalidItems;

    public static ValidationResponse success() {
        ValidationResponse response = new ValidationResponse();
        response.setValid(true);
        return response;
    }

    public static ValidationResponse error(List<String> invalidItems) {
        ValidationResponse response = new ValidationResponse();
        response.setValid(false);
        response.setInvalidItems(invalidItems);
        return response;
    }
}