package com.gaoxinjie.gmall.service;


import com.gaoxinjie.gmall.bean.OrderInfo;

public interface OrderService {

    public String saveOrder(OrderInfo orderInfo);

    public String genToken(String userId);

    public Boolean verifyToken(String userId,String token);

    public OrderInfo getOrderInfoByOrderId(String orderId);
}
