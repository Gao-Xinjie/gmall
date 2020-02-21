package com.gaoxinjie.gmall.service;

import com.gaoxinjie.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    public CartInfo addToCart(String userId, String skuId, Integer num);

    public List<CartInfo> cartList(String userId);

    public List<CartInfo> megerCarList(String userIdDest,String userIdOrig);

    public void checkCart(String userId,String skuId,String isChecked);

    public List<CartInfo> getChechedCartList(String userId);

    public void deleteCheckedCartList(String userId);
}
