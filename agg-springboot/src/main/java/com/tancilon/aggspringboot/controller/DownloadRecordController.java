package com.tancilon.aggspringboot.controller;

import com.tancilon.aggspringboot.entity.DownloadRecord;
import com.tancilon.aggspringboot.repository.DownloadRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/download-records")
public class DownloadRecordController {

    @Autowired
    private DownloadRecordRepository downloadRecordRepository;

    @PostMapping
    public ResponseEntity<DownloadRecord> createDownloadRecord(@RequestBody DownloadRecord downloadRecord) {
        try {
            DownloadRecord savedRecord = downloadRecordRepository.save(downloadRecord);
            return ResponseEntity.ok(savedRecord);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}