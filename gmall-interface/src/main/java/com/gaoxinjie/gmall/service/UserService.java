package com.gaoxinjie.gmall.service;



import com.gaoxinjie.gmall.bean.UserAddress;
import com.gaoxinjie.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    List<UserInfo> getUserInfoListAll();

    void addUser(UserInfo userInfo);

    UserInfo getUserInfoById(String userid);

    void updateUser(UserInfo userInfo);

    void updateUserByName(String name,UserInfo userInfo);

    void delUser(UserInfo userInfo);

    public UserInfo login(UserInfo userInfo);

    public Boolean verify(String userId);

    public List<UserAddress> getUserAddressList(String userId);
}
