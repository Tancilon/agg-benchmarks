<template>
  <div class="container mx-auto px-4 py-8">
    <!-- 指标标题部分 -->
    <div class="mb-8">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <BarChart class="w-8 h-8 text-blue-500" />
          <h1 class="text-3xl font-bold">{{ metric?.name }}</h1>
        </div>
        <!-- 下载按钮 -->
        <label 
          v-if="metric?.implementationFile"
          class="label implementation-btn" 
          :class="{ 'downloading': isDownloading || downloadComplete }"
          @click="handleDownloadImplementation"
          :disabled="isDownloading"
        >
          <span class="circle">
            <Download class="icon" />
            <svg class="checkmark" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path class="check" d="M20 6L9 17L4 12" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
          <p class="title">Download Implementation</p>
        </label>
      </div>
      <!-- 添加指标类型和范围信息 -->
      <div class="mt-4 flex items-center gap-4">
        <span class="px-3 py-1 bg-blue-50 text-blue-600 rounded-full text-sm">
          Type: {{ metric?.type }}
        </span>
        <span class="px-3 py-1 bg-blue-50 text-blue-600 rounded-full text-sm">
          Range: {{ metric?.range }}
        </span>
      </div>
    </div>

    <!-- 指标描述部分 -->
    <div class="prose prose-lg max-w-none">
      <div v-if="metric?.description && metric.description.trim()" class="bg-white rounded-xl shadow-lg p-8">
        <MdPreview :modelValue="metric.description" />
      </div>
      <div v-else class="text-center py-12 text-gray-500">
        <p>No detailed description available for this metric.</p>
        <p class="mt-2 text-sm">This metric is used to {{ getMetricPurpose(metric?.type) }}</p>
      </div>
    </div>

    <!-- 添加调试信息 -->
    <div v-if="error" class="mt-8 p-4 bg-red-50 text-red-600 rounded-lg">
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { BarChart, Download, Loader2 } from 'lucide-vue-next'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'

const route = useRoute()
const metric = ref(null)
const error = ref(null)
const isDownloading = ref(false)
const downloadComplete = ref(false)

// 根据指标类型返回基本描述
const getMetricPurpose = (type) => {
  switch (type?.toLowerCase()) {
    case 'recall':
      return 'measure the proportion of relevant items that are successfully retrieved'
    case 'precision':
      return 'measure the proportion of retrieved items that are relevant'
    case 'accuracy':
      return 'measure the overall correctness of the results'
    case 'time':
      return 'measure the computational efficiency or processing time'
    default:
      return 'evaluate specific aspects of algorithm performance'
  }
}

// 处理下载实现文件
const handleDownloadImplementation = async () => {
  if (isDownloading.value) return
  
  try {
    isDownloading.value = true
    downloadComplete.value = false
    
    const response = await fetch(`/api/metrics/${metric.value.name}/download`)
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Failed to download implementation')
    }
    
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const originalExt = metric.value.implementationFile.substring(
      metric.value.implementationFile.lastIndexOf('.')
    )
    a.download = `${metric.value.name}_implementation${originalExt}`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    downloadComplete.value = true
    
    // 延迟重置下载状态，让动画有足够时间完成
    setTimeout(() => {
      isDownloading.value = false
      // 再延迟一段时间重置完成状态
      setTimeout(() => {
        downloadComplete.value = false
      }, 3000)  // 增加完成状态显示时间
    }, 3500)  // 增加总动画时间
  } catch (err) {
    console.error('Error downloading implementation:', err)
    error.value = 'Failed to download implementation file'
    setTimeout(() => {
      isDownloading.value = false
      downloadComplete.value = false
    }, 3000)  // 保持一致的时间
  }
}

