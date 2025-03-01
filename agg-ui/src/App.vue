<template>
  <div class="min-h-screen flex flex-col">
    <!-- Navigation -->
    <nav class="bg-black px-4 py-3 sticky top-0 z-50 backdrop-blur-sm bg-black/95 border-b border-zinc-800">
      <div class="container mx-auto flex items-center gap-8">
        <router-link 
          to="/" 
          class="flex items-center gap-2 text-white"
          @click="scrollToSection('home')"
        >
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-600 to-indigo-700 flex items-center justify-center shadow-lg">
            <span class="text-white text-lg">🖥️</span>
          </div>
          <span class="font-semibold text-lg tracking-tight text-white">AGG-Benchmarks</span>
        </router-link>
        <div class="flex gap-8">
          <router-link 
            to="/" 
            class="text-gray-400 hover:text-white transition-colors relative py-1"
            :class="{ 'text-white after:absoldiute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600': activeNav === 'home' }"
            @click="scrollToSection('home')"
          >
            Home
          </router-link>
          <router-link 
            to="/#datasets" 
            class="text-gray-400 hover:text-white transition-colors relative py-1"
            :class="{ 'text-white after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600': activeNav === 'datasets' }"
            @click="scrollToSection('datasets')"
          >
            Datasets
          </router-link>
          <router-link 
            to="/#algorithms" 
            class="text-gray-400 hover:text-white transition-colors relative py-1"
            :class="{ 'text-white after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600': activeNav === 'algorithms' }"
            @click="scrollToSection('algorithms')"
          >
            Algorithms
          </router-link>
          <router-link 
            to="/#contact" 
            class="text-gray-400 hover:text-white transition-colors relative py-1"
            :class="{ 'text-white after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600': activeNav === 'contact' }"
            @click="scrollToSection('contact')"
          >
            Contact
          </router-link>
          <div class="relative" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
            <button class="text-gray-400 hover:text-white transition-colors relative py-1">
              Upload
            </button>
            
            <!-- Dropdown Menu -->
            <div v-show="showUploadMenu" 
                 class="absolute top-full left-0 mt-1 w-56 bg-black/95 rounded-lg shadow-lg border border-zinc-800 py-2"
                 @mouseenter="handleMouseEnter"
                 @mouseleave="handleMouseLeave">
              <button
                @click="handleUploadDataset"
                class="flex items-center gap-3 px-6 py-2.5 text-gray-400 hover:text-white hover:bg-zinc-800/50"
              >
                <Database class="w-4 h-4" />
                <span>Upload Dataset</span>
              </button>
              <button
                @click="handleUploadAlgorithm"
                class="flex items-center gap-3 px-6 py-2.5 text-gray-400 hover:text-white hover:bg-zinc-800/50"
              >
                <Network class="w-4 h-4" />
                <span>Upload Algorithm</span>
              </button>
              <button
                @click="handleUploadResults"
                class="flex items-center gap-3 px-6 py-2.5 text-gray-400 hover:text-white hover:bg-zinc-800/50"
              >
                <FileText class="w-4 h-4" />
                <span>Upload Results</span>
              </button>
              <button
                @click="handleUploadMetrics"
                class="flex items-center gap-3 px-6 py-2.5 text-gray-400 hover:text-white hover:bg-zinc-800/50"
              >
                <LineChart class="w-4 h-4" />
                <span>Upload Metrics</span>
              </button>
            </div>
          </div>
        </div>
        <div class="ml-auto flex items-center gap-4">
          <a href="https://github.com" target="_blank" class="text-gray-400 hover:text-white transition-colors">
            <Github class="w-5 h-5" />
          </a>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="flex-grow">
      <router-view></router-view>
    </main>

    <!-- Contact Us Section -->
    <section id="contact" class="py-12 px-4 bg-black">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-8 flex items-center justify-center gap-2 text-white">
          <Mail class="w-5 h-5" />
          Contact Us
        </h2>
        <div>
          <p class="text-zinc-300 leading-relaxed">
            AGG-Benchmarks has been developed by Martin Aumueller (
            <a href="mailto:maua@itu.dk" class="text-[#336FFF] hover:underline">maua@itu.dk</a>
            ), Erik Bernhardsson (
            <a href="mailto:mail@erikbern.com" class="text-[#336FFF] hover:underline">mail@erikbern.com</a>
            ), and Alec Faithfull (
            <a href="mailto:alef@itu.dk" class="text-[#336FFF] hover:underline">alef@itu.dk</a>
            ). Please use 
            <a href="https://github.com" target="_blank" class="text-[#336FFF] hover:underline inline-flex items-center gap-1">
              Github <Github class="w-4 h-4" />
            </a>
            to submit your implementation or improvements.
          </p>
        </div>
      </div>
    </section>

    <!-- Upload Dataset Dialog -->
    <UploadDatasetDialog 
      :show="showUploadDatasetDialog"
      @close="showUploadDatasetDialog = false"
      @submit="handleUploadSuccess"
    />
    <UploadAlgorithmDialog
      :show="showUploadAlgorithmDialog"
      @close="showUploadAlgorithmDialog = false"
      @submit="handleAlgorithmUploadSuccess"
    />
    <UploadResultsDialog
      :show="showUploadResultsDialog"
      @close="showUploadResultsDialog = false"
    />
    <UploadMetricsDialog
      :show="showUploadMetricsDialog"
      @close="showUploadMetricsDialog = false"
    />
  </div>
