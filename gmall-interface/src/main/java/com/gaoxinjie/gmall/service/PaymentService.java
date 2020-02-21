package com.gaoxinjie.gmall.service;

import com.gaoxinjie.gmall.bean.PaymentInfo;

public interface PaymentService {

    public void savePaymentInfo(PaymentInfo paymentInfo);

    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    public void updayePaymentInfoByOutTradeNo(String outTradeNo,PaymentInfo paymentInfo);
}
