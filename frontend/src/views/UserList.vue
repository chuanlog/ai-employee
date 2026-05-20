<template>
  <div class="user-list">
    <div class="header">
      <h3>用户管理</h3>
      <el-button type="primary" @click="showAddModal = true" v-if="isAdmin">添加用户</el-button>
    </div>
    
    <el-table :data="users" border>
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="role" label="角色">
        <template #default="scope">
          {{ scope.row.role === 'admin' || scope.row.role === 1 ? '管理员' : '普通用户' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          {{ scope.row.status === 1 ? '启用' : '禁用' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="editUser(scope.row)" 
            :disabled="!isAdmin && scope.row.id !== currentUserId">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteUser(scope.row)" v-if="isAdmin">删除</el-button>
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

    <el-dialog v-model="showAddModal" :title="dialogTitle" @close="resetForm">
      <el-form :model="form" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!isEdit">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username" v-else>
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role" v-if="isAdmin">
          <el-select v-model="form.role">
            <el-option value="1" label="管理员" />
            <el-option value="2" label="普通用户" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="isEdit && isAdmin">
          <el-select v-model="form.status">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../axios'

const users = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showAddModal = ref(false)
const formRef = ref(null)
const isEdit = ref(false)

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const currentUserId = currentUser.id
const isAdmin = computed(() => {
  return currentUser.role === 'admin' || currentUser.role === 1
})

const form = reactive({
  id: null,
  username: '',
  password: '',
  email: '',
  phone: '',
  role: '2',
  status: 1
})

const dialogTitle = computed(() => {
  return isEdit.value ? '编辑用户' : '添加用户'
})

onMounted(() => {
  loadUsers()
})

const loadUsers = async () => {
  try {
    const response = await axios.get(`/api/users?pageNum=${pageNum.value}&pageSize=${pageSize.value}`)
    if (isAdmin.value) {
      users.value = response.data.records
      total.value = response.data.total
    } else {
      users.value = response.data.records.filter(u => u.id === currentUserId)
      total.value = users.value.length
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败: ' + (error.response?.data?.message || error.message))
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadUsers()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  loadUsers()
}

const resetForm = () => {
  form.id = null
  form.username = ''
  form.password = ''
  form.email = ''
  form.phone = ''
  form.role = '2'
  form.status = 1
  isEdit.value = false
}

const editUser = (user) => {
  form.id = user.id
  form.username = user.username
  form.password = ''
  form.email = user.email || ''
  form.phone = user.phone || ''
  form.role = String(user.role) || '2'
  form.status = user.status !== undefined ? user.status : 1
  isEdit.value = true
  showAddModal.value = true
}

const saveUser = async () => {
  try {
    if (isEdit.value) {
      const data = {
        password: form.password || undefined,
        email: form.email,
        phone: form.phone
      }
      if (isAdmin.value) {
        data.role = form.role
        data.status = form.status
      }
      await axios.put(`/api/users/${form.id}`, data)
      ElMessage.success('更新成功')
    } else {
      const data = {
        username: form.username,
        password: form.password,
        email: form.email,
        phone: form.phone,
        role: form.role
      }
      await axios.post('/api/users', data)
      ElMessage.success('添加成功')
    }
    showAddModal.value = false
    resetForm()
    loadUsers()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.response?.data || error.message))
  }
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await axios.delete(`/api/users/${user.id}`)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.response?.data || error.message))
    }
  }
}
</script>

<style scoped>
.user-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>