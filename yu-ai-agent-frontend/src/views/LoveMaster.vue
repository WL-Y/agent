<template>
  <div class="love-master-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">AI恋爱大师</h1>
      <div class="chat-id">会话ID: {{ chatId }}</div>
    </div>

    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom
          ref="chatRoomRef"
          :messages="messages"
          :connection-status="connectionStatus"
          :active-chat-id="chatId"
          ai-type="love"
          @send-message="sendMessage"
          @load-session="handleLoadSession"
          @new-chat="handleNewChat"
        />
      </div>
    </div>

    <div class="footer-container">
      <AppFooter />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithLoveApp, chatWithLoveAppImage } from '../api'

// 设置页面标题和元数据
useHead({
  title: 'AI恋爱大师 - wwwwAI超级智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'AI恋爱大师是wwwwAI超级智能体应用平台的专业情感顾问，帮你解答各种恋爱问题，提供情感建议'
    },
    {
      name: 'keywords',
      content: 'AI恋爱大师,情感顾问,恋爱咨询,AI聊天,情感问题,wwww,AI智能体'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
const chatRoomRef = ref(null)
let eventSource = null

// localStorage key
const STORAGE_KEY = 'love_master_sessions'

// 从localStorage加载会话列表
const loadSessions = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

// 保存会话列表到localStorage
const saveSessions = (sessions) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions))
  } catch {
    // ignore storage errors
  }
}

// 计算属性：获取当前会话的标题（取第一条用户消息的前20个字符）
const currentSessionTitle = computed(() => {
  const userMsg = messages.value.find(m => m.isUser)
  if (userMsg) {
    return userMsg.content.substring(0, 20) + (userMsg.content.length > 20 ? '...' : '')
  }
  return '新对话'
})

// 生成随机会话ID
const generateChatId = () => {
  return 'love_' + Math.random().toString(36).substring(2, 10)
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'

  return (date.getMonth() + 1) + '/' + date.getDate() + ' ' + date.getHours() + ':' + String(date.getMinutes()).padStart(2, '0')
}

// 添加消息到列表
const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

// 保存当前会话到localStorage
const saveCurrentSession = () => {
  const sessions = loadSessions()
  const existingIndex = sessions.findIndex(s => s.id === chatId.value)

  const sessionData = {
    id: chatId.value,
    title: currentSessionTitle.value,
    messages: messages.value,
    time: new Date().getTime()
  }

  if (existingIndex >= 0) {
    sessions[existingIndex] = sessionData
  } else {
    sessions.unshift(sessionData)
  }

  // 最多保存20个会话
  if (sessions.length > 20) {
    sessions.pop()
  }

  saveSessions(sessions)
  // 通知 ChatRoom 刷新历史记录
  if (chatRoomRef.value) {
    chatRoomRef.value.loadAllSessions()
  }
}

// 处理从 ChatRoom 加载会话的事件
const handleLoadSession = (session) => {
  try {
    // 关闭旧连接
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    connectionStatus.value = 'disconnected'
    chatId.value = session.id || ''
    // 使用深拷贝防止响应式污染
    const msgs = Array.isArray(session.messages) ? session.messages : []
    messages.value = msgs.map(m => ({
      content: String(m.content || ''),
      isUser: Boolean(m.isUser),
      time: Number(m.time) || Date.now()
    }))
  } catch (e) {
    console.error('加载会话失败:', e)
    messages.value = []
  }
}

// 处理新建对话事件
const handleNewChat = () => {
  // 先保存当前会话
  if (messages.value.length > 1) {
    saveCurrentSession()
  }

  // 重置
  chatId.value = generateChatId()
  messages.value = []
  addMessage('欢迎来到AI恋爱大师，请告诉我你的恋爱问题，我会尽力给予帮助和建议。', false)
}

// 发送消息
const sendMessage = (payload) => {
  let message, formData

  if (payload && payload.type === 'image') {
    // 图片消息
    formData = payload.data
    message = formData.get('message')
  } else {
    // 文字消息
    message = payload
  }

  addMessage(message, true)

  // 连接SSE
  if (eventSource) {
    eventSource.close()
  }

  // 创建一个空的AI回复消息
  const aiMessageIndex = messages.value.length
  addMessage('', false)

  connectionStatus.value = 'connecting'

  // 选择接口
  const eventSourceCallback = (data, isDone) => {
    if (data && data !== '[DONE]') {
      if (aiMessageIndex < messages.value.length) {
        messages.value[aiMessageIndex].content += data
      }
    }

    if (isDone || data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      if (eventSource) eventSource.close()
      saveCurrentSession()
    }
  }

  if (payload && payload.type === 'image') {
    // 带图片的请求
    formData.append('chatId', chatId.value)
    eventSource = chatWithLoveAppImage(formData, {
      onMessage: (data) => eventSourceCallback(data, false),
      onError: (error) => {
        console.error('SSE Error:', error)
        connectionStatus.value = 'error'
        eventSource.close()
      },
      onDone: () => eventSourceCallback(null, true)
    })
  } else {
    // 纯文字请求
    eventSource = chatWithLoveApp(message, chatId.value)
    eventSource.onmessage = (event) => {
      eventSourceCallback(event.data, false)
    }
    eventSource.onerror = (error) => {
      console.error('SSE Error:', error)
      connectionStatus.value = 'error'
      eventSource.close()
    }
  }
}

// 返回主页
const goBack = () => {
  // 保存当前会话
  if (messages.value.length > 1) {
    saveCurrentSession()
  }
  router.push('/')
}

// 页面加载时添加欢迎消息
onMounted(() => {
  // 生成聊天ID
  chatId.value = generateChatId()

  // 添加欢迎消息
  addMessage('欢迎来到AI恋爱大师，请告诉我你的恋爱问题，我会尽力给予帮助和建议。', false)
})

// 组件销毁前关闭SSE连接
onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
  // 保存当前会话
  if (messages.value.length > 1) {
    saveCurrentSession()
  }
})
</script>

<style scoped>
.love-master-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--bg-secondary);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: var(--primary);
  color: white;
  box-shadow: var(--shadow-md);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: opacity 0.2s;
}

.back-button:hover {
  opacity: 0.8;
}

.back-button:before {
  content: '←';
  margin-right: 8px;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
}

.chat-id {
  font-size: 14px;
  opacity: 0.8;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  position: relative;
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  min-height: calc(100vh - 56px - 120px);
  margin-bottom: 0;
}

.footer-container {
  margin-top: auto;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .title {
    font-size: 18px;
  }

  .chat-id {
    font-size: 12px;
  }

  .chat-area {
    padding: 12px;
    min-height: calc(100vh - 48px - 160px);
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 10px 12px;
  }

  .back-button {
    font-size: 14px;
  }

  .title {
    font-size: 16px;
  }

  .chat-id {
    display: none;
  }

  .chat-area {
    padding: 8px;
    min-height: calc(100vh - 42px - 150px);
    margin-bottom: 8px;
  }
}
</style> 