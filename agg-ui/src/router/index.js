import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import DatasetDetail from '../views/DatasetDetail.vue'
import AlgorithmDetail from '../views/AlgorithmDetail.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home
    },
    {
      path: '/dataset/:id',
      name: 'dataset-detail',
      component: DatasetDetail
    },
    {
      path: '/algorithm/:id',
      name: 'algorithm-detail',
      component: AlgorithmDetail
    }
  ]
})

export default router 