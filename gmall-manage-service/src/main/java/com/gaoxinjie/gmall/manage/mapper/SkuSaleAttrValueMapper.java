package com.gaoxinjie.gmall.manage.mapper;

import com.gaoxinjie.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    public List<Map> getSaleAttrValuesBySpuId(String spuId);
}

