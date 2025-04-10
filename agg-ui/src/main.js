import { createApp } from 'vue'
import router from './router'
import './style.css'
import App from './App.vue'

// 使用环境变量配置 API 地址
const baseURL = import.meta.env.VITE_APP_API_URL || 'http://localhost:8080'

// 创建一个原始的 fetch 引用
const originalFetch = window.fetch

// 重写 fetch
window.fetch = async (url, options = {}) => {
    const finalUrl = url.startsWith('http') ? url : `${baseURL}${url}`

    // 只有当不是 FormData 时才设置 Content-Type
    const headers = options.body instanceof FormData
        ? options.headers
        : {
            'Content-Type': 'application/json',
            ...options.headers,
        }

    return originalFetch(finalUrl, {
        ...options,
        credentials: 'include',
        headers
    })
}

const app = createApp(App)
app.use(router)
app.mount('#app')
