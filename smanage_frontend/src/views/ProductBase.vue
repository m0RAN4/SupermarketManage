<template>
  <div class="page-container">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入商品名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select
            v-model="searchForm.categoryId"
            placeholder="请选择分类"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增商品</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="图片" width="90" align="center">
          <template #default="{ row }">
            <PositionedImage
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :cropData="row.cropData"
              :aspectRatio="(16/9)"
              style="width:72px;border-radius:4px"
            />
            <span v-else style="color:#c0c4cc;font-size:12px">暂无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="categoryName" label="所属分类" width="150" align="center" />
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
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="500px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-select
            v-model="form.categoryId"
            placeholder="请选择分类"
            style="width: 100%"
          >
            <el-option
              v-for="item in dialogCategoryOptions"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品图片">
          <div style="display:flex;flex-direction:column;gap:8px;width:100%">
            <el-input v-model="form.imageUrl" placeholder="可粘贴图片链接，或点击上传按钮" clearable />
            <div style="display:flex;align-items:center;gap:12px">
              <el-upload
                :show-file-list="false"
                :http-request="customUpload"
                :before-upload="beforeUpload"
                accept="image/*"
              >
                <el-button type="primary" plain size="small" :loading="uploadLoading">上传图片</el-button>
              </el-upload>
              <span style="color:#909399;font-size:12px">支持 jpg/png，最大 5MB</span>
            </div>
            <PositionedImage
              v-if="form.imageUrl"
              :src="form.imageUrl"
              :cropData="form.cropData"
              :aspectRatio="(16/9)"
              style="width:160px;height:90px;border:1px solid #dcdfe6;border-radius:4px"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Image Cropper -->
    <ImageCropper ref="cropperRef" :aspect-ratio="16/9" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit } from '@element-plus/icons-vue'
import request from '../api/index'
import { getProductBasePage, addProductBase, updateProductBase, toggleProductBase } from '../api/product'
import { getCategoryPage } from '../api/product'
import ImageCropper from '../components/ImageCropper.vue'
import PositionedImage from '../components/PositionedImage.vue'

// ---- Category Options ----
const categoryOptions = ref([])
const dialogCategoryOptions = ref([])

async function loadCategories() {
  try {
    const res = await getCategoryPage({ current: 1, size: 1000 })
    const records = (res.data?.records || []).filter(item => item.isEnabled === 1)
    categoryOptions.value = records
    dialogCategoryOptions.value = records
  } catch {
    categoryOptions.value = []
    dialogCategoryOptions.value = []
  }
}

// ---- Search Form ----
const searchForm = reactive({
  keyword: '',
  categoryId: null
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
  categoryId: null,
  productName: '',
  imageUrl: '',
  cropData: null
})

const rules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

// ---- Image Upload ----
const uploadLoading = ref(false)
const cropperRef = ref(null)
let pendingCropResult = null

async function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('仅支持上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  // 打开定位弹窗
  const result = await cropperRef.value?.open(file)
  if (!result) return false
  pendingCropResult = result
  return true
}

async function customUpload(options) {
  uploadLoading.value = true
  try {
    const { file, cropData } = pendingCropResult || { file: options.file, cropData: null }
    const formData = new FormData()
    formData.append('file', file)
    if (cropData) formData.append('cropData', cropData)
    const res = await request.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200 && res.data) {
      form.imageUrl = '/api/picture/' + (res.data.url || res.data)
      form.cropData = res.data.cropData ?? cropData
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
      options.onError()
    }
  } catch (e) {
    ElMessage.error(e?.message || '图片上传失败，请重试')
    options.onError()
  } finally {
    uploadLoading.value = false
    pendingCropResult = null
  }
}

// ---- Methods ----
async function fetchData() {
  loading.value = true
  try {
    const res = await getProductBasePage({
      current: pagination.current,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      categoryId: searchForm.categoryId || undefined
    })
    const data = res.data
    tableData.value = data.records || []
    pagination.total = data.total || 0
    pagination.current = data.current || pagination.current
    pagination.size = data.size || pagination.size
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
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
  searchForm.categoryId = null
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
  form.categoryId = row.categoryId
  form.productName = row.productName
  form.imageUrl = row.imageUrl || ''
  form.cropData = row.cropData || null
  dialogVisible.value = true
}

function handleDialogClosed() {
  resetForm()
  formRef.value?.resetFields()
}

function resetForm() {
  form.categoryId = null
  form.productName = ''
  form.imageUrl = ''
  form.cropData = null
}

async function handleToggle(row) {
  const action = row.isEnabled === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该商品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await toggleProductBase(row.id)
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '操作失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      categoryId: form.categoryId,
      productName: form.productName,
      imageUrl: form.imageUrl || null,
      cropData: form.cropData || null
    }
    if (isEdit.value) {
      data.id = editingId.value
      await updateProductBase(data)
      ElMessage.success('编辑成功')
    } else {
      await addProductBase(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ---- Init ----
onMounted(() => {
  loadCategories()
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
