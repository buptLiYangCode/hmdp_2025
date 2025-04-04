package com.example.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.hmdp.entity.Voucher;

public interface IVoucherService extends IService<Voucher> {
    /**
     * 添加秒杀券
     * @param voucher 券信息
     */
    void addSeckillVoucher(Voucher voucher);
    
    /**
     * 将秒杀券库存加载到Redis
     * @param voucherId 券ID
     */
    void loadVoucherToRedis(Long voucherId);
} 