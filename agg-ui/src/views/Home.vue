<script setup>
import { ClipboardIcon, Database, Network, BarChart, FileText, Mail, Github } from 'lucide-vue-next'
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const datasets = [
  { name: "Market1501", category: "Field: Person Re-identification" },
  { name: "DukeMTMC-reID", category: "Field: Person Re-identification" },
  { name: "CUHK03 (detected)", category: "Field: Person Re-identification" },
  { name: "CUHK03 (labeled)", category: "Field: Person Re-identification" },
  { name: "MovieLens 1M", category: "Field: Recommendation Systems" },
  { name: "NRGLC", category: "Field: Bioinformatics" },
  { name: "WUR 2022", category: "Field: Global Cities" },
]

const algorithms = [
  { name: "Comb*", category: "Unsupervised" },
  { name: "CSRA", category: "Supervised" },
  { name: "Borda Count", category: "Unsupervised" },
  { name: "MC1-4", category: "Unsupervised" },
  { name: "RRF", category: "Unsupervised" },
  { name: "AggRankDE", category: "Supervised" },
  { name: "SSRA", category: "Semi-Supervised" },
  { name: "IRANK", category: "Unsupervised" },
]

const activeTab = ref('dataset')
const selectedCategory = ref('all')
const selectedDatasetCategory = ref('all')

const allDatasetResults = [
  {
    name: "Market1501",
    description: "Over 32,000 labeled bounding boxes, 500K distractor images.",
    metrics: [
      { value: 210, color: '#336FFF' },
      { value: 180, color: '#D7BEFD' },
      { value: 150, color: '#FF69B4' },
      { value: 100, color: '#FF4444' },
      { value: 80, color: '#FF7F50' },
      { value: 120, color: '#FFD700' },
      { value: 190, color: '#4169E1' },
    ]
  },
  {
    name: "DukeMTMC-reID",
    description: "A large-scale person re-identification dataset with 36,411 images of 1,812 identities.",
    metrics: [
      { value: 190, color: '#336FFF' },
      { value: 160, color: '#D7BEFD' },
      { value: 140, color: '#FF69B4' },
      { value: 110, color: '#FF4444' },
      { value: 90, color: '#FF7F50' },
      { value: 130, color: '#FFD700' },
      { value: 170, color: '#4169E1' },
    ]
  },
  {
    name: "CUHK03 (detected)",
    description: "1,467 identities from 5 different pairs of camera views, automatically detected.",
    metrics: [
      { value: 180, color: '#336FFF' },
      { value: 150, color: '#D7BEFD' },
      { value: 130, color: '#FF69B4' },
      { value: 90, color: '#FF4444' },
      { value: 70, color: '#FF7F50' },
      { value: 110, color: '#FFD700' },
      { value: 160, color: '#4169E1' },
    ]
  },
  {
    name: "CUHK03 (labeled)",
    description: "1,467 identities from 5 different pairs of camera views, manually labeled.",
    metrics: [
      { value: 200, color: '#336FFF' },
      { value: 170, color: '#D7BEFD' },
      { value: 140, color: '#FF69B4' },
      { value: 95, color: '#FF4444' },
      { value: 75, color: '#FF7F50' },
      { value: 115, color: '#FFD700' },
      { value: 180, color: '#4169E1' },
    ]
  },
  {
    name: "MovieLens 1M",
    description: "1 million ratings from 6,000 users on 4,000 movies, including user demographics.",
    metrics: [
      { value: 170, color: '#336FFF' },
      { value: 140, color: '#D7BEFD' },
      { value: 120, color: '#FF69B4' },
      { value: 85, color: '#FF4444' },
      { value: 65, color: '#FF7F50' },
      { value: 105, color: '#FFD700' },
      { value: 150, color: '#4169E1' },
    ]
  },
  {
    name: "NRGLC",
    description: "Novel research gene-level classification dataset for bioinformatics analysis.",
    metrics: [
      { value: 160, color: '#336FFF' },
      { value: 130, color: '#D7BEFD' },
      { value: 110, color: '#FF69B4' },
      { value: 80, color: '#FF4444' },
      { value: 60, color: '#FF7F50' },
      { value: 100, color: '#FFD700' },
      { value: 140, color: '#4169E1' },
    ]
  },
  {
    name: "WUR 2022",
    description: "World University Rankings dataset with comprehensive metrics and indicators.",
    metrics: [
      { value: 150, color: '#336FFF' },
      { value: 120, color: '#D7BEFD' },
      { value: 100, color: '#FF69B4' },
      { value: 75, color: '#FF4444' },
      { value: 55, color: '#FF7F50' },
      { value: 95, color: '#FFD700' },
      { value: 130, color: '#4169E1' },
    ]
  }
]

