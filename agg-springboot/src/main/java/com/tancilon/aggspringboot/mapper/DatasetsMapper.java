package com.tancilon.aggspringboot.mapper;

import com.tancilon.aggspringboot.pojo.Datasets;

public interface DatasetsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Datasets row);

    int insertSelective(Datasets row);

    Datasets selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Datasets row);

    int updateByPrimaryKeyWithBLOBs(Datasets row);

    int updateByPrimaryKey(Datasets row);
}