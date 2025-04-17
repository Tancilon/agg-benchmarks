<script setup>
import { ref, computed, nextTick } from 'vue'
import { LineChart, X, Upload as UploadIcon, CheckCircle2 } from 'lucide-vue-next'
import { marked } from 'marked'
import katex from 'katex'
import hljs from 'highlight.js'
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/github.css'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'submit'])

// 配置 marked
marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return code
  },
  breaks: true,
  gfm: true
})

// 添加数学公式支持
const mathExtension = {
  name: 'math',
  level: 'inline',
  start(src) { return src.match(/\$/)?.index },
  tokenizer(src) {
    const blockRule = /^\$\$([\s\S]*?)\$\$/
    const inlineRule = /^\$((?:\\\$|[^$])+)\$/
    
    let match
    if (match = blockRule.exec(src)) {
      return {
        type: 'math',
        raw: match[0],
        text: match[1].trim(),
        display: true
      }
    } else if (match = inlineRule.exec(src)) {
      return {
        type: 'math',
        raw: match[0],
        text: match[1].trim(),
        display: false
      }
    }
  },
  renderer(token) {
    try {
      return katex.renderToString(token.text, {
        displayMode: token.display,
        throwOnError: false
      })
    } catch (err) {
      console.error('KaTeX error:', err)
      return token.raw
    }
  }
}

// 添加扩展
marked.use({ extensions: [mathExtension] })

// 修改示例模板
const descriptionTemplate = `# Metric Description

## Definition
Briefly explain what this metric measures...

## Formula
Inline formula: $\\text{MAP} = \\frac{1}{|Q|}\\sum_{q\\in Q}\\text{AP}(q)$

Block formula:
$$
\\begin{aligned}
\\text{NDCG}@k &= \\frac{\\text{DCG}@k}{\\text{IDCG}@k} \\\\[0.5em]
\\text{where:} \\\\[0.5em]
\\text{DCG}@k &= \\sum_{i=1}^k \\frac{2^{rel_i} - 1}{\\log_2(i+1)} \\\\[0.5em]
\\text{IDCG}@k &= \\sum_{i=1}^k \\frac{2^{rel_i^*} - 1}{\\log_2(i+1)}
\\end{aligned}
$$

## Parameters
- Q: query set
- AP(q): average precision for query q
- rel_i: relevance score at position i
- rel_i*: relevance score in ideal ranking

## Example
\`\`\`python
def calculate_metric(predictions, ground_truth):
    # Implementation example
    return score
\`\`\`

## References
- [Paper Title](paper_link)
`

// 初始化表单数据
const formData = ref({
  name: '',
  description: descriptionTemplate, // 使用模板作为初始值
  type: '',
  rangeType: 'numeric', // 新增：范围类型
  rangeMin: '', // 新增：最小值
  rangeMax: '', // 新增：最大值
  file: null
})

// 修改渲染函数
const renderedDescription = computed(() => {
  try {
    return marked(formData.value.description || '')
  } catch (error) {
    console.error('Markdown rendering error:', error)
    return '<p class="text-red-500">Error rendering markdown</p>'
  }
})

const fileName = ref('')
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    console.log('File selected:', {
      name: file.name,
      size: file.size,
      type: file.type
    })
    
    fileName.value = file.name
    formData.value.file = file
    
    if (!formData.value.file) {
      console.error('Failed to store file in formData')
    }
  }
}

// 添加表单验证
const formErrors = ref({
  name: '',
  description: '',
  type: '',
  range: '',
  file: ''
})

// 添加反馈状态
const feedbackState = ref({
  show: false,
  type: '', // 'success' 或 'error'
  title: '',
  message: ''
})

