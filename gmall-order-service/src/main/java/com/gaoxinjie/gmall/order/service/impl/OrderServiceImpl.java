package com.gaoxinjie.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.bean.OrderDetail;
import com.gaoxinjie.gmall.bean.OrderInfo;
import com.gaoxinjie.gmall.bean.enums.ProcessStatus;
import com.gaoxinjie.gmall.order.mapper.OrderDetailMapper;
import com.gaoxinjie.gmall.order.mapper.OrderInfoMapper;
import com.gaoxinjie.gmall.service.OrderService;
import com.gaoxinjie.gmall.util.RedisUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.xml.soap.Detail;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderInfoMapper orderInfoMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;


    //删除购物车信息
    @Transactional
    public void saveOrderAndDelCart(OrderInfo orderInfo) {
        String checkedCacheKey="cart:"+orderInfo.getUserId()+":checked";
        String userOrderLockKey="order:"+orderInfo.getUserId()+"lock";
        Jedis jedis = redisUtil.getJedis();
        RLock lock = redissonClient.getLock(userOrderLockKey);
        lock.lock(2, TimeUnit.SECONDS);
        orderInfoMapper.insertSelective(orderInfo);
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        lock.unlock();
        jedis.close();
    }

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        orderInfoMapper.insertSelective(orderInfo);
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        return orderInfo.getId();
    }

    @Override
    public String genToken(String userId) {
        //type String   key user:101:trade_code   value token
        String tokenKey="user:"+userId+":trade_code";
        String token = UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        jedis.setex(tokenKey,10*60,token);

        jedis.close();
        return token;
    }

    @Override
    public Boolean verifyToken(String userId, String token) {
        String tokenKey="user:"+userId+":trade_code";
        Jedis jedis = redisUtil.getJedis();
        String tokenExist = jedis.get(tokenKey);
        jedis.watch(tokenKey);
        Transaction transaction = jedis.multi();
        if (tokenExist!=null&&token.equals(tokenExist)){
            transaction.del(tokenKey);
        }
        List<Object> list = transaction.exec();
        jedis.close();
        if (list!=null&&list.size()>0&&(Long)list.get(0)==1){
            return true;
        }else {

            return false;
        }
    }

    @Override
    public OrderInfo getOrderInfoByOrderId(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus,OrderInfo... orderInfos) {
        OrderInfo orderInfo = new OrderInfo();
        if (orderInfos!=null&&orderInfos.length>0){
            orderInfo=orderInfos[0];
        }
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public List<Map> orderSplit(String orderId, String wareSkuMap) {
        List<Map> paramMapList = new ArrayList<>();
        //1 根据订单的id 查询出原始订单
        OrderInfo orderInfoParent = getOrderInfoByOrderId(orderId);
        //2遍历 wareMap 一次就是生成一个订单， 主订单 orderInfo 订单明细 orderDetial
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        for (Map wareMap : mapList) {
            OrderInfo orderInfoSub = new OrderInfo();
            //3子订单 与 订单主表的 orderInfo 内容基本相同 待修改 金额 id parent_order_id
            try {
                BeanUtils.copyProperties(orderInfoSub,orderInfoParent);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            //4子订单的订单明细
            List<String> skuIdList=(List<String>)wareMap.get("skuIds"); //拆单方案中子订单的skuId
            List<OrderDetail> orderDetailList = orderInfoParent.getOrderDetailList(); //父订单中所有的子订单明细
            List<OrderDetail> orderDetailSubList=new ArrayList<>(); //保存子订单明细
            for (String skuId : skuIdList) {
                for (OrderDetail orderDetail : orderDetailList) {
                    OrderDetail orderDetailSub = new OrderDetail();
                    try {
                        BeanUtils.copyProperties(orderDetailSub,orderDetail);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    orderDetailSub.setOrderId(null);
                    orderDetailSub.setId(null);
                    orderDetailSubList.add(orderDetailSub);
                }
            }
            //5组合完成好 保存拆分的订单
            orderInfoSub.setOrderDetailList(orderDetailSubList);
            orderInfoSub.setId(null);
            orderInfoSub.sumTotalAmount();
            orderInfoSub.setParentOrderId(orderInfoParent.getId());
            //6把子订单 包装成库存结构需要的map结构
            saveOrder(orderInfoSub);
            Map map = initWareParamJsonFromOrderInfo(orderInfoSub);
            String wareId = (String) wareMap.get("wareId");
            map.put("wareId",wareId);
            paramMapList.add(map);
            //原始订单的装态改为一拆分
            updateOrderStatus(orderId,ProcessStatus.SPLIT);
        }
        //组合成list 返回


        return paramMapList;
    }

    public Map initWareParamJsonFromOrderInfo(OrderInfo orderInfo){
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("orderId",orderInfo.getId());
        paramMap.put("consignee", orderInfo.getConsignee());
        paramMap.put("consigneeTel",orderInfo.getConsigneeTel());
        paramMap.put("orderComment",orderInfo.getOrderComment());
        paramMap.put("orderBody",orderInfo.genSubject());
        paramMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
        paramMap.put("paymentWay","2");
        List<Map<String,String>> details = new ArrayList<>();
        List<OrderDetail> orderDetails = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetails) {
            Map<String,String> detailMap = new HashMap<>();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum().toString());
            details.add(detailMap);
        }
        paramMap.put("details",details);
        return paramMap;
    }
}
