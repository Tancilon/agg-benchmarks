<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { Download, FileText, ExternalLink, BarChart } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import DownloadResultsDialog from '../components/DownloadResultsDialog.vue'

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

// 下载结果对话框控制
const isDownloadDialogOpen = ref(false)

// 添加对图表实例的引用
const chartRef = ref(null)

// 添加获取指标详情的函数
const metricDetails = ref({})

// 添加错误状态
const error = ref(null)

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
    const metrics = await response.json()
    availableMetrics.value = metrics
    // 默认选择第一个指标
    if (metrics.length > 0) {
      activeMetric.value = metrics[0]
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

// 添加获取指标详情的函数
const fetchMetricDetails = async (metricName) => {
  try {
    error.value = null;
    const response = await fetch(`/api/metrics/by-name/${metricName}`);
    
    // 添加详细的错误日志
    console.log('Response status:', response.status);
    console.log('Response headers:', Object.fromEntries(response.headers));
    
    const responseText = await response.text();
    console.log('Response body:', responseText);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}, body: ${responseText}`);
    }

    let data;
    try {
      data = JSON.parse(responseText);
    } catch (e) {
      console.error('Invalid JSON response:', responseText);
      throw new Error('服务器返回了无效的数据格式');
    }

    metricDetails.value[metricName] = data;
  } catch (error) {
    console.error('获取指标详情失败:', {
      error: error.message,
      stack: error.stack,
      metricName
    });
    error.value = `获取指标 ${metricName} 详情失败: ${error.message}`;
    throw error;
  }
}

// 修改指标变化的监听函数
watch(activeMetric, async () => {
  if (activeMetric.value && !metricDetails.value[activeMetric.value]) {
    await fetchMetricDetails(activeMetric.value)
  }
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
    // 获取文件扩展名
    const originalFileName = algorithmInfo.value.implementationFilePath.split('/').pop()
    const fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'))
    // 使用算法名_implementation格式，并保留原始扩展名
    a.download = `${algorithmInfo.value.name}_implementation${fileExtension}`
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

// 修改 getMetricInfo 函数
const getMetricInfo = (metricName) => {
  const defaultInfo = {
    title: `Metric: ${metricName}`,
    xAxis: "Dataset",
    yAxis: metricName,
    isKMetric: false
  }

  if (!metricName || !metricDetails.value[metricName]) {
    return defaultInfo
  }

  const details = metricDetails.value[metricName]
  const isKMetric = details.type === 'at-k'

  return {
    title: `Metric: ${metricName}`,
    xAxis: isKMetric ? "@k" : "Dataset",
    yAxis: isKMetric ? `${metricName}@k` : metricName,
    isKMetric: isKMetric
  }
}

// 计算当前指标的信息
const currentMetricInfo = computed(() => {
  return getMetricInfo(activeMetric.value)
})

// 图表配置
const getChartOption = computed(() => {
  if (!performanceData.value) return {}
  
  const metricInfo = currentMetricInfo.value
  
  // 判断是否是@k类型的指标
  const isKMetric = metricInfo.isKMetric
  
  if (isKMetric) {
    // 获取需要的颜色数量
    const seriesCount = performanceData.value.series.length;
    const colors = generateColors(seriesCount);

    // 折线图配置
    return {
      color: colors, // 设置全局颜色列表
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: performanceData.value.series.map(s => s.name),
        bottom: '5%',      // 距离底部的距离
        textStyle: { 
          fontSize: 14, 
          color: '#333',
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        itemGap: 25,        // 图例每项之间的间隔
        itemWidth: 25,      // 图例标记的图形宽度
        itemHeight: 14,     // 图例标记的图形高度
        type: 'plain',      // 普通图例
        orient: 'horizontal', // 水平布局
        width: '90%',       // 图例区域的宽度，留出左右空白
        left: 'center',     // 居中对齐
        padding: [0, 20],   // 图例区域内边距
        formatter: name => {
          // 如果名称过长，可以在这里处理换行
          return name.length > 30 ? name.substring(0, 27) + '...' : name;
        }
      },
      grid: {
        left: '5%',         // 左侧空白
        right: '5%',        // 右侧空白
        bottom: '25%',      // 增加底部空白，使图表与图例之间有更大距离
        top: '5%',          // 顶部空白
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
        nameLocation: 'middle',
        nameGap: 50,
        nameTextStyle: {
          fontSize: 14,
          color: '#86868b',
          padding: [0, 0, 30, 0],
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        min: (value) => {
          const minValue = Math.min(0, value.min * 0.9)
          return Number(minValue.toFixed(4))
        },
        max: (value) => {
          const maxValue = value.max * 1.1
          return Number(maxValue.toFixed(4))
        },
        axisLabel: {
          color: '#86868b',
          fontSize: 12,
          margin: 16,
          formatter: (value) => {
            return value.toFixed(4)
          }
        },
        splitLine: {
          lineStyle: {
            type: 'solid',
            color: '#f5f5f7'
          }
        },
        axisLine: {
          show: false
        },
        axisTick: {
          show: false
        }
      },
      series: performanceData.value.series.map((item, index) => ({
        name: item.name,
        type: 'line',
        data: item.data,
        itemStyle: { color: colors[index] },
        lineStyle: { 
          width: 2,
          type: index % 2 === 0 ? 'solid' : 'dashed' // 交替使用实线和虚线
        },
        symbol: 'circle',
        symbolSize: 8,
        smooth: false // 设置为true可以让线条更平滑
      }))
    }
  } else {
    // 打印数据结构以便调试
    console.log('Performance Data:', performanceData.value)
    console.log('Series Data:', performanceData.value.series)
    
    // 修改柱状图配置
    return {
      title: {
        show: false
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e7e7e7',
        borderWidth: 1,
        padding: [16, 20],
        textStyle: {
          color: '#1d1d1f',
          fontSize: 14
        },
        formatter: function(params) {
          const dataIndex = params[0].dataIndex
          const item = performanceData.value.series[dataIndex]
          return `
            <div style="font-weight: 500; margin-bottom: 8px; font-size: 15px;">
              ${item.name}
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin: 8px 0;">
              <span style="color: #86868b">${metricInfo.yAxis}:</span>
              <span style="color: #0066CC; font-weight: 500; font-size: 15px">
                ${item.data[0].toFixed(4)}
              </span>
            </div>
          `
        }
      },
      grid: {
        top: '10%',
        bottom: '25%',  // 增加底部空间
        left: '8%',
        right: '8%',
        height: 'auto',  // 改为自动高度，不再使用固定高度
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: performanceData.value.series.map(s => s.name),
        axisLabel: {
          show: true,
          interval: 0,
          rotate: 45,
          color: '#86868b',
          fontSize: 12,
          margin: 15,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
          formatter: function(value) {
            if (value.length > 20) {
              return value.substring(0, 17) + '...';
            }
            return value;
          }
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#e7e7e7'
          }
        },
        axisTick: {
          show: false
        }
      },
      yAxis: {
        type: 'value',
        name: metricInfo.yAxis,
        nameLocation: 'middle',
        nameGap: 50,
        nameTextStyle: {
          fontSize: 14,
          color: '#86868b',
          padding: [0, 0, 30, 0],
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        min: (value) => {
          if (isKMetric) {
            // 折线图使用原来的计算逻辑
            const minValue = Math.min(0, value.min * 0.9)
            return Number(minValue.toFixed(4))
          } else {
            // 柱状图使用新的计算逻辑
            const minValue = Math.min(...performanceData.value.series.map(s => s.data[0]));
            return Number((minValue * 0.9).toFixed(4));
          }
        },
        max: (value) => {
          if (isKMetric) {
            // 折线图使用原来的计算逻辑
            const maxValue = value.max * 1.1
            return Number(maxValue.toFixed(4))
          } else {
            // 柱状图使用新的计算逻辑
            const maxValue = Math.max(...performanceData.value.series.map(s => s.data[0]));
            return Number((maxValue * 1.05).toFixed(4));
          }
        },
        axisLabel: {
          color: '#86868b',
          fontSize: 12,
          margin: 16,
          formatter: (value) => {
            return value.toFixed(4)
          }
        },
        splitLine: {
          lineStyle: {
            type: 'solid',
            color: '#f5f5f7'
          }
        },
        axisLine: {
          show: false
        },
        axisTick: {
          show: false
        }
      },
      series: [{
        type: 'bar',
        data: performanceData.value.series.map((item, index) => ({
          name: item.name,
          value: item.data[0],
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

// 添加颜色生成函数
const generateColors = (count) => {
  const baseColors = [
    '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
    '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#0d947c',
    '#2f4554', '#61a0a8', '#d48265', '#749f83', '#ca8622',
    '#bda29a', '#6e7074', '#546570', '#c4ccd3', '#4780bf',
    '#7eb00a', '#e6a23c', '#f56c6c', '#909399', '#c67171'
  ];

  // 如果基础颜色不够，则动态生成更多颜色
  if (count <= baseColors.length) {
    return baseColors.slice(0, count);
  }

  const colors = [...baseColors];
  const hueStep = 360 / (count - baseColors.length);

  for (let i = baseColors.length; i < count; i++) {
    const hue = (i - baseColors.length) * hueStep;
    // 使用 HSL 颜色空间生成新颜色
    const color = `hsl(${hue}, 70%, 50%)`;
    colors.push(color);
  }

  return colors;
};

// 处理下载结果
const handleDownloadResults = async (downloadConfig) => {
  try {
    isDownloadingResults.value = true
    
    const requestData = {
      metric: downloadConfig.metric,
      includeCSV: true,
      selectedAlgorithms: downloadConfig.algorithms,
      selectedDatasets: downloadConfig.datasets,
      selectedKValues: downloadConfig.selectedKValues || []
    }

    const response = await fetch(`/api/results/download`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData)
    })

    if (!response.ok) {
      throw new Error('Failed to download results')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${algorithmInfo.value.name}_results.zip`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (error) {
    console.error('Error downloading results:', error)
  } finally {
    isDownloadingResults.value = false
  }
}

</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Hero Section -->
    <section class="bg-black px-4 py-16">
      <div class="container mx-auto">
        <div v-if="algorithmInfo" class="flex justify-between items-start">
          <div class="space-y-4">
            <h1 class="text-4xl font-bold text-white">{{ algorithmInfo.name }}</h1>
            <p class="text-zinc-400 max-w-3xl whitespace-normal break-words leading-relaxed">
              {{ algorithmInfo.description }}
            </p>
            <div class="flex flex-wrap gap-2">
              <span v-for="category in algorithmInfo.categories"
                    :key="category"
                    class="px-3 py-1 bg-zinc-800 text-zinc-300 rounded-full text-sm">
                {{ category }}
              </span>
            </div>
          </div>
          <!-- 操作按钮 -->
          <div class="flex gap-4 ml-4">
            <!-- 下载实现文件按钮 -->
            <div v-if="algorithmInfo?.implementationFilePath" class="flex items-center">
              <label 
                class="label implementation-btn" 
                :class="{ 'downloading': isDownloadingImplementation, 'complete': downloadComplete }"
                @click="handleDownloadImplementation"
              >
                <span class="circle">
                  <Download v-if="!isDownloadingImplementation" class="icon" />
                  <div v-else class="loader"></div>
                  <div class="progress-bar"></div>
                </span>
                <p class="title">Download Implementation</p>
                <p class="title">Complete</p>
              </label>
            </div>
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
            <v-chart class="w-full h-[500px]" :option="getChartOption" autoresize />
          </div>

          <!-- Download Results Button -->
          <div class="flex justify-end mt-8">
            <label 
              class="label" 
              :class="{ 'downloading': isDownloadingResults }"
              @click="isDownloadDialogOpen = true"
            >
              <span class="circle">
                <Download class="icon" />
                <div class="square"></div>
              </span>
              <p class="title">Download Results</p>
              <p class="title">Open</p>
            </label>
          </div>
        </div>
      </div>
    </section>

    <!-- Download Results Dialog -->
    <DownloadResultsDialog
      v-if="algorithmInfo"
      :is-open="isDownloadDialogOpen"
      :current-metric="activeMetric"
      :performance-data="performanceData"
      :algorithm-info="algorithmInfo"
      view-type="algorithm"
      @close="isDownloadDialogOpen = false"
      @download="handleDownloadResults"
    />
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

.implementation-btn {
  width: 320px;
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
    width: 160px;
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

.implementation-btn.downloading {
  width: 57px;
  animation: implementationInstalled 0.4s ease 3.5s forwards;
}

@keyframes implementationInstalled {
  100% {
    width: 200px;
    border-color: #22c55e;
    background-color: #22c55e;
  }
}
</style> 