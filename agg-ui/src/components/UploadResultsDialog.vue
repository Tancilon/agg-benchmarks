<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { FileText, X, Upload as UploadIcon, Search } from 'lucide-vue-next'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'submit'])

// 预定义的性能指标列表
const metrics = [
  {
    id: 'mAP',
    name: 'Mean Average Precision (mAP)',
    description: 'Evaluates ranking quality across all recall levels',
    range: '0-1',
    category: 'Ranking'
  },
  {
    id: 'NDCG',
    name: 'Normalized Discounted Cumulative Gain (NDCG)',
    description: 'Measures ranking quality with position-weighted relevance',
    range: '0-1',
    category: 'Ranking'
  },
  {
    id: 'precision',
    name: 'Precision@K',
    description: 'Proportion of relevant items in top K results',
    range: '0-1',
    category: 'Precision'
  },
  {
    id: 'recall',
    name: 'Recall@K',
    description: 'Proportion of relevant items found in top K results',
    range: '0-1',
    category: 'Recall'
  }
]

// 初始化表单数据
const formData = ref({
  algorithm: '',
  dataset: '',
  metrics: {},
  metricsFile: null,
  file: null
})

// 初始化 metrics 数据结构
metrics.forEach(metric => {
  formData.value.metrics[metric.id] = {
    enabled: false,
    value: ''
  }
})

// 从现有数据中获取算法和数据集列表
const algorithms = [
  'Comb*',
  'CSRA',
  'Borda Count',
  'MC1-4',
  'RRF',
  'AggRankDE',
  'SSRA',
  'IRANK'
]

const datasets = [
  'Market1501',
  'DukeMTMC-reID',
  'CUHK03 (detected)',
  'CUHK03 (labeled)',
  'MovieLens 1M',
  'NRGLC',
  'WUR 2022'
]

// 搜索和展开状态
const searchQuery = ref('')
const isMetricsDropdownOpen = ref(false)
const selectedMetrics = ref([])

// 表单错误状态
const formErrors = ref({
  algorithm: '',
  dataset: '',
  metrics: '',
  file: ''
})

// 文件相关状态
const fileName = ref('')
const metricsFileName = ref('')

// 添加文件格式选择状态
const activeFileFormat = ref('json') // 'json' 或 'csv'
const fileErrors = ref('')

// 添加搜索和下拉状态
const algorithmSearchQuery = ref('')
const datasetSearchQuery = ref('')
const isAlgorithmDropdownOpen = ref(false)
const isDatasetDropdownOpen = ref(false)

// 添加下拉引用
const algorithmDropdown = ref(null)
const datasetDropdown = ref(null)

// 添加上传模式选择
const uploadMode = ref('form') // 'form' 或 'file'

// 过滤算法列表
const filteredAlgorithms = computed(() => {
  if (!algorithmSearchQuery.value.trim()) return algorithms
  const query = algorithmSearchQuery.value.toLowerCase()
  return algorithms.filter(algo => 
    algo.toLowerCase().includes(query)
  )
})

// 过滤数据集列表
const filteredDatasets = computed(() => {
  if (!datasetSearchQuery.value.trim()) return datasets
  const query = datasetSearchQuery.value.toLowerCase()
  return datasets.filter(dataset => 
    dataset.toLowerCase().includes(query)
  )
})

// 处理文件选择
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  const fileType = activeFileFormat.value === 'json' ? 'application/json' : 'text/csv'
  const fileExt = activeFileFormat.value === 'json' ? '.json' : '.csv'
  
  if (!file.name.toLowerCase().endsWith(fileExt)) {
    fileErrors.value = `Please upload a ${activeFileFormat.value.toUpperCase()} file`
    fileName.value = ''
    formData.value.file = null
    e.target.value = ''
    return
  }
  
  fileName.value = file.name
  formData.value.file = file
  fileErrors.value = ''
}

