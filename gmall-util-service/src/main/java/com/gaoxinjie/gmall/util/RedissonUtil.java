package com.gaoxinjie.gmall.util;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonUtil {

    public RedissonClient getRedisson(String adress){
        Config config = new Config();
        config.useSingleServer().setAddress(adress);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
