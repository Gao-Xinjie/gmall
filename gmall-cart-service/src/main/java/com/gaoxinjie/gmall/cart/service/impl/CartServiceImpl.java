package com.gaoxinjie.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.bean.CartInfo;
import com.gaoxinjie.gmall.bean.SkuInfo;
import com.gaoxinjie.gmall.cart.mapper.CartInfoMapper;
import com.gaoxinjie.gmall.service.CartService;
import com.gaoxinjie.gmall.service.ManageService;
import com.gaoxinjie.gmall.util.RedisUtil;

import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Reference
    ManageService manageService;

    @Override
    public CartInfo addToCart(String userId, String skuId, Integer num) {

        Jedis jedis = redisUtil.getJedis();
        String cartKey="cart:"+userId+":info";
        //保存到数据库中 先查找有没有 ， 有则update，如果没有就insert
        CartInfo queryCartInfo = new CartInfo();
        queryCartInfo.setUserId(userId);
        queryCartInfo.setSkuId(skuId);
        CartInfo cartInfoExists = cartInfoMapper.selectOne(queryCartInfo);
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        if (cartInfoExists!=null){ //修改
            cartInfoExists.setSkuNum(cartInfoExists.getSkuNum()+num);
            cartInfoExists.setSkuName(skuInfo.getSkuName());
            cartInfoExists.setCartPrice(skuInfo.getPrice());
            cartInfoExists.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExists);
        }else {
            //新增
            CartInfo insertCartInfo= new CartInfo();
            insertCartInfo.setSkuId(skuId);
            insertCartInfo.setUserId(userId);
            insertCartInfo.setSkuNum(num);
            insertCartInfo.setSkuName(skuInfo.getSkuName());
            insertCartInfo.setSkuPrice(skuInfo.getPrice());
            insertCartInfo.setCartPrice(skuInfo.getPrice());
            insertCartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.insertSelective(insertCartInfo);
            //将新增加的赋值给已经存在的
            cartInfoExists=insertCartInfo;
        }

        //保存到缓存里 type hash key  cart:102:info  filed skuId   value cartInfo
        String cartInfoJson = JSON.toJSONString(cartInfoExists);
        jedis.hset(cartKey,skuId,cartInfoJson);

        jedis.close();
        return cartInfoExists;
    }

    @Override
    public List<CartInfo> cartList(String userId) {
        //先查缓存
        Jedis jedis = redisUtil.getJedis();
        String cartKey="cart:"+userId+":info";
        List<String> cartInfoListJson = jedis.hvals(cartKey);
        List<CartInfo> cartInfoList = new ArrayList<>();

        if (cartInfoListJson!=null&&cartInfoList.size()>0){  //缓存命中
            for (String cartInfoJson : cartInfoListJson) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //按照时间排序
            cartInfoList.sort((o1,o2)->{
               return o2.getId().compareTo(o1.getId());
            });
            jedis.close();
            return cartInfoList;

        }else { //查询数据库,并且加入到缓存中
            return loadCartCache(userId);
        }
    }

    @Override
    public List<CartInfo> megerCarList(String userIdDest, String userIdOrig) {
        //先做合并
        cartInfoMapper.megerCartInfoList(userIdDest,userIdOrig);
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userIdOrig);
        cartInfoMapper.delete(cartInfo);
        //查数据，更新缓存,先清理缓存
        Jedis jedis = redisUtil.getJedis();
        jedis.del("cart:"+userIdOrig+":info");
        List<CartInfo> cartInfoList = loadCartCache("userIdDest");
        jedis.close();
        return cartInfoList;
    }

   

    public List<CartInfo> loadCartCache(String userId){
        //查询数据库
        Jedis jedis = redisUtil.getJedis();
        String cartKey="cart:"+userId+":info";
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithSkuPrice(userId);
        //写入缓存
        //为了方便将list存在缓存中，将list转换为map
        if (cartInfoList!=null&&cartInfoList.size()>0) {
            Map<String, String> cartMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                cartMap.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
            }
            jedis.del(cartKey);
            jedis.hmset(cartKey, cartMap);
            jedis.expire(cartKey, 60 * 60 * 24);
        }
        jedis.close();
        return cartInfoList;
    }

    public void loadCartCacheExist(String userId){
        String cartkey="cart:"+userId+":info";
        Jedis jedis = redisUtil.getJedis();
        Long ttl = jedis.ttl(cartkey);
        int ttlInt = ttl.intValue();
        jedis.expire(cartkey,ttlInt+10);
        Boolean exists = jedis.exists(cartkey);
        jedis.close();
        if(!exists){
            loadCartCache(userId);
        }
    }
    @Override
    public void checkCart(String userId, String skuId, String isChecked) {
        loadCartCacheExist(userId); //查询购物车的缓存是否存在

        //将ischecked数据放入缓存当中
        String cartkey="cart:"+userId+":info";
        Jedis jedis = redisUtil.getJedis();
        String cartInfoJson = jedis.hget(cartkey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartInfoJsonNew = JSON.toJSONString(cartInfo);
        String cartCheckedKey="cart:"+userId+":checked";
        jedis.hset(cartkey,skuId,cartInfoJsonNew);
        //为了支付方便，将已经选择的购物项放入到缓存中
        if ("1".equals(isChecked)){
            jedis.hset(cartCheckedKey,skuId,cartInfoJsonNew);
            jedis.expire(cartCheckedKey,60*60);
        }else {
            jedis.hdel(cartCheckedKey,skuId,cartInfoJsonNew);
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getChechedCartList(String userId) {
        String cartCheckedKey="cart:"+userId+":checked";
        Jedis jedis = redisUtil.getJedis();
        List<String> checkedCartListJson = jedis.hvals(cartCheckedKey);
        List<CartInfo> checkedCartList = new ArrayList<>();
        if (checkedCartListJson!=null&&checkedCartListJson.size()>0);
        {
            for (String cartInfo : checkedCartListJson) {
                checkedCartList.add(JSON.parseObject(cartInfo,CartInfo.class));
            }
        }
        jedis.close();

        return checkedCartList;
    }

    @Override
    public void deleteCheckedCartList(String userId){

        String cartCheckedKey="cart:"+userId+":checked";
        Jedis jedis = redisUtil.getJedis();
        jedis.del(cartCheckedKey);
        jedis.close();
    }
}
