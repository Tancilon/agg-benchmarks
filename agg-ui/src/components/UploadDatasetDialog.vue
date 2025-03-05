<script setup>
import { ref, computed, onMounted } from 'vue'
import { Database, X, Upload as UploadIcon } from 'lucide-vue-next'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'submit'])
// 添加反馈状态
const feedbackState = ref({
  show: false,
  type: '', // 'success' 或 'error'
  title: '',
  message: ''
})
const formData = ref({
  name: '',
  category: '',
  customCategory: '',
  description: '',
  file: null
})

const fileName = ref('')
const categories = ref([])
const showSuccessDialog = ref(false)
const formErrors = ref({})

// 获取现有类别
const fetchCategories = async () => {
  try {
    const response = await fetch('/api/datasets/categories')
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    const data = await response.json()
    // 确保 'Other' 是最后一个选项，并且数组中没有重复值
    categories.value = [...new Set([...(data || []), 'Other'])]
  } catch (error) {
    console.error('Error fetching categories:', error)
    // 出错时至少保留 'Other' 选项
    categories.value = ['Other']
  }
}

// 在组件挂载时获取类别
onMounted(() => {
  fetchCategories()
})

const finalCategory = computed(() => {
  return formData.value.category === 'Other' 
    ? formData.value.customCategory 
    : formData.value.category
})

// 表单验证
const validateForm = () => {
  const errors = {}
  
  if (!formData.value.name.trim()) {
    errors.name = 'Dataset name is required'
  }
  
  if (!formData.value.category) {
    errors.category = 'Category is required'
  } else if (formData.value.category === 'Other' && !formData.value.customCategory.trim()) {
    errors.customCategory = 'Custom category is required'
  }
  
  if (!formData.value.description.trim()) {
    errors.description = 'Description is required'
  }
  
  if (!formData.value.file) {
    errors.file = 'Dataset file is required'
  }
  
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

const handleSubmit = async () => {
  try {
    // 验证表单
    if (!validateForm()) {
      return
    }

    // 先检查数据集名称是否已存在
    const checkResponse = await fetch(`/api/datasets/check-name?name=${encodeURIComponent(formData.value.name)}`);
    if (!checkResponse.ok) {
      const errorText = await checkResponse.text();
      if (checkResponse.status === 409) {
        // 显示名称重复的错误反馈
        feedbackState.value = {
          show: true,
          type: 'error',
          title: 'Upload Failed',
          message: 'A dataset with this name already exists.'
        }
        return;
      }
      throw new Error(errorText || 'Failed to check dataset name');
    }

    const form = new FormData()
    const datasetData = {
      name: formData.value.name,
      category: finalCategory.value,
      description: formData.value.description
    }

    form.append('file', formData.value.file)
    form.append('dataset', new Blob([JSON.stringify(datasetData)], { type: 'application/json' }))

    const response = await fetch('/api/datasets', {
      method: 'POST',
      body: form
    })

    if (!response.ok) {
      const error = await response.text()
      // 检查是否是名称重复错误
      if (error.includes("already exists")) {
        feedbackState.value = {
          show: true,
          type: 'error',
          title: 'Upload Failed',
          message: 'A dataset with this name already exists.'
        }
        return;
      }
      throw new Error(error || 'Upload failed')
    }

    await response.json()

    // 重置表单和错误
    Object.assign(formData.value, {
      name: '',
      category: '',
      customCategory: '',
      description: '',
      file: null
    })
    fileName.value = ''
    formErrors.value = {}

    // 显示成功提示
    showSuccessDialog.value = true
    
    // 触发数据刷新事件
    emit('submit')
    
    // 延迟关闭对话框和成功提示
    setTimeout(() => {
      showSuccessDialog.value = false
      setTimeout(() => {
        emit('close')
      }, 300)
    }, 1500)

  } catch (error) {
    console.error('Error uploading dataset:', error)
    // 使用反馈对话框显示错误
    feedbackState.value = {
      show: true,
      type: 'error',
      title: 'Upload Failed',
      message: error.message || 'Failed to upload dataset. Please try again.'
    }
  }
}

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    fileName.value = file.name
    formData.value.file = file
    formErrors.value.file = null
  }
}
</script>