// 处理指标文件选择
const handleMetricsFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    if (file.type !== 'application/json') {
      alert('Please upload a JSON file')
      return
    }
    metricsFileName.value = file.name
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target.result)
        // 验证并更新指标值
        metrics.forEach(metric => {
          if (data[metric.id] !== undefined) {
            formData.value.metrics[metric.id].value = data[metric.id]
            formData.value.metrics[metric.id].enabled = true
            if (!selectedMetrics.value.find(m => m.id === metric.id)) {
              selectedMetrics.value.push(metrics.find(m => m.id === metric.id))
            }
          }
        })
      } catch (error) {
        alert('Invalid JSON file')
      }
    }
    reader.readAsText(file)
    formData.value.metricsFile = file
  }
}

// 处理指标选择
const toggleMetric = (metric) => {
  const index = selectedMetrics.value.findIndex(m => m.id === metric.id)
  if (index === -1) {
    selectedMetrics.value.push(metric)
    formData.value.metrics[metric.id].enabled = true
  } else {
    selectedMetrics.value.splice(index, 1)
    formData.value.metrics[metric.id].enabled = false
    formData.value.metrics[metric.id].value = ''
  }
}

// 检查指标是否被选中
const isMetricSelected = (metricId) => {
  return formData.value.metrics[metricId]?.enabled || false
}

// 过滤指标
const filteredMetrics = computed(() => {
  if (!searchQuery.value.trim()) return metrics
  const query = searchQuery.value.toLowerCase()
  return metrics.filter(metric => 
    metric.name.toLowerCase().includes(query) ||
    metric.description.toLowerCase().includes(query) ||
    metric.category.toLowerCase().includes(query)
  )
})

// 表单验证
const validateForm = () => {
  let isValid = true
  formErrors.value = {
    algorithm: '',
    dataset: '',
    metrics: '',
    file: ''
  }

  if (!formData.value.algorithm) {
    formErrors.value.algorithm = 'Algorithm is required'
    isValid = false
  }

  if (!formData.value.dataset) {
    formErrors.value.dataset = 'Dataset is required'
    isValid = false
  }

  const hasValidMetric = Object.values(formData.value.metrics).some(
    metric => metric.enabled && metric.value !== ''
  )
  
  if (!hasValidMetric) {
    formErrors.value.metrics = 'At least one metric value is required'
    isValid = false
  }

  if (!formData.value.file) {
    formErrors.value.file = 'Results file is required'
    isValid = false
  }

  return isValid
}

// 处理提交
const handleSubmit = () => {
  if (!validateForm()) return
  emit('submit', formData.value)
  emit('close')
}

// 修改搜索处理逻辑
const handleSearchInput = (e) => {
  searchQuery.value = e.target.value
}

