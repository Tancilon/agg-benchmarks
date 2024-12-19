package com.tancilon.aggspringboot.mapper;

import com.tancilon.aggspringboot.pojo.Uploads;

public interface UploadsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Uploads row);

    int insertSelective(Uploads row);

    Uploads selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Uploads row);

    int updateByPrimaryKey(Uploads row);
}