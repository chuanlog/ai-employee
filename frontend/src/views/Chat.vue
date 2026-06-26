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
          <div class="markdown-body" v-html="msg.content ? marked.parse(msg.content) : ''"></div>
          <div v-if="msg.type === 'ai' && msg.showTransferBtn" class="transfer-btn-container">
            <el-button size="small" type="danger" @click="transferToHuman(msg, index)">
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
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../axios'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

marked.setOptions({
  highlight: function (code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  breaks: true,
})

const messages = ref([])
const inputMessage = ref('')
const messagesContainer = ref(null)

const buildHistoryMessages = (historyMessages) => {
  let latestUserQuestion = ''
  return historyMessages.map((msg) => {
    const isAI = msg.role === 'assistant'
    if (!isAI) {
      latestUserQuestion = msg.content || ''
    }

    return {
      type: isAI ? 'ai' : 'user',
      content: msg.content,
      question: isAI ? (msg.question || latestUserQuestion) : '',
      showTransferBtn: isAI
    }
  })
}

const findPreviousQuestion = (messageIndex) => {
  for (let index = messageIndex - 1; index >= 0; index -= 1) {
    const message = messages.value[index]
    if (message?.type === 'user' && message.content?.trim()) {
      return message.content
    }
  }
  return ''
}

onMounted(async () => {
  try {
    const res = await axios.get('/api/chat/history')
    if (res.data?.data && Array.isArray(res.data.data) && res.data.data.length > 0) {
      messages.value = buildHistoryMessages(res.data.data)
    } else {
      messages.value = [
        { type: 'ai', content: '您好！我是 AI Employee，请问有什么可以帮助您的？' }
      ]
    }
    await nextTick(() => {
      scrollToBottom()
    })
  } catch (error) {
    console.error('加载历史记录失败:', error)
    messages.value = [
      { type: 'ai', content: '您好！我是 AI Employee，请问有什么可以帮助您的？' }
    ]
  }
})

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
      Question: question
    })
    
    console.log('API 响应:', response)
    
    let answer = '暂无回答'
    if (response.data?.data?.answer) {
      answer = response.data.data.answer
    } else if (response.data?.Answer) {
      answer = response.data.Answer
    } else if (response.data?.answer) {
      answer = response.data.answer
    } else if (typeof response.data === 'string') {
      answer = response.data
    }
    
    console.log('提取到的回答:', answer)
    const index = messages.value.length - 1
    messages.value[index] = {
      ...messages.value[index],
      content: answer,
      showTransferBtn: true
    }
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

