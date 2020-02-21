package com.gaoxinjie.gmall.manage.service.impl.serviceImpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.bean.*;
import com.gaoxinjie.gmall.manage.mapper.*;
import com.gaoxinjie.gmall.service.ManageService;
import com.gaoxinjie.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;


import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    SpuInfoMapper spuInfoMapper;
    @Autowired
    SpuImageMapper spuImageMapper;

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;


    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedissonClient redissonClient;

    public static final String SKUKET_PREFIX="sku:";
    public static final String SKUKET_INFO_SUFFIX=":info";
    public static final int SKU_EXPIRE_TIME_SEC=10;
    public static final String SKUKET_INFO_LOCK="lock";
    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        List<BaseCatalog2> catalog2List = baseCatalog2Mapper.select(baseCatalog2);

        return catalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        List<BaseCatalog3> catalog3List = baseCatalog3Mapper.select(baseCatalog3);

        return catalog3List;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {

//        final Example example = new Example(BaseAttrInfo.class);
//        example.createCriteria().andEqualTo("catalog3Id",catalog3Id);
//        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectByExample(example);
//
//        //查询平台属性值
//        for (BaseAttrInfo baseAttrInfo : baseAttrInfoList) {
//            BaseAttrValue baseAttrValue = new BaseAttrValue();
//            baseAttrValue.setAttrId(baseAttrInfo.getId());
//            List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
//            baseAttrInfo.setAttrValueList(baseAttrValueList);
//        }
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
        return baseAttrInfoList;
    }

    @Override
    public List<BaseAttrInfo> getAttrListByValueIds(List attrValueIdList) {
        //将list 编程 =》13，14，15的字符串
        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.getBaseAttrInfoListByValueIds(valueIds);
        return baseAttrInfoList;
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        BaseAttrValue  baseAttrValueQuerty = new BaseAttrValue();
        baseAttrValueQuerty.setAttrId(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValueQuerty);
        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if (baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        }else {
            baseAttrInfo.setId(null);
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        Example example = new Example(BaseAttrValue.class);
        example.createCriteria().andEqualTo("attrId",baseAttrInfo.getId());
        baseAttrValueMapper.deleteByExample(example);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        for (BaseAttrValue baseAttrValue : attrValueList) {
            String id = baseAttrInfo.getId();
            baseAttrValue.setAttrId(id);
            baseAttrValueMapper.insert(baseAttrValue);
        }

    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectAll();
        return baseSaleAttrList;
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存spu基本信息 然后获取到自增的id
        spuInfoMapper.insertSelective(spuInfo);
        //图片信息
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        for (SpuImage spuImage : spuImageList) {
            spuImage.setSpuId(spuInfo.getId());
            spuImageMapper.insertSelective(spuImage);
        }
        //销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
            spuSaleAttr.setSpuId(spuInfo.getId());
            spuSaleAttrMapper.insertSelective(spuSaleAttr);
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
            for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                spuSaleAttrValue.setSpuId(spuInfo.getId());
                spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
            }
        }
    }

    @Override
    public List<SpuInfo> spuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        List<SpuImage> spuImageList = spuImageMapper.select(spuImage);
        return spuImageList;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuId(String SpuId) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.getSpuSaleAttrListBySpuId(SpuId);
        return spuSaleAttrList;
    }

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        // sku_info
        if (skuInfo.getId()==null || skuInfo.getId().length()==0){
            // 设置id 为自增
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        }else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }

        //        sku_img,
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);

        // insert
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null && skuImageList.size()>0){
            for (SkuImage image : skuImageList) {
                /* "" 区别 null*/
                if (image.getId()!=null && image.getId().length()==0){
                    image.setId(null);
                }
                // skuId 必须赋值
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(image);
            }
        }
//        sku_attr_value,
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);

        // 插入数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            for (SkuAttrValue attrValue : skuAttrValueList) {
                if (attrValue.getId()!=null && attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                // skuId
                attrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(attrValue);
            }
        }
        //销售属性值
//        sku_sale_attr_value,
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);
//      插入数据
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                if (saleAttrValue.getId()!=null && saleAttrValue.getId().length()==0){
                    saleAttrValue.setId(null);
                }
                // skuId
                saleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        SkuInfo skuInfoResult = new SkuInfo();
        //先查询缓存，没有再查询数据库
        Jedis jedis = redisUtil.getJedis();