// 添加高亮文本的方法
const highlightText = (text, query) => {
  if (!query.trim()) return text
  
  const searchText = query.toLowerCase()
  const index = text.toLowerCase().indexOf(searchText)
  if (index === -1) return text
  
  const before = text.slice(0, index)
  const match = text.slice(index, index + searchText.length)
  const after = text.slice(index + searchText.length)
  
  return `${before}<mark class="bg-yellow-100 rounded px-0.5">${match}</mark>${after}`
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (algorithmDropdown.value && !algorithmDropdown.value.contains(e.target)) {
    isAlgorithmDropdownOpen.value = false
  }
  if (datasetDropdown.value && !datasetDropdown.value.contains(e.target)) {
    isDatasetDropdownOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 添加批量上传文件处理
const handleBatchFileUpload = (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  if (!file.name.toLowerCase().endsWith('.json') && !file.name.toLowerCase().endsWith('.csv')) {
    alert('Please upload a JSON or CSV file')
    e.target.value = ''
    return
  }
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      let data
      if (file.name.toLowerCase().endsWith('.json')) {
        data = JSON.parse(e.target.result)
      } else {
        // 处理 CSV 文件
        const csvRows = e.target.result.split('\n')
        data = {}
        for (let i = 1; i < csvRows.length; i++) {
          const [key, value] = csvRows[i].split(',')
          if (key && value) {
            data[key.trim()] = parseFloat(value.trim())
          }
        }
      }
      
      // 验证数据格式
      if (!data.algorithm || !data.dataset || !data.metrics) {
        throw new Error('Invalid file format. File must contain algorithm, dataset, and metrics information.')
      }
      
      // 更新表单数据
      formData.value.algorithm = data.algorithm
      formData.value.dataset = data.dataset
      
      // 更新指标
      Object.entries(data.metrics).forEach(([metricId, value]) => {
        const metric = metrics.find(m => m.id === metricId)
        if (metric) {
          formData.value.metrics[metricId] = {
            enabled: true,
            value: value
          }
          if (!selectedMetrics.value.find(m => m.id === metricId)) {
            selectedMetrics.value.push(metric)
          }
        }
      })
      
      // 自动提交
      handleSubmit()
    } catch (error) {
      alert(`Error processing file: ${error.message}`)
    }
  }
  
  reader.readAsText(file)
}
</script>

