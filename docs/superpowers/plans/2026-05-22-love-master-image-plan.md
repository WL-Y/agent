# AI恋爱大师 - 图片理解功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 AI 恋爱大师添加图片理解功能，用户可发送照片给 AI 分析，支持聊天截图、自拍、场景照片等多种场景。

**Architecture:** 前端通过 `multipart/form-data` 发送图片 + 文字混合输入，后端利用 MiniMax-M2.7 多模态能力处理，流式返回 AI 回复。

**Tech Stack:** Spring Boot + Spring AI (OpenAI兼容) / Vue 3 + Vite / SSE 流式通信

---

## 文件结构

```
后端:
- src/main/java/com/yupi/yuaiagent/controller/AiController.java  (新增接口)
- src/main/java/com/yupi/yuaiagent/app/LoveApp.java              (新增方法)

前端:
- yu-ai-agent-frontend/src/api/index.js                         (新增接口)
- yu-ai-agent-frontend/src/components/ChatRoom.vue               (UI改动)
- yu-ai-agent-frontend/src/views/LoveMaster.vue                  (事件处理改动)
```

---

## Task 1: 后端 - AiController 新增图片对话接口

**Files:**
- Modify: `src/main/java/com/yupi/yuaiagent/controller/AiController.java` (新增方法)
- Test: `src/main/java/com/yupi/yuaiagent/controller/AiController.java` (启动验证)

- [ ] **Step 1: 添加 MultipartFile 导入和注解依赖**

在 `AiController.java` 文件顶部添加:
```java
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
```

- [ ] **Step 2: 在类中添加新接口方法**

在 `AiController.java` 的 `doChatWithLoveAppServerSseEmitter` 方法后添加:
```java
/**
 * 带图片的流式对话
 *
 * @param image 图片文件
 * @param message 文字消息
 * @param chatId 会话ID
 * @return SSE 流
 */
@PostMapping(value = "/love_app/chatWithImage", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> doChatWithLoveAppImage(
        @RequestParam("image") MultipartFile image,
        @RequestParam("message") String message,
        @RequestParam("chatId") String chatId) {
    return loveApp.doChatWithImage(image, message, chatId);
}
```

- [ ] **Step 3: 验证后端启动**

Run: `cd D:\ProgramData\IDEA\yu-ai-agent-master && ./mvnw spring-boot:run -DskipTests`
Expected: `Tomcat started on port 8123` 无报错

---

## Task 2: 后端 - LoveApp 新增多模态对话方法

**Files:**
- Modify: `src/main/java/com/yupi/yuaiagent/app/LoveApp.java` (新增方法)

- [ ] **Step 1: 添加必要的导入**

在文件顶部添加:
```java
import org.springframework.ai.chat.messages.UserContent;
import org.springframework.ai.content.ImageContent;
import org.springframework.ai.content.TextContent;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
```

- [ ] **Step 2: 在类中添加新方法**

在 `doChatByStream` 方法后添加:
```java
/**
 * 带图片的对话（支持多模态）
 *
 * @param image 图片文件
 * @param message 文字消息
 * @param chatId 会话ID
 * @return SSE 流
 */
public Flux<String> doChatWithImage(MultipartFile image, String message, String chatId) {
    // 1. 图片转为 Base64
    String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
    String imageUrl = "data:image/jpeg;base64," + base64Image;

    // 2. 构建多模态用户消息
    UserContent userContent = UserContent.of(
            ImageContent.create(imageUrl),
            TextContent.of(message)
    );

    // 3. 流式调用
    return chatClient.prompt()
            .user(userContent)
            .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
            .stream()
            .content();
}
```

- [ ] **Step 3: 验证后端启动**

Run: `cd D:\ProgramData\IDEA\yu-ai-agent-master && ./mvnw spring-boot:run -DskipTests`
Expected: `Started YuAiAgentApplication` 无编译错误

---

## Task 3: 前端 - API 层新增图片对话接口

**Files:**
- Modify: `yu-ai-agent-frontend/src/api/index.js`

- [ ] **Step 1: 添加图片对话接口**

在 `chatWithLoveApp` 函数后添加:
```javascript
// 带图片的对话
export const chatWithLoveAppImage = (formData, callbacks = {}) => {
  const { onMessage, onError, onDone } = callbacks

  // 模拟 EventSource 行为，使用 fetch + ReadableStream
  const eventSource = {
    onmessage: null,
    onerror: null,
    close: null
  }

  fetch(`${BASE_URL}/ai/love_app/chatWithImage`, {
    method: 'POST',
    body: formData,
    headers: {
      // 不设置 Content-Type，让浏览器自动设置 boundary
    }
  })
    .then(response => {
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      const read = () => {
        reader.read().then(({ done, value }) => {
          if (done) {
            if (onDone) onDone()
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() // 保留未完成的行

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6)
              if (data === '[DONE]') {
                if (onDone) onDone()
              } else {
                if (onMessage) onMessage(data)
              }
            }
          }

          if (!done) read()
        })
      }

      read()
    })
    .catch(error => {
      if (onError) onError(error)
    })

  eventSource.close = () => {}

  return eventSource
}
```

