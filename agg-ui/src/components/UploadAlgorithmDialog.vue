<script setup>
import { ref, computed } from 'vue'
import { Network, X, Upload as UploadIcon, Plus, X as XIcon } from 'lucide-vue-next'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'submit'])

const formData = ref({
  name: '',
  categories: [],
  customCategories: [],
  description: '',
  file: null
})

const predefinedCategories = [
  'Unsupervised',
  'Supervised',
  'Semi-Supervised',
  'Other'
]

const newCustomCategory = ref('')

const addCustomCategory = () => {
  if (newCustomCategory.value.trim()) {
    formData.value.customCategories.push(newCustomCategory.value.trim())
    newCustomCategory.value = ''
  }
}

const removeCustomCategory = (index) => {
  formData.value.customCategories.splice(index, 1)
}

const finalCategories = computed(() => {
  const selectedCategories = formData.value.categories.filter(cat => cat !== 'Other')
  return [...new Set([...selectedCategories, ...formData.value.customCategories])]
})

const fileName = ref('')
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    fileName.value = file.name
    formData.value.file = file
  }
}

// 添加表单错误状态
const formErrors = ref({})

// 表单验证
const validateForm = () => {
  const errors = {}
  
  if (!formData.value.name.trim()) {
    errors.name = 'Algorithm name is required'
  }
  
  if (formData.value.categories.length === 0 && formData.value.customCategories.length === 0) {
    errors.categories = 'At least one category is required'
  }
  
  if (!formData.value.description.trim()) {
    errors.description = 'Description is required'
  }
  
  if (!formData.value.file) {
    errors.file = 'Implementation file is required'
  }
  
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

const handleSubmit = () => {
  if (!validateForm()) {
    return
  }

  const submitData = {
    ...formData.value,
    categories: finalCategories.value
  }
  emit('submit', submitData)
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
              <Network class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-medium text-gray-900">Upload Algorithm</h2>
            <p class="text-sm text-gray-500">Add a new algorithm to the benchmark collection</p>
          </div>

          <!-- Form -->
          <div class="space-y-8">
            <!-- Algorithm Name -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Algorithm Name <span class="text-red-500">*</span>
              </label>
              <input 
                v-model="formData.name"
                type="text"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                :class="{ 'border-red-500': formErrors.name }"
                placeholder="e.g. Comb*, CSRA"
              />
              <span v-if="formErrors.name" class="text-sm text-red-500">{{ formErrors.name }}</span>
            </div>
            
            <!-- Category -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Categories <span class="text-red-500">*</span>
              </label>
              
              <!-- 预定义类别多选 -->
              <div class="flex flex-wrap gap-2">
                <label 
                  v-for="category in predefinedCategories" 
                  :key="category"
                  class="inline-flex items-center px-4 py-2 rounded-xl cursor-pointer transition-all duration-200"
                  :class="[
                    formData.categories.includes(category)
                      ? 'bg-blue-500 text-white shadow-md hover:bg-blue-600'
                      : 'bg-black/5 text-gray-700 hover:bg-black/10'
                  ]"
                >
                  <input
                    type="checkbox"
                    :value="category"
                    v-model="formData.categories"
                    class="hidden"
                  />
                  <span class="flex items-center gap-2">
                    <span v-if="formData.categories.includes(category)" class="text-xs">✓</span>
                    {{ category }}
                  </span>
                </label>
              </div>
              <span v-if="formErrors.categories" class="text-sm text-red-500">{{ formErrors.categories }}</span>

              <!-- 自定义类别输入 -->
              <div class="mt-4 space-y-3">
                <div class="flex gap-2">
                  <input 
                    v-model="newCustomCategory"
                    type="text"
                    class="flex-1 px-4 py-2 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                    placeholder="Add custom category"
                    @keyup.enter="addCustomCategory"
                  />
                  <button 
                    @click="addCustomCategory"
                    class="px-4 py-2 bg-blue-500 text-white rounded-xl hover:bg-blue-600 transition-all duration-200 shadow-md hover:shadow-lg"
                    :disabled="!newCustomCategory.trim()"
                    :class="{ 'opacity-50 cursor-not-allowed': !newCustomCategory.trim() }"
                  >
                    <Plus class="w-5 h-5" />
                  </button>
                </div>

                <!-- 自定义类别标签 -->
                <div class="flex flex-wrap gap-2">
                  <div 
                    v-for="(category, index) in formData.customCategories" 
                    :key="index"
                    class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-500 text-white shadow-md transition-all duration-200"
                  >
                    {{ category }}
                    <button 
                      @click="removeCustomCategory(index)"
                      class="hover:bg-blue-600 rounded-full p-1 transition-colors"
                    >
                      <XIcon class="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- Description -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Description <span class="text-red-500">*</span>
              </label>
              <textarea 
                v-model="formData.description"
                rows="4"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400 resize-none"
                :class="{ 'border-red-500': formErrors.description }"
                placeholder="Describe your algorithm..."
              ></textarea>
              <span v-if="formErrors.description" class="text-sm text-red-500">{{ formErrors.description }}</span>
            </div>
            
            <!-- File Upload -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Implementation File <span class="text-red-500">*</span>
              </label>
              <input 
                type="file"
                @change="handleFileChange"
                class="hidden"
                id="algorithm-upload"
              />
              <label 
                for="algorithm-upload"
                class="flex items-center justify-center gap-2 px-4 py-3 bg-black/5 rounded-xl cursor-pointer hover:bg-black/10 transition-colors"
                :class="{ 'border-red-500': formErrors.file }"
              >
                <UploadIcon class="w-5 h-5 text-gray-500" />
                <span class="text-gray-600">{{ fileName || 'Upload Algorithm Implementation' }}</span>
              </label>
              <span v-if="formErrors.file" class="text-sm text-red-500">{{ formErrors.file }}</span>
            </div>
          </div>

          <!-- Submit Button -->
          <button 
            @click="handleSubmit"
            class="w-full py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium shadow-lg hover:shadow-xl"
          >
            Upload Algorithm
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

/* 自定义滚动条样式 */
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

/* 当不滚动时隐藏滚动条 */
.custom-scrollbar:not(:hover)::-webkit-scrollbar-thumb {
  background-color: transparent;
}

/* 保持文本区域的滚动条隐藏 */
textarea {
  scrollbar-width: none;
}
textarea::-webkit-scrollbar {
  display: none;
}

/* 添加这些新样式 */
.category-enter-active,
.category-leave-active {
  transition: all 0.3s ease;
}

.category-enter-from,
.category-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 确保选中状态的过渡效果平滑 */
label {
  position: relative;
  overflow: hidden;
}

label::before {
  content: '';
  position: absolute;
  inset: 0;
  background-color: currentColor;
  opacity: 0;
  transition: opacity 0.2s ease;
}

label:hover::before {
  opacity: 0.1;
}
</style> 