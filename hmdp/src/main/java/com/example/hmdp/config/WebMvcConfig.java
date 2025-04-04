package com.example.hmdp.config;

import com.example.hmdp.interceptor.LoginInterceptor;
import com.example.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final RefreshTokenInterceptor refreshTokenInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor, RefreshTokenInterceptor refreshTokenInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.refreshTokenInterceptor = refreshTokenInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 第一个拦截器：token刷新，优先级高
        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/**")
                .order(0);
        
        // 第二个拦截器：登录拦截
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/user/login",
                        "/user/register"
                ).order(1);
    }
} 