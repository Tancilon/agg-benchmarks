<script setup>
import { ClipboardIcon, Database, Network, BarChart, FileText, Mail, Github, Filter, Calendar, BookOpen, Plus, Search, Hash } from 'lucide-vue-next'
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import UploadDatasetDialog from '../components/UploadDatasetDialog.vue'
import UploadAlgorithmDialog from '../components/UploadAlgorithmDialog.vue'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import { getMetricInfo } from '../utils/metrics'

const router = useRouter()

// 数据集相关状态
const datasets = ref([])
const categories = ref([])
const currentPage = ref(1)
const pageSize = ref(100)
const selectedCategory = ref('all')
const totalItems = ref(0)
const showUploadDatasetDialog = ref(false)
const currentDatasetPage = ref(1)
const datasetsPerPage = ref(8)

// Results by Dataset 分页相关
const currentResultPage = ref(1)
const resultsPerPage = ref(10)  // 每页显示10条数据

// 算法相关状态
const algorithms = ref([])
const selectedAlgorithmCategory = ref('all')
const currentAlgorithmPage = ref(1)
const algorithmsPerPage = ref(8)
const activeTab = ref('dataset')
const algorithmPerformanceData = ref({})
const algorithmMetrics = ref({})
const availableMetrics = ref([])

// 添加数据集性能相关的状态
const datasetPerformanceData = ref({})
const datasetMetrics = ref({})

// 添加出处颜色映射
const sourceColors = {
  'TPAMI': '#FF6B6B',
  'IJCAI': '#4ECDC4',
  'AAAI': '#45B7D1',
  'ICCV': '#96CEB4',
  'CVPR': '#FFEEAD',
  'ECCV': '#D4A5A5',
  'NeurIPS': '#9B59B6'
}

// 添加类别颜色映射
const categoryColors = {
  'Unsupervised': { bg: '#336FFF20', text: '#336FFF' },
  'Supervised': { bg: '#D7BEFD20', text: '#D7BEFD' },
  'Semi-Supervised': { bg: '#B6A49420', text: '#B6A494' },
  'Undefined': { bg: '#86868B20', text: '#86868B' }
}

// 添加算法图标颜色映射
const algorithmIconColors = {
  'HNSW': '#FF6B6B',
  'KGraph': '#4ECDC4',
  'Annoy': '#45B7D1',
  'FAISS': '#96CEB4',
  'NGT': '#FFEEAD',
  'SW-Graph': '#D4A5A5',
  'IVF': '#9B59B6'
}

// 生成随机颜色
const generateRandomColor = () => {
  const colors = [
    '#FF7F50', '#87CEEB', '#DDA0DD', '#F0E68C', '#98FB98',
    '#E6A8D7', '#B0C4DE', '#DEB887', '#20B2AA', '#D8BFD8'
  ]
  return colors[Math.floor(Math.random() * colors.length)]
}

// 获取出处的颜色，如果是自定义出处则生成并缓存随机颜色
const getSourceColor = (source) => {
  if (!sourceColors[source]) {
    // 使用localStorage来持久化存储自定义标签的颜色
    const storedColors = JSON.parse(localStorage.getItem('customSourceColors') || '{}')
    if (storedColors[source]) {
      sourceColors[source] = storedColors[source]
    } else {
      sourceColors[source] = generateRandomColor()
      storedColors[source] = sourceColors[source]
      localStorage.setItem('customSourceColors', JSON.stringify(storedColors))
    }
  }
  return sourceColors[source]
}

// 获取类别的颜色，如果是自定义类别则生成并缓存随机颜色
const getCategoryColor = (category) => {
  if (!categoryColors[category]) {
    // 使用 localStorage 持久化存储自定义类别的颜色
    const storedColors = JSON.parse(localStorage.getItem('customCategoryColors') || '{}')
    if (storedColors[category]) {
      categoryColors[category] = storedColors[category]
    } else {
      // 为新类别生成一个随机颜色
      const predefinedColors = [
        '#FF6347', // 红色系
        '#4ECDC4', // 青色系
        '#FF69B4', // 粉色系
        '#22C55E', // 绿色系
        '#9B59B6', // 紫色系
        '#F1C40F', // 黄色系
        '#E67E22', // 橙色系
        '#3498DB'  // 蓝色系
      ]
      const color = predefinedColors[Object.keys(categoryColors).length % predefinedColors.length]
      categoryColors[category] = {
        bg: `${color}20`,
        text: color
      }
      // 保存到 localStorage 以保持一致性
      storedColors[category] = categoryColors[category]
      localStorage.setItem('customCategoryColors', JSON.stringify(storedColors))
    }
  }
  return categoryColors[category]
}

// 获取算法图标颜色
const getAlgorithmIconColor = (algorithmName) => {
  if (!algorithmIconColors[algorithmName]) {
    // 使用localStorage来持久化存储自定义算法的颜色
    const storedColors = JSON.parse(localStorage.getItem('customAlgorithmColors') || '{}')
    if (storedColors[algorithmName]) {
      algorithmIconColors[algorithmName] = storedColors[algorithmName]
    } else {
      algorithmIconColors[algorithmName] = generateRandomColor()
      storedColors[algorithmName] = algorithmIconColors[algorithmName]
      localStorage.setItem('customAlgorithmColors', JSON.stringify(storedColors))
    }
  }
  return algorithmIconColors[algorithmName]
}

// 获取数据集列表
const fetchDatasets = async () => {
  console.log('Fetching datasets...')
  try {
    const response = await fetch(
      `/api/datasets?page=0&size=${pageSize.value}${
        selectedCategory.value !== 'all' ? `&category=${selectedCategory.value}` : ''
      }`
    )
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
    const data = await response.json()
    datasets.value = data.content
    totalItems.value = data.totalElements
    console.log('Datasets fetched successfully:', data)

    // 获取每个数据集的性能数据
    for (const dataset of datasets.value) {
      try {
        // 1. 获取该数据集的可用指标
        console.log(`Fetching metrics for dataset: ${dataset.name}`)
        const metricsResponse = await fetch(`/api/results/metrics/${dataset.name}`)
        if (!metricsResponse.ok) throw new Error('Failed to fetch available metrics')
        const metrics = await metricsResponse.json()
        console.log(`Available metrics for ${dataset.name}:`, metrics)
        
        // 2. 如果有可用指标，获取第一个指标的性能数据
        if (metrics.length > 0) {
          const firstMetric = metrics[0]
          datasetMetrics.value[dataset.name] = firstMetric
          console.log(`Selected metric for ${dataset.name}:`, firstMetric)
          
          // 3. 获取性能数据
          console.log(`Fetching performance data for ${dataset.name} with metric ${firstMetric}`)
          const performanceResponse = await fetch(`/api/results/${dataset.name}/${firstMetric}`)
          if (!performanceResponse.ok) throw new Error('Failed to fetch performance data')
          const data = await performanceResponse.json()
          console.log(`Performance data for ${dataset.name}:`, data)
          datasetPerformanceData.value[dataset.name] = data
        }
      } catch (error) {
        console.error(`Error fetching data for dataset ${dataset.name}:`, error)
      }
    }

    console.log('Final datasetMetrics:', datasetMetrics.value)
    console.log('Final datasetPerformanceData:', datasetPerformanceData.value)

  } catch (error) {
    console.error('Error fetching datasets:', error)
    datasets.value = []
    totalItems.value = 0
  }
}

// 获取分类统计
const fetchCategories = async () => {
  try {
    const response = await fetch('/api/datasets/categories/stats')
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText);
    }
    const data = await response.json()
    categories.value = data
  } catch (error) {
    console.error('Error fetching categories:', error)
    alert('Failed to load categories: ' + error.message)
  }
}

// 处理分类切换
const handleCategoryChange = (category) => {
  selectedCategory.value = category
  currentDatasetPage.value = 1
  currentAlgorithmPage.value = 1
  currentResultPage.value = 1
  fetchDatasets()
}

// 修改算法分类相关的状态和方法
const algorithmCategories = ref([{ id: 'all', name: 'All' }])

