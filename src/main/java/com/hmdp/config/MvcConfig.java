package com.hmdp.config;

import com.hmdp.utils.LoginIntercepor;
import com.hmdp.utils.RefreshTokenIntercepor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new LoginIntercepor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/user/me",
                        "/voucher/**",
                        "/test/**").order(1);
        // token刷新拦截器
        registry.addInterceptor(new RefreshTokenIntercepor(stringRedisTemplate))
                .addPathPatterns("/**").order(0);
    }
}