- [ ] **Step 2: 验证语法正确**

前端已运行，无需额外验证

---

## Task 4: 前端 - ChatRoom.vue 图片上传 UI

**Files:**
- Modify: `yu-ai-agent-frontend/src/components/ChatRoom.vue` (模板+样式+逻辑)

- [ ] **Step 1: 添加隐藏的文件 input**

在模板的 `history-button` 后添加:
```html
<input
  type="file"
  ref="imageInput"
  accept="image/jpeg,image/png,image/webp,image/gif"
  style="display: none"
  @change="handleImageSelect"
/>
```

- [ ] **Step 2: 在 `history-button` 后添加图片按钮**

```html
<button class="image-button" @click="triggerImageSelect" title="发送图片">
  📷
</button>
```

- [ ] **Step 3: 在聊天消息区域上方添加图片预览区域**

在 `chat-messages` div 内部开头添加:
```html
<div v-if="imagePreview" class="image-preview-container">
  <div class="image-preview">
    <img :src="imagePreview" alt="Preview" />
    <button class="remove-image" @click="removeImage">×</button>
  </div>
</div>
```

- [ ] **Step 4: 添加图片相关响应式变量**

在 `const inputMessage = ref('')` 后添加:
```javascript
const imageInput = ref(null)
const selectedImage = ref(null)
const imagePreview = ref('')
```

- [ ] **Step 5: 添加图片处理方法**

在 `sendMessage` 方法前添加:
```javascript
// 触发图片选择
const triggerImageSelect = () => {
  imageInput.value.click()
}

// 处理图片选择
const handleImageSelect = (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 校验文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    alert('仅支持 JPG、PNG、WebP、GIF 格式图片')
    return
  }

  // 校验文件大小 (10MB)
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
  imagePreview.value = ''
  if (imageInput.value) {
    imageInput.value.value = ''
  }
}
```

- [ ] **Step 6: 修改 sendMessage 方法支持 FormData**

原 `sendMessage` 方法开头添加图片处理:
```javascript
const sendMessage = () => {
  if (!inputMessage.value.trim() && !selectedImage.value) return

  // 如果有图片，发送 FormData 类型
  if (selectedImage.value) {
    const formData = new FormData()
    formData.append('image', selectedImage.value)
    formData.append('message', inputMessage.value)

    emit('send-message', { type: 'image', data: formData })
    inputMessage.value = ''
    removeImage()
    return
  }

  // 原有文字消息逻辑
  emit('send-message', inputMessage.value)
  inputMessage.value = ''
}
```

- [ ] **Step 7: 添加图片预览和按钮样式**

在 `history-button` 样式后添加:
```css
.image-button {
  position: absolute;
  top: 10px;
  left: 50px;
  width: 32px;
  height: 32px;
  background-color: #ff6b8b;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: background-color 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.image-button:hover {
  background-color: #ff4d6a;
}

.image-preview-container {
  padding: 10px 16px;
  background-color: #f9f9f9;
  border-bottom: 1px solid #eee;
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-preview img {
  max-width: 200px;
  max-height: 150px;
  border-radius: 8px;
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
```

---

## Task 5: 前端 - LoveMaster.vue 支持图片消息

**Files:**
- Modify: `yu-ai-agent-frontend/src/views/LoveMaster.vue`

- [ ] **Step 1: 修改 sendMessage 方法支持图片**

在 `sendMessage` 方法中添加图片判断分支:
```javascript
// 发送消息（支持图片）
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
```

- [ ] **Step 2: 导入图片接口**

在文件顶部添加:
```javascript
import { chatWithLoveApp, chatWithLoveAppImage } from '../api'
```

---

## Task 6: 集成测试

**Files:**
- Test: `http://localhost:3001/#/love-master`

- [ ] **Step 1: 启动前后端**

后端: `cd D:\ProgramData\IDEA\yu-ai-agent-master && ./mvnw spring-boot:run -DskipTests`
前端: `cd D:\ProgramData\IDEA\yu-ai-agent-master\yu-ai-agent-frontend && npm run dev`

- [ ] **Step 2: 验证功能**

1. 访问 http://localhost:3001/#/love-master
2. 点击左上角📷按钮，选择一张图片
3. 确认图片预览显示在输入框上方
4. 点击×移除图片，确认预览消失
5. 输入文字"请分析这张照片"并发送
6. 确认 AI 流式返回分析结果

---

## 验证清单

| 功能 | 验证点 |
|------|--------|
| 图片选择 | 点击📷能打开文件选择器 |
| 格式校验 | 选择非图片文件时弹出提示 |
| 大小校验 | 选择 >10MB 图片时弹出提示 |
| 预览显示 | 选中图片后显示缩略图 |
| 移除图片 | 点击×后预览消失 |
| 图片发送 | 发送后消息出现在聊天框 |
| AI 回复 | 流式返回图片分析内容 |
| 历史记录 | 图片对话能正常保存和加载 |

---

**Plan complete.** 两个执行选项:

**1. Subagent-Driven (recommended)** - 派遣子 agent 逐任务执行，任务间审查
**2. Inline Execution** - 在本会话中执行任务，带检查点

选择哪个？