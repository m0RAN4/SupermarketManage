<template>
  <div class="order-list-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">
          <el-icon class="page-title-icon"><Document /></el-icon>
          订单管理
        </h2>
        <p class="page-desc">查看和管理所有订单，支持支付、取消和退货操作</p>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-select v-model="searchForm.payStatus" placeholder="支付状态" clearable class="search-select">
        <el-option label="全部" :value="undefined" />
        <el-option label="未支付" :value="0" />
        <el-option label="已支付" :value="1" />
        <el-option label="已退款" :value="2" />
        <el-option label="部分退款" :value="4" />
        <el-option label="已取消" :value="3" />
      </el-select>
      <el-date-picker
        v-model="searchForm.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="search-date"
      />
      <el-button type="primary" class="search-btn" @click="handleSearch">
        <el-icon><Search /></el-icon> 查询
      </el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-wrapper">
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%" class="custom-table">
        <el-table-column prop="id" label="订单号" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额" min-width="110" align="center">
          <template #default="{ row }">
            <span class="amount-text">¥{{ Number(row.payAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.payStatus === 0" type="info" effect="plain" round>未支付</el-tag>
            <el-tag v-else-if="row.payStatus === 1" type="success" effect="plain" round>已支付</el-tag>
            <el-tag v-else-if="row.payStatus === 2" type="danger" effect="plain" round>已退款</el-tag>
            <el-tag v-else-if="row.payStatus === 3" type="info" effect="plain" round>已取消</el-tag>
            <el-tag v-else-if="row.payStatus === 4" type="warning" effect="plain" round>部分退款</el-tag>
            <el-tag v-else type="info" effect="plain" round>未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.payStatus === 0" size="small" type="success" @click="handlePay(row)">支付</el-button>
            <el-button v-if="row.payStatus === 0" size="small" type="danger" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="isAdmin" size="small" type="warning" @click="handleEditStatus(row)">编辑状态</el-button>
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
          background
        />
      </div>
    </div>

    <!-- 编辑状态弹窗 -->
    <el-dialog v-model="editStatusVisible" title="编辑订单状态" width="420px" class="edit-dialog">
      <el-form label-width="100px">
        <el-form-item label="订单ID"><span class="form-text">{{ editStatusForm.orderId }}</span></el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="editStatusForm.payStatus" style="width: 100%">
            <el-option :value="0" label="未支付" />
            <el-option :value="1" label="已支付" />
            <el-option :value="2" label="已退款" />
            <el-option :value="3" label="已取消" />
            <el-option :value="4" label="部分退款" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editStatusVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEditStatus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Document } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { getOrderPage, payOrder, cancelOrder, updateOrderPayStatus } from '../api/order'
import { formatTime } from '../utils/format'

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

const loading = ref(false)
const tableData = ref([])
const editStatusVisible = ref(false)
const editStatusForm = reactive({ orderId: null, payStatus: null })

const searchForm = reactive({
  payStatus: undefined,
  dateRange: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchOrderList = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.payStatus !== undefined && searchForm.payStatus !== null) {
      params.payStatus = searchForm.payStatus
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const res = await getOrderPage(params)
    if (res.code === 200) {
      const data = res.data
      tableData.value = data.records || []
      pagination.total = data.total || 0
      pagination.current = data.current || 1
      pagination.size = data.size || 10
    } else {
      ElMessage.error(res.message || '查询失败')
    }
  } catch (err) {
    ElMessage.error(err?.message || '查询订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchOrderList() }
const handleReset = () => {
  searchForm.payStatus = undefined
  searchForm.dateRange = null
  pagination.current = 1
  fetchOrderList()
}
const handleSizeChange = () => { pagination.current = 1; fetchOrderList() }
const handlePageChange = () => { fetchOrderList() }
const viewDetail = (row) => { router.push(`/order/${row.id}`) }

const handlePay = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认支付 ¥${(row.payAmount || 0).toFixed(2)} 吗？`,
      '确认支付',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'info' }
    )
    const res = await payOrder({ orderId: row.id })
    if (res.code === 200) { ElMessage.success('支付成功！'); fetchOrderList() }
    else { ElMessage.error(res.message || '支付失败') }
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err?.message || '支付失败')
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确认取消此订单吗？', '确认取消', {
      confirmButtonText: '确认取消', cancelButtonText: '返回', type: 'warning'
    })
    const res = await cancelOrder({ orderId: row.id })
    if (res.code === 200) { ElMessage.success('订单已取消'); fetchOrderList() }
    else { ElMessage.error(res.message || '取消失败') }
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err?.message || '取消失败')
  }
}

function handleEditStatus(row) {
  editStatusForm.orderId = row.id
  editStatusForm.payStatus = row.payStatus
  editStatusVisible.value = true
}

async function confirmEditStatus() {
  try {
    await updateOrderPayStatus({ orderId: editStatusForm.orderId, payStatus: editStatusForm.payStatus })
    ElMessage.success('订单状态已更新')
    editStatusVisible.value = false
    fetchOrderList()
  } catch (err) {
    ElMessage.error(err?.message || '更新失败')
  }
}

onMounted(() => { fetchOrderList() })
</script>

<style scoped>
.order-list-container { max-width: 1100px; margin: 0 auto; }

.page-header { margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #1a1a2e; display: flex; align-items: center; gap: 8px; }
.page-title-icon { color: #1e88e5; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #909399; }

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #ffffff;
  padding: 16px 20px;
  border-radius: 14px;
  margin-bottom: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}

.search-select { width: 150px; }
.search-date { width: 260px; }

/* 表格 */
.table-wrapper {
  background: #ffffff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}

.custom-table :deep(.el-table__header th) {
  background: #fafafa;
  color: #606266;
  font-weight: 600;
}

.amount-text { color: #f56c6c; font-weight: 700; font-size: 14px; }

.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }

.form-text { font-size: 14px; color: #303133; }
</style>
