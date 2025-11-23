import { createRouter, createWebHistory } from 'vue-router'
import ReviewQueue from '@/views/ReviewQueue.vue'
import CandidateDetail from '@/views/CandidateDetail.vue'
import CandidateEnhance from '@/views/CandidateEnhance.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'review-queue',
      component: ReviewQueue
    },
    {
      path: '/candidates/:id',
      name: 'candidate-detail',
      component: CandidateDetail
    },
    {
      path: '/candidates/:id/enhance',
      name: 'candidate-enhance',
      component: CandidateEnhance
    }
  ]
})

export default router
