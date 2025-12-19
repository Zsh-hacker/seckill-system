package com.zsh.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsh.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductDao extends BaseMapper<Product> {
}
