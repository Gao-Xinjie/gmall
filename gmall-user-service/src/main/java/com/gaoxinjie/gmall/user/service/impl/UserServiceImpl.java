package com.gaoxinjie.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gaoxinjie.gmall.bean.UserAddress;
import com.gaoxinjie.gmall.bean.UserInfo;
import com.gaoxinjie.gmall.service.UserService;
import com.gaoxinjie.gmall.user.mapper.UserAddressMapper;
import com.gaoxinjie.gmall.util.RedisUtil;
import com.gaoxinjie.gmall.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;


    @Autowired
    UserMapper userMapper;

    @Autowired
    UserAddressMapper addressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UserInfo> getUserInfoListAll() {
        List<UserInfo> userInfoList = userMapper.selectAll();
        return userInfoList;
    }


    @Override
    public void addUser(UserInfo userInfo) {
        userMapper.insert(userInfo);
    }

    @Override
    public UserInfo getUserInfoById(String userid) {
        UserInfo userInfo = userMapper.selectByPrimaryKey(userid);
        return  userInfo;
    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    public void updateUserByName(String name, UserInfo userInfo) {

        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",name);
        userMapper.updateByExample(userInfo,example);

    }

    @Override
    public void delUser(UserInfo userInfo) {
        userMapper.deleteByPrimaryKey(userInfo.getId());
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //从数据库中查询是否存在
        String passwdMD5 = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(passwdMD5);

        UserInfo userInfoExists = userMapper.selectOne(userInfo);
        if (userInfoExists!=null){
            //加载到缓存当中 type String  key user:101:info  value
            Jedis jedis = redisUtil.getJedis();
            String userKey=userKey_prefix+userInfoExists.getId()+userinfoKey_suffix;
            String userInfoJson = JSON.toJSONString(userInfoExists);
            jedis.setex(userKey,userKey_timeOut,userInfoJson);
            jedis.close();
            return userInfoExists;
        }
        return null;
    }

    @Override
    public Boolean verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userKey=userKey_prefix+userId+userinfoKey_suffix;
        Boolean exists = jedis.exists(userKey);
        if (exists) {
            //如果经过验证 延长过期时间
            jedis.expire(userKey,userKey_timeOut);
            jedis.close();
            return true;
        }else {
            jedis.close();
            return false;
        }
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddressList = addressMapper.select(userAddress);

        return userAddressList;
    }


}
