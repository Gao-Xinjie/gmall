package com.gaoxinjie.gmall.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.gaoxinjie.gmall.bean.PaymentInfo;
import com.gaoxinjie.gmall.bean.enums.PaymentStatus;
import com.gaoxinjie.gmall.payment.mapper.PaymentMapper;
import com.gaoxinjie.gmall.service.PaymentService;
import com.gaoxinjie.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentMapper paymentMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;




    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        PaymentInfo paymentInfoResult = paymentMapper.selectOne(paymentInfo);
        return paymentInfoResult;
    }

    @Override
    public void updayePaymentInfoByOutTradeNo(String outTradeNo, PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",outTradeNo);
        paymentMapper.updateByExampleSelective(paymentInfo,example);
    }

    @Override
    public void sendPaymentToOrder(String orderId,String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("PAYMENT_TO_ORDER");

            MessageProducer producer = session.createProducer(queue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId",orderId);
            mapMessage.setString("result",result);
            producer.send(mapMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    //查询支付宝 支付状态

    @Override
    public PaymentStatus checkAliPayPayment(PaymentInfo paymentInfo) {

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "\"out_trade_no\":\""+paymentInfo.getOutTradeNo()+"\"" +
                "  }");
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            if ("TRADE_SUCCESS".equals(response.getTradeStatus())){
                //更新支付信息 通知订单更改订单状态
                return PaymentStatus.PAID;
            }else if("WAIT_BUYER_PAY".equals(response.getTradeStatus())){
                return PaymentStatus.UNPAID;
            }
        } else {
            System.out.println("调用失败");
        }
        return null;
    }
    //发送延迟消息

    @Override
    public void sendDelayPaymentResult(String outTradeNo,Long delaySec,int checkCount) {
        Connection connection = activeMQUtil.getConnection();
        Session session = null;
        try {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setLong("delaySec",delaySec);
            mapMessage.setInt("checkCount",checkCount);

            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            producer.send(mapMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerDelayCheck(MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("outTradeNo");
        long delaySec = mapMessage.getLong("delaySec");
        int checkCount = mapMessage.getInt("checkCount");
        //判断 要不要检查 如何已经付款了 不用再查询支付宝了，也不用再发一次检查了
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfoResult = getPaymentInfo(paymentInfoQuery);
        //如果支付单据的状态不是未支付之际返回
        if (paymentInfoResult.getPaymentStatus()!=PaymentStatus.UNPAID){
            return;
        }
        //如果支付宝得到的是付款已成功 则修改订单状态
        PaymentStatus paymentStatus = checkAliPayPayment(paymentInfoResult);
        if (paymentStatus==PaymentStatus.PAID){
            PaymentInfo paymentInfoUpdate = new PaymentInfo();
            paymentInfoUpdate.setPaymentStatus(PaymentStatus.PAID);
            updayePaymentInfoByOutTradeNo(outTradeNo,paymentInfoUpdate);
            sendPaymentToOrder(paymentInfoResult.getOrderId(),"success");
        }else if (paymentStatus==PaymentStatus.UNPAID) {
            //如果支付宝得到的是未付款
            //判断checkount是否大于零       //把延迟队列减1 再发延迟队列
            if (checkCount>0){
                checkCount--;
                sendDelayPaymentResult(outTradeNo,delaySec,checkCount);
            }
        }


    }
}
