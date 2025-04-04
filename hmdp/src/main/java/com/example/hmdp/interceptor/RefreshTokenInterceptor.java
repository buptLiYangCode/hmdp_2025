package com.example.hmdp.interceptor;

import com.example.hmdp.entity.User;
import com.example.hmdp.utils.JsonUtil;
import com.example.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.TimeUnit;

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {
    
    private final StringRedisTemplate stringRedisTemplate;
    private static final String TOKEN_PREFIX = "token:";
    private static final Long TOKEN_TTL = 30L;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // 1. 获取请求头中的token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return true;
        }
        token = token.substring(7);

        // 2. 从Redis获取用户
        String key = TOKEN_PREFIX + token;
        String userJson = stringRedisTemplate.opsForValue().get(key);
        if (userJson == null) {
            return true;
        }

        // 3. 将用户信息反序列化并保存到ThreadLocal
        User user = JsonUtil.fromJson(userJson, User.class);
        UserHolder.saveUser(user);

        // 4. 刷新token有效期
        stringRedisTemplate.expire(key, TOKEN_TTL, TimeUnit.MINUTES);
        
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) {
        UserHolder.removeUser();
    }
} 