// 模拟算法结果数据
const allAlgorithmResults = [
  {
    name: "Comb*",
    category: "Unsupervised",
    description: "A novel unsupervised rank aggregation algorithm that combines multiple ranking lists effectively.",
    metrics: [
      { value: 200, color: '#336FFF' },  // Market1501
      { value: 185, color: '#D7BEFD' },  // DukeMTMC
      { value: 170, color: '#FF69B4' },  // CUHK03-detected
      { value: 175, color: '#FF4444' },  // CUHK03-labeled
      { value: 160, color: '#FF7F50' },  // MovieLens
    ]
  },
  {
    name: "CSRA",
    category: "Supervised",
    description: "Context-aware Supervised Rank Aggregation method that learns from labeled ranking data.",
    metrics: [
      { value: 210, color: '#336FFF' },
      { value: 195, color: '#D7BEFD' },
      { value: 180, color: '#FF69B4' },
      { value: 185, color: '#FF4444' },
      { value: 170, color: '#FF7F50' },
    ]
  },
  {
    name: "Borda Count",
    category: "Unsupervised",
    description: "A classical voting-based rank aggregation method that assigns scores based on positions.",
    metrics: [
      { value: 180, color: '#336FFF' },
      { value: 165, color: '#D7BEFD' },
      { value: 150, color: '#FF69B4' },
      { value: 155, color: '#FF4444' },
      { value: 140, color: '#FF7F50' },
    ]
  },
  {
    name: "SSRA",
    category: "Semi-Supervised",
    description: "Semi-Supervised Rank Aggregation leveraging both labeled and unlabeled ranking data.",
    metrics: [
      { value: 195, color: '#336FFF' },
      { value: 180, color: '#D7BEFD' },
      { value: 165, color: '#FF69B4' },
      { value: 170, color: '#FF4444' },
      { value: 155, color: '#FF7F50' },
    ]
  }
]

// 新增：获取所有算法类别
const algorithmCategories = [
  { id: 'all', name: 'All' },
  { id: 'Unsupervised', name: 'Unsupervised' },
  { id: 'Supervised', name: 'Supervised' },
  { id: 'Semi-Supervised', name: 'Semi-Supervised' }
]

// 新增：根据选中类别过滤算法结果
const filteredAlgorithmResults = computed(() => {
  if (selectedCategory.value === 'all') {
    return allAlgorithmResults
  }
  return allAlgorithmResults.filter(algo => algo.category === selectedCategory.value)
})

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)

// 获取所有数据集类别
const datasetCategories = [
  { id: 'all', name: 'All' },
  { id: 'Person Re-identification', name: 'Person Re-identification' },
  { id: 'Recommendation Systems', name: 'Recommendation Systems' },
  { id: 'Bioinformatics', name: 'Bioinformatics' },
  { id: 'Global Cities', name: 'Global Cities' }
]

// 根据选中类别过滤数据集结果
const filteredDatasetResults = computed(() => {
  if (selectedDatasetCategory.value === 'all') {
    return allDatasetResults
  }
  return allDatasetResults.filter(dataset => {
    const category = dataset.name === "Market1501" || 
                     dataset.name === "DukeMTMC-reID" || 
                     dataset.name === "CUHK03 (detected)" || 
                     dataset.name === "CUHK03 (labeled)" 
      ? "Person Re-identification"
      : dataset.name === "MovieLens 1M" 
        ? "Recommendation Systems"
        : dataset.name === "NRGLC"
          ? "Bioinformatics"
          : "Global Cities"
    return category === selectedDatasetCategory.value
  })
})

// 修改数据集分页计算
const paginatedDatasetResults = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredDatasetResults.value.slice(start, end)
})

// 计算算法分页
const paginatedAlgorithmResults = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredAlgorithmResults.value.slice(start, end)
})

// 修改总页数计算
const totalPages = computed(() => {
  const total = activeTab.value === 'dataset' 
    ? filteredDatasetResults.value.length 
    : filteredAlgorithmResults.value.length
  return Math.ceil(total / pageSize.value)
})

// 切换页码
const handlePageChange = (page) => {
  currentPage.value = page
}

