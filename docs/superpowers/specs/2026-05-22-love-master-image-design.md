# AI恋爱大师 - 图片理解功能设计

**日期：** 2026-05-22
**状态：** 已批准

---

## 概述

为 AI 恋爱大师添加图片理解功能，用户可发送照片给 AI 分析，支持自拍/对方照片、聊天截图、礼物/场景照片等多种场景。

**技术方案：** 利用 MiniMax-M2.7 多模态能力，前端通过 `multipart/form-data` 发送图片 + 文字混合输入，后端流式返回 AI 回复。

---

## 架构

```
┌─────────────┐      multipart/form-data      ┌──────────────┐
│  Vue 前端   │ ──────────────────────────────→│ Spring Boot  │
│  ChatRoom   │  image(file) + message + chatId│  AiController│
└─────────────┘                                └──────┬───────┘
                                                       │
                                                  OpenAI 兼容
                                                  多模态API
                                                       │
                                                  ┌─────▼─────┐
                                                  │MiniMax-M2.7│
                                                  └───────────┘
```

---

## 后端改动

### 1. AiController 新增接口

**文件：** `src/main/java/com/yupi/yuaiagent/controller/AiController.java`

```java
/**
 * 带图片的流式对话
 */
@PostMapping(value = "/love_app/chatWithImage", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> doChatWithLoveAppImage(
    @RequestParam("image") MultipartFile image,
    @RequestParam("message") String message,
    @RequestParam("chatId") String chatId) {
    return loveApp.doChatWithImage(image, message, chatId);
}
```

### 2. LoveApp 新增方法

**文件：** `src/main/java/com/yupi/yuaiagent/app/LoveApp.java`

```java
/**
 * 带图片的对话（支持多模态）
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

---

## 前端改动

### 1. ChatRoom.vue

**文件：** `yu-ai-agent-frontend/src/components/ChatRoom.vue`

#### 模板改动
- 输入框左侧添加📷图片按钮
- 图片预览区域（发送前显示选中图片）
- 支持移除已选图片

#### 逻辑改动
```javascript
// 新增响应式变量
const selectedImage = ref(null)
const imagePreview = ref('')

// 图片选择处理
const handleImageSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    selectedImage.value = file
    imagePreview.value = URL.createObjectURL(file)
  }
}

// 移除图片
const removeImage = () => {
  selectedImage.value = null
  imagePreview.value = ''
}

// 发送消息（新增图片处理）
const sendMessage = () => {
  if (!inputMessage.value.trim() && !selectedImage.value) return

  const formData = new FormData()
  if (selectedImage.value) {
    formData.append('image', selectedImage.value)
  }
  formData.append('message', inputMessage.value)

  emit('send-message', formData)  // 传递 FormData
  // ...
}
```

### 2. API 层改动

**文件：** `yu-ai-agent-frontend/src/api/index.js`

新增接口：
```javascript
// 带图片的对话
export const chatWithLoveAppImage = (formData) => {
  return fetchEventSource(`${BASE_URL}/ai/love_app/chatWithImage`, {
    method: 'POST',
    body: formData,
    // SSE 处理...
  })
}
```

### 3. LoveMaster.vue 改动

- `@send-message` 事件处理支持 FormData 类型
- 判断是否包含图片，选择对应接口

---

## 数据流

1. 用户点击📷 → file input 触发 → 选择本地图片
2. 前端显示图片缩略图预览
3. 用户输入文字 + 点击发送
4. 前端构造 FormData: `{ image: File, message: String, chatId: String }`
5. 后端接收 MultipartFile，转 Base64，构建 `ImageContent + TextContent`
6. MiniMax-M2.7 多模态处理，流式返回
7. 前端 SSE 接收并更新 UI（与现有文字对话流程一致）

---

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 图片格式不支持 | 前端校验，仅接受 jpg/png/webp/gif |
| 图片过大（>10MB） | 前端拦截，提示用户压缩 |
| AI 无法识别图片 | 流式返回错误信息 |
| 网络中断 | 显示重连按钮，保留输入内容 |

---

## 文件清单

### 后端
- `AiController.java` - 新增 `/love_app/chatWithImage` 接口
- `LoveApp.java` - 新增 `doChatWithImage()` 方法

### 前端
- `ChatRoom.vue` - 图片上传 UI + FormData 构建
- `api/index.js` - 新增 `chatWithLoveAppImage` 接口
- `LoveMaster.vue` - 适配 FormData 发送逻辑

---

## 待后续处理

- 语音转文字功能
- 用户认证与多设备同步