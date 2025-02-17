import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import DashboardView from './views/DashboardView.vue'
import BookingView from './views/BookingView.vue'
import TripsView from './views/TripsView.vue'
import './index.css'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/dashboard' },
        { path: '/dashboard', component: DashboardView },
        { path: '/book', component: BookingView },
        { path: '/trips', component: TripsView }
    ]
})

const app = createApp(App)
app.use(router)
app.mount('#app')
