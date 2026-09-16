<template>
  <div class="profit-stat-container">
    <h2 class="page-title">月度盈利统计</h2>

    <!-- Month selector -->
    <div class="search-bar">
      <el-date-picker
        v-model="selectedMonth"
        type="month"
        placeholder="选择月份"
        value-format="YYYY-MM"
        class="month-picker"
      />
      <el-button type="primary" @click="fetchStats" :loading="loading">
        <el-icon><Search /></el-icon> 查询
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card green">
        <div class="stat-icon"><el-icon :size="22"><Sell /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">销售收入</span>
          <span class="stat-value">¥{{ stats.monthlySalesRevenue.toFixed(2) }}</span>
        </div>
      </div>
      <div class="stat-card orange">
        <div class="stat-icon"><el-icon :size="22"><Refrigerator /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">退款金额</span>
          <span class="stat-value">¥{{ stats.monthlyRefundAmount.toFixed(2) }}</span>
        </div>
      </div>
      <div class="stat-card blue">
        <div class="stat-icon"><el-icon :size="22"><Wallet /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">实际收入</span>
          <span class="stat-value">¥{{ stats.monthlyActualRevenue.toFixed(2) }}</span>
        </div>
      </div>
      <div class="stat-card purple">
        <div class="stat-icon"><el-icon :size="22"><Box /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">进货总额</span>
          <span class="stat-value">¥{{ stats.monthlyPurchaseAmount.toFixed(2) }}</span>
        </div>
      </div>
      <div class="stat-card cyan">
        <div class="stat-icon"><el-icon :size="22"><Goods /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">销售成本</span>
          <span class="stat-value">¥{{ stats.monthlySoldCost.toFixed(2) }}</span>
        </div>
      </div>
      <div class="stat-card red">
        <div class="stat-icon"><el-icon :size="22"><DeleteFilled /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">损耗成本</span>
          <span class="stat-value">¥{{ stats.monthlyLossCost.toFixed(2) }}</span>
        </div>
      </div>
      <div class="stat-card red-light">
        <div class="stat-icon"><el-icon :size="22"><WarningFilled /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">损耗金额 / 占比</span>
          <span class="stat-value">
            ¥{{ stats.monthlyLossAmount.toFixed(2) }}
            <span class="stat-sub">（{{ (stats.lossRatio ?? 0).toFixed(2) }}%）</span>
          </span>
        </div>
      </div>
      <div class="stat-card gold">
        <div class="stat-icon"><el-icon :size="22"><DataLine /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">净利润</span>
          <span class="stat-value" :class="stats.monthlyNetProfit >= 0 ? 'text-green' : 'text-red'">
            ¥{{ stats.monthlyNetProfit.toFixed(2) }}
          </span>
        </div>
      </div>
      <div class="stat-card teal">
        <div class="stat-icon"><el-icon :size="22"><RefreshLeft /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">退货回库成本</span>
          <span class="stat-value">¥{{ (stats.monthlyReturnCost - stats.monthlyReturnLossCost).toFixed(2) }}</span>
        </div>
      </div>
      <div class="stat-card brown">
        <div class="stat-icon"><el-icon :size="22"><SwitchButton /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">退货转损耗成本</span>
          <span class="stat-value">¥{{ stats.monthlyReturnLossCost.toFixed(2) }}</span>
        </div>
      </div>
    </div>

    <!-- 利润明细 -->
    <div class="detail-card">
      <h3 class="detail-title">利润明细</h3>
      <el-empty v-if="!loading && !hasData" description="暂无统计数据，请选择月份后查询" />
      <div v-else class="detail-list">
        <div class="detail-row">
          <span class="detail-label">实际收入</span>
          <span class="detail-value">¥{{ stats.monthlyActualRevenue.toFixed(2) }}</span>
          <span class="detail-hint">（销售收入 ¥{{ stats.monthlySalesRevenue.toFixed(2) }} − 退款 ¥{{ stats.monthlyRefundAmount.toFixed(2) }}）</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">销售成本</span>
          <span class="detail-value text-orange">− ¥{{ stats.monthlySoldCost.toFixed(2) }}</span>
          <span class="detail-hint">已售出商品进货成本（不含退货）</span>
        </div>

        <div class="detail-row">
          <span class="detail-label">损耗成本</span>
          <span class="detail-value text-red">− ¥{{ stats.monthlyLossCost.toFixed(2) }}</span>
          <span class="detail-hint">损耗商品进货成本（含退货转损耗 ¥{{ stats.monthlyReturnLossCost.toFixed(2) }}）</span>
        </div>
        <div class="detail-row detail-divider"></div>
        <div class="detail-row total">
          <span class="detail-label">净利润</span>
          <span class="detail-value" :class="stats.monthlyNetProfit >= 0 ? 'text-green' : 'text-red'">
            ¥{{ stats.monthlyNetProfit.toFixed(2) }}
          </span>
          <span class="detail-hint">实际收入 − 销售成本 − 损耗成本</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, SwitchButton } from '@element-plus/icons-vue'
import { getMonthlyProfit } from '../api/order'

