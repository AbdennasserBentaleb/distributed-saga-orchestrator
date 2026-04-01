<template>
  <div class="page-header">
    <h2>Book a Trip</h2>
    <p>Submit a new booking request to the saga orchestrator.</p>
  </div>
  <div class="page-body">
    <div v-if="result" class="result-box success">
      <h4>Saga initiated</h4>
      <p>Saga ID: <code>{{ result.sagaId }}</code></p>
      <p style="margin-top: 6px">
        The orchestrator is now coordinating the flight, hotel, and car reservations asynchronously.
        Check Recent Trips to see the final status.
      </p>
    </div>
    <div v-if="submitError" class="result-box error">
      <h4>Request failed</h4>
      <p>{{ submitError }}</p>
    </div>

    <div class="card form-card">
      <div class="form-section-title">Customer</div>
      <div class="form-body">
        <div class="form-group">
          <label for="customerId">Customer ID</label>
          <input
            id="customerId"
            v-model="form.customerId"
            class="form-control"
            type="text"
            placeholder="e.g. cust-42"
            autocomplete="off"
          />
        </div>
      </div>

      <div class="form-section-title">Flight</div>
      <div class="form-body">
        <div class="form-group">
          <label for="flightDetails">Flight Details</label>
          <input
            id="flightDetails"
            v-model="form.flightDetails"
            class="form-control"
            type="text"
            placeholder="e.g. NYC to LAX, 2025-06-15"
            autocomplete="off"
          />
        </div>
      </div>

      <div class="form-section-title">Hotel</div>
      <div class="form-body">
        <div class="form-group">
          <label for="hotelDetails">Hotel Details</label>
          <input
            id="hotelDetails"
            v-model="form.hotelDetails"
            class="form-control"
            type="text"
            placeholder="e.g. Hilton Los Angeles, 3 nights"
            autocomplete="off"
          />
        </div>
      </div>

      <div class="form-section-title">Car Rental</div>
      <div class="form-body">
        <div class="form-group">
          <label for="carDetails">Car Details</label>
          <input
            id="carDetails"
            v-model="form.carDetails"
            class="form-control"
            type="text"
            placeholder="e.g. Tesla Model 3, compact"
            autocomplete="off"
          />
        </div>
      </div>

      <div class="form-actions">
        <button
          class="btn btn-primary"
          :disabled="submitting || !isValid"
          @click="submit"
        >
          {{ submitting ? 'Submitting...' : 'Submit Booking' }}
        </button>
        <button class="btn btn-ghost" style="margin-left: 10px" @click="reset">
          Clear
        </button>
      </div>
    </div>

    <div class="card card-pad" style="max-width: 560px; margin-top: 16px; font-size: 13px; color: var(--text-secondary); line-height: 1.65;">
      <strong style="color: var(--text-primary)">How it works</strong>
      <p style="margin-top: 6px">
        Submitting this form sends a single POST request to the orchestrator. The response returns
        immediately with a saga ID. In the background, the orchestrator publishes commands to Kafka
        which the flight, hotel, and car services consume sequentially. If the car service fails
        (it does so randomly 20% of the time), the orchestrator automatically cancels the hotel and
        flight bookings in reverse order.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE || ''

const form = ref({
  customerId: '',
  flightDetails: '',
  hotelDetails: '',
  carDetails: ''
})

const submitting = ref(false)
const result = ref(null)
const submitError = ref(null)

const isValid = computed(() =>
  form.value.customerId.trim() &&
  form.value.flightDetails.trim() &&
  form.value.hotelDetails.trim() &&
  form.value.carDetails.trim()
)

async function submit() {
  submitting.value = true
  result.value = null
  submitError.value = null

  try {
    const res = await fetch(`${API_BASE}/api/trips/book`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })

    const text = await res.text()

    if (!res.ok) {
      submitError.value = `Server returned ${res.status}: ${text}`
      return
    }

    // Extract UUID from response text: "Trip booking saga initiated with ID: <uuid>"
    const match = text.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i)
    result.value = { sagaId: match ? match[0] : text }
  } catch (err) {
    submitError.value = 'Could not reach the orchestrator service. Make sure the stack is running.'
  } finally {
    submitting.value = false
  }
}

function reset() {
  form.value = { customerId: '', flightDetails: '', hotelDetails: '', carDetails: '' }
  result.value = null
  submitError.value = null
}
</script>
