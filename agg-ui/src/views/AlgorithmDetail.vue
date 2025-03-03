<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { Download, FileText, ExternalLink, BarChart } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'

const route = useRoute()
const algorithmId = route.params.id
const isDownloadingImplementation = ref(false)
const downloadComplete = ref(false)
const isDownloadingResults = ref(false)

// 算法信息
const algorithmInfo = ref(null)
const performanceData = ref(null)
// 可用的指标列表
const availableMetrics = ref([])
// 当前选中的指标
const activeMetric = ref('')

// 获取算法详情
const fetchAlgorithmInfo = async () => {
  try {
    const response = await fetch(`/api/algorithms/${algorithmId}`)
    if (!response.ok) throw new Error('Failed to fetch algorithm info')
    algorithmInfo.value = await response.json()
  } catch (error) {
    console.error('Error fetching algorithm info:', error)
  }
}

// 获取该算法的可用指标
const fetchAvailableMetrics = async () => {
  try {
    const response = await fetch(`/api/algorithms/${algorithmId}/metrics`)
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
    const response = await fetch(`/api/algorithms/${algorithmId}/performance/${activeMetric.value}`)
    if (!response.ok) throw new Error('Failed to fetch performance data')
    performanceData.value = await response.json()
  } catch (error) {
    console.error('Error fetching performance data:', error)
  }
}

// 监听指标变化
watch(activeMetric, async () => {
  await fetchPerformanceData()
})

onMounted(async () => {
  await fetchAlgorithmInfo()
  await fetchAvailableMetrics()
})

