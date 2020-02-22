package com.gaoxinjie.gmall.service;


import com.gaoxinjie.gmall.bean.OrderInfo;
import com.gaoxinjie.gmall.bean.enums.ProcessStatus;

public interface OrderService {

    public String saveOrder(OrderInfo orderInfo);

    public String genToken(String userId);

    public Boolean verifyToken(String userId,String token);

    public OrderInfo getOrderInfoByOrderId(String orderId);

    void updateOrderStatus(String orderId, ProcessStatus processStatus,OrderInfo... orderInfos);
}
