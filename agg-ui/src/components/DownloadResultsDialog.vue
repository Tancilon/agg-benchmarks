<script setup>
import { ref, computed, watch } from 'vue'
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/vue'
import { Download, FileText, Table, FileCheck } from 'lucide-vue-next'

const props = defineProps({
  isOpen: Boolean,
  currentMetric: String,
  performanceData: {
    type: Object,
    default: () => ({})
  },
  algorithmInfo: Object,
  viewType: {
    type: String,
    validator: (value) => ['dataset', 'algorithm'].includes(value),
    required: true
  },
  onClose: Function
})

const emit = defineEmits(['close', 'download'])

// 下载选项
const downloadOptions = ref({
  includeCSV: true,  // 默认为 true 且不可更改
  selectedDatasets: [],
  selectedKValues: []
})

// 添加全选功能
const selectAllDatasets = ref(false)
const selectAllKValues = ref(false)

// 监听全选状态
watch(selectAllDatasets, (newValue) => {
  if (newValue) {
    downloadOptions.value.selectedDatasets = availableDatasets.value
  } else {
    downloadOptions.value.selectedDatasets = []
  }
})

watch(selectAllKValues, (newValue) => {
  if (newValue) {
    downloadOptions.value.selectedKValues = availableKValues.value
  } else {
    downloadOptions.value.selectedKValues = []
  }
})

// 监听选中项变化，更新全选状态
watch(() => downloadOptions.value.selectedDatasets, (newValue) => {
  selectAllDatasets.value = newValue.length === availableDatasets.value.length
}, { deep: true })

watch(() => downloadOptions.value.selectedKValues, (newValue) => {
  selectAllKValues.value = newValue.length === availableKValues.value.length
}, { deep: true })

// 添加指标详情的响应式状态
const metricDetails = ref({})
const error = ref(null)

// 获取指标详情的函数
const fetchMetricDetails = async (metricName) => {
  try {
    error.value = null;
    const response = await fetch(`/api/metrics/by-name/${metricName}`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
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

// 监听指标变化
watch(() => props.currentMetric, async (newMetric) => {
  if (newMetric && !metricDetails.value[newMetric]) {
    await fetchMetricDetails(newMetric)
  }
}, { immediate: true })

// 判断是否是@k类型的指标
const isAtKMetric = computed(() => {
  const metricInfo = getMetricInfo(props.currentMetric)
  return metricInfo.isKMetric
})

// 计算可用的数据集
const availableDatasets = computed(() => {
  if (!props.performanceData || !props.algorithmInfo) return []
  
  const datasets = new Set()
  
  if (isAtKMetric.value) {
    // 对于@k类型的指标，从series中提取数据集
    props.performanceData.series?.forEach(series => {
      // 排除当前算法名称
      if (series.name && !series.name.startsWith(props.algorithmInfo.name)) {
        datasets.add(series.name)
      }
    })
  } else {
    // 对于非@k类型的指标，从series名称中获取数据集
    props.performanceData.series?.forEach(series => {
      datasets.add(series.name)
    })
  }
  
  return Array.from(datasets)
})

// 计算可用的K值
const availableKValues = computed(() => {
  console.log('Is @k metric:', isAtKMetric.value) // 调试日志
  console.log('Performance data:', props.performanceData) // 调试日志
  if (!isAtKMetric.value || !props.performanceData?.xAxis) return []
  return props.performanceData.xAxis
})

// 监听对话框打开状态
watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    // 重置选择
    downloadOptions.value.selectedDatasets = []
    downloadOptions.value.selectedKValues = []
  }
}, { immediate: true })

// 添加用户信息表单数据
const userInfo = ref({
  name: '',
  email: '',
  institution: ''
})

// 添加表单验证错误
const formErrors = ref({
  name: '',
  email: '',
  institution: ''
})

// 验证邮箱格式
const validateEmail = (email) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

// 验证表单
const validateForm = () => {
  let isValid = true
  formErrors.value = {
    name: '',
    email: '',
    institution: ''
  }

  if (!userInfo.value.name.trim()) {
    formErrors.value.name = 'Please enter your name'
    isValid = false
  }

  if (!userInfo.value.email.trim()) {
    formErrors.value.email = 'Please enter your email address'
    isValid = false
  } else if (!validateEmail(userInfo.value.email)) {
    formErrors.value.email = 'Please enter a valid email address'
    isValid = false
  }

  if (!userInfo.value.institution.trim()) {
    formErrors.value.institution = 'Please enter your institution name'
    isValid = false
  }

  return isValid
}

