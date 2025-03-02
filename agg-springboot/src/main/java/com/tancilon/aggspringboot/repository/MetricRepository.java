package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    boolean existsByName(String name);
}