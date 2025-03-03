package com.tancilon.aggspringboot.repository;

import com.tancilon.aggspringboot.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    // 可以添加自定义查询方法
}