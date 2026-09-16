<template>
  <div class="forget-wrapper">
    <!-- 动态装饰背景 -->
    <div class="bg-layer">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="floating-shape shape-5"></div>
      <div class="floating-shape shape-6"></div>
    </div>

    <!-- 忘记密码卡片 -->
    <div class="forget-container">
      <div class="forget-card">
        <div class="card-header">
          <h2 class="forget-title">重置密码</h2>
          <p class="forget-subtitle">RESET YOUR PASSWORD</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="0"
          size="default"
          class="forget-form"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              :prefix-icon="Phone"
              maxlength="11"
              class="custom-input"
            />
          </el-form-item>

          <el-form-item prop="smsCode">
            <el-input
              v-model="form.smsCode"
              placeholder="请输入短信验证码"
              :prefix-icon="Message"
              class="custom-input"
            />
            <div class="sms-actions">
              <el-button
                :disabled="countdown > 0"
                class="sms-btn"
                @click="handleSendSms"
              >
                {{ countdown > 0 ? countdown + 's后重发' : '获取验证码' }}
              </el-button>
              <el-button class="sms-btn" @click="handleViewSms">
                查看验证码
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入新密码（至少6位）"
              :prefix-icon="Lock"
              show-password
              class="custom-input"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              :prefix-icon="Lock"
              show-password
              class="custom-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="forget-btn"
              @click="handleSubmit"
            >
              提 交
            </el-button>
          </el-form-item>
        </el-form>

        <div class="forget-link">
          <span>记起密码了？</span>
          <el-link type="primary" @click="router.push('/login')">去登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Phone, Lock, Message } from '@element-plus/icons-vue'
import { forgetPassword, sendSms, viewSmsCode } from '../api/auth'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function handleSendSms() {
  if (!form.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    await sendSms(form.phone)
    ElMessage.success('验证码已发送，5分钟内有效')
    startCountdown()
  } catch (error) {
    ElMessage.error(error.message || '发送验证码失败')
  }
}

async function handleViewSms() {
  if (!form.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    const res = await viewSmsCode(form.phone)
    ElMessageBox.alert(res.data || res, '验证码', {
      confirmButtonText: '知道了'
    })
  } catch (error) {
    ElMessage.error(error.message || '获取验证码失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await forgetPassword({
      phone: form.phone,
      password: form.password,
      smsCode: form.smsCode
    })

    ElMessage.success('密码重置成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '密码重置失败')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
/* ========== 整体布局 ========== */
.forget-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #0c1929 0%, #1a3a5c 30%, #1a5c4a 70%, #0c1929 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
}

/* ========== 浮动装饰形状 ========== */
.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  filter: blur(1px);
}

.shape-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #4fc3f7, transparent 70%);
  top: -200px;
  right: -150px;
  animation: float1 18s ease-in-out infinite;
}

.shape-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #81c784, transparent 70%);
  bottom: -180px;
  left: -120px;
  animation: float2 20s ease-in-out infinite;
}

.shape-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #64b5f6, transparent 70%);
  top: 40%;
  left: 5%;
  animation: float3 14s ease-in-out infinite;
}

.shape-4 {
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, #a5d6a7, transparent 70%);
  top: 10%;
  left: 55%;
  animation: float4 16s ease-in-out infinite;
}

.shape-5 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #ce93d8, transparent 70%);
  bottom: 30%;
  right: 10%;
  animation: float2 13s ease-in-out infinite;
}

.shape-6 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, #90caf9, transparent 70%);
  top: 55%;
  right: 22%;
  animation: float1 11s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-35px, -20px) scale(1.08); }
  66% { transform: translate(25px, 30px) scale(0.93); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(0, -40px) scale(1.1); }
}

@keyframes float4 {
  0%, 100% { transform: translate(0, 0) scale(1) rotate(0deg); }
  50% { transform: translate(30px, 25px) scale(1.06) rotate(10deg); }
}

/* ========== 卡片容器 ========== */
.forget-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.forget-card {
  width: 460px;
  padding: 40px 36px 32px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.forget-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

/* ========== 卡片头部 ========== */
.card-header {
  text-align: center;
  margin-bottom: 28px;
}

.forget-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2px;
}

.forget-subtitle {
  margin: 6px 0 0;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 4px;
  text-transform: uppercase;
}

/* ========== 表单 ========== */
.custom-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  border-radius: 12px !important;
  box-shadow: none !important;
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.12) !important;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #81c784 !important;
  background: rgba(255, 255, 255, 0.12) !important;
  box-shadow: 0 0 0 3px rgba(129, 199, 132, 0.15) !important;
}

.custom-input :deep(.el-input__inner) {
  color: #ffffff !important;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

.custom-input :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.5) !important;
}

.custom-input :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.5) !important;
}

/* ========== 短信按钮 ========== */
.sms-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.sms-btn {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  color: rgba(255, 255, 255, 0.7) !important;
  border-radius: 10px !important;
  transition: all 0.2s;
}

.sms-btn:hover {
  background: rgba(255, 255, 255, 0.14) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
}

.sms-btn.is-disabled {
  opacity: 0.4 !important;
}

/* ========== 提交按钮 ========== */
.forget-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border: none !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #43a047, #66bb6a) !important;
  transition: all 0.3s ease !important;
}

.forget-btn:hover {
  background: linear-gradient(135deg, #388e3c, #43a047) !important;
  box-shadow: 0 6px 20px rgba(67, 160, 71, 0.4) !important;
  transform: translateY(-1px);
}

.forget-btn:active {
  transform: translateY(0);
}

/* ========== 底部链接 ========== */
.forget-link {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  margin-top: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.forget-link :deep(.el-link) {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6) !important;
  transition: color 0.2s;
}

.forget-link :deep(.el-link:hover) {
  color: #81c784 !important;
}
</style>
