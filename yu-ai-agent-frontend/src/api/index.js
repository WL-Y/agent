import axios from 'axios'

// 根据环境变量设置 API 基础 URL
const API_BASE_URL = process.env.NODE_ENV === 'production' 
 ? '/api' // 生产环境使用相对路径，适用于前后端部署在同一域名下
 : 'http://localhost:8123/api' // 开发环境指向本地后端服务

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 封装SSE连接
export const connectSSE = (url, params, onMessage, onError) => {
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  const eventSource = new EventSource(fullUrl)

  eventSource.onmessage = event => {
    if (onMessage) onMessage(event.data)
  }

  eventSource.onerror = error => {
    if (onError) onError(error)
    // 不主动关闭，让 EventSource 自动重连
  }

  return eventSource
}

// AI恋爱大师聊天
export const chatWithLoveApp = (message, chatId) => {
  return connectSSE('/ai/love_app/chat/sse', { message, chatId })
}

// 带图片的恋爱大师对话
export const chatWithLoveAppImage = (formData, callbacks = {}) => {
  const { onMessage, onError, onDone } = callbacks
  let doneFired = false
  const abortController = new AbortController()

  const fireDoneOnce = () => {
    if (!doneFired) {
      doneFired = true
      if (onDone) onDone()
    }
  }

  fetch(`${API_BASE_URL}/ai/love_app/chatWithImage`, {
    method: 'POST',
    body: formData,
    signal: abortController.signal
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      const read = () => {
        reader.read().then(({ done, value }) => {
          if (done) {
            fireDoneOnce()
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop()

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6)
              if (data === '[DONE]') {
                fireDoneOnce()
                reader.cancel()
                return
              }
              if (onMessage) onMessage(data)
            }
          }

          read()
        }).catch(err => {
          if (err.name !== 'AbortError' && onError) onError(err)
        })
      }

      read()
    })
    .catch(error => {
      if (error.name !== 'AbortError' && onError) onError(error)
    })

  return {
    close: () => abortController.abort()
  }
}

// AI超级智能体聊天
export const chatWithManus = (message) => {
  return connectSSE('/ai/manus/chat', { message })
}

export default {
  chatWithLoveApp,
  chatWithLoveAppImage,
  chatWithManus
} 