// 添加获取算法类别的函数
const fetchAlgorithmCategories = async () => {
  try {
    const response = await fetch('/api/algorithms/categories/stats')
    if (!response.ok) throw new Error('Failed to fetch algorithm categories')
    const data = await response.json()
    
    // 将获取到的类别添加到数组中，保持 'All' 选项在最前面
    algorithmCategories.value = [
  { id: 'all', name: 'All' },
      ...data.map(category => ({
        id: category.name,
        name: category.name,
        count: category.count
      }))
    ]
    
    console.log('Fetched algorithm categories:', algorithmCategories.value)
  } catch (error) {
    console.error('Error fetching algorithm categories:', error)
  }
}

// 获取算法列表
const fetchAlgorithms = async (category = '') => {
  try {
    const url = category && category !== 'all'
      ? `/api/algorithms?category=${encodeURIComponent(category)}`
      : '/api/algorithms'
    
    console.log('Fetching algorithms from:', url)
    const response = await fetch(url)
    if (!response.ok) throw new Error('Failed to fetch algorithms')
    const data = await response.json()
    
    // 直接使用后端返回的数据
    algorithms.value = data
    
    console.log('Fetched algorithms with categories:', algorithms.value)
    
    // 获取每个算法的性能数据
    for (const algorithm of algorithms.value) {
      try {
        // 1. 获取该算法的可用指标
        console.log(`Fetching metrics for algorithm: ${algorithm.name}`)
        const metricsResponse = await fetch(`/api/algorithms/${algorithm.name}/metrics`)
        if (!metricsResponse.ok) throw new Error('Failed to fetch available metrics')
        const metrics = await metricsResponse.json()
        console.log(`Available metrics for ${algorithm.name}:`, metrics)
        
        // 2. 如果有可用指标，获取第一个指标的性能数据
        if (metrics.length > 0) {
          const firstMetric = metrics[0]
          algorithmMetrics.value[algorithm.name] = firstMetric
          console.log(`Selected metric for ${algorithm.name}:`, firstMetric)
          
          // 3. 获取性能数据
          console.log(`Fetching performance data for ${algorithm.name} with metric ${firstMetric}`)
          const performanceResponse = await fetch(`/api/algorithms/${algorithm.name}/performance/${firstMetric}`)
          if (!performanceResponse.ok) throw new Error('Failed to fetch performance data')
          const data = await performanceResponse.json()
          console.log(`Performance data for ${algorithm.name}:`, data)
          algorithmPerformanceData.value[algorithm.name] = data
        }
      } catch (error) {
        console.error(`Error fetching data for algorithm ${algorithm.name}:`, error)
      }
    }

    console.log('Final algorithmMetrics:', algorithmMetrics.value)
    console.log('Final algorithmPerformanceData:', algorithmPerformanceData.value)

  } catch (error) {
    console.error('Error fetching algorithms:', error)
  }
}

// 处理算法分类切换
const handleAlgorithmCategoryChange = (category) => {
  selectedAlgorithmCategory.value = category.id
  currentAlgorithmPage.value = 1
  fetchAlgorithms(category.id)
}

// 添加筛选状态
const algorithmFilters = ref({
  categories: [],
  years: [],
  sources: []
})

// 添加筛选器展开状态
const expandedFilters = ref({
  categories: false,
  years: false,
  sources: false
})

// 获取所有可用的年份
const availableYears = computed(() => {
  if (!algorithms.value) return []
  const years = [...new Set(algorithms.value.map(algo => algo.year))]
  return years.sort((a, b) => b - a) // 降序排列
})

// 获取所有可用的出处
const availableSources = computed(() => {
  if (!algorithms.value) return []
  const sources = new Set()
  algorithms.value.forEach(algo => {
    algo.sources.forEach(source => sources.add(source))
  })
  return Array.from(sources).sort()
})

// 修改过滤器计算属性
const filteredAlgorithmResults = computed(() => {
  if (!algorithms.value) return []
  
  return algorithms.value.filter(algo => {
    // 类别筛选
    if (algorithmFilters.value.categories.length > 0) {
      const hasMatchingCategory = algo.categories.some(category => 
        algorithmFilters.value.categories.includes(category)
      )
      if (!hasMatchingCategory) return false
    }
    
    // 年份筛选
    if (algorithmFilters.value.years.length > 0) {
      if (!algorithmFilters.value.years.includes(algo.year)) return false
    }
    
    // 出处筛选
    if (algorithmFilters.value.sources.length > 0) {
      const hasMatchingSource = algo.sources.some(source => 
        algorithmFilters.value.sources.includes(source)
      )
      if (!hasMatchingSource) return false
    }
    
    return true
  })
})

// 处理筛选器变化
const handleFilterChange = (type, value) => {
  const index = algorithmFilters.value[type].indexOf(value)
  if (index === -1) {
    algorithmFilters.value[type].push(value)
  } else {
    algorithmFilters.value[type].splice(index, 1)
  }
}

// 清除所有筛选器
const clearFilters = () => {
  algorithmFilters.value = {
    categories: [],
    years: [],
    sources: []
  }
}

// 获取筛选器计数
const getFilterCount = computed(() => {
  return Object.values(algorithmFilters.value).reduce((sum, arr) => sum + arr.length, 0)
})

// 计算分页后的算法列表（用于Algorithms Section）
const paginatedAlgorithms = computed(() => {
  if (!algorithms.value) return []
  
  // 先进行搜索过滤
  let filtered = algorithms.value
  if (algorithmsSearchQuery.value) {
    const query = algorithmsSearchQuery.value.toLowerCase()
    filtered = algorithms.value.filter(algorithm => 
      algorithm.name.toLowerCase().includes(query) ||
      algorithm.description?.toLowerCase().includes(query) ||
      algorithm.categories.some(cat => cat.toLowerCase().includes(query))
    )
  }
  
  // 然后进行分页
  const start = (currentAlgorithmPage.value - 1) * algorithmsPerPage.value
  const end = start + algorithmsPerPage.value
  return filtered.slice(start, Math.min(end, filtered.length))
})

// 计算分页后的算法结果（用于Results by Algorithm）
const paginatedAlgorithmResults = computed(() => {
  const start = (currentAlgorithmPage.value - 1) * algorithmsPerPage.value
  const end = start + algorithmsPerPage.value
  return filteredAlgorithmResults.value.slice(start, Math.min(end, filteredAlgorithmResults.value.length))
})

// 计算算法总页数
const totalAlgorithmPages = computed(() => {
  if (!algorithms.value) return 0
  return Math.ceil(algorithms.value.length / algorithmsPerPage.value)
})

// 切换算法页码
const handleAlgorithmPageChange = (page) => {
  if (page < 1) page = 1
  if (page > totalAlgorithmPages.value) page = totalAlgorithmPages.value
  currentAlgorithmPage.value = page
}

// 修改获取分类数量的方法
const getAlgorithmCountByCategory = (category) => {
  if (!algorithms.value) return 0
  return category.id === 'all'
    ? algorithms.value.length
    : algorithms.value.filter(algo => 
        algo.categories.includes(category.id)
      ).length
}

// 在 script setup 顶部添加 metrics 变量
const metrics = ref([])
const metricsSearchQuery = ref('')
const currentMetricPage = ref(1)
const metricsPerPage = ref(8)

// 添加获取指标列表的方法
const fetchMetrics = async () => {
  try {
    const response = await fetch('/api/metrics')
    if (!response.ok) throw new Error('Failed to fetch metrics')  
    const data = await response.json()
    console.log('Fetched metrics:', data)
    metrics.value = data
  } catch (error) {
    console.error('Error fetching metrics:', error)
    metrics.value = []
  }
}

// 修改过滤和分页逻辑
const filteredMetrics = computed(() => {
  if (!metrics.value || metrics.value.length === 0) return []
  
  // 先进行搜索过滤
  let filtered = metrics.value
  if (metricsSearchQuery.value) {
    const query = metricsSearchQuery.value.toLowerCase()
    filtered = metrics.value.filter(metric => 
      metric.name.toLowerCase().includes(query) ||
      metric.description?.toLowerCase().includes(query) ||
      metric.type.toLowerCase().includes(query)
    )
  }
  
  // 然后进行分页
  const start = (currentMetricPage.value - 1) * metricsPerPage.value
  const end = start + metricsPerPage.value
  return filtered.slice(start, Math.min(end, filtered.length))
})

// 修改总页数计算，使用过滤后的数据长度
const totalMetricPages = computed(() => {
  const filteredLength = metrics.value?.filter(metric => {
    if (!metricsSearchQuery.value) return true
    const query = metricsSearchQuery.value.toLowerCase()
    return metric.name.toLowerCase().includes(query) ||
      metric.description?.toLowerCase().includes(query) ||
      metric.type.toLowerCase().includes(query)
  }).length || 0
  return Math.ceil(filteredLength / metricsPerPage.value)
})