<template>
  <Transition name="dialog">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- Backdrop with blur -->
      <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="emit('close')"></div>
      
      <!-- Dialog -->
      <div class="relative w-[1000px] max-h-[85vh] overflow-y-auto bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl custom-scrollbar">
        <!-- Close Button -->
        <button 
          @click="emit('close')"
          class="absolute top-4 right-4 p-2 rounded-full hover:bg-black/5 transition-colors"
        >
          <X class="w-5 h-5 text-gray-500" />
        </button>

        <!-- Upload Mode Tabs -->
        <div class="border-b border-gray-200">
          <div class="flex px-8 pt-6">
            <button
              v-for="mode in [
                { id: 'form', name: 'Form Upload', icon: FileText },
                { id: 'file', name: 'Batch Upload', icon: UploadIcon }
              ]"
              :key="mode.id"
              @click="uploadMode = mode.id"
              class="relative px-6 py-3 text-sm font-medium transition-colors"
              :class="uploadMode === mode.id
                ? 'text-blue-600'
                : 'text-gray-500 hover:text-gray-700'"
            >
              <div class="flex items-center gap-2">
                <component :is="mode.icon" class="w-4 h-4" />
                {{ mode.name }}
              </div>
              <!-- Active Indicator -->
              <div
                class="absolute bottom-0 left-0 right-0 h-0.5 transition-colors"
                :class="uploadMode === mode.id ? 'bg-blue-600' : 'bg-transparent'"
              ></div>
            </button>
          </div>
        </div>

        <!-- Form Upload Mode -->
        <div v-if="uploadMode === 'form'" class="p-8 space-y-8">
          <!-- Algorithm Selection -->
          <div class="space-y-2" ref="algorithmDropdown">
            <label class="block text-sm font-medium text-gray-700">
              Algorithm
              <span class="text-red-500 ml-1">*</span>
            </label>
            <div class="relative">
              <div 
                @click.stop="isAlgorithmDropdownOpen = !isAlgorithmDropdownOpen"
                :class="[
                  'w-full px-4 py-3 bg-black/5 border-2 rounded-xl cursor-pointer transition-colors',
                  formErrors.algorithm 
                    ? 'border-red-300' 
                    : 'border-transparent hover:border-blue-200'
                ]"
              >
                {{ formData.algorithm || 'Select Algorithm' }}
              </div>

              <!-- Dropdown Panel -->
              <div 
                v-if="isAlgorithmDropdownOpen"
                class="absolute z-50 w-full mt-2 bg-white/90 backdrop-blur-xl border border-gray-100 rounded-xl shadow-2xl overflow-hidden"
              >
                <!-- Search Box -->
                <div class="p-3 border-b border-gray-100">
                  <div class="relative">
                    <Search class="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                    <input
                      v-model="algorithmSearchQuery"
                      type="text"
                      placeholder="Search algorithms..."
                      class="w-full pl-10 pr-4 py-2 bg-gray-50/50 border-0 rounded-lg focus:ring-2 focus:ring-blue-500"
                      @click.stop
                    />
                  </div>
                </div>

                <!-- Algorithm List -->
                <div class="max-h-[200px] overflow-y-auto custom-scrollbar">
                  <div 
                    v-for="algo in filteredAlgorithms"
                    :key="algo"
                    @click="formData.algorithm = algo; isAlgorithmDropdownOpen = false"
                    class="px-4 py-2 hover:bg-gray-50 cursor-pointer"
                    :class="{ 'bg-blue-50 text-blue-600': formData.algorithm === algo }"
                  >
                    {{ algo }}
                  </div>
                </div>
              </div>
            </div>
            <p v-if="formErrors.algorithm" class="mt-1 text-sm text-red-500">
              {{ formErrors.algorithm }}
            </p>
          </div>

          <!-- Dataset Selection -->
          <div class="space-y-2" ref="datasetDropdown">
            <label class="block text-sm font-medium text-gray-700">
              Dataset
              <span class="text-red-500 ml-1">*</span>
            </label>
            <div class="relative">
              <div 
                @click.stop="isDatasetDropdownOpen = !isDatasetDropdownOpen"
                :class="[
                  'w-full px-4 py-3 bg-black/5 border-2 rounded-xl cursor-pointer transition-colors',
                  formErrors.dataset 
                    ? 'border-red-300' 
                    : 'border-transparent hover:border-blue-200'
                ]"
              >
                {{ formData.dataset || 'Select Dataset' }}
              </div>

              <!-- Dropdown Panel -->
              <div 
                v-if="isDatasetDropdownOpen"
                class="absolute z-50 w-full mt-2 bg-white/90 backdrop-blur-xl border border-gray-100 rounded-xl shadow-2xl overflow-hidden"
              >
                <!-- Search Box -->
                <div class="p-3 border-b border-gray-100">
                  <div class="relative">
                    <Search class="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                    <input
                      v-model="datasetSearchQuery"
                      type="text"
                      placeholder="Search datasets..."
                      class="w-full pl-10 pr-4 py-2 bg-gray-50/50 border-0 rounded-lg focus:ring-2 focus:ring-blue-500"
                      @click.stop
                    />
                  </div>
                </div>

                <!-- Dataset List -->
                <div class="max-h-[200px] overflow-y-auto custom-scrollbar">
                  <div 
                    v-for="dataset in filteredDatasets"
                    :key="dataset"
                    @click="formData.dataset = dataset; isDatasetDropdownOpen = false"
                    class="px-4 py-2 hover:bg-gray-50 cursor-pointer"
                    :class="{ 'bg-blue-50 text-blue-600': formData.dataset === dataset }"
                  >
                    {{ dataset }}
                  </div>
                </div>
              </div>
            </div>
            <p v-if="formErrors.dataset" class="mt-1 text-sm text-red-500">
              {{ formErrors.dataset }}
            </p>
          </div>

          <!-- Metrics -->
          <div class="space-y-4" ref="metricsDropdown">
            <div class="flex items-center justify-between">
              <label class="block text-sm font-medium text-gray-700">
                Performance Metrics
                <span class="text-red-500 ml-1">*</span>
              </label>
            </div>

            <!-- Metrics Dropdown -->
            <div class="relative">
              <!-- Selected Metrics Display -->
              <div 
                @click.stop="isMetricsDropdownOpen = !isMetricsDropdownOpen"
                class="w-full px-4 py-3 bg-white/80 backdrop-blur-sm border border-gray-200 rounded-xl cursor-pointer hover:border-blue-200 transition-all duration-300 min-h-[3rem] flex items-center"
                :class="{ 'border-blue-200': isMetricsDropdownOpen }"
              >
                <div v-if="selectedMetrics.length === 0" class="text-gray-400">
                  Select metrics to measure performance
                </div>
                <div v-else class="flex flex-wrap gap-2">
                  <span 
                    v-for="metric in selectedMetrics" 
                    :key="metric.id"
                    class="px-2 py-1 bg-blue-50 text-blue-600 rounded-lg text-sm flex items-center gap-1"
                  >
                    {{ metric.name }}
                    <button 
                      @click.stop="toggleMetric(metric)"
                      class="hover:text-blue-800"
                    >
                      ×
                    </button>
                  </span>
                </div>
              </div>

              <!-- Dropdown Panel -->
              <div 
                v-show="isMetricsDropdownOpen"
                class="absolute z-50 w-full mt-2 bg-white/90 backdrop-blur-xl border border-gray-100 rounded-xl shadow-2xl overflow-hidden transition-all duration-300 animate-slideDown"
              >
                <!-- Search Box -->
                <div class="p-3 border-b border-gray-100">
                  <div class="relative">
                    <Search class="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                    <input
                      v-model="searchQuery"
                      type="text"
                      placeholder="Search metrics..."
                      class="w-full pl-10 pr-4 py-2 bg-gray-50/50 border-0 rounded-lg focus:ring-2 focus:ring-blue-500"
                      @click.stop
                    />
                    <button
                      v-if="searchQuery"
                      @click.stop="searchQuery = ''"
                      class="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      <X class="w-4 h-4" />
                    </button>
                  </div>
                  <div v-if="searchQuery" class="mt-2 text-xs text-gray-500">
                    Found {{ filteredMetrics.length }} results
                  </div>
                </div>

                <!-- Metrics List -->
                <div class="max-h-[300px] overflow-y-auto custom-scrollbar">
                  <div 
                    v-for="metric in filteredMetrics"
                    :key="metric.id"
                    @click.stop="toggleMetric(metric)"
                    class="p-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-start gap-3 group"
                  >
                    <!-- Checkbox -->
                    <div class="relative flex items-center justify-center mt-1">
                      <input 
                        type="checkbox"
                        :checked="isMetricSelected(metric.id)"
                        class="peer sr-only"
                      />
                      <div 
                        class="w-5 h-5 border-2 rounded-md transition-all duration-300"
                        :class="isMetricSelected(metric.id) 
                          ? 'border-blue-500 bg-blue-500' 
                          : 'border-gray-300 group-hover:border-blue-300'"
                      >
                        <svg 
                          class="w-4 h-4 text-white transition-transform duration-300"
                          :class="isMetricSelected(metric.id) ? 'scale-100' : 'scale-0'"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                        >
                          <path 
                            stroke-linecap="round" 
                            stroke-linejoin="round" 
                            stroke-width="2" 
                            d="M5 13l4 4L19 7"
                          />
                        </svg>
                      </div>
                    </div>

                    <!-- Metric Info -->
                    <div class="flex-1">
                      <div class="flex items-center justify-between">
                        <h4 class="font-medium text-gray-900" v-html="highlightText(metric.name, searchQuery)"></h4>
                        <span class="text-xs text-gray-500">{{ metric.category }}</span>
                      </div>
                      <p class="text-sm text-gray-500 mt-1" v-html="highlightText(metric.description, searchQuery)"></p>
                      <p class="text-xs text-gray-400 mt-1">Range: {{ metric.range }}</p>
                    </div>
                  </div>
                  
                  <!-- No Results State -->
                  <div v-if="searchQuery && filteredMetrics.length === 0" 
                       class="p-8 text-center text-gray-500">
                    <p>No metrics found matching "{{ searchQuery }}"</p>
                    <button 
                      @click.stop="searchQuery = ''"
                      class="mt-2 text-sm text-blue-500 hover:text-blue-600"
                    >
                      Clear search
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Selected Metrics Values -->
            <div v-if="selectedMetrics.length > 0" class="space-y-4 mt-4">
              <div 
                v-for="metric in selectedMetrics" 
                :key="metric.id"
                class="bg-white/80 backdrop-blur-sm rounded-xl border border-gray-100 p-4 space-y-2"
              >
                <label class="text-sm font-medium text-gray-700">{{ metric.name }}</label>
                <input 
                  v-model="formData.metrics[metric.id].value"
                  type="number"
                  step="0.01"
                  min="0"
                  max="1"
                  :class="[
                    'w-full px-4 py-3 bg-black/5 border-2 rounded-xl focus:ring-2 transition-colors',
                    formErrors.metrics && !formData.metrics[metric.id].value
                      ? 'border-red-300 focus:ring-red-500' 
                      : 'border-transparent focus:ring-blue-500'
                  ]"
                  :placeholder="`Enter ${metric.name} value (${metric.range})`"
                />
              </div>
            </div>
          </div>

          <!-- Results File Section -->
          <div class="space-y-4">
            <label class="block text-sm font-medium text-gray-700">
              Results File
              <span class="text-red-500 ml-1">*</span>
            </label>

            <!-- File Format Tabs -->
            <div class="flex rounded-lg bg-gray-100 p-1">
              <button
                v-for="format in ['json', 'csv']"
                :key="format"
                @click="activeFileFormat = format"
                class="flex-1 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-300"
                :class="activeFileFormat === format
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'"
              >
                {{ format.toUpperCase() }}
              </button>
            </div>

            <!-- File Upload Area -->
            <div class="space-y-4">
              <!-- JSON Format Guide -->
              <div v-if="activeFileFormat === 'json'" class="bg-gray-50/50 rounded-lg p-4 space-y-2 text-sm">
                <p class="text-gray-600 font-medium">JSON Format Example:</p>
                <pre class="text-gray-500 text-xs bg-black/5 p-2 rounded-md">
{
  "mAP": 0.85,
  "NDCG": 0.92,
  "precision": 0.78,
  "recall": 0.89
}</pre>
              </div>

              <!-- CSV Format Guide -->
              <div v-if="activeFileFormat === 'csv'" class="bg-gray-50/50 rounded-lg p-4 space-y-2 text-sm">
                <p class="text-gray-600 font-medium">CSV Format Example:</p>
                <pre class="text-gray-500 text-xs bg-black/5 p-2 rounded-md">metric,value
