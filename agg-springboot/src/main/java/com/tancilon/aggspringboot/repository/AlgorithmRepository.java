package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.Algorithm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AlgorithmRepository extends JpaRepository<Algorithm, Long> {
    List<Algorithm> findByCategories(String category);

    @Query("SELECT DISTINCT a.categories FROM Algorithm a")
    List<String> findAllCategories();

    boolean existsByName(String name);

    Optional<Algorithm> findByName(String name);
}