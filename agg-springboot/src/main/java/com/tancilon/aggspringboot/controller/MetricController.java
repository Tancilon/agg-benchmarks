package com.tancilon.aggspringboot.controller;

import com.tancilon.aggspringboot.entity.Metric;
import com.tancilon.aggspringboot.service.MetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    @Autowired
    private MetricService metricService;

    @GetMapping
    public ResponseEntity<List<Metric>> getAllMetrics() {
        try {
            List<Metric> metrics = metricService.getAllMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Metric> createMetric(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam("range") String range,
            @RequestPart(value = "implementationFile", required = false) MultipartFile implementationFile) {
        try {
            // 打印接收到的参数
            System.out.println("Received metric creation request:");
            System.out.println("Name: " + name);
            System.out.println("Description: " + description);
            System.out.println("Type: " + type);
            System.out.println("Range: " + range);
            System.out.println(
                    "File: " + (implementationFile != null ? implementationFile.getOriginalFilename() : "null"));

            Metric metric = new Metric();
            metric.setName(name);
            metric.setDescription(description);
            metric.setType(type);
            metric.setRange(range);

            Metric createdMetric = metricService.createMetric(metric, implementationFile);
            return ResponseEntity.ok(createdMetric);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Metric> getMetricById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(metricService.getMetricById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMetric(@PathVariable Long id) {
        try {
            metricService.deleteMetric(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/check-name")
    public ResponseEntity<?> checkMetricName(@RequestParam String name) {
        if (metricService.existsByName(name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Metric name already exists");
        }
        return ResponseEntity.ok().build();
    }
}