mAP,0.85
NDCG,0.92
precision,0.78
recall,0.89</pre>
              </div>

              <!-- Upload Button -->
              <div>
                <input 
                  type="file"
                  @change="handleFileChange"
                  class="hidden"
                  :accept="activeFileFormat === 'json' ? '.json' : '.csv'"
                  id="results-upload"
                />
                <label 
                  for="results-upload"
                  :class="[
                    'flex items-center justify-center gap-2 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300',
                    formErrors.file
                      ? 'bg-red-50 border-2 border-red-200 hover:bg-red-100'
                      : 'bg-black/5 hover:bg-black/10 border-2 border-transparent'
                  ]"
                >
                  <UploadIcon 
                    class="w-5 h-5" 
                    :class="formErrors.file ? 'text-red-500' : 'text-gray-500'"
                  />
                  <span :class="formErrors.file ? 'text-red-600' : 'text-gray-600'">
                    {{ fileName || `Upload ${activeFileFormat.toUpperCase()} File` }}
                  </span>
                </label>
                
                <!-- Error Message -->
                <p v-if="formErrors.file || fileErrors" class="mt-2 text-sm text-red-500">
                  {{ formErrors.file || fileErrors }}
                </p>
                
                <!-- Help Text -->
                <p class="mt-2 text-xs text-gray-500">
                  Upload your performance results in {{ activeFileFormat.toUpperCase() }} format.
                  Make sure the file follows the format shown in the example above.
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Batch Upload Mode -->
        <div v-else class="p-8 space-y-8">
          <div class="text-center space-y-2">
            <div class="w-12 h-12 bg-blue-600 rounded-2xl flex items-center justify-center mx-auto">
              <UploadIcon class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-medium text-gray-900">Batch Upload</h2>
            <p class="text-sm text-gray-500">Upload a JSON or CSV file containing all results information</p>
          </div>

          <!-- File Format Examples -->
          <div class="space-y-4">
            <div class="bg-gray-50/50 rounded-lg p-4 space-y-2">
              <p class="text-sm font-medium text-gray-700">JSON Format Example:</p>
              <pre class="text-xs bg-black/5 p-3 rounded-md overflow-x-auto">
{
  "algorithm": "Comb*",
  "dataset": "Market1501",
  "metrics": {
    "mAP": 0.85,
    "NDCG": 0.92,
    "precision": 0.78,
    "recall": 0.89
  }
}</pre>
            </div>

            <div class="bg-gray-50/50 rounded-lg p-4 space-y-2">
              <p class="text-sm font-medium text-gray-700">CSV Format Example:</p>
              <pre class="text-xs bg-black/5 p-3 rounded-md overflow-x-auto">algorithm,dataset,metric,value
