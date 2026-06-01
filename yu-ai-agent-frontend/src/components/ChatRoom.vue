<template>
  <div class="chat-layout">
    <!-- 隐藏的文件input -->
    <input
      type="file"
      ref="imageInput"
      accept="image/jpeg,image/png,image/webp,image/gif"
      style="display: none"
      @change="handleImageSelect"
    />

    <!-- 左侧历史记录侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <!-- 侧边栏头部 -->
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="startNewChat">
          <Plus :size="16" />
          <span>新建对话</span>
        </button>
        <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开' : '收起'">
          <PanelLeftClose v-if="!sidebarCollapsed" :size="18" />
          <PanelLeftOpen v-else :size="18" />
        </button>
      </div>

      <!-- 搜索框 -->
      <div class="sidebar-search" v-show="!sidebarCollapsed">
        <Search :size="16" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索对话..."
          class="search-input"
        />
      </div>

      <!-- 会话列表 -->
      <div class="sidebar-content" v-show="!sidebarCollapsed">
        <template v-if="filteredGroupedSessions.length > 0">
          <div v-for="group in filteredGroupedSessions" :key="group.label" class="session-group">
            <div class="group-label">{{ group.label }}</div>
            <div
              v-for="session in group.sessions"
              :key="session.id"
              class="session-item"
              :class="{ 'active': session.id === activeChatId }"
              @click="loadSession(session)"
              @mouseenter="hoveredSession = session.id"
              @mouseleave="hoveredSession = ''"
            >
              <div class="session-title">{{ session.title }}</div>
              <div class="session-actions" v-show="hoveredSession === session.id">
                <button class="session-action" @click.stop="deleteSession(session.id)" title="删除">
                  <Trash2 :size="14" />
                </button>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="no-history">
          <MessageSquare :size="24" />
          <span>{{ searchQuery ? '未找到匹配对话' : '暂无历史记录' }}</span>
        </div>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 聊天记录区域 -->
      <div class="chat-messages" ref="messagesContainer">
        <!-- 图片预览 -->
        <div v-if="imagePreview" class="image-preview-container">
          <div class="image-preview">
            <img :src="imagePreview" alt="Preview" />
            <button class="remove-image" @click="removeImage"><X :size="14" /></button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
          <!-- AI消息 -->
          <div v-if="!msg.isUser" class="message ai-message">
            <div class="avatar ai-avatar">
              <AiAvatarFallback :type="aiType" />
            </div>
            <div class="message-bubble">
              <div class="message-content">
                {{ msg.content }}
                <span v-if="connectionStatus === 'connecting' && index === messages.length - 1" class="typing-indicator">▋</span>
              </div>
              <div class="message-time">{{ formatTime(msg.time) }}</div>
            </div>
          </div>

          <!-- 用户消息 -->
          <div v-else class="message user-message">
            <div class="message-bubble">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.time) }}</div>
            </div>
            <div class="avatar user-avatar">
              <div class="avatar-placeholder">我</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-container">
        <div class="chat-input">
          <textarea
            v-model="inputMessage"
            @keydown="handleKeydown"
            @input="handleInput"
            placeholder="请输入消息...（Shift+Enter换行）"
            class="input-box"
            :disabled="connectionStatus === 'connecting'"
          ></textarea>
          <button class="image-button-inline" @click="triggerImageSelect" title="发送图片">
            <Camera :size="18" />
          </button>
          <button
            @click="sendMessage"
            class="send-button"
            :disabled="connectionStatus === 'connecting' || (!inputMessage.trim() && !selectedImage)"
          ><Send :size="16" /></button>
        </div>

        <!-- 命令提示下拉框 -->
        <div v-if="showCommands" class="commands-dropdown">
          <div
            v-for="cmd in filteredCommands"
            :key="cmd.name"
            class="command-item"
            @click="selectCommand(cmd)"
          >
            <span class="command-name">{{ cmd.name }}</span>
            <span class="command-desc">{{ cmd.description }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { Camera, X, Send, Plus, Search, Trash2, MessageSquare, PanelLeftClose, PanelLeftOpen } from 'lucide-vue-next'
import AiAvatarFallback from './AiAvatarFallback.vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  connectionStatus: {
    type: String,
    default: 'disconnected'
  },
  aiType: {
    type: String,
    default: 'default'
  },
  activeChatId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['send-message', 'load-session', 'new-chat'])