//        jedis.set("k1","v1");
        //redis的结构 type string，key sku:35:info，value skuInfoJson
        String skuKey =SKUKET_PREFIX+skuId+SKUKET_INFO_SUFFIX;
        String skuJson = jedis.get(skuKey);
        if (skuJson !=null){
            if (!"EMPTY".equals(skuJson)) {
                System.out.println(Thread.currentThread() + "命中缓存");
                skuInfoResult = JSON.parseObject(skuJson, SkuInfo.class);
            }
        }else {
            String lockKey=SKUKET_PREFIX+skuId+SKUKET_INFO_LOCK;
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock(10,TimeUnit.SECONDS);
            boolean tryLock=false;
            try {
                tryLock = lock.tryLock(5, 5, TimeUnit.SECONDS);
            }catch (Exception e){
                e.printStackTrace();
            }
            if(tryLock) {
                System.out.println(Thread.currentThread() + "写入缓存操作！！！！！");
                //先查询缓存，看看别的是否已经写入缓存，不用再去查询数据据
                String skuInfoJsonResult = jedis.get(skuKey);
                if (skuInfoJsonResult != null) {
                    if (!"EMPTY".equals(skuInfoJsonResult)) {
                        System.out.println(Thread.currentThread() + "再次命中缓存！！！！");
                        skuInfoResult = JSON.parseObject(skuJson, SkuInfo.class);
                    }
                } else {
                    System.out.println(Thread.currentThread()+"查询数据库！！！！");
                    skuInfoResult = getSkuInfoDB(skuId);
                    if (skuInfoResult != null) {
                        skuInfoJsonResult = JSON.toJSONString(skuInfoResult);
                    } else {
                        skuInfoJsonResult = "EMPTY";
                    }
                    jedis.setex(skuKey, SKU_EXPIRE_TIME_SEC, skuInfoJsonResult);
                }
                lock.unlock();
            }

        }
        jedis.close();
        return skuInfoResult;
    }


    public SkuInfo getSkuInfo_redis(String skuId) {
        SkuInfo skuInfoResult = new SkuInfo();
        //先查询缓存，没有再查询数据库
        Jedis jedis = redisUtil.getJedis();
//        jedis.set("k1","v1");
        //redis的结构 type string，key sku:35:info，value skuInfoJson
        String skuKey =SKUKET_PREFIX+skuId+SKUKET_INFO_SUFFIX;
        String skuJson = jedis.get(skuKey);
        if (skuJson !=null){
            if (!"EMPTY".equals(skuJson)) {
                System.out.println(Thread.currentThread() + "命中缓存");
                skuInfoResult = JSON.parseObject(skuJson, SkuInfo.class);
            }
        }else {
            String lockKey=SKUKET_PREFIX+skuId+SKUKET_INFO_LOCK;
            String lockValue = UUID.randomUUID().toString();
        //  Long lock = jedis.setnx(lockKey, lockValue);
            String lock = jedis.set(lockKey, lockValue, "NX", "EX", 10);
            if ("ok".equals(lock)) {
                System.out.println(Thread.currentThread() + "写入缓存！！！！！");
                skuInfoResult = getSkuInfoDB(skuId);
                String skuInfoJsonResult =null;
                if (skuInfoResult !=null){
                     skuInfoJsonResult = JSON.toJSONString(skuInfoResult);
                }else {
                    skuInfoJsonResult="EMPTY";
                }

                jedis.setex(skuKey, SKU_EXPIRE_TIME_SEC, skuInfoJsonResult);

            }else {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toSeconds(1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getSkuInfo(skuId);
            }
        }


        jedis.close();
        return skuInfoResult;
    }


    public SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        if (skuInfo==null){
            return null;
        }
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);

        //销售属性
        SkuSaleAttrValue attrSaleValue = new SkuSaleAttrValue();
        attrSaleValue.setSkuId(skuId);
        List<SkuSaleAttrValue> saleAttrValueList = skuSaleAttrValueMapper.select(attrSaleValue);
        skuInfo.setSkuSaleAttrValueList(saleAttrValueList);

        //平台属性
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> attrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(attrValueList);



        return skuInfo;
    }


    @Override
    public List<SpuSaleAttr> getSpuSaleAttrCheckSku(String skuId, String spuId) {
        List<SpuSaleAttr> list = spuSaleAttrMapper.getSpuSaleAttrListBySpuIdCheckSku(skuId, spuId);
        return list;
    }

    @Override
    public Map getSkuValueIdsMap(String spuId) {
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpuId(spuId);
        Map<String,String> skuIdValueIdsMap = new HashMap<>();

        //得到属性组合与sku的映射关系，用于页面根据属性组合进行跳转

        for (Map map: mapList) {
            String skuId = map.get("sku_id")+"";
            String valueIds = (String) map.get("value_ids");
            skuIdValueIdsMap.put(valueIds,skuId);
        }
        return skuIdValueIdsMap;
    }

    @Override
    public List<SkuInfo> getSkuInfoBySpuId(String spuId) {
        List<SkuInfo> SkuInfoList = new ArrayList<>();
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        List<SkuInfo> list = skuInfoMapper.select(skuInfo);
        for (SkuInfo info : list) {
            SkuInfo skuInfo1 = getSkuInfoDB(info.getId());
            SkuInfoList.add(skuInfo1);
        }
        return SkuInfoList;
    }


}



