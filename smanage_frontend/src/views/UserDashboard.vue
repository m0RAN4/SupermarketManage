<template>
  <div class="user-dashboard-container">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1 class="welcome-greeting">
            <el-icon class="greeting-icon"><UserFilled /></el-icon>
            欢迎回来，{{ username }}
          </h1>
          <p class="welcome-desc">超市管理系统为您提供便捷的商品选购、订单管理和反馈服务</p>
        </div>
        <div class="welcome-illustration">
          <div class="deco-circle deco-1"></div>
          <div class="deco-circle deco-2"></div>
          <div class="deco-circle deco-3"></div>
        </div>
      </div>
      <div class="banner-stats">
        <div class="stat-item">
          <el-icon class="stat-icon"><Goods /></el-icon>
          <span class="stat-label">便捷选购</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <el-icon class="stat-icon"><Box /></el-icon>
          <span class="stat-label">订单追踪</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <el-icon class="stat-icon"><ChatDotSquare /></el-icon>
          <span class="stat-label">在线反馈</span>
        </div>
      </div>
    </div>

    <!-- 宣传轮播 -->
    <div class="ad-section" v-if="ads.length > 0">
      <el-carousel :interval="5000" arrow="hover" height="280px" class="ad-carousel">
        <el-carousel-item v-for="ad in ads" :key="ad.id">
          <PositionedImage
            v-if="ad.imageUrl"
            :src="getImageUrl(ad.imageUrl)"
            :cropData="ad.cropData"
            :aspectRatio="(16/9)"
            style="height:100%;width:auto;margin:0 auto;border-radius:14px"
          />
          <div v-else class="ad-placeholder">
            <el-icon :size="36"><PictureFilled /></el-icon>
            <span>暂无宣传图片</span>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 快捷入口 -->
    <div class="section-header">
      <h3 class="section-title">快捷操作</h3>
      <span class="section-subtitle">选择您需要的功能</span>
    </div>

    <div class="quick-links-grid">
      <div
        v-for="link in quickLinks"
        :key="link.path"
        class="quick-card"
        @click="router.push(link.path)"
      >
        <div class="quick-card-icon" :style="{ background: link.bg }">
          <el-icon :size="24" :color="link.color"><component :is="link.icon" /></el-icon>
        </div>
        <div class="quick-card-info">
          <span class="quick-card-title">{{ link.title }}</span>
          <span class="quick-card-desc">{{ link.desc }}</span>
        </div>
        <el-icon class="quick-card-arrow" :size="16"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { getActivePromotions } from '../api/product'
import { Sell, Document, ChatDotSquare, User, UserFilled, Goods, Box, PictureFilled, ArrowRight } from '@element-plus/icons-vue'
import PositionedImage from '../components/PositionedImage.vue'

const router = useRouter()
const authStore = useAuthStore()
const username = computed(() => authStore.userInfo?.username || '用户')

const ads = ref([])

const quickLinks = [
  {
    title: '收银台',
    desc: '浏览商品、加入购物车、结算下单',
    path: '/cashier',
    icon: Sell,
    color: '#ffffff',
    bg: 'linear-gradient(135deg, #43a047, #66bb6a)'
  },
  {
    title: '订单查询',
    desc: '查看历史订单、支付、退货',
    path: '/orders',
    icon: Document,
    color: '#ffffff',
    bg: 'linear-gradient(135deg, #1e88e5, #42a5f5)'
  },
  {
    title: '用户反馈',
    desc: '提交意见、建议与问题反馈',
    path: '/user-feedback',
    icon: ChatDotSquare,
    color: '#ffffff',
    bg: 'linear-gradient(135deg, #f57c00, #ff9800)'
  },
  {
    title: '个人信息',
    desc: '查看资料、修改密码、账号管理',
    path: '/profile',
    icon: User,
    color: '#ffffff',
    bg: 'linear-gradient(135deg, #8e24aa, #ab47bc)'
  }
]

function getImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api/picture/')) return url
  return '/api/picture/' + url
}

onMounted(async () => {
  try {
    const res = await getActivePromotions()
    if (res.code === 200 && res.data) {
      ads.value = res.data
    }
  } catch {
    // 静默失败
  }
})
</script>

<style scoped>
.user-dashboard-container {
  max-width: 1100px;
  margin: 0 auto;
}

/* ========== 欢迎横幅 ========== */
.welcome-banner {
  background: linear-gradient(135deg, #1a3a5c 0%, #1a5c4a 100%);
  border-radius: 16px;
  padding: 28px 32px 20px;
  margin-bottom: 24px;
  overflow: hidden;
  position: relative;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.welcome-text {
  position: relative;
  z-index: 1;
}

.welcome-greeting {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.greeting-icon {
  color: rgba(255, 255, 255, 0.8);
}

.welcome-desc {
  margin: 8px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.welcome-illustration {
  flex-shrink: 0;
  width: 100px;
  height: 80px;
  position: relative;
  z-index: 1;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.12);
}

.deco-1 {
  width: 70px;
  height: 70px;
  top: 5px;
  right: 0;
}

.deco-2 {
  width: 45px;
  height: 45px;
  top: 18px;
  right: 35px;
  border-color: rgba(129, 199, 132, 0.25);
}

.deco-3 {
  width: 20px;
  height: 20px;
  top: 30px;
  right: 12px;
  background: rgba(129, 199, 132, 0.2);
  border-color: rgba(129, 199, 132, 0.3);
}

.banner-stats {
  display: flex;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  gap: 24px;
  position: relative;
  z-index: 1;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stat-icon {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}

.stat-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.15);
}

/* ========== 轮播 ========== */
.ad-section {
  margin-bottom: 28px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.ad-carousel :deep(.el-carousel__container) {
  border-radius: 14px;
}

.ad-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  gap: 12px;
  border-radius: 14px;
}

/* ========== 快捷入口标题 ========== */
.section-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

.section-subtitle {
  font-size: 13px;
  color: #909399;
}

/* ========== 快捷卡片 ========== */
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #ffffff;
  padding: 20px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.quick-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: transparent;
}

.quick-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.quick-card-info {
  flex: 1;
  min-width: 0;
}

.quick-card-title {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.quick-card-desc {
  display: block;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quick-card-arrow {
  color: #c0c4cc;
  flex-shrink: 0;
  transition: all 0.2s;
}

.quick-card:hover .quick-card-arrow {
  color: #43a047;
  transform: translateX(4px);
}
</style>
