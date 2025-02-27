<script setup>
import { ref, computed } from 'vue'
import { LineChart, X, Upload as UploadIcon } from 'lucide-vue-next'
import MarkdownIt from 'markdown-it'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'submit'])

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
  typographer: true
})

const formData = ref({
  name: '',
  description: '',
  type: '',
  file: null
})

// 计算属性：将 Markdown 转换为 HTML
const renderedDescription = computed(() => {
  return md.render(formData.value.description)
})

const fileName = ref('')
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    fileName.value = file.name
    formData.value.file = file
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
              <LineChart class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-medium text-gray-900">Upload Metrics</h2>
            <p class="text-sm text-gray-500">Add new performance metrics to the benchmark</p>
          </div>

          <!-- Form -->
          <div class="space-y-8">
            <!-- Metric Name -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">Metric Name *</label>
              <input 
                v-model="formData.name"
                type="text"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                placeholder="e.g. Precision@K, NDCG"
              />
            </div>

            <!-- Description -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">Description *</label>
              <div class="flex flex-col gap-4">
                <!-- Markdown Editor -->
                <textarea
                  v-model="formData.description"
                  rows="6"
                  class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400 resize-none font-mono text-sm custom-scrollbar"
                  placeholder="Describe how this metric is calculated using Markdown...
Example:
# Metric Name
This metric calculates...

## Formula
$MAP = \frac{1}{|Q|} \sum_{q \in Q} AP(q)$

## Parameters
- Q: query set
- AP: average precision"
                ></textarea>
                
                <!-- Preview -->
                <div class="bg-black/5 rounded-xl p-4">
                  <h3 class="text-sm font-medium text-gray-700 mb-2">Preview</h3>
                  <div 
                    class="prose prose-sm max-w-none overflow-y-auto custom-scrollbar"
                    style="max-height: 200px"
                    v-html="renderedDescription"
                  ></div>
                </div>
              </div>
              <p class="text-xs text-gray-500 mt-1">
                Use Markdown syntax for formatting. Supports math formulas using LaTeX syntax ($...$).
              </p>
            </div>

            <!-- Implementation File -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">Implementation File *</label>
              <input 
                type="file"
                @change="handleFileChange"
                class="hidden"
                id="metric-upload"
              />
              <label 
                for="metric-upload"
                class="flex items-center justify-center gap-2 px-4 py-3 bg-black/5 rounded-xl cursor-pointer hover:bg-black/10 transition-colors"
              >
                <UploadIcon class="w-5 h-5 text-gray-500" />
                <span class="text-gray-600">{{ fileName || 'Upload Metric Implementation' }}</span>
              </label>
              <p class="text-xs text-gray-500">Upload the implementation code for this metric</p>
            </div>
          </div>

          <!-- Submit Button -->
          <button 
            @click="handleSubmit"
            class="w-full py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium shadow-lg hover:shadow-xl"
          >
            Upload Metric
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* Dialog Transition */
.dialog-enter-active,
.dialog-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* Custom Scrollbar Styles */
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

/* Hide scrollbar when not scrolling */
.custom-scrollbar:not(:hover)::-webkit-scrollbar-thumb {
  background-color: transparent;
}

/* Markdown Preview Styles */
:deep(.prose) {
  color: #374151;
}

:deep(.prose pre) {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 0.5rem;
  padding: 1rem;
}

:deep(.prose code) {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 0.25rem;
  padding: 0.125rem 0.25rem;
}

:deep(.prose img) {
  border-radius: 0.5rem;
}
</style> 