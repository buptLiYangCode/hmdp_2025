package com.example.hmdp.service.impl;

import com.example.hmdp.entity.User;
import com.example.hmdp.entity.VoucherOrder;
import com.example.hmdp.mapper.VoucherMapper;
import com.example.hmdp.mapper.VoucherOrderMapper;
import com.example.hmdp.service.IAsyncTaskService;
import com.example.hmdp.utils.RedisConstants;
import com.example.hmdp.utils.SimpleRedisLock;
import com.example.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class AsyncTaskServiceImpl implements IAsyncTaskService {

    @Resource
    private ExecutorService asyncTaskExecutor;
    
    @Autowired
    private VoucherOrderMapper voucherOrderMapper;
    
    @Autowired
    private VoucherMapper voucherMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void asyncCreateVoucherOrder(VoucherOrder voucherOrder) {
        // 获取当前用户信息，这里会从父线程的ThreadLocal中获取
        User user = UserHolder.getUser();
        log.info("订单创建开始 - 线程：{}，用户ID：{}，商品ID：{}", 
                Thread.currentThread().getName(), user.getId(), voucherOrder.getVoucherId());
        
        // 提交到异步线程池执行
        asyncTaskExecutor.submit(() -> {
            // 使用分布式锁保证一人一单
            SimpleRedisLock lock = new SimpleRedisLock(
                    RedisConstants.LOCK_KEY + "order:" + voucherOrder.getUserId(), 
                    stringRedisTemplate);
            
            boolean isLock = false;
            try {
                // 1. 在子线程中仍然可以获取到用户信息
                User asyncUser = UserHolder.getUser();
                log.info("异步处理订单 - 线程：{}，用户ID：{}，订单ID：{}", 
                        Thread.currentThread().getName(), asyncUser.getId(), voucherOrder.getId());
                
                // 2. 获取分布式锁
                isLock = lock.tryLock(10);
                if (!isLock) {
                    // 获取锁失败，可能是重复下单
                    log.warn("获取分布式锁失败 - 用户ID：{}，商品ID：{}", 
                            asyncUser.getId(), voucherOrder.getVoucherId());
                    // 可能是并发问题，此处不需要恢复缓存
                    return;
                }
                
                // 3. 检查是否已经下过单（一人一单）
                int count = checkExistOrder(voucherOrder.getUserId(), voucherOrder.getVoucherId());
                if (count > 0) {
                    log.warn("用户已经购买过此商品 - 用户ID：{}，商品ID：{}", 
                            asyncUser.getId(), voucherOrder.getVoucherId());
                    // 这是正常业务限制，需要恢复缓存
                    restoreCache(voucherOrder.getVoucherId());
                    return;
                }
                
                // 4. 执行订单创建和库存扣减逻辑
                boolean success = createVoucherOrderAsync(voucherOrder);
                
                // 5. 记录详细日志
                if (success) {
                    log.info("订单创建成功 - 用户ID：{}，订单ID：{}，商品ID：{}，创建时间：{}", 
                            asyncUser.getId(), voucherOrder.getId(), 
                            voucherOrder.getVoucherId(), LocalDateTime.now());
                } else {
                    log.error("订单创建失败 - 用户ID：{}，订单ID：{}，商品ID：{}", 
                            asyncUser.getId(), voucherOrder.getId(), voucherOrder.getVoucherId());
                    // 订单创建失败，恢复缓存
                    restoreCache(voucherOrder.getVoucherId());
                }
            } catch (Exception e) {
                log.error("异步创建订单异常 - 订单ID：{}，异常信息：{}", 
                        voucherOrder.getId(), e.getMessage(), e);
                // 异常情况下，恢复缓存
                restoreCache(voucherOrder.getVoucherId());
            } finally {
                // 释放锁
                if (isLock) {
                    lock.unlock();
                }
                // 清理线程变量，防止内存泄漏
                UserHolder.removeUser();
            }
        });
    }
    
    /**
     * 检查用户是否已经购买过此商品
     */
    private int checkExistOrder(Long userId, Long voucherId) {
        // 简单计数查询，检查是否存在该用户的订单
        // 实际项目中可能需要考虑订单状态等条件
        Long count = voucherOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getVoucherId, voucherId));
        return count == null ? 0 : count.intValue();
    }
    
    /**
     * 恢复缓存，确保缓存与数据库一致
     * @param voucherId 优惠券ID
     */
    private void restoreCache(Long voucherId) {
        try {
            log.info("开始恢复缓存 - 商品ID：{}", voucherId);
            // 1. 构建缓存key
            String key = RedisConstants.SECKILL_STOCK_KEY + voucherId;
            
            // 2. 重新加载库存到缓存（延迟双删策略）
            // 先删除旧缓存
            stringRedisTemplate.delete(key);
            
            // 3. 查询数据库获取最新库存
            com.example.hmdp.entity.Voucher voucher = voucherMapper.selectById(voucherId);
            if (voucher != null) {
                // 将最新库存写入缓存
                stringRedisTemplate.opsForValue().set(key, voucher.getStock().toString());
                log.info("缓存恢复成功 - 商品ID：{}，库存：{}", voucherId, voucher.getStock());
            } else {
                log.warn("缓存恢复失败 - 商品不存在，ID：{}", voucherId);
            }
        } catch (Exception e) {
            log.error("恢复缓存异常 - 商品ID：{}，异常：{}", voucherId, e.getMessage(), e);
        }
    }
    
    /**
     * 异步创建订单和扣减库存
     * @param voucherOrder 订单信息
     * @return 是否成功
     */
    @Transactional
    public boolean createVoucherOrderAsync(VoucherOrder voucherOrder) {
        try {
            // 1. 检查库存是否充足（数据库级别检查）
            // 我们使用乐观锁来处理并发问题，在SQL中检查stock>0
            
            // 2. 保存订单
            log.info("保存订单 - 订单ID：{}，用户ID：{}，商品ID：{}", 
                    voucherOrder.getId(), voucherOrder.getUserId(), voucherOrder.getVoucherId());
            int result = voucherOrderMapper.insert(voucherOrder);
            
            if (result <= 0) {
                log.warn("订单保存失败 - 订单ID：{}", voucherOrder.getId());
                return false;
            }
            
            // 3. 扣减库存（使用乐观锁机制）
            log.info("开始扣减库存 - 商品ID：{}", voucherOrder.getVoucherId());
            int updated = voucherMapper.decreaseStock(voucherOrder.getVoucherId());
            
            if (updated <= 0) {
                // 库存不足或并发冲突，回滚事务
                log.warn("库存扣减失败 - 商品ID：{}", voucherOrder.getVoucherId());
                throw new RuntimeException("库存扣减失败，可能库存不足或并发冲突");
            }
            
            log.info("库存扣减成功 - 商品ID：{}", voucherOrder.getVoucherId());
            return true;
        } catch (Exception e) {
            log.error("订单创建事务异常 - 订单ID：{}，异常：{}", 
                    voucherOrder.getId(), e.getMessage(), e);
            throw e; // 重新抛出异常，让事务回滚
        }
    }
} 