package com.gaoxinjie.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.bean.*;
import com.gaoxinjie.gmall.bean.enums.OrderStatus;
import com.gaoxinjie.gmall.bean.enums.ProcessStatus;
import com.gaoxinjie.gmall.config.LoginRequire;
import com.gaoxinjie.gmall.service.CartService;
import com.gaoxinjie.gmall.service.ManageService;
import com.gaoxinjie.gmall.service.OrderService;
import com.gaoxinjie.gmall.service.UserService;
import com.gaoxinjie.gmall.util.HttpClientUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.templateresolver.ITemplateResolutionValidity;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;


@Controller
public class OrderController {

    @Reference
     UserService userService;

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    ManageService manageService;

     @GetMapping("trade")
     @LoginRequire
    public String trade(HttpServletRequest request){
         String userId = (String) request.getAttribute("userId");
         //用户的地址列表
         List<UserAddress> userAddressList = userService.getUserAddressList(userId);

         request.setAttribute("userAddressList",userAddressList);

         //用户需要购买的商品清单
         BigDecimal totalAmount = new BigDecimal("0");
         List<CartInfo> chechedCartList = cartService.getChechedCartList(userId);
         for (CartInfo cartInfo : chechedCartList) {
             BigDecimal cartInfoAmount = cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
             totalAmount = totalAmount.add(cartInfoAmount);
         }
         String token = orderService.genToken(userId);

         request.setAttribute("tradeNo",token);

         request.setAttribute("chechedCartList",chechedCartList);

         request.setAttribute("totalAmount",totalAmount);
         return "trade";
    }

    @PostMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        String token = request.getParameter("tradeNo");
        Boolean isEnable = orderService.verifyToken(userId, token);
        if (isEnable) {
            orderInfo.setOrderStatus(OrderStatus.UNPAID);
            orderInfo.setProcessStatus(ProcessStatus.UNPAID);
            orderInfo.setCreateTime(new Date());
            orderInfo.setExpireTime(DateUtils.addMilliseconds(new Date(), 15));
            orderInfo.sumTotalAmount();
            orderInfo.setUserId(userId);
            List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
            for (OrderDetail orderDetail : orderDetailList) {
                SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
                orderDetail.setSkuName(skuInfo.getSkuName());
                orderDetail.setImgUrl(skuInfo.getSkuDefaultImg());
                if(!orderDetail.getOrderPrice().equals(skuInfo.getPrice())){
                    request.setAttribute("errMsg","商品价格已发生变动，请重新下单！");
                    return "tradeFail";
                }
            }
            //多线程验证验证库存
            List<OrderDetail> errlist = Collections.synchronizedList(new ArrayList<>());
            Stream<CompletableFuture<String>> completableFutureStream = orderDetailList.stream().map(orderDetail -> CompletableFuture.supplyAsync(() -> checkSkuNum(orderDetail)).whenComplete((hasStock, ex) -> {
                if ("0".equals(hasStock)) {
                    errlist.add(orderDetail);
                }
            }));
            CompletableFuture[] completableFutures = completableFutureStream.toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(completableFutures).join();

            if (errlist!=null&&errlist.size()>0){
                StringBuffer errstringBuffer = new StringBuffer();
                for (OrderDetail orderDetail : errlist) {
                    errstringBuffer.append("商品"+orderDetail.getSkuName()+"库存不足！");
                }
                request.setAttribute("errMsg",errstringBuffer.toString());
                return "tradeFail";
            }
           String OrderId= orderService.saveOrder(orderInfo);
            return "redirect://payment.gmall.com/index?orderId="+OrderId;
        }else {
            request.setAttribute("errMsg","页面已失效，请重新下单");
            return "tradeFail";
        }
    }

    public String checkSkuNum(OrderDetail orderDetail){
        String hasStock = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + orderDetail.getSkuId() + "&num=" + orderDetail.getSkuNum());
        return hasStock;
    }

    @PostMapping("orderSplit")
    @ResponseBody
    public String orderSplit(@RequestParam("orderId")String orderId,@RequestParam("wareSkuMap")String wareSkuMap){
        List<Map> paramMapList = orderService.orderSplit(orderId, wareSkuMap);
        String paramMapListJson = JSON.toJSONString(paramMapList);
        return paramMapListJson;
    }
}
