package com.gaoxinjie.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gaoxinjie.gmall.bean.CartInfo;
import com.gaoxinjie.gmall.config.LoginRequire;
import com.gaoxinjie.gmall.constants.WebConst;
import com.gaoxinjie.gmall.service.CartService;
import com.gaoxinjie.gmall.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    CartService cartService;

    @PostMapping("addToCart")
    @LoginRequire(autoRedirect = false) //如果没有登陆也可以增加到购物车
    public String addToCart(@RequestParam("skuId") String skuId, @RequestParam("num") Integer num, HttpServletRequest request, HttpServletResponse response){
        if (skuId!=null&&num!=null) {
            String userId = (String) request.getAttribute("userId");
            if (userId==null)
                //如果用户未登录，检查用户是否有token
            {
                String  user_temp_id= CookieUtil.getCookieValue(request, "user_temp_id", false);

                if (user_temp_id!=null){
                    userId=user_temp_id;
                }else {
                    userId = UUID.randomUUID().toString();
                    CookieUtil.setCookie(request,response,"user_temp_id",userId, WebConst.COOKIE_MAXAGE,false);
                }
            }
            CartInfo cartInfo = cartService.addToCart(userId, skuId, num);
            request.setAttribute("cartInfo",cartInfo);
            request.setAttribute("num",num);
            return "success";
        }

        return "fail";
    }

    @GetMapping("cartList")
    @LoginRequire(autoRedirect = false)  //不需要登陆
    public String cartList(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartList=null;
        if(userId!=null&&userId!=""){
             cartList = cartService.cartList(userId);
        }
        String userTempId = CookieUtil.getCookieValue(request,"user_temp_id",false);
        List<CartInfo> cartListTemp=null;
        if (userTempId!=null){
            cartListTemp = cartService.cartList(userTempId);
            if (cartListTemp!=null&&cartListTemp.size()>0) { //防止出现刷新后 登陆用户的 被 临时用户的替换掉
                cartList = cartListTemp;
            }
        }
        //合并
        if (userId!=null&&cartListTemp!=null&&cartListTemp.size()>0){
            cartList = cartService.megerCarList(userId, userTempId);
        }

        request.setAttribute("cartList",cartList);
        return "cartList";
    }


    @PostMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void  checkCart(@RequestParam("skuId") String skuId,@RequestParam("isChecked") String isChecked,HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        if(userId==null){
            userId = CookieUtil.getCookieValue(request,"user_temp_id",false);
        }
        cartService.checkCart(userId,skuId,isChecked);
    }
}
