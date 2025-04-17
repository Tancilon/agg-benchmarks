package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    Optional<Metric> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT m FROM Metric m WHERE m.name = :name")
    Optional<Metric> findByNameWithDebug(@Param("name") String name);
}