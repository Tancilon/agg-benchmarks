<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { FileText, X, Upload as UploadIcon, Search, CheckCircle2, AlertCircle, Loader2 } from 'lucide-vue-next'

const props = defineProps({
  show: Boolean
})
// 添加反馈状态
const feedbackState = ref({
  show: false,
  type: '', // 'success' 或 'error'
  title: '',
  message: ''
})
const emit = defineEmits(['close', 'submit'])

// 从后端获取的指标列表
const metrics = ref([])

// 指标类型映射
const metricTypeMap = {
  'at-k': '@k Support',
  'fixed': 'Fixed Metric',
  'Other': 'Other'
}

// 指标类型的描述映射
const metricTypeDescMap = {
  'at-k': 'These metrics support @k operations (e.g., mAP@k, NDCG@k)',
  'fixed': 'These metrics just have a fixed calculation method',
}

// 指标类型图标映射
const metricTypeIcons = {
  'at-k': 'Target',
  'fixed': 'Lock',
  'Other': 'Hash'
}

// 指标类型颜色映射
const metricTypeColors = {
  'at-k': 'bg-amber-400/90 text-amber-50',
  'fixed': 'bg-blue-400/90 text-blue-50',
  'Other': 'bg-slate-400/90 text-slate-50'
}

// 获取指标列表
const fetchMetrics = async () => {
  try {
    const response = await fetch('/api/metrics')
    if (!response.ok) {
      throw new Error('Failed to fetch metrics')
    }
    const data = await response.json()
    metrics.value = data.map(metric => ({
      id: metric.name,
      name: metric.name,
      range: metric.range,  // 确保包含范围信息
      type: metric.type,
      displayType: metricTypeMap[metric.type] || metric.type,
      icon: metricTypeIcons[metric.type] || 'Hash'
    }))
  } catch (error) {
    console.error('Error fetching metrics:', error)
    feedbackState.value = {
      show: true,
      type: 'error',
      title: 'Error',
      message: 'Failed to load metrics. Please try again.'
    }
  }
}

// 在组件挂载时获取指标列表
onMounted(() => {
  fetchMetrics()
})

// 初始化表单数据
const formData = ref({
  algorithm: '',
  dataset: '',
  metrics: {},
  metricsFile: null,
  file: null
})

// 在 setup 中添加
const algorithms = ref([])
const datasets = ref([])

// 获取算法列表
const fetchAlgorithms = async () => {
  try {
    const response = await fetch('/api/algorithms')
    const data = await response.json()
    algorithms.value = data.map(algo => algo.name)
  } catch (error) {
    console.error('Error fetching algorithms:', error)
  }
}

// 获取数据集列表
const fetchDatasets = async () => {
  try {
    const response = await fetch('/api/datasets?page=0&size=100')  // 获取足够多的数据
    const data = await response.json()
    // 从分页数据中提取数据集名称
    datasets.value = data.content.map(dataset => dataset.name)
  } catch (error) {
    console.error('Error fetching datasets:', error)
  }
}

// 在组件挂载时获取数据
onMounted(async () => {
  await Promise.all([
    fetchAlgorithms(),
    fetchDatasets()
  ])
})

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

// 添加指标下拉列表的引用
const metricsDropdown = ref(null)

// 添加上传模式选择
const uploadMode = ref('form') // 'form' 或 'file'

// 添加CSV指标类型选择
const csvMetricType = ref('fixed') // 'fixed' 或 'at-k'

// 过滤算法列表
const filteredAlgorithms = computed(() => {
  if (!algorithms.value) return []
  if (!algorithmSearchQuery.value.trim()) return algorithms.value
  const query = algorithmSearchQuery.value.toLowerCase()
  return algorithms.value.filter(algo => 
    algo.toLowerCase().includes(query)
  )
})

// 过滤数据集列表
const filteredDatasets = computed(() => {
  if (!datasets.value) return []
  if (!datasetSearchQuery.value.trim()) return datasets.value
  const query = datasetSearchQuery.value.toLowerCase()
  return datasets.value.filter(dataset => 
    dataset.toLowerCase().includes(query)
  )
})

// 添加验证状态对象
const validationState = ref({
  isValidating: false,
  steps: [
    {
      id: 'datasets',
      name: 'Validate Datasets',
      status: 'pending', // 'pending' | 'validating' | 'success' | 'error'
      message: '',
      invalidItems: []
    },
    {
      id: 'algorithms',
      name: 'Validate Algorithms', 
      status: 'pending',
      message: '',
      invalidItems: []
    },
    {
      id: 'metrics',
      name: 'Validate Metrics',
      status: 'pending',
      message: '',
      invalidItems: []
    },
    {
      id: 'format',
      name: 'Validate Data Format',
      status: 'pending',
      message: '',
      invalidItems: []
    }
  ]
})

