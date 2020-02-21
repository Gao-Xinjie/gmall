package com.gaoxinjie.gmall.service;

import com.gaoxinjie.gmall.bean.SkuLsInfo;
import com.gaoxinjie.gmall.bean.SkuLsParams;
import com.gaoxinjie.gmall.bean.SkuLsResult;

public interface ListService {

    public void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    public SkuLsResult getSkuLsInfoList(SkuLsParams skuLsParams);

    public void incrHotScore(String skuId);
}
