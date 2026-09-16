package com.supermarket.controller;

import com.supermarket.common.Result;
import com.supermarket.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Autowired
    private HttpServletRequest request;

    /**
     * 发送短信验证码
     */
    @PostMapping("/send-sms")
    public Result<?> sendSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        authService.sendSmsCode(phone);
        return Result.ok("验证码已发送");
    }

    /**
     * 查看验证码（课程设计模拟）
     */
    @PostMapping("/view-sms-code")
    public Result<?> viewSmsCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = authService.viewSmsCode(phone);
        return Result.ok(code);
    }

    /**
     * 普通用户注册
     */
    @PostMapping("/register-user")
    public Result<?> registerUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String phone = body.get("phone");
        String password = body.get("password");
        String smsCode = body.get("smsCode");
        authService.registerUser(username, phone, password, smsCode);
        return Result.ok("注册成功");
    }

    /**
     * 管理员注册
     */
    @PostMapping("/register-admin")
    public Result<?> registerAdmin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String phone = body.get("phone");
        String password = body.get("password");
        String smsCode = body.get("smsCode");
        String adminCode = body.get("adminCode");
        authService.registerAdmin(username, phone, password, smsCode, adminCode);
        return Result.ok("管理员注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String account = body.get("account");
        String password = body.get("password");
        Map<String, Object> userMap = authService.login(account, password);
        return Result.ok(userMap);
    }

    /**
     * 忘记密码 - 重置
     */
    @PostMapping("/forget-password")
    public Result<?> forgetPassword(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String password = body.get("password");
        String smsCode = body.get("smsCode");
        authService.forgetPassword(phone, password, smsCode);
        return Result.ok("密码重置成功");
    }

    /**
     * 修改密码（登录态）
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.ok("密码修改成功");
    }

    /**
     * 账号注销（逻辑注销）
     */
    @PostMapping("/cancel-account")
    public Result<?> cancelAccount() {
        Long userId = (Long) request.getAttribute("userId");
        authService.cancelAccount(userId);
        return Result.ok("账号注销成功");
    }
}
