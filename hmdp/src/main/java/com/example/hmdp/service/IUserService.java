package com.example.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.hmdp.entity.User;

public interface IUserService extends IService<User> {
    /**
     * 检查用户是否存在
     * @param username 用户名
     * @return true: 可能存在 false: 一定不存在
     */
    boolean mightContain(String username);

    /**
     * 添加用户到布隆过滤器
     * @param username 用户名
     */
    void addUser(String username);

    /**
     * 用户注册
     * @param user 用户信息
     * @return 是否注册成功
     */
    boolean register(User user);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 用户信息，登录失败返回null
     */
    User login(String username, String password);

    /**
     * 获取当前登录用户
     * @return 用户信息
     */
    User getCurrentUser();

    /**
     * 生成Token并保存到Redis
     * @param user 用户信息
     * @return Token
     */
    String saveUserToken(User user);
} 