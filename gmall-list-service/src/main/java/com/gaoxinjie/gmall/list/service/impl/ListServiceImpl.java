package com.gaoxinjie.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.gaoxinjie.gmall.bean.SkuLsInfo;
import com.gaoxinjie.gmall.bean.SkuLsParams;
import com.gaoxinjie.gmall.bean.SkuLsResult;
import com.gaoxinjie.gmall.service.ListService;
import com.gaoxinjie.gmall.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;

import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import javafx.scene.input.InputMethodTextRun;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;
    @Autowired
    RedisUtil redisUtil;
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo){

        Index.Builder builder = new Index.Builder(skuLsInfo);
        builder.index("gmall_sku_info").type("doc").id(skuLsInfo.getId());
        Index index = builder.build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult getSkuLsInfoList(SkuLsParams skuLsParams) {

        SkuLsResult skuLsResult = new SkuLsResult();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder =QueryBuilders.boolQuery();

        //商品的全文搜索
        if (skuLsParams.getKeyword()!=null) {
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            //高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuName").preTags("<span style='color:red'>").postTags("</span>");
            searchSourceBuilder.highlight(highlightBuilder);
        }
        //三级分类过滤
        if(skuLsParams.getCatalog3Id() !=null) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            //平台属性过滤
            boolQueryBuilder.filter(termQueryBuilder);
        }


        if(skuLsParams.getValueId()!=null  && skuLsParams.getValueId().length>0) {
            String[] valueIds = skuLsParams.getValueId();

            for (int i = 0; i < valueIds.length; i++) {
                String valueId = valueIds[i];
                TermQueryBuilder valueQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(valueQueryBuilder);
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);
        //创建分页
        searchSourceBuilder.from((skuLsParams.getPageNo()-1)*skuLsParams.getPageSize());
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //创建聚合函数
        TermsBuilder groupbyValueId = AggregationBuilders.terms("groupby_value_id").field("skuAttrValueList.valueId").size(1000);
        searchSourceBuilder.aggregation(groupbyValueId);
        //设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);



        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        Search search = builder.addIndex("gmall_sku_info").addType("doc").build();


        try {
            SearchResult searchResult = jestClient.execute(search);
            List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
            List<String> attrValueIdList = new ArrayList<>();
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                String skuName = hit.highlight.get("skuName").get(0);
                skuLsInfo.setSkuName(skuName);
                skuLsInfoList.add(skuLsInfo);
            }
            //总数
            Long total = searchResult.getTotal();
            skuLsResult.setTotal(total);
            //总页数 计算方法=（总数+每页数-1）/每页数
            Long totalPage =(total+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize() ;
            skuLsResult.setTotalPages(totalPage);
            //商品详情列表
            skuLsResult.setSkuLsInfoList(skuLsInfoList);

            //聚合部分  商品设计的平台属性
            List<TermsAggregation.Entry> buckets = searchResult.getAggregations().getTermsAggregation("groupby_value_id").getBuckets();
            for (TermsAggregation.Entry bucket : buckets) {
                String key = bucket.getKey();
                attrValueIdList.add(key);
            }
            skuLsResult.setAttrValueIdList(attrValueIdList);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        // key =sku:101:hotscore type:string
        String hotScoreKey = "sku:"+skuId+":hostscore";
        Long incr = jedis.incr(hotScoreKey);
        if(incr%10==0){
            //更新es
            updateHostScoreEs(skuId,incr);
        }
        jedis.close();

    }

    public void updateHostScoreEs(String skuId,Long hotScore){
        String updateString ="{\n" +
                "  \"doc\": {\n" +
                "  \"hotScore\":"+hotScore+"\n" +
                "  }  \n" +
                "}";
        Update.Builder builder = new Update.Builder(updateString);
        Update update = builder.index("gmall_sku_info").type("doc").id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
