package com.gaoxinjie.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.gaoxinjie.gmall.bean.OrderDetail;
import com.gaoxinjie.gmall.bean.OrderInfo;
import com.gaoxinjie.gmall.bean.enums.ProcessStatus;
import com.gaoxinjie.gmall.order.mapper.OrderDetailMapper;
import com.gaoxinjie.gmall.order.mapper.OrderInfoMapper;
import com.gaoxinjie.gmall.service.OrderService;
import com.gaoxinjie.gmall.util.RedisUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.xml.soap.Detail;
import java.util.List;
import java.util.UUID;
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
}
