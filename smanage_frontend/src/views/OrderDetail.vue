<template>
  <div class="order-detail-container">
    <div class="back-bar">
      <el-button @click="$router.push('/orders')" :icon="ArrowLeft" class="back-btn">返回订单列表</el-button>
    </div>

    <div class="page-header">
      <h2 class="page-title">订单详情</h2>
      <el-tag v-if="displayPayStatus === 0" type="info" size="large" effect="plain" round>未支付</el-tag>
      <el-tag v-else-if="displayPayStatus === 1" type="success" size="large" effect="plain" round>已支付</el-tag>
      <el-tag v-else-if="displayPayStatus === 2" type="danger" size="large" effect="plain" round>已退款</el-tag>
      <el-tag v-else-if="displayPayStatus === 3" type="info" size="large" effect="plain" round>已取消</el-tag>
      <el-tag v-else-if="displayPayStatus === 4" type="warning" size="large" effect="plain" round>部分退款</el-tag>
    </div>

    <!-- 订单信息卡片 -->
    <div class="info-card" v-loading="loading">
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">订单号</span>
          <span class="info-value">{{ orderInfo.id || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">支付金额</span>
          <span class="info-value amount">¥{{ (orderInfo.payAmount || 0).toFixed(2) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">操作人</span>
          <span class="info-value">{{ orderInfo.username || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span class="info-value">{{ formatTime(orderInfo.createTime) }}</span>
        </div>
      </div>

      <div class="info-actions" v-if="orderInfo.payStatus === 0 && !isAdmin">
        <el-button type="success" size="large" @click="handlePay" :loading="paying" class="action-btn pay-btn">
          <el-icon><Check /></el-icon> 去支付
        </el-button>
        <el-button type="danger" size="large" @click="handleCancelOrder" :loading="cancelling" class="action-btn">
          <el-icon><Close /></el-icon> 取消订单
        </el-button>
      </div>
    </div>

    <!-- 商品明细 -->
    <div class="items-card">
      <div class="items-header">
        <h3>商品明细</h3>
        <el-button
          v-if="(displayPayStatus === 1 || displayPayStatus === 4) && !isAdmin"
          type="warning"
          :disabled="selectedRefundItems.length === 0"
          class="batch-refund-btn"
          @click="openBatchRefund"
        >
          批量退货 ({{ selectedRefundItems.length }})
        </el-button>
      </div>

      <el-table :data="orderItems" stripe style="width: 100%" class="custom-table"
        @selection-change="onRefundSelectionChange" ref="refundTableRef">
        <el-table-column v-if="canRefund && !isAdmin" type="selection" width="40" :selectable="canSelectForRefund" />
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <PositionedImage
              v-if="row.imageUrl"
              :src="getImageUrl(row.imageUrl)"
              :cropData="row.cropData"
              :aspectRatio="(1/1)"
              style="width:56px;border-radius:6px"
            />
            <span v-else style="color:#c0c4cc;font-size:12px">暂无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="120" />
        <el-table-column label="展示名称" min-width="120">
          <template #default="{ row }">{{ row.batchShelfDisplayName || '-' }}</template>
        </el-table-column>
        <el-table-column label="货架" width="80" align="center">
          <template #default="{ row }">{{ row.batchShelfCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="单价" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.batchDiscountPrice" class="price-discount">¥{{ Number(row.batchDiscountPrice).toFixed(2) }}</span>
            <span v-else>¥{{ (row.buyUnitPrice || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="buyQuantity" label="数量" width="60" align="center" />
        <el-table-column label="小计" width="100" align="center">
          <template #default="{ row }">
            <span class="amount-sub">¥{{ (row.itemTotalAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canRefund" label="已退/待审" width="85" align="center">
          <template #default="{ row }">
            <span>{{ row.refundQuantity || 0 }}</span>
            <span v-if="row.pendingRefundQuantity" class="pending"> / {{ row.pendingRefundQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canRefund" label="退款状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.hasPendingRefund" type="warning" size="small" round>待审核</el-tag>
            <el-tag v-else-if="(row.refundQuantity||0) >= (row.buyQuantity||0)" type="success" size="small" round>已退款</el-tag>
            <el-tag v-else-if="(row.refundQuantity||0) > 0" type="info" size="small" round>部分退款</el-tag>
            <span v-else class="no-refund">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canRefund && !isAdmin" label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canSelectForRefund(row)" size="small" type="warning" @click="openSingleRefund(row)">退货</el-button>
            <span v-else-if="row.hasPendingRefund" class="auditing-text">审核中</span>
            <span v-else class="no-action">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 退货弹窗 -->
    <el-dialog v-model="refundVisible" title="退货申请" width="850px" @close="resetRefundForm" class="refund-dialog">
      <el-alert type="info" :closable="false" style="margin-bottom:14px">
        已选择 <strong>{{ refundItems.length }}</strong> 件商品
      </el-alert>

      <el-form label-width="120px" style="margin-bottom:12px">
        <el-form-item label="统一退货原因">
          <el-input v-model="refundForm.unifiedReason" placeholder="可选，填写后将应用于所有未单独填写原因的商品" clearable />
        </el-form-item>
      </el-form>

      <el-table :data="refundItems" stripe size="small" max-height="350" style="width:100%">
        <el-table-column prop="productName" label="商品" min-width="120" />
        <el-table-column label="已退/总共" width="90" align="center">
          <template #default="{ row }">{{ row.refundQuantity || 0 }} / {{ row.buyQuantity }}</template>
        </el-table-column>
        <el-table-column label="可退数量" width="80" align="center">
          <template #default="{ row }">
            {{ (row.buyQuantity || 0) - (row.refundQuantity || 0) - (row.pendingRefundQuantity || 0) }}
          </template>
        </el-table-column>
        <el-table-column label="退货数量" width="140" align="center">
          <template #default="{ row, $index }">
            <el-input-number
              v-model="refundForm.itemQuantities[$index]"
              :min="1"
              :max="(row.buyQuantity || 0) - (row.refundQuantity || 0) - (row.pendingRefundQuantity || 0)"
              size="small" style="width:120px"
            />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="80" align="center">
          <template #default="{ row }">¥{{ (row.buyUnitPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="单独原因（可选）" min-width="150">
          <template #default="{ row, $index }">
            <el-input v-model="refundForm.itemReasons[$index]" placeholder="留空则使用统一原因" size="small" />
          </template>
        </el-table-column>
      </el-table>

      <div class="refund-total">
        退货总金额：<span class="refund-amount">¥{{ refundTotalAmount.toFixed(2) }}</span>
      </div>

      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund" :loading="refunding">提交退货申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Close } from '@element-plus/icons-vue'
import { getOrderDetail, payOrder, cancelOrder, processRefund } from '../api/order'
import { formatTime } from '../utils/format'
import { useAuthStore } from '../store/auth'
import PositionedImage from '../components/PositionedImage.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const orderId = route.params.id

const isAdmin = computed(() => authStore.isAdmin)

const loading = ref(false)
const paying = ref(false)
const cancelling = ref(false)
const refunding = ref(false)
const refundTableRef = ref(null)

const orderInfo = ref({})
const orderItems = ref([])
const selectedRefundItems = ref([])

const displayPayStatus = computed(() => orderInfo.value.payStatus)

// 只有已支付和部分退款状态才能退货
const canRefund = computed(() => displayPayStatus.value === 1 || displayPayStatus.value === 4)

function getImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api/picture/')) return url
  return '/api/picture/' + url
}

const refundVisible = ref(false)
const refundItems = ref([])
const refundForm = reactive({
  unifiedReason: '',
  itemQuantities: [],
  itemReasons: []
})

const refundTotalAmount = computed(() => {
  return refundItems.value.reduce((sum, item, i) => {
    return sum + (item.buyUnitPrice || 0) * (refundForm.itemQuantities[i] || 0)
  }, 0)
})

function canSelectForRefund(row) {
  const pendingQty = row.pendingRefundQuantity || 0
  const refundedQty = row.refundQuantity || 0
  return !row.hasPendingRefund && (refundedQty + pendingQty) < (row.buyQuantity || 0)
}

function onRefundSelectionChange(rows) { selectedRefundItems.value = rows }

async function fetchOrderDetail() {
  loading.value = true
  try {
    const res = await getOrderDetail(orderId)
    if (res.code === 200) { orderInfo.value = res.data || {}; orderItems.value = res.data?.items || [] }
    else ElMessage.error(res.message || '查询失败')
  } catch (e) { ElMessage.error(e?.message || '查询失败') }
  finally { loading.value = false }
}

async function handlePay() {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${(orderInfo.value.payAmount||0).toFixed(2)} 吗？`, '确认支付', { type: 'info' })
    paying.value = true
    const res = await payOrder({ orderId })
    if (res.code === 200) { ElMessage.success('支付成功'); fetchOrderDetail() }
    else ElMessage.error(res.message || '支付失败')
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '支付失败') }
  finally { paying.value = false }
}

async function handleCancelOrder() {
  try {
    await ElMessageBox.confirm('确认取消此订单吗？', '确认取消', { type: 'warning' })
    cancelling.value = true
    const res = await cancelOrder({ orderId })
    if (res.code === 200) { ElMessage.success('订单已取消'); fetchOrderDetail() }
    else ElMessage.error(res.message || '取消失败')
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '取消失败') }
  finally { cancelling.value = false }
}

function openSingleRefund(row) {
  refundItems.value = [row]
  refundForm.unifiedReason = ''
  refundForm.itemQuantities = [1]
  refundForm.itemReasons = ['']
  refundVisible.value = true
}

function openBatchRefund() {
  if (selectedRefundItems.value.length === 0) { ElMessage.warning('请选择要退货的商品'); return }
  refundItems.value = [...selectedRefundItems.value]
  refundForm.unifiedReason = ''
  refundForm.itemQuantities = selectedRefundItems.value.map(() => 1)
  refundForm.itemReasons = selectedRefundItems.value.map(() => '')
  refundVisible.value = true
}

function resetRefundForm() {
  refundItems.value = []
  refundForm.unifiedReason = ''
  refundForm.itemQuantities = []
  refundForm.itemReasons = []
}

async function submitRefund() {
  for (let i = 0; i < refundItems.value.length; i++) {
    if (!refundForm.itemQuantities[i] || refundForm.itemQuantities[i] <= 0) {
      ElMessage.warning(`"${refundItems.value[i].productName}" 退货数量无效`); return
    }
    const reason = refundForm.itemReasons[i]?.trim() || refundForm.unifiedReason?.trim()
    if (!reason) { ElMessage.warning(`"${refundItems.value[i].productName}" 未填写退货原因`); return }
  }

  try {
    await ElMessageBox.confirm(
      `确认提交 ${refundItems.value.length} 件商品的退货申请？退货总金额 ¥${refundTotalAmount.value.toFixed(2)}`,
      '确认退货申请', { confirmButtonText: '确认提交', type: 'warning' }
    )
    refunding.value = true
    for (let i = 0; i < refundItems.value.length; i++) {
      const item = refundItems.value[i]
      const reason = refundForm.itemReasons[i]?.trim() || refundForm.unifiedReason?.trim()
      await processRefund({
        orderId: orderInfo.value.id,
        orderItemId: item.id,
        refundQuantity: refundForm.itemQuantities[i],
        refundReason: reason
      })
    }
    ElMessage.success('退货申请已提交')
    refundVisible.value = false
    selectedRefundItems.value = []
    fetchOrderDetail()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '提交失败')
  } finally { refunding.value = false }
}

onMounted(() => {
  if (!orderId) { ElMessage.error('订单ID不存在'); router.push('/orders'); return }
  fetchOrderDetail()
})
</script>

<style scoped>
.order-detail-container { max-width: 1100px; margin: 0 auto; }

.back-bar { margin-bottom: 12px; }
.back-btn { border-radius: 10px; }

.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #1a1a2e; }

/* 订单信息卡片 */
.info-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label { font-size: 12px; color: #909399; text-transform: uppercase; letter-spacing: 1px; }
.info-value { font-size: 16px; font-weight: 600; color: #303133; }
.info-value.amount { font-size: 20px; color: #f56c6c; }

.info-actions { display: flex; gap: 12px; margin-top: 20px; padding-top: 18px; border-top: 1px solid #f5f5f5; }
.action-btn { border-radius: 12px; font-weight: 600; }

/* 商品明细卡片 */
.items-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}

.items-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.items-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #1a1a2e; }

.batch-refund-btn { border-radius: 10px; }

.custom-table :deep(.el-table__header th) { background: #fafafa; font-weight: 600; }

.price-discount { color: #f56c6c; font-weight: 600; }
.amount-sub { color: #f56c6c; font-weight: 700; }
.pending { color: #e6a23c; }
.no-refund, .no-action { color: #c0c4cc; }
.auditing-text { color: #e6a23c; font-size: 12px; }

.refund-total { margin-top: 14px; text-align: right; font-size: 15px; color: #303133; }
.refund-amount { color: #f56c6c; font-weight: 700; font-size: 20px; }
</style>
