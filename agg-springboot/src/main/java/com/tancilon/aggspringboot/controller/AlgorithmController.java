package com.tancilon.aggspringboot.controller;

import com.tancilon.aggspringboot.entity.Algorithm;
import com.tancilon.aggspringboot.service.AlgorithmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/algorithms")
public class AlgorithmController {

    private final AlgorithmService algorithmService;
    private final ObjectMapper objectMapper;

    public AlgorithmController(AlgorithmService algorithmService, ObjectMapper objectMapper) {
        this.algorithmService = algorithmService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> createAlgorithm(
            @RequestPart("algorithm") String algorithmJson,
            @RequestPart("bibFile") MultipartFile bibFile,
            @RequestPart(value = "implementationFile", required = false) MultipartFile implementationFile) {
        try {
            System.out.println("Received request to create algorithm");
            System.out.println("Algorithm JSON: " + algorithmJson);
            System.out.println("Bib file name: " + bibFile.getOriginalFilename());
            System.out.println("Bib file size: " + bibFile.getSize());
            if (implementationFile != null) {
                System.out.println("Implementation file name: " + implementationFile.getOriginalFilename());
                System.out.println("Implementation file size: " + implementationFile.getSize());
            }

            // 验证文件
            if (bibFile == null || bibFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Bibliography file is required");
            }

            Algorithm algorithm = objectMapper.readValue(algorithmJson, Algorithm.class);
            Algorithm savedAlgorithm = algorithmService.createAlgorithm(algorithm, bibFile, implementationFile);

            System.out.println("Successfully created algorithm: " + savedAlgorithm);
            return ResponseEntity.ok(savedAlgorithm);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "Error creating algorithm: " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }

    @GetMapping
    public ResponseEntity<List<Algorithm>> getAlgorithms(
            @RequestParam(required = false) String category) {
        List<Algorithm> algorithms = category == null ? algorithmService.getAllAlgorithms()
                : algorithmService.getAlgorithmsByCategory(category);
        return ResponseEntity.ok(algorithms);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(algorithmService.getAllCategories());
    }

    @GetMapping("/check-name")
    public ResponseEntity<?> checkAlgorithmName(@RequestParam String name) {
        if (algorithmService.existsByName(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Algorithm name already exists");
        }
        return ResponseEntity.ok().build();
    }
}