const inputMessage = ref('')
const messagesContainer = ref(null)
const imageInput = ref(null)
const selectedImage = ref(null)
const imagePreview = ref('')
const showCommands = ref(false)
const searchQuery = ref('')
const sidebarCollapsed = ref(false)
const hoveredSession = ref('')

// 可用命令列表
const commands = [
  { name: '/情话', description: '生成浪漫情话', example: '/情话 浪漫' },
  { name: '/开场白', description: '推荐开场白', example: '/开场白 相亲' },
  { name: '/love_words', description: 'Generate love words (English)', example: '/love_words sweet' },
  { name: '/opening_lines', description: 'Get opening lines (English)', example: '/opening_lines dating' }
]

// 过滤匹配的命令
const filteredCommands = computed(() => {
  if (!inputMessage.value.startsWith('/')) return []
  const search = inputMessage.value.toLowerCase()
  return commands.filter(cmd =>
    cmd.name.toLowerCase().includes(search) ||
    cmd.description.toLowerCase().includes(search)
  )
})

// 处理键盘事件
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 处理输入
const handleInput = () => {
  if (inputMessage.value === '/') {
    showCommands.value = true
  } else if (inputMessage.value.startsWith('/')) {
    showCommands.value = filteredCommands.value.length > 0
  } else {
    showCommands.value = false
  }
}

// 选择命令
const selectCommand = (cmd) => {
  inputMessage.value = cmd.name + ' '
  showCommands.value = false
}

// localStorage key
const STORAGE_KEY = 'love_master_sessions'

// 加载历史会话
const chatSessions = ref([])

const loadSessions = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

const loadAllSessions = () => {
  chatSessions.value = loadSessions()
}

// 删除会话
const deleteSession = (sessionId) => {
  try {
    const sessions = loadSessions().filter(s => s.id !== sessionId)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions))
    chatSessions.value = sessions
  } catch {
    // ignore storage errors
  }
  if (props.activeChatId === sessionId) {
    emit('new-chat')
  }
}

// 按时间分组会话
const groupedSessions = computed(() => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const yesterday = today - 86400000
  const sevenDaysAgo = today - 7 * 86400000
  const thirtyDaysAgo = today - 30 * 86400000

  const groups = {
    today: [],
    yesterday: [],
    sevenDays: [],
    thirtyDays: [],
    older: []
  }

  chatSessions.value.forEach(session => {
    const time = session.time
    if (time >= today) {
      groups.today.push(session)
    } else if (time >= yesterday) {
      groups.yesterday.push(session)
    } else if (time >= sevenDaysAgo) {
      groups.sevenDays.push(session)
    } else if (time >= thirtyDaysAgo) {
      groups.thirtyDays.push(session)
    } else {
      groups.older.push(session)
    }
  })

  const result = []
  if (groups.today.length > 0) result.push({ label: '今天', sessions: groups.today })
  if (groups.yesterday.length > 0) result.push({ label: '昨天', sessions: groups.yesterday })
  if (groups.sevenDays.length > 0) result.push({ label: '近 7 天', sessions: groups.sevenDays })
  if (groups.thirtyDays.length > 0) result.push({ label: '近 30 天', sessions: groups.thirtyDays })
  if (groups.older.length > 0) result.push({ label: '更早', sessions: groups.older })

  return result
})

