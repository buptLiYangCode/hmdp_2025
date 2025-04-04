package com.example.hmdp.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.example.hmdp.entity.User;

public class UserHolder {
    private static final TransmittableThreadLocal<User> tl = new TransmittableThreadLocal<>();

    public static void saveUser(User user){
        tl.set(user);
    }

    public static User getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
} 