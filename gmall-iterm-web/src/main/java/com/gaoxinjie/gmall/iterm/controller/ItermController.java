package com.gaoxinjie.gmall.iterm.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.bean.SkuInfo;
import com.gaoxinjie.gmall.bean.SpuSaleAttr;
import com.gaoxinjie.gmall.config.LoginRequire;
import com.gaoxinjie.gmall.service.ListService;
import com.gaoxinjie.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


@Controller
public class ItermController {

    @Reference
    ManageService manageService;

//    @Reference
//    ListService listService;


    @GetMapping("{skuId}.html")
    public String iterm(@PathVariable("skuId")String skuId, HttpServletRequest request){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrCheckSku(skuId, skuInfo.getSpuId());
        Map skuValueIdsMap = manageService.getSkuValueIdsMap(skuInfo.getSpuId());
        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);


//        String jsonString = JSON.toJSONString(skuInfo);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        request.setAttribute("valuesSkuJson",valuesSkuJson);
//        listService.incrHotScore(skuId);
        return "item";
    }

}
