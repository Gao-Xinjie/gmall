package com.gaoxinjie.gmall.cart.mapper;

import com.gaoxinjie.gmall.bean.CartInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {

    public List<CartInfo> selectCartListWithSkuPrice(@Param("userId") String userId);

    public void megerCartInfoList(@Param("userIdDest") String userIdDest,@Param("userIdOrig") String userIdOrig);
}
