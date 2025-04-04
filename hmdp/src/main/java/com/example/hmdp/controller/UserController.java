package com.example.hmdp.controller;

import com.example.hmdp.entity.User;
import com.example.hmdp.service.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        // 1. 先通过布隆过滤器快速判断用户是否存在
        if (!userService.mightContain(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
        }
        
        // 2. 验证用户名和密码
        User user = userService.login(username, password);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名或密码错误"));
        }
        
        // 3. 生成Token并保存到Redis
        String token = userService.saveUserToken(user);
        
        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return ResponseEntity.ok(result);
    }

    /**
     * 用户注册
     * @param user 用户信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        // 1. 参数校验
        if (user.getUsername() == null || user.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }

        // 2. 注册用户
        boolean success = userService.register(user);
        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }
        
        // 3. 生成Token并保存到Redis
        String token = userService.saveUserToken(user);
        
        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(user);
    }
} 