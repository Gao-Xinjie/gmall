package com.gaoxinjie.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;

import com.gaoxinjie.gmall.bean.BaseAttrInfo;
import com.gaoxinjie.gmall.bean.BaseAttrValue;
import com.gaoxinjie.gmall.bean.SkuLsParams;
import com.gaoxinjie.gmall.bean.SkuLsResult;
import com.gaoxinjie.gmall.service.ListService;
import com.gaoxinjie.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import javax.jws.WebParam;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    ListService listService;

    @Reference
    ManageService manageService;

    @GetMapping("list.html")
    public String list(SkuLsParams skuLsParams, Model model){

        SkuLsResult skuLsResult = listService.getSkuLsInfoList(skuLsParams);
        model.addAttribute("skuLsResult",skuLsResult);
        //得到平台属性列表清单
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrListByValueIds(attrValueIdList);
        model.addAttribute("attrList",attrList);
        //设置历史搜索数据
        String paramUrl = getParamUrl(skuLsParams);
        model.addAttribute("paramUrl",paramUrl);
        //面包屑集合
        List<BaseAttrValue> selectedValueList = new ArrayList<>();

        //将已选择的属性值从属性+属性值清单中清除
        //清单：attrList    从清单里删除属性skuLsParams.getValueId()
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0) {
            for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo = iterator.next();
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    //遍历已经选择的属性值id
                    for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                        String selectedValueId = skuLsParams.getValueId()[i];
                        if (baseAttrValue.getId().equals(selectedValueId)) {
                            //如何清单中的属性值和已经选择的属性值相等 那么把整个行全部删掉
                            iterator.remove();
                            //将面包屑中的属性值增加 url =历史url - 属性值的id
                            String selectedParamUrl = getParamUrl(skuLsParams, baseAttrValue.getId());
                            baseAttrValue.setParamUrl(selectedParamUrl);
                            //并且将其加入到集合中渲染成面包屑
                            selectedValueList.add(baseAttrValue);

                        }
                    }
                }
            }
        }

        model.addAttribute("selectedValueList",selectedValueList);
        model.addAttribute("keyWord",skuLsParams.getKeyword());
        //分页
        model.addAttribute("pageNo",skuLsParams.getPageNo());
        model.addAttribute("totalPage",skuLsResult.getTotalPages());
        return "list";
    }

    public String getParamUrl(SkuLsParams skuLsParams,String... exculdeValueId){
        String paramUrl = "";
        if (skuLsParams.getKeyword()!=null){
            paramUrl+="keyWord="+skuLsParams.getKeyword();
        }else if(skuLsParams.getCatalog3Id()!=null){
            paramUrl+="catalog3Id="+skuLsParams.getCatalog3Id();
        }

        if (skuLsParams.getValueId()!=null &&skuLsParams.getValueId().length>0){
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];
                if (exculdeValueId!=null&&exculdeValueId.length>0){
                    if (exculdeValueId[0].equals(valueId)) {
                        continue;
                    }
                }
                if (paramUrl.length()>0){
                    paramUrl+="&";
                }
                paramUrl+="valueId="+valueId;
            }
        }

       return paramUrl;
    }
}
