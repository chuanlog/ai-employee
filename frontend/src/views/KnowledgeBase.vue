<template>
  <div class="knowledge-base">
    <div class="header">
      <div>
        <h3>知识库管理</h3>
        <p class="subtitle">管理员可在此维护知识库文档，并在确认文档集后重建向量数据库。</p>
      </div>
      <div class="actions">
        <el-button @click="loadDocuments">刷新</el-button>
        <el-button type="primary" @click="triggerFilePicker('upload')">上传文档</el-button>
        <el-button type="danger" :loading="rebuildLoading" @click="handleRebuild">重建向量数据库</el-button>
      </div>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      class="hidden-input"
      accept=".txt,.md"
      @change="handleFileChange"
    />

    <el-alert
      v-if="!isAdmin"
      title="当前账号不是管理员，无法管理知识库。"
      type="warning"
      :closable="false"
      class="mb16"
    />

    <el-table v-loading="loading" :data="documents" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="fileName" label="文档名称" min-width="220" show-overflow-tooltip />
      <el-table-column label="文件大小" width="120">
        <template #default="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="上传人" width="140">
        <template #default="scope">
          {{ scope.row.uploaderName || scope.row.uploaderId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">
            {{ formatStatus(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="错误信息" min-width="220" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.errorMessage || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="viewDocument(scope.row)">查看</el-button>
          <el-button size="small" type="primary" @click="triggerFilePicker('replace', scope.row)">替换</el-button>
          <el-button size="small" type="danger" @click="deleteDocument(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="pageNum"
      :page-sizes="[10, 20, 30]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    />

    <el-dialog v-model="detailVisible" title="文档详情" width="680px">
      <el-descriptions v-if="selectedDocument" :column="1" border>
        <el-descriptions-item label="文档名称">{{ selectedDocument.fileName }}</el-descriptions-item>
        <el-descriptions-item label="对象 Key">{{ selectedDocument.objectKey }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(selectedDocument.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="上传人">
          {{ selectedDocument.uploaderName || selectedDocument.uploaderId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">{{ formatStatus(selectedDocument.status) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(selectedDocument.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(selectedDocument.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ selectedDocument.errorMessage || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../axios'

const documents = ref([])
const loading = ref(false)
const rebuildLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const selectedDocument = ref(null)
const fileInputRef = ref(null)
const uploadMode = ref('upload')
const replaceTarget = ref(null)

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const isAdmin = computed(() => currentUser.role === 'admin' || currentUser.role === 1)

onMounted(() => {
  if (isAdmin.value) {
    loadDocuments()
  }
})

const loadDocuments = async () => {
  if (!isAdmin.value) {
    return
  }
  loading.value = true
  try {
    const { data } = await axios.get('/api/knowledge-base/documents', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    documents.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载知识库文档失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

const viewDocument = async (row) => {
  try {
    const { data } = await axios.get(`/api/knowledge-base/documents/${row.id}`)
    selectedDocument.value = data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('加载文档详情失败: ' + (error.response?.data?.message || error.message))
  }
}

const triggerFilePicker = (mode, row = null) => {
  uploadMode.value = mode
  replaceTarget.value = row
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
    fileInputRef.value.click()
  }
}

const handleFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    if (uploadMode.value === 'replace' && replaceTarget.value) {
      const { data } = await axios.put(`/api/knowledge-base/documents/${replaceTarget.value.id}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      ElMessage.success(data.message || '文档替换成功')
    } else {
      const { data } = await axios.post('/api/knowledge-base/documents', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      ElMessage.success(data.message || '文档上传成功')
    }
    await loadDocuments()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
  } finally {
    uploadMode.value = 'upload'
    replaceTarget.value = null
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
}

const deleteDocument = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库文档 "${row.fileName}" 吗？删除后会同时清理 MinIO 对象与对应向量数据。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const { data } = await axios.delete(`/api/knowledge-base/documents/${row.id}`)
    ElMessage.success(data.message || '删除成功')
    await loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const handleRebuild = async () => {
  try {
    await ElMessageBox.confirm(
      '该操作会清空当前 Milvus 向量集合，并基于当前知识库中的全部文档重新构建索引。确定继续吗？',
      '确认重建',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    rebuildLoading.value = true
    const { data } = await axios.post('/api/knowledge-base/rebuild')
    const result = data.data || {}
    const summary = `重建完成：共 ${result.totalDocuments || 0} 个文档，成功 ${result.successCount || 0} 个，失败 ${result.failCount || 0} 个`
    ElMessage.success(summary)
    if ((result.failCount || 0) > 0) {
      ElMessage.warning((result.failedDocuments || []).join('；'))
    }
    await loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重建失败: ' + (error.response?.data?.message || error.message))
    }
  } finally {
    rebuildLoading.value = false
  }
}

const handleSizeChange = (value) => {
  pageSize.value = value
  loadDocuments()
}

const handleCurrentChange = (value) => {
  pageNum.value = value
  loadDocuments()
}

const formatFileSize = (size) => {
  if (size === null || size === undefined) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const formatStatus = (status) => {
  if (status === 'INDEXED') return '已索引'
  if (status === 'ERROR') return '索引失败'
  if (status === 'PENDING') return '待处理'
  return status || '-'
}

const statusTagType = (status) => {
  if (status === 'INDEXED') return 'success'
  if (status === 'ERROR') return 'danger'
  return 'warning'
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}
</script>

<style scoped>
.knowledge-base {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.subtitle {
  margin: 6px 0 0;
  color: #606266;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.hidden-input {
  display: none;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
