package com.tancilon.aggspringboot.controller;

import com.tancilon.aggspringboot.mapper.AlgorithmsMapper;
import com.tancilon.aggspringboot.pojo.Algorithms;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlgorithmsController {
    @Resource
    private AlgorithmsMapper algorithmsMapper;

    // RequestBody 能接收传输过来的json对象
    @PostMapping("algorithms/create")
    public String create(@RequestBody Algorithms algorithm) {
        algorithmsMapper.insertSelective(algorithm);
        return algorithmsMapper.selectByPrimaryKey(1).toString();
    }
}