const transferToHuman = async (message, messageIndex) => {
  try {
    const question = (message.question || findPreviousQuestion(messageIndex)).trim()
    if (!question) {
      ElMessage.warning('未找到对应的问题内容，请重新发送问题后再转人工')
      return
    }

    await ElMessageBox.confirm(
      '确定要将此问题转人工处理吗？',
      '确认转人工',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.post('/api/tickets', { question, aiAnswer: message.content || '' })
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
  overflow-x: auto;
}

.markdown-body {
  background: transparent !important;
  font-family: inherit !important;
  font-size: 14px !important;
  line-height: 1.6 !important;
  color: #333 !important;
}

.markdown-body * {
  color: inherit !important;
}

.markdown-body p,
.markdown-body li,
.markdown-body span,
.markdown-body strong,
.markdown-body em {
  color: #333 !important;
}

/* Fix dark table styles for better readability - HIGH PRIORITY OVERRIDE */
.chat-container .chat-messages .message .content table,
.chat-container .chat-messages .message .content .markdown-body table,
.chat-container .chat-messages .message .content > table,
.chat-container .chat-messages .message .content :deep(table) {
  background-color: #ffffff !important;
  border-collapse: collapse !important;
  width: 100% !important;
  font-size: 13px !important;
  box-shadow: none !important;
}

.chat-container .chat-messages .message .content table th,
.chat-container .chat-messages .message .content table td,
.chat-container .chat-messages .message .content .markdown-body table th,
.chat-container .chat-messages .message .content .markdown-body table td,
.chat-container .chat-messages .message .content > table th,
.chat-container .chat-messages .message .content > table td,
.chat-container .chat-messages .message .content :deep(table th),
.chat-container .chat-messages .message .content :deep(table td) {
  color: #333333 !important;
  background-color: #ffffff !important;
  border: 1px solid #e0e0e0 !important;
  padding: 10px 14px !important;
  text-align: left !important;
  font-weight: normal !important;
}

.chat-container .chat-messages .message .content table th,
.chat-container .chat-messages .message .content .markdown-body table th,
.chat-container .chat-messages .message .content > table th,
.chat-container .chat-messages .message .content :deep(table th) {
  background-color: #f8f9fa !important;
  font-weight: 600 !important;
  color: #212529 !important;
}

.chat-container .chat-messages .message .content table tr,
.chat-container .chat-messages .message .content .markdown-body table tr,
.chat-container .chat-messages .message .content > table tr,
.chat-container .chat-messages .message .content :deep(table tr) {
  background-color: #ffffff !important;
}

.chat-container .chat-messages .message .content table tr:nth-child(even) td,
.chat-container .chat-messages .message .content .markdown-body table tr:nth-child(even) td,
.chat-container .chat-messages .message .content > table tr:nth-child(even) td,
.chat-container .chat-messages .message .content :deep(table tr:nth-child(even) td) {
  background-color: #fafafa !important;
}

.chat-container .chat-messages .message .content table tr:hover td,
.chat-container .chat-messages .message .content .markdown-body table tr:hover td {
  background-color: #f0f0f0 !important;
}

/* Code block styles */
.content pre,
.content code,
.content .markdown-body pre,
.content .markdown-body code {
  background-color: #1e1e1e !important;
  color: #d4d4d4 !important;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 13px !important;
}

.content pre,
.content .markdown-body pre {
  padding: 16px !important;
  border-radius: 8px !important;
  overflow-x: auto !important;
  line-height: 1.5 !important;
  white-space: pre-wrap !important;
  word-wrap: break-word !important;
}

.content code,
.content .markdown-body code {
  padding: 3px 6px !important;
  border-radius: 4px !important;
  font-size: 12px !important;
}

.content pre code,
.content .markdown-body pre code {
  padding: 0 !important;
  font-size: 13px !important;
}

/* Syntax highlighting colors */
.content .hljs,
.content .markdown-body .hljs {
  background: transparent !important;
}

.content .hljs-keyword,
.content .markdown-body .hljs-keyword {
  color: #569cd6 !important;
}

.content .hljs-string,
.content .markdown-body .hljs-string {
  color: #ce9178 !important;
}

.content .hljs-number,
.content .markdown-body .hljs-number {
  color: #b5cea8 !important;
}

.content .hljs-comment,
.content .markdown-body .hljs-comment {
  color: #6a9955 !important;
}

.content .hljs-function,
.content .markdown-body .hljs-function {
  color: #dcdcaa !important;
}

/* Fix inline code in user messages */
/* User message styles override */
.message.user .markdown-body {
  color: white !important;
}

.message.user .markdown-body table {
  background-color: rgba(255,255,255,0.1) !important;
}

.message.user .markdown-body table th,
.message.user .markdown-body table td {
  color: white !important;
  background-color: rgba(255,255,255,0.1) !important;
  border-color: rgba(255,255,255,0.2) !important;
}

.message.user .markdown-body table th {
  background-color: rgba(255,255,255,0.15) !important;
}

.message.user .markdown-body table tr:nth-child(even) td {
  background-color: rgba(255,255,255,0.05) !important;
}

.message.user .markdown-body pre,
.message.user .markdown-body pre code {
  background-color: rgba(0,0,0,0.3) !important;
  color: #fff !important;
}
.message.user .markdown-body * {
  color: white !important;
}
.message.user .markdown-body p, 
.message.user .markdown-body li,
.message.user .markdown-body span,
.message.user .markdown-body strong,
.message.user .markdown-body em {
  color: white !important;
}
.message.user .markdown-body code {
  color: #333 !important;
  background-color: rgba(255,255,255,0.2) !important;
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
