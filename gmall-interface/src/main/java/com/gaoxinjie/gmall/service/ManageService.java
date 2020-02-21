package com.gaoxinjie.gmall.service;

import com.gaoxinjie.gmall.bean.*;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.util.List;
import java.util.Map;

public interface ManageService {

    //查询一级分类
    public List<BaseCatalog1> getCatalog1();

    //查询二级分类 根据一级分类去查
    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    //查询三级分类 根据二级分类
    public List<BaseCatalog3> getCatalog3(String catalog2Id);


    //根据平台属性id 查询平台属性的详情
    public BaseAttrInfo getAttrInfo(String attrId);
    //保存平台属性
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo);
    //删除平台属性

    //获得常用销售属性
    public List<BaseSaleAttr> getBaseSaleAttrList();

    //保存spu详细详细
    public void saveSpuInfo(SpuInfo spuInfo);

    //查询spu基本信息
    public List<SpuInfo> spuList(String catalog3Id);

    //sku业务

    //查询spu的图片集合
    public List<SpuImage> getSpuImageList(String spuId);

    //查询销售属性
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuId(String SpuId);

    //保存sku 的详细信息
    public void saveSkuInfo(SkuInfo skuInfo);

    //查询skuinfo
    public SkuInfo getSkuInfo(String skuId);

    //根据SpuId 查询销售属性 ，选中传入的skuId涉及的销售属性值
    public List<SpuSaleAttr> getSpuSaleAttrCheckSku(String skuId,String spuId);

    //根据spuId查询已有的sku涉及的销售清单
    public Map getSkuValueIdsMap(String spuId);

    //通过spuId查找Sku
    public List<SkuInfo> getSkuInfoBySpuId(String spuId);

    //根据三级分类查询平台属性
    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    //根据多个属性值id查询平台属性
    public List<BaseAttrInfo> getAttrListByValueIds(List attrValueIdList);

    //

}
