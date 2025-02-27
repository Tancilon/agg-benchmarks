<script setup>
import { ref } from 'vue'
import { Download } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import { BarChart } from 'lucide-vue-next'

const route = useRoute()
const algorithmId = route.params.id
const activeMetric = ref('mAP')
const isDownloadingResults = ref(false)
const isDownloadingAlgorithm = ref(false)

// 模拟性能数据
const performanceData = {
  mAP: {
    xAxis: [1, 5, 10, 20, 50, 100],
    series: [
      {
        name: 'Market1501',
        data: [0.82, 0.78, 0.75, 0.71, 0.68, 0.65],
        color: '#336FFF'
      },
      {
        name: 'DukeMTMC-reID',
        data: [0.85, 0.81, 0.77, 0.74, 0.70, 0.67],
        color: '#D7BEFD'
      },
      {
        name: 'CUHK03',
        data: [0.79, 0.75, 0.72, 0.68, 0.65, 0.62],
        color: '#FF69B4'
      },
      {
        name: 'MovieLens',
        data: [0.81, 0.77, 0.74, 0.70, 0.67, 0.64],
        color: '#FF4444'
      }
    ]
  },
  NDCG: {
    xAxis: [1, 5, 10, 20, 50, 100],
    series: [
      {
        name: 'Market1501',
        data: [0.88, 0.85, 0.82, 0.79, 0.76, 0.73],
        color: '#336FFF'
      },
      {
        name: 'DukeMTMC-reID',
        data: [0.90, 0.87, 0.84, 0.81, 0.78, 0.75],
        color: '#D7BEFD'
      },
      {
        name: 'CUHK03',
        data: [0.86, 0.83, 0.80, 0.77, 0.74, 0.71],
        color: '#FF69B4'
      },
      {
        name: 'MovieLens',
        data: [0.87, 0.84, 0.81, 0.78, 0.75, 0.72],
        color: '#FF4444'
      }
    ]
  }
}

// 图表配置
const getChartOption = (metricType) => {
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: performanceData[metricType].series.map(item => item.name),
      bottom: 0,
      textStyle: {
        fontSize: 14,
        color: '#333'
      },
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
      name: '@k',
      boundaryGap: false,
      data: performanceData[metricType].xAxis,
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
    yAxis: {
      type: 'value',
      name: metricType,
      min: 0.5,
      max: 1.0,
      nameTextStyle: {
        fontSize: 14,
        padding: [0, 0, 10, 0],
        fontWeight: 'bold'
      },
      axisLabel: {
        fontSize: 12,
        margin: 12
      }
    },
    series: performanceData[metricType].series.map(item => ({
      name: item.name,
      type: 'line',
      data: item.data,
      itemStyle: {
        color: item.color
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
}

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

// 根据路由参数获取算法信息
const algorithmInfo = {
  name: algorithmId,
  category: "Unsupervised",  // 这里应该根据实际数据动态设置
  description: "A comprehensive rank aggregation algorithm that effectively combines multiple ranking lists."
}

const handleDownloadResults = () => {
  isDownloadingResults.value = true
  setTimeout(() => {
    isDownloadingResults.value = false
  }, 4000)
}

const handleDownloadAlgorithm = () => {
  isDownloadingAlgorithm.value = true
  setTimeout(() => {
    isDownloadingAlgorithm.value = false
  }, 4000)
}
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Hero Section -->
    <section class="bg-black px-4 py-16">
      <div class="container mx-auto">
        <div class="flex justify-between items-start">
          <div class="space-y-4">
            <h1 class="text-4xl font-bold text-white">{{ algorithmInfo.name }}</h1>
            <p class="text-zinc-400">{{ algorithmInfo.description }}</p>
            <span class="px-3 py-1 rounded-full text-sm inline-block"
                  :class="{
                    'bg-[#336FFF]/20 text-[#336FFF]': algorithmInfo.category === 'Unsupervised',
                    'bg-[#D7BEFD]/20 text-[#D7BEFD]': algorithmInfo.category === 'Supervised',
                    'bg-[#B6A494]/20 text-[#B6A494]': algorithmInfo.category === 'Semi-Supervised'
                  }">
              {{ algorithmInfo.category }}
            </span>
          </div>
          <div>
            <label class="label" :class="{ 'downloading': isDownloadingAlgorithm }">
              <input type="checkbox" class="input" v-model="isDownloadingAlgorithm" @click="handleDownloadAlgorithm" />
              <span class="circle">
                <svg class="icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 19V5m0 14-4-4m4 4 4-4"></path>
                </svg>
                <div class="square"></div>
              </span>
              <p class="title">Download Algorithm</p>
              <p class="title">Open</p>
            </label>
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
              @click="activeMetric = 'mAP'"
              :class="[
                'relative px-8 py-3 text-base font-bold rounded-lg transition-all duration-300',
                activeMetric === 'mAP' 
                  ? 'text-gray-900 bg-white shadow-sm' 
                  : 'text-gray-600 hover:text-gray-900'
              ]"
            >
              mAP
            </button>
            <button 
              @click="activeMetric = 'NDCG'"
              :class="[
                'relative px-8 py-3 text-base font-bold rounded-lg transition-all duration-300',
                activeMetric === 'NDCG' 
                  ? 'text-gray-900 bg-white shadow-sm' 
                  : 'text-gray-600 hover:text-gray-900'
              ]"
            >
              NDCG
            </button>
          </div>
        </div>

        <div class="h-px bg-gray-200 mb-8"></div>

        <!-- Metric Content -->
        <div class="space-y-8 transition-all duration-300 pt-4">
          <div>
            <h2 class="text-2xl font-semibold mb-3 text-gray-900">{{ metrics[activeMetric].title }}</h2>
            <p class="text-gray-600 text-sm leading-relaxed">{{ metrics[activeMetric].subtitle }}</p>
          </div>

          <!-- Performance Graph -->
          <div class="bg-white rounded-xl border border-gray-200 p-8 flex flex-col">
            <v-chart class="w-full h-[400px]" :option="getChartOption(activeMetric)" />
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
</style> 