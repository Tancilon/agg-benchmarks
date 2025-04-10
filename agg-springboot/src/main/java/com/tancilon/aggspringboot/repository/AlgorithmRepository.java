package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.Algorithm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.tancilon.aggspringboot.dto.CategoryStats;

@Repository
public interface AlgorithmRepository extends JpaRepository<Algorithm, Long> {
    List<Algorithm> findByCategories(String category);

    @Query("SELECT DISTINCT a.categories FROM Algorithm a")
    List<String> findAllCategories();

    boolean existsByName(String name);

    Optional<Algorithm> findByName(String name);

    @Query("SELECT new com.tancilon.aggspringboot.dto.CategoryStats(c, COUNT(a)) " +
            "FROM Algorithm a JOIN a.categories c GROUP BY c")
    List<CategoryStats> findAllCategoriesWithCount();

    @Query("SELECT DISTINCT s FROM Algorithm a, IN(a.sources) s")
    List<String> findAllSources();
}