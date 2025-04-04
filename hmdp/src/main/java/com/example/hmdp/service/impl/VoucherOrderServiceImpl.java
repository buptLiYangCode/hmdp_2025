package com.example.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.hmdp.entity.VoucherOrder;
import com.example.hmdp.mapper.VoucherMapper;
import com.example.hmdp.mapper.VoucherOrderMapper;
import com.example.hmdp.service.IAsyncTaskService;
import com.example.hmdp.service.IVoucherOrderService;
import com.example.hmdp.utils.RedisConstants;
import com.example.hmdp.utils.RedisIdWorker;
import com.example.hmdp.utils.SimpleRedisLock;
import com.example.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisIdWorker redisIdWorker;
    
    @Autowired
    private IAsyncTaskService asyncTaskService;

    // Lua脚本：原子性检查库存并扣减
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Long seckillVoucher(Long voucherId) {
        // 1. 获取用户ID
        Long userId = UserHolder.getUser().getId();
        
        // 2. 使用Lua脚本，原子性地判断库存和一人一单，并扣减库存
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.singletonList(RedisConstants.SECKILL_STOCK_KEY + voucherId),
                userId.toString(), voucherId.toString()
        );
        
        // 3. 判断结果
        // 0: 库存不足; 1: 下单成功; 2: 已经下过单
        int r = result.intValue();
        if (r != 1) {
            // 库存不足或已经下过单
            return r == 0 ? -1L : -2L;
        }

        // 4. 生成订单ID
        Long orderId = redisIdWorker.nextId("order");
        
        // 5. 创建订单对象
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setStatus(1);
        voucherOrder.setCreateTime(LocalDateTime.now());
        
        // 6. 异步处理订单
        asyncTaskService.asyncCreateVoucherOrder(voucherOrder);
        
        // 7. 返回订单ID
        return orderId;
    }
    // TODO 这个方法可以删除
    @Transactional
    public Long createVoucherOrder(Long voucherId, Long userId, Long orderId) {
        // 1. 使用分布式锁，保证一人一单
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        boolean isLock = lock.tryLock(10);
        if (!isLock) {
            // 获取锁失败，可能是重复下单
            return -2L;
        }
        
        try {
            // 2. 再次检查是否已经购买过
            Long count = lambdaQuery()
                    .eq(VoucherOrder::getUserId, userId)
                    .eq(VoucherOrder::getVoucherId, voucherId)
                    .count();
            if (count > 0) {
                // 用户已经购买过
                return -2L;
            }

            // 3. 扣减库存(乐观锁)，确保最终的数据安全
            int updated = voucherMapper.decreaseStock(voucherId);
            if (updated == 0) {
                // 扣减库存失败
                return -1L;
            }

            // 4. 创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(orderId);
            voucherOrder.setUserId(userId);
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setStatus(1);
            voucherOrder.setCreateTime(LocalDateTime.now());
            
            // 5. 保存订单
            save(voucherOrder);
            
            // 6. 返回订单ID
            return orderId;
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
} 