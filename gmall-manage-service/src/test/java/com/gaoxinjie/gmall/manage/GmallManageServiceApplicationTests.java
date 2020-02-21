package com.gaoxinjie.gmall.manage;


import com.alibaba.dubbo.config.annotation.Reference;
import com.gaoxinjie.gmall.config.RedisConfig;
import com.gaoxinjie.gmall.service.ManageService;
import com.gaoxinjie.gmall.util.RedisUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;

@SpringBootTest
public class GmallManageServiceApplicationTests {

    @Autowired
    RedisUtil redisUtil;
    @Test
    public void testRedis(){
        Jedis jedis = redisUtil.getJedis();
        jedis.set("test_key","test_value");
    }
}
