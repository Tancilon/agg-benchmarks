package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.DownloadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DownloadRecordRepository extends JpaRepository<DownloadRecord, Long> {
}