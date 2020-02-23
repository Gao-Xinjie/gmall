package com.gaoxinjie.gmall.order.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.bean.OrderDetail;
import com.gaoxinjie.gmall.bean.OrderInfo;
import com.gaoxinjie.gmall.bean.enums.ProcessStatus;
import com.gaoxinjie.gmall.service.OrderService;
import com.gaoxinjie.gmall.util.ActiveMQUtil;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderConsumer {

    @Reference
    OrderService orderService;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @JmsListener(destination = "PAYMENT_TO_ORDER",containerFactory = "jsmQueueListener")
    public void consumerPayment(MapMessage mapMessage){
        try {
            String orderId = mapMessage.getString("orderId");
            String result= mapMessage.getString("result");
            if ("success".equals(result)){
                //更改订单的状态
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                //发消息给库存系统
                sendOrderToWare(orderId);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    public void sendOrderToWare(String orderId){
        String paramJson = initParamJson(orderId);
        Connection connection = activeMQUtil.getConnection();
        try {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(queue);
            TextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(paramJson);
            producer.send(textMessage);
            //发送消息时要更改订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
            session.commit();
            session.close();
            producer.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * json 参数组装
     * @param orderId
     * @return
     */
    public String initParamJson(String orderId){
        OrderInfo orderInfo = orderService.getOrderInfoByOrderId(orderId);
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
        String paramMapJson = JSON.toJSONString(paramMap);
        return paramMapJson;
    }

    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jsmQueueListener")
    public void consumerDeduct(MapMessage mapMessage){
        try {
            String orderId = mapMessage.getString("orderId");
            String status = mapMessage.getString("status");
            if ("DEDUCTED".equals(status)){
                //减库存成功 修改订单状态为待发货
                orderService.updateOrderStatus(orderId,ProcessStatus.WAITING_DELEVER);
            }else {
                //超卖
                orderService.updateOrderStatus(orderId,ProcessStatus.STOCK_EXCEPTION);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @JmsListener(destination = "SKU_DELIVER_QUEUE",containerFactory = "jsmQueueListener")
    public void consumerDeliver(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        String trackingNo = mapMessage.getString("trackingNo");
        if ("DELEVERED".equals(status)) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setTrackingNo(trackingNo);
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED, orderInfo);
        }
    }

}