// 添加页码切换方法
const handleMetricPageChange = (page) => {
  if (page < 1) page = 1
  if (page > totalMetricPages.value) page = totalMetricPages.value
  currentMetricPage.value = page
}

// 添加搜索状态
const algorithmsSearchQuery = ref('')
const datasetsSearchQuery = ref('')

// 监听搜索框的变化
watch(algorithmsSearchQuery, () => {
  // 重置算法分页到第一页
  currentAlgorithmPage.value = 1
})

watch(datasetsSearchQuery, () => {
  // 重置数据集分页到第一页
  currentDatasetPage.value = 1
})

watch(metricsSearchQuery, () => {
  // 重置指标分页到第一页
  currentMetricPage.value = 1
})

// 生成图表配置
const getChartOption = computed(() => {
  return (algorithmName) => {
    const data = algorithmPerformanceData.value[algorithmName]
    const metricName = algorithmMetrics.value[algorithmName]
    
    if (!data || !metricName) {
      console.log(`No data or metric for ${algorithmName}`)
      return {}
    }
    
    const metricInfo = getMetricInfo(metricName)
    
    // 计算数据的最大值和最小值
    const allValues = data.series.flatMap(item => item.data)
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
          fontSize: 14,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        formatter: function(params) {
          return `
            <div style="font-weight: 500; margin-bottom: 8px; font-size: 15px;">
              ${params[0].name}
            </div>
            ${params.map(param => `
              <div style="display: flex; justify-content: space-between; align-items: center; margin: 8px 0;">
                <span style="color: #86868b">${param.seriesName}:</span>
                <span style="color: #0066CC; font-weight: 500; font-size: 15px">
                  ${param.value.toFixed(4)}
                </span>
              </div>
            `).join('')}
          `
        }
      },
      grid: {
        top: '8%',       // 从 15% 减少到 8%
        left: '8%',      // 从 15% 减少到 8%
        right: '8%',     // 从 15% 减少到 8%
        bottom: '8%',    // 从 15% 减少到 8%
        containLabel: true,
        width: 'auto',   
        height: 'auto'   
      },
      legend: {
        bottom: 0,
        textStyle: {
          color: '#86868b',
          fontSize: 12,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        itemGap: 20
      }
    }

    if (metricInfo.isKMetric) {
      // 折线图配置
      return {
        ...baseConfig,
        legend: {
          show: false  // 隐藏折线图的图例
        },
        xAxis: {
          type: 'category',
          // 删除 name 属性
          boundaryGap: false,
          data: data.xAxis,
          axisLine: {
            lineStyle: { color: '#e7e7e7' }
          },
          axisTick: { show: false },
          axisLabel: {
            color: '#86868b',
            fontSize: 12,
            margin: 12
          }
        },
        yAxis: {
          type: 'value',
          name: metricInfo.yAxis,
          nameLocation: 'end',  // 设置标题位置在轴的末端（左侧）
          nameGap: 35,         // 调整标题与轴的距离
          nameTextStyle: {
            fontSize: 14,
            color: '#86868b',
            padding: [0, 0, 0, 35],  // 左侧增加额外的内边距
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
          },
          // 删除 name 属性
          splitLine: {
            lineStyle: {
              color: '#f5f5f7',
              type: 'dashed'
            }
          },
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: {
            color: '#86868b',
            fontSize: 12,
            margin: 12,
            formatter: value => value.toFixed(4)
          }
        },
        series: data.series.map(item => ({
          name: item.name,
          type: 'line',
          data: item.data,
          itemStyle: { color: getAlgorithmIconColor(item.name) },
          lineStyle: { 
            width: 2,
            cap: 'round',
            join: 'round'
          },
          symbol: 'circle',
          symbolSize: 6,
          emphasis: {
            focus: 'series',
            scale: 1.1,
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.1)'
          }
        }))
      }
    } else {
      // 柱状图配置
      return {
        ...baseConfig,
        legend: {
          show: false  // 隐藏柱状图的图例
        },
        xAxis: {
          type: 'category',
          data: data.series.map(item => item.name),
          axisLabel: {
            show: false  // 隐藏横轴标签
          },
          axisLine: {
            lineStyle: { color: '#e7e7e7' }
          },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          name: metricInfo.yAxis,
          nameLocation: 'end',  // 设置标题位置在轴的末端（左侧）
          nameGap: 35,         // 调整标题与轴的距离
          nameTextStyle: {
            fontSize: 14,
            color: '#86868b',
            padding: [0, 0, 0, 35],  // 左侧增加额外的内边距
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
          },
          min: value => {
            const allValues = data.series.map(item => item.data[0])
            const minValue = Math.min(...allValues)
            return Number((minValue * 0.95).toFixed(4))  // 最小值下调5%
          },
          max: value => {
            const allValues = data.series.map(item => item.data[0])
            const maxValue = Math.max(...allValues)
            return Number((maxValue * 1.05).toFixed(4))  // 最大值上调5%
          },
          splitLine: {
            lineStyle: {
              color: '#f5f5f7',
              type: 'dashed'
            }
          },
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: {
            color: '#86868b',
            fontSize: 12,
            margin: 12,
            formatter: value => value.toFixed(4)
          }
        },
        series: [{
          type: 'bar',
          data: data.series.map(item => ({
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
            show: false  // 隐藏柱状图的数据标签
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
  }
})


// 修改 getDatasetChartOption 计算属性
const getDatasetChartOption = computed(() => {
  return (datasetName) => {
    const data = datasetPerformanceData.value[datasetName]
    const metricName = datasetMetrics.value[datasetName]
    
    if (!data || !metricName) {
      console.log(`No data or metric for ${datasetName}`)
      return {}
    }
    
    const metricInfo = getMetricInfo(metricName)
    
    // 计算数据的最大值和最小值
    const allValues = data.series.flatMap(item => item.data)
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
          fontSize: 14,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        formatter: function(params) {
          return `
            <div style="font-weight: 500; margin-bottom: 8px; font-size: 15px;">
              ${params[0].name}
            </div>
            ${params.map(param => `
              <div style="display: flex; justify-content: space-between; align-items: center; margin: 8px 0;">
                <span style="color: #86868b">${param.seriesName}:</span>
                <span style="color: #0066CC; font-weight: 500; font-size: 15px">
                  ${param.value.toFixed(4)}
                </span>
              </div>
            `).join('')}
          `
        }
      },
      grid: {
        top: '8%',       // 从 15% 减少到 8%
        left: '8%',      // 从 15% 减少到 8%
        right: '8%',     // 从 15% 减少到 8%
        bottom: '8%',    // 从 15% 减少到 8%
        containLabel: true,
        width: 'auto',   
        height: 'auto'   
      },
      legend: {
        bottom: 0,
        textStyle: {
          color: '#86868b',
          fontSize: 12,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        },
        itemGap: 20
      }
    }

    if (metricInfo.isKMetric) {
      // 折线图配置
      return {
        ...baseConfig,
        legend: {
          show: false  // 隐藏折线图的图例
        },
        xAxis: {
          type: 'category',
          name: metricInfo.xAxis,
          boundaryGap: false,
          data: data.xAxis,
          nameTextStyle: {
            fontSize: 12,
            padding: [0, 0, 0, 10],
            fontWeight: 500,
            color: '#86868b',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
          },
          axisLine: {
            lineStyle: { color: '#e7e7e7' }
          },
          axisTick: { show: false },
          axisLabel: {
            color: '#86868b',
            fontSize: 12,
            margin: 12
          }
        },
        yAxis: {
          type: 'value',
          name: metricInfo.yAxis,
          nameTextStyle: {
            fontSize: 12,
            padding: [0, 0, 10, 0],
            fontWeight: 500,
            color: '#86868b',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
          },
          min: value => Number((minValue - Math.abs(minValue) * 0.1).toFixed(4)),
          max: value => Number((maxValue + Math.abs(maxValue) * 0.1).toFixed(4)),
          splitLine: {
            lineStyle: {
              color: '#f5f5f7',
              type: 'dashed'
            }
          },
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: {
            color: '#86868b',
            fontSize: 12,
            margin: 12,
            formatter: value => value.toFixed(4)
          }
        },
        series: data.series.map(item => ({
          name: item.name,
          type: 'line',
          data: item.data,
          itemStyle: { color: getAlgorithmIconColor(item.name) },
          lineStyle: { 
            width: 2,
            cap: 'round',
            join: 'round'
          },
          symbol: 'circle',
          symbolSize: 6,
          emphasis: {
            focus: 'series',
            scale: 1.1,
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.1)'
          }
        }))
      }
    } else {
      // 柱状图配置
      return {
        ...baseConfig,
        legend: {
          show: false  // 隐藏柱状图的图例
        },
        xAxis: {
          type: 'category',
          data: data.series.map(item => item.name),
          axisLabel: {
            show: false  // 隐藏横轴标签
          },
          axisLine: {
            lineStyle: { color: '#e7e7e7' }
          },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          name: metricInfo.yAxis,
          nameLocation: 'end',  // 设置标题位置在轴的末端（左侧）
          nameGap: 35,         // 调整标题与轴的距离
          nameTextStyle: {
            fontSize: 14,
            color: '#86868b',
            padding: [0, 0, 0, 35],  // 左侧增加额外的内边距
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
          },
          min: value => {
            const allValues = data.series.map(item => item.data[0])
            const minValue = Math.min(...allValues)
            return Number((minValue * 0.95).toFixed(4))  // 最小值下调5%
          },
          max: value => {
            const allValues = data.series.map(item => item.data[0])
            const maxValue = Math.max(...allValues)
            return Number((maxValue * 1.05).toFixed(4))  // 最大值上调5%
          },
          splitLine: {
            lineStyle: {
              color: '#f5f5f7',
              type: 'dashed'
            }
          },
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: {
            color: '#86868b',
            fontSize: 12,
            margin: 12,
            formatter: value => value.toFixed(4)
          }
        },
        series: [{
          type: 'bar',
          data: data.series.map(item => ({
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
            show: false  // 隐藏柱状图的数据标签
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
  }
})

// 修改 onMounted 和 onUnmounted 的位置和结构
onMounted(() => {
  const init = async () => {
    console.log('Component mounted, fetching metrics first...')
    // 先获取指标列表
    await fetchMetrics()
    
  // 数据集相关
  fetchDatasets()
  fetchCategories()
    
    // 获取算法类别
    await fetchAlgorithmCategories()
  
  // 算法相关
  fetchAlgorithms()
  }
  
  init()

  // 事件监听
  const handleDatasetUpdate = async () => {
    try {
      currentDatasetPage.value = 1
      await Promise.all([fetchDatasets(), fetchCategories()])
      const lastPage = Math.ceil(datasets.value.length / datasetsPerPage.value)
      currentDatasetPage.value = lastPage
    } catch (error) {
      console.error('Error refreshing dataset data:', error)
    }
  }

  const handleAlgorithmUpdate = async () => {
    try {
      await fetchAlgorithms()
    } catch (error) {
      console.error('Error refreshing algorithm data:', error)
    }
  }

  const handleMetricUpdate = async () => {
    try {
      currentMetricPage.value = 1
      await fetchMetrics()
      const lastPage = Math.ceil(metrics.value.length / metricsPerPage.value)
      currentMetricPage.value = lastPage
    } catch (error) {
      console.error('Error refreshing metric data:', error)
    }
  }

  window.addEventListener('dataset-updated', handleDatasetUpdate)
  window.addEventListener('algorithm-updated', handleAlgorithmUpdate)
  window.addEventListener('metric-updated', handleMetricUpdate)

  // 返回清理函数
  return () => {
    window.removeEventListener('dataset-updated', handleDatasetUpdate)
    window.removeEventListener('algorithm-updated', handleAlgorithmUpdate)
    window.removeEventListener('metric-updated', handleMetricUpdate)
  }
})

const selectedDatasetCategory = ref('all')

// 修改计算属性，添加分页和随机数据处理
const paginatedDatasetResults = computed(() => {
  const start = (currentResultPage.value - 1) * resultsPerPage.value
  const end = start + resultsPerPage.value
  return datasets.value.map(dataset => ({
    ...dataset,
    metrics: dataset.metrics?.length > 0 
      ? dataset.metrics 
      : generateRandomMetrics()  // 如果没有性能数据，使用随机数据
  })).slice(start, Math.min(end, datasets.value.length))
})

// 计算 Results by Dataset 的总页数
const totalResultPages = computed(() => {
  return Math.ceil(datasets.value.length / resultsPerPage.value)
})

// 切换 Results by Dataset 页码
const handleResultPageChange = (page) => {
  if (page < 1) page = 1
  if (page > totalResultPages.value) page = totalResultPages.value
  currentResultPage.value = page
}

// 修改总页数计算
const totalPages = computed(() => {
  const total = activeTab.value === 'dataset' 
    ? datasets.value.length 
    : filteredAlgorithmResults.value.length
  return Math.ceil(total / pageSize.value)
})

// 计算数据集总页数
const totalDatasetPages = computed(() => {
  return Math.ceil(datasets.value.length / datasetsPerPage.value)
})

// 切换数据集页码
const handleDatasetPageChange = (page) => {
  if (page < 1) page = 1
  if (page > totalDatasetPages.value) page = totalDatasetPages.value
  currentDatasetPage.value = page
}

// 处理上传成功
const handleUploadSuccess = async () => {
  console.log('handleUploadSuccess called') // 添加调试日志
  try {
    console.log('Starting data refresh...') // 添加调试日志
    
    // 重置当前页码
    currentDatasetPage.value = 1
    
    // 等待数据刷新完成
    await Promise.all([
      fetchDatasets(),  // 重新获取数据集列表
      fetchCategories()  // 重新获取分类统计
    ])
    
    console.log('Data refreshed successfully') // 添加调试日志
    
    // 计算并跳转到最后一页
    const lastPage = Math.ceil(datasets.value.length / datasetsPerPage.value)
    currentDatasetPage.value = lastPage
    console.log('Moved to last page:', lastPage) // 添加调试日志
  } catch (error) {
    console.error('Error refreshing data:', error)
  }
}

// 计算分页后的数据集
const paginatedDatasets = computed(() => {
  if (!datasets.value) return []
  
  // 先进行搜索过滤
  let filtered = datasets.value
  if (datasetsSearchQuery.value) {
    const query = datasetsSearchQuery.value.toLowerCase()
    filtered = datasets.value.filter(dataset => 
      dataset.name.toLowerCase().includes(query) ||
      dataset.description?.toLowerCase().includes(query)
    )
  }
  
  // 然后进行分页
  const start = (currentDatasetPage.value - 1) * datasetsPerPage.value
  const end = start + datasetsPerPage.value
  return filtered.slice(start, Math.min(end, filtered.length))
})

// 添加生成随机数据的函数
const generateRandomMetrics = () => {
  return Array.from({ length: 7 }, () => ({
    value: Math.floor(Math.random() * (200 - 50) + 50),  // 生成50-200之间的随机数
    color: [
      '#336FFF', '#D7BEFD', '#FF69B4', '#FF4444',
      '#FF7F50', '#FFD700', '#4169E1'
    ][Math.floor(Math.random() * 7)]  // 随机选择一个颜色
  }))
}

// 添加检查标签容器是否需要滚动的方法
const checkNeedsScroll = (containerEl, contentEl) => {
  return contentEl.scrollWidth > containerEl.clientWidth
}

// 修改滚动检查逻辑
const vScrollCheck = {
  mounted: (el) => {
    const container = el
    const content = el.querySelector('.tag-scroll-content')
    
    const updateScroll = () => {
      if (content.scrollWidth > container.clientWidth) {
        content.classList.add('scroll-animation')
        // 设置滚动距离为内容宽度
        el.style.setProperty('--scroll-width', `-${content.scrollWidth}px`)
      } else {
        content.classList.remove('scroll-animation')
      }
    }

    updateScroll()
    window.addEventListener('resize', updateScroll)
  }
}

const showUploadDialog = ref(false)
</script>

<template>
  <div class="min-h-screen bg-background">
    <!-- Hero Section -->
    <section class="bg-zinc-900 px-4 py-16">
      <div class="container mx-auto grid md:grid-cols-2 gap-8 items-center">
        <div class="space-y-4">
          <h1 class="text-4xl font-bold text-white leading-tight">
            Comprehensive performance evaluation of rank aggregation algorithms
          </h1>
          <p class="text-zinc-400">with multi-dataset support and dynamic visualizations.</p>
        </div>
        <div class="relative">
          <div class="w-96 h-96 rounded-full flex items-center justify-center text-white text-2xl font-semibold relative shadow-lg border-8 border-white" style="background-color: #336FFF">
            <span class="opacity-90">Unsupervised</span>
            <div class="absolute -right-32 -top-16 w-64 h-64 rounded-full flex items-center justify-center text-zinc-800 text-xl font-semibold shadow-lg border-8 border-white backdrop-blur" style="background-color: #D7BEFD">
              <span class="opacity-90">Supervised</span>
            </div>
            <div class="absolute -right-24 bottom-0 w-52 h-52 rounded-full flex items-center justify-center text-zinc-800 text-lg font-semibold shadow-lg border-8 border-white backdrop-blur" style="background-color: #B6A494">
              <span class="opacity-90">Semi-Supervised</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Information Section -->
    <section class="py-16 px-4" style="background: linear-gradient(180deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 50%, rgba(240,244,248,1) 100%)">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-12 flex items-center justify-center gap-2">
          <ClipboardIcon class="w-5 h-5" />
          Information
        </h2>
        <div class="grid grid-cols-1 gap-8">
          <div>
            <p class="text-zinc-600 leading-relaxed">
              RA-Lib is an open-source toolkit based on GitHub repository that includes public benchmarks. This toolkit provides open-source algorithm implementations, processes experimental results across different datasets, and generates comparative visualizations. You can access it at 
              <a href="https://github.com/nercms-mmap/RA-Lib" class="text-blue-600 hover:underline">
                https://github.com/nercms-mmap/RA-Lib
              </a>.

              This website hosts current benchmark results and aims to provide users with an enhanced interactive experience. The website project repository is available at 
              <a href="https://github.com/Tancilon/agg-benchmarks" class="text-blue-600 hover:underline">
                https://github.com/Tancilon/agg-benchmarks
              </a>.

              We welcome pull requests on GitHub to contribute your own code or improvements to either the RA-Lib toolkit or this website.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Datasets Section -->
    <section id="datasets" class="py-16 px-4" style="background: linear-gradient(180deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 100%)">
      <div class="container mx-auto">
        <!-- Header -->
        <div class="flex flex-col items-center space-y-4 mb-12">
          <div class="flex items-center gap-2">
            <Database class="w-5 h-5 text-gray-900" />
            <h2 class="text-2xl font-semibold text-gray-900">Datasets</h2>
          </div>
          
          <!-- Search and Filter -->
          <div class="flex items-center justify-center gap-4 w-full mt-6">
            <div class="relative w-96">
              <input
                type="text"
                v-model="datasetsSearchQuery"
                placeholder="Search datasets..."
                class="w-full pl-10 pr-4 py-3 bg-white/70 backdrop-blur-sm rounded-xl border border-gray-200/50 shadow-inner 
                       focus:ring-0 focus:border-gray-300 transition-all duration-300
                       placeholder:text-gray-400 text-gray-700"
              />
              <Search class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
            </div>

            <button class="p-3 bg-white/70 backdrop-blur-sm border border-gray-200/50 rounded-xl shadow-sm hover:border-gray-300 transition-all duration-300">
              <Filter class="w-5 h-5 text-gray-500" />
            </button>
          </div>
        </div>

        <!-- Dataset Cards -->
        <div class="space-y-12">
          <!-- Empty State -->
          <div v-if="!paginatedDatasets.length" class="text-center py-12">
            <Database class="w-16 h-16 mx-auto mb-4 text-gray-400" />
            <p class="text-gray-500">No datasets found.</p>
          </div>

          <!-- Dataset Cards (only show when there is data) -->
          <div v-else>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
              <div v-for="dataset in paginatedDatasets" :key="dataset.id" 
                   class="p-6 rounded-xl shadow-lg border border-gray-100 text-center transform transition-all duration-300 hover:scale-105 bg-white hover:shadow-xl cursor-pointer"
                   @click="router.push(`/dataset/${dataset.name}`)">
                <Database class="w-12 h-12 mx-auto text-[#336FFF] mb-4" />
                <h3 class="font-medium mb-2">{{ dataset.name }}</h3>
                <p class="text-sm text-[#336FFF]">{{ dataset.category }}</p>
              </div>
            </div>

            <!-- Dot Pagination -->
            <div class="flex justify-center mt-8 gap-2">
              <div 
                v-for="page in totalDatasetPages" 
                :key="page"
                @click="handleDatasetPageChange(page)"
                class="w-2 h-2 rounded-full cursor-pointer transition-colors"
                :class="currentDatasetPage === page ? 'bg-blue-500' : 'bg-gray-300 hover:bg-gray-400'"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Algorithms Section -->
    <section id="algorithms" class="py-16 px-4" style="background: linear-gradient(0deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 100%)">
      <div class="container mx-auto">
        <!-- Header -->
        <div class="flex flex-col items-center space-y-4 mb-12">
          <div class="flex items-center gap-2">
            <Network class="w-5 h-5 text-gray-900" />
            <h2 class="text-2xl font-semibold text-gray-900">Algorithms</h2>
          </div>
          
          <!-- Search and Filter -->
          <div class="flex items-center justify-center gap-4 w-full mt-6">
            <div class="relative w-96">
              <input
                type="text"
                v-model="algorithmsSearchQuery"
                placeholder="Search algorithms..."
                class="w-full pl-10 pr-4 py-3 bg-white/70 backdrop-blur-sm rounded-xl border border-gray-200/50 shadow-inner 
                       focus:ring-0 focus:border-gray-300 transition-all duration-300
                       placeholder:text-gray-400 text-gray-700"
              />
              <Search class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
            </div>

            <button class="p-3 bg-white/70 backdrop-blur-sm border border-gray-200/50 rounded-xl shadow-sm hover:border-gray-300 transition-all duration-300">
              <Filter class="w-5 h-5 text-gray-500" />
            </button>
          </div>
        </div>

        <!-- Algorithm Cards -->
        <div v-if="algorithms && algorithms.length > 0" class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div v-for="algorithm in paginatedAlgorithms" 
               :key="algorithm.id"
               class="p-6 rounded-xl shadow-lg border border-gray-100 text-center transform transition-all duration-300 hover:scale-105 bg-white hover:shadow-xl cursor-pointer"
               @click="router.push(`/algorithm/${algorithm.name}`)">
            <!-- 算法图标 -->
            <Network 
              class="w-12 h-12 mx-auto mb-4 transition-colors duration-300" 
              :style="{ color: getAlgorithmIconColor(algorithm.name) }"
            />
            
            <!-- 算法名称 -->
            <h3 class="font-medium text-lg mb-2">{{ algorithm.name }}</h3>
            
            <!-- 标签行 -->
            <div class="relative w-full mb-3">
              <!-- 渐变遮罩 -->
              <div class="absolute left-0 top-0 bottom-0 w-8 bg-gradient-to-r from-white to-transparent z-10"></div>
              <div class="absolute right-0 top-0 bottom-0 w-8 bg-gradient-to-l from-white to-transparent z-10"></div>
              
              <!-- 标签容器 -->
              <div class="tag-scroll-container" v-scroll-check>
                <div class="tag-scroll-content">
                  <!-- 标签 -->
                  <span v-for="category in algorithm.categories" 
                        :key="category"
                        class="px-2 py-0.5 text-xs rounded-full flex-shrink-0" 
                        :style="{
                          backgroundColor: getCategoryColor(category).bg,
                          color: getCategoryColor(category).text
                        }">
                    {{ category }}
                  </span>
                  
                  <span class="px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600 flex-shrink-0">
                    {{ algorithm.year }}
                  </span>
                  
                  <span v-for="source in algorithm.sources" 
                        :key="source"
                        class="px-2 py-0.5 text-xs rounded-full text-white source-tag flex-shrink-0"
                        :style="{ backgroundColor: getSourceColor(source) }">
                    {{ source }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else-if="!algorithms" 
             class="text-center py-12 text-gray-500">
          <Network class="w-16 h-16 mx-auto mb-4 opacity-50" />
          <p>Loading algorithms...</p>
        </div>

        <!-- No Results State -->
        <div v-else 
             class="text-center py-12 text-gray-500">
          <Network class="w-16 h-16 mx-auto mb-4 opacity-50" />
          <p>No algorithms found.</p>
        </div>

        <!-- Pagination -->
        <div v-if="algorithms && algorithms.length > 0" class="flex justify-center mt-8 gap-2">
          <div 
            v-for="page in totalAlgorithmPages" 
            :key="page"
            @click="handleAlgorithmPageChange(page)"
            class="w-2 h-2 rounded-full cursor-pointer transition-colors"
            :class="currentAlgorithmPage === page ? 'bg-blue-500' : 'bg-gray-300 hover:bg-gray-400'"
          ></div>
        </div>
      </div>
    </section>

    <!-- Metrics Section -->
    <section class="space-y-8 rounded-2xl p-8" 
             style="background: linear-gradient(180deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 50%, rgba(240,244,248,1) 100%)">
      <!-- Header -->
      <div class="flex flex-col items-center space-y-4 mb-8">
        <div class="flex items-center gap-2">
          <BarChart class="w-5 h-5 text-gray-900" />
          <h2 class="text-2xl font-semibold text-gray-900">Performance Metrics</h2>
        </div>
        <p class="text-sm text-gray-500">Explore and analyze different evaluation metrics</p>
      </div>

      <!-- Filters and Search -->
      <div class="flex items-center justify-center gap-4 mb-8">
        <!-- Search -->
        <div class="relative w-96">
          <input
            type="text"
            v-model="metricsSearchQuery"
            placeholder="Search metrics..."
            class="w-full pl-10 pr-4 py-3 bg-white/70 backdrop-blur-sm rounded-xl border border-gray-200/50 shadow-inner 
                   focus:ring-0 focus:border-gray-300 transition-all duration-300
                   placeholder:text-gray-400 text-gray-700"
          />
          <Search class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
        </div>

        <!-- Filter -->
        <button class="p-3 bg-white/70 backdrop-blur-sm border border-gray-200/50 rounded-xl shadow-sm hover:border-gray-300 transition-all duration-300">
          <Filter class="w-5 h-5 text-gray-500" />
        </button>
      </div>

      <!-- Metrics Grid -->
      <div class="container mx-auto">
        <!-- Empty State -->
        <div v-if="!filteredMetrics.length" class="text-center py-12">
          <BarChart class="w-16 h-16 mx-auto mb-4 text-gray-400" />
          <p class="text-gray-500">No metrics found.</p>
        </div>

        <!-- Metrics Grid (only show when there is data) -->
        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <!-- Metric Card -->
          <div
            v-for="metric in filteredMetrics"
            :key="metric.id"
            class="group relative bg-white/80 backdrop-blur-sm rounded-2xl p-6 
                    shadow-[0_0_20px_rgba(0,0,0,0.05)] hover:shadow-[0_0_25px_rgba(0,0,0,0.1)] 
                    transition-all duration-500 border border-gray-100/50 hover:border-gray-200/50
                    hover:bg-gradient-to-br hover:from-white hover:to-blue-50/50
                    flex flex-col justify-center min-h-[120px]"
          >
            <!-- Range Badge -->
            <div class="absolute right-6 top-1/2 -translate-y-1/2 flex items-center gap-1.5 px-3 py-1 
                        bg-blue-50 backdrop-blur-sm rounded-full text-sm text-blue-600/90 border border-blue-100/50">
              <span>range: {{ metric.range }}</span>
            </div>

            <!-- Metric Header -->
            <div class="flex items-center gap-3 pr-32">
              <div class="w-10 h-10 rounded-xl bg-blue-500/10 flex items-center justify-center">
                <BarChart class="w-5 h-5 text-blue-600" />
              </div>
              <h3 class="text-lg font-medium text-gray-800 truncate">{{ metric.name }}</h3>
            </div>

            <!-- Hover Actions -->
            <div 
              class="absolute inset-0 bg-white/95 backdrop-blur-md opacity-0 group-hover:opacity-100 
                     transition-all duration-300 flex flex-col items-center justify-center gap-3 rounded-2xl"
            >
              <button 
                @click="router.push(`/metric/${metric.name}`)"
                class="p-3 bg-blue-500 text-white rounded-xl hover:bg-blue-600 
                             transition-all duration-300 shadow-lg hover:shadow-xl 
                             hover:scale-105 active:scale-95">
                <FileText class="w-5 h-5" />
              </button>
              <span class="text-sm font-medium text-gray-600">View Details</span>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="totalMetricPages > 1" class="flex justify-center mt-8 gap-2">
          <div 
            v-for="page in totalMetricPages" 
            :key="page"
            @click="handleMetricPageChange(page)"
            class="w-2 h-2 rounded-full cursor-pointer transition-colors"
            :class="currentMetricPage === page ? 'bg-blue-500' : 'bg-gray-300 hover:bg-gray-400'"
          ></div>
        </div>
      </div>
    </section>

    <!-- Result Visualizations Section -->
    <section class="py-16 px-4" style="background: linear-gradient(180deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 50%, rgba(240,244,248,1) 100%)">
      <div class="container mx-auto">
        <div class="flex items-center gap-2 justify-center mb-12">
          <FileText class="w-5 h-5 text-gray-900" />
          <h2 class="text-2xl font-semibold text-gray-900">Result Visualizations</h2>
        </div>
        
        <!-- Tabs -->
        <div class="flex justify-center mb-12">
          <div class="flex gap-8 border-b border-gray-200">
            <button 
              @click="activeTab = 'dataset'"
              :class="[
                'pb-4 px-4 text-sm font-medium',
                activeTab === 'dataset' 
                  ? 'border-b-2 border-blue-500 text-blue-500' 
                  : 'text-gray-500 hover:text-gray-700'
              ]"
            >
              Results by Dataset
            </button>
            <button 
              @click="activeTab = 'algorithm'"
              :class="[
                'pb-4 px-4 text-sm font-medium',
                activeTab === 'algorithm' 
                  ? 'border-b-2 border-blue-500 text-blue-500' 
                  : 'text-gray-500 hover:text-gray-700'
              ]"
            >
              Results by Algorithm
            </button>
          </div>
        </div>

        <!-- Results Content -->
        <div v-if="activeTab === 'dataset'" class="space-y-12">
          <!-- Category Filter -->
          <div class="flex justify-center mb-8">
            <div class="inline-flex p-1 rounded-xl bg-gray-100">
              <!-- All 标签 -->
              <button 
                @click="handleCategoryChange('all')"
                class="px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300"
                :class="[
                  selectedCategory === 'all'
                    ? 'bg-white text-gray-900 shadow-sm' 
                    : 'text-gray-600 hover:text-gray-900'
                ]"
              >
                All
                <span class="ml-2 px-2 py-0.5 rounded-full text-xs bg-blue-50 text-blue-600">
                  {{ totalItems }}
                </span>
              </button>

              <!-- 其他分类标签 -->
              <button 
                v-for="category in categories"
                :key="category.id"
                @click="handleCategoryChange(category.category)"
                class="px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300"
                :class="[
                  selectedCategory === category.category
                    ? 'bg-white text-gray-900 shadow-sm' 
                    : 'text-gray-600 hover:text-gray-900'
                ]"
              >
                {{ category.category }}
                <span class="ml-2 px-2 py-0.5 rounded-full text-xs bg-blue-50 text-blue-600">
                  {{ category.count }}
                </span>
              </button>
            </div>
          </div>
          
          <!-- Dataset Results -->
          <div v-for="(datasetResult, index) in paginatedDatasetResults" 
               :key="index"
               class="grid md:grid-cols-2 gap-8 rounded-2xl overflow-hidden shadow-2xl" 
               style="background: linear-gradient(145deg, #1a1f35 0%, #2a3045 100%)">
            <!-- Left Panel -->
            <div class="p-8 text-white flex flex-col justify-between h-full">
              <div class="space-y-4">
                <div class="flex items-center gap-3">
                  <h3 class="text-2xl font-semibold">{{ datasetResult.name }}</h3>
                  <!-- 类别标签组 -->
                  <div class="flex items-center gap-2 max-w-[300px] overflow-hidden">
                  <span 
                      class="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors"
                      :style="{
                        backgroundColor: getCategoryColor(datasetResult.category || 'Undefined').bg,
                        color: getCategoryColor(datasetResult.category || 'Undefined').text
                      }">
                      {{ datasetResult.category || 'Undefined' }}
                  </span>
                </div>
                </div>
                
                <p class="text-zinc-300 line-clamp-2">{{ datasetResult.description }}</p>
                
                <p class="text-sm text-zinc-300 line-clamp-2">
                  Click the following button to explore detailed performance metrics across different algorithms.
                </p>
              </div>
              
              <div class="mt-6">
                <button 
                  @click="router.push(`/dataset/${datasetResult.name}`)"
                  class="inline-block bg-[#336FFF] hover:bg-[#2952cc] text-white px-6 py-2 rounded-lg transition-all duration-300 hover:shadow-lg"
                >
                  View Details
                </button>
              </div>
            </div>

            <!-- Right Panel -->
            <div class="bg-[#2a3045]/30 p-8 backdrop-blur-sm border-l border-white/10">
              <div class="h-full flex items-center">
                <!-- Performance Chart -->
                <div class="w-full h-64">
                  <v-chart 
                    v-if="datasetPerformanceData[datasetResult.name] && 
                          datasetMetrics[datasetResult.name]"
                    :option="getDatasetChartOption(datasetResult.name)"
                    autoresize
                    class="!bg-transparent"
                  />
                  <div v-else 
                       class="w-full h-full flex items-center justify-center text-sm text-zinc-400">
                    Loading performance data...
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-if="datasets.length === 0" 
               class="text-center py-12 text-gray-500">
            <Database class="w-16 h-16 mx-auto mb-4 opacity-50" />
            <p>No datasets found in this category.</p>
          </div>

          <!-- Results Pagination -->
          <div v-if="datasets.length > resultsPerPage" class="flex justify-center items-center gap-2 pt-8">
            <button 
              @click="handleResultPageChange(currentResultPage - 1)"
              :disabled="currentResultPage === 1"
              class="p-2 rounded-lg transition-colors"
              :class="currentResultPage === 1 ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              Previous
            </button>
            
            <div class="flex items-center gap-2">
              <button 
                v-for="page in totalResultPages" 
                :key="page"
                @click="handleResultPageChange(page)"
                class="w-8 h-8 rounded-lg text-sm font-medium transition-colors"
                :class="currentResultPage === page ? 'bg-blue-500 text-white' : 'text-gray-600 hover:text-gray-900'"
              >
                {{ page }}
              </button>
            </div>
            
            <button 
              @click="handleResultPageChange(currentResultPage + 1)"
              :disabled="currentResultPage === totalResultPages"
              class="p-2 rounded-lg transition-colors"
              :class="currentResultPage === totalResultPages ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              Next
            </button>
          </div>
        </div>
        
        <!-- Algorithm Results -->
        <div v-if="activeTab === 'algorithm'" class="space-y-12">
          <!-- Advanced Filters -->
          <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-2xl p-8 border border-white/20">
            <div class="flex justify-between items-center mb-6">
              <h3 class="text-xl font-semibold bg-gradient-to-r from-gray-900 to-gray-600 bg-clip-text text-transparent">
                Filter Results
              </h3>
              <button 
                v-if="getFilterCount > 0"
                @click="clearFilters"
                class="px-4 py-2 text-sm rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 transition-all duration-300 flex items-center gap-2"
              >
                <span>Clear filters</span>
                <span class="w-5 h-5 rounded-full bg-blue-500 text-white flex items-center justify-center text-xs">
                  {{ getFilterCount }}
                </span>
              </button>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
              <!-- Categories Filter -->
              <div class="space-y-3">
                <button 
                  @click="expandedFilters.categories = !expandedFilters.categories"
                  class="w-full flex items-center justify-between p-4 rounded-xl border border-gray-100 hover:border-blue-200 bg-white hover:bg-blue-50/50 transition-all duration-300"
                  :class="{ 'border-blue-200 bg-blue-50/50': expandedFilters.categories }"
                >
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                      <Filter class="w-4 h-4 text-blue-500" />
                    </div>
                    <span class="font-medium">Categories</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-gray-500">
                      {{ algorithmFilters.categories.length || 'Any' }}
                    </span>
                    <svg 
                      class="w-4 h-4 transition-transform duration-300"
                      :class="{ 'rotate-180': expandedFilters.categories }"
                      viewBox="0 0 24 24" 
                      fill="none" 
                      stroke="currentColor"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </button>
                
                <div v-if="expandedFilters.categories" 
                     class="p-4 bg-white rounded-xl border border-gray-100 shadow-lg space-y-2 animate-slideDown">
                  <label v-for="category in algorithmCategories.filter(c => c.id !== 'all')" 
                :key="category.id"
                         class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer group transition-colors">
                    <div class="relative flex items-center justify-center">
                      <input 
                        type="checkbox"
                        :checked="algorithmFilters.categories.includes(category.id)"
                        @change="handleFilterChange('categories', category.id)"
                        class="peer sr-only"
                      />
                      <div class="w-5 h-5 border-2 border-gray-300 rounded-md peer-checked:border-blue-500 peer-checked:bg-blue-500 transition-all">
                        <svg 
                          class="w-4 h-4 text-white scale-0 peer-checked:scale-100 transition-transform"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                    </div>
                    <span class="text-gray-700 group-hover:text-gray-900">{{ category.name }}</span>
                  </label>
                </div>
              </div>

              <!-- Years Filter -->
              <div class="space-y-3">
                <button 
                  @click="expandedFilters.years = !expandedFilters.years"
                  class="w-full flex items-center justify-between p-4 rounded-xl border border-gray-100 hover:border-purple-200 bg-white hover:bg-purple-50/50 transition-all duration-300"
                  :class="{ 'border-purple-200 bg-purple-50/50': expandedFilters.years }"
                >
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-purple-100 flex items-center justify-center">
                      <Calendar class="w-4 h-4 text-purple-500" />
                    </div>
                    <span class="font-medium">Years</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-gray-500">
                      {{ algorithmFilters.years.length || 'Any' }}
                </span>
                    <svg 
                      class="w-4 h-4 transition-transform duration-300"
                      :class="{ 'rotate-180': expandedFilters.years }"
                      viewBox="0 0 24 24" 
                      fill="none" 
                      stroke="currentColor"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
              </button>
                
                <div v-if="expandedFilters.years" 
                     class="p-4 bg-white rounded-xl border border-gray-100 shadow-lg max-h-[300px] overflow-y-auto custom-scrollbar animate-slideDown">
                  <div class="space-y-2">
                    <label v-for="year in availableYears" 
                           :key="year"
                           class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer group transition-colors">
                      <div class="relative flex items-center justify-center">
                        <input 
                          type="checkbox"
                          :checked="algorithmFilters.years.includes(year)"
                          @change="handleFilterChange('years', year)"
                          class="peer sr-only"
                        />
                        <div class="w-5 h-5 border-2 border-gray-300 rounded-md peer-checked:border-purple-500 peer-checked:bg-purple-500 transition-all">
                          <svg 
                            class="w-4 h-4 text-white scale-0 peer-checked:scale-100 transition-transform"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                          >
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                          </svg>
                        </div>
                      </div>
                      <span class="text-gray-700 group-hover:text-gray-900">{{ year }}</span>
                    </label>
                  </div>
            </div>
          </div>

              <!-- Sources Filter -->
              <div class="space-y-3">
                <button 
                  @click="expandedFilters.sources = !expandedFilters.sources"
                  class="w-full flex items-center justify-between p-4 rounded-xl border border-gray-100 hover:border-green-200 bg-white hover:bg-green-50/50 transition-all duration-300"
                  :class="{ 'border-green-200 bg-green-50/50': expandedFilters.sources }"
                >
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center">
                      <BookOpen class="w-4 h-4 text-green-500" />
                    </div>
                    <span class="font-medium">Sources</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-gray-500">
                      {{ algorithmFilters.sources.length || 'Any' }}
                    </span>
                    <svg 
                      class="w-4 h-4 transition-transform duration-300"
                      :class="{ 'rotate-180': expandedFilters.sources }"
                      viewBox="0 0 24 24" 
                      fill="none" 
                      stroke="currentColor"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </button>
                
                <div v-if="expandedFilters.sources" 
                     class="p-4 bg-white rounded-xl border border-gray-100 shadow-lg max-h-[300px] overflow-y-auto custom-scrollbar animate-slideDown">
                  <div class="space-y-2">
                    <label v-for="source in availableSources" 
                           :key="source"
                           class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer group transition-colors">
                      <div class="relative flex items-center justify-center">
                        <input 
                          type="checkbox"
                          :checked="algorithmFilters.sources.includes(source)"
                          @change="handleFilterChange('sources', source)"
                          class="peer sr-only"
                        />
                        <div class="w-5 h-5 border-2 border-gray-300 rounded-md peer-checked:border-green-500 peer-checked:bg-green-500 transition-all">
                          <svg 
                            class="w-4 h-4 text-white scale-0 peer-checked:scale-100 transition-transform"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                          >
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                          </svg>
                        </div>
                      </div>
                      <span class="text-gray-700 group-hover:text-gray-900">{{ source }}</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Results List -->
          <template v-if="filteredAlgorithmResults.length > 0">
          <div v-for="(algorithmResult, index) in paginatedAlgorithmResults" 
               :key="index"
               class="grid md:grid-cols-2 gap-8 rounded-2xl overflow-hidden shadow-2xl" 
               style="background: linear-gradient(145deg, #1a1f35 0%, #2a3045 100%)">
            <!-- Left Panel -->
            <div class="p-8 text-white flex flex-col justify-between h-full">
              <div class="space-y-4">
                <div class="flex items-center gap-3">
                  <h3 class="text-2xl font-semibold">{{ algorithmResult.name }}</h3>
                  <!-- 类别标签组 -->
                  <div class="flex items-center gap-2 max-w-[300px] overflow-hidden">
                    <template v-if="algorithmResult.categories && algorithmResult.categories.length > 0">
                      <span v-for="(category, index) in algorithmResult.categories" 
                            :key="index"
                            class="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors"
                            :style="{
                              backgroundColor: `${categoryColors[category]?.bg || categoryColors['Undefined'].bg}`,
                              color: `${categoryColors[category]?.text || categoryColors['Undefined'].text}`
                            }">
                        {{ category }}
                      </span>
                      <span v-if="algorithmResult.categories.length > 2" 
                            class="px-2 py-1 text-xs font-medium text-zinc-400">
                        +{{ algorithmResult.categories.length - 2 }}
                      </span>
                    </template>
                    <span v-else 
                          class="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap bg-[#86868B]/20 text-[#86868B]">
                      Undefined
                  </span>
                </div>
                </div>
                
                <p class="text-zinc-300 line-clamp-2">{{ algorithmResult.description }}</p>
                
                <!-- 标签组 -->
                <div class="flex flex-wrap gap-2 mt-3">
                  <!-- 年份标签 -->
                  <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium bg-zinc-700/50 text-zinc-300">
                    <Calendar class="w-3.5 h-3.5" />
                    {{ algorithmResult.year }}
                  </span>
                  
                  <!-- 出处标签 -->
                  <div class="flex flex-wrap gap-2">
                    <span v-for="source in algorithmResult.sources" 
                          :key="source"
                          class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium"
                          :style="{
                            backgroundColor: `${getSourceColor(source)}20`,
                            color: getSourceColor(source)
                          }">
                      <BookOpen class="w-3.5 h-3.5" />
                      {{ source }}
                    </span>
                  </div>
                </div>
                
                <p class="text-sm text-zinc-300 line-clamp-2">
                  Click the following button to explore detailed performance metrics across different datasets.
                </p>
              </div>
              
              <div class="mt-6">
                <button 
                  @click="router.push(`/algorithm/${algorithmResult.name}`)"
                  class="inline-block bg-[#336FFF] hover:bg-[#2952cc] text-white px-6 py-2 rounded-lg transition-all duration-300 hover:shadow-lg"
                >
                  View Details
                </button>
              </div>
            </div>

            <!-- Right Panel -->
            <div class="bg-[#2a3045]/30 p-8 backdrop-blur-sm border-l border-white/10">
              <div class="h-full flex items-center">
                <!-- Performance Chart -->
                <div class="w-full h-64">
                  <v-chart 
                    v-if="algorithmPerformanceData[algorithmResult.name] && 
                          algorithmMetrics[algorithmResult.name]"
                    :option="getChartOption(algorithmResult.name)"
                    autoresize
                    class="!bg-transparent"
                  />
                  <div v-else 
                       class="w-full h-full flex items-center justify-center text-sm text-zinc-400">
                    Loading performance data...
                  </div>
                </div>
              </div>
            </div>
          </div>
          </template>

          <!-- Empty State with Filter Info -->
          <div v-else class="text-center py-12">
            <Network class="w-16 h-16 mx-auto mb-4 text-gray-400" />
            <p class="text-gray-500">No algorithms found with the selected filters.</p>
            <button 
              @click="clearFilters"
              class="mt-4 text-blue-500 hover:text-blue-600"
            >
              Clear all filters
            </button>
          </div>

          <!-- Results Pagination -->
          <div v-if="filteredAlgorithmResults.length > algorithmsPerPage" 
               class="flex justify-center items-center gap-2 pt-8">
            <button 
              @click="handleAlgorithmPageChange(currentAlgorithmPage - 1)"
              :disabled="currentAlgorithmPage === 1"
              class="p-2 rounded-lg transition-colors"
              :class="currentAlgorithmPage === 1 ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              Previous
            </button>
            
            <div class="flex items-center gap-2">
              <button 
                v-for="page in totalAlgorithmPages" 
                :key="page"
                @click="handleAlgorithmPageChange(page)"
                class="w-8 h-8 rounded-lg text-sm font-medium transition-colors"
                :class="currentAlgorithmPage === page ? 'bg-blue-500 text-white' : 'text-gray-600 hover:text-gray-900'"
              >
                {{ page }}
              </button>
            </div>
            
            <button 
              @click="handleAlgorithmPageChange(currentAlgorithmPage + 1)"
              :disabled="currentAlgorithmPage === totalAlgorithmPages"
              class="p-2 rounded-lg transition-colors"
              :class="currentAlgorithmPage === totalAlgorithmPages ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 将 UploadDatasetDialog 移到这里，作为根元素的直接子元素 -->
    <UploadDatasetDialog
      v-if="showUploadDatasetDialog"
      :show="showUploadDatasetDialog"
      @close="showUploadDatasetDialog = false"
      @submit="handleUploadSuccess"
    />

    <!-- Upload Dialog -->
    <UploadAlgorithmDialog
      :show="showUploadDialog"
      @close="showUploadDialog = false"
      @submit="fetchAlgorithms(selectedCategory)"
    />
  </div>
</template>

<style scoped>
.shadow-lg {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

/* 隐藏滚动条但保持滚动功能 */
.scrollbar-hide {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}
.scrollbar-hide::-webkit-scrollbar {
  display: none;  /* Chrome, Safari and Opera */
}

/* 标签动画效果 */
.source-tag {
  transition: all 0.3s ease;
  white-space: nowrap;
}
.source-tag:hover {
  transform: scale(1.05);
  filter: brightness(1.1);
}

/* 添加鼠标悬停时的滚动效果 */
.scrollbar-hide:hover {
  animation: scroll 20s linear infinite;
}

@keyframes scroll {
  0% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(calc(-100% + 100vw));
  }
  100% {
    transform: translateX(0);
  }
}

/* 当鼠标悬停时暂停滚动 */
.scrollbar-hide:hover {
  animation-play-state: paused;
}

.tag-scroll-container {
  width: 100%;
  overflow: hidden;
  position: relative;
}

.tag-scroll-content {
  display: inline-flex;
  gap: 0.5rem;
  padding: 0.25rem 0;
  white-space: nowrap;
}

/* 使用CSS变量的滚动动画 */
.scroll-animation {
  animation: smoothScroll 15s linear infinite;
}

@keyframes smoothScroll {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(var(--scroll-width, -100%));
  }
}

/* 当鼠标悬停时暂停滚动 */
.scroll-animation:hover {
  animation-play-state: paused;
}

/* 标签样式 */
.source-tag {
  transition: all 0.3s ease;
}

.source-tag:hover {
  transform: scale(1.05);
  filter: brightness(1.1);
}

/* 隐藏滚动条 */
.tag-scroll-container::-webkit-scrollbar {
  display: none;
}
.tag-scroll-container {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 添加滚动条样式 */
.overflow-y-auto {
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}

.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

/* 添加动画 */
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-slideDown {
  animation: slideDown 0.3s ease-out;
}

/* 自定义滚动条 */
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.3);
}
</style> 