<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { Download } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import { BarChart } from 'lucide-vue-next'

const route = useRoute()
const datasetId = route.params.id
const isDownloadingDataset = ref(false)
const isDownloadingResults = ref(false)

// 数据集信息
const datasetInfo = ref(null)
// 可用的指标列表
const availableMetrics = ref([])
// 当前选中的指标
const activeMetric = ref('')
// 性能数据
const performanceData = ref(null)

const downloadComplete = ref(false)

// 获取数据集信息
const fetchDatasetInfo = async () => {
  try {
    const response = await fetch(`/api/datasets/${datasetId}`)
    if (!response.ok) throw new Error('Failed to fetch dataset info')
    datasetInfo.value = await response.json()
  } catch (error) {
    console.error('Error fetching dataset info:', error)
  }
}

// 获取该数据集的可用指标
const fetchAvailableMetrics = async () => {
  try {
    const response = await fetch(`/api/results/metrics/${datasetId}`)
    if (!response.ok) throw new Error('Failed to fetch available metrics')
    availableMetrics.value = await response.json()
    // 默认选择第一个指标
    if (availableMetrics.value.length > 0) {
      activeMetric.value = availableMetrics.value[0]
      await fetchPerformanceData()
    }
  } catch (error) {
    console.error('Error fetching available metrics:', error)
  }
}

// 获取性能数据
const fetchPerformanceData = async () => {
  if (!activeMetric.value) return
  
  try {
    const response = await fetch(`/api/results/${datasetId}/${activeMetric.value}`)
    if (!response.ok) throw new Error('Failed to fetch performance data')
    performanceData.value = await response.json()
  } catch (error) {
    console.error('Error fetching performance data:', error)
  }
}

// 修改图表配置
const getChartOption = computed(() => {
  if (!performanceData.value) return {}
  
  const metricInfo = getMetricInfo(activeMetric.value)
  
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: performanceData.value.series.map(s => s.name),
      bottom: 0,
      textStyle: { fontSize: 14, color: '#333' },
      itemGap: 20
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      name: metricInfo.xAxis,
      boundaryGap: false,
      data: performanceData.value.xAxis,
      nameTextStyle: {
        fontSize: 14,
        padding: [0, 0, 0, 10],
        fontWeight: 'bold'
      }
    },
    yAxis: {
      type: 'value',
      name: metricInfo.yAxis,
      nameTextStyle: {
        fontSize: 14,
        padding: [0, 0, 10, 0],
        fontWeight: 'bold'
      }
    },
    series: performanceData.value.series.map(item => ({
      name: item.name,
      type: 'line',
      data: item.data,
      itemStyle: { color: item.color },
      lineStyle: { width: 2 },
      symbol: 'circle',
      symbolSize: 8
    }))
  }
})

// 监听指标变化
watch(activeMetric, () => {
  fetchPerformanceData()
})

// 组件挂载时获取数据
onMounted(async () => {
  await Promise.all([
    fetchDatasetInfo(),
    fetchAvailableMetrics()
  ])
})

// 修改 metrics 定义，使用函数来获取指标信息
const getMetricInfo = (metricName) => {
  const defaultInfo = {
    title: `Metric: ${metricName}`,
    subtitle: "Performance comparison across different algorithms",
    xAxis: "@k",
    yAxis: metricName
  }

  const metricInfoMap = {
    'mAP': {
      title: "Metric: mAP",
      subtitle: "Mean Average Precision at different k values",
      xAxis: "@k",
      yAxis: "mAP@k"
    },
    'NDCG': {
      title: "Metric: NDCG",
      subtitle: "Normalized Discounted Cumulative Gain at different k values",
      xAxis: "@k",
      yAxis: "NDCG@k"
    }
    // 可以添加更多指标的描述
  }

  return metricInfoMap[metricName] || defaultInfo
}

// 计算当前指标的信息
const currentMetricInfo = computed(() => {
  return getMetricInfo(activeMetric.value)
})

const handleDownloadDataset = async () => {
  if (isDownloadingDataset.value) return
  
  try {
    isDownloadingDataset.value = true
    downloadComplete.value = false
    
    const response = await fetch(`/api/datasets/${datasetId}/download`)
    
    if (!response.ok) {
      throw new Error('Failed to download dataset')
    }

    // 解析 Content-Disposition header
    const contentDisposition = response.headers.get('content-disposition')
    console.log('Raw Content-Disposition:', contentDisposition)
    
    let filename = 'dataset'
    if (contentDisposition) {
      const parts = contentDisposition.split('filename="')
      console.log('After first split:', parts)
      
      if (parts.length > 1) {
        const filenamePart = parts[1]
        console.log('Filename part:', filenamePart)
        
        filename = filenamePart.split('"')[0]
        console.log('Final filename:', filename)
      }
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)

    downloadComplete.value = true
    setTimeout(() => {
      downloadComplete.value = false
    }, 3000)
  } catch (error) {
    console.error('Error downloading dataset:', error)
  } finally {
    isDownloadingDataset.value = false
  }
}