// 监听标签切换和类别选择，重置页码
watch([activeTab, selectedCategory, selectedDatasetCategory], () => {
  currentPage.value = 1
})

// Datasets 分页相关
const currentDatasetPage = ref(1)
const datasetsPerPage = ref(8)

// Algorithms 分页相关
const currentAlgorithmPage = ref(1)
const algorithmsPerPage = ref(8)

// 计算分页后的数据集
const paginatedDatasets = computed(() => {
  const start = (currentDatasetPage.value - 1) * datasetsPerPage.value
  const end = start + datasetsPerPage.value
  return datasets.slice(start, end)
})

// 计算分页后的算法
const paginatedAlgorithms = computed(() => {
  const start = (currentAlgorithmPage.value - 1) * algorithmsPerPage.value
  const end = start + algorithmsPerPage.value
  return algorithms.slice(start, end)
})

// 计算数据集总页数
const totalDatasetPages = computed(() => {
  return Math.ceil(datasets.length / datasetsPerPage.value)
})

// 计算算法总页数
const totalAlgorithmPages = computed(() => {
  return Math.ceil(algorithms.length / algorithmsPerPage.value)
})

// 切换数据集页码
const handleDatasetPageChange = (page) => {
  currentDatasetPage.value = page
}

// 切换算法页码
const handleAlgorithmPageChange = (page) => {
  currentAlgorithmPage.value = page
}
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
              ANN-Benchmarks is a benchmarking environment for approximate nearest neighbor algorithms search. 
              This website contains the current benchmarking results. Please visit 
              <a href="http://github.com/erikbern/ann-benchmarks/" class="text-blue-600 hover:underline">
                http://github.com/erikbern/ann-benchmarks/
              </a>
              to get an overview over evaluated data sets and algorithms. Make a pull request on Github to add your
              own code or improvements to the benchmarking system.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Datasets Section -->
    <section id="datasets" class="py-16 px-4" style="background: linear-gradient(180deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 100%)">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-12 flex items-center justify-center gap-2">
          <Database class="w-5 h-5" />
          Datasets
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div 
            v-for="(dataset, index) in paginatedDatasets" 
            :key="index" 
            class="p-6 rounded-xl shadow-lg border border-gray-100 text-center transform transition-all duration-300 hover:scale-105 bg-white hover:shadow-xl cursor-pointer"
            @click="router.push(`/dataset/${dataset.name}`)"
          >
            <Database class="w-12 h-12 mx-auto text-[#336FFF] mb-4" />
            <h3 class="font-medium mb-2">{{ dataset.name }}</h3>
            <p class="text-sm text-[#336FFF]">{{ dataset.category }}</p>
          </div>
        </div>
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
    </section>

    <!-- Algorithms Section -->
    <section id="algorithms" class="py-16 px-4" style="background: linear-gradient(0deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 100%)">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-12 flex items-center justify-center gap-2">
          <Network class="w-5 h-5" />
          Algorithms
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div v-for="(algorithm, index) in paginatedAlgorithms" :key="index" 
               class="p-6 rounded-xl shadow-lg border border-gray-100 text-center transform transition-all duration-300 hover:scale-105 bg-white hover:shadow-xl cursor-pointer"
               @click="router.push(`/algorithm/${algorithm.name}`)">
            <Network class="w-12 h-12 mx-auto mb-4" :class="{
              'text-[#336FFF]': algorithm.category === 'Unsupervised',
              'text-[#D7BEFD]': algorithm.category === 'Supervised',
              'text-[#B6A494]': algorithm.category === 'Semi-Supervised'
            }" />
            <h3 class="font-medium text-lg mb-2">{{ algorithm.name }}</h3>
            <p class="text-sm text-zinc-600" :class="{
              'text-[#336FFF]': algorithm.category === 'Unsupervised',
              'text-[#D7BEFD]': algorithm.category === 'Supervised',
              'text-[#B6A494]': algorithm.category === 'Semi-Supervised'
            }">{{ algorithm.category }}</p>
          </div>
        </div>
        <div class="flex justify-center mt-8 gap-2">
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

    <!-- Result Visualizations Section -->
    <section class="py-16 px-4" style="background: linear-gradient(180deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 50%, rgba(240,244,248,1) 100%)">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-12 flex items-center justify-center gap-2">
          <FileText class="w-5 h-5" />
          Result Visualizations
        </h2>
        
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
          <!-- Dataset Category Filter -->
          <div class="flex justify-center mb-8">
            <div class="inline-flex p-1 rounded-xl bg-gray-100">
              <button 
                v-for="category in datasetCategories"
                :key="category.id"
                @click="selectedDatasetCategory = category.id"
                class="px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300"
                :class="[
                  selectedDatasetCategory === category.id 
                    ? 'bg-white text-gray-900 shadow-sm' 
                    : 'text-gray-600 hover:text-gray-900'
                ]"
              >
                {{ category.name }}
                <span 
                  v-if="category.id !== 'all'"
                  class="ml-2 px-2 py-0.5 rounded-full text-xs bg-blue-50 text-blue-500"
                >
                  {{ allDatasetResults.filter(dataset => {
                    const datasetCategory = dataset.name === "Market1501" || 
                                          dataset.name === "DukeMTMC-reID" || 
                                          dataset.name === "CUHK03 (detected)" || 
                                          dataset.name === "CUHK03 (labeled)" 
                      ? "Person Re-identification"
                      : dataset.name === "MovieLens 1M" 
                        ? "Recommendation Systems"
                        : dataset.name === "NRGLC"
                          ? "Bioinformatics"
                          : "Global Cities"
                    return datasetCategory === category.id
                  }).length }}
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
                  <span 
                    class="px-3 py-1 rounded-full text-sm"
                    :class="{
                      'bg-[#336FFF]/20 text-[#336FFF]': datasetResult.name === 'Market1501' || 
                                                        datasetResult.name === 'DukeMTMC-reID' || 
                                                        datasetResult.name === 'CUHK03 (detected)' || 
                                                        datasetResult.name === 'CUHK03 (labeled)',
                      'bg-[#D7BEFD]/20 text-[#D7BEFD]': datasetResult.name === 'MovieLens 1M',
                      'bg-[#FF69B4]/20 text-[#FF69B4]': datasetResult.name === 'NRGLC',
                      'bg-[#22C55E]/20 text-[#22C55E]': datasetResult.name === 'WUR 2022'
                    }"
                  >
                    {{ datasetResult.name === "Market1501" || 
                       datasetResult.name === "DukeMTMC-reID" || 
                       datasetResult.name === "CUHK03 (detected)" || 
                       datasetResult.name === "CUHK03 (labeled)" 
                      ? "Person Re-identification"
                      : datasetResult.name === "MovieLens 1M" 
                        ? "Recommendation Systems"
                        : datasetResult.name === "NRGLC"
                          ? "Bioinformatics"
                          : "Global Cities" }}
                  </span>
                </div>
                <p class="text-zinc-300 line-clamp-2">{{ datasetResult.description }}</p>
                <p class="text-sm text-zinc-300 line-clamp-2">
                  Click the following button, you can find an overview of all algorithm's performance on this dataset.
                </p>
              </div>
              <div class="mt-6">
                <router-link 
                  :to="`/dataset/${datasetResult.name}`"
                  class="inline-block bg-[#336FFF] hover:bg-[#2952cc] text-white px-6 py-2 rounded-lg transition-all duration-300 hover:shadow-lg"
                >
                  View Details
                </router-link>
              </div>
            </div>

            <!-- Right Panel -->
            <div class="bg-[#2a3045]/30 p-8 backdrop-blur-sm border-l border-white/10">
              <div class="h-full flex items-center">
                <!-- Bar Chart -->
                <div class="w-full h-64 flex items-end justify-around gap-4">
                  <div v-for="(metric, metricIndex) in datasetResult.metrics" 
                       :key="metricIndex" 
                       class="w-8 rounded-t-lg transition-all duration-500 shadow-lg hover:brightness-110"
                       :style="{
                         height: `${metric.value}px`,
                         backgroundColor: metric.color
                       }"
                       title="Performance Metric"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-if="filteredDatasetResults.length === 0" 
               class="text-center py-12 text-gray-500">
            <Database class="w-16 h-16 mx-auto mb-4 opacity-50" />
            <p>No datasets found in this category.</p>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="flex justify-center items-center gap-2 pt-8">
            <button 
              @click="handlePageChange(currentPage - 1)"
              :disabled="currentPage === 1"
              class="p-2 rounded-lg transition-colors"
              :class="currentPage === 1 ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            
            <div class="flex items-center gap-2">
              <button 
                v-for="page in totalPages" 
                :key="page"
                @click="handlePageChange(page)"
                class="w-8 h-8 rounded-lg text-sm font-medium transition-colors"
                :class="currentPage === page 
                  ? 'bg-blue-500 text-white' 
                  : 'text-gray-600 hover:text-gray-900'"
              >
                {{ page }}
              </button>
            </div>
            
            <button 
              @click="handlePageChange(currentPage + 1)"
              :disabled="currentPage === totalPages"
              class="p-2 rounded-lg transition-colors"
              :class="currentPage === totalPages ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
        
        <!-- Algorithm Results -->
        <div v-if="activeTab === 'algorithm'" class="space-y-12">
          <!-- Category Filter -->
          <div class="flex justify-center mb-8">
            <div class="inline-flex p-1 rounded-xl bg-gray-100">
              <button 
                v-for="category in algorithmCategories"
                :key="category.id"
                @click="selectedCategory = category.id"
                class="px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300"
                :class="[
                  selectedCategory === category.id 
                    ? 'bg-white text-gray-900 shadow-sm' 
                    : 'text-gray-600 hover:text-gray-900'
                ]"
              >
                {{ category.name }}
                <span 
                  v-if="category.id !== 'all'"
                  class="ml-2 px-2 py-0.5 rounded-full text-xs"
                  :class="{
                    'bg-[#336FFF]/10 text-[#336FFF]': category.id === 'Unsupervised',
                    'bg-[#D7BEFD]/10 text-[#D7BEFD]': category.id === 'Supervised',
                    'bg-[#B6A494]/10 text-[#B6A494]': category.id === 'Semi-Supervised'
                  }"
                >
                  {{ allAlgorithmResults.filter(algo => algo.category === category.id).length }}
                </span>
              </button>
            </div>
          </div>

          <div v-for="(algorithmResult, index) in paginatedAlgorithmResults" 
               :key="index"
               class="grid md:grid-cols-2 gap-8 rounded-2xl overflow-hidden shadow-2xl" 
               style="background: linear-gradient(145deg, #1a1f35 0%, #2a3045 100%)">
            <!-- Left Panel -->
            <div class="p-8 text-white flex flex-col justify-between h-full">
              <div class="space-y-4">
                <div class="flex items-center gap-3">
                  <h3 class="text-2xl font-semibold">{{ algorithmResult.name }}</h3>
                  <span class="px-3 py-1 rounded-full text-sm" 
                        :class="{
                          'bg-[#336FFF]/20 text-[#336FFF]': algorithmResult.category === 'Unsupervised',
                          'bg-[#D7BEFD]/20 text-[#D7BEFD]': algorithmResult.category === 'Supervised',
                          'bg-[#B6A494]/20 text-[#B6A494]': algorithmResult.category === 'Semi-Supervised'
                        }">
                    {{ algorithmResult.category }}
                  </span>
                </div>
                <p class="text-zinc-300 line-clamp-2">{{ algorithmResult.description }}</p>
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
                <!-- Bar Chart -->
                <div class="w-full h-64 flex items-end justify-around gap-4">
                  <div v-for="(metric, metricIndex) in algorithmResult.metrics" 
                       :key="metricIndex" 
                       class="w-8 rounded-t-lg transition-all duration-500 shadow-lg hover:brightness-110"
                       :style="{
                         height: `${metric.value}px`,
                         backgroundColor: metric.color
                       }"
                       title="Performance Metric"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-if="filteredAlgorithmResults.length === 0" 
               class="text-center py-12 text-gray-500">
            <Network class="w-16 h-16 mx-auto mb-4 opacity-50" />
            <p>No algorithms found in this category.</p>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="flex justify-center items-center gap-2 pt-8">
            <button 
              @click="handlePageChange(currentPage - 1)"
              :disabled="currentPage === 1"
              class="p-2 rounded-lg transition-colors"
              :class="currentPage === 1 ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            
            <div class="flex items-center gap-2">
              <button 
                v-for="page in totalPages" 
                :key="page"
                @click="handlePageChange(page)"
                class="w-8 h-8 rounded-lg text-sm font-medium transition-colors"
                :class="currentPage === page 
                  ? 'bg-blue-500 text-white' 
                  : 'text-gray-600 hover:text-gray-900'"
              >
                {{ page }}
              </button>
            </div>
            
            <button 
              @click="handlePageChange(currentPage + 1)"
              :disabled="currentPage === totalPages"
              class="p-2 rounded-lg transition-colors"
              :class="currentPage === totalPages ? 'text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:text-gray-900'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.shadow-lg {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style> 