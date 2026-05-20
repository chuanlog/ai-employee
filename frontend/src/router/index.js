import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import UserList from '../views/UserList.vue'
import Chat from '../views/Chat.vue'
import TicketList from '../views/TicketList.vue'
import MyTickets from '../views/MyTickets.vue'
import KnowledgeBase from '../views/KnowledgeBase.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { 
    path: '/dashboard', 
    component: Dashboard, 
    children: [
      { path: '', redirect: '/dashboard/chat' },
      { path: 'chat', component: Chat },
      { path: 'users', component: UserList },
      { path: 'tickets', component: TicketList },
      { path: 'my-tickets', component: MyTickets },
      { path: 'knowledge-base', component: KnowledgeBase, meta: { requiresAdmin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const isAdmin = user.role === 'admin' || user.role === 1
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.meta?.requiresAdmin && !isAdmin) {
    next('/dashboard/chat')
  } else {
    next()
  }
})

export default router
