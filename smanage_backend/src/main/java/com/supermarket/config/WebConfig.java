package com.supermarket.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置 - 跨域支持 + 拦截器注册 + 静态资源
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有需要登录的接口，排除登录/注册相关 及 图片访问
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register-user",
                        "/api/auth/register-admin",
                        "/api/auth/send-sms",
                        "/api/auth/view-sms-code",
                        "/api/auth/forget-password",
                        "/api/picture/**"
                );
    }

    /**
     * 配置本地图片静态资源映射
     * /api/picture/xxx.jpg -> SupermarketManage/picture/xxx.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String picturePath = System.getProperty("user.dir") + File.separator + ".." + File.separator + "picture" + File.separator;
        registry.addResourceHandler("/api/picture/**")
                .addResourceLocations("file:" + picturePath);
    }
}
