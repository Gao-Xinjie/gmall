package com.gaoxinjie.gmall.bean.enums;

public enum PaymentStatus {

    UNPAID("支付中"),
    PAID("已支付"),
    PAY_FAIL("支付失败"),
    PAY_REFOUND("用户已退款"),
    ClOSED("已关闭");

    private String name ;

    PaymentStatus(String name) {
        this.name=name;
    }
}