const handleDownloadResults = () => {
  isDownloadingResults.value = true
  setTimeout(() => {
    isDownloadingResults.value = false
  }, 4000)
}
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Hero Section -->
    <section class="bg-black px-4 py-16">
      <div class="container mx-auto">
        <div v-if="datasetInfo" class="flex justify-between items-start">
          <div class="space-y-4">
            <h1 class="text-4xl font-bold text-white">{{ datasetInfo.name }}</h1>
            <p class="text-zinc-400">{{ datasetInfo.description }}</p>
            <p class="text-sm text-zinc-400">{{ datasetInfo.details }}</p>
          </div>
          <button 
            @click="handleDownloadDataset"
            :disabled="isDownloadingDataset"
            class="download-button"
            :class="{ 'downloading': isDownloadingDataset, 'downloaded': downloadComplete }"
          >
            <span class="button-content">
              <span class="icon">
                <Download v-if="!isDownloadingDataset && !downloadComplete" />
                <span v-else-if="isDownloadingDataset" class="loader"></span>
                <span v-else class="check">✓</span>
              </span>
              <span class="text">
                {{ isDownloadingDataset ? 'Downloading...' : downloadComplete ? 'Downloaded' : 'Download Dataset' }}
              </span>
            </span>
            <span class="progress-bar"></span>
          </button>
        </div>
        <div v-else class="animate-pulse">
          <!-- 数据集信息骨架屏 -->
          <div class="space-y-6">
            <!-- 标题骨架 -->
            <div class="h-10 bg-zinc-800 rounded-lg w-1/3"></div>
            
            <!-- 描述骨架 -->
            <div class="space-y-3">
              <div class="h-4 bg-zinc-800 rounded w-3/4"></div>
              <div class="h-4 bg-zinc-800 rounded w-2/3"></div>
            </div>
            
            <!-- 详情骨架 -->
            <div class="space-y-2">
              <div class="h-3 bg-zinc-800 rounded w-1/2"></div>
              <div class="h-3 bg-zinc-800 rounded w-1/3"></div>
            </div>
          </div>

          <!-- 下载按钮骨架 -->
          <div class="mt-8 flex justify-end">
            <div class="h-10 bg-zinc-800 rounded-xl w-32"></div>
          </div>

          <!-- 分页点骨架 -->
          <div class="flex gap-2 mt-8">
            <div v-for="i in 3" :key="i" 
                 class="w-2 h-2 rounded-full bg-zinc-800"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Metric Section -->
    <section class="py-16 px-4">
      <div class="container mx-auto">
        <!-- Header with Title and Tabs -->
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-2">
            <BarChart class="w-5 h-5 text-gray-900" />
            <h2 class="text-2xl font-semibold text-gray-900">Metrics</h2>
          </div>
          
          <!-- 指标选择骨架 -->
          <div v-if="!availableMetrics.length" class="animate-pulse">
            <div class="inline-flex p-1 rounded-xl bg-gray-100">
              <div class="h-9 w-24 bg-gray-200 rounded-lg"></div>
              <div class="h-9 w-24 bg-gray-200 rounded-lg ml-1"></div>
            </div>
          </div>
          
          <!-- 实际的指标选择按钮 -->
          <div v-else class="inline-flex p-1 rounded-xl bg-gray-100">
            <button 
              v-for="metric in availableMetrics"
              :key="metric"
              @click="activeMetric = metric"
              class="px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300"
              :class="[
                activeMetric === metric
                  ? 'bg-white text-gray-900 shadow-sm' 
                  : 'text-gray-600 hover:text-gray-900'
              ]"
            >
              {{ metric }}
            </button>
          </div>
        </div>

        <div class="h-px bg-gray-200 mb-8"></div>

        <!-- Metric Content -->
        <div class="space-y-8 transition-all duration-300 pt-4">
          <!-- 指标信息骨架 -->
          <div v-if="!performanceData" class="animate-pulse space-y-4">
            <div class="h-8 bg-gray-200 rounded w-1/4"></div>
            <div class="h-4 bg-gray-200 rounded w-1/2"></div>
          </div>
          
          <div v-else>
            <div>
              <h2 class="text-2xl font-semibold mb-3 text-gray-900">
                {{ currentMetricInfo.title }}
              </h2>
              <p class="text-gray-600 text-sm leading-relaxed">
                {{ currentMetricInfo.subtitle }}
              </p>
            </div>
          </div>

          <!-- Performance Graph -->
          <div class="bg-white rounded-xl border border-gray-200 p-8">
            <div v-if="!performanceData" class="animate-pulse">
              <!-- 图表骨架 -->
              <div class="h-[500px] flex flex-col justify-center space-y-4">
                <div class="h-1 bg-gray-200 rounded w-full"></div>
                <div class="h-1 bg-gray-200 rounded w-3/4"></div>
                <div class="h-1 bg-gray-200 rounded w-5/6"></div>
                <div class="h-1 bg-gray-200 rounded w-2/3"></div>
              </div>
              <!-- 图例骨架 -->
              <div class="flex justify-center gap-4 mt-8">
                <div v-for="i in 4" :key="i" 
                     class="h-4 bg-gray-200 rounded w-20"></div>
              </div>
            </div>
            
            <v-chart v-else
              class="h-[500px] mb-8"
              :option="getChartOption"
              :autoresize="true"
            />
            <div class="flex justify-end items-center gap-4">
              <label class="label" :class="{ 'downloading': isDownloadingResults }">
                <input type="checkbox" class="input" v-model="isDownloadingResults" @click="handleDownloadResults" />
                <span class="circle">
                  <svg class="icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 19V5m0 14-4-4m4 4 4-4"></path>
                  </svg>
                  <div class="square"></div>
                </span>
                <p class="title">Download Results</p>
                <p class="title">Open</p>
              </label>
            </div>
          </div>
        </div>  
      </div>
    </section>
  </div>
