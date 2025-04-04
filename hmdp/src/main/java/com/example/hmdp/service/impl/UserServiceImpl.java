package com.example.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.hmdp.entity.User;
import com.example.hmdp.mapper.UserMapper;
import com.example.hmdp.service.IUserService;
import com.example.hmdp.utils.JsonUtil;
import com.example.hmdp.utils.PasswordEncoder;
import com.example.hmdp.utils.UserHolder;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    
    private final RBloomFilter<String> userBloomFilter;
    private final StringRedisTemplate stringRedisTemplate;
    
    private static final String TOKEN_PREFIX = "token:";
    private static final Long TOKEN_TTL = 30L;

    public UserServiceImpl(RBloomFilter<String> userBloomFilter, UserMapper userMapper, StringRedisTemplate stringRedisTemplate) {
        this.userBloomFilter = userBloomFilter;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean mightContain(String username) {
        return userBloomFilter.contains(username);
    }

    @Override
    public void addUser(String username) {
        userBloomFilter.add(username);
    }

    @Override
    @Transactional
    public boolean register(User user) {
        // 1. 检查用户名是否已存在
        if (mightContain(user.getUsername())) {
            return false;
        }

        // 2. 密码加密
        String encodedPassword = PasswordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setStatus(1); // 设置用户状态为正常

        // 3. 保存用户信息
        int result = baseMapper.insert(user);
        if (result > 0) {
            // 4. 添加到布隆过滤器
            addUser(user.getUsername());
            return true;
        }
        return false;
    }

    @Override
    public User login(String username, String password) {
        // 1. 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = baseMapper.selectOne(queryWrapper);
        
        // 2. 验证密码
        if (user != null && PasswordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public User getCurrentUser() {
        return UserHolder.getUser();
    }

    @Override
    public String saveUserToken(User user) {
        // 1. 生成token
        String token = UUID.randomUUID().toString();
        
        // 2. 将User对象转为JSON
        String userJson = JsonUtil.toJson(user);
        
        // 3. 保存到Redis
        String key = TOKEN_PREFIX + token;
        stringRedisTemplate.opsForValue().set(key, userJson, TOKEN_TTL, TimeUnit.MINUTES);
        
        // 4. 返回token
        return token;
    }
} 