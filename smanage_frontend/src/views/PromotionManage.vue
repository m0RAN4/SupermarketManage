<template>
  <div class="promotion-container">
    <div class="page-header">
      <h2>宣传管理</h2>
      <el-button type="primary" @click="showAddDialog">添加宣传图片</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="预览" width="150">
        <template #default="{ row }">
          <PositionedImage
            v-if="row.imageUrl"
            :src="getImageUrl(row.imageUrl)"
            :cropData="row.cropData"
            :aspectRatio="(16/9)"
            style="width:160px;height:90px"
          />
          <span v-else class="no-image">暂无图片</span>
        </template>
      </el-table-column>
      <el-table-column prop="imageUrl" label="图片URL" min-width="250" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
            {{ row.isActive === 1 ? '启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            :type="row.isActive === 1 ? 'warning' : 'success'"
            size="small"
            @click="handleToggle(row)"
          >
            {{ row.isActive === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadData"
      style="margin-top:16px;justify-content:flex-end"
    />

    <!-- Add Dialog -->
    <el-dialog v-model="dialogVisible" title="添加宣传图片" width="500px" @close="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="图片URL">
          <el-input v-model="form.imageUrl" placeholder="请输入宣传图片URL" />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            :http-request="customUpload"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept="image/*"
          >
            <el-button type="primary">选择图片上传</el-button>
            <template #tip>
              <div class="el-upload__tip">上传后自动填充图片URL，也可手动填写</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="form.imageUrl" label="图片预览">
          <PositionedImage
            :src="getImageUrl(form.imageUrl)"
            :cropData="form.cropData"
            :aspectRatio="(16/9)"
            style="width:240px;height:135px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="submitting">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- Image Cropper -->
    <ImageCropper ref="cropperRef" :aspect-ratio="16/9" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPromotionPage, addPromotion, togglePromotion, deletePromotion } from '../api/product'
import request from '../api/index'
import ImageCropper from '../components/ImageCropper.vue'
import PositionedImage from '../components/PositionedImage.vue'

const tableData = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const submitting = ref(false)
const form = ref({ imageUrl: '' })

const cropperRef = ref(null)

function getImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api/picture/')) return url
  return '/api/picture/' + url
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPromotionPage({ current: current.value, size: size.value })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function showAddDialog() {
  form.value = { imageUrl: '', cropData: null }
  dialogVisible.value = true
}

function resetForm() {
  form.value = { imageUrl: '', cropData: null }
}

let pendingCropResult = null

async function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  // 打开定位弹窗，获取展示位置数据（不裁剪原图）
  const result = await cropperRef.value?.open(file)
  if (!result) return false
  pendingCropResult = result
  return true
}

async function customUpload(options) {
  try {
    const { file, cropData } = pendingCropResult || { file: options.file, cropData: null }
    const formData = new FormData()
    formData.append('file', file)
    if (cropData) formData.append('cropData', cropData)
    const res = await request.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200 && res.data) {
      form.value.imageUrl = res.data.url || res.data
      form.value.cropData = res.data.cropData ?? cropData
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
      options.onError()
    }
  } catch (e) {
    ElMessage.error('上传失败')
    options.onError()
  } finally {
    pendingCropResult = null
  }
}

async function handleAdd() {
  if (!form.value.imageUrl.trim()) {
    ElMessage.warning('请输入或上传图片URL')
    return
  }
  submitting.value = true
  try {
    const res = await addPromotion({
      imageUrl: form.value.imageUrl,
      cropData: form.value.cropData || null
    })
    if (res.code === 200) {
      ElMessage.success('添加成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}

async function handleToggle(row) {
  const action = row.isActive === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}该宣传图片？`, '提示', { type: 'warning' })
    const res = await togglePromotion(row.id)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      loadData()
    } else {
      ElMessage.error(res.message || `${action}失败`)
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(`${action}失败`)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该宣传图片？删除后管理员端也不可见，仅数据库留存。', '确认删除', { type: 'warning' })
    const res = await deletePromotion(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.promotion-container { padding: 10px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.no-image { color: #c0c4cc; font-size: 12px; }
.image-slot { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; background: #f5f7fa; color: #c0c4cc; font-size: 12px; }
</style>
