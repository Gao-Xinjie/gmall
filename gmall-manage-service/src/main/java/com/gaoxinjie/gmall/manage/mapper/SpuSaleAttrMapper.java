package com.gaoxinjie.gmall.manage.mapper;

import com.gaoxinjie.gmall.bean.SpuSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

//spu销售属性
public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    public List<SpuSaleAttr> getSpuSaleAttrListBySpuId(String spuId);

    public List<SpuSaleAttr> getSpuSaleAttrListBySpuIdCheckSku(@Param("skuId") String skuId,@Param("spuId") String spuId);
}
