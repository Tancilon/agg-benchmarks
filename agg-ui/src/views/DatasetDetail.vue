<script setup>
import { ref } from 'vue'
import { Download } from 'lucide-vue-next'
import { useRoute } from 'vue-router'

const route = useRoute()
const datasetId = route.params.id
const activeMetric = ref('mAP') // 'mAP' or 'NDCG'

const metrics = {
  mAP: {
    title: "Metric: mAP",
    subtitle: "Recall-Queries per second (1/s) tradeoff - up and to the right is better",
    xAxis: "Recall",
    yAxis: "mAP@k"
  },
  NDCG: {
    title: "Metric: NDCG",
    subtitle: "Recall-Build time (s) tradeoff - down and to the right is better",
    xAxis: "Recall",
    yAxis: "NDCG@k"
  }
}

// 根据路由参数获取数据集信息
const datasetInfo = {
  name: datasetId,
  description: "Over 32,000 labeled bounding boxes, 500K distractor images.",
  details: "Click the following button, you can find an overview of all algorithm's performance on this dataset."
}
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Hero Section -->
    <section class="bg-black px-4 py-16">
      <div class="container mx-auto">
        <div class="flex justify-between items-start">
          <div class="space-y-4">
            <h1 class="text-4xl font-bold text-white">{{ datasetInfo.name }}</h1>
            <p class="text-zinc-400">{{ datasetInfo.description }}</p>
            <p class="text-sm text-zinc-400">{{ datasetInfo.details }}</p>
          </div>
          <button class="bg-[#336FFF] hover:bg-[#2952cc] text-white px-6 py-3 rounded-lg transition-all duration-300 hover:shadow-lg flex items-center gap-2">
            <Download class="w-4 h-4" />
            Download Dataset
          </button>
        </div>
        <!-- Pagination Dots -->
        <div class="flex gap-2 mt-8">
          <div v-for="i in 3" :key="i" 
               class="w-2 h-2 rounded-full"
               :class="i === 1 ? 'bg-white' : 'bg-zinc-600'"></div>
        </div>
      </div>
    </section>

    <!-- Metrics Section -->
    <section class="py-16 px-4">
      <div class="container mx-auto">
        <!-- Metric Tabs -->
        <div class="flex gap-8 mb-12">
          <button 
            @click="activeMetric = 'mAP'"
            :class="[
              'text-lg font-medium pb-2 border-b-2',
              activeMetric === 'mAP' 
                ? 'border-[#336FFF] text-[#336FFF]' 
                : 'border-transparent text-gray-500 hover:text-gray-700'
            ]"
          >
            mAP
          </button>
          <button 
            @click="activeMetric = 'NDCG'"
            :class="[
              'text-lg font-medium pb-2 border-b-2',
              activeMetric === 'NDCG' 
                ? 'border-[#336FFF] text-[#336FFF]' 
                : 'border-transparent text-gray-500 hover:text-gray-700'
            ]"
          >
            NDCG
          </button>
        </div>

        <!-- Metric Content -->
        <div class="space-y-8">
          <div>
            <h2 class="text-2xl font-semibold mb-2">{{ metrics[activeMetric].title }}</h2>
            <p class="text-zinc-600">{{ metrics[activeMetric].subtitle }}</p>
          </div>

          <!-- Performance Graph -->
          <div class="bg-white rounded-xl border border-gray-200 p-8 h-[600px] relative">
            <!-- 这里是图表占位符，后续可以集成 ECharts 等图表库 -->
            <div class="absolute bottom-8 right-8 flex items-center gap-4">
              <button class="bg-[#336FFF] text-white px-4 py-2 rounded-lg hover:bg-[#2952cc] transition-colors flex items-center gap-2">
                <Download class="w-4 h-4" />
                Download Results
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template> 