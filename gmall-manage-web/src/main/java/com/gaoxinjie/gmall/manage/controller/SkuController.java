package com.gaoxinjie.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gaoxinjie.gmall.bean.SkuInfo;
import com.gaoxinjie.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class SkuController {

    @Reference
    ManageService manageService;


    @PostMapping
    public String saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return "success";
    }
}
