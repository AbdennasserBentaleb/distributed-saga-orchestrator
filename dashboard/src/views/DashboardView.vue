<template>
  <div class="page-header">
    <h2>Overview</h2>
    <p>Live saga statistics. Refreshes every 3 seconds.</p>
  </div>
  <div class="page-body">
    <div v-if="error" class="alert alert-error">
      Could not reach the orchestrator service. Make sure Docker Compose is running.
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">Total Requests</div>
        <div class="stat-value">{{ stats.TOTAL_REQUESTS ?? 0 }}</div>
        <div class="stat-sub">all time</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Completed</div>
        <div class="stat-value" style="color: var(--green)">{{ stats.COMPLETED ?? 0 }}</div>
        <div class="stat-sub">all steps succeeded</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Compensated</div>
        <div class="stat-value" style="color: var(--red)">{{ stats.COMPENSATED ?? 0 }}</div>
        <div class="stat-sub">rolled back cleanly</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">In Progress</div>
        <div class="stat-value" style="color: var(--blue)">{{ inProgress }}</div>
        <div class="stat-sub">pending or intermediate</div>
      </div>
    </div>

    <div class="refresh-bar" style="margin-top: 36px">
      <h3>What this system demonstrates</h3>
    </div>
    <div class="card card-pad" style="margin-top: 0; line-height: 1.7; color: var(--text-secondary); font-size: 13px;">
      <p>
        Each booking request creates a saga that coordinates a flight reservation, hotel booking, and
        car rental as a single logical transaction across three independent microservices. If the car
        service fails after the flight and hotel have already committed, the orchestrator issues
        compensating cancellations in reverse order, leaving all three databases in a consistent state.
        Kafka provides message durability so no command is lost if a service is temporarily unavailable.
        Every message carries a W3C trace context header, making the full flow visible in Zipkin as a
        single end-to-end trace.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE || ''
const stats = ref({})
const error = ref(false)

const inProgress = computed(() => {
  const total = stats.value.TOTAL_REQUESTS ?? 0
  const done = (stats.value.COMPLETED ?? 0) + (stats.value.COMPENSATED ?? 0)
  return Math.max(0, total - done)
})

async function fetchStats() {
  try {
    const res = await fetch(`${API_BASE}/api/trips/audit`)
    if (!res.ok) throw new Error()
    stats.value = await res.json()
    error.value = false
  } catch {
    error.value = true
  }
}

let timer = null
onMounted(() => {
  fetchStats()
  timer = setInterval(fetchStats, 3000)
})
onUnmounted(() => clearInterval(timer))
</script>
