package com.gaoxinjie.gmall.config;


import com.gaoxinjie.gmall.util.RedisUtil;
import com.gaoxinjie.gmall.util.RedissonUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    //读取配置文件中的redis的ip地址
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${Spring.redisson.adress:disabled}")
    private String adress;

    @Bean
    public RedisUtil getRedisUtil(){
        if(host.equals("disabled")){
            return null;
        }
        RedisUtil redisUtil=new RedisUtil();
        redisUtil.initJedisPool(host,port,database);
        return redisUtil;
    }

    @Bean
    public RedissonClient getRedission(){
        RedissonUtil redissonUtil = new RedissonUtil();
        RedissonClient redisson = redissonUtil.getRedisson(adress);
        return redisson;
    }


}
