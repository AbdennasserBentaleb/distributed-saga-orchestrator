<template>
  <span class="badge" :class="badgeClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const LABELS = {
  COMPLETED: 'Completed',
  COMPENSATED: 'Compensated',
  PENDING: 'Pending',
  FLIGHT_BOOKED: 'Flight Booked',
  HOTEL_BOOKED: 'Hotel Booked',
  CANCELLING_HOTEL: 'Cancelling Hotel',
  CANCELLING_FLIGHT: 'Cancelling Flight'
}

const label = computed(() => LABELS[props.status] ?? props.status)

const badgeClass = computed(() => {
  switch (props.status) {
    case 'COMPLETED':         return 'badge-completed'
    case 'COMPENSATED':       return 'badge-compensated'
    case 'PENDING':           return 'badge-pending'
    case 'CANCELLING_HOTEL':
    case 'CANCELLING_FLIGHT': return 'badge-other'
    default:                  return 'badge-pending'
  }
})
</script>
