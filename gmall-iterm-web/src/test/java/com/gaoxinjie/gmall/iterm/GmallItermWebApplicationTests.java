package com.gaoxinjie.gmall.iterm;


import com.alibaba.dubbo.config.annotation.Reference;
import com.gaoxinjie.gmall.bean.SkuInfo;
import com.gaoxinjie.gmall.service.ManageService;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
public class GmallItermWebApplicationTests {



    @Reference
    ManageService manageService;

    @Test
    public void getSkuInfo(){
        SkuInfo skuInfo = manageService.getSkuInfo("35");
        System.out.println(skuInfo);
    }

    @Test
    public void getSkuValueIds(){
        Map skuValueIdsMap = manageService.getSkuValueIdsMap("59");

    }
}
