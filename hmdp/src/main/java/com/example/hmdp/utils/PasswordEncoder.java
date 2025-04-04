package com.example.hmdp.utils;

import org.springframework.util.DigestUtils;
import java.util.UUID;

public class PasswordEncoder {
    
    public static String encode(String password) {
        // 生成盐值
        String salt = UUID.randomUUID().toString().replaceAll("-", "");
        // 对密码进行加密
        String encodedPassword = DigestUtils.md5DigestAsHex((password + salt).getBytes());
        return salt + ":" + encodedPassword;
    }

    public static boolean matches(String password, String encodedPassword) {
        String[] parts = encodedPassword.split(":");
        if (parts.length != 2) {
            return false;
        }
        String salt = parts[0];
        String encoded = DigestUtils.md5DigestAsHex((password + salt).getBytes());
        return encoded.equals(parts[1]);
    }
} 