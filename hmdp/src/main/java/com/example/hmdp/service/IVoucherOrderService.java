package com.example.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.hmdp.entity.VoucherOrder;

public interface IVoucherOrderService extends IService<VoucherOrder> {
    /**
     * 秒杀下单
     * @param voucherId 券ID
     * @return 订单ID
     */
    Long seckillVoucher(Long voucherId);
} 