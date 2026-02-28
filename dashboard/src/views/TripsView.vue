<template>
  <div class="page-header">
    <h2>Recent Trips</h2>
    <p>The 20 most recent saga records. Refreshes every 5 seconds.</p>
  </div>
  <div class="page-body">
    <div v-if="error" class="alert alert-error">
      Could not reach the orchestrator service. Make sure Docker Compose is running.
    </div>

    <div class="refresh-bar">
      <h3>Saga Records</h3>
      <span class="refresh-hint">Last updated: {{ lastUpdated }}</span>
    </div>

    <div class="card table-wrap">
      <div v-if="loading" class="loading">Loading...</div>
      <div v-else-if="trips.length === 0" class="empty-state">No trips found. Submit a booking to get started.</div>
      <table v-else>
        <thead>
          <tr>
            <th>Saga ID</th>
            <th>Customer</th>
            <th>Status</th>
            <th>Flight</th>
            <th>Hotel</th>
            <th>Car</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="trip in trips" :key="trip.id">
            <td class="cell-id">{{ short(trip.id) }}</td>
            <td>{{ trip.customerId }}</td>
            <td><StatusBadge :status="trip.status" /></td>
            <td class="cell-detail" :title="trip.flightDetails">{{ trip.flightDetails }}</td>
            <td class="cell-detail" :title="trip.hotelDetails">{{ trip.hotelDetails }}</td>
            <td class="cell-detail" :title="trip.carDetails">{{ trip.carDetails }}</td>
            <td class="cell-time">{{ formatTime(trip.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import StatusBadge from '../components/StatusBadge.vue'

const API_BASE = import.meta.env.VITE_API_BASE || ''
const trips = ref([])
const loading = ref(true)
const error = ref(false)
const lastUpdated = ref('—')

function short(id) {
  if (!id) return ''
  return id.substring(0, 8) + '...'
}

function formatTime(dt) {
  if (!dt) return '—'
  const d = new Date(dt)
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

async function fetchTrips() {
  try {
    const res = await fetch(`${API_BASE}/api/trips/recent`)
    if (!res.ok) throw new Error()
    trips.value = await res.json()
    error.value = false
    lastUpdated.value = new Date().toLocaleTimeString()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

let timer = null
onMounted(() => {
  fetchTrips()
  timer = setInterval(fetchTrips, 5000)
})
onUnmounted(() => clearInterval(timer))
</script>
