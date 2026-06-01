<template>
  <div class="home-container">
    <div class="header">
      <h1 class="main-title">wwwwAI应用平台</h1>
      <p class="subtitle">探索AI的无限可能</p>
      <div class="divider"></div>
    </div>

    <div class="apps-container">
      <div class="app-card" @click="navigateTo('/love-master')">
        <div class="app-icon love-icon"><Sparkles :size="36" color="white" /></div>
        <div class="app-info">
          <div class="app-title">AI恋爱大师</div>
          <div class="app-desc">智能情感顾问，帮你解答恋爱烦恼</div>
        </div>
        <div class="app-button">
          <span class="btn-text">立即体验</span>
          <span class="btn-icon">→</span>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import { Sparkles } from 'lucide-vue-next'
import AppFooter from '../components/AppFooter.vue'

// 设置页面标题和元数据
useHead({
  title: 'wwwwAI应用平台 - 首页',
  meta: [
    {
      name: 'description',
      content: 'wwwwAI应用平台提供AI恋爱大师服务，满足您的情感对话需求'
    },
    {
      name: 'keywords',
      content: 'AI应用,AI恋爱大师,AI助手,智能对话,wwww,首页'
    }
  ]
})

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--bg-secondary);
  position: relative;
}

.header {
  padding: 70px 20px 50px;
  text-align: center;
  background-color: transparent;
  position: relative;
  z-index: 2;
}

.main-title {
  font-family: var(--font-display);
  font-size: 3.2rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 2px;
  margin-bottom: 16px;
}

.subtitle {
  font-family: var(--font-sans);
  font-size: 1.2rem;
  color: var(--text-secondary);
  max-width: 600px;
  margin: 0 auto 20px;
  letter-spacing: 3px;
}

.divider {
  height: 2px;
  width: 80%;
  max-width: 600px;
  margin: 0 auto;
  background: linear-gradient(90deg, transparent, var(--primary), transparent);
  position: relative;
}

.apps-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 50px;
  max-width: 1200px;
  margin: 60px auto;
  padding: 0 20px;
  flex: 1;
  position: relative;
  z-index: 2;
}

.app-card {
  width: 340px;
  background-color: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border);
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(99, 102, 241, 0.15);
  border-color: var(--primary);
}

.app-icon {
  font-size: 4rem;
  margin-bottom: 25px;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  position: relative;
  z-index: 1;
}

.love-icon {
  background: linear-gradient(135deg, var(--accent), #fb7185);
  box-shadow: 0 4px 12px rgba(244, 63, 94, 0.3);
}

.robot-icon {
  background: linear-gradient(135deg, var(--primary), #818cf8);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.app-info {
  text-align: center;
  margin-bottom: 30px;
  width: 100%;
}

.app-title {
  font-family: var(--font-display);
  font-size: 1.6rem;
  font-weight: bold;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.app-desc {
  font-size: 1rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

.app-button {
  background: linear-gradient(135deg, var(--primary), var(--primary-hover));
  color: white;
  padding: 12px 28px;
  border-radius: var(--radius-full);
  font-weight: 500;
  transition: all 0.3s;
  margin-top: auto;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  border: none;
}

.app-button:hover {
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
  transform: translateY(-2px);
}

.btn-text {
  margin-right: 8px;
  letter-spacing: 1px;
}

.btn-icon {
  font-size: 1.2rem;
  transition: transform 0.3s;
}

.app-button:hover .btn-icon {
  transform: translateX(4px);
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.app-card {
  animation: fadeIn 0.6s ease-out;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-title {
    font-size: 2.5rem;
  }

  .subtitle {
    font-size: 1rem;
  }

  .apps-container {
    gap: 30px;
    margin: 40px auto;
  }

  .app-card {
    width: 100%;
    max-width: 420px;
    padding: 25px;
  }

  .app-icon {
    font-size: 3.5rem;
    width: 80px;
    height: 80px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 50px 15px 40px;
  }

  .main-title {
    font-size: 2rem;
  }

  .subtitle {
    font-size: 0.9rem;
    letter-spacing: 2px;
  }

  .apps-container {
    margin: 30px auto;
    padding: 0 15px;
  }

  .app-card {
    padding: 20px;
  }

  .app-icon {
    font-size: 3rem;
    margin-bottom: 20px;
    width: 70px;
    height: 70px;
  }

  .app-title {
    font-size: 1.4rem;
  }

  .app-desc {
    font-size: 0.9rem;
  }
}
</style> 