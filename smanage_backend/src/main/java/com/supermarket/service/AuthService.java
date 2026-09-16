package com.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supermarket.common.BusinessException;
import com.supermarket.common.JwtUtil;
import com.supermarket.entity.AdminRegisterCode;
import com.supermarket.entity.SmsCode;
import com.supermarket.entity.UserInfo;
import com.supermarket.mapper.AdminRegisterCodeMapper;
import com.supermarket.mapper.SmsCodeMapper;
import com.supermarket.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserInfoMapper userInfoMapper;
    private final SmsCodeMapper smsCodeMapper;
    private final AdminRegisterCodeMapper adminRegisterCodeMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String PHONE_REGEX = "^1[3-9][0-9]{9}$";

    @Transactional(rollbackFor = Exception.class)
    public void sendSmsCode(String phone) {
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确，需为11位纯数字");
        }
        smsCodeMapper.invalidateOldCodes(phone);
        String code = String.format("%06d", new Random().nextInt(1000000));
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(5);
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setExpireTime(expireTime);
        smsCode.setIsUsed(0);
        smsCodeMapper.insert(smsCode);
    }

    public String viewSmsCode(String phone) {
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确");
        }
        SmsCode smsCode = smsCodeMapper.selectLatestValidByPhone(phone);
        if (smsCode == null) {
            throw new BusinessException("暂无有效验证码");
        }
        return smsCode.getCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void registerUser(String username, String phone, String password, String smsCode) {
        validateRegisterInput(username, phone, password, smsCode);
        checkPhoneAvailable(phone);
        checkUsernameAvailable(username);
        validateSmsCode(phone, smsCode);
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setUserType(1);
        userInfoMapper.insert(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void registerAdmin(String username, String phone, String password, String smsCode, String adminCode) {
        validateRegisterInput(username, phone, password, smsCode);
        checkPhoneAvailable(phone);
        checkUsernameAvailable(username);
        validateSmsCode(phone, smsCode);
        AdminRegisterCode code = adminRegisterCodeMapper.selectValidByCode(adminCode);
        if (code == null) {
            throw new BusinessException("管理员校验码不存在、已失效或已用完");
        }
        code.setUsedTimes(code.getUsedTimes() + 1);
        if (code.getUsedTimes() >= code.getTotalUsableTimes()) {
            code.setIsValid(0);
        }
        adminRegisterCodeMapper.updateById(code);
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setUserType(0);
        userInfoMapper.insert(user);
    }

    public Map<String, Object> login(String account, String password) {
        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<>();
        query.and(w ->
            w.eq(UserInfo::getUsername, account)
             .or()
             .eq(UserInfo::getPhone, account)
        );
        query.notLike(UserInfo::getPhone, "%-%");
        UserInfo user = userInfoMapper.selectOne(query);
        if (user == null) {
            throw new BusinessException("账号不存在或已注销");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("userType", user.getUserType());
        result.put("phone", user.getPhone());
        result.put("createTime", user.getCreateTime() != null ? user.getCreateTime().toString() : null);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void forgetPassword(String phone, String password, String smsCode) {
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确");
        }
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getPhone, phone);
        UserInfo user = userInfoMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }
        validateSmsCode(phone, smsCode);
        user.setPassword(passwordEncoder.encode(password));
        userInfoMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userInfoMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelAccount(Long userId) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String phone = user.getPhone();
        if (phone.contains("-")) {
            throw new BusinessException("该账号已注销");
        }
        String prefix = phone;
        Integer maxSuffix = userInfoMapper.selectMaxSuffixByPhonePrefix(prefix);
        int newSuffix = (maxSuffix == null ? 0 : maxSuffix) + 1;
        String newPhone = prefix + "-" + newSuffix;
        user.setPhone(newPhone);
        userInfoMapper.updateById(user);
    }

    private void validateRegisterInput(String username, String phone, String password, String smsCode) {
        if (username == null || username.trim().isEmpty()) throw new BusinessException("用户名不能为空");
        if (phone == null || !phone.matches(PHONE_REGEX)) throw new BusinessException("手机号格式不正确，需为11位纯数字");
        if (password == null || password.length() < 6) throw new BusinessException("密码长度不能少于6位");
        if (smsCode == null || smsCode.trim().isEmpty()) throw new BusinessException("验证码不能为空");
    }

    private void checkPhoneAvailable(String phone) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getPhone, phone);
        if (userInfoMapper.selectCount(wrapper) > 0) throw new BusinessException("该手机号已被注册");
    }

    private void checkUsernameAvailable(String username) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUsername, username);
        if (userInfoMapper.selectCount(wrapper) > 0) throw new BusinessException("该用户名已被使用");
    }

    private void validateSmsCode(String phone, String code) {
        SmsCode smsCode = smsCodeMapper.selectLatestValidByPhone(phone);
        if (smsCode == null) throw new BusinessException("验证码不存在或已过期");
        if (!smsCode.getCode().equals(code)) throw new BusinessException("验证码错误");
        smsCode.setIsUsed(1);
        smsCodeMapper.updateById(smsCode);
    }
}
