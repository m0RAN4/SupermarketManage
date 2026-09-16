<template>
  <div class="page-container">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="商品名称">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入商品名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="入库时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table Card -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增进货</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="baseProductName" label="商品名称" min-width="160" />
        <el-table-column prop="purchaseQuantity" label="进货数量" width="100" align="center" />
        <el-table-column prop="purchaseUnitPrice" label="进货单价" width="110" align="center">
          <template #default="{ row }">
            {{ row.purchaseUnitPrice != null ? '¥' + Number(row.purchaseUnitPrice).toFixed(2) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="purchaseTotalAmount" label="进货总金额" width="120" align="center">
          <template #default="{ row }">
            {{ row.purchaseTotalAmount != null ? '¥' + Number(row.purchaseTotalAmount).toFixed(2) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="入库时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>

    <!-- Add Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="新增进货"
      width="550px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="选择商品" prop="baseProductId">
          <el-select
            v-model="form.baseProductId"
            placeholder="请选择商品档案中的商品"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in productBaseOptions"
              :key="item.id"
              :label="item.productName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="进货数量" prop="purchaseQuantity">
          <el-input-number v-model="form.purchaseQuantity" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="进货单价" prop="purchaseUnitPrice">
          <el-input-number v-model="form.purchaseUnitPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="进货总金额">
          <el-input :model-value="computedTotalAmount" disabled style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定进货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getProductBaseList } from '../api/product'
import { getPurchasePage, addPurchase } from '../api/order'
import { formatTime } from '../utils/format'

// ---- Product Base Options ----
const productBaseOptions = ref([])

async function loadProductBases() {
  try {
    const res = await getProductBaseList()
    const list = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    productBaseOptions.value = list.filter(item => item.isEnabled === 1)
  } catch {
    // Error handled by interceptor
  }
}

// ---- Search Form ----
const searchForm = reactive({
  keyword: '',
  dateRange: null
})

// ---- Pagination ----
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// ---- Table ----
const loading = ref(false)
const tableData = ref([])

// ---- Dialog ----
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({
  baseProductId: null,
  purchaseQuantity: null,
  purchaseUnitPrice: null
})

const rules = {
  baseProductId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  purchaseQuantity: [
    { required: true, message: '请输入进货数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '进货数量必须大于0', trigger: 'blur' }
  ],
  purchaseUnitPrice: [
    { required: true, message: '请输入进货单价', trigger: 'blur' },
    { type: 'number', min: 0, message: '进货单价不能为负', trigger: 'blur' }
  ]
}

// ---- Computed ----
const computedTotalAmount = computed(() => {
  const qty = form.purchaseQuantity
  const price = form.purchaseUnitPrice
  if (qty != null && price != null && qty > 0 && price >= 0) {
    return '¥' + (qty * price).toFixed(2)
  }
  return '-'
})

// ---- Methods: Table ----
async function fetchData() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getPurchasePage(params)
    const data = res.data
    tableData.value = data.records || []
    pagination.total = data.total || 0
    pagination.current = data.current || pagination.current
    pagination.size = data.size || pagination.size
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.dateRange = null
  pagination.current = 1
  fetchData()
}

// ---- Methods: Add Dialog ----
function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

function handleDialogClosed() {
  resetForm()
  formRef.value?.resetFields()
}

function resetForm() {
  form.baseProductId = null
  form.purchaseQuantity = null
  form.purchaseUnitPrice = null
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await addPurchase({
      baseProductId: form.baseProductId,
      purchaseQuantity: form.purchaseQuantity,
      purchaseUnitPrice: form.purchaseUnitPrice
    })
    ElMessage.success('进货入库成功')
    dialogVisible.value = false
    fetchData()
  } catch {
  } finally {
    submitLoading.value = false
  }
}

// ---- Init ----
onMounted(() => {
  loadProductBases()
  fetchData()
})
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  min-height: 400px;
}

.table-header {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
