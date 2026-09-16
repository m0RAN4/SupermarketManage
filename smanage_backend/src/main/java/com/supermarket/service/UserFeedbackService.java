package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.BusinessException;
import com.supermarket.entity.UserFeedback;
import com.supermarket.mapper.UserFeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFeedbackService {

    private final UserFeedbackMapper userFeedbackMapper;

    public Page<UserFeedback> page(Integer pageNum, Integer pageSize, Long userId, Integer status) {
        Page<UserFeedback> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(UserFeedback::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(UserFeedback::getStatus, status);
        }
        wrapper.orderByDesc(UserFeedback::getCreateTime);
        return userFeedbackMapper.selectPage(page, wrapper);
    }

    public void submit(Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("反馈内容不能为空");
        }
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setContent(content.trim());
        feedback.setStatus(0);
        userFeedbackMapper.insert(feedback);
    }

    public void reply(Long feedbackId, String reply) {
        if (reply == null || reply.trim().isEmpty()) {
            throw new BusinessException("回复内容不能为空");
        }
        UserFeedback feedback = userFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException("反馈记录不存在");
        }
        feedback.setReply(reply.trim());
        feedback.setStatus(1);
        userFeedbackMapper.updateById(feedback);
    }

    public UserFeedback getById(Long id) {
        return userFeedbackMapper.selectById(id);
    }
}
