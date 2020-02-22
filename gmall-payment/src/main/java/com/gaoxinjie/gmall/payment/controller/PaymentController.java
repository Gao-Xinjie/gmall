package com.gaoxinjie.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.gaoxinjie.gmall.bean.OrderInfo;
import com.gaoxinjie.gmall.bean.PaymentInfo;
import com.gaoxinjie.gmall.bean.enums.PaymentStatus;
import com.gaoxinjie.gmall.payment.config.AlipayConfig;
import com.gaoxinjie.gmall.service.OrderService;

import com.gaoxinjie.gmall.service.PaymentService;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;

@Controller
public class PaymentController {


    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    AlipayClient alipayClient;

    @GetMapping("index")
    public String index(String orderId, HttpServletRequest request){
        OrderInfo orderInfo = orderService.getOrderInfoByOrderId(orderId);
        request.setAttribute("orderId",orderId);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    @PostMapping("/alipay/submit")
    @ResponseBody
    public String aplipaySubmit(String orderId, HttpServletResponse response){
        //准备参数给支付宝提交
        OrderInfo orderInfo = orderService.getOrderInfoByOrderId(orderId);
        AlipayTradePagePayRequest alipayRequest=new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        long currentTimeMillis = System.currentTimeMillis();
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        String outTradeNo="GAOXINJIE-"+orderId+"-"+currentTimeMillis;
        String productCode="FAST_INSTANT_TRADE_PAY";
        String subject = orderInfo.genSubject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no",outTradeNo);
        jsonObject.put("product_code",productCode);
        jsonObject.put("total_amount",totalAmount);
        jsonObject.put("subject",subject);
        alipayRequest.setBizContent(jsonObject.toJSONString());
        String submitHtml="";
        try {
            submitHtml = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html:charset=UTF-8");

        //2将支付信息提交操作保存起来
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setAlipayTradeNo(outTradeNo);
        paymentInfo.setOrderId(orderId);
        paymentInfo.setSubject(subject);
        paymentInfo.setTotalAmount(totalAmount);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentService.savePaymentInfo(paymentInfo);
        paymentService.sendDelayPaymentResult(outTradeNo,5L,3);
        return submitHtml;
    }

    @PostMapping("alipay/callback/notify")
    public String notify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) throws AlipayApiException {
        //1验证签名
        String sign = paramMap.get("sign");
        boolean ifPass = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "UTF-8", AlipayConfig.sign_type);
        if (ifPass){
            //2 判断支付失败成功标志
            String tradeStatus = paramMap.get("trade_status");
            String totalAmount = paramMap.get("total_amount");
            String outTradeNo = paramMap.get("out_trade_no");
            if ("TRADE_SUCCESS".equals(tradeStatus)){
                //查询订单的金额是否一致
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(outTradeNo);
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
                if(paymentInfo.getTotalAmount().compareTo(new BigDecimal(totalAmount))==0){
                    //3根据查询的订单状态 进行修改
                    if (paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID)){
                        //更新操作 状态 时间戳 回调信息集合
                        PaymentInfo paymentInfoForUpdate = new PaymentInfo();
                        paymentInfoForUpdate.setPaymentStatus(PaymentStatus.PAID);
                        paymentInfoForUpdate.setCallbackTime(new Date());
                        paymentInfoForUpdate.setAlipayTradeNo(paramMap.get("trade_no"));
                        paymentInfoForUpdate.setCallbackContent(JSON.toJSONString(paramMap));
                        paymentService.updayePaymentInfoByOutTradeNo(outTradeNo,paymentInfoForUpdate);
                        //TODO 发送异步消息
                        //4通知订单系统
                        //
                        return "success";
                    }else if(paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED)){
                        //手动发送关单操作
                        return "fail";
                    }else if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID)){
                        return "success";
                    }

                }
            }
        }
        //5返回success 或者 fail
        return "fail";
    }

    @GetMapping("/alipay/callback/return")
    public String alipayReturn(){

        return "success";
    }

    @GetMapping("refound")
    public String refound(String orderId) throws AlipayApiException {
        AlipayTradeRefundRequest alipayTradeRefundRequest = new AlipayTradeRefundRequest();
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no",paymentInfo.getOutTradeNo());
        jsonObject.put("refund_amount",paymentInfo.getTotalAmount());
        alipayTradeRefundRequest.setBizContent(jsonObject.toJSONString());
        AlipayTradeRefundResponse response = alipayClient.execute(alipayTradeRefundRequest);
        if (response.isSuccess()){
            System.out.println("调用成功");
                //更新状态为已退款
                PaymentInfo paymentInfoForUpdate = new PaymentInfo();
                paymentInfoForUpdate.setPaymentStatus(PaymentStatus.PAY_REFOUND);
                paymentService.updayePaymentInfoByOutTradeNo(paymentInfo.getOutTradeNo(),paymentInfoForUpdate);
                //处理订单状态
                //todo 消息队列
                return "success";
        }else {

            System.out.println("调用失败");
            return "fail";
        }
    }

}
