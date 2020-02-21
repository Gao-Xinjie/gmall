package com.gaoxinjie.gmall.manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan(basePackages = "com.gaoxinjie.gmall.manage.mapper")
@SpringBootApplication
@ComponentScan(basePackages = "com.gaoxinjie.gmall")
public class GmallManageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallManageServiceApplication.class, args);
    }

}
