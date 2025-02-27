<script setup>
import { ref, computed } from 'vue'
import { Database, X, Upload as UploadIcon } from 'lucide-vue-next'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'submit'])

const formData = ref({
  name: '',
  category: '',
  customCategory: '',
  description: '',
  file: null
})

const categories = [
  'Person Re-identification',
  'Recommendation Systems',
  'Bioinformatics',
  'Global Cities',
  'Other'
]

const finalCategory = computed(() => {
  return formData.value.category === 'Other' 
    ? formData.value.customCategory 
    : formData.value.category
})

const handleSubmit = () => {
  const submitData = {
    ...formData.value,
    category: finalCategory.value
  }
  emit('submit', submitData)
  emit('close')
}

const fileName = ref('')
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    fileName.value = file.name
    formData.value.file = file
  }
}
</script>

<template>
  <Transition name="dialog">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- Backdrop with blur -->
      <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="emit('close')"></div>
      
      <!-- Dialog -->
      <div class="relative w-[600px] bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl overflow-hidden">
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
              <Database class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-medium text-gray-900">Upload Dataset</h2>
            <p class="text-sm text-gray-500">Add a new dataset to the benchmark collection</p>
          </div>

          <!-- Form -->
          <div class="space-y-6">
            <!-- Dataset Name -->
            <div class="space-y-2">
              <input 
                v-model="formData.name"
                type="text"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                placeholder="Dataset Name"
              />
            </div>

            <!-- Category -->
            <div class="space-y-2">
              <select 
                v-model="formData.category"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 appearance-none cursor-pointer"
              >
                <option value="" disabled selected>Select Category</option>
                <option v-for="category in categories" :key="category" :value="category">
                  {{ category }}
                </option>
              </select>

              <!-- Custom Category Input -->
              <input 
                v-if="formData.category === 'Other'"
                v-model="formData.customCategory"
                type="text"
                class="mt-2 w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                placeholder="Enter custom category"
              />
            </div>

            <!-- Description -->
            <div class="space-y-2">
              <textarea 
                v-model="formData.description"
                rows="4"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400 resize-none"
                placeholder="Describe your dataset..."
              ></textarea>
            </div>

            <!-- File Upload -->
            <div class="relative">
              <input 
                type="file"
                @change="handleFileChange"
                class="hidden"
                id="file-upload"
              />
              <label 
                for="file-upload"
                class="flex items-center justify-center gap-2 px-4 py-3 bg-black/5 rounded-xl cursor-pointer hover:bg-black/10 transition-colors"
              >
                <UploadIcon class="w-5 h-5 text-gray-500" />
                <span class="text-gray-600">{{ fileName || 'Choose Dataset File' }}</span>
              </label>
            </div>
          </div>

          <!-- Submit Button -->
          <button 
            @click="handleSubmit"
            class="w-full py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium"
          >
            Upload Dataset
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

/* Custom select arrow */
select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%236B7280'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-position: right 1rem center;
  background-repeat: no-repeat;
  background-size: 1.5em 1.5em;
  padding-right: 2.5rem;
}

/* Hide scrollbar but keep functionality */
textarea {
  scrollbar-width: none;
}
textarea::-webkit-scrollbar {
  display: none;
}

/* 添加自定义类别输入框的过渡动画 */
input[type="text"] {
  transition: all 0.3s ease;
}
</style> 