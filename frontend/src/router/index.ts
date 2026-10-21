import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'image-generator',
      component: () => import('@/views/ImageGenerator.vue'),
    },
  ],
})

export default router
