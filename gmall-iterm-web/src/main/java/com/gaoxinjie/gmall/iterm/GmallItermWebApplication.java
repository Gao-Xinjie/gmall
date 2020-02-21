package com.gaoxinjie.gmall.iterm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.gaoxinjie.gmall")
public class GmallItermWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallItermWebApplication.class, args);
    }

}