</template>

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

.label:hover {
  background-color: #2952cc;
  border-color: #2952cc;
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

.label .input {
  display: none;
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

.label .title:last-child {
  opacity: 0;
  visibility: hidden;
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

.label .circle .square {
  aspect-ratio: 1;
  width: 15px;
  border-radius: 2px;
  background-color: #fff;
  opacity: 0;
  visibility: hidden;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  transition: all 0.4s ease;
}

.label .circle::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  background-color: rgba(0, 0, 0, 0.1);
  width: 100%;
  height: 0;
  transition: all 0.4s ease;
}

.label.downloading {
  width: 57px;
  animation: installed 0.4s ease 3.5s forwards;
}

.label.downloading::before {
  animation: rotate 3s ease-in-out 0.4s forwards;
}

.label.downloading .circle {
  animation: pulse 1s forwards, circleDelete 0.2s ease 3.5s forwards;
  rotate: 180deg;
}

.label.downloading .circle::before {
  animation: installing 3s ease-in-out forwards;
}

.label.downloading .circle .icon {
  opacity: 0;
  visibility: hidden;
}

.label.downloading .circle .square {
  opacity: 1;
  visibility: visible;
}

.label.downloading .title {
  opacity: 0;
  visibility: hidden;
}

.label.downloading .title:last-child {
  animation: showInstalledMessage 0.4s ease 3.5s forwards;
  color: #22c55e;
}

@keyframes pulse {
  0% {
    scale: 0.95;
    box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.7);
  }
  70% {
    scale: 1;
    box-shadow: 0 0 0 16px rgba(255, 255, 255, 0);
  }
  100% {
    scale: 0.95;
    box-shadow: 0 0 0 0 rgba(255, 255, 255, 0);
  }
}

@keyframes installing {
  from {
    height: 0;
  }
  to {
    height: 100%;
  }
}

@keyframes rotate {
  0% {
    transform: rotate(-90deg) translate(27px) rotate(0);
    opacity: 1;
    visibility: visible;
  }
  99% {
    transform: rotate(270deg) translate(27px) rotate(270deg);
    opacity: 1;
    visibility: visible;
  }
  100% {
    opacity: 0;
    visibility: hidden;
  }
}

@keyframes installed {
  100% {
    width: 200px;
    border-color: #22c55e;
    background-color: #22c55e;
  }
}

@keyframes circleDelete {
  100% {
    opacity: 0;
    visibility: hidden;
  }
}

@keyframes showInstalledMessage {
  100% {
    opacity: 1;
    visibility: visible;
    right: 80px;
  }
}

/* 添加新的样式 */
.shadow-sm {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

/* 添加过渡动画 */
.transition-all {
  transition-property: all;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 300ms;
}

/* 添加骨架屏动画 */
.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: .5;
  }
}

.download-button {
  position: relative;
  padding: 12px 24px;
  border-radius: 8px;
  background: #007AFF;
  color: white;
  font-weight: 500;
  border: none;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
}

.download-button:hover:not(:disabled) {
  background: #0066CC;
  transform: translateY(-1px);
}

.download-button:disabled {
  cursor: default;
  opacity: 0.7;
}

.button-content {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

/* 加载动画 */
.loader {
  width: 16px;
  height: 16px;
  border: 2px solid #ffffff;
  border-bottom-color: transparent;
  border-radius: 50%;
  animation: rotate 1s linear infinite;
}

/* 进度条 */
.progress-bar {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 3px;
  background: rgba(255, 255, 255, 0.2);
  transform-origin: left;
}

.downloading .progress-bar {
  animation: progress 2s ease-in-out;
}

/* 完成状态 */
.downloaded {
  background: #34C759;
}

.downloaded .check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  animation: checkmark 0.3s ease-in-out;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes progress {
  0% { transform: scaleX(0); }
  100% { transform: scaleX(1); }
}

@keyframes checkmark {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}
</style> 