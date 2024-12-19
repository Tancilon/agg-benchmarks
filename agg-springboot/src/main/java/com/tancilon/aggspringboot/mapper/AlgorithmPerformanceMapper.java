package com.tancilon.aggspringboot.mapper;

import com.tancilon.aggspringboot.pojo.AlgorithmPerformance;

public interface AlgorithmPerformanceMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AlgorithmPerformance row);

    int insertSelective(AlgorithmPerformance row);

    AlgorithmPerformance selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(AlgorithmPerformance row);

    int updateByPrimaryKey(AlgorithmPerformance row);
}