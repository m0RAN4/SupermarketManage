<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="货架编号">
          <el-input v-model="searchForm.shelfCode" placeholder="请输入货架编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-input v-model="searchForm.shelfArea" placeholder="请输入所属区域" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增货架</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width:100%" @expand-change="handleExpand">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content" v-loading="capacityLoading[row.id]">
              <h5 style="margin:0 0 8px">每层使用详情</h5>
              <el-table :data="capacityDetails[row.id] || []" border size="small" style="width:100%">
                <el-table-column prop="shelfLayer" label="层数" width="80" align="center" />
                <el-table-column label="已使用" width="100" align="center">
                  <template #default="{ row: r }">
                    <span :style="{ color: r.usedCount >= r.maxCapacity ? '#f56c6c' : '#67c23a' }">
                      {{ r.usedCount }} / {{ r.maxCapacity }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="剩余" width="100" align="center">
                  <template #default="{ row: r }">
                    {{ Math.max(0, r.maxCapacity - r.usedCount) }}
                  </template>
                </el-table-column>
                <el-table-column label="使用率" min-width="200">
                  <template #default="{ row: r }">
                    <el-progress :percentage="r.maxCapacity > 0 ? Math.round(r.usedCount / r.maxCapacity * 100) : 0"
                      :color="r.usedCount >= r.maxCapacity ? '#f56c6c' : r.usedCount / r.maxCapacity > 0.7 ? '#e6a23c' : '#67c23a'" />
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="shelfCode" label="货架编号" width="120" align="center" />
        <el-table-column prop="shelfArea" label="所属区域" width="120" align="center" />
        <el-table-column prop="shelfLayer" label="层数" width="70" align="center" />
        <el-table-column prop="layerCapacity" label="每层容量" width="90" align="center" />
        <el-table-column prop="totalCapacity" label="总容量" width="80" align="center" />
        <el-table-column label="启用状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.isEnabled === 1" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper"
          @current-change="fetchData" @size-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑货架':'新增货架'" width="500px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="货架编号" prop="shelfCode">
          <el-input v-model="form.shelfCode" placeholder="如 A-01" />
        </el-form-item>
        <el-form-item label="所属区域" prop="shelfArea">
          <el-input v-model="form.shelfArea" placeholder="如 食品区" />
        </el-form-item>
        <el-form-item label="层数" prop="shelfLayer">
          <el-input-number v-model="form.shelfLayer" :min="1" placeholder="货架共几层" style="width:100%" />
        </el-form-item>
        <el-form-item label="每层容量" prop="layerCapacity">
          <el-input-number v-model="form.layerCapacity" :min="1" placeholder="每层放多少件" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="form.shelfLayer && form.layerCapacity" label="总容量">
          <el-input :model-value="form.shelfLayer * form.layerCapacity" disabled style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit } from '@element-plus/icons-vue'
import { getShelfPage, addShelf, updateShelf, toggleShelf, getShelfCapacity } from '../api/product'
import { formatTime } from '../utils/format'

const searchForm = reactive({ shelfCode: '', shelfArea: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })
const loading = ref(false)
const tableData = ref([])

const capacityDetails = reactive({})
const capacityLoading = reactive({})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({ shelfCode: '', shelfArea: '', shelfLayer: undefined, layerCapacity: undefined })
const rules = {
  shelfCode: [{ required: true, message: '请输入货架编号', trigger: 'blur' }],
  shelfArea: [{ required: true, message: '请输入所属区域', trigger: 'blur' }]
}

async function handleExpand(row, expandedRows) {
  if (!expandedRows.includes(row)) return
  if (capacityDetails[row.id]) return
  capacityLoading[row.id] = true
  try {
    const res = await getShelfCapacity(row.id)
    capacityDetails[row.id] = res.data || []
  } catch { capacityDetails[row.id] = [] }
  finally { capacityLoading[row.id] = false }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getShelfPage({
      current: pagination.current, size: pagination.size,
      shelfCode: searchForm.shelfCode || undefined, shelfArea: searchForm.shelfArea || undefined
    })
    const data = res.data
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch {} finally { loading.value = false }
}

function handleQuery() { pagination.current = 1; fetchData() }
function handleReset() { searchForm.shelfCode = ''; searchForm.shelfArea = ''; handleQuery() }

function handleAdd() { isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true }
function handleEdit(row) {
  isEdit.value = true; editingId.value = row.id
  form.shelfCode = row.shelfCode; form.shelfArea = row.shelfArea
  form.shelfLayer = row.shelfLayer; form.layerCapacity = row.layerCapacity
  dialogVisible.value = true
}
function handleDialogClosed() { resetForm(); formRef.value?.resetFields() }
function resetForm() { form.shelfCode = ''; form.shelfArea = ''; form.shelfLayer = undefined; form.layerCapacity = undefined }

async function handleToggle(row) {
  const action = row.isEnabled === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该货架吗？`, '提示', { type:'warning' })
    await toggleShelf(row.id)
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch {}
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data = { shelfCode: form.shelfCode, shelfArea: form.shelfArea, shelfLayer: form.shelfLayer, layerCapacity: form.layerCapacity }
    if (isEdit.value) { data.id = editingId.value; await updateShelf(data); ElMessage.success('编辑成功') }
    else { await addShelf(data); ElMessage.success('新增成功') }
    dialogVisible.value = false
    // 清除容量缓存
    for (const k of Object.keys(capacityDetails)) delete capacityDetails[k]
    fetchData()
  } catch (e) { ElMessage.error(e?.message||'操作失败') }
  finally { submitLoading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-container { max-width:1200px; margin:0 auto; }
.search-card { margin-bottom:16px; }
.table-card { min-height:400px; }
.table-header { margin-bottom:16px; }
.pagination-wrapper { display:flex; justify-content:flex-end; margin-top:16px; }
.expand-content { padding:12px 20px; background:#fafafa; }
</style>
