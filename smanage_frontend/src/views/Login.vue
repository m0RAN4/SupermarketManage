<template>
  <div class="login-wrapper">
    <!-- 动态装饰背景 -->
    <div class="bg-layer">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="floating-shape shape-5"></div>
      <div class="floating-shape shape-6"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-container">
      <div class="login-card">
        <div class="card-header">
          <div class="logo-icon">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="8" width="40" height="34" rx="4" stroke="currentColor" stroke-width="2.5" fill="none"/>
              <path d="M4 18h40" stroke="currentColor" stroke-width="2.5"/>
              <circle cx="16" cy="30" r="4" stroke="currentColor" stroke-width="2.5" fill="none"/>
              <circle cx="32" cy="30" r="4" stroke="currentColor" stroke-width="2.5" fill="none"/>
              <path d="M16 26v-3M32 26v-3" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h2 class="login-title">超市管理系统</h2>
          <p class="login-subtitle">Supermarket Management System</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="0"
          size="large"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名 / 手机号"
              :prefix-icon="User"
              class="custom-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              class="custom-input"
              @keyup.enter="handleLogin"
            >
              <template #suffix>
                <el-icon
                  class="password-toggle"
                  @click="showPassword = !showPassword"
                >
                  <View v-if="!showPassword" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-links">
          <el-link type="primary" @click="router.push('/register')">
            还没有账号？去注册
          </el-link>
          <el-link type="primary" @click="router.push('/forget-password')">
            忘记密码？
          </el-link>
        </div>
      </div>

      <p class="footer-text">© 2026 Supermarket Manage · 让超市管理更简单</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名或手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)

    ElMessage.success('登录成功')

    if (authStore.userType === 0) {
      router.push('/dashboard')
    } else {
      router.push('/user-dashboard')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ========== 整体布局 ========== */
.login-wrapper {
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
  right: 25%;
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

/* ========== 登录卡片容器 ========== */
.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-card {
  width: 420px;
  padding: 48px 40px 36px;
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

.login-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

/* ========== 卡片头部 ========== */
.card-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: #81c784;
  animation: logoPulse 3s ease-in-out infinite;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

@keyframes logoPulse {
  0%, 100% { filter: drop-shadow(0 0 8px rgba(129, 199, 132, 0.3)); }
  50% { filter: drop-shadow(0 0 16px rgba(129, 199, 132, 0.6)); }
}

.login-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2px;
}

.login-subtitle {
  margin: 8px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 4px;
  text-transform: uppercase;
}

/* ========== 表单 ========== */
.login-form {
  margin-top: 8px;
}

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

.password-toggle {
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  transition: color 0.2s;
}

.password-toggle:hover {
  color: rgba(255, 255, 255, 0.85);
}

/* ========== 登录按钮 ========== */
.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 8px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border: none !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #43a047, #66bb6a) !important;
  transition: all 0.3s ease !important;
}

.login-btn:hover {
  background: linear-gradient(135deg, #388e3c, #43a047) !important;
  box-shadow: 0 6px 20px rgba(67, 160, 71, 0.4) !important;
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
}

/* ========== 底部链接 ========== */
.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  padding: 0 4px;
}

.login-links :deep(.el-link) {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6) !important;
  transition: color 0.2s;
}

.login-links :deep(.el-link:hover) {
  color: #81c784 !important;
}

/* ========== 页脚 ========== */
.footer-text {
  margin-top: 28px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.25);
  letter-spacing: 1px;
}
</style>
