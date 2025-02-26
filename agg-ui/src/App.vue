<template>
  <div class="min-h-screen flex flex-col">
    <!-- Navigation -->
    <nav class="bg-zinc-900 px-4 py-4">
      <div class="container mx-auto flex items-center gap-8">
        <router-link to="/" class="text-white font-semibold">
          🖥️ AGG-Benchmarks
        </router-link>
        <div class="flex gap-6">
          <router-link to="/" class="text-zinc-400 hover:text-white"
            :class="{ 'text-white': $route.path === '/' }">
            Home
          </router-link>
          <router-link to="#" class="text-zinc-400 hover:text-white">
            Dataset
          </router-link>
          <router-link to="#" class="text-zinc-400 hover:text-white">
            Algorithms
          </router-link>
          <router-link to="#" class="text-zinc-400 hover:text-white">
            Contact
          </router-link>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="flex-grow">
      <router-view></router-view>
    </main>

    <!-- Contact Us Section -->
    <section class="py-12 px-4 bg-black">
      <div class="container mx-auto">
        <h2 class="text-2xl font-semibold mb-8 flex items-center justify-center gap-2 text-white">
          <Mail class="w-5 h-5" />
          Contact Us
        </h2>
        <div>
          <p class="text-zinc-300 leading-relaxed">
            AGG-Benchmarks has been developed by Martin Aumueller (
            <a href="mailto:maua@itu.dk" class="text-[#336FFF] hover:underline">maua@itu.dk</a>
            ), Erik Bernhardsson (
            <a href="mailto:mail@erikbern.com" class="text-[#336FFF] hover:underline">mail@erikbern.com</a>
            ), and Alec Faithfull (
            <a href="mailto:alef@itu.dk" class="text-[#336FFF] hover:underline">alef@itu.dk</a>
            ). Please use 
            <a href="https://github.com" target="_blank" class="text-[#336FFF] hover:underline inline-flex items-center gap-1">
              Github <Github class="w-4 h-4" />
            </a>
            to submit your implementation or improvements.
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

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

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}

.shadow-lg {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>
