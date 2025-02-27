<script setup>
import { ref, computed } from 'vue'
import { FileText, X, Upload as UploadIcon } from 'lucide-vue-next'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'submit'])

const formData = ref({
  algorithm: '',
  dataset: '',
  metrics: {
    mAP: {
      enabled: false,
      value: ''
    },
    NDCG: {
      enabled: false,
      value: ''
    }
  },
  metricsFile: null,
  file: null
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

const fileName = ref('')
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    fileName.value = file.name
    formData.value.file = file
  }
}

const metricsFileName = ref('')
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
        // 验证JSON格式
        if (data.mAP || data.NDCG) {
          formData.value.metrics.mAP.value = data.mAP || ''
          formData.value.metrics.mAP.enabled = !!data.mAP
          formData.value.metrics.NDCG.value = data.NDCG || ''
          formData.value.metrics.NDCG.enabled = !!data.NDCG
        } else {
          alert('Invalid metrics format')
        }
      } catch (error) {
        alert('Invalid JSON file')
      }
    }
    reader.readAsText(file)
    formData.value.metricsFile = file
  }
}

const handleSubmit = () => {
  emit('submit', formData.value)
  emit('close')
}
</script>

<template>
  <Transition name="dialog">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- Backdrop with blur -->
      <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="emit('close')"></div>
      
      <!-- Dialog -->
      <div class="relative w-[600px] max-h-[80vh] overflow-y-auto bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl custom-scrollbar">
        <!-- Close Button -->
        <button 
          @click="emit('close')"
          class="absolute top-4 right-4 p-2 rounded-full hover:bg-black/5 transition-colors"
        >
          <X class="w-5 h-5 text-gray-500" />
        </button>

        <!-- Dialog Content -->
        <div class="p-8 space-y-8">
          <!-- Header -->
          <div class="text-center space-y-2">
            <div class="w-12 h-12 bg-blue-600 rounded-2xl flex items-center justify-center mx-auto">
              <FileText class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-medium text-gray-900">Upload Results</h2>
            <p class="text-sm text-gray-500">Add performance results to the benchmark collection</p>
          </div>

          <!-- Form -->
          <div class="space-y-8">
            <!-- Algorithm Selection -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">Algorithm *</label>
              <select 
                v-model="formData.algorithm"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 appearance-none cursor-pointer"
              >
                <option value="" disabled selected>Select Algorithm</option>
                <option v-for="algo in algorithms" :key="algo" :value="algo">
                  {{ algo }}
                </option>
              </select>
            </div>

            <!-- Dataset Selection -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">Dataset *</label>
              <select 
                v-model="formData.dataset"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 appearance-none cursor-pointer"
              >
                <option value="" disabled selected>Select Dataset</option>
                <option v-for="dataset in datasets" :key="dataset" :value="dataset">
                  {{ dataset }}
                </option>
              </select>
            </div>

            <!-- Metrics -->
            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <h3 class="text-sm font-medium text-gray-700">Performance Metrics *</h3>
                <div class="flex items-center gap-2">
                  <input 
                    type="file"
                    accept="application/json"
                    @change="handleMetricsFileChange"
                    class="hidden"
                    id="metrics-upload"
                  />
                  <label 
                    for="metrics-upload"
                    class="text-xs text-blue-600 hover:text-blue-700 cursor-pointer flex items-center gap-1"
                  >
                    <UploadIcon class="w-4 h-4" />
                    Import JSON
                  </label>
                </div>
              </div>

              <!-- JSON Format Guide -->
              <div class="bg-gray-50/50 rounded-lg p-4 space-y-2 text-sm">
                <p class="text-gray-600 font-medium">JSON Format Example:</p>
                <pre class="text-gray-500 text-xs bg-black/5 p-2 rounded-md">
{
  "mAP": 0.85,
  "NDCG": 0.92
}</pre>
              </div>

              <!-- Manual Input -->
              <div class="space-y-4">
                <!-- mAP Metric -->
                <div class="space-y-3">
                  <label class="flex items-center gap-2">
                    <input 
                      type="checkbox"
                      v-model="formData.metrics.mAP.enabled"
                      class="rounded text-blue-600 focus:ring-blue-500"
                    />
                    <span class="text-sm text-gray-600">Mean Average Precision (mAP)</span>
                  </label>
                  <input 
                    v-if="formData.metrics.mAP.enabled"
                    v-model="formData.metrics.mAP.value"
                    type="number"
                    step="0.01"
                    min="0"
                    max="1"
                    class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                    placeholder="Enter mAP value (0-1)"
                  />
                </div>

                <!-- NDCG Metric -->
                <div class="space-y-3">
                  <label class="flex items-center gap-2">
                    <input 
                      type="checkbox"
                      v-model="formData.metrics.NDCG.enabled"
                      class="rounded text-blue-600 focus:ring-blue-500"
                    />
                    <span class="text-sm text-gray-600">Normalized Discounted Cumulative Gain (NDCG)</span>
                  </label>
                  <input 
                    v-if="formData.metrics.NDCG.enabled"
                    v-model="formData.metrics.NDCG.value"
                    type="number"
                    step="0.01"
                    min="0"
                    max="1"
                    class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                    placeholder="Enter NDCG value (0-1)"
                  />
                </div>
              </div>
            </div>

            <!-- Results File -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">Results File (Optional)</label>
              <input 
                type="file"
                @change="handleFileChange"
                class="hidden"
                id="results-upload"
              />
              <label 
                for="results-upload"
                class="flex items-center justify-center gap-2 px-4 py-3 bg-black/5 rounded-xl cursor-pointer hover:bg-black/10 transition-colors"
              >
                <UploadIcon class="w-5 h-5 text-gray-500" />
                <span class="text-gray-600">{{ fileName || 'Upload Detailed Results' }}</span>
              </label>
              <p class="text-xs text-gray-500">Upload detailed performance results file (optional)</p>
            </div>
          </div>

          <!-- Submit Button -->
          <button 
            @click="handleSubmit"
            class="w-full py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium shadow-lg hover:shadow-xl"
          >
            Upload Results
          </button>
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
</style> 