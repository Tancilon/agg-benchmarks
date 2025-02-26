<script setup>
import { ClipboardIcon, Database, Network, BarChart, FileText, Mail, Github } from 'lucide-vue-next'
import { ref } from 'vue'

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
    <section class="py-16 px-4">
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
    <section class="py-16 px-4" style="background: linear-gradient(180deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 100%)">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-12 flex items-center justify-center gap-2">
          <Database class="w-5 h-5" />
          Datasets
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div v-for="(dataset, index) in datasets" :key="index" 
               class="p-6 rounded-xl shadow-lg border border-gray-100 text-center transform transition-all duration-300 hover:scale-105 bg-white hover:shadow-xl">
            <Database class="w-12 h-12 mx-auto text-[#336FFF] mb-4" />
            <h3 class="font-medium mb-2">{{ dataset.name }}</h3>
            <p class="text-sm text-[#336FFF]">{{ dataset.category }}</p>
          </div>
        </div>
        <div class="flex justify-center mt-8 gap-2">
          <div class="w-2 h-2 rounded-full bg-gray-300"></div>
          <div class="w-2 h-2 rounded-full bg-blue-500"></div>
          <div class="w-2 h-2 rounded-full bg-gray-300"></div>
        </div>
      </div>
    </section>

    <!-- Algorithms Section -->
    <section class="py-16 px-4" style="background: linear-gradient(0deg, rgba(240,244,248,1) 0%, rgba(255,255,255,1) 100%)">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-12 flex items-center justify-center gap-2">
          <Network class="w-5 h-5" />
          Algorithms
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div v-for="(algorithm, index) in algorithms" :key="index" 
               class="p-6 rounded-xl shadow-lg border border-gray-100 text-center transform transition-all duration-300 hover:scale-105 bg-white hover:shadow-xl">
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
          <div class="w-2 h-2 rounded-full bg-gray-300"></div>
          <div class="w-2 h-2 rounded-full bg-blue-500"></div>
          <div class="w-2 h-2 rounded-full bg-gray-300"></div>
        </div>
      </div>
    </section>

    <!-- Result Visualizations Section -->
    <section class="py-16 px-4 bg-white">
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
          <div v-for="(datasetResult, index) in allDatasetResults" 
               :key="index"
               class="grid md:grid-cols-2 gap-8 rounded-2xl overflow-hidden shadow-2xl" 
               style="background: linear-gradient(145deg, #1a1f35 0%, #2a3045 100%)">
            <!-- Left Panel -->
            <div class="p-8 text-white">
              <h3 class="text-2xl font-semibold mb-4">{{ datasetResult.name }}</h3>
              <p class="text-zinc-300 mb-6">{{ datasetResult.description }}</p>
              <p class="text-sm text-zinc-300 mb-4">
                Click the following button, you can find an overview of all algorithm's performance on this dataset.
              </p>
              <router-link 
                :to="`/dataset/${datasetResult.name}`"
                class="inline-block bg-[#336FFF] hover:bg-[#2952cc] text-white px-6 py-2 rounded-lg transition-all duration-300 hover:shadow-lg"
              >
                View Details
              </router-link>
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