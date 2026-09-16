<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入商品名称" clearable @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item label="筛选方式">
          <el-select v-model="searchForm.filterMode" style="width:220px">
            <el-option value="available" label="仅显示已上架和未上架" />
            <el-option value="onshelf" label="仅显示已上架" />
            <el-option value="notshelf" label="仅显示未上架" />
            <el-option value="all" label="全部显示" />
            <el-option value="sold" label="仅显示已售出" />
            <el-option value="loss" label="仅显示已损耗" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchData">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div style="margin-bottom:12px">
        <el-button type="success" :disabled="selectedBatches.length===0" @click="openBatchShelfOn">批量上架</el-button>
        <el-button type="warning" :disabled="selectedBatches.length===0" @click="openBatchShelfOff">批量下架</el-button>
        <el-button type="primary" :disabled="selectedBatches.length===0" @click="openBatchPrice">批量改价</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="baseProductId" style="width:100%">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <el-table :data="row.batches || []" border size="small" style="width:100%"
                @selection-change="(rows) => onExpandSelectionChange(row, rows)"
                ref="expandTableRef"
              >
                <el-table-column type="selection" width="45" />
                <el-table-column prop="id" label="批次ID" width="80" align="center" />
                <el-table-column prop="purchaseRecordId" label="进货编号" width="100" align="center" />
                <el-table-column label="货架" width="100" align="center">
                  <template #default="{ row: b }">{{ b.shelfCode || '-' }}</template>
                </el-table-column>
                <el-table-column label="层数" width="70" align="center">
                  <template #default="{ row: b }">{{ b.shelfLayer ?? '-' }}</template>
                </el-table-column>
                <el-table-column prop="shelfDisplayName" label="展示名称" min-width="120">
                  <template #default="{ row: b }">{{ b.shelfDisplayName || '-' }}</template>
                </el-table-column>
                <el-table-column label="成本价" width="90" align="center">
                  <template #default="{ row: b }">¥{{ (b.purchaseUnitPrice||0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="售价" width="90" align="center">
                  <template #default="{ row: b }">¥{{ (b.salePrice||0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="折扣价" width="90" align="center">
                  <template #default="{ row: b }">{{ b.discountPrice ? '¥'+Number(b.discountPrice).toFixed(2) : '-' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="110" align="center">
                  <template #default="{ row: b }">
                    <el-tag :type="statusType(b.status)" size="small">{{ statusText(b.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="创建时间" width="160" align="center">
                  <template #default="{ row: b }">{{ formatTime(b.createTime) }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="baseProductId" label="商品ID" width="80" align="center" />
        <el-table-column prop="productName" label="商品名称" min-width="140" />
        <el-table-column label="总库存" width="80" align="center">
          <template #default="{ row }">{{ row.totalCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="已上架" width="80" align="center">
          <template #default="{ row }">{{ row.onShelfCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="仓库" width="80" align="center">
          <template #default="{ row }">{{ row.warehouseCount ?? 0 }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Shelf On Dialog -->
    <el-dialog v-model="shelfOnVisible" title="批量上架" width="700px" @closed="resetShelfOnForm">
      <el-alert type="info" :closable="false" style="margin-bottom:12px">
        已选择 <strong>{{ batchShelfOnIds.length }}</strong> 个批次
      </el-alert>
      <el-form :model="shelfOnForm" label-width="100px">
        <el-form-item label="目标货架">
          <el-select v-model="shelfOnForm.shelfId" placeholder="不选=自动从第一个货架开始分配" clearable filterable style="width:100%" @change="onShelfChange">
            <el-option v-for="s in shelfOptions" :key="s.id" :label="(s.shelfCode||'')+'（'+s.shelfArea+'）'" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="shelfOnForm.shelfId && shelfLayers.length > 0" label="目标层数">
          <el-select v-model="shelfOnForm.shelfLayer" placeholder="不选=自动分配空位" clearable style="width:100%">
            <el-option v-for="l in shelfLayers" :key="l.layer" :label="'第'+l.layer+'层（容量:'+l.capacity+'，已用:'+l.used+'，可用:'+l.available+'）'" :value="l.layer" :disabled="l.available <= 0" />
          </el-select>
          <div style="color:#909399;font-size:12px;margin-top:4px">
            总容量：{{ shelfTotalCapacity }}，已用：{{ shelfTotalUsed }}，可用：{{ shelfTotalAvailable }}
            <span v-if="!shelfOnForm.shelfLayer" style="color:#e6a23c">（不选层数：依次填满有空位的层，总计可用 {{ shelfTotalAvailable }} 个空位）</span>
            <span v-else style="color:#e6a23c">（该层可用：{{ selectedLayerAvailable }} 个空位；超出部分优先在本货架其他层顺延，本货架填满后从第一个货架开始顺延）</span>
          </div>
        </el-form-item>
        <el-form-item label="展示名称">
          <el-input v-model="shelfOnForm.shelfDisplayName" placeholder="上架展示名称" />
        </el-form-item>
        <el-form-item label="售价" required>
          <el-input-number v-model="shelfOnForm.salePrice" :min="0.01" :precision="2" :step="0.5" style="width:100%" />
        </el-form-item>
        <el-form-item label="折扣价">
          <el-input-number v-model="shelfOnForm.discountPrice" :min="0" :precision="2" :step="0.5" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shelfOnVisible=false">取消</el-button>
        <el-button type="primary" :loading="shelfOnLoading" @click="submitShelfOn">确定上架</el-button>
      </template>
    </el-dialog>

    <!-- Shelf Off Confirm -->
    <el-dialog v-model="shelfOffVisible" title="批量下架" width="400px">
      <p>确认将 <strong>{{ batchShelfOnIds.length }}</strong> 个批次下架？</p>
      <template #footer>
        <el-button @click="shelfOffVisible=false">取消</el-button>
        <el-button type="primary" :loading="shelfOffLoading" @click="submitShelfOff">确定下架</el-button>
      </template>
    </el-dialog>

    <!-- Batch Price Dialog -->
    <el-dialog v-model="priceVisible" title="批量改价" width="450px" @closed="resetPriceForm">
      <el-alert type="info" :closable="false" style="margin-bottom:12px">
        已选择 <strong>{{ batchShelfOnIds.length }}</strong> 个批次
      </el-alert>
      <el-form :model="priceForm" label-width="100px">
        <el-form-item label="新售价">
          <el-input-number v-model="priceForm.salePrice" :min="0.01" :precision="2" :step="0.5" style="width:100%" />
        </el-form-item>
        <el-form-item label="新折扣价">
          <el-input-number v-model="priceForm.discountPrice" :min="0" :precision="2" :step="0.5" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceVisible=false">取消</el-button>
        <el-button type="primary" :loading="priceLoading" @click="submitPrice">确定改价</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getShelfOnBatches, getShelfLayers, batchShelfOn, batchShelfOff, batchUpdatePrice, getShelfList } from '../api/product'
import { formatTime } from '../utils/format'

const loading = ref(false)
const tableData = ref([])
const shelfOptions = ref([])
const selectedBatches = ref([])
const expandTableRef = ref(null)

const searchForm = reactive({ keyword: '', filterMode: 'available' })

const statusType = (s) => ({ 0:'info',1:'success',2:'',3:'danger',4:'warning',5:'info' }[s]||'info')
const statusText = (s) => ({ 0:'在库未上架',1:'已上架在售',2:'已售出',3:'已损耗',4:'退货待处理',5:'待支付锁定' }[s]||'未知')

// Store per-row selections
const rowSelections = reactive({})

function onExpandSelectionChange(row, rows) {
  rowSelections[row.baseProductId] = rows.map(r => r.id)
  updateSelectedBatches()
}

function updateSelectedBatches() {
  const all = []
  for (const [key, ids] of Object.entries(rowSelections)) {
    all.push(...ids)
  }
  selectedBatches.value = all
}

// Shelf On
const shelfOnVisible = ref(false)
const shelfOnLoading = ref(false)
const batchShelfOnIds = ref([])
const shelfLayers = ref([])
const shelfOnForm = reactive({ shelfId: null, shelfLayer: null, shelfDisplayName: '', salePrice: null, discountPrice: null })

const shelfTotalCapacity = computed(() => shelfLayers.value.reduce((s,l)=>s+(l.capacity||0),0))
const shelfTotalUsed = computed(() => shelfLayers.value.reduce((s,l)=>s+(l.used||0),0))
const shelfTotalAvailable = computed(() => shelfTotalCapacity.value - shelfTotalUsed.value)
const selectedLayerAvailable = computed(() => {
  const l = shelfLayers.value.find(l=>l.layer===shelfOnForm.shelfLayer)
  return l ? l.available : 0
})

async function onShelfChange(shelfId) {
  if (!shelfId) { shelfLayers.value = []; return }
  try {
    const res = await getShelfLayers(shelfId)
    shelfLayers.value = res.data?.layers || []
    shelfOnForm.shelfLayer = null
  } catch { shelfLayers.value = [] }
}

function findBatchById(batchId) {
  for (const row of tableData.value) {
    if (row.batches) {
      for (const b of row.batches) {
        if (b.id === batchId) return b
      }
    }
  }
  return null
}

function openBatchShelfOn() {
  const allIds = [...selectedBatches.value]
  const alreadyShelved = []
  const validIds = []
  for (const id of allIds) {
    const batch = findBatchById(id)
    if (batch && batch.status !== 0) {
      alreadyShelved.push(id)
    } else {
      validIds.push(id)
    }
  }
  if (alreadyShelved.length > 0) {
    ElMessageBox.confirm(
      `已选择 ${allIds.length} 个批次，其中 ${alreadyShelved.length} 个非可上架物品。是否取消勾选非可上架操作批次并继续上架操作？`,
      '提示',
      { confirmButtonText: '确定（跳过非可上架）', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
      if (validIds.length === 0) { ElMessage.warning('没有可上架的批次'); return }
      doOpenBatchShelfOn(validIds)
    }).catch(() => {})
    return
  }
  doOpenBatchShelfOn(validIds)
}

function doOpenBatchShelfOn(ids) {
  batchShelfOnIds.value = ids
  shelfOnForm.shelfId = null
  shelfOnForm.shelfLayer = null
  shelfOnForm.shelfDisplayName = ''
  shelfOnForm.salePrice = null
  shelfOnForm.discountPrice = null
  shelfLayers.value = []
  shelfOnVisible.value = true
}

function resetShelfOnForm() {
  batchShelfOnIds.value = []
  shelfLayers.value = []
}

async function submitShelfOn(forceContinue = false) {
  if (shelfOnForm.salePrice===null || shelfOnForm.salePrice<=0) { ElMessage.warning('请输入有效售价（必须大于0）'); return }
  shelfOnLoading.value = true
  try {
    const res = await batchShelfOn({
      batchIds: batchShelfOnIds.value,
      shelfId: shelfOnForm.shelfId || null,
      shelfLayer: shelfOnForm.shelfLayer || null,
      shelfDisplayName: shelfOnForm.shelfDisplayName || null,
      salePrice: shelfOnForm.salePrice,
      discountPrice: shelfOnForm.discountPrice || null,
      forceContinue: forceContinue
    })

    if (res.code === 200) {
      const data = res.data
      // 需要确认容量不足
      if (data && data.needConfirm) {
        const msg = data.message + (data.totalRemaining != null
          ? '\n\n超市货架总剩余容量：' + data.totalRemaining
          : '')
        try {
          await ElMessageBox.confirm(msg, '容量提示', {
            confirmButtonText: '确定（超出部分不上架）',
            cancelButtonText: '取消',
            type: 'warning',
            distinguishCancelAndClose: true
          })
          // 用户确认，强制继续
          await submitShelfOn(true)
          return
        } catch (e) {
          // 用户取消
          return
        }
      }
      // 上架成功
      ElMessage.success(data.message || '上架成功')
      if (data.warnings && data.warnings.length > 0) {
        data.warnings.forEach(w => ElMessage.warning(w))
      }
      shelfOnVisible.value = false
      clearSelections()
      fetchData()
    }
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message||'上架失败') }
  finally { shelfOnLoading.value = false }
}

// Shelf Off
const shelfOffVisible = ref(false)
const shelfOffLoading = ref(false)

function openBatchShelfOff() {
  const allIds = [...selectedBatches.value]
  const alreadyOffShelf = []
  const validIds = []
  for (const id of allIds) {
    const batch = findBatchById(id)
    if (batch && batch.status !== 1) {
      alreadyOffShelf.push(id)
    } else {
      validIds.push(id)
    }
  }
  if (alreadyOffShelf.length > 0) {
    ElMessageBox.confirm(
      `已选择 ${allIds.length} 个批次，其中 ${alreadyOffShelf.length} 个非可下架物品。是否取消勾选非可下架操作批次并继续下架操作？`,
      '提示',
      { confirmButtonText: '确定（跳过非可下架）', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
      if (validIds.length === 0) { ElMessage.warning('没有可下架的批次'); return }
      batchShelfOnIds.value = validIds
      shelfOffVisible.value = true
    }).catch(() => {})
    return
  }
  batchShelfOnIds.value = validIds
  shelfOffVisible.value = true
}

async function submitShelfOff() {
  shelfOffLoading.value = true
  try {
    await batchShelfOff({ batchIds: batchShelfOnIds.value })
    ElMessage.success('下架成功')
    shelfOffVisible.value = false
    clearSelections()
    fetchData()
  } catch (e) { ElMessage.error(e?.message||'下架失败') }
  finally { shelfOffLoading.value = false }
}

// Batch Price
const priceVisible = ref(false)
const priceLoading = ref(false)
const priceForm = reactive({ salePrice: null, discountPrice: null })

function openBatchPrice() {
  batchShelfOnIds.value = [...selectedBatches.value]
  priceForm.salePrice = null
  priceForm.discountPrice = null
  priceVisible.value = true
}

function resetPriceForm() { batchShelfOnIds.value = [] }

async function submitPrice() {
  priceLoading.value = true
  try {
    await batchUpdatePrice({
      batchIds: batchShelfOnIds.value,
      salePrice: priceForm.salePrice,
      discountPrice: priceForm.discountPrice || null
    })
    ElMessage.success('改价成功')
    priceVisible.value = false
    clearSelections()
    fetchData()
  } catch (e) { ElMessage.error(e?.message||'改价失败') }
  finally { priceLoading.value = false }
}

function clearSelections() {
  selectedBatches.value = []
  for (const k of Object.keys(rowSelections)) delete rowSelections[k]
}

// Data
async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (searchForm.keyword) params.baseProductId = null // search all
    // 根据筛选方式映射后端参数
    if (searchForm.filterMode === 'available') {
      params.statuses = '0,1'
    } else if (searchForm.filterMode === 'onshelf') {
      params.status = 1
    } else if (searchForm.filterMode === 'notshelf') {
      params.status = 0
    } else if (searchForm.filterMode === 'sold') {
      params.status = 2
    } else if (searchForm.filterMode === 'loss') {
      params.status = 3
    }
    // 'all' 模式不传任何状态参数，后端返回全部
    const res = await getShelfOnBatches(params)
    tableData.value = res.data || []
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.filterMode = 'available'
  fetchData()
}

async function loadShelves() {
  try {
    const res = await getShelfList()
    shelfOptions.value = Array.isArray(res.data) ? res.data : (res.data?.records||[])
  } catch {}
}

onMounted(() => {
  loadShelves()
  fetchData()
})
</script>

<style scoped>
.page-container { max-width:1400px; margin:0 auto; }
.search-card { margin-bottom:16px; }
.table-card { min-height:400px; }
.expand-content { padding:12px 20px; background:#fafafa; }
</style>
