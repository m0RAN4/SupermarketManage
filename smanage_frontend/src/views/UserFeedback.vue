<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="page-title-icon"><ChatDotSquare /></el-icon>
        用户反馈
      </h2>
      <p class="page-desc" v-if="!isAdmin">提交您的意见、建议和问题，我们将尽快回复</p>
    </div>

    <!-- 搜索栏 (管理员) -->
    <div class="search-bar" v-if="isAdmin">
      <el-select v-model="searchForm.status" placeholder="状态" clearable class="search-select">
        <el-option :value="0" label="未处理" />
        <el-option :value="1" label="已回复" />
      </el-select>
      <el-button type="primary" @click="handleQuery" class="search-btn">查询</el-button>
    </div>

    <!-- 反馈列表 -->
    <div class="table-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%" class="custom-table">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="content" label="反馈内容" min-width="200">
          <template #default="{ row }">
            <div class="feedback-content">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" round>
              {{ row.status === 1 ? '已回复' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回复" min-width="160">
          <template #default="{ row }">
            <div class="reply-content">{{ row.reply || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170" align="center">
          <template #default="{ row }">{{ row.createTime ? row.createTime.replace('T',' ') : '-' }}</template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleReply(row)">
              {{ row.status === 1 ? '编辑回复' : '回复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchData" @size-change="fetchData" background
        />
      </div>
    </div>

    <!-- 提交反馈 (普通用户) -->
    <div class="submit-card" v-if="!isAdmin">
      <div class="submit-header">
        <h3>提交新反馈</h3>
      </div>
      <el-form :model="submitForm" label-width="0">
        <el-form-item>
          <el-input
            v-model="submitForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入您的反馈或建议，我们会认真对待每一条反馈..."
            class="submit-textarea"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" class="submit-btn">
            <el-icon><Promotion /></el-icon> 提交反馈
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" :title="replyFeedbackId?.status === 1 ? '编辑回复' : '回复反馈'" width="500px" class="reply-dialog">
      <div class="original-feedback" v-if="replyFeedbackContent">
        <span class="original-label">原反馈：</span>
        <p>{{ replyFeedbackContent }}</p>
      </div>
      <el-form label-width="0">
        <el-form-item>
          <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="请输入回复内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReply">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, ChatDotSquare } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { getFeedbackPage, submitFeedback, replyFeedback } from '../api/order'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ status: null })
const pagination = reactive({ current: 1, size: 10, total: 0 })

const submitForm = reactive({ content: '' })
const submitting = ref(false)

const replyVisible = ref(false)
const replyFeedbackId = ref(null)
const replyFeedbackContent = ref('')
const replyContent = ref('')

async function fetchData() {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size }
    if (searchForm.status !== null && searchForm.status !== undefined) params.status = searchForm.status
    const res = await getFeedbackPage(params)
    if (res.code === 200) {
      const d = res.data
      tableData.value = d.records || []
      pagination.total = d.total || 0
    }
  } catch (e) { ElMessage.error(e?.message || '查询失败') }
  finally { loading.value = false }
}

function handleQuery() { pagination.current = 1; fetchData() }

async function handleSubmit() {
  if (!submitForm.content.trim()) { ElMessage.warning('请输入反馈内容'); return }
  submitting.value = true
  try {
    await submitFeedback({ content: submitForm.content })
    ElMessage.success('反馈已提交，感谢您的宝贵意见！')
    submitForm.content = ''
    fetchData()
  } catch (e) { ElMessage.error(e?.message || '提交失败') }
  finally { submitting.value = false }
}

function handleReply(row) {
  replyFeedbackId.value = row.id
  replyFeedbackContent.value = row.content
  replyContent.value = row.reply || ''
  replyVisible.value = true
}

async function confirmReply() {
  if (!replyContent.value.trim()) { ElMessage.warning('请输入回复内容'); return }
  try {
    await replyFeedback({ feedbackId: replyFeedbackId.value, reply: replyContent.value })
    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error(e?.message || '回复失败') }
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-container { max-width: 1000px; margin: 0 auto; }

.page-header { margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #1a1a2e; display: flex; align-items: center; gap: 8px; }
.page-title-icon { color: #43a047; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #909399; }

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  padding: 14px 18px;
  border-radius: 14px;
  margin-bottom: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}
.search-select { width: 140px; }

/* 表格卡片 */
.table-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}

.custom-table :deep(.el-table__header th) { background: #fafafa; font-weight: 600; }
.feedback-content { font-size: 14px; line-height: 1.5; }
.reply-content { color: #43a047; font-size: 13px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }

/* 提交卡片 */
.submit-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 20px 24px;
  margin-top: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
}
.submit-header h3 { margin: 0 0 12px; font-size: 16px; font-weight: 600; color: #1a1a2e; }
.submit-textarea :deep(.el-textarea__inner) { border-radius: 12px; font-size: 14px; line-height: 1.6; }
.submit-btn { border-radius: 12px; font-weight: 600; }

/* 回复弹窗 */
.original-feedback {
  background: #f5f7fa;
  padding: 12px 14px;
  border-radius: 10px;
  margin-bottom: 16px;
}
.original-label { font-size: 12px; color: #909399; }
.original-feedback p { margin: 6px 0 0; font-size: 14px; color: #303133; }
</style>
