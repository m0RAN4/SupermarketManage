<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderId" placeholder="订单号" clearable />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.handleStatus" placeholder="全部" clearable style="width:130px">
            <el-option :value="0" label="待审核" />
            <el-option :value="1" label="已同意" />
            <el-option :value="2" label="已拒绝" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:260px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div style="margin-bottom:12px">
        <el-button type="success" :disabled="selectedRows.length===0" @click="batchApprove">批量同意</el-button>
        <el-button type="danger" :disabled="selectedRows.length===0" @click="batchReject">批量拒绝</el-button>
      </div>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%"
        @selection-change="(rows)=>selectedRows=rows" ref="tableRef">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="orderId" label="订单号" width="100" />
        <el-table-column prop="productName" label="商品" min-width="120" />
        <el-table-column prop="operatorName" label="申请人" width="100" />
        <el-table-column prop="refundQuantity" label="退货数量" width="100" align="center" />
        <el-table-column prop="refundUnitPrice" label="单价" width="100" align="center">
          <template #default="{ row }">¥{{ Number(row.refundUnitPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="refundTotalAmount" label="退款金额" width="110" align="center">
          <template #default="{ row }">¥{{ Number(row.refundTotalAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="refundReason" label="原因" min-width="120" />
        <el-table-column label="处理状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.refundHandleStatus === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.refundHandleStatus === 1" type="success">已同意</el-tag>
            <el-tag v-else-if="row.refundHandleStatus === 2" type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.refundHandleStatus === 0">
              <el-button type="success" link @click="handleApprove(row)">同意</el-button>
              <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper"
          @current-change="fetchData" @size-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="approveVisible" title="同意退货" width="500px" @closed="approveForm.backQty=0;approveForm.lossQty=0">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>退货数量：{{ approveForm.refundQty }}，回库+转损耗必须等于此数</template>
      </el-alert>
      <el-form label-width="120px">
        <el-form-item label="回库数量">
          <el-input-number v-model="approveForm.backQty" :min="0" :max="approveForm.refundQty" style="width:100%" />
        </el-form-item>
        <el-form-item label="转损耗数量">
          <el-input-number v-model="approveForm.lossQty" :min="0" :max="approveForm.refundQty" style="width:100%" />
        </el-form-item>
        <el-form-item>
          <span style="color:#909399">合计：{{ approveForm.backQty + approveForm.lossQty }} / {{ approveForm.refundQty }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmApprove">确认同意</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRefundPage, approveRefund, rejectRefund, batchApproveRefund, batchRejectRefund } from '../api/order'
import { formatTime } from '../utils/format'

const loading = ref(false)
const tableData = ref([])
const selectedRows = ref([])
const tableRef = ref(null)
const searchForm = reactive({ orderId: null, handleStatus: null, dateRange: null })
const pagination = reactive({ current: 1, size: 10, total: 0 })

const approveVisible = ref(false)
const approveForm = reactive({ refundId: null, refundQty: 0, backQty: 0, lossQty: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size }
    if (searchForm.orderId) params.orderId = searchForm.orderId
    if (searchForm.handleStatus !== null && searchForm.handleStatus !== undefined) params.handleStatus = searchForm.handleStatus
    if (searchForm.dateRange?.length === 2) { params.startTime = searchForm.dateRange[0]; params.endTime = searchForm.dateRange[1] }
    const res = await getRefundPage(params)
    if (res.code === 200) {
      const d = res.data
      tableData.value = d.records || []
      pagination.total = d.total || 0
    }
  } catch (e) { ElMessage.error(e?.message || '查询失败') }
  finally { loading.value = false }
}

function handleQuery() { pagination.current = 1; fetchData() }
function handleReset() { searchForm.orderId = null; searchForm.handleStatus = null; searchForm.dateRange = null; handleQuery() }

function handleApprove(row) {
  approveForm.refundId = row.id
  approveForm.refundQty = row.refundQuantity
  approveForm.backQty = 0
  approveForm.lossQty = 0
  approveVisible.value = true
}

async function confirmApprove() {
  if (approveForm.backQty + approveForm.lossQty !== approveForm.refundQty) {
    ElMessage.warning('回库数量+转损耗数量必须等于退货数量（'+approveForm.refundQty+'）')
    return
  }
  try {
    await approveRefund({ refundId: approveForm.refundId, backStockQuantity: approveForm.backQty, toLossQuantity: approveForm.lossQty })
    ElMessage.success('退货已同意')
    approveVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
}

async function handleReject(row) {
  try {
    await ElMessageBox.confirm('确认拒绝此退货申请吗？', '确认拒绝', { type: 'warning' })
    await rejectRefund({ refundId: row.id })
    ElMessage.success('退货已拒绝')
    fetchData()
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '操作失败') }
}

async function batchApprove() {
  const pending = selectedRows.value.filter(r => r.refundHandleStatus === 0)
  if (pending.length === 0) { ElMessage.warning('没有待审核的退货记录'); return }
  try {
    await ElMessageBox.confirm(`确认批量同意 ${pending.length} 条退货申请吗？`, '批量同意', { type: 'warning' })
    await batchApproveRefund({
      refundIds: pending.map(r => r.id),
      backStockQuantities: pending.map(() => 0),
      toLossQuantities: pending.map(() => 0)
    })
    ElMessage.success('批量同意成功')
    selectedRows.value = []
    fetchData()
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '操作失败') }
}

async function batchReject() {
  const pending = selectedRows.value.filter(r => r.refundHandleStatus === 0)
  if (pending.length === 0) { ElMessage.warning('没有待审核的退货记录'); return }
  try {
    await ElMessageBox.confirm(`确认批量拒绝 ${pending.length} 条退货申请吗？`, '批量拒绝', { type: 'warning' })
    await batchRejectRefund({ refundIds: pending.map(r => r.id) })
    ElMessage.success('批量拒绝成功')
    selectedRows.value = []
    fetchData()
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '操作失败') }
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-container { max-width:1300px; margin:0 auto; }
.search-card { margin-bottom:16px; }
.table-card { min-height:400px; }
.pagination-wrapper { display:flex; justify-content:flex-end; margin-top:16px; }
</style>
