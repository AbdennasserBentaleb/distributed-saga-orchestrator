<template>
  <div id="app">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <h1>Saga Orchestrator</h1>
        <p>Travel Booking System</p>
      </div>
      <nav class="sidebar-nav">
        <RouterLink to="/dashboard" class="nav-link">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path d="M3 4a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4zm0 6a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-6zm10 0a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1v-6z"/>
          </svg>
          Overview
        </RouterLink>
        <RouterLink to="/book" class="nav-link">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 3a1 1 0 0 1 1 1v5h5a1 1 0 1 1 0 2h-5v5a1 1 0 1 1-2 0v-5H4a1 1 0 1 1 0-2h5V4a1 1 0 0 1 1-1z" clip-rule="evenodd"/>
          </svg>
          Book a Trip
        </RouterLink>
        <RouterLink to="/trips" class="nav-link">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path d="M5 3a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2H5zm0 2h10v10H5V5zm2 2a1 1 0 0 0 0 2h6a1 1 0 1 0 0-2H7zm0 4a1 1 0 0 0 0 2h4a1 1 0 1 0 0-2H7z"/>
          </svg>
          Recent Trips
        </RouterLink>
      </nav>
      <div class="sidebar-footer">
        <p>Saga Orchestration Pattern</p>
      </div>
    </aside>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>
