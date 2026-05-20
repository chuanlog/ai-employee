<template>
  <div class="my-tickets">
    <div class="header">
      <h3>我的工单</h3>
      <el-select v-model="filterStatus" placeholder="全部状态" @change="loadTickets">
        <el-option :value="null" label="全部" />
        <el-option :value="1" label="待处理" />
        <el-option :value="3" label="已完成" />
      </el-select>
    </div>
    
    <el-table :data="tickets" border>
      <el-table-column prop="id" label="工单ID" width="100" />
      <el-table-column prop="question" label="问题" show-overflow-tooltip />
      <el-table-column prop="statusText" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ scope.row.statusText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button size="small" @click="viewTicket(scope.row)">查看</el-button>
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
        <el-descriptions-item label="提交时间">{{ currentTicket.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '../axios'

const tickets = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)
const showViewModal = ref(false)
const currentTicket = ref(null)

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

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
    let url = `/api/tickets?pageNum=${pageNum.value}&pageSize=${pageSize.value}&userId=${currentUser.id}`
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
</script>

<style scoped>
.my-tickets {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
