package com.example.hmdp.demo;

import com.example.hmdp.entity.User;
import com.example.hmdp.entity.VoucherOrder;
import com.example.hmdp.service.IAsyncTaskService;
import com.example.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异步订单创建演示类
 * 主要用于演示为什么异步订单创建中需要用户权限校验
 */
@Slf4j
@RestController
@RequestMapping("/demo")
public class AsyncOrderDemo {

    @Autowired
    private IAsyncTaskService asyncTaskService;

    /**
     * 演示没有传递用户信息时的安全问题
     */
    @GetMapping("/unsafe-order")
    public String unsafeOrderDemo() {
        log.info("=== 安全风险演示开始 ===");
        
        // 假设这是一个普通的异步线程（不支持ThreadLocal传递）
        new Thread(() -> {
            try {
                // 尝试获取用户信息（在普通子线程中无法获取）
                User user = UserHolder.getUser();
                log.info("子线程中获取到的用户: {}", user);  // 这里会是null
                
                // 如果没有进行权限校验，可能会发生以下安全问题：
                // 1. 身份伪造：任何人都可以创建其他用户的订单
                // 2. 权限绕过：没有购买资格的用户也可以下单
                // 3. 业务规则绕过：比如一人一单限制可能被绕过
                log.info("没有用户身份验证，可能导致安全问题！");
            } catch (Exception e) {
                log.error("异常：", e);
            }
        }).start();
        
        return "安全风险演示已启动，请查看日志";
    }
    
    /**
     * 演示正确传递用户信息的安全实践
     */
    @GetMapping("/safe-order")
    public String safeOrderDemo() {
        log.info("=== 安全实践演示开始 ===");
        
        // 获取当前用户
        User user = UserHolder.getUser();
        log.info("主线程用户: {}", user.getId());
        
        // 创建订单对象
        VoucherOrder order = new VoucherOrder();
        order.setUserId(user.getId());
        order.setVoucherId(1L); // 假设的优惠券ID
        
        // 使用支持ThreadLocal传递的线程池处理订单
        asyncTaskService.asyncCreateVoucherOrder(order);
        
        return "安全实践演示已启动，请查看日志";
    }
    
    /**
     * 演示用户权限校验的重要性
     */
    @GetMapping("/permission-check")
    public String permissionCheckDemo() {
        log.info("=== 权限校验演示开始 ===");
        
        // 设置随机用户权限（正常情况下从数据库获取）
        // User user = UserHolder.getUser();
        
        // 演示几种权限校验情况
        log.info("权限校验的重要性：");
        log.info("1. 用户身份验证 - 确认操作者就是登录用户");
        log.info("2. 资源拥有权 - 用户只能操作自己的订单");
        log.info("3. 操作权限验证 - 用户是否有权限进行特定操作");
        log.info("4. 业务规则校验 - 例如一人一单、库存限制等");
        
        // 现实业务场景示例
        log.info("实际业务场景中的权限校验示例：");
        log.info("- 秒杀场景：验证用户是否具备秒杀资格（如会员等级）");
        log.info("- 优惠券：验证用户是否符合领取条件（如新用户专享）");
        log.info("- 订单操作：验证操作者是否为订单创建者");
        
        return "权限校验演示已启动，请查看日志";
    }
} 