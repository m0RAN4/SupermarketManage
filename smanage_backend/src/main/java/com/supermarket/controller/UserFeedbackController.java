package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import com.supermarket.service.UserFeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class UserFeedbackController {

    private final UserFeedbackService userFeedbackService;

    @Autowired
    private HttpServletRequest request;

    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    /** 分页查询反馈（管理员看全部，普通用户看自己的） */
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Integer userType = (Integer) request.getAttribute("userType");
        Long userId = (Long) request.getAttribute("userId");
        Long filterUserId = (userType != null && userType == 1) ? userId : null;
        return Result.ok(userFeedbackService.page(pageNum, pageSize, filterUserId, status));
    }

    /** 提交反馈（所有登录用户） */
    @PostMapping
    public Result<?> submit(@RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String content = body.get("content") != null ? body.get("content").toString() : null;
        userFeedbackService.submit(userId, content);
        return Result.ok("反馈已提交");
    }

    /** 管理员回复反馈 */
    @PostMapping("/reply")
    public Result<?> reply(@RequestBody Map<String, Object> body) {
        checkAdmin();
        Long feedbackId = Long.valueOf(body.get("feedbackId").toString());
        String reply = body.get("reply") != null ? body.get("reply").toString() : null;
        userFeedbackService.reply(feedbackId, reply);
        return Result.ok("回复成功");
    }
}