const loading = ref(false)
// Default to current month
const now = new Date()
const selectedMonth = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)

const stats = reactive({
  monthlySalesRevenue: 0,
  monthlyRefundAmount: 0,
  monthlyActualRevenue: 0,
  monthlyPurchaseAmount: 0,
  monthlySoldCost: 0,
  monthlyReturnCost: 0,
  monthlyReturnLossCost: 0,
  monthlyLossCost: 0,
  monthlyLossAmount: 0,
  lossRatio: 0,
  monthlyNetProfit: 0
})

const hasData = ref(false)

const fetchStats = async () => {
  if (!selectedMonth.value) {
    ElMessage.warning('请选择查询月份')
    return
  }

  const [year, month] = selectedMonth.value.split('-')

  loading.value = true
  try {
    const res = await getMonthlyProfit({
      year: parseInt(year),
      month: parseInt(month)
    })

    if (res.code === 200) {
      const data = res.data || {}
      Object.assign(stats, {
        monthlySalesRevenue: data.monthlySalesRevenue || 0,
        monthlyRefundAmount: data.monthlyRefundAmount || 0,
        monthlyActualRevenue: data.monthlyActualRevenue ||
          (data.monthlySalesRevenue || 0) - (data.monthlyRefundAmount || 0),
        monthlyPurchaseAmount: data.monthlyPurchaseAmount || 0,
        monthlySoldCost: data.monthlySoldCost || 0,
        monthlyReturnCost: data.monthlyReturnCost || 0,
        monthlyReturnLossCost: data.monthlyReturnLossCost || 0,
        monthlyLossCost: data.monthlyLossCost || 0,
        monthlyLossAmount: data.monthlyLossAmount || 0,
        lossRatio: data.lossRatio ?? 0,
        monthlyNetProfit: data.monthlyNetProfit || 0
      })
      hasData.value = true
    } else {
      ElMessage.error(res.message || '查询失败')
      hasData.value = false
    }
  } catch (err) {
    ElMessage.error(err?.message || '查询利润统计失败')
    hasData.value = false
  } finally {
    loading.value = false
  }
}

// Auto-load current month data on mount
onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.profit-stat-container { max-width: 1000px; margin: 0 auto; }

.page-title { margin-bottom: 16px; font-size: 20px; font-weight: 700; color: #1a1a2e; }

/* 搜索栏 */
.search-bar {
  display: flex; align-items: center; gap: 12px;
  background: #fff; padding: 16px 20px; border-radius: 14px;
  margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); border: 1px solid #f0f0f0;
}
.month-picker { width: 200px; }

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
  border-left: 4px solid #dcdfe6;
}

.stat-card.green { border-left-color: #67c23a; }
.stat-card.orange { border-left-color: #e6a23c; }
.stat-card.blue { border-left-color: #409eff; }
.stat-card.purple { border-left-color: #9b59b6; }
.stat-card.cyan { border-left-color: #00bcd4; }
.stat-card.red { border-left-color: #f56c6c; }
.stat-card.red-light { border-left-color: #e6a23c; }
.stat-card.gold { border-left-color: #f0ad4e; }
.stat-card.teal { border-left-color: #008080; }
.stat-card.brown { border-left-color: #8b4513; }

.stat-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  background: #f5f7fa;
  display: flex; align-items: center; justify-content: center;
  color: #909399; flex-shrink: 0;
}

.stat-body { flex: 1; min-width: 0; }

.stat-label {
  display: block; font-size: 12px; color: #909399; margin-bottom: 4px;
}

.stat-value {
  display: block; font-size: 18px; font-weight: 700; color: #303133;
}

.stat-sub { font-size: 12px; font-weight: 400; color: #909399; }

.text-green { color: #67c23a !important; }
.text-red { color: #f56c6c !important; }
.text-orange { color: #e6a23c !important; }

/* 明细 */
.detail-card {
  background: #fff; border-radius: 14px; padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); border: 1px solid #f0f0f0;
}

.detail-title { margin: 0 0 16px; font-size: 16px; font-weight: 600; color: #1a1a2e; }

.detail-row {
  display: flex; align-items: center;
  padding: 10px 0; border-bottom: 1px solid #f5f5f5;
}

.detail-row:last-child { border-bottom: none; }

.detail-section-title {
  font-size: 13px; font-weight: 600; color: #909399;
  padding: 8px 0 4px 0; border-bottom: 1px dashed #e8e8e8; margin-bottom: 4px;
}

.sub-row { padding-left: 16px; }
.sub-row .detail-label { font-size: 13px; color: #909399; }

.detail-label { width: 80px; font-size: 14px; color: #606266; flex-shrink: 0; }

.detail-value { font-size: 16px; font-weight: 700; color: #303133; margin-right: 12px; }

.detail-hint { font-size: 12px; color: #909399; }

.detail-divider { border-bottom: 2px dashed #ebeef5; margin: 4px 0; }

.detail-row.total .detail-label { font-size: 16px; font-weight: 700; }
.detail-row.total .detail-value { font-size: 22px; }

.text-blue { color: #409eff !important; }
</style>
