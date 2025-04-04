package com.example.hmdp.controller;

import com.example.hmdp.entity.User;
import com.example.hmdp.entity.VoucherOrder;
import com.example.hmdp.service.IAsyncTaskService;
import com.example.hmdp.service.IVoucherOrderService;
import com.example.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;

@Slf4j
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Autowired
    private IVoucherOrderService voucherOrderService;
    
    @Autowired
    private ExecutorService asyncTaskExecutor;
    
    @Autowired
    private IAsyncTaskService asyncTaskService;

    /**
     * 秒杀券下单
     * @param voucherId 券ID
     * @return 订单ID
     */
    @PostMapping("/seckill/{voucherId}")
    public Long seckill(@PathVariable("voucherId") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
    
    /**
     * 测试异步线程传递ThreadLocal
     * @return 测试结果
     */
    @GetMapping("/test-async")
    public String testAsync() {
        // 当前线程中的用户信息
        User user = UserHolder.getUser();
        log.info("主线程 - 当前用户: {}", user.getId());
        
        // 使用普通线程池（不支持ThreadLocal传递）
        new Thread(() -> {
            User threadUser = UserHolder.getUser();
            log.info("普通线程 - 用户信息: {}", threadUser != null ? threadUser.getId() : "null");
        }).start();
        
        // 使用支持TTL的线程池
        asyncTaskExecutor.submit(() -> {
            User ttlUser = UserHolder.getUser();
            log.info("TTL线程 - 用户信息: {}", ttlUser != null ? ttlUser.getId() : "null");
        });
        
        return "异步测试已启动，请查看日志";
    }
    
    /**
     * 演示异步订单创建
     * @param voucherId 优惠券ID
     * @return 处理结果
     */
    @PostMapping("/async-seckill/{voucherId}")
    public String asyncSeckill(@PathVariable("voucherId") Long voucherId) {
        // 获取用户ID
        Long userId = UserHolder.getUser().getId();
        log.info("接收到异步秒杀请求 - 用户ID: {}，商品ID: {}", userId, voucherId);
        
        // 创建订单对象
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        
        // 提交异步处理
        asyncTaskService.asyncCreateVoucherOrder(voucherOrder);
        
        return "订单已提交异步处理，处理结果将通过日志记录";
    }
} 