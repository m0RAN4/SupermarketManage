<template>
  <div class="layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '220px'" class="aside-transition">
        <div class="logo-area" :class="{ 'logo-collapsed': isCollapse }">
          <div class="logo-icon" v-if="isCollapse">
            <svg viewBox="0 0 48 48" fill="none" width="28" height="28">
              <rect x="4" y="8" width="40" height="34" rx="4" stroke="#81c784" stroke-width="2.5" fill="none"/>
              <path d="M4 18h40" stroke="#81c784" stroke-width="2.5"/>
              <circle cx="16" cy="30" r="4" stroke="#81c784" stroke-width="2.5" fill="none"/>
              <circle cx="32" cy="30" r="4" stroke="#81c784" stroke-width="2.5" fill="none"/>
            </svg>
          </div>
          <div v-else class="logo-full">
            <svg viewBox="0 0 48 48" fill="none" width="32" height="32">
              <rect x="4" y="8" width="40" height="34" rx="4" stroke="#81c784" stroke-width="2.5" fill="none"/>
              <path d="M4 18h40" stroke="#81c784" stroke-width="2.5"/>
              <circle cx="16" cy="30" r="4" stroke="#81c784" stroke-width="2.5" fill="none"/>
              <circle cx="32" cy="30" r="4" stroke="#81c784" stroke-width="2.5" fill="none"/>
            </svg>
            <span class="logo-text">超市管理系统</span>
          </div>
        </div>

        <!-- 用户信息区域 -->
        <div class="user-area" v-if="!isCollapse">
          <div class="user-avatar">
            <span>{{ username.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="user-info-text">
            <div class="user-name">{{ username }}</div>
            <div class="user-role">{{ isAdmin ? '管理员' : '普通用户' }}</div>
          </div>
        </div>
        <div class="user-area user-area-collapsed" v-else>
          <div class="user-avatar user-avatar-small">
            <span>{{ username.charAt(0).toUpperCase() }}</span>
          </div>
        </div>

        <div class="menu-divider" :class="{ 'divider-collapsed': isCollapse }"></div>

        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="custom-menu"
          background-color="transparent"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#ffffff"
        >
          <!-- 管理员菜单 -->
          <template v-if="isAdmin">
            <el-menu-item index="/dashboard">
              <el-icon><HomeFilled /></el-icon>
              <template #title>主页</template>
            </el-menu-item>

            <el-sub-menu index="goods">
              <template #title>
                <el-icon><Goods /></el-icon>
                <span>商品管理</span>
              </template>
              <el-menu-item index="/category">商品分类</el-menu-item>
              <el-menu-item index="/product-base">商品档案</el-menu-item>
              <el-menu-item index="/shelf">货架管理</el-menu-item>
              <el-menu-item index="/shelf-on">上架管理</el-menu-item>
            </el-sub-menu>

            <el-menu-item index="/purchase">
              <el-icon><Box /></el-icon>
              <template #title>进货管理</template>
            </el-menu-item>

            <el-sub-menu index="sales">
              <template #title>
                <el-icon><Sell /></el-icon>
                <span>销售管理</span>
              </template>
              <el-menu-item index="/orders">订单查询</el-menu-item>
              <el-menu-item index="/refund-manage">退货管理</el-menu-item>
              <el-menu-item index="/user-feedback">用户反馈</el-menu-item>
            </el-sub-menu>

            <el-menu-item index="/promotion-manage">
              <el-icon><PictureFilled /></el-icon>
              <template #title>宣传管理</template>
            </el-menu-item>

            <el-menu-item index="/loss">
              <el-icon><DeleteFilled /></el-icon>
              <template #title>损耗管理</template>
            </el-menu-item>

            <el-menu-item index="/profit">
              <el-icon><DataLine /></el-icon>
              <template #title>月度盈利</template>
            </el-menu-item>

            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <template #title>个人中心</template>
            </el-menu-item>
          </template>

          <!-- 普通用户菜单 -->
          <template v-else>
            <el-menu-item index="/user-dashboard">
              <el-icon><HomeFilled /></el-icon>
              <template #title>主页</template>
            </el-menu-item>

            <el-menu-item index="/cashier">
              <el-icon><Sell /></el-icon>
              <template #title>收银台</template>
            </el-menu-item>

            <el-menu-item index="/orders">
              <el-icon><Document /></el-icon>
              <template #title>订单查询</template>
            </el-menu-item>

            <el-menu-item index="/user-feedback">
              <el-icon><ChatDotSquare /></el-icon>
              <template #title>用户反馈</template>
            </el-menu-item>

            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <template #title>个人中心</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <!-- 右侧主体 -->
      <el-container>
        <el-header class="top-header">
          <div class="header-left">
            <el-button
              :icon="isCollapse ? Expand : Fold"
              text
              class="collapse-btn"
              @click="isCollapse = !isCollapse"
            />
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-tooltip content="全屏切换" placement="bottom">
              <el-button text circle class="header-btn" @click="toggleFullscreen">
                <el-icon><FullScreen /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="个人中心" placement="bottom">
              <el-button text circle class="header-btn" @click="router.push('/profile')">
                <el-icon><User /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button type="danger" text @click="handleLogout" class="logout-btn">
              <el-icon><SwitchButton /></el-icon>
              <span>退出</span>
            </el-button>
          </div>
        </el-header>

        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import {
  HomeFilled, Goods, Box, Sell, DeleteFilled, DataLine, User,
  Document, ChatDotSquare, Expand, Fold, PictureFilled,
  FullScreen, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapse = ref(false)

const isAdmin = computed(() => authStore.userType === 0)
const username = computed(() => authStore.userInfo?.username || '未登录')

const activeMenu = computed(() => route.path)

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  } else {
    document.exitFullscreen()
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.layout > .el-container {
  height: 100%;
}

/* ========== 侧边栏 ========== */
.aside-transition {
  transition: width 0.3s ease;
  overflow: hidden;
  background: linear-gradient(180deg, #1a2640 0%, #162036 40%, #0f1929 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-full {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  white-space: nowrap;
  letter-spacing: 1px;
}

/* 用户区域 */
.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 12px;
}

.user-area-collapsed {
  justify-content: center;
  padding: 12px 0;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #43a047, #66bb6a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(67, 160, 71, 0.3);
}

.user-avatar-small {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  font-size: 15px;
}

.user-info-text {
  overflow: hidden;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 2px;
}

/* 菜单分隔线 */
.menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
  margin: 0 16px 8px;
}

.divider-collapsed {
  margin: 0 12px 8px;
}

/* ========== 菜单样式 ========== */
.custom-menu {
  border-right: none !important;
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.custom-menu :deep(.el-menu-item) {
  margin: 2px 0;
  border-radius: 10px;
  height: 44px;
  line-height: 44px;
  transition: all 0.2s ease;
}

.custom-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.custom-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(67, 160, 71, 0.3), rgba(102, 187, 106, 0.2)) !important;
  color: #ffffff !important;
  font-weight: 600;
  box-shadow: inset 3px 0 0 #81c784;
}

.custom-menu :deep(.el-sub-menu__title) {
  margin: 2px 0;
  border-radius: 10px;
  height: 44px;
  line-height: 44px;
  transition: all 0.2s ease;
}

.custom-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.custom-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #81c784 !important;
}

/* 子菜单 */
.custom-menu :deep(.el-menu .el-menu-item) {
  padding-left: 56px !important;
  height: 40px;
  line-height: 40px;
  font-size: 13px;
}

.custom-menu :deep(.el-sub-menu .el-menu) {
  background: rgba(0, 0, 0, 0.12) !important;
  border-radius: 0 0 8px 8px;
}

/* 折叠状态 */
.custom-menu.el-menu--collapse :deep(.el-menu-item) {
  justify-content: center;
  padding: 0 !important;
}

/* ========== 顶栏 ========== */
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 24px;
  height: 56px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  color: #606266;
  font-size: 18px;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #43a047;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-btn {
  color: #909399;
  font-size: 16px;
  transition: all 0.2s;
}

.header-btn:hover {
  color: #43a047;
  background: #f0f9eb;
}

.logout-btn {
  margin-left: 8px;
}

.logout-btn:hover {
  background: #fef0f0;
}

/* ========== 主内容区 ========== */
.main-content {
  background: #f0f2f5;
  min-height: calc(100vh - 56px);
  padding: 20px 24px;
}
</style>
