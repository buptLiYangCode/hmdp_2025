package com.example.hmdp.service;

import com.example.hmdp.entity.VoucherOrder;

public interface IAsyncTaskService {

    /**
     * 异步创建订单
     * @param voucherOrder 订单信息
     */
    void asyncCreateVoucherOrder(VoucherOrder voucherOrder);
} 