package com.tancilon.aggspringboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tancilon.aggspringboot.service.ResultService;
import com.tancilon.aggspringboot.dto.ResultSubmitDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import com.tancilon.aggspringboot.dto.ErrorResponse;

@RestController
@RequestMapping("/api/results")
public class ResultController {
    private static final Logger logger = LoggerFactory.getLogger(ResultController.class);

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> submitResults(@RequestBody ResultSubmitDTO[] results) {
        try {
            for (ResultSubmitDTO result : results) {
                resultService.saveResults(result);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}