// 添加验证函数
const validateFileData = async (fileData) => {
  validationState.value.isValidating = true

  try {
    // 1. 验证数据集
    const datasetsStep = validationState.value.steps.find(s => s.id === 'datasets')
    datasetsStep.status = 'validating'
    const uniqueDatasets = [...new Set(Array.isArray(fileData) ? fileData.map(d => d.dataset) : [fileData.dataset])]
    
    try {
    const datasetsResponse = await fetch('/api/datasets/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(uniqueDatasets)
    })
    const datasetsResult = await datasetsResponse.json()
    
    if (!datasetsResult.valid) {
      datasetsStep.status = 'error'
        datasetsStep.message = 'Invalid datasets found. Please upload these datasets first.'
        datasetsStep.invalidItems = datasetsResult.invalidItems
        return false // 直接返回，不再继续验证
      }
      datasetsStep.status = 'success'
    } catch (error) {
      datasetsStep.status = 'error'
      datasetsStep.message = 'Failed to validate datasets'
      return false
    }

    // 2. 验证算法
    const algorithmsStep = validationState.value.steps.find(s => s.id === 'algorithms')
    algorithmsStep.status = 'validating'
    const uniqueAlgorithms = [...new Set(Array.isArray(fileData) ? fileData.map(d => d.algorithm) : [fileData.algorithm])]
    
    try {
    const algorithmsResponse = await fetch('/api/algorithms/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(uniqueAlgorithms)
    })
    const algorithmsResult = await algorithmsResponse.json()
    
    if (!algorithmsResult.valid) {
      algorithmsStep.status = 'error'
        algorithmsStep.message = 'Invalid algorithms found. Please upload these algorithms first.'
        algorithmsStep.invalidItems = algorithmsResult.invalidItems
        return false
      }
      algorithmsStep.status = 'success'
    } catch (error) {
      algorithmsStep.status = 'error'
      algorithmsStep.message = 'Failed to validate algorithms'
      return false
    }

    // 3. 验证指标
    const metricsStep = validationState.value.steps.find(s => s.id === 'metrics')
    metricsStep.status = 'validating'
    const uniqueMetrics = [...new Set(Object.keys(fileData.metrics || {}))]
    
    try {
    const metricsResponse = await fetch('/api/metrics/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(uniqueMetrics)
    })
    const metricsResult = await metricsResponse.json()
    
    if (!metricsResult.valid) {
      metricsStep.status = 'error'
        metricsStep.message = 'Invalid metrics found. Please upload these metrics first.'
        metricsStep.invalidItems = metricsResult.invalidItems
        return false
      }
      metricsStep.status = 'success'
    } catch (error) {
      metricsStep.status = 'error'
      metricsStep.message = 'Failed to validate metrics'
      return false
    }

    // 4. 验证数据格式
    const formatStep = validationState.value.steps.find(s => s.id === 'format')
    formatStep.status = 'validating'
    
    try {
    const formatErrors = []
      
      // 首先获取所有涉及的指标的范围信息
      const uniqueMetrics = new Set()
      if (Array.isArray(fileData)) {
        fileData.forEach(result => {
          Object.keys(result.metrics).forEach(metricName => uniqueMetrics.add(metricName))
        })
        } else {
        Object.keys(fileData.metrics).forEach(metricName => uniqueMetrics.add(metricName))
      }
      
      // 获取指标范围信息
      const metricsResponse = await fetch('/api/metrics/info', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(Array.from(uniqueMetrics))
      })
      const metricsInfo = await metricsResponse.json()

      // 遍历所有结果进行验证
      const validateResult = (result, index = 0) => {
        Object.entries(result.metrics).forEach(([metricName, metricValue]) => {
          const metricInfo = metricsInfo[metricName]
          if (!metricInfo) {
            formatErrors.push(`Metric "${metricName}" not found in database`)
            return
          }

          if (typeof metricValue === 'object') {
            // @k 类型指标验证
            if (!Object.keys(metricValue).length) {
              formatErrors.push(`${metricName}: No k values provided`)
              return
            }

            Object.entries(metricValue).forEach(([k, value]) => {
              const kNum = parseInt(k)
              if (!Number.isInteger(kNum) || kNum <= 0) {
                formatErrors.push(`${metricName}: Invalid k value "${k}" (must be a positive integer)`)
              }
              if (value < metricInfo.minValue || value > metricInfo.maxValue) {
                formatErrors.push(
                  `${metricName}@${k}: Value ${value} is out of range ` +
                  `[${metricInfo.minValue}, ${metricInfo.maxValue}]`
                )
              }
            })
      } else {
            // 固定类型指标验证
            if (typeof metricValue !== 'number') {
              formatErrors.push(`${metricName}: Invalid value type (expected number, got ${typeof metricValue})`)
            } else if (metricValue < metricInfo.minValue || metricValue > metricInfo.maxValue) {
              formatErrors.push(
                `${metricName}: Value ${metricValue} is out of range ` +
                `[${metricInfo.minValue}, ${metricInfo.maxValue}]`
              )
            }
          }
        })
      }

      if (Array.isArray(fileData)) {
        fileData.forEach((result, index) => validateResult(result, index))
      } else {
        validateResult(fileData)
    }

    if (formatErrors.length > 0) {
      formatStep.status = 'error'
        formatStep.message = 'Invalid data format. Please check the following issues:'
      formatStep.invalidItems = formatErrors
        return false
      }

      formatStep.status = 'success'
      return true
    } catch (error) {
      formatStep.status = 'error'
      formatStep.message = 'Failed to validate data format: ' + error.message
      return false
    }

  } catch (error) {
    console.error('Validation error:', error)
    return false
  } finally {
    validationState.value.isValidating = false
  }
}

// 修改文件处理函数
const handleFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) {
    // 重置所有状态
    fileName.value = ''
    formData.value.file = null
    fileErrors.value = ''
    validationState.value.steps.forEach(step => {
      step.status = 'pending'
      step.message = ''
      step.invalidItems = []
    })
    return
  }
  
  const fileType = activeFileFormat.value === 'json' ? 'application/json' : 'text/csv'
  const fileExt = activeFileFormat.value === 'json' ? '.json' : '.csv'
  
  if (!file.name.toLowerCase().endsWith(fileExt)) {
    fileErrors.value = `Please upload a ${activeFileFormat.value.toUpperCase()} file`
    fileName.value = ''
    formData.value.file = null
    e.target.value = ''
    // 重置验证状态
    validationState.value.steps.forEach(step => {
      step.status = 'pending'
      step.message = ''
      step.invalidItems = []
    })
    return
  }
  
  // 读取并验证文件内容
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      let fileData
      if (activeFileFormat.value === 'json') {
        fileData = JSON.parse(e.target.result)
      } else {
        fileData = parseCSVData(e.target.result)
      }
      
      // 设置文件名,这样用户可以看到正在处理哪个文件
      fileName.value = file.name
      
      // 验证文件数据
      const isValid = await validateFileData(fileData)
      if (isValid) {
        formData.value.file = file
        fileErrors.value = ''
      } else {
        // 验证失败时清除文件
        fileName.value = ''
        formData.value.file = null
        e.target.value = ''
      }
    } catch (error) {
      // 修改这里，使用实际的错误信息
      fileErrors.value = error.message || 'Invalid file format'
      fileName.value = ''
      formData.value.file = null
      e.target.value = ''
      // 重置验证状态
      validationState.value.steps.forEach(step => {
        step.status = 'pending'
        step.message = ''
        step.invalidItems = []
      })
    }
  }
  reader.readAsText(file)
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
        metrics.value.forEach(metric => {
          const metricData = data[metric.id]
          if (metricData !== undefined) {
            // 处理@k类型指标
            if (metric.type === 'at-k') {
              if (typeof metricData !== 'object') {
                throw new Error(`Metric ${metric.id} requires k values in format: {"k1": value1, "k2": value2}`)
              }
              // 获取第一个k值和对应的性能值
              const [k, value] = Object.entries(metricData)[0]
              formData.value.metrics[metric.id].k = k
              formData.value.metrics[metric.id].value = value
            } else {
              // 处理固定类型指标
              if (typeof metricData !== 'number') {
                throw new Error(`Metric ${metric.id} requires a single numeric value`)
              }
              formData.value.metrics[metric.id].value = metricData
            }
            formData.value.metrics[metric.id].enabled = true
            if (!selectedMetrics.value.find(m => m.id === metric.id)) {
              selectedMetrics.value.push(metrics.value.find(m => m.id === metric.id))
            }
          }
        })
      } catch (error) {
        alert(`Invalid JSON file: ${error.message}`)
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
  if (!searchQuery.value.trim()) return metrics.value
  const query = searchQuery.value.toLowerCase()
  return metrics.value.filter(metric => 
    metric.name.toLowerCase().includes(query) ||
    metric.type.toLowerCase().includes(query)
  )
})

