import { createRouter, createWebHashHistory } from 'vue-router'

// 注意：main.js 里没有 app.use(router)，路由实例由 App.vue 直接驱动，
// 所以这里全部用静态 import —— <component :is> 拿到的是组件对象本身。
import MachineView from '../views/MachineView.vue'
import FieldView from '../views/FieldView.vue'
import WorkOrderView from '../views/WorkOrderView.vue'
import MaintainView from '../views/MaintainView.vue'

const routes = [
  { path: '/', redirect: '/machines' },
  { path: '/machines', name: 'machines', component: MachineView },
  { path: '/fields', name: 'fields', component: FieldView },
  { path: '/work-orders', name: 'work-orders', component: WorkOrderView },
  { path: '/maintains', name: 'maintains', component: MaintainView },
  { path: '/:pathMatch(.*)*', redirect: '/machines' }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
