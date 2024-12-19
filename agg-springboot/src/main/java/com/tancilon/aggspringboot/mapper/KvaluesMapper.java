package com.tancilon.aggspringboot.mapper;

import com.tancilon.aggspringboot.pojo.Kvalues;

public interface KvaluesMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Kvalues row);

    int insertSelective(Kvalues row);

    Kvalues selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Kvalues row);

    int updateByPrimaryKey(Kvalues row);
}