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
  year: '',
  sources: [],
  customSources: [],
  description: '',
  paperUrl: '',
  file: null,
  bibFile: null
})

const predefinedCategories = [
  'Unsupervised',
  'Supervised',
  'Semi-Supervised'
]

const predefinedSources = [
  'TPAMI',
  'IJCAI',
  'AAAI',
  'ICCV',
  'CVPR',
  'ECCV',
  'NeurIPS'
]

const newCustomCategory = ref('')
const newCustomSource = ref('')

const addCustomCategory = () => {
  if (newCustomCategory.value.trim()) {
    formData.value.customCategories.push(newCustomCategory.value.trim())
    newCustomCategory.value = ''
  }
}

const removeCustomCategory = (index) => {
  formData.value.customCategories.splice(index, 1)
}

const addCustomSource = () => {
  if (newCustomSource.value.trim()) {
    formData.value.customSources.push(newCustomSource.value.trim())
    newCustomSource.value = ''
  }
}

const removeCustomSource = (index) => {
  formData.value.customSources.splice(index, 1)
}

const finalCategories = computed(() => {
  const selectedCategories = formData.value.categories.filter(cat => cat !== 'Other')
  return [...new Set([...selectedCategories, ...formData.value.customCategories])]
})

const finalSources = computed(() => {
  return [...new Set([...formData.value.sources, ...formData.value.customSources])]
})

const fileName = ref('')
const bibFileName = ref('')
const showSuccessDialog = ref(false)
const formErrors = ref({})

const handleBibFileChange = async (e) => {
  const file = e.target.files[0]
  if (file) {
    console.log('Selected bib file:', file.name, file.size);
    
    // 验证文件类型和大小
    if (!file.name.toLowerCase().endsWith('.bib')) {
      formErrors.value.bibFile = 'Only .bib files are allowed'
      bibFileName.value = ''
      formData.value.bibFile = null
      e.target.value = ''
      return
    }
    
    if (file.size === 0) {
      formErrors.value.bibFile = 'File is empty'
      bibFileName.value = ''
      formData.value.bibFile = null
      e.target.value = ''
      return
    }
    
    if (file.size > 5 * 1024 * 1024) {
      formErrors.value.bibFile = 'File size should not exceed 5MB'
      bibFileName.value = ''
      formData.value.bibFile = null
      e.target.value = ''
      return
    }
    
    // 读取文件内容以验证
    try {
      const content = await file.text();
      if (!content.trim()) {
        formErrors.value.bibFile = 'File is empty'
        bibFileName.value = ''
        formData.value.bibFile = null
        e.target.value = ''
        return
      }
    } catch (error) {
      console.error('Error reading file:', error);
      formErrors.value.bibFile = 'Error reading file'
      bibFileName.value = ''
      formData.value.bibFile = null
      e.target.value = ''
      return
    }
    
    formErrors.value.bibFile = ''
    bibFileName.value = file.name
    formData.value.bibFile = file
    console.log('Bib file set:', formData.value.bibFile);
  }
}

const handleFileChange = async (e) => {
  const file = e.target.files[0]
  if (file) {
    console.log('Selected implementation file:', file.name, file.size);
    
    if (file.size === 0) {
      alert('Implementation file is empty');
      fileName.value = ''
      formData.value.file = null
      e.target.value = ''
      return
    }
    
    // 读取文件内容以验证
    try {
      const content = await file.text();
      if (!content.trim()) {
        alert('Implementation file is empty');
        fileName.value = ''
        formData.value.file = null
        e.target.value = ''
        return
      }
    } catch (error) {
      console.error('Error reading file:', error);
      alert('Error reading implementation file');
      fileName.value = ''
      formData.value.file = null
      e.target.value = ''
      return
    }
    
    fileName.value = file.name
    formData.value.file = file
    console.log('Implementation file set:', formData.value.file);
  }
}