// 按类型分组的已过滤指标
const groupedFilteredMetrics = computed(() => {
  const query = searchQuery.value.toLowerCase()
  const groups = {}
  
  metrics.value.forEach(metric => {
    if (query && !metric.name.toLowerCase().includes(query)) {
      return
    }
    
    if (!groups[metric.type]) {
      groups[metric.type] = []
    }
    groups[metric.type].push(metric)
  })
  
  // 只返回非空组
  return Object.fromEntries(
    Object.entries(groups).filter(([_, metrics]) => metrics.length > 0)
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

  const hasValidMetric = Object.values(formData.value.metrics).some(metric => {
    if (!metric.enabled) return false
    if (metric.value === '') return false
    if (metric.k !== undefined && metric.k === '') return false
    return true
  })
  
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

// 修改计算属性检查表单是否完整
const isFormComplete = computed(() => {
  console.log('Checking form completion, mode:', uploadMode.value)
  
  if (uploadMode.value === 'form') {
    // Form Upload 模式验证
    if (!formData.value.algorithm || !formData.value.dataset) {
      console.log('Missing algorithm or dataset')
      return false
    }

    if (selectedMetrics.value.length === 0) {
      console.log('No metrics selected')
      return false
    }

    return selectedMetrics.value.every(selectedMetric => {
      const metric = formData.value.metrics[selectedMetric.id]
      if (!metric || !metric.enabled) {
        console.log(`Metric ${selectedMetric.id} not enabled`)
        return false
      }
      
      if (selectedMetric.type === 'at-k') {
        const hasK = metric.k !== undefined && metric.k !== null && metric.k !== ''
        const hasValue = metric.value !== undefined && metric.value !== null && metric.value !== ''
        const isComplete = hasK && hasValue
        console.log(`@k metric ${selectedMetric.id} complete:`, isComplete)
        return isComplete
      }
      
      const hasValue = metric.value !== undefined && metric.value !== null && metric.value !== ''
      console.log(`Fixed metric ${selectedMetric.id} complete:`, hasValue)
      return hasValue
    })
  } else {
    // Batch Upload 模式验证
    const hasFile = formData.value.file !== null
    const isValidated = !validationState.value.isValidating
    const hasNoErrors = !validationState.value.steps.some(step => step.status === 'error')
    
    console.log('Batch mode validation:', {
      hasFile,
      isValidated,
      hasNoErrors,
      validationState: validationState.value
    })
    
    return hasFile && isValidated && hasNoErrors
  }
})

// 修改上传按钮状态计算属性
const isUploadButtonDisabled = computed(() => {
  const disabled = !isFormComplete.value
  console.log('Upload button disabled:', disabled, {
    mode: uploadMode.value,
    formComplete: isFormComplete.value
  })
  return disabled
})

// 添加一个新的函数来验证性能值范围
const validateMetricRanges = async () => {
  try {
    const metricsResponse = await fetch('/api/metrics/info', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(selectedMetrics.value.map(m => m.id))
    })
    const metricsInfo = await metricsResponse.json()
    
    return selectedMetrics.value.every(selectedMetric => {
      const metric = formData.value.metrics[selectedMetric.id]
      const metricInfo = metricsInfo[selectedMetric.id]
      
      if (!metric || !metricInfo) return false
      
      const value = parseFloat(metric.value)
      return value >= metricInfo.minValue && value <= metricInfo.maxValue
    })
  } catch (error) {
    console.error('Error validating metric ranges:', error)
    return false
  }
}

// 修改提交处理函数
const handleSubmit = async () => {
  console.log('handleSubmit called')
  
  if (!isFormComplete.value) {
    console.log('Form is not complete')
    return
  }

  try {
    if (uploadMode.value === 'form') {
      console.log('Form upload mode')
      // Form Upload 模式处理
      const submitData = {
        algorithm: formData.value.algorithm,
        dataset: formData.value.dataset,
        metrics: {}
      }

      // 处理选中的指标数据
      selectedMetrics.value.forEach(metric => {
        const metricData = formData.value.metrics[metric.id]
        if (metricData && metricData.enabled) {
          if (metric.type === 'at-k') {
            submitData.metrics[metric.id] = {
              [metricData.k]: parseFloat(metricData.value)
            }
          } else {
            submitData.metrics[metric.id] = parseFloat(metricData.value)
          }
        }
      })

      // 发送表单数据
      const response = await fetch('/api/results', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(submitData)
      })

      if (!response.ok) throw new Error('Failed to submit results')

    } else {
      console.log('Batch upload mode')
      if (!formData.value.file) {
        throw new Error('No file selected')
      }

      let dataToSubmit
      if (activeFileFormat.value === 'json') {
        // JSON 文件处理
        const fileReader = new FileReader()
        const jsonData = await new Promise((resolve, reject) => {
          fileReader.onload = (e) => {
            try {
              resolve(JSON.parse(e.target.result))
            } catch (error) {
              reject(new Error('Invalid JSON format'))
            }
          }
          fileReader.onerror = () => reject(new Error('Failed to read file'))
          fileReader.readAsText(formData.value.file)
        })
        dataToSubmit = Array.isArray(jsonData) ? jsonData : [jsonData]
      } else {
        // CSV 文件处理
        const fileReader = new FileReader()
        const csvData = await new Promise((resolve, reject) => {
          fileReader.onload = (e) => {
            try {
              const results = parseCSVData(e.target.result)
              resolve(results)
            } catch (error) {
              reject(error)
            }
          }
          fileReader.onerror = () => reject(new Error('Failed to read file'))
          fileReader.readAsText(formData.value.file)
        })
        dataToSubmit = csvData
      }

      // 发送数据到后端
      const response = await fetch('/api/results', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(dataToSubmit)
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Failed to submit results')
      }

      // 提交成功
      feedbackState.value = {
        show: true,
        type: 'success',
        title: 'Success',
        message: 'Results uploaded successfully'
      }

      // 关闭对话框
  emit('close')
    }
  } catch (error) {
    console.error('Error submitting results:', error)
    feedbackState.value = {
      show: true,
      type: 'error',
      title: 'Error',
      message: error.message || 'Failed to upload results. Please try again.'
    }
  }
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
  // 检查点击是否在下拉框内
  const isClickInAlgorithmDropdown = algorithmDropdown.value?.contains(e.target)
  const isClickInDatasetDropdown = datasetDropdown.value?.contains(e.target)
  const isClickInMetricsDropdown = metricsDropdown.value?.contains(e.target)
  
  // 如果点击不在相应的下拉框内，则关闭对应的下拉框
  if (!isClickInAlgorithmDropdown) {
    isAlgorithmDropdownOpen.value = false
  }
  if (!isClickInDatasetDropdown) {
    isDatasetDropdownOpen.value = false
  }
  
  // 如果点击不在指标下拉框内，关闭指标选择下拉框
  if (!isClickInMetricsDropdown) {
    isMetricsDropdownOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 处理批量上传文件
const handleBatchFileUpload = (e) => {
  const file = e.target.files[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      let data
      if (file.name.toLowerCase().endsWith('.json')) {
        data = JSON.parse(e.target.result)
      } else {
        // 处理 CSV 文件
        const csvRows = e.target.result.split('\n')
        const headers = csvRows[0].split(',').map(h => h.trim())
        
        // 验证CSV格式
        if (csvMetricType.value === 'at-k') {
          if (!headers.includes('K Value')) {
            throw new Error('CSV file must include "K Value" column for @k metrics')
          }
        }
        
        data = {
          algorithm: '',
          dataset: '',
          metrics: {}
        }

        // 处理数据行
        for (let i = 1; i < csvRows.length; i++) {
          const values = csvRows[i].split(',').map(v => v.trim())
          if (values.length < headers.length) continue
          
          const row = {}
          headers.forEach((header, index) => {
            row[header] = values[index]
          })
          
          // 设置算法和数据集
          if (!data.algorithm) data.algorithm = row['Algorithm']
          if (!data.dataset) data.dataset = row['Dataset']
          
          // 处理指标值
          const metricName = row['Metric']
          if (csvMetricType.value === 'at-k') {
            if (!data.metrics[metricName]) {
              data.metrics[metricName] = {}
            }
            data.metrics[metricName][row['K Value']] = parseFloat(row['Performance Value'])
          } else {
            data.metrics[metricName] = parseFloat(row['Performance Value'])
          }
        }
      }

      // 自动提交
      formData.value = { ...data, metricsFile: null, file: null }
      handleSubmit()
    } catch (error) {
      alert(`Error processing file: ${error.message}`)
    }
  }
  reader.readAsText(file)
}

// 初始化 metrics 数据结构
watch(metrics, (newMetrics) => {
  newMetrics.forEach(metric => {
    formData.value.metrics[metric.id] = {
      enabled: false,
      value: '',
      k: metric.type === 'at-k' ? '' : undefined
    }
  })
})

const getProgressWidth = () => {
  if (!fileName.value) return 0
  
  const steps = validationState.value.steps
  const totalSteps = steps.length - 1
  let progress = 0
  
  for (let i = 0; i < steps.length; i++) {
    if (steps[i].status === 'success') {
      // 完成的步骤
      progress = ((i + 1) / totalSteps) * 100
    } else if (steps[i].status === 'validating') {
      // 正在验证的步骤，从上一步到当前步骤之间平滑过渡
      const prevProgress = (i / totalSteps) * 100
      const targetProgress = ((i + 1) / totalSteps) * 100
      const progressDiff = targetProgress - prevProgress
      progress = prevProgress + (progressDiff * 0.6) // 60% 的过渡进度
    } else if (steps[i].status === 'error') {
      // 错误步骤，停在当前位置
      progress = ((i + 1) / totalSteps) * 100
      break
    }
  }
  
  return progress
}

const getProgressColor = () => {
  const errorStep = validationState.value.steps.find(s => s.status === 'error')
  if (errorStep) return 'rgb(239 68 68)' // red-500
  return 'rgb(59 130 246)' // blue-500
}

const getCurrentError = () => {
  return validationState.value.steps.find(s => s.status === 'error')
}

// 添加对文件格式切换的监听
watch(activeFileFormat, () => {
  // 重置文件相关状态
  fileName.value = ''
  formData.value.file = null
  fileErrors.value = ''
  
  // 重置验证状态
  validationState.value.steps.forEach(step => {
    step.status = 'pending'
    step.message = ''
    step.invalidItems = []
  })
})

// 添加对指标类型切换的监听
watch(csvMetricType, () => {
  // 重置文件相关状态
  fileName.value = ''
  formData.value.file = null
  fileErrors.value = ''
  
  // 重置验证状态
  validationState.value.steps.forEach(step => {
    step.status = 'pending'
    step.message = ''
    step.invalidItems = []
  })
  
  // 清空文件输入
  const fileInput = document.getElementById('results-upload')
  if (fileInput) {
    fileInput.value = ''
  }
})

const parseCSVData = (csvContent) => {
  try {
    const csvRows = csvContent.split('\n').map(row => row.trim()).filter(row => row)
    if (csvRows.length < 2) {
      throw new Error('CSV file must contain headers and at least one data row')
    }

    // 验证表头
    const headers = csvRows[0].split(',').map(h => h.trim())
    
    // 根据选择的指标类型确定必需的表头
    let requiredHeaders
    if (csvMetricType.value === 'at-k') {
      requiredHeaders = ['Algorithm', 'Dataset', 'Metric', 'K Value', 'Performance Value']
      // 验证是否包含 K Value 列
      if (!headers.includes('K Value')) {
        throw new Error('For @k metrics, the CSV must include a "K Value" column')
      }
    } else {
      requiredHeaders = ['Algorithm', 'Dataset', 'Metric', 'Performance Value']
      // 如果是 Fixed Metric 但包含了 K Value 列，给出警告
      if (headers.includes('K Value')) {
        throw new Error('For Fixed metrics, the CSV should not include a "K Value" column')
      }
    }
    
    const missingHeaders = requiredHeaders.filter(h => !headers.includes(h))
    if (missingHeaders.length > 0) {
      throw new Error(`Missing required headers for ${csvMetricType.value === 'at-k' ? '@k' : 'Fixed'} metric type: ${missingHeaders.join(', ')}`)
    }

    // 解析数据，只做基本的格式检查
    const results = []
    const seenCombinations = new Set()

    // 处理数据行
    for (let i = 1; i < csvRows.length; i++) {
      const values = csvRows[i].split(',').map(v => v.trim())
      if (values.length < headers.length) {
        throw new Error(`Row ${i + 1} has insufficient columns`)
      }
      
      const row = {}
      headers.forEach((header, index) => {
        row[header] = values[index]
      })

      // 检查算法-数据集组合是否重复
      const combination = `${row['Algorithm']}-${row['Dataset']}`
      if (seenCombinations.has(combination)) {
        // 找到已存在的结果
        const existingResult = results.find(r => 
          r.algorithm === row['Algorithm'] && r.dataset === row['Dataset']
        )

        // 只做基本的数值格式检查
        const performanceValue = parseFloat(row['Performance Value'])
        if (isNaN(performanceValue)) {
          throw new Error(`Invalid performance value in row ${i + 1}: "${row['Performance Value']}" is not a number`)
        }

        if (csvMetricType.value === 'at-k') {
          const kValue = parseInt(row['K Value'])
          if (isNaN(kValue)) {
            throw new Error(`Invalid K value in row ${i + 1}: "${row['K Value']}" is not a number`)
          }

          if (!existingResult.metrics[row['Metric']]) {
            existingResult.metrics[row['Metric']] = {}
          }
          existingResult.metrics[row['Metric']][kValue] = performanceValue
        } else {
          existingResult.metrics[row['Metric']] = performanceValue
        }
      } else {
        // 新的算法-数据集组合
        seenCombinations.add(combination)
        const newResult = {
          algorithm: row['Algorithm'],
          dataset: row['Dataset'],
          metrics: {}
        }

        const metricName = row['Metric']
        const performanceValue = parseFloat(row['Performance Value'])

        if (isNaN(performanceValue)) {
          throw new Error(`Invalid performance value in row ${i + 1}: "${row['Performance Value']}" is not a number`)
        }

        if (csvMetricType.value === 'at-k') {
          const kValue = parseInt(row['K Value'])
          if (isNaN(kValue) || kValue <= 0) {
            throw new Error(`Invalid K value in row ${i + 1}: must be a positive integer`)
          }

          newResult.metrics[metricName] = {
            [kValue]: performanceValue
          }
        } else {
          newResult.metrics[metricName] = performanceValue
        }

        results.push(newResult)
      }
    }

    return results
  } catch (error) {
    fileErrors.value = error.message
    validationState.value.steps.forEach(step => {
      step.status = 'pending'
      step.message = ''
      step.invalidItems = []
    })
    throw error
  }
}

// 添加对话框显示状态的监听
watch(() => props.show, async (newValue) => {
  if (newValue) {
    // 当对话框打开时，重新获取所有数据
    try {
      await Promise.all([
        fetchAlgorithms(),
        fetchDatasets(),
        fetchMetrics()
      ])
    } catch (error) {
      console.error('Error fetching data:', error)
      feedbackState.value = {
        show: true,
        type: 'error',
        title: 'Error',
        message: 'Failed to load data. Please try again.'
      }
    }
  }
})

// 监控表单数据的变化
watch(() => formData.value, (newFormData) => {
  if (uploadMode.value === 'form') {
    // 检查必填字段
    if (!newFormData.algorithm || !newFormData.dataset) {
      formErrors.value = {
        algorithm: !newFormData.algorithm ? 'Algorithm is required' : '',
        dataset: !newFormData.dataset ? 'Dataset is required' : '',
      }
    } else {
      formErrors.value.algorithm = ''
      formErrors.value.dataset = ''
    }
  }
}, { deep: true })

// 监控指标数据的变化
watch(() => formData.value.metrics, (newMetrics) => {
  if (uploadMode.value === 'form') {
    const hasValidMetric = Object.values(newMetrics).some(metric => {
      if (!metric.enabled) return false
      
      if (metric.type === 'at-k') {
        return metric.k && metric.value !== ''
      }
      return metric.value !== ''
    })
    
    formErrors.value.metrics = !hasValidMetric ? 'At least one metric is required' : ''
  }
}, { deep: true })

// 添加对 feedbackState 的监听
watch(() => feedbackState.value.show, (newValue) => {
  if (newValue) {
    // 当显示反馈时，设置定时器自动关闭
    const timeout = feedbackState.value.type === 'success' ? 3000 : 5000
    setTimeout(() => {
      feedbackState.value.show = false
    }, timeout)
  }
})
</script>

<template>
  <div>
    <Transition 
      name="dialog"
    >
      <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center">
        <!-- Backdrop with blur -->
        <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="emit('close')"></div>
        
        <!-- Dialog -->
        <div 
          class="relative w-[1000px] max-h-[85vh] overflow-y-auto bg-gray-50/90 backdrop-blur-xl rounded-2xl shadow-lg custom-scrollbar"
          style="transform: none;"
        >
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
                  class="w-full px-4 py-3 bg-gray-50/80 backdrop-blur-sm border border-gray-200 rounded-xl cursor-pointer hover:border-blue-200 transition-colors duration-200"
                  :class="{ 'border-blue-200': isAlgorithmDropdownOpen }"
                >
                  <span :class="formData.algorithm ? 'text-gray-900' : 'text-gray-400'">
                    {{ formData.algorithm || 'Select algorithm' }}
                  </span>
                </div>

                <!-- Algorithm Dropdown -->
                <div 
                  v-if="isAlgorithmDropdownOpen"
                  ref="algorithmDropdown"
                  class="absolute z-50 w-full mt-2 bg-gray-50 rounded-xl shadow-lg border border-gray-100 overflow-hidden"
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
                  <div class="max-h-60 overflow-y-auto p-2">
                    <div 
                      v-for="algorithm in filteredAlgorithms"
                      :key="algorithm"
                      class="px-4 py-2 rounded-lg hover:bg-gray-50/80 cursor-pointer transition-colors
                             flex items-center justify-between"
                      @click="formData.algorithm = algorithm; isAlgorithmDropdownOpen = false"
                    >
                      <span class="text-sm" :class="{'text-blue-600 font-medium': formData.algorithm === algorithm}">
                        {{ algorithm }}
                      </span>
                      <CheckCircle2 
                        v-if="formData.algorithm === algorithm"
                        class="w-4 h-4 text-blue-500" 
                      />
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
                  class="w-full px-4 py-3 bg-gray-50/80 backdrop-blur-sm border border-gray-200 rounded-xl cursor-pointer hover:border-blue-200 transition-colors duration-200"
                  :class="{ 'border-blue-200': isDatasetDropdownOpen }"
                >
                  <span :class="formData.dataset ? 'text-gray-900' : 'text-gray-400'">
                    {{ formData.dataset || 'Select dataset' }}
                  </span>
                </div>

                <!-- Dataset Dropdown -->
                <div 
                  v-if="isDatasetDropdownOpen"
                  ref="datasetDropdown"
                  class="absolute z-50 w-full mt-2 bg-gray-50 rounded-xl shadow-lg border border-gray-100 overflow-hidden"
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
                  <div class="max-h-60 overflow-y-auto p-2">
                    <div 
                      v-for="dataset in filteredDatasets"
                      :key="dataset"
                      class="px-4 py-2 rounded-lg hover:bg-gray-50/80 cursor-pointer transition-colors
                             flex items-center justify-between"
                      @click="formData.dataset = dataset; isDatasetDropdownOpen = false"
                    >
                      <span class="text-sm" :class="{'text-blue-600 font-medium': formData.dataset === dataset}">
                        {{ dataset }}
                      </span>
                      <CheckCircle2 
                        v-if="formData.dataset === dataset"
                        class="w-4 h-4 text-blue-500" 
                      />
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
                  class="w-full px-4 py-3 bg-gray-50/80 backdrop-blur-sm border border-gray-200 rounded-xl cursor-pointer hover:border-blue-200 transition-colors duration-200 min-h-[3rem] flex items-center"
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
                  ref="metricsDropdown"
                  class="absolute left-0 right-0 top-full mt-2 bg-gray-50 rounded-xl shadow-lg border border-gray-100 overflow-hidden z-10"
                >
                  <!-- Search Box -->
                  <div class="p-3 border-b border-gray-100">
                    <div class="relative">
                      <Search class="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                      <input
                        v-model="searchQuery"
                        type="text"
                        placeholder="Search metrics..."
                        class="w-full pl-10 pr-4 py-2 bg-gray-50/80 border-0 rounded-lg focus:ring-2 focus:ring-blue-500"
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
                  <div class="space-y-4 mt-2 max-h-[300px] overflow-y-auto custom-scrollbar">
                    <!-- 按类型分组显示指标 -->
                    <div v-for="(metrics, type) in groupedFilteredMetrics" :key="type" 
                         class="mb-8 bg-gray-50/30 backdrop-blur-sm rounded-2xl p-4">
                      <!-- 类型标题 -->
                      <div class="flex items-center gap-3 mb-4 px-2">
                        <div 
                          class="w-8 h-8 rounded-full flex items-center justify-center"
                          :class="metricTypeColors[type] || 'bg-blue-500/10 text-blue-500'"
                        >
                          <component 
                            :is="metricTypeIcons[type]" 
                            class="w-4 h-4"
                          />
                        </div>
                        <div>
                          <h3 class="text-sm font-medium text-gray-900">
                            {{ metricTypeMap[type] || type }}
                          </h3>
                          <p class="text-xs text-gray-500" v-if="metricTypeDescMap[type]">
                            {{ metricTypeDescMap[type] }}
                          </p>
                          <p class="text-xs text-gray-400">
                            {{ metrics.length }} metric{{ metrics.length > 1 ? 's' : '' }} available
                          </p>
                        </div>
                      </div>
                      
                      <!-- 该类型下的指标列表 -->
                      <div class="space-y-2">
                        <div 
                          v-for="metric in metrics"
                          :key="metric.id"
                          class="flex items-center gap-3 p-3 rounded-xl bg-gray-50/50 
                                cursor-pointer group transition-colors duration-200"
                          @click="toggleMetric(metric)"
                        >
                          <!-- Checkbox -->
                          <div class="flex-shrink-0">
                            <div 
                              class="w-5 h-5 border-2 rounded-lg transition-colors duration-200"
                              :class="isMetricSelected(metric.id) 
                                ? 'border-blue-500 bg-blue-500 shadow-inner' 
                                : 'border-gray-200 group-hover:border-blue-400 bg-gray-50'"
                            >
                              <CheckCircle2 
                                v-if="isMetricSelected(metric.id)"
                                class="w-4 h-4 text-white" 
                              />
                            </div>
                          </div>

                          <!-- Metric Info -->
                          <div class="flex-1 flex items-center justify-between">
                            <span class="text-sm text-gray-900 font-medium" 
                                  v-html="highlightText(metric.name, searchQuery)">
                            </span>
                            <span class="text-xs font-medium px-2 py-1 rounded-full"
                                  :class="isMetricSelected(metric.id) 
                                    ? 'bg-blue-50 text-blue-600' 
                                    : 'bg-gray-50 text-gray-500'">
                              Range: {{ metric.range }}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Selected Metrics Values -->
              <div v-if="selectedMetrics.length > 0" class="space-y-4 mt-4">
                <div 
                  v-for="metric in selectedMetrics" 
                  :key="metric.id"
                  class="bg-gray-50/80 backdrop-blur-sm rounded-xl border border-gray-100 p-4 space-y-2"
                >
                  <div class="flex items-center justify-between">
                    <label class="text-sm font-medium text-gray-700">
                      {{ metric.name }}
                    </label>
                    <!-- 显示指标类型标签 -->
                    <span class="text-xs px-2 py-1 rounded-full" :class="metricTypeColors[metric.type]">
                      {{ metric.displayType }}
                    </span>
                  </div>

                  <!-- @k 类型指标的输入 -->
                  <div v-if="metric.type === 'at-k'" class="space-y-2">
                    <div class="flex gap-2">
                      <div class="flex-1">
                        <input 
                          v-model="formData.metrics[metric.id].k"
                          type="number"
                          placeholder="K Value"
                          class="w-full px-3 py-2 bg-white border border-gray-200 rounded-lg"
                        />
                      </div>
                    <div class="flex-1">
                      <input 
                        v-model="formData.metrics[metric.id].value"
                        type="number"
                          step="0.0001"
                          placeholder="Performance Value"
                          class="w-full px-3 py-2 bg-white border border-gray-200 rounded-lg"
                      />
                    </div>
                  </div>
                    <!-- 添加值域范围提示 -->
                    <p class="text-xs text-gray-500">
                      Valid range: {{ metric.range || '[0, 1]' }}
                    </p>
                  </div>

                  <!-- 固定类型指标的输入 -->
                  <div v-else>
                    <input
                      v-model="formData.metrics[metric.id].value"
                      type="number"
                      step="0.0001"
                      placeholder="Performance Value"
                      class="w-full px-3 py-2 bg-white border border-gray-200 rounded-lg"
                    />
                    <!-- 添加值域范围提示 -->
                    <p class="text-xs text-gray-500 mt-1">
                      Valid range: {{ metric.range || '[0, 1]' }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Batch Upload Mode -->
          <div v-else class="p-8 space-y-8">
            <!-- Results File Section -->
            <div class="space-y-4">
              <label class="block text-sm font-medium text-gray-700">
                Upload Results File
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
              
              <!-- File Format Guide -->
              <div v-if="activeFileFormat === 'json'" class="bg-gray-50/50 rounded-lg p-4 space-y-2 text-sm">
                <p class="text-gray-600 font-medium">JSON Format Example:</p>
                <pre class="bg-white p-3 rounded-lg text-gray-600 overflow-x-auto">
{
  "algorithm": "CSRA",
  "dataset": "Market1501",
  "metrics": {
    "mAP": {
      "10": 0.234,
      "20": 0.345
    },
    "precision_fixed": 0.876
  }
}</pre>
                <p class="text-gray-500">
                  Note: For @k metrics, provide k values as object keys. For fixed metrics, provide a single value.
                </p>
              </div>

              <!-- CSV Format Guide -->
              <div v-if="activeFileFormat === 'csv'" class="bg-gray-50/50 rounded-lg p-4 space-y-2 text-sm">
                <!-- Metric Type Selection -->
                <div class="mb-4">
                  <label class="text-sm font-medium text-gray-700 mb-2 block">
                    Select Metric Type
                  </label>
                  <div class="flex gap-4">
                    <label class="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        v-model="csvMetricType"
                        value="fixed"
                        class="text-blue-500 focus:ring-blue-500"
                      />
                      <span class="text-sm text-gray-600">Fixed Metric</span>
                    </label>
                    <label class="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        v-model="csvMetricType"
                        value="at-k"
                        class="text-blue-500 focus:ring-blue-500"
                      />
                      <span class="text-sm text-gray-600">@k Support</span>
                    </label>
                  </div>
                </div>

                <!-- Required Headers Notice -->
                <div class="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-4">
                  <p class="text-amber-700 font-medium mb-2">Required CSV Headers:</p>
                  <code class="block text-amber-600 bg-white/50 p-2 rounded">
                    {{ csvMetricType === 'at-k' 
                      ? 'Algorithm,Dataset,Metric,K Value,Performance Value'
                      : 'Algorithm,Dataset,Metric,Performance Value' }}
                  </code>
                  <p class="text-amber-600 text-xs mt-2">
                    Please ensure your CSV file includes these exact column headers in the first row.
                  </p>
                </div>

                <p class="text-gray-600 font-medium">CSV Format Example:</p>
                <pre class="bg-white p-3 rounded-lg text-gray-600 overflow-x-auto" v-if="csvMetricType === 'at-k'">
Algorithm,Dataset,Metric,K Value,Performance Value
CSRA,Market1501,mAP,10,0.234
CSRA,Market1501,mAP,20,0.345
CSRA,Market1501,NDCG,10,0.567</pre>
                <pre class="bg-white p-3 rounded-lg text-gray-600 overflow-x-auto" v-else>
Algorithm,Dataset,Metric,Performance Value
CSRA,Market1501,precision_fixed,0.876
CSRA,Market1501,recall_fixed,0.789</pre>
                <p class="text-gray-500 mt-2">
                  Note: 
                  <ul class="list-disc ml-4 mt-1 space-y-1">
                    <li>Headers are case-sensitive and must match exactly as shown above</li>
                    <li>The first row must contain the headers</li>
                    <li>Values should be comma-separated</li>
                    <li>Performance values must be within the valid range specified by each metric</li>
                  </ul>
                </p>
              </div>

              <!-- Upload Button -->
              <div>
                <!-- 修改验证进度显示部分 -->
                <div class="mb-4 rounded-2xl overflow-hidden bg-white/80 backdrop-blur-xl border border-white/20 shadow-lg">
                  <!-- 进度条标题 -->
                  <div class="px-6 py-4 bg-white/40 backdrop-blur-sm border-b border-gray-100">
                    <div class="flex items-center justify-between">
                      <h4 class="text-sm font-medium text-gray-800">File Validation</h4>
                      <span v-if="validationState.isValidating" 
                            class="text-xs text-blue-600 flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50/50 backdrop-blur-sm">
                        <Loader2 class="w-3 h-3 animate-spin" />
                        Validating...
                      </span>
                    </div>
                  </div>

                  <!-- 验证步骤进度条 -->
                  <div class="px-6 py-4">
                    <div class="relative flex items-center" style="height: 32px">
                      <!-- 步骤连接线容器 -->
                      <div class="absolute inset-x-4 top-1/2 -translate-y-1/2">
                        <div class="relative w-full">
                          <!-- 背景线 -->
                          <div class="absolute inset-0 flex">
                            <div v-for="(step, index) in validationState.steps.slice(0, -1)" 
                                 :key="step.id"
                                 class="flex-1 transition-all duration-700 ease-in-out"
                                 :class="{
                                   'bg-gray-200': !fileName || step.status === 'pending' || 
                                                  validationState.steps[index].status === 'error',
                                   'bg-blue-500': step.status === 'validating',
                                   'bg-green-500': step.status === 'success'
                                 }"
                                 style="height: 2px"
                            ></div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 步骤点容器 -->
                    <div class="relative w-full flex justify-between">
                    <div v-for="step in validationState.steps" 
                         :key="step.id"
                           class="flex flex-col items-center">
                        <!-- 状态圆点 -->
                        <div class="w-8 h-8 rounded-full flex items-center justify-center transition-all duration-500"
                         :class="{
                               'bg-gray-100': !fileName || step.status === 'pending',
                               'bg-blue-100 animate-pulse scale-110': step.status === 'validating',
                               'bg-green-100 scale-110': step.status === 'success',
                               'bg-red-100 scale-110': step.status === 'error'
                             }">
                        <Loader2 v-if="step.status === 'validating'"
                                  class="w-4 h-4 text-blue-600 animate-spin" />
                        <CheckCircle2 v-else-if="step.status === 'success'"
                                       class="w-4 h-4 text-green-600" />
                        <AlertCircle v-else-if="step.status === 'error'"
                                      class="w-4 h-4 text-red-600" />
                          <div v-else class="w-4 h-4 rounded-full border-2 border-gray-300"></div>
                      </div>
                      
                        <!-- 步骤名称 -->
                        <span class="text-xs font-medium whitespace-nowrap mt-2"
                                :class="{
                                'text-gray-400': !fileName || step.status === 'pending',
                                  'text-blue-600': step.status === 'validating',
                                  'text-green-600': step.status === 'success',
                                  'text-red-600': step.status === 'error'
                                }">
                          {{ step.name.split(' ')[1] }}
                          </span>
                      </div>
                    </div>
                        </div>
                        
                        <!-- 错误信息 -->
                  <div v-if="getCurrentError()" 
                       class="mt-4 p-3 rounded-xl bg-red-50/50 backdrop-blur-sm border border-red-100/50">
                    <p class="text-sm text-red-600 font-medium">{{ getCurrentError().message }}</p>
                    <ul class="mt-2 space-y-1.5">
                      <li v-for="(item, index) in getCurrentError().invalidItems.slice(0, 2)" 
                                :key="index"
                          class="text-xs text-red-500 flex items-center gap-2">
                              <span class="w-1 h-1 rounded-full bg-red-400"></span>
                              {{ item }}
                            </li>
                      <li v-if="getCurrentError().invalidItems.length > 2"
                          class="text-xs text-red-500/80 italic pl-3">
                        And {{ getCurrentError().invalidItems.length - 2 }} more errors...
                            </li>
                          </ul>
                  </div>
                </div>

                <!-- 原有的文件上传按钮 -->
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
                    'flex items-center justify-center gap-2 px-4 py-3 rounded-xl cursor-pointer transition-colors duration-200',
                    formErrors.file
                      ? 'bg-red-50 border-2 border-red-200 hover:bg-red-100'
                      : 'bg-gray-100/80 hover:bg-gray-200/80 border-2 border-transparent'
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

          <!-- Dialog Footer -->
          <div class="border-t border-gray-100 p-8">
            <div class="flex justify-end">
              <button
                type="button"
                @click.prevent="handleSubmit"
                :disabled="isUploadButtonDisabled"
                class="w-full px-4 py-2 rounded-xl font-medium transition-colors duration-200"
                :class="[
                  isUploadButtonDisabled
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    : 'bg-blue-500 text-white hover:bg-blue-600'
                ]"
              >
                Upload Results
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 反馈对话框 -->
    <Transition name="feedback">
      <div v-if="feedbackState.show" 
          class="fixed top-4 right-4 z-[70] max-w-sm w-full bg-white rounded-xl shadow-lg border overflow-hidden">
        <div class="p-4 flex gap-3" 
            :class="feedbackState.type === 'success' ? 'bg-green-50' : 'bg-red-50'">
          <!-- 图标 -->
          <div class="flex-shrink-0">
            <div v-if="feedbackState.type === 'success'"
                class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
              <svg class="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
            </div>
            <div v-else
                class="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
              <svg class="w-5 h-5 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                      d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </div>
          </div>

          <!-- 内容 -->
          <div class="flex-1">
            <h3 class="text-lg font-medium" 
                :class="feedbackState.type === 'success' ? 'text-green-800' : 'text-red-800'">
              {{ feedbackState.title }}
            </h3>
            <p class="mt-1 text-sm" 
              :class="feedbackState.type === 'success' ? 'text-green-600' : 'text-red-600'">
              {{ feedbackState.message }}
            </p>
          </div>

          <!-- 关闭按钮 -->
          <button @click="feedbackState.show = false"
                  class="flex-shrink-0 flex items-center justify-center w-6 h-6 rounded-full 
                        hover:bg-black/5 transition-colors">
            <X class="w-4 h-4" 
              :class="feedbackState.type === 'success' ? 'text-green-600' : 'text-red-600'" />
          </button>
        </div>
      </div>
    </Transition>

    <!-- 添加验证状态显示 -->
    <div v-if="fileName && (validationState.isValidating || validationState.steps.some(step => step.status !== 'pending'))"
         class="mt-4 space-y-3 bg-gray-50 rounded-lg p-4">
      <h3 class="text-sm font-medium text-gray-700">Validation Progress</h3>
      
      <div v-for="step in validationState.steps" 
           :key="step.id"
           class="flex items-center gap-3 p-2 rounded-lg"
           :class="{
             'bg-gray-100': step.status === 'pending',
             'bg-blue-50': step.status === 'validating',
             'bg-green-50': step.status === 'success',
             'bg-red-50': step.status === 'error'
           }">
        
        <!-- 状态图标 -->
        <div class="flex-shrink-0">
          <Loader2 v-if="step.status === 'validating'"
                  class="w-5 h-5 text-blue-500 animate-spin" />
          <CheckCircle2 v-else-if="step.status === 'success'"
                       class="w-5 h-5 text-green-500" />
          <AlertCircle v-else-if="step.status === 'error'"
                      class="w-5 h-5 text-red-500" />
          <div v-else class="w-5 h-5 rounded-full border-2 border-gray-300"></div>
        </div>
        
        <!-- 步骤信息 -->
        <div class="flex-1">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium" 
                  :class="{
                    'text-gray-600': step.status === 'pending',
                    'text-blue-600': step.status === 'validating',
                    'text-green-600': step.status === 'success',
                    'text-red-600': step.status === 'error'
                  }">
              {{ step.name }}
            </span>
            <span v-if="step.status === 'validating'" 
                  class="text-xs text-blue-500">
              Validating...
            </span>
          </div>
          
          <!-- 错误信息 -->
          <div v-if="step.status === 'error' && step.invalidItems.length" 
               class="mt-2 text-sm text-red-500">
            <p class="font-medium">{{ step.message }}:</p>
            <ul class="mt-1 ml-4 list-disc">
              <li v-for="(item, index) in step.invalidItems" 
                  :key="index">
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 0.3s ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
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

/* 添加清除按钮动画 */
button {
  transition: all 0.2s ease;
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

/* 添加验证状态相关动画 */
.validate-enter-active,
.validate-leave-active {
  transition: all 0.3s ease;
}

.validate-enter-from,
.validate-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style> 