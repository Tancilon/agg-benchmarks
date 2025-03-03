package com.tancilon.aggspringboot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tancilon.aggspringboot.entity.Dataset;
import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    Page<Dataset> findByCategoryIgnoreCase(String category, Pageable pageable);

    @Query(value = "SELECT d.category as category, COUNT(d) as count FROM Dataset d GROUP BY d.category")
    List<Object[]> getCategoryStatsRaw();

    /**
     * 查询所有不重复的分类
     */
    @Query("SELECT DISTINCT d.category FROM Dataset d")
    List<String> findDistinctCategories();

    List<Dataset> findByCategory(String category);

    @Query("SELECT DISTINCT d.category FROM Dataset d")
    List<String> findAllCategories();

    boolean existsByName(String name);

    Dataset findByName(String name);
}