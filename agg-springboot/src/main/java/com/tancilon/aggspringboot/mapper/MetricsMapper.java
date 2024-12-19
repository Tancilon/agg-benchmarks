package com.tancilon.aggspringboot.mapper;

import com.tancilon.aggspringboot.pojo.Metrics;

public interface MetricsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Metrics row);

    int insertSelective(Metrics row);

    Metrics selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Metrics row);

    int updateByPrimaryKeyWithBLOBs(Metrics row);

    int updateByPrimaryKey(Metrics row);
}