// 获取指标详情
const fetchMetricDetail = async () => {
  try {
    console.log('Fetching metric details for:', route.params.name)
    error.value = null
    const response = await fetch(`/api/metrics/by-name/${route.params.name}`)
    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to fetch metric details: ${errorText}`)
    }
    metric.value = await response.json()
    console.log('Fetched metric details:', {
      name: metric.value?.name,
      type: metric.value?.type,
      range: metric.value?.range,
      description: metric.value?.description,
      implementationFile: metric.value?.implementationFile
    })
  } catch (err) {
    console.error('Error fetching metric details:', err)
    error.value = `Error loading metric details: ${err.message}`
  }
}

onMounted(() => {
  fetchMetricDetail()
})
</script>

<style scoped>
.label {
  background-color: transparent;
  border: 2px solid #336FFF;
  display: flex;
  align-items: center;
  border-radius: 50px;
  width: 240px;
  cursor: pointer;
  transition: all 0.4s ease;
  padding: 5px;
  position: relative;
  background-color: #336FFF;
  height: 55px;
}

.implementation-btn {
  width: 320px;
}

.label:hover {
  background-color: #2952cc;
  border-color: #2952cc;
}

.prose {
  color: #374151;
}

.prose h1, .prose h2, .prose h3 {
  color: #111827;
  font-weight: 600;
}

.prose code {
  background-color: #f3f4f6;
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
}

.prose pre {
  background-color: #111827;
  color: white;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
}

.prose a {
  color: #2563eb;
}

.prose a:hover {
  color: #1e40af;
}

.prose blockquote {
  border-left: 4px solid #e5e7eb;
  padding-left: 1rem;
  font-style: italic;
}

.label::before {
  content: "";
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #fff;
  width: 8px;
  height: 8px;
  transition: all 0.4s ease;
  border-radius: 100%;
  margin: auto;
  opacity: 0;
  visibility: hidden;
}

.label .title {
  font-size: 15px;
  color: #ffffff;
  transition: all 0.4s ease;
  position: absolute;
  right: 32px;
  top: 50%;
  text-align: center;
  white-space: nowrap;
  transform: translateY(-50%);
  font-weight: 600;
  letter-spacing: 0.5px;
}

.label .circle {
  height: 45px;
  width: 45px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.2);
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.4s ease;
  position: relative;
  box-shadow: 0 0 0 0 rgb(255, 255, 255);
  overflow: hidden;
}

.label .circle .icon {
  color: #fff;
  width: 30px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  transition: all 0.4s ease;
}

.label .circle .checkmark {
  width: 24px;
  height: 24px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  visibility: hidden;
  stroke: #22c55e;
  transform-origin: center;
}

/* 下载动画相关样式 */
.label.downloading {
  width: 57px;
  animation: installed 0.4s ease 2.5s forwards;
}

.label.downloading::before {
  animation: rotate 2s ease-in-out 0.4s forwards;
}

.label.downloading .circle {
  animation: pulse 1s forwards, successCircle 0.3s ease 2.5s forwards;
}

.label.downloading .circle::before {
  animation: installing 2s ease-in-out forwards;
}

.label.downloading .circle .icon {
  opacity: 0;
  visibility: hidden;
}

.label.downloading .circle .checkmark {
  opacity: 1;
  visibility: visible;
  animation: drawCheck 0.3s ease 2.5s forwards;
}

.label.downloading .title {
  opacity: 0;
  visibility: hidden;
}

@keyframes pulse {
  0% { scale: 0.95; box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.7); }
  70% { scale: 1; box-shadow: 0 0 0 16px rgba(255, 255, 255, 0); }
  100% { scale: 0.95; box-shadow: 0 0 0 0 rgba(255, 255, 255, 0); }
}

@keyframes installing {
  from { height: 0; }
  to { height: 100%; }
}

@keyframes rotate {
  0% { transform: rotate(-90deg) translate(27px) rotate(0); opacity: 1; visibility: visible; }
  99% { transform: rotate(270deg) translate(27px) rotate(270deg); opacity: 1; visibility: visible; }
  100% { opacity: 0; visibility: hidden; }
}

@keyframes installed {
  100% { 
    width: 57px;
    border-color: #22c55e; 
    background-color: #22c55e;
  }
}

@keyframes successCircle {
  100% {
    background-color: white;
    transform: scale(0.8);
    border-radius: 50%;
  }
}

@keyframes drawCheck {
  from {
    stroke-dasharray: 40;
    stroke-dashoffset: 40;
  }
  to {
    stroke-dasharray: 40;
    stroke-dashoffset: 0;
  }
}
</style> 