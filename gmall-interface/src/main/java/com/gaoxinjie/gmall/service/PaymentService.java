package com.gaoxinjie.gmall.service;

import com.gaoxinjie.gmall.bean.PaymentInfo;
import com.gaoxinjie.gmall.bean.enums.PaymentStatus;

public interface PaymentService {

    public void savePaymentInfo(PaymentInfo paymentInfo);

    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    public void updayePaymentInfoByOutTradeNo(String outTradeNo,PaymentInfo paymentInfo);

    public void sendPaymentToOrder(String orderId,String result);

    public PaymentStatus checkAliPayPayment(PaymentInfo paymentInfo);

    public void sendDelayPaymentResult(String outTradeNo,Long delaySec,int checkCount);
}
