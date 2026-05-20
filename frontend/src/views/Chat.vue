<template>
  <div class="chat-container">
    <div class="chat-header">
      <h3>智能对话</h3>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.type]">
        <div class="avatar">
          {{ msg.type === 'user' ? '我' : 'AI' }}
        </div>
        <div class="content">
          <p>{{ msg.content }}</p>
          <div v-if="msg.type === 'ai' && msg.showTransferBtn" class="transfer-btn-container">
            <el-button size="small" type="danger" @click="transferToHuman(msg.question)">
              转人工
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <el-input 
        v-model="inputMessage" 
        placeholder="请输入您的问题..."
        @keyup.enter="sendMessage"
        style="flex: 1"
      />
      <el-button type="primary" @click="sendMessage">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../axios'

const messages = ref([])
const inputMessage = ref('')
const messagesContainer = ref(null)
const STORAGE_KEY = 'chat_messages'

onMounted(() => {
  const savedMessages = localStorage.getItem(STORAGE_KEY)
  if (savedMessages) {
    messages.value = JSON.parse(savedMessages)
  } else {
    messages.value = [
      { type: 'ai', content: '您好！我是 AI Employee，请问有什么可以帮助您的？' }
    ]
  }
})

watch(messages, (newMessages) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(newMessages))
}, { deep: true })

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return
  
  messages.value.push({ type: 'user', content: inputMessage.value })
  const tempMessage = { type: 'ai', content: '', question: inputMessage.value, showTransferBtn: false }
  messages.value.push(tempMessage)
  
  const question = inputMessage.value
  inputMessage.value = ''
  
  await nextTick(() => {
    scrollToBottom()
  })
  
  try {
    const response = await axios.post('/api/chat', {
      Id: 'session-' + Date.now(),
      Question: question
    })
    
    console.log('API 响应:', response)
    
    let answer = '暂无回答'
    let showTransfer = false
    
    if (response.data?.data?.answer) {
      answer = response.data.data.answer
    } else if (response.data?.Answer) {
      answer = response.data.Answer
    } else if (response.data?.answer) {
      answer = response.data.answer
    } else if (typeof response.data === 'string') {
      answer = response.data
    }
    
    showTransfer = true
    
    console.log('提取到的回答:', answer)
    messages.value[messages.value.length - 1].content = answer
    messages.value[messages.value.length - 1].showTransferBtn = showTransfer
  } catch (error) {
    console.error('请求失败:', error)
    tempMessage.content = '抱歉，暂时无法回答您的问题，请稍后重试。\n\n您也可以点击下方按钮转人工服务。'
    tempMessage.showTransferBtn = true
    ElMessage.error('请求失败: ' + (error.response?.data?.message || error.message))
  }
  
  await nextTick(() => {
    scrollToBottom()
  })
}

const transferToHuman = async (question) => {
  try {
    await ElMessageBox.confirm(
      '确定要将此问题转人工处理吗？',
      '确认转人工',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const aiMessage = messages.value[messages.value.length - 1]
    await axios.post('/api/tickets', { question, aiAnswer: aiMessage.content })
    ElMessage.success('已成功提交工单，运维人员会尽快处理！')
    
    messages.value.push({ 
      type: 'ai', 
      content: '您的问题已提交工单，工单编号已生成。运维人员会尽快处理并回复您。' 
    })
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提交工单失败: ' + (error.response?.data?.message || error.response?.data || error.message))
    }
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  padding: 16px;
  background-color: #409eff;
  color: white;
}

.chat-header h3 {
  margin: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f7fa;
}

.message {
  display: flex;
  margin-bottom: 16px;
}

.message.user {
  justify-content: flex-end;
}

.message.user .content {
  background-color: #409eff;
  color: white;
}

.message.ai .content {
  background-color: white;
  color: #333;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 8px;
  font-size: 14px;
  background-color: #ddd;
}

.content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.content p {
  margin: 0;
  word-break: break-all;
}

.chat-input {
  display: flex;
  gap: 12px;
  padding: 16px;
  background-color: white;
  border-top: 1px solid #eee;
}

.transfer-btn-container {
  margin-top: 12px;
  text-align: center;
}
</style>