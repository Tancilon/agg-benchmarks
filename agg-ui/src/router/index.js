import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import DatasetDetail from '../views/DatasetDetail.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: Home
    },
    {
      path: '/dataset/:id',
      name: 'DatasetDetail',
      component: DatasetDetail,
      props: true
    }
  ]
})

export default router 