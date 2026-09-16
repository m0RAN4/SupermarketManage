<template>
  <div class="dashboard-container">
    <div class="welcome-section">
      <h1 class="welcome-title">欢迎回来，{{ username }}</h1>
      <p class="welcome-desc">超市管理系统 · 系统概览</p>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :lg="6" v-for="card in statCards" :key="card.title">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-content">
            <div class="stat-icon" :style="{ backgroundColor: card.color }">
              <el-icon :size="28"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ card.value }}</span>
              <span class="stat-label">{{ card.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :sm="12" :lg="6" v-for="link in quickLinks" :key="link.path">
        <el-card class="quick-link-card" shadow="hover" @click="router.push(link.path)">
          <div class="quick-link-content">
            <el-icon :size="32" :color="link.color"><component :is="link.icon" /></el-icon>
            <span class="quick-link-title">{{ link.title }}</span>
            <span class="quick-link-desc">{{ link.desc }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { getDashboardStats } from '../api/product'
import { ElMessage } from 'element-plus'
import {
  Goods, Box, Document, DeleteFilled, DataLine, User, PictureFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const username = computed(() => authStore.userInfo?.username || '管理员')

const statCards = ref([
  { title: '商品总数', value: '-', icon: Goods, color: '#409eff' },
  { title: '今日订单', value: '-', icon: Document, color: '#67c23a' },
  { title: '库存预警', value: '-', icon: DeleteFilled, color: '#e6a23c' },
  { title: '本月盈利', value: '-', icon: DataLine, color: '#f56c6c' }
])

const quickLinks = [
  { title: '商品档案', desc: '管理商品基础档案', path: '/product-base', icon: Goods, color: '#409eff' },
  { title: '进货入库', desc: '商品采购入库管理', path: '/purchase', icon: Box, color: '#67c23a' },
  { title: '订单查询', desc: '查看与管理订单', path: '/orders', icon: Document, color: '#909399' },
  { title: '损耗登记', desc: '商品报损与损耗记录', path: '/loss', icon: DeleteFilled, color: '#f56c6c' },
  { title: '月度盈利', desc: '经营数据统计分析', path: '/profit', icon: DataLine, color: '#00bcd4' },
  { title: '宣传管理', desc: '宣传图片上传与管理', path: '/promotion-manage', icon: PictureFilled, color: '#e040fb' },
  { title: '个人信息', desc: '查看与修改个人资料', path: '/profile', icon: User, color: '#ff9800' }
]

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    if (res.code === 200 && res.data) {
      const d = res.data
      statCards.value[0].value = d.totalProducts != null ? d.totalProducts : 0
      statCards.value[1].value = d.todayOrders != null ? d.todayOrders : 0
      statCards.value[2].value = d.stockWarning != null ? d.stockWarning : 0
      statCards.value[3].value = d.monthlyProfit != null ? '¥' + Number(d.monthlyProfit).toFixed(2) : '¥0.00'
    }
  } catch (e) {
    ElMessage.error('获取仪表盘数据失败')
  }
})
</script>

<style scoped>
.dashboard-container { max-width: 1200px; margin: 0 auto; }
.welcome-section { margin-bottom: 24px; }
.welcome-title { font-size: 24px; font-weight: 600; color: #303133; margin: 0 0 8px 0; }
.welcome-desc { font-size: 14px; color: #909399; margin: 0; }
.stat-card { border-radius: 8px; }
.stat-card-content { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 24px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.quick-link-card { border-radius: 8px; cursor: pointer; transition: transform 0.2s; }
.quick-link-card:hover { transform: translateY(-4px); }
.quick-link-content { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px; padding: 12px 0; }
.quick-link-title { font-size: 15px; font-weight: 600; color: #303133; }
.quick-link-desc { font-size: 12px; color: #909399; }
</style>