<template>
  <div>
    <Transition name="dialog">
      <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center">
        <!-- Backdrop with blur -->
        <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="emit('close')"></div>
        
        <!-- Dialog -->
        <div class="relative w-[1000px] max-h-[85vh] overflow-y-auto bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl custom-scrollbar">
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
            <form @submit.prevent="handleSubmit" class="space-y-6">
              <!-- Name -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700">
                  Dataset Name <span class="text-red-500">*</span>
                </label>
                <input 
                  type="text" 
                  v-model="formData.name"
                  class="w-full px-4 py-2 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  :class="{ 'border-red-500': formErrors.name }"
                  placeholder="Enter dataset name"
                />
                <span v-if="formErrors.name" class="text-sm text-red-500">{{ formErrors.name }}</span>
              </div>

              <!-- Category -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700">
                  Category <span class="text-red-500">*</span>
                </label>
                <select 
                  v-model="formData.category"
                  class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 appearance-none cursor-pointer"
                  :class="{ 'border-red-500': formErrors.category }"
                >
                  <option value="" disabled selected>Select Category</option>
                  <option v-for="category in categories" :key="category" :value="category">
                    {{ category }}
                  </option>
                </select>
                <span v-if="formErrors.category" class="text-sm text-red-500">{{ formErrors.category }}</span>

                <!-- Custom Category Input -->
                <input 
                  v-if="formData.category === 'Other'"
                  v-model="formData.customCategory"
                  type="text"
                  class="mt-2 w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                  :class="{ 'border-red-500': formErrors.customCategory }"
                  placeholder="Enter custom category"
                />
                <span v-if="formErrors.customCategory" class="text-sm text-red-500">{{ formErrors.customCategory }}</span>
              </div>

              <!-- Description -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700">
                  Description <span class="text-red-500">*</span>
                </label>
                <textarea 
                  v-model="formData.description"
                  rows="4"
                  class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400 resize-none"
                  :class="{ 'border-red-500': formErrors.description }"
                  placeholder="Describe your dataset..."
                ></textarea>
                <span v-if="formErrors.description" class="text-sm text-red-500">{{ formErrors.description }}</span>
              </div>

              <!-- File Upload -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700">
                  Dataset File <span class="text-red-500">*</span>
                </label>
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
                    :class="{ 'border-red-500': formErrors.file }"
                  >
                    <UploadIcon class="w-5 h-5 text-gray-500" />
                    <span class="text-gray-600">{{ fileName || 'Choose Dataset File' }}</span>
                  </label>
                  <span v-if="formErrors.file" class="text-sm text-red-500">{{ formErrors.file }}</span>
                </div>
              </div>

              <button 
                type="submit"
                class="w-full py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium"
              >
                Upload Dataset
              </button>
            </form>
          </div>
        </div>

        <!-- 成功提示对话框 -->
        <div v-if="showSuccessDialog" 
             class="absolute top-4 right-4 bg-green-100 border border-green-400 text-green-700 px-6 py-4 rounded-lg shadow-lg flex items-center gap-3">
          <svg class="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
          </svg>
          <div>
            <h4 class="font-medium">Success!</h4>
            <p class="text-sm">Dataset uploaded successfully</p>
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
  </div>
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

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 优化滚动条样式 */
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
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

/* 当不hover时隐藏滚动条 */
.custom-scrollbar:not(:hover)::-webkit-scrollbar-thumb {
  background-color: transparent;
}

/* 优化水平滚动条 */
.custom-scrollbar::-webkit-scrollbar-track:horizontal {
  margin: 0 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:horizontal {
  border-radius: 3px;
}

/* 优化滚动条交叉处 */
.custom-scrollbar::-webkit-scrollbar-corner {
  background: transparent;
}

/* 添加平滑滚动 */
.custom-scrollbar {
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

/* 优化滚动容器 */
.custom-scrollbar {
  overflow: overlay;
  padding-right: 6px; /* 防止滚动条占用内容空间 */
  margin-right: -6px; /* 补偿padding */
}

/* 确保内容不会被滚动条遮挡 */
.custom-scrollbar > * {
  margin-right: 6px;
}
</style> 