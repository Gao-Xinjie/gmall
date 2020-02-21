package com.gaoxinjie.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gaoxinjie.gmall.bean.*;
import com.gaoxinjie.gmall.service.ListService;
import com.gaoxinjie.gmall.service.ManageService;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@CrossOrigin
@RestController
public class ManageController {

    @Reference
    ManageService manageService;

    @Reference
    ListService listService;


    @PostMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        List<BaseCatalog1> catalog1List = manageService.getCatalog1();
        return catalog1List;
    }

    @PostMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        List<BaseCatalog2> baseCatalog2List = manageService.getCatalog2(catalog1Id);
        return baseCatalog2List;
    }

    @PostMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        List<BaseCatalog3> baseCatalog3List = manageService.getCatalog3(catalog2Id);
        return baseCatalog3List;
    }

    //spu业务
    @GetMapping("attrInfoList")
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id){
        List<BaseAttrInfo> attrInfoList = manageService.getAttrList(catalog3Id);
        return attrInfoList;
    }

    @PostMapping("saveAttrInfo")
    public String saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return "success";
    }

    @PostMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo attrInfo = manageService.getAttrInfo(attrId);
        return attrInfo.getAttrValueList();
    }

    @PostMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return "success";
    }

    @PostMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return baseSaleAttrList;
    }

    @GetMapping("spuList")
    public List<SpuInfo> spuList(String catalog3Id){
        List<SpuInfo> spuInfoList = manageService.spuList(catalog3Id);
        return spuInfoList;
    }

    //sku业务

    @GetMapping("spuImageList")
    public List<SpuImage> getSpuImageList(String spuId){

        return manageService.getSpuImageList(spuId);
    }

    @GetMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){
        return manageService.getSpuSaleAttrListBySpuId(spuId);
    }

    @PostMapping("onSale")
    public String saveSku(@RequestParam("spuId") String spuId){
        List<SkuInfo> skuInfoBySpuId = manageService.getSkuInfoBySpuId(spuId);
        for (SkuInfo skuInfo : skuInfoBySpuId) {
            SkuLsInfo skuLsInfo =new SkuLsInfo();
            try {
                BeanUtils.copyProperties(skuLsInfo,skuInfo);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            listService.saveSkuLsInfo(skuLsInfo);
        }

        return "success";
    }
}
