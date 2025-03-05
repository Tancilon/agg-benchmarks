<template>
  <Dialog :open="isOpen" @close="$emit('close')" class="relative z-50">
    <div class="fixed inset-0 bg-black/20 backdrop-blur-sm" aria-hidden="true" />
    <div class="fixed inset-0 flex items-center justify-center p-4 sm:p-6 md:p-8">
      <DialogPanel class="w-[90%] max-w-[320px] sm:max-w-md md:max-w-lg bg-white/95 backdrop-blur-sm rounded-3xl shadow-2xl overflow-hidden border border-gray-100">
        <!-- 标题区域 -->
        <div class="px-4 sm:px-6 md:px-8 py-4 sm:py-5 md:py-6">
          <DialogTitle as="h3" class="text-xl font-semibold text-gray-900">
            Support Our Project
          </DialogTitle>
          <p class="mt-2 text-gray-500 text-sm leading-relaxed">
            Thank you for considering supporting AGG-Benchmarks! Your generous contribution helps us maintain and enhance the platform. Every donation, big or small, makes a meaningful difference in our mission to advance rank aggregation research.
          </p>
        </div>

        <!-- 内容区域 -->
        <div class="px-4 sm:px-6 md:px-8 pb-6 sm:pb-7 md:pb-8">
          <div class="space-y-6">
            <!-- 支付方式选择 -->
            <div class="flex gap-4">
              <button 
                @click="activeMethod = 'wechat'"
                class="flex-1 py-4 sm:py-5 md:py-6 px-4 sm:px-6 md:px-8 rounded-xl sm:rounded-2xl border transition-all duration-200 flex items-center justify-center"
                :class="activeMethod === 'wechat' ? 'border-[#07C160] bg-[#07C160]/5 shadow-lg shadow-[#07C160]/10' : 'border-gray-200 hover:border-[#07C160] hover:bg-[#07C160]/5'"
              >
                <Icon icon="ri:wechat-pay-fill" class="h-8 sm:h-10 md:h-12 w-8 sm:w-10 md:w-12 text-[#07C160] transition-transform duration-200 hover:scale-105" />
              </button>
              
              <button 
                @click="activeMethod = 'alipay'"
                class="flex-1 py-4 sm:py-5 md:py-6 px-4 sm:px-6 md:px-8 rounded-xl sm:rounded-2xl border transition-all duration-200 flex items-center justify-center"
                :class="activeMethod === 'alipay' ? 'border-[#1677FF] bg-[#1677FF]/5 shadow-lg shadow-[#1677FF]/10' : 'border-gray-200 hover:border-[#1677FF] hover:bg-[#1677FF]/5'"
              >
                <Icon icon="simple-icons:alipay" class="h-8 sm:h-10 md:h-12 w-8 sm:w-10 md:w-12 text-[#1677FF] transition-transform duration-200 hover:scale-105" />
              </button>
            </div>

            <!-- 二维码显示区域 -->
            <div class="flex justify-center py-6">
              <div class="w-48 sm:w-52 md:w-56 h-48 sm:h-52 md:h-56 border-2 rounded-xl sm:rounded-2xl p-2 sm:p-3 transition-all duration-300 backdrop-blur" 
                   :class="activeMethod === 'wechat' ? 'border-[#07C160] shadow-lg shadow-[#07C160]/10' : 'border-[#1677FF] shadow-lg shadow-[#1677FF]/10'">
                <img 
                  :src="activeMethod === 'wechat' ? '/wechat-qr.jpg' : '/alipay-qr.jpg'"
                  :alt="activeMethod === 'wechat' ? 'WeChat Pay QR Code' : 'Alipay QR Code'"
                  class="w-full h-full object-contain"
                />
              </div>
            </div>

            <p class="text-sm text-gray-500 text-center font-medium">
              Scan with {{ activeMethod === 'wechat' ? 'WeChat' : 'Alipay' }} to donate
            </p>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="px-4 sm:px-6 md:px-8 py-4 sm:py-5 bg-gray-50/50 backdrop-blur-sm flex justify-end border-t border-gray-100">
          <button
            @click="$emit('close')"
            class="px-5 py-2.5 text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors duration-200"
          >
            Close
          </button>
        </div>
      </DialogPanel>
    </div>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/vue'
import { Icon } from '@iconify/vue'

defineProps({
  isOpen: Boolean
})

defineEmits(['close'])

const activeMethod = ref('wechat')
</script>

<style scoped>
/* 添加平滑过渡效果 */
.v-enter-active,
.v-leave-active {
  transition: opacity 0.3s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}

/* 微信和支付宝的品牌色 */
:root {
  --wechat-color: #07C160;
  --alipay-color: #1677FF;
}

/* 添加图标悬停效果 */
button:hover img {
  transform: scale(1.05);
}

/* 添加按钮点击效果 */
button:active {
  transform: scale(0.98);
}

button {
  transition: all 0.2s ease;
}
</style> 