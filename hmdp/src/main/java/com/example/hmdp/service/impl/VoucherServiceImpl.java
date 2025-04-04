package com.example.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.hmdp.entity.Voucher;
import com.example.hmdp.mapper.VoucherMapper;
import com.example.hmdp.service.IVoucherService;
import com.example.hmdp.utils.CacheClient;
import com.example.hmdp.utils.RedisConstants;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private CacheClient cacheClient;

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存到数据库
        save(voucher);
        // 加载到Redis
        loadVoucherToRedis(voucher.getId());
    }

    @Override
    public void loadVoucherToRedis(Long voucherId) {
        // 查询库存
        Voucher voucher = getById(voucherId);
        if (voucher == null || voucher.getStock() == null || voucher.getStock() <= 0) {
            return;
        }
        
        // 1. 保存库存到Redis
        stringRedisTemplate.opsForValue().set(
                RedisConstants.SECKILL_STOCK_KEY + voucherId, 
                voucher.getStock().toString()
        );
        
        // 2. 保存商品信息，使用逻辑过期方式解决缓存击穿
        cacheClient.setWithLogicalExpire(
                RedisConstants.CACHE_VOUCHER_KEY + voucherId,
                voucher,
                RedisConstants.CACHE_VOUCHER_TTL,
                TimeUnit.MINUTES
        );
    }

    /**
     * 项目启动时加载所有秒杀券库存到Redis
     */
    @PostConstruct
    public void loadVoucherStocksToRedis() {
        // 查询所有秒杀券
        List<Voucher> voucherList = list();
        // 加载到Redis
        for (Voucher voucher : voucherList) {
            if (voucher.getStock() != null && voucher.getStock() > 0) {
                loadVoucherToRedis(voucher.getId());
            }
        }
    }
    
    /**
     * 查询商品，解决缓存击穿问题
     * @param id 商品ID
     * @return 商品信息
     */
    public Voucher queryVoucherWithCache(Long id) {
        // 使用逻辑过期方式解决缓存击穿
        return cacheClient.queryWithLogicalExpire(
                RedisConstants.CACHE_VOUCHER_KEY,
                id,
                Voucher.class,
                this::getById,
                RedisConstants.CACHE_VOUCHER_TTL,
                TimeUnit.MINUTES
        );
    }
} 