// 下载实现文件
const handleDownloadImplementation = async () => {
  if (isDownloadingImplementation.value) return
  
  try {
    isDownloadingImplementation.value = true
    downloadComplete.value = false
    
    const response = await fetch(`/api/algorithms/${algorithmId}/download`)
    
    if (!response.ok) {
      throw new Error('Failed to download implementation')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${algorithmInfo.value.name}_implementation`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
    
    downloadComplete.value = true
  } catch (error) {
    console.error('Error downloading implementation:', error)
  } finally {
    setTimeout(() => {
      isDownloadingImplementation.value = false
      downloadComplete.value = false
    }, 2000)
  }
}

// 打开论文链接
const openPaperUrl = () => {
  if (algorithmInfo.value?.paperUrl) {
    window.open(algorithmInfo.value.paperUrl, '_blank')
  }
}

// 获取指标信息
const getMetricInfo = (metricName) => {
  const defaultInfo = {
    title: metricName,
    yAxis: metricName,
    xAxis: "Dataset",
    isKMetric: false
  }

  const metricInfoMap = {
    'mAP': {
      title: "mAP",
      subtitle: "Mean Average Precision at different k values",
      xAxis: "@k",
      yAxis: "mAP@k",
      isKMetric: true
    },
    'NDCG': {
      title: "NDCG",
      subtitle: "Normalized Discounted Cumulative Gain at different k values",
      xAxis: "@k",
      yAxis: "NDCG@k",
      isKMetric: true
    },
    'Precision': {
      title: "Precision",
      yAxis: "Precision",
      xAxis: "Dataset",
      isKMetric: false
    },
    'Recall': {
      title: "Recall",
      yAxis: "Recall",
      xAxis: "Dataset",
      isKMetric: false
    }
  }

  return metricInfoMap[metricName] || defaultInfo
}

// 计算当前指标的信息
const currentMetricInfo = computed(() => {
  return getMetricInfo(activeMetric.value)
})

// 图表配置
const getChartOption = computed(() => {
  if (!performanceData.value) return {}
  
  const metricInfo = currentMetricInfo.value
  
  // 计算数据的最大值和最小值
  const allValues = performanceData.value.series.flatMap(item => item.data)
  const minValue = Math.min(...allValues)
  const maxValue = Math.max(...allValues)
  
  // 基础配置
  const baseConfig = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e7e7e7',
      borderWidth: 1,
      padding: [16, 20],
      textStyle: {
        color: '#1d1d1f',
        fontSize: 14
      }
    },
    legend: {
      data: performanceData.value.series.map(item => item.name),
      bottom: 0,
      textStyle: {
        fontSize: 14,
        color: '#333'
      },
      itemGap: 20
    },
    grid: {
      top: '10%',
      left: '8%',
      right: '8%',
      bottom: '15%',
      containLabel: true,
      height: 'auto',
      aspectRatio: 0.8
    },
    yAxis: {
      type: 'value',
      name: metricInfo.yAxis,
      nameTextStyle: {
        fontSize: 14,
        padding: [0, 0, 10, 0],
        fontWeight: 'bold'
      },
      min: value => Number((minValue - Math.abs(minValue) * 0.1).toFixed(4)),
      max: value => Number((maxValue + Math.abs(maxValue) * 0.1).toFixed(4)),
      axisLabel: {
        fontSize: 12,
        margin: 12,
        formatter: value => value.toFixed(4)
      }
    }
  }

  if (metricInfo.isKMetric) {
    // 折线图配置
    return {
      ...baseConfig,
      xAxis: {
        type: 'category',
        name: metricInfo.xAxis,
        boundaryGap: false,
        data: performanceData.value.xAxis,
        nameTextStyle: {
          fontSize: 14,
          padding: [0, 0, 0, 10],
          fontWeight: 'bold'
        },
        axisLabel: {
          fontSize: 12,
          margin: 12
        }
      },
      series: performanceData.value.series.map(item => ({
        name: item.name,
        type: 'line',
        data: item.data,
        itemStyle: {
          color: item.color || undefined
        },
        lineStyle: {
          width: 2
        },
        symbol: 'circle',
        symbolSize: 8,
        emphasis: {
          focus: 'series',
          label: {
            show: true,
            fontSize: 14,
            color: '#333'
          }
        }
      }))
    }
  } else {
    // 柱状图配置
    return {
      ...baseConfig,
      xAxis: {
        type: 'category',
        data: performanceData.value.series.map(item => item.name),  // 使用数据集名称作为横坐标
        axisLabel: {
          interval: 0,
          rotate: 45,
          fontSize: 12,
          margin: 12
        }
      },
      series: [{
        type: 'bar',
        data: performanceData.value.series.map(item => ({
          value: item.data[0],  // 只取第一个值
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#007AFF' },
              { offset: 1, color: '#0066CC' }
            ]),
            borderRadius: [6, 6, 0, 0]
          }
        })),
        barWidth: '50%',
        barGap: '30%',
        label: {
          show: true,
          position: 'top',
          formatter: function(params) {
            return params.value.toFixed(4)
          },
          fontSize: 12,
          color: '#86868b',
          distance: 15,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#34C759' },
              { offset: 1, color: '#28a745' }
            ])
          }
        }
      }]
    }
  }
})

const metrics = {
  mAP: {
    title: "Metric: mAP",
    subtitle: "Performance comparison across different datasets using mAP metric",
    xAxis: "k",
    yAxis: "mAP@k"
  },
  NDCG: {
    title: "Metric: NDCG",
    subtitle: "Performance comparison across different datasets using NDCG metric",
    xAxis: "k",
    yAxis: "NDCG@k"
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
        <div class="flex flex-col space-y-8">
          <!-- 算法信息 -->
          <div class="flex justify-between items-start">
            <div class="space-y-4">
              <h1 class="text-4xl font-bold text-white">{{ algorithmInfo?.name }}</h1>
              <p class="text-zinc-400">{{ algorithmInfo?.description }}</p>
              <span class="px-3 py-1 rounded-full text-sm inline-block"
                    :class="{
                      'bg-[#336FFF]/20 text-[#336FFF]': algorithmInfo?.category === 'Unsupervised',
                      'bg-[#D7BEFD]/20 text-[#D7BEFD]': algorithmInfo?.category === 'Supervised',
                      'bg-[#B6A494]/20 text-[#B6A494]': algorithmInfo?.category === 'Semi-Supervised'
                    }">
                {{ algorithmInfo?.category }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex gap-4">
            <!-- 下载实现文件按钮 -->
            <button v-if="algorithmInfo?.implementationFile"
                    @click="handleDownloadImplementation"
                    class="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-xl 
                           hover:bg-blue-700 transition-all duration-200 relative overflow-hidden"
                    :class="{ 'downloading': isDownloadingImplementation }">
              <span class="flex items-center gap-2">
                <Download v-if="!isDownloadingImplementation && !downloadComplete" 
                         class="w-5 h-5" />
                <div v-if="isDownloadingImplementation" class="loader"></div>
                <FileText v-if="downloadComplete" class="w-5 h-5" />
                Download Implementation
              </span>
              <div v-if="isDownloadingImplementation" class="progress-bar"></div>
            </button>

            <!-- 查看论文按钮 -->
            <button v-if="algorithmInfo?.paperUrl"
                    @click="openPaperUrl"
                    class="flex items-center gap-2 px-6 py-3 bg-zinc-800 text-white rounded-xl 
                           hover:bg-zinc-700 transition-all duration-200">
              <ExternalLink class="w-5 h-5" />
              View Paper
            </button>
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
          
          <div class="inline-flex p-1 rounded-xl bg-gray-100">
            <button 
              v-for="metric in availableMetrics"
              :key="metric"
              @click="activeMetric = metric"
              :class="[
                'relative px-8 py-3 text-base font-bold rounded-lg transition-all duration-300',
                activeMetric === metric
                  ? 'text-gray-900 bg-white shadow-sm' 
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
          <div>
            <h2 class="text-2xl font-semibold mb-3 text-gray-900">{{ currentMetricInfo.title }}</h2>
            <p class="text-gray-600 text-sm leading-relaxed">
              Performance comparison across different datasets
            </p>
          </div>

          <!-- Performance Graph -->
          <div v-if="performanceData" class="bg-white rounded-xl border border-gray-200 p-8 flex flex-col">
            <v-chart class="w-full h-[400px]" :option="getChartOption" />
          </div>

          <!-- Download Results Button -->
          <div class="flex justify-end mt-8">
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

.downloading {
  pointer-events: none;
}

.loader {
  width: 16px;
  height: 16px;
  border: 2px solid #ffffff;
  border-bottom-color: transparent;
  border-radius: 50%;
  animation: rotate 1s linear infinite;
}

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

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes progress {
  0% { transform: scaleX(0); }
  100% { transform: scaleX(1); }
}
</style> 