package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    boolean existsByName(String name);

    Optional<Metric> findByName(String name);
}