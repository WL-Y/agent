<script setup>
</script>

<template>
  <router-view />
</template>

<style>
:root {
  /* 主色调 - Indigo */
  --primary: #6366f1;
  --primary-hover: #4f46e5;
  --primary-light: #eef2ff;

  /* 强调色 - Rose */
  --accent: #f43f5e;
  --accent-hover: #e11d48;
  --accent-light: #fff1f2;

  /* 中性色 */
  --bg-primary: #ffffff;
  --bg-secondary: #f8fafc;
  --bg-tertiary: #f1f5f9;

  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-tertiary: #94a3b8;

  --border: #e2e8f0;
  --border-hover: #cbd5e1;

  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.1);
  --shadow-lg: 0 10px 15px -3px rgba(0,0,0,0.1);

  /* 字体 */
  --font-sans: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-display: 'Noto Serif SC', 'SimSun', serif;

  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-full: 9999px;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html, body {
  font-family: var(--font-sans);
  font-size: 16px;
  color: var(--text-primary);
  background-color: var(--bg-secondary);
  width: 100%;
  height: 100%;
  overflow-x: hidden;
}

#app {
  width: 100%;
  height: 100%;
}

a {
  text-decoration: none;
  color: inherit;
}

button {
  cursor: pointer;
}

/* 响应式字体大小 */
@media (max-width: 768px) {
  html, body {
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  html, body {
    font-size: 14px;
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: var(--border-hover);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}
</style>
