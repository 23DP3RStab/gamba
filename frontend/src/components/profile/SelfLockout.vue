<script setup>
import { computed, ref } from 'vue'
import { activeLockout, lockOut } from '../../profile'
import { formatDateTime } from '../../format'

// Self-lockout: no games and no deposits until it ends. It can be extended, never shortened.
const props = defineProps({
  limits: { type: Object, required: true },
})
const emit = defineEmits(['update', 'failed'])

const OPTIONS = [
  { days: 1, label: '24 hours' },
  { days: 7, label: '7 days' },
  { days: 30, label: '30 days' },
  { days: 180, label: '6 months' },
]

const choice = ref(null) // the option waiting for confirmation
const error = ref('')
const until = computed(() => activeLockout(props.limits))

async function confirm() {
  error.value = ''
  try {
    emit('update', await lockOut(choice.value.days))
    choice.value = null
  } catch (e) {
    emit('failed', e)
    error.value = e.message
  }
}
</script>

<template>
  <p v-if="until" class="locked" role="status">
    Your account is locked until <strong>{{ formatDateTime(until) }}</strong>. Until then you can't
    play or deposit; withdrawals still work. You can make the lockout longer, but not shorter.
  </p>
  <p v-else>
    Need a break? Lock your account and you can't play any game or deposit money until the
    lockout ends. It can't be undone or shortened.
  </p>

  <template v-if="choice">
    <p class="confirm">
      Lock your account for <strong>{{ choice.label }}</strong>? This can't be undone.
    </p>
    <div class="actions">
      <button type="button" class="btn btn-outline" @click="choice = null">Cancel</button>
      <button type="button" class="btn btn-danger" @click="confirm">
        Yes, lock for {{ choice.label }}
      </button>
    </div>
  </template>
  <div v-else class="actions">
    <button
      v-for="option in OPTIONS"
      :key="option.days"
      type="button"
      class="btn btn-outline-gold"
      @click="choice = option"
    >
      {{ option.label }}
    </button>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>
</template>

<style scoped>
.locked {
  color: #ff6b6b;
}

.confirm {
  margin-top: 0.75rem;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.error {
  margin-top: 0.75rem;
}
</style>