// 修改下载处理函数
const handleDownload = async () => {
  if (!validateForm()) {
    return
  }

  try {
    // 先保存用户信息
    await fetch('/api/download-records', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        ...userInfo.value,
        downloadType: props.viewType,
        resourceName: props.viewType === 'dataset' ? props.algorithmInfo.name : props.algorithmInfo.name
      })
    })

    // 继续执行原有的下载逻辑
    const downloadConfig = {
      metric: props.currentMetric,
      includeCSV: true,
      selectedKValues: isAtKMetric.value ? downloadOptions.value.selectedKValues : [],
      datasets: [],
      algorithms: []
    }

    if (props.viewType === 'dataset') {
      downloadConfig.algorithms = downloadOptions.value.selectedDatasets
      downloadConfig.datasets = [props.algorithmInfo.name]
    } else {
      downloadConfig.algorithms = [props.algorithmInfo.name]
      downloadConfig.datasets = downloadOptions.value.selectedDatasets
    }

    emit('download', downloadConfig)
    emit('close')
  } catch (error) {
    console.error('Error saving download record:', error)
  }
}
</script>

<template>
  <Dialog :open="props.isOpen" @close="$emit('close')" class="relative z-50">
    <div class="fixed inset-0 bg-black/20 backdrop-blur-sm" aria-hidden="true" />
    <div class="fixed inset-0 flex items-center justify-center p-4">
      <DialogPanel class="w-full max-w-2xl bg-gray-50/95 rounded-2xl shadow-xl overflow-hidden">
        <!-- 标题区域 -->
        <div class="px-8 py-6 border-b border-gray-100">
          <DialogTitle as="div">
            <div class="flex items-center gap-3">
              <h2 class="text-xl font-semibold text-gray-800">Download Results</h2>
              <span class="text-gray-400">·</span>
              <span class="text-lg font-medium text-gray-600">{{ props.currentMetric }}</span>
            </div>
          </DialogTitle>
        </div>

        <!-- 内容区域 -->
        <div class="max-h-[calc(100vh-12rem)] overflow-y-auto custom-scrollbar">
          <div class="px-8 py-6 space-y-8">
            <!-- 移除文件格式选择部分，改为显示固定的 CSV 格式 -->
            <div class="space-y-3">
              <h3 class="text-sm font-semibold text-gray-800">File Format</h3>
              <div class="px-4 py-3 bg-gray-50 rounded-xl text-sm text-gray-600">
                CSV Data
              </div>
            </div>

            <!-- 根据视图类型显示不同的内容 -->
            <div v-if="props.viewType === 'dataset'" class="space-y-3">
              <!-- 当前数据集信息 -->
              <div class="space-y-3">
                <h3 class="text-sm font-semibold text-gray-800">Dataset</h3>
                <div class="px-4 py-3 bg-white rounded-xl text-sm text-gray-700 border border-gray-200">
                  {{ props.algorithmInfo?.name }}
                </div>
              </div>

              <!-- 算法选择 -->
              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <h3 class="text-sm font-semibold text-gray-800">Select Algorithms</h3>
                    <span class="text-xs bg-amber-50 text-amber-600 px-2 py-0.5 rounded-full font-medium">Required</span>
                  </div>
                  <button 
                    @click="selectAllDatasets = !selectAllDatasets"
                    class="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                  >
                    {{ selectAllDatasets ? 'Deselect All' : 'Select All' }}
                  </button>
                </div>
                <!-- 算法列表 -->
                <div class="relative">
                  <div class="w-full rounded-xl border border-gray-200 bg-white/80 shadow-sm overflow-y-auto custom-scrollbar"
                       :style="{ maxHeight: `${Math.min(availableDatasets.length * 40 + 16, 280)}px` }"
                  >
                    <div class="p-2 space-y-1">
                      <label 
                        v-for="algorithm in availableDatasets" 
                        :key="algorithm"
                        class="flex items-center px-3 py-2 rounded-lg hover:bg-gray-50 cursor-pointer group"
                      >
                        <input 
                          type="checkbox"
                          :value="algorithm"
                          v-model="downloadOptions.selectedDatasets"
                          class="sr-only peer"
                        >
                        <div class="w-5 h-5 rounded-lg border-2 border-gray-300 
                                    peer-checked:border-green-500 peer-checked:bg-green-500 
                                    flex items-center justify-center transition-all">
                          <svg v-if="downloadOptions.selectedDatasets.includes(algorithm)" 
                               class="w-3.5 h-3.5 text-white stroke-[3]" 
                               viewBox="0 0 24 24" 
                               fill="none" 
                               stroke="currentColor">
                            <polyline points="20 6 9 17 4 12"></polyline>
                          </svg>
                        </div>
                        <span class="ml-3 text-sm text-gray-600 group-hover:text-gray-900">{{ algorithm }}</span>
                      </label>
                    </div>
                  </div>
                  <div class="mt-2 text-xs text-gray-500">
                    Selected: {{ downloadOptions.selectedDatasets.length }} of {{ availableDatasets.length }}
                  </div>
                </div>
              </div>
            </div>
            <div v-else>
              <!-- 算法信息 -->
              <div class="space-y-3 mb-8">
                <h3 class="text-sm font-semibold text-gray-800">Algorithm</h3>
                <div class="px-4 py-3 bg-white rounded-xl text-sm text-gray-700 border border-gray-200">
                  {{ props.algorithmInfo?.name }}
                </div>
              </div>

              <!-- 数据集选择 -->
              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <h3 class="text-sm font-semibold text-gray-800">Select Datasets</h3>
                    <span class="text-xs bg-amber-50 text-amber-600 px-2 py-0.5 rounded-full font-medium">Required</span>
                  </div>
                  <button 
                    @click="selectAllDatasets = !selectAllDatasets"
                    class="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                  >
                    {{ selectAllDatasets ? 'Deselect All' : 'Select All' }}
                  </button>
                </div>
                <div class="relative">
                  <div class="w-full rounded-xl border border-gray-200 bg-white/80 shadow-sm overflow-y-auto custom-scrollbar"
                       :style="{ maxHeight: `${Math.min(availableDatasets.length * 40 + 16, 280)}px` }"
                  >
                    <div class="p-2 space-y-1">
                      <label 
                        v-for="dataset in availableDatasets" 
                        :key="dataset"
                        class="flex items-center px-3 py-2 rounded-lg hover:bg-gray-50 cursor-pointer group"
                      >
                        <input 
                          type="checkbox"
                          :value="dataset"
                          v-model="downloadOptions.selectedDatasets"
                          class="sr-only peer"
                        >
                        <div class="w-5 h-5 rounded-lg border-2 border-gray-300 
                                    peer-checked:border-green-500 peer-checked:bg-green-500 
                                    flex items-center justify-center transition-all">
                          <svg v-if="downloadOptions.selectedDatasets.includes(dataset)" 
                               class="w-3.5 h-3.5 text-white stroke-[3]" 
                               viewBox="0 0 24 24" 
                               fill="none" 
                               stroke="currentColor">
                            <polyline points="20 6 9 17 4 12"></polyline>
                          </svg>
                        </div>
                        <span class="ml-3 text-sm text-gray-600 group-hover:text-gray-900">{{ dataset }}</span>
                      </label>
                    </div>
                  </div>
                  <div class="mt-2 text-xs text-gray-500">
                    Selected: {{ downloadOptions.selectedDatasets.length }} of {{ availableDatasets.length }}
                  </div>
                </div>
              </div>
            </div>

            <!-- K值选择 -->
            <div v-if="isAtKMetric" class="space-y-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <h3 class="text-sm font-semibold text-gray-800">Select K Values</h3>
                  <span class="text-xs bg-amber-50 text-amber-600 px-2 py-0.5 rounded-full font-medium">Required</span>
                </div>
                <button 
                  @click="selectAllKValues = !selectAllKValues"
                  class="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                >
                  {{ selectAllKValues ? 'Deselect All' : 'Select All' }}
                </button>
              </div>
              <div class="relative">
                <div class="w-full rounded-xl border border-gray-200 bg-white/80 shadow-sm overflow-y-auto custom-scrollbar"
                     :style="{ maxHeight: `${Math.min(availableKValues.length * 40 + 16, 200)}px` }"
                >
                  <div class="p-2 space-y-1">
                    <label 
                      v-for="k in availableKValues" 
                      :key="k"
                      class="flex items-center px-3 py-2 rounded-lg hover:bg-gray-50 cursor-pointer group"
                    >
                      <input 
                        type="checkbox"
                        :value="k"
                        v-model="downloadOptions.selectedKValues"
                        class="sr-only peer"
                      >
                      <div class="w-5 h-5 rounded-lg border-2 border-gray-300 
                                  peer-checked:border-green-500 peer-checked:bg-green-500 
                                  flex items-center justify-center transition-all">
                        <svg v-if="downloadOptions.selectedKValues.includes(k)" 
                             class="w-3.5 h-3.5 text-white stroke-[3]" 
                             viewBox="0 0 24 24" 
                             fill="none" 
                             stroke="currentColor">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                      </div>
                      <span class="ml-3 text-sm text-gray-600 group-hover:text-gray-900">K = {{ k }}</span>
                    </label>
                  </div>
                </div>
                <div class="mt-2 text-xs text-gray-500">
                  Selected: {{ downloadOptions.selectedKValues.length }} of {{ availableKValues.length }}
                </div>
              </div>
            </div>

            <!-- 添加用户信息表单 -->
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <h3 class="text-sm font-semibold text-gray-800">User Information</h3>
                  <span class="text-xs bg-amber-50 text-amber-600 px-2 py-0.5 rounded-full font-medium">Required</span>
                </div>
              </div>
              <div class="space-y-3">
                <!-- 名称输入 -->
                <div class="relative">
                  <div class="w-full rounded-xl border border-gray-200 bg-white/80 shadow-sm hover:border-gray-300">
                    <label class="block text-sm font-medium px-4 pt-2">
                      Name
                      <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="userInfo.name"
                      type="text"
                      class="w-full px-4 pb-3 pt-1 bg-transparent border-0 rounded-xl"
                      :class="{ 'border-red-500': formErrors.name }"
                      placeholder="Enter your name"
                    />
                  </div>
                  <p v-if="formErrors.name" class="mt-1 text-sm text-red-500">
                    {{ formErrors.name }}
                  </p>
                </div>

                <!-- 邮箱输入 -->
                <div class="relative">
                  <div class="w-full rounded-xl border border-gray-200 bg-white/80 shadow-sm hover:border-gray-300">
                    <label class="block text-sm font-medium px-4 pt-2">
                      Email
                      <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="userInfo.email"
                      type="email"
                      class="w-full px-4 pb-3 pt-1 bg-transparent border-0 rounded-xl"
                      :class="{ 'border-red-500': formErrors.email }"
                      placeholder="Enter your email"
                    />
                  </div>
                  <p v-if="formErrors.email" class="mt-1 text-sm text-red-500">
                    {{ formErrors.email }}
                  </p>
                </div>

                <!-- 机构输入 -->
                <div class="relative">
                  <div class="w-full rounded-xl border border-gray-200 bg-white/80 shadow-sm hover:border-gray-300">
                    <label class="block text-sm font-medium px-4 pt-2">
                      Institution
                      <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="userInfo.institution"
                      type="text"
                      class="w-full px-4 pb-3 pt-1 bg-transparent border-0 rounded-xl"
                      :class="{ 'border-red-500': formErrors.institution }"
                      placeholder="Enter your institution"
                    />
                  </div>
                  <p v-if="formErrors.institution" class="mt-1 text-sm text-red-500">
                    {{ formErrors.institution }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="px-8 py-5 border-t border-gray-100">
          <div class="flex justify-end gap-3">
            <button
              @click="$emit('close')"
              class="px-5 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors"
            >
              Cancel
            </button>
            <button
              @click="handleDownload"
              class="px-5 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg 
                     hover:bg-indigo-700 shadow-sm
                     disabled:opacity-50 disabled:cursor-not-allowed 
                     transition-colors duration-200 
                     flex items-center gap-2"
              :disabled="
                !downloadOptions.selectedDatasets.length || 
                (isAtKMetric && !downloadOptions.selectedKValues.length) || 
                (!downloadOptions.includeCSV) ||
                !userInfo.name ||
                !userInfo.email ||
                !userInfo.institution
              "
            >
              <Download class="w-4 h-4" />
              Download
            </button>
          </div>
        </div>
      </DialogPanel>
    </div>
  </Dialog>
</template>

<style scoped>
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(79, 70, 229, 0.2) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(79, 70, 229, 0.2);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(79, 70, 229, 0.3);
}

/* 优化选中状态的过渡动画 */
.peer-checked\:border-green-500 {
  transition: all 0.2s ease;
}

/* 添加选中时的缩放动画 */
@keyframes check-scale {
  0% { transform: scale(0.8); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.peer:checked + div svg {
  animation: check-scale 0.2s ease-out;
}

/* 输入框容器样式 */
.relative > div {
  transition: all 0.2s ease;
  border-color: rgba(0, 0, 0, 0.1);
}

/* 输入框焦点状态 */
.relative > div:focus-within {
  border-color: #0066cc;
  box-shadow: 0 0 0 4px rgba(0, 102, 204, 0.1);
}

/* 输入框样式 */
input {
  transition: all 0.2s ease;
  outline: none !important;
}

input:focus {
  outline: none !important;
  box-shadow: none !important;
}

/* 标签动画 */
.relative label {
  transition: all 0.2s ease;
  color: #666;
}

.relative > div:focus-within label {
  color: #0066cc;
}
</style> 