</template>

<script setup>
import { ClipboardIcon, Database, Network, BarChart, FileText, Mail, Github, LineChart } from 'lucide-vue-next'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import UploadDatasetDialog from './components/UploadDatasetDialog.vue'
import UploadAlgorithmDialog from './components/UploadAlgorithmDialog.vue'
import UploadResultsDialog from './components/UploadResultsDialog.vue'
import UploadMetricsDialog from './components/UploadMetricsDialog.vue'

const router = useRouter()
const showUploadMenu = ref(false)
const showUploadDatasetDialog = ref(false)
const showUploadAlgorithmDialog = ref(false)
const showUploadResultsDialog = ref(false)
const showUploadMetricsDialog = ref(false)

// 添加延迟关闭菜单的处理
let closeTimeout = null

const handleMouseEnter = () => {
  if (closeTimeout) {
    clearTimeout(closeTimeout)
  }
  showUploadMenu.value = true
}

const handleMouseLeave = () => {
  closeTimeout = setTimeout(() => {
    showUploadMenu.value = false
  }, 200) // 200ms 的延迟
}

// 计算当前激活的导航项
const activeNav = computed(() => {
  if (router.currentRoute.value.hash) {
    return router.currentRoute.value.hash.slice(1) // 移除 '#' 前缀
  }
  return router.currentRoute.value.path === '/' ? 'home' : ''
})

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

const activeTab = ref('dataset') // 'dataset' or 'algorithm'

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

const scrollToSection = (sectionId) => {
  // 如果是 home，直接滚动到顶部
  if (sectionId === 'home') {
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
    return
  }
  // 如果不在首页，先跳转到首页
  if (router.currentRoute.value.path !== '/') {
    router.push('/')
    // 等待路由切换完成后再滚动
    setTimeout(() => {
      const element = document.getElementById(sectionId)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
      }
    }, 100)
  } else {
    // 如果在首页，直接滚动
    const element = document.getElementById(sectionId)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' })
    }
  }
}

const handleUploadDataset = () => {
  showUploadMenu.value = false
  showUploadDatasetDialog.value = true
}

const handleUploadAlgorithm = () => {
  showUploadMenu.value = false
  showUploadAlgorithmDialog.value = true
}

const handleUploadResults = () => {
  showUploadMenu.value = false
  showUploadResultsDialog.value = true
}

const handleUploadMetrics = () => {
  showUploadMenu.value = false
  showUploadMetricsDialog.value = true
}

const handleUploadSuccess = async () => {
  console.log('handleUploadSuccess called')
  try {
    // 触发一个全局事件，通知需要刷新数据的组件
    window.dispatchEvent(new CustomEvent('dataset-updated'))
    console.log('Dataset updated event dispatched')
  } catch (error) {
    console.error('Error handling upload success:', error)
  }
}

// 处理算法上传成功
const handleAlgorithmUploadSuccess = () => {
  // 触发自定义事件通知其他组件更新数据
  window.dispatchEvent(new CustomEvent('algorithm-updated'))
}
</script>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}

.shadow-lg {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>
