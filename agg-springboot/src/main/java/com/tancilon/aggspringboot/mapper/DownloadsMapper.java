package com.tancilon.aggspringboot.mapper;

import com.tancilon.aggspringboot.pojo.Downloads;

public interface DownloadsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Downloads row);

    int insertSelective(Downloads row);

    Downloads selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Downloads row);

    int updateByPrimaryKey(Downloads row);
}