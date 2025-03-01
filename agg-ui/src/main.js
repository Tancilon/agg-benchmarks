import { createApp } from 'vue'
import router from './router'
import './style.css'
import App from './App.vue'

// 配置全局的 fetch 默认选项
const baseURL = 'http://localhost:8080'  // 后端服务地址

// 创建一个原始的 fetch 引用
const originalFetch = window.fetch

// 重写 fetch
window.fetch = async (url, options = {}) => {
    const finalUrl = url.startsWith('http') ? url : `${baseURL}${url}`
    return originalFetch(finalUrl, {
        ...options,
        credentials: 'include',
        headers: {
            ...options.headers,
        }
    })
}

const app = createApp(App)
app.use(router)
app.mount('#app')
