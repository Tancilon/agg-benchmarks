package com.tancilon.aggspringboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tancilon.aggspringboot.entity.MetricScoreHistory;

@Repository
public interface MetricScoreHistoryRepository extends JpaRepository<MetricScoreHistory, Long> {
}