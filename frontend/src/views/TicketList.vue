<template>
  <div class="ticket-list">
    <div class="header">
      <h3>工单管理</h3>
      <el-select v-model="filterStatus" placeholder="全部状态" @change="loadTickets">
        <el-option :value="null" label="全部" />
        <el-option :value="1" label="待处理" />
        <el-option :value="3" label="已完成" />
      </el-select>
    </div>
    
    <el-table :data="tickets" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="question" label="问题" show-overflow-tooltip />
      <el-table-column prop="statusText" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ scope.row.statusText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handlerName" label="处理人" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button size="small" @click="viewTicket(scope.row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleTicket(scope.row)" v-if="isAdmin && scope.row.status !== 3">处理</el-button>
          <el-button size="small" type="danger" @click="deleteTicket(scope.row)" v-if="isAdmin && scope.row.status === 3">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="pageNum"
      :page-sizes="[10, 20, 30]"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total">
    </el-pagination>

    <el-dialog v-model="showViewModal" title="工单详情" width="60%">
      <el-descriptions :column="1" border v-if="currentTicket">
        <el-descriptions-item label="工单ID">{{ currentTicket.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentTicket.username }}</el-descriptions-item>
        <el-descriptions-item label="问题内容">{{ currentTicket.question }}</el-descriptions-item>
        <el-descriptions-item label="AI回答" v-if="currentTicket.aiAnswer">
          {{ currentTicket.aiAnswer }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentTicket.status)">
            {{ currentTicket.statusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理人" v-if="currentTicket.handlerName">
          {{ currentTicket.handlerName }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间" v-if="currentTicket.handledAt">
          {{ currentTicket.handledAt }}
        </el-descriptions-item>
        <el-descriptions-item label="处理结果" v-if="currentTicket.result">
          {{ currentTicket.result }}
        </el-descriptions-item>
        <el-descriptions-item label="回访记录" v-if="currentTicket.followUp">
          {{ currentTicket.followUp }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTicket.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="showHandleModal" title="处理工单" width="50%">
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="问题">
          <el-input v-model="currentTicket.question" type="textarea" rows="3" disabled />
        </el-form-item>
        <el-form-item label="AI回答" v-if="currentTicket.aiAnswer">
          <el-input v-model="currentTicket.aiAnswer" type="textarea" rows="4" disabled />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="handleForm.result" type="textarea" rows="4" placeholder="请输入处理结果" />
        </el-form-item>
        <el-form-item label="回访记录">
          <el-input v-model="handleForm.followUp" type="textarea" rows="3" placeholder="请输入回访记录（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandleModal = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../axios'

const tickets = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)
const showViewModal = ref(false)
const showHandleModal = ref(false)
const currentTicket = ref(null)

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const isAdmin = computed(() => {
  return currentUser.role === 'admin' || currentUser.role === 1
})

const handleForm = reactive({
  result: '',
  followUp: ''
})

const getStatusType = (status) => {
  switch (status) {
    case 1:
      return 'warning'
    case 2:
      return 'primary'
    case 3:
      return 'success'
    default:
      return 'info'
  }
}

onMounted(() => {
  loadTickets()
})

const loadTickets = async () => {
  try {
    let url = `/api/tickets?pageNum=${pageNum.value}&pageSize=${pageSize.value}`
    if (filterStatus.value) {
      url += `&status=${filterStatus.value}`
    }
    const response = await axios.get(url)
    tickets.value = response.data.records
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('加载工单列表失败: ' + (error.response?.data?.message || error.message))
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadTickets()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  loadTickets()
}

const viewTicket = (ticket) => {
  currentTicket.value = ticket
  showViewModal.value = true
}

const handleTicket = (ticket) => {
  currentTicket.value = ticket
  handleForm.result = ''
  handleForm.followUp = ''
  showHandleModal.value = true
}

const submitHandle = async () => {
  if (!handleForm.result) {
    ElMessage.warning('请输入处理结果')
    return
  }
  try {
    await axios.put(`/api/tickets/${currentTicket.value.id}/handle`, handleForm)
    ElMessage.success('处理成功')
    showHandleModal.value = false
    loadTickets()
  } catch (error) {
    ElMessage.error('处理失败: ' + (error.response?.data?.message || error.response?.data || error.message))
  }
}

const deleteTicket = async (ticket) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个工单吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await axios.delete(`/api/tickets/${ticket.id}`)
    ElMessage.success('删除成功')
    loadTickets()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.response?.data || error.message))
    }
  }
}
</script>

<style scoped>
.ticket-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
