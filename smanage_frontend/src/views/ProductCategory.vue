<template>
  <div class="page-container">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="分类名称">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入分类名称"
            clearable
            @keyup.enter="handleQuery"
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="categoryName" label="分类名称" min-width="150" />
        <el-table-column prop="sort" label="排序" width="100" align="center" />
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isEnabled === 1"
              :active-value="true"
              :inactive-value="false"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          </template>
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

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="500px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" placeholder="请输入排序值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit } from '@element-plus/icons-vue'
import {
  getCategoryPage,
  addCategory,
  updateCategory,
  toggleCategory
} from '../api/product'

// ---- Search Form ----
const searchForm = reactive({
  keyword: ''
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
const isEdit = ref(false)
const submitLoading = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  categoryName: '',
  sort: 0
})

const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }]
}

// ---- Methods ----
async function fetchData() {
  loading.value = true
  try {
    const res = await getCategoryPage({
      current: pagination.current,
      size: pagination.size,
      keyword: searchForm.keyword || undefined
    })
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
  pagination.current = 1
  fetchData()
}

function handleAdd() {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  form.categoryName = row.categoryName
  form.sort = row.sort
  dialogVisible.value = true
}

function handleDialogClosed() {
  resetForm()
  formRef.value?.resetFields()
}

function resetForm() {
  form.categoryName = ''
  form.sort = 0
}

async function handleToggle(row) {
  const action = row.isEnabled === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该分类吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await toggleCategory(row.id)
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch {
    // Cancelled or error handled by interceptor
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      categoryName: form.categoryName,
      sort: form.sort
    }
    if (isEdit.value) {
      data.id = editingId.value
      await updateCategory(data)
      ElMessage.success('编辑成功')
    } else {
      await addCategory(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // Error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

// ---- Init ----
onMounted(() => {
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