// 修改表单提交逻辑
const handleSubmit = async () => {
  try {
    if (!validateForm()) return;

    // 先检查指标名称是否已存在
    const checkResponse = await fetch(`/api/metrics/check-name?name=${encodeURIComponent(formData.value.name)}`);
    if (!checkResponse.ok) {
      const errorText = await checkResponse.text();
      if (checkResponse.status === 409) {
        feedbackState.value = {
          show: true,
          type: 'error',
          title: 'Upload Failed',
          message: 'A metric with this name already exists.'
        }
        return;
      }
      throw new Error(errorText || 'Failed to check metric name');
    }

    const formDataToSubmit = new FormData();
    
    // 添加各个字段作为独立的表单参数
    formDataToSubmit.append('name', formData.value.name);
    formDataToSubmit.append('description', formData.value.description);
    formDataToSubmit.append('type', formData.value.type);
    formDataToSubmit.append('range', formattedRange.value);
    
    // 如果有文件，添加文件
    if (formData.value.file) {
      formDataToSubmit.append('implementationFile', formData.value.file);
    }

    // 打印发送的数据用于调试
    for (let [key, value] of formDataToSubmit.entries()) {
      console.log(`${key}:`, value instanceof File ? `File: ${value.name}` : value);
    }
    
    const response = await fetch('/api/metrics', {
      method: 'POST',
      body: formDataToSubmit
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('Server error response:', {
        status: response.status,
        statusText: response.statusText,
        errorText: errorText
      });
      throw new Error(errorText || 'Failed to upload metric');
    }
    
    const result = await response.json();
    console.log('Upload successful:', result);
    
    // 显示成功反馈
    feedbackState.value = {
      show: true,
      type: 'success',
      title: 'Upload Successful',
      message: 'Your metric has been successfully uploaded.'
    };
    
    // 3秒后关闭对话框
    setTimeout(() => {
      feedbackState.value.show = false;
      setTimeout(() => {
        emit('close');
        emit('submit');
      }, 300);
    }, 3000);

  } catch (error) {
    console.error('Error uploading metric:', error);
    
    feedbackState.value = {
      show: true,
      type: 'error',
      title: 'Upload Failed',
      message: error.message || 'Failed to upload metric. Please try again.'
    };
  }
};

// 修改表单验证
const validateForm = () => {
  let isValid = true
  formErrors.value = {
    name: '',
    description: '',
    type: '',
    range: '',
    file: ''
  }

  if (!formData.value.name.trim()) {
    formErrors.value.name = 'Metric name is required'
    isValid = false
  }

  if (!formData.value.description.trim()) {
    formErrors.value.description = 'Description is required'
    isValid = false
  }

  if (!formData.value.type) {
    formErrors.value.type = 'Metric type is required'
    isValid = false
  }

  if (formData.value.rangeType === 'numeric') {
    if (!formData.value.rangeMin || !formData.value.rangeMax) {
      formErrors.value.range = 'Please specify both minimum and maximum values'
      isValid = false
    } else if (isNaN(Number(formData.value.rangeMin)) || isNaN(Number(formData.value.rangeMax))) {
      formErrors.value.range = 'Range values must be numbers'
      isValid = false
    } else if (Number(formData.value.rangeMin) >= Number(formData.value.rangeMax)) {
      formErrors.value.range = 'Maximum value must be greater than minimum value'
      isValid = false
    }
  }

  return isValid
}

// 计算属性：格式化后的范围字符串
const formattedRange = computed(() => {
  if (formData.value.rangeType === 'real') {
    return 'ℝ'
  } else {
    return `[${formData.value.rangeMin}, ${formData.value.rangeMax}]`
  }
})

// 添加指标类型说明
const typeDescriptions = {
  'at-k': 'The metric supports @k operations (e.g., mAP@k, NDCG@k). Results will be shown as line charts.',
  'fixed': 'The metric has a fixed calculation method. Results will be shown as bar charts.'
}

// 添加 Markdown 快捷操作
const markdownActions = {
  heading: {
    icon: 'i-lucide-heading',
    title: 'Add Heading',
    template: '\n# '
  },
  bold: {
    icon: 'i-lucide-bold',
    title: 'Bold Text',
    template: '**bold text**'
  },
  italic: {
    icon: 'i-lucide-italic',
    title: 'Italic Text',
    template: '*italic text*'
  },
  code: {
    icon: 'i-lucide-code',
    title: 'Code Block',
    template: '\n```python\n\n```'
  },
  math: {
    icon: 'i-lucide-sigma',
    title: 'Math Block',
    template: '\n$$\n\n$$'
  },
  inlineMath: {
    icon: 'i-lucide-function',
    title: 'Inline Math',
    template: '$formula$'
  },
  link: {
    icon: 'i-lucide-link',
    title: 'Add Link',
    template: '[link text](url)'
  }
}

// 插入 Markdown 模板
const insertMarkdown = (template) => {
  const textarea = document.querySelector('textarea')
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const text = formData.value.description
  
  formData.value.description = text.substring(0, start) + template + text.substring(end)
  
  // 设置光标位置
  nextTick(() => {
    textarea.focus()
    textarea.selectionStart = start + template.length
    textarea.selectionEnd = start + template.length
  })
}

// 添加数学公式快捷操作
const mathActions = {
  fraction: {
    icon: 'i-lucide-divide',
    title: 'Fraction',
    template: '\\frac{numerator}{denominator}'
  },
  sum: {
    icon: 'i-lucide-sigma',
    title: 'Summation',
    template: '\\sum_{i=1}^n'
  },
  integral: {
    icon: 'i-lucide-function-square',
    title: 'Integral',
    template: '\\int_{a}^{b}'
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
                <LineChart class="w-6 h-6 text-white" />
              </div>
              <h2 class="text-2xl font-medium text-gray-900">Upload Metrics</h2>
              <p class="text-sm text-gray-500">Add new performance metrics to the benchmark</p>
            </div>

            <!-- Form -->
            <div class="space-y-8">
              <!-- Metric Name -->
              <div class="space-y-2">
                <label class="block text-sm font-medium text-gray-700">
                  Metric Name
                  <span class="text-red-500 ml-1">*</span>
                </label>
                <input 
                  v-model="formData.name"
                  type="text"
                  :class="[
                    'w-full px-4 py-3 bg-black/5 border-2 rounded-xl focus:ring-2 transition-colors',
                    formErrors.name 
                      ? 'border-red-300 focus:ring-red-500' 
                      : 'border-transparent focus:ring-blue-500'
                  ]"
                  placeholder="e.g. Precision, NDCG"
                />
                <p v-if="formErrors.name" class="mt-1 text-sm text-red-500">
                  {{ formErrors.name }}
                </p>
              </div>

              <!-- Metric Type -->
              <div class="space-y-2">
                <label class="block text-sm font-medium text-gray-700">
                  Metric Type
                  <span class="text-red-500 ml-1">*</span>
                </label>
                <div class="space-y-3">
                  <!-- Radio Buttons -->
                  <div class="space-y-2">
                    <label 
                      v-for="(description, type) in typeDescriptions" 
                      :key="type"
                      class="flex items-start p-3 gap-3 rounded-xl cursor-pointer transition-all duration-300"
                      :class="[
                        formData.type === type 
                          ? 'bg-blue-50 border-2 border-blue-200' 
                          : 'bg-black/5 border-2 border-transparent hover:bg-black/10'
                      ]"
                    >
                      <div class="flex items-center h-5 mt-0.5">
                        <input
                          type="radio"
                          :value="type"
                          v-model="formData.type"
                          name="metric-type"
                          class="w-4 h-4 text-blue-600 border-gray-300 focus:ring-blue-500"
                        />
                      </div>
                      <div class="flex-1">
                        <div class="flex items-center">
                          <span class="font-medium text-gray-900">
                            {{ type === 'at-k' ? '@k Support' : 'Fixed Metric' }}
                          </span>
                          <span 
                            class="ml-2 px-2 py-0.5 text-xs rounded-full"
                            :class="type === 'at-k' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'"
                          >
                            {{ type === 'at-k' ? 'Line Chart' : 'Bar Chart' }}
                          </span>
                        </div>
                        <p class="mt-1 text-sm text-gray-500">{{ description }}</p>
                      </div>
                    </label>
                  </div>

                  <!-- Error Message -->
                  <p v-if="formErrors.type" class="mt-1 text-sm text-red-500">
                    {{ formErrors.type }}
                  </p>
                </div>
              </div>

              <!-- Data Range Input -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700">
                  Data Range <span class="text-red-500">*</span>
                </label>
                
                <!-- Range Type Selection -->
                <div class="flex items-center gap-4 mb-2">
                  <label class="flex items-center gap-2">
                    <input 
                      type="radio" 
                      v-model="formData.rangeType" 
                      value="numeric"
                      class="text-blue-600 focus:ring-blue-500"
                    />
                    <span class="text-sm text-gray-600">Numeric Range</span>
                  </label>
                  <label class="flex items-center gap-2">
                    <input 
                      type="radio" 
                      v-model="formData.rangeType" 
                      value="real"
                      class="text-blue-600 focus:ring-blue-500"
                    />
                    <span class="text-sm text-gray-600">Real Numbers (ℝ)</span>
                  </label>
                </div>

                <!-- Numeric Range Input -->
                <template v-if="formData.rangeType === 'numeric'">
                  <div class="flex items-center gap-2">
                    <input 
                      type="text"
                      v-model="formData.rangeMin"
                      placeholder="Min"
                      class="w-24 px-4 py-2 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      :class="{ 'border-red-500': formErrors.range }"
                    />
                    <span class="text-gray-500">to</span>
                    <input 
                      type="text"
                      v-model="formData.rangeMax"
                      placeholder="Max"
                      class="w-24 px-4 py-2 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      :class="{ 'border-red-500': formErrors.range }"
                    />
                  </div>
                  <p class="text-xs text-gray-500">Enter the minimum and maximum values (e.g., 0 to 1, or 0 to 100)</p>
                </template>

                <!-- Real Numbers Display -->
                <template v-else>
                  <div class="px-4 py-2 bg-gray-50 rounded-xl border border-gray-200 text-gray-700">
                    ℝ (All real numbers)
                  </div>
                  <p class="text-xs text-gray-500">This metric accepts any real number value</p>
                </template>

                <span v-if="formErrors.range" class="text-sm text-red-500">{{ formErrors.range }}</span>
              </div>

              <!-- Description -->
              <div class="space-y-2">
                <div class="flex items-center justify-between">
                  <label class="block text-sm font-medium text-gray-700">
                    Description
                    <span class="text-red-500 ml-1">*</span>
                  </label>
                  <button 
                    @click="formData.description = descriptionTemplate"
                    class="text-xs text-blue-600 hover:text-blue-700 flex items-center gap-1"
                  >
                    <span class="i-lucide-file-text text-sm"></span>
                    Use Template
                  </button>
                </div>

                <!-- Editor and Preview Container -->
                <div class="grid grid-cols-2 gap-6 bg-white/50 rounded-xl p-6 border border-gray-100">
                  <!-- Markdown Editor -->
                  <div class="space-y-3">
                    <div class="flex items-center justify-between px-2">
                      <h3 class="text-xs font-medium text-gray-500">MARKDOWN</h3>
                      <div class="flex items-center gap-2">
                        <button 
                          v-for="(action, key) in markdownActions"
                          :key="key"
                          @click="insertMarkdown(action.template)"
                          class="p-1.5 rounded hover:bg-gray-100 text-gray-500 hover:text-gray-700 transition-colors"
                          :title="action.title"
                        >
                          <component :is="action.icon" class="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                    <textarea
                      v-model="formData.description"
                      rows="20"
                      :class="[
                        'w-full px-4 py-3 bg-black/5 border-2 rounded-xl focus:ring-2 transition-colors font-mono text-sm custom-scrollbar',
                        formErrors.description 
                          ? 'border-red-300 focus:ring-red-500' 
                          : 'border-transparent focus:ring-blue-500'
                      ]"
                      placeholder="Describe how this metric is calculated..."
                    ></textarea>
                  </div>

                  <!-- Preview -->
                  <div class="space-y-3">
                    <div class="flex items-center justify-between px-2">
                      <h3 class="text-xs font-medium text-gray-500">PREVIEW</h3>
                      <div class="flex items-center gap-2 text-xs text-gray-500">
                        <span class="i-lucide-eye text-sm"></span>
                        Live Preview
                      </div>
                    </div>
                    <div 
                      class="prose prose-sm max-w-none overflow-y-auto custom-scrollbar bg-black/5 rounded-xl p-6"
                      style="height: 480px"
                      v-html="renderedDescription"
                    ></div>
                  </div>
                </div>

                <!-- Error and Help Text -->
                <div class="space-y-2">
                  <p v-if="formErrors.description" class="text-sm text-red-500">
                    {{ formErrors.description }}
                  </p>
                  <div class="bg-blue-50 rounded-lg p-3 space-y-2">
                    <p class="text-xs font-medium text-blue-700">Markdown Syntax Guide:</p>
                    <div class="grid grid-cols-2 gap-2 text-xs text-blue-600">
                      <div class="space-y-1">
                        <p class="font-mono"># Heading 1</p>
                        <p class="font-mono">## Heading 2</p>
                        <p class="font-mono">**bold text**</p>
                        <p class="font-mono">*italic text*</p>
                      </div>
                      <div class="space-y-1">
                        <p class="font-mono">$E = mc^2$ (inline math)</p>
                        <p class="font-mono">```python (code block)</p>
                        <p class="font-mono">[link](url)</p>
                        <p class="font-mono">- bullet point</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Implementation File -->
              <div class="space-y-2">
                <label class="block text-sm font-medium text-gray-700">
                  Implementation File
                  <span class="text-gray-400 text-xs ml-2">(Optional)</span>
                </label>
                <input
                  type="file"
                  @change="handleFileChange"
                  class="hidden"
                  id="metric-upload"
                />
                <label 
                  for="metric-upload"
                  class="flex items-center justify-center gap-2 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300 bg-black/5 hover:bg-black/10 border-2 border-transparent group"
                >
                  <UploadIcon class="w-5 h-5 text-gray-500 group-hover:text-gray-700" />
                  <span class="text-gray-600 group-hover:text-gray-800">
                    {{ fileName || 'Upload Implementation File (Optional)' }}
                  </span>
                </label>
                <div class="space-y-2">
                  <p class="text-xs text-gray-500">
                    You can upload the implementation code later if it's not ready yet.
                  </p>
                </div>
              </div>

              <!-- Submit Button -->
              <button 
                @click="handleSubmit"
                class="w-full py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors font-medium shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="!formData.name || !formData.description || !formData.type || 
                         (formData.rangeType === 'numeric' && (!formData.rangeMin || !formData.rangeMax))"
              >
                Upload Metric
              </button>
            </div>
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
  height: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
  margin: 4px 0;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

/* Hide scrollbar when not scrolling */
.custom-scrollbar:not(:hover)::-webkit-scrollbar-thumb {
  background-color: transparent;
}

/* Markdown Preview Styles */
:deep(.prose) {
  font-size: 0.875rem;
  line-height: 1.6;
  max-width: none;
}

:deep(.prose h1) {
  font-size: 1.5em;
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

:deep(.prose h2) {
  font-size: 1.25em;
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

:deep(.prose p) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

:deep(.prose ul) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
  padding-left: 1.5em;
  list-style-type: disc;
}

:deep(.prose pre) {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 0.5rem;
  padding: 1rem;
  margin: 1rem 0;
}

:deep(.prose code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.8em;
}

:deep(.prose .katex) {
  font-size: 1.1em;
}

:deep(.prose a) {
  color: #2563eb;
  text-decoration: underline;
}

:deep(.prose blockquote) {
  border-left: 4px solid #e5e7eb;
  padding-left: 1rem;
  font-style: italic;
  color: #6b7280;
}

/* 添加必填项标记的样式 */
.required::after {
  content: '*';
  color: #EF4444;
  margin-left: 0.25rem;
}

/* 自定义单选按钮样式 */
input[type="radio"] {
  position: relative;
  cursor: pointer;
}

input[type="radio"]:checked::after {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #2563eb;
}

input[type="radio"]:checked {
  border-color: #2563eb;
}

/* 添加组悬停效果 */
.group:hover .group-hover\:text-gray-700 {
  color: #374151;
}

.group:hover .group-hover\:text-gray-800 {
  color: #1F2937;
}

/* 添加编辑器工具栏按钮动画 */
button {
  transition: all 0.2s ease;
}

button:hover {
  transform: scale(1.05);
}

/* 优化数学公式样式 */
:deep(.katex-display) {
  margin: 1.5em 0;
  padding: 1em;
  overflow: visible;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 0.5rem;
  text-align: center;
}

:deep(.katex) {
  font-size: 1.1em;
  line-height: 1.5;
  text-rendering: optimizeLegibility;
}

:deep(.katex-html) {
  max-width: 100%;
  overflow-x: auto;
  padding: 0.5em;
}

/* 改进分数样式 */
:deep(.katex .mfrac) {
  margin: 0 0.2em;
  min-width: 0.6em;
}

:deep(.katex .mfrac .frac-line) {
  border-bottom-width: 0.04em;
  margin: 0;
}

:deep(.katex .mfrac .frac-line::after) {
  border-bottom-width: 0.04em;
}

:deep(.katex .mfrac .mfrac-left, .katex .mfrac .mfrac-right) {
  padding: 0 0.1em;
}

:deep(.katex .mord.text) {
  font-family: inherit;
}

/* 优化对齐环境 */
:deep(.katex .align) {
  width: 100%;
  text-align: center;
  margin: 0.5em auto;
}

:deep(.katex .align > .align-row) {
  margin: 0.2em 0;
}

/* 优化求和符号 */
:deep(.katex .op-symbol) {
  margin: 0 0.1em;
}

:deep(.katex .op-limits > .vlist-t) {
  text-align: center;
}

/* 优化上下标 */
:deep(.katex .msupsub) {
  text-align: center;
}

/* 优化大型公式容器 */
:deep(.katex-display > .katex) {
  display: block;
  text-align: center;
  white-space: nowrap;
  max-width: 100%;
}

:deep(.katex-display > .katex > .katex-html) {
  display: block;
  position: relative;
  overflow-x: auto;
  overflow-y: hidden;
  text-align: center;
}

/* 优化分数中的文本对齐 */
:deep(.katex .mfrac .mord.text) {
  text-align: center;
  display: inline-block;
  width: 100%;
}

/* 优化分数的垂直对齐 */
:deep(.katex .vlist-t) {
  display: inline-table;
  table-layout: fixed;
}

:deep(.katex .vlist-r) {
  display: table-row;
}

:deep(.katex .vlist) {
  display: table-cell;
  vertical-align: middle;
}

/* 优化 Markdown 预览样式 */
:deep(.markdown-body) {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  font-size: 0.875rem;
  line-height: 1.6;
  color: #24292e;
}

:deep(.markdown-body pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}

:deep(.markdown-body code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 85%;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  padding: 0.2em 0.4em;
}

/* 优化数学公式样式 */
:deep(.katex-display) {
  margin: 1em 0;
  padding: 1em;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 0.5rem;
  overflow-x: auto;
  overflow-y: hidden;
}

:deep(.katex) {
  font-size: 1.1em;
  line-height: 1.5;
  text-rendering: optimizeLegibility;
}

.feedback-enter-active,
.feedback-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.feedback-enter-from,
.feedback-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style> 