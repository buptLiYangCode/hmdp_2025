package com.example.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final String LOCK_KEY = "lock:";
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String SECKILL_ORDER_KEY = "seckill:order:";
    
    public static final String CACHE_VOUCHER_KEY = "cache:voucher:";
    public static final Long CACHE_VOUCHER_TTL = 30L;
    
    public static final Long LOCK_TTL = 10L;
} 