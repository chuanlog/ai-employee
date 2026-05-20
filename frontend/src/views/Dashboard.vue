<template>
  <el-container style="height: 100vh; border: 1px solid #eee">
    <el-aside width="200px" style="background-color: rgb(240, 242, 245)">
      <el-menu :default-active="activeMenu" class="el-menu-vertical-demo" @select="handleMenuSelect">
        <el-menu-item index="chat">
          <span>📧 智能对话</span>
        </el-menu-item>
        <el-menu-item index="my-tickets">
          <span>📋 我的工单</span>
        </el-menu-item>
        <el-menu-item index="users">
          <span>👤 用户管理</span>
        </el-menu-item>
        <el-menu-item index="tickets" v-if="isAdmin">
          <span>🎫 工单管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="text-align: right; font-size: 12px">
        <span>欢迎, {{ user?.username }}</span>
        <el-button type="text" @click="logout" style="margin-left: 20px">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import router from '../router'

const activeMenu = ref('chat')
const user = ref(null)

const isAdmin = computed(() => {
  return user.value?.role === 'admin' || user.value?.role === 1
})

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }
})

const handleMenuSelect = (key) => {
  activeMenu.value = key
  router.push(`/dashboard/${key}`)
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('chat_messages')
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.el-header {
  background-color: #b3c0d1;
  color: #333;
  line-height: 60px;
}

.el-aside {
  color: #333;
}
</style>