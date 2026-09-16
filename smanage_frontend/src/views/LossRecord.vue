<template>
  <div class="loss-record-container">
    <h2 class="page-title">损耗管理</h2>

    <!-- Monthly stats cards with month selector -->
    <el-card class="stats-header-card" shadow="never">
      <div class="stats-header-row">
        <div class="stats-header-left">
          <span class="stats-title">月度损耗概况</span>
          <el-date-picker
            v-model="statsSelectedMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            class="stats-month-picker"
            @change="onStatsMonthChange"
          />
        </div>
        <div class="stats-header-right">
          <el-button type="primary" size="small" @click="fetchMonthlyStats" :loading="statsLoading">
            <el-icon><Search /></el-icon> 查询
          </el-button>
        </div>
      </div>
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="stat-card">
            <div class="stat-label">月损耗金额</div>
            <div class="stat-value loss">¥{{ monthlyStats.totalLossAmount.toFixed(2) }}</div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="stat-card">
            <div class="stat-label">月损耗笔数</div>
            <div class="stat-value">{{ monthlyStats.lossCount }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- Search bar -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select
            v-model="searchForm.baseProductId"
            placeholder="选择商品"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="p.productName"
              :value="p.id"
            />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-col>
        <el-col :span="4" style="text-align: right">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">新增损耗</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Table -->
    <el-card class="table-card" shadow="never" style="margin-top: 16px">
      <el-table :data="tableData" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.productName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="shelfDisplayName" label="展示名称" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.shelfDisplayName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="lossReason" label="损耗原因" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.lossReason || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="lossQuantity" label="损耗数量" width="100" align="center" />
        <el-table-column prop="lossUnitPrice" label="损耗单价" width="110" align="center">
          <template #default="{ row }">
            {{ row.lossUnitPrice != null ? '¥' + row.lossUnitPrice : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="lossTotalAmount" label="损耗总金额" width="120" align="center">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold">
              {{ row.lossTotalAmount != null ? '¥' + row.lossTotalAmount : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Add loss dialog -->
    <el-dialog
      v-model="addDialogVisible"
      title="新增损耗记录"
      width="950px"
      :close-on-click-modal="false"
      @close="resetAddForm"
    >
      <!-- Step 1: Select product -->
      <el-form :model="addForm" label-width="80px" style="margin-bottom: 16px">
        <el-form-item label="选择商品" required>
          <el-select
            v-model="addForm.baseProductId"
            placeholder="请选择商品"
            filterable
            style="width: 400px"
            @change="handleProductChange"
          >
            <el-option
              v-for="p in enabledProductOptions"
              :key="p.id"
              :label="p.productName"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- Step 2: Batch selection table (appears after product selected) -->
      <div v-if="batchDetails.length > 0" class="batch-select-area">
        <el-alert type="info" :closable="false" style="margin-bottom: 12px">
          已选择 <strong>{{ selectedBatchIds.length }}</strong> 个批次，损耗总金额：
          <strong style="color: #f56c6c">¥{{ selectedLossTotal.toFixed(2) }}</strong>
        </el-alert>

        <el-table
          ref="batchTableRef"
          :data="batchDetails"
          border
          stripe
          size="small"
          max-height="350"
          style="width: 100%"
          @selection-change="onBatchSelectionChange"
        >
          <el-table-column type="selection" width="45" :selectable="checkSelectable" />
          <el-table-column prop="id" label="批次ID" width="80" align="center" />
          <el-table-column prop="purchaseRecordId" label="进货编号" width="100" align="center" />
          <el-table-column prop="shelfCode" label="货架" width="90" align="center">
            <template #default="{ row }">{{ row.shelfCode || '-' }}</template>
          </el-table-column>
          <el-table-column label="层数" width="60" align="center">
            <template #default="{ row }">{{ row.shelfLayer ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="shelfDisplayName" label="展示名称" min-width="120">
            <template #default="{ row }">{{ row.shelfDisplayName || '-' }}</template>
          </el-table-column>
          <el-table-column label="成本价" width="90" align="center">
            <template #default="{ row }">¥{{ (row.purchaseUnitPrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="售价" width="90" align="center">
            <template #default="{ row }">¥{{ (row.salePrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="折扣价" width="90" align="center">
            <template #default="{ row }">{{ row.discountPrice ? '¥' + Number(row.discountPrice).toFixed(2) : '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160" align="center">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else-if="addForm.baseProductId && batchLoading" style="text-align: center; padding: 20px; color: #909399">
        加载批次信息中...
      </div>
      <div v-else-if="addForm.baseProductId && !batchLoading" style="text-align: center; padding: 20px; color: #909399">
        该商品暂无可用批次
      </div>

      <!-- Step 3: Loss reason -->
      <el-form :model="addForm" label-width="80px" style="margin-top: 16px">
        <el-form-item label="损耗原因" required>
          <el-input
            v-model="addForm.lossReason"
            type="textarea"
            :rows="3"
            placeholder="请输入损耗原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleAddConfirm"
          :loading="adding"
          :disabled="selectedBatchIds.length === 0"
        >
          确认登记（{{ selectedBatchIds.length }} 件）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getLossPage, addLoss, getLossMonthlyStats } from '../api/order'
import { getProductBaseList, getProductStockDetail } from '../api/product'

const loading = ref(false)
const adding = ref(false)
const batchLoading = ref(false)
const statsLoading = ref(false)
const tableData = ref([])
const productOptions = ref([])

// Stats month picker - defaults to current month
const now = new Date()
const statsSelectedMonth = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)

// Batch selection state
const batchDetails = ref([])
const selectedBatches = ref([])

const searchForm = reactive({
  baseProductId: null,
  dateRange: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const addDialogVisible = ref(false)
const addForm = reactive({
  baseProductId: null,
  lossReason: ''
})

const selectedBatchIds = computed(() => selectedBatches.value.map(b => b.id))

const selectedLossTotal = computed(() => {
  return selectedBatches.value.reduce((sum, b) => sum + (Number(b.purchaseUnitPrice) || 0), 0)
})

// Status helpers
const statusType = (s) => ({ 0: 'info', 1: 'success', 2: '', 3: 'danger', 4: 'warning', 5: 'info' }[s] || 'info')
const statusText = (s) => ({ 0: '在库未上架', 1: '已上架在售', 2: '已售出', 3: '已损耗', 4: '退货待处理', 5: '待支付锁定' }[s] || '未知')

// Only enabled products for the add dialog
const enabledProductOptions = computed(() => {
  return productOptions.value.filter(p => p.isEnabled === 1)
})

// Monthly stats
const monthlyStats = reactive({
  totalLossAmount: 0,
  lossCount: 0
})

function onStatsMonthChange() {
  fetchMonthlyStats()
}

function formatTime(timeStr) {
  if (!timeStr) return '-'
  return String(timeStr).replace('T', ' ')
}

// ---- Product Options ----
const fetchProductOptions = async () => {
  try {
    const res = await getProductBaseList()
    if (res.data) {
      productOptions.value = Array.isArray(res.data) ? res.data : (res.data.records || [])
    }
  } catch {
    // silently fail
  }
}

// ---- Monthly Stats ----
const fetchMonthlyStats = async () => {
  const [year, month] = statsSelectedMonth.value.split('-')
  statsLoading.value = true
  try {
    const res = await getLossMonthlyStats({ year: parseInt(year), month: parseInt(month) })
    if (res.code === 200 && res.data) {
      monthlyStats.totalLossAmount = res.data.lossTotalAmount || 0
      monthlyStats.lossCount = res.data.lossCount || 0
    }
  } catch {
    // silently fail
  } finally {
    statsLoading.value = false
  }
}

// ---- Loss List ----
const fetchLossList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.size
    }
    if (searchForm.baseProductId) {
      params.baseProductId = searchForm.baseProductId
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }

    const res = await getLossPage(params)
    if (res.code === 200) {
      const data = res.data
      tableData.value = data.records || []
      pagination.total = data.total || 0
      pagination.current = data.current || 1
      pagination.size = data.size || 10
      ElMessage.success('查询成功')
    } else {
      ElMessage.error(res.message || '查询失败')
    }
  } catch (err) {
    ElMessage.error(err?.message || '查询损耗记录失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchLossList()
}

const handleReset = () => {
  searchForm.baseProductId = null
  searchForm.dateRange = null
  pagination.current = 1
  fetchLossList()
}

const handleSizeChange = () => {
  pagination.current = 1
  fetchLossList()
}

const handlePageChange = () => {
  fetchLossList()
}

// ---- Add Loss (batch selection mode) ----
const openAddDialog = () => {
  addForm.baseProductId = null
  addForm.lossReason = ''
  batchDetails.value = []
  selectedBatches.value = []
  addDialogVisible.value = true
}

const resetAddForm = () => {
  addForm.baseProductId = null
  addForm.lossReason = ''
  batchDetails.value = []
  selectedBatches.value = []
}

// Only allow selecting batches that are in-warehouse (0) or on-shelf (1)
const checkSelectable = (row) => {
  return row.status === 0 || row.status === 1
}

const onBatchSelectionChange = (rows) => {
  selectedBatches.value = rows
}

const handleProductChange = async (baseProductId) => {
  batchDetails.value = []
  selectedBatches.value = []
  if (!baseProductId) return

  batchLoading.value = true
  try {
    const res = await getProductStockDetail(baseProductId)
    const data = res.data
    // Sort: on-shelf first, then warehouse; oldest first within each
    const list = Array.isArray(data) ? data : (data?.records || data?.details || [])
    list.sort((a, b) => {
      if (a.status !== b.status) return b.status - a.status // status 1 before 0
      return (a.id || 0) - (b.id || 0)
    })
    batchDetails.value = list
  } catch {
    batchDetails.value = []
    ElMessage.error('获取批次信息失败')
  } finally {
    batchLoading.value = false
  }
}

const handleAddConfirm = async () => {
  if (!addForm.baseProductId) {
    ElMessage.warning('请选择商品')
    return
  }
  if (selectedBatchIds.value.length === 0) {
    ElMessage.warning('请至少选择一个批次')
    return
  }
  if (!addForm.lossReason || !addForm.lossReason.trim()) {
    ElMessage.warning('请输入损耗原因')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认将选中的 ${selectedBatchIds.value.length} 个批次登记为损耗吗？总损耗金额：¥${selectedLossTotal.value.toFixed(2)}`,
      '确认登记',
      {
        confirmButtonText: '确认',
        type: 'warning'
      }
    )

    adding.value = true
    const res = await addLoss({
      batchDetailIds: selectedBatchIds.value,
      lossReason: addForm.lossReason
    })
    if (res.code === 200) {
      ElMessage.success('损耗记录登记成功！')
      addDialogVisible.value = false
      fetchLossList()
      fetchMonthlyStats()
      fetchProductOptions()
    } else {
      ElMessage.error(res.message || '登记失败')
    }
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.message || '登记失败')
    }
  } finally {
    adding.value = false
  }
}

onMounted(() => {
  fetchProductOptions()
  fetchLossList()
  fetchMonthlyStats()
})
</script>

<style scoped>
.loss-record-container {
  padding: 10px;
}

.page-title {
  margin-bottom: 16px;
  font-size: 20px;
}

/* Stats header with month picker */
.stats-header-card {
  margin-bottom: 16px;
}
.stats-header-card :deep(.el-card__body) {
  padding: 16px 20px;
}
.stats-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.stats-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stats-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.stats-month-picker {
  width: 180px;
}

.stat-card {
  text-align: center;
  padding: 8px 0;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value.loss {
  color: #f56c6c;
}

.search-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.table-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.batch-select-area {
  margin-bottom: 4px;
}
</style>