// 表单验证
const validateForm = () => {
  const errors = {}
  
  if (!formData.value.name.trim()) {
    errors.name = 'Algorithm name is required'
  }
  
  if (formData.value.categories.length === 0 && formData.value.customCategories.length === 0) {
    errors.categories = 'At least one category is required'
  }
  
  if (!formData.value.year) {
    errors.year = 'Year is required'
  } else {
    const year = parseInt(formData.value.year)
    const currentYear = new Date().getFullYear()
    if (!/^\d{4}$/.test(formData.value.year)) {
      errors.year = 'Please enter a valid year (YYYY)'
    } else if (year < 0 || year > currentYear + 1) {
      errors.year = `Year must be between 0000 and ${currentYear + 1}`
    }
  }
  
  if (formData.value.sources.length === 0 && formData.value.customSources.length === 0) {
    errors.sources = 'At least one source is required'
  }
  
  if (!formData.value.description.trim()) {
    errors.description = 'Description is required'
  }
  
  if (!formData.value.bibFile) {
    errors.bibFile = 'Bibliography file is required'
  }
  
  if (!formData.value.paperUrl.trim()) {
    errors.paperUrl = 'Paper URL is required'
  } else if (!isValidUrl(formData.value.paperUrl)) {
    errors.paperUrl = 'Please enter a valid URL'
  }
  
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

// URL验证函数
const isValidUrl = (url) => {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

// 添加年份输入处理函数
const handleYearInput = (e) => {
  // 只允许输入数字
  const value = e.target.value.replace(/\D/g, '')
  formData.value.year = value
}

const handleSubmit = async () => {
  try {
    if (!validateForm()) return;

    const form = new FormData()
    
    // 添加算法基本信息
    const algorithmData = {
      name: formData.value.name,
      categories: finalCategories.value,
      year: parseInt(formData.value.year),
      sources: finalSources.value,
      description: formData.value.description,
      paperUrl: formData.value.paperUrl
    }

    // 将算法数据转换为Blob并添加到FormData
    form.append('algorithm', new Blob([JSON.stringify(algorithmData)], { 
      type: 'application/json' 
    }))

    // 添加BibTeX文件
    if (formData.value.bibFile) {
      console.log('Uploading bib file:', formData.value.bibFile.name, formData.value.bibFile.size);
      form.append('bibFile', formData.value.bibFile);
    } else {
      throw new Error('Bibliography file is required');
    }

    // 添加实现文件
    if (formData.value.file) {
      console.log('Uploading implementation file:', formData.value.file.name, formData.value.file.size);
      form.append('implementationFile', formData.value.file);
    }

    // 打印整个FormData内容
    for (let pair of form.entries()) {
      console.log(pair[0], pair[1]);
    }

    const response = await fetch('/api/algorithms', {
      method: 'POST',
      body: form
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('Server response:', errorText);
      throw new Error(errorText || 'Upload failed');
    }

    const result = await response.json();
    console.log('Upload successful:', result);

    // 重置表单和错误
    Object.assign(formData.value, {
      name: '',
      categories: [],
      customCategories: [],
      year: '',
      sources: [],
      customSources: [],
      description: '',
      paperUrl: '',
      file: null,
      bibFile: null
    })
    bibFileName.value = ''
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
    console.error('Error uploading algorithm:', error);
    alert(error.message || 'Failed to upload algorithm');
  }
}
</script>

<template>
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
            
            <!-- Year -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Year <span class="text-red-500">*</span>
              </label>
              <input 
                v-model="formData.year"
                type="text"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                :class="{ 'border-red-500': formErrors.year }"
                placeholder="e.g. 2024"
                maxlength="4"
                @input="handleYearInput"
              />
              <span v-if="formErrors.year" class="text-sm text-red-500">{{ formErrors.year }}</span>
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
            
            <!-- Sources -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Sources <span class="text-red-500">*</span>
              </label>
              
              <!-- 预定义出处多选 -->
              <div class="flex flex-wrap gap-2">
                <label 
                  v-for="source in predefinedSources" 
                  :key="source"
                  class="inline-flex items-center px-4 py-2 rounded-xl cursor-pointer transition-all duration-200"
                  :class="[
                    formData.sources.includes(source)
                      ? 'bg-blue-500 text-white shadow-md hover:bg-blue-600'
                      : 'bg-black/5 text-gray-700 hover:bg-black/10'
                  ]"
                >
                  <input
                    type="checkbox"
                    :value="source"
                    v-model="formData.sources"
                    class="hidden"
                  />
                  <span class="flex items-center gap-2">
                    <span v-if="formData.sources.includes(source)" class="text-xs">✓</span>
                    {{ source }}
                  </span>
                </label>
              </div>
              <span v-if="formErrors.sources" class="text-sm text-red-500">{{ formErrors.sources }}</span>

              <!-- 自定义出处输入 -->
              <div class="mt-4 space-y-3">
                <div class="flex gap-2">
                  <input 
                    v-model="newCustomSource"
                    type="text"
                    class="flex-1 px-4 py-2 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                    placeholder="Add custom source"
                    @keyup.enter="addCustomSource"
                  />
                  <button 
                    @click="addCustomSource"
                    class="px-4 py-2 bg-blue-500 text-white rounded-xl hover:bg-blue-600 transition-all duration-200 shadow-md hover:shadow-lg"
                    :disabled="!newCustomSource.trim()"
                    :class="{ 'opacity-50 cursor-not-allowed': !newCustomSource.trim() }"
                  >
                    <Plus class="w-5 h-5" />
                  </button>
                </div>

                <!-- 自定义出处标签 -->
                <div class="flex flex-wrap gap-2">
                  <div 
                    v-for="(source, index) in formData.customSources" 
                    :key="index"
                    class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-500 text-white shadow-md transition-all duration-200"
                  >
                    {{ source }}
                    <button 
                      @click="removeCustomSource(index)"
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
            
            <!-- Paper URL -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Paper URL <span class="text-red-500">*</span>
              </label>
              <input 
                v-model="formData.paperUrl"
                type="url"
                class="w-full px-4 py-3 bg-black/5 border-0 rounded-xl focus:ring-2 focus:ring-blue-500 placeholder:text-gray-400"
                :class="{ 'border-red-500': formErrors.paperUrl }"
                placeholder="e.g. https://doi.org/..."
              />
              <span v-if="formErrors.paperUrl" class="text-sm text-red-500">{{ formErrors.paperUrl }}</span>
            </div>
            
            <!-- File Upload -->
            <div class="space-y-8">
              <!-- Bibliography File Upload -->
              <div class="space-y-2">
                <label class="block text-sm font-medium text-gray-700">
                  Bibliography File <span class="text-red-500">*</span>
                </label>
                <input 
                  type="file"
                  accept=".bib"
                  @change="handleBibFileChange"
                  class="hidden"
                  id="bib-upload"
                />
                <label 
                  for="bib-upload"
                  class="flex items-center justify-center gap-2 px-4 py-3 bg-black/5 rounded-xl cursor-pointer hover:bg-black/10 transition-colors"
                  :class="{ 'border-red-500': formErrors.bibFile }"
                >
                  <UploadIcon class="w-5 h-5 text-gray-500" />
                  <span class="text-gray-600">{{ bibFileName || 'Upload Bibliography (.bib)' }}</span>
                </label>
                <span v-if="formErrors.bibFile" class="text-sm text-red-500">{{ formErrors.bibFile }}</span>
                <span class="text-xs text-gray-500">Only .bib files are allowed (max 5MB)</span>
              </div>
              
              <!-- Implementation File Upload (Optional) -->
              <div class="space-y-2">
                <label class="block text-sm font-medium text-gray-700">
                  Implementation File <span class="text-gray-400">(Optional)</span>
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
                >
                  <UploadIcon class="w-5 h-5 text-gray-500" />
                  <span class="text-gray-600">{{ fileName || 'Upload Algorithm Implementation' }}</span>
                </label>
              </div>
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

      <!-- 成功提示对话框 -->
      <div v-if="showSuccessDialog" 
           class="absolute top-4 right-4 bg-green-100 border border-green-400 text-green-700 px-6 py-4 rounded-lg shadow-lg flex items-center gap-3">
        <svg class="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
        </svg>
        <div>
          <h4 class="font-medium">Success!</h4>
          <p class="text-sm">Algorithm uploaded successfully</p>
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