// 搜索过滤后的分组
const filteredGroupedSessions = computed(() => {
  if (!searchQuery.value.trim()) return groupedSessions.value

  const query = searchQuery.value.toLowerCase()
  return groupedSessions.value
    .map(group => ({
      label: group.label,
      sessions: group.sessions.filter(s =>
        s.title.toLowerCase().includes(query)
      )
    }))
    .filter(group => group.sessions.length > 0)
})

// 根据AI类型选择不同头像
// 触发图片选择
const triggerImageSelect = () => {
  imageInput.value.click()
}

// 处理图片选择
const handleImageSelect = (event) => {
  const file = event.target.files[0]
  if (!file) return

  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    alert('仅支持 JPG、PNG、WebP、GIF 格式图片')
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    alert('图片大小不能超过 10MB')
    return
  }

  selectedImage.value = file
  imagePreview.value = URL.createObjectURL(file)
}

// 移除已选图片
const removeImage = () => {
  selectedImage.value = null
  if (imagePreview.value) {
    URL.revokeObjectURL(imagePreview.value)
    imagePreview.value = ''
  }
  if (imageInput.value) {
    imageInput.value.value = ''
  }
}

// 发送消息
const sendMessage = () => {
  if (selectedImage.value) {
    const formData = new FormData()
    formData.append('image', selectedImage.value)
    formData.append('message', inputMessage.value)

    emit('send-message', { type: 'image', data: formData })
    inputMessage.value = ''
    removeImage()
    return
  }

  if (!inputMessage.value.trim()) return

  emit('send-message', inputMessage.value)
  inputMessage.value = ''
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 加载指定会话
const loadSession = (session) => {
  emit('load-session', session)
}

// 开始新对话
const startNewChat = () => {
  emit('new-chat')
}

// 自动滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 监听消息列表变化，自动滚动
watch(() => props.messages.length, () => {
  scrollToBottom()
})

// 监听消息变化
onMounted(() => {
  loadAllSessions()
  scrollToBottom()
})

// 暴露方法给父组件
defineExpose({
  loadAllSessions
})
</script>

<style scoped>
/* 整体布局 */
.chat-layout {
  display: flex;
  height: 70vh;
  min-height: 600px;
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 260px;
  background-color: var(--bg-primary);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.sidebar-collapsed {
  width: 52px;
}

.sidebar-header {
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--border);
}

.new-chat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background-color: var(--primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
}

.new-chat-btn:hover {
  background-color: var(--primary-hover);
}

.sidebar-collapsed .new-chat-btn span {
  display: none;
}

.sidebar-collapsed .new-chat-btn {
  padding: 10px;
}

.sidebar-toggle {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

/* 搜索框 */
.sidebar-search {
  padding: 12px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-tertiary);
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  outline: none;
  font-size: 14px;
  color: var(--text-primary);
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

/* 会话列表 */
.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.sidebar-content::-webkit-scrollbar {
  width: 4px;
}

.sidebar-content::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-content::-webkit-scrollbar-thumb {
  background-color: var(--border);
  border-radius: 4px;
}

.session-group {
  margin-bottom: 8px;
}

.group-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  padding: 8px 12px 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s ease;
}

.session-item:hover {
  background-color: var(--bg-tertiary);
}

.session-item.active {
  background-color: var(--primary-light);
}

.session-title {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.session-item.active .session-title {
  color: var(--primary);
  font-weight: 500;
}

.session-actions {
  display: flex;
  gap: 2px;
  margin-left: 4px;
}

.session-action {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-tertiary);
  transition: all 0.15s ease;
}

.session-action:hover {
  background-color: var(--border);
  color: var(--accent);
}

/* 空状态 */
.no-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-tertiary);
  gap: 8px;
}

