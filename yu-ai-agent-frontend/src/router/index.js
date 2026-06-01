import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - wwwwAI应用平台',
      description: 'wwwwAI应用平台提供AI恋爱大师服务，满足您的情感对话需求'
    }
  },
  {
    path: '/love-master',
    name: 'LoveMaster',
    component: () => import('../views/LoveMaster.vue'),
    meta: {
      title: 'AI恋爱大师 - wwwwAI应用平台',
      description: 'AI恋爱大师是wwwwAI应用平台的专业情感顾问，帮你解答各种恋爱问题，提供情感建议'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局导航守卫，设置文档标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router 