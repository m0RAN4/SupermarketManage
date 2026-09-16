<template>
  <div class="profile-container">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="page-title-icon"><User /></el-icon>
        个人中心
      </h2>
      <p class="page-desc">管理您的账户信息和密码</p>
    </div>

    <div class="profile-content">
      <!-- 左侧：头像和基本信息 -->
      <div class="profile-sidebar">
        <div class="avatar-section">
          <div class="avatar-circle">
            <span>{{ (userInfo.username || 'U').charAt(0).toUpperCase() }}</span>
          </div>
          <h3 class="avatar-name">{{ userInfo.username }}</h3>
          <el-tag :type="userInfo.userType === 0 ? 'danger' : 'success'" effect="light" round size="small">
            {{ userInfo.userType === 0 ? '管理员' : '普通用户' }}
          </el-tag>
        </div>
        <div class="profile-meta">
          <div class="meta-item">
            <el-icon><Phone /></el-icon>
            <span>{{ userInfo.phone || '-' }}</span>
          </div>
          <div class="meta-item">
            <el-icon><Calendar /></el-icon>
            <span>{{ formatTime(userInfo.createTime) || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：功能区域 -->
      <div class="profile-main">
        <!-- 基本信息 -->
        <div class="info-section">
          <h3 class="section-title">基本信息</h3>
          <div class="info-rows">
            <div class="info-row">
              <span class="row-label">用户名</span>
              <span class="row-value">{{ userInfo.username }}</span>
            </div>
            <div class="info-row">
              <span class="row-label">手机号</span>
              <span class="row-value">{{ userInfo.phone }}</span>
            </div>
            <div class="info-row">
              <span class="row-label">角色</span>
              <span class="row-value">
                <el-tag :type="userInfo.userType === 0 ? 'danger' : 'success'" effect="plain" size="small" round>
                  {{ userInfo.userType === 0 ? '管理员' : '普通用户' }}
                </el-tag>
              </span>
            </div>
            <div class="info-row">
              <span class="row-label">注册时间</span>
              <span class="row-value">{{ formatTime(userInfo.createTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 修改密码 -->
        <div class="info-section">
          <h3 class="section-title">修改密码</h3>
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="0"
            size="default"
            class="password-form"
          >
            <el-form-item prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入旧密码"
                show-password
                :prefix-icon="Lock"
                class="custom-input"
              />
            </el-form-item>

            <el-form-item prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
                :prefix-icon="Lock"
                class="custom-input"
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请确认新密码"
                show-password
                :prefix-icon="Lock"
                class="custom-input"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword" class="password-btn">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 危险操作 -->
        <div class="info-section danger-section">
          <h3 class="section-title danger-title">账号操作</h3>
          <p class="danger-desc">注销后账号将无法登录，原手机号可重新注册</p>
          <el-button type="danger" @click="handleCancelAccount" class="cancel-btn">
            注销账号
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Phone, Calendar, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { changePassword, cancelAccount } from '../api/auth'

const router = useRouter()
const authStore = useAuthStore()

const passwordFormRef = ref(null)
const passwordLoading = ref(false)

const userInfo = reactive({
  username: '',
  phone: '',
  userType: null,
  createTime: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

function fetchUserInfo() {
  userInfo.username = authStore.userInfo?.username || '-'
  userInfo.phone = authStore.userInfo?.phone || '-'
  userInfo.userType = authStore.userInfo?.userType
  userInfo.createTime = authStore.userInfo?.createTime || null
}

function formatTime(time) {
  if (!time) return '-'
  return String(time).replace('T', ' ')
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    authStore.logout()
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

async function handleCancelAccount() {
  try {
    await ElMessageBox.confirm(
      '注销后账号将无法登录，原手机号可重新注册，确认注销吗？',
      '二次确认',
      { type: 'warning', confirmButtonText: '确认注销', cancelButtonText: '取消' }
    )
    await cancelAccount()
    ElMessage.success('账号已注销')
    authStore.logout()
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '注销失败')
    }
  }
}

onMounted(() => { fetchUserInfo() })
</script>

<style scoped>
.profile-container { max-width: 860px; margin: 0 auto; }

.page-header { margin-bottom: 20px; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #1a1a2e; display: flex; align-items: center; gap: 8px; }
.page-title-icon { color: #8e24aa; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #909399; }

.profile-content { display: flex; gap: 20px; }

/* 左侧头像区 */
.profile-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #1a3a5c 0%, #1a5c4a 100%);
  border-radius: 16px;
  padding: 28px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #43a047, #81c784);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(67, 160, 71, 0.4);
  margin-bottom: 12px;
}

.avatar-name {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.profile-meta {
  margin-top: 20px;
  width: 100%;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.65);
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.meta-item:last-child { border-bottom: none; }

/* 右侧主区域 */
.profile-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-section {
  background: #ffffff;
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  padding-bottom: 12px;
  border-bottom: 2px solid #f5f5f5;
}

.info-rows { display: flex; flex-direction: column; gap: 12px; }

.info-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.row-label {
  width: 80px;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.row-value { font-size: 14px; color: #303133; font-weight: 500; }

/* 密码表单 */
.password-form { max-width: 380px; }

.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.password-btn {
  border-radius: 12px;
  font-weight: 600;
  padding: 10px 28px;
}

/* 危险区域 */
.danger-section {
  border: 1px solid #fde2e2;
  background: #fefafa;
}

.danger-title { color: #f56c6c; border-bottom-color: #fde2e2; }
.danger-desc { font-size: 13px; color: #909399; margin: 0 0 12px; }

.cancel-btn { border-radius: 12px; font-weight: 600; }
</style>