.no-history span {
  font-size: 13px;
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 90px;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

.message-wrapper {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.message {
  display: flex;
  align-items: flex-start;
  max-width: 85%;
  margin-bottom: 8px;
}

.user-message {
  margin-left: auto;
  flex-direction: row;
}

.ai-message {
  margin-right: auto;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  margin-left: 8px;
}

.ai-avatar {
  margin-right: 8px;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--primary);
  color: white;
  font-weight: bold;
}

.message-bubble {
  padding: 12px;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
  min-width: 100px;
}

.user-message .message-bubble {
  background-color: var(--primary);
  color: white;
  border-bottom-right-radius: 4px;
  text-align: left;
}

.ai-message .message-bubble {
  background-color: var(--bg-primary);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
  text-align: left;
  border: 1px solid var(--border);
}

.message-content {
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 4px;
  text-align: right;
}

/* 输入区域 */
.chat-input-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: var(--bg-primary);
  border-top: 1px solid var(--border);
  z-index: 10;
  padding: 12px 16px;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 800px;
  margin: 0 auto;
}

.input-box {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 10px 16px;
  font-size: 14px;
  resize: none;
  min-height: 20px;
  max-height: 120px;
  outline: none;
  transition: border-color 0.2s;
  overflow-y: auto;
  scrollbar-width: none;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  font-family: inherit;
  line-height: 1.5;
}

.input-box::-webkit-scrollbar {
  display: none;
}

.input-box:focus {
  border-color: var(--primary);
}

.input-box:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.image-button-inline {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: 1px solid var(--border);
  border-radius: 50%;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.image-button-inline:hover {
  border-color: var(--primary);
  color: var(--primary);
  background-color: var(--primary-light);
}

.send-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--primary);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-button:hover:not(:disabled) {
  background-color: var(--primary-hover);
  transform: scale(1.05);
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 命令提示 */
.commands-dropdown {
  position: absolute;
  bottom: 100%;
  left: 16px;
  right: 16px;
  max-width: 800px;
  margin: 0 auto;
  background-color: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  max-height: 200px;
  overflow-y: auto;
  z-index: 20;
}

.command-item {
  display: flex;
  flex-direction: column;
  padding: 10px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  transition: background-color 0.15s;
}

.command-item:hover {
  background-color: var(--primary-light);
}

.command-item:last-child {
  border-bottom: none;
}

.command-name {
  font-size: 14px;
  color: var(--primary);
  font-weight: 500;
  margin-bottom: 2px;
}

.command-desc {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 图片预览 */
.image-preview-container {
  padding: 12px 16px;
  background-color: var(--bg-tertiary);
  border-bottom: 1px solid var(--border);
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-preview img {
  max-width: 200px;
  max-height: 150px;
  border-radius: var(--radius-md);
  object-fit: cover;
}

.remove-image {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-image:hover {
  background-color: rgba(0, 0, 0, 0.8);
}

/* 打字指示器 */
.typing-indicator {
  display: inline-block;
  animation: blink 0.7s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

/* 连续AI消息样式 */
.ai-message + .ai-message {
  margin-top: 4px;
}

.ai-message + .ai-message .avatar {
  visibility: hidden;
}

.ai-message + .ai-message .message-bubble {
  border-top-left-radius: 10px;
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    width: 52px;
  }

  .sidebar .sidebar-search,
  .sidebar .group-label,
  .sidebar .session-title,
  .sidebar .session-actions,
  .sidebar .new-chat-btn span {
    display: none;
  }

  .sidebar .new-chat-btn {
    padding: 10px;
  }

  .message {
    max-width: 95%;
  }

  .message-content {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .chat-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    height: auto;
    max-height: 48px;
    border-right: none;
    border-bottom: 1px solid var(--border);
    flex-direction: row;
    overflow: hidden;
  }

  .sidebar-header {
    border-bottom: none;
    padding: 6px;
  }

  .sidebar-search {
    display: none !important;
  }

  .sidebar-content {
    display: none !important;
  }

  .avatar {
    width: 32px;
    height: 32px;
  }

  .message-bubble {
    padding: 10px;
  }

  .message-content {
    font-size: 13px;
  }
}
</style>