Comb*,Market1501,mAP,0.85
Comb*,Market1501,NDCG,0.92
Comb*,Market1501,precision,0.78
Comb*,Market1501,recall,0.89</pre>
            </div>
          </div>

          <!-- Upload Area -->
          <div class="space-y-4">
            <input 
              type="file"
              @change="handleBatchFileUpload"
              accept=".json,.csv"
              class="hidden"
              id="batch-upload"
            />
            <label 
              for="batch-upload"
              class="flex flex-col items-center justify-center gap-4 p-8 border-2 border-dashed border-gray-300 rounded-xl cursor-pointer hover:border-blue-300 transition-all duration-300"
            >
              <div class="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center">
                <UploadIcon class="w-6 h-6 text-blue-500" />
              </div>
              <div class="text-center">
                <p class="text-sm font-medium text-gray-700">Click to upload or drag and drop</p>
                <p class="text-xs text-gray-500">JSON or CSV files</p>
              </div>
            </label>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.dialog-enter-active,
.dialog-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* Custom scrollbar styles */
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
  margin: 4px 0;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
  transition: background-color 0.2s ease;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.3);
}

.custom-scrollbar:not(:hover)::-webkit-scrollbar-thumb {
  background-color: transparent;
}

/* JSON Example Styles */
pre {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  overflow-x: auto;
}

/* 添加必填项标记的样式 */
.required::after {
  content: '*';
  color: #EF4444;
  margin-left: 0.25rem;
}

/* 优化下拉动画 */
.animate-slideDown {
  animation: slideDown 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: top;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: scaleY(0.95);
  }
  to {
    opacity: 1;
    transform: scaleY(1);
  }
}

/* 添加高亮动画 */
mark {
  transition: background-color 0.2s ease;
}

mark:hover {
  background-color: #fef08a;
}

/* 添加搜索框动画 */
input {
  transition: all 0.3s ease;
}

input:focus {
  transform: scale(1.01);
}

/* 添加清除按钮动画 */
button {
  transition: all 0.2s ease;
}

button:hover {
  transform: scale(1.1);
}

/* 添加文件格式 tab 动画 */
.format-tab-enter-active,
.format-tab-leave-active {
  transition: all 0.3s ease;
}

.format-tab-enter-from,
.format-tab-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 文件上传区域动画 */
label[for="results-upload"] {
  transition: all 0.3s ease;
}

label[for="results-upload"]:hover {
  transform: translateY(-1px);
}

/* 文件格式示例样式 */
pre {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  white-space: pre-wrap;
}

/* 添加下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style> 