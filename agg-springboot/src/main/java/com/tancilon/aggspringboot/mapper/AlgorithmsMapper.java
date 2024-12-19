package com.tancilon.aggspringboot.mapper;

import com.tancilon.aggspringboot.pojo.Algorithms;

public interface AlgorithmsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Algorithms row);

    int insertSelective(Algorithms row);

    Algorithms selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Algorithms row);

    int updateByPrimaryKeyWithBLOBs(Algorithms row);

    int updateByPrimaryKey(Algorithms row);
}