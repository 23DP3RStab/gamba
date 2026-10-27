<script setup>
import { computed, ref } from 'vue'
import { setLossLimit } from '../../profile'
import { formatDateTime, formatMoney } from '../../format'

// The daily loss limit. Lowering it applies at once; raising or removing it waits 24 hours.
const props = defineProps({
  limits: { type: Object, required: true },
})
const emit = defineEmits(['update', 'failed'])

const MAX_LIMIT = 100000

const amount = ref(props.limits.dailyLossLimit ?? '')
const error = ref('')
const saved = ref('')

const hasLimit = computed(() => props.limits.dailyLossLimit !== null)
// lostToday goes below zero when the player is ahead today; winnings leave more room under the limit
const today = computed(() =>
  props.limits.lostToday < 0
    ? `You're ${formatMoney(-props.limits.lostToday)} up today`
    : `Lost today: ${formatMoney(props.limits.lostToday)}`,
)

async function save(value) {
  error.value = ''
  saved.value = ''
  try {
    const limits = await setLossLimit(value)
    emit('update', limits)
    saved.value = limits.pendingFrom
      ? `Saved. The change applies from ${formatDateTime(limits.pendingFrom)}`
      : 'Limit saved'
  } catch (e) {
    emit('failed', e)
    error.value = e.message
  }
}

function submit() {
  const value = Number(amount.value)
  if (amount.value === '' || !Number.isFinite(value) || value < 1) {
    error.value = `Enter a limit of at least ${formatMoney(1)}`
    return
  }
  if (value > MAX_LIMIT) {
    error.value = `The limit can be at most ${formatMoney(MAX_LIMIT)}`
    return
  }
  save(Math.round(value * 100) / 100)
}
</script>

<template>
  <p>
    Caps how much you can lose in a day, across all games. Lowering the limit applies at once;
    raising or removing it only takes effect after 24 hours.
  </p>

  <p class="status">
    <template v-if="hasLimit">
      Your limit is <strong>{{ formatMoney(limits.dailyLossLimit) }}</strong> a day. {{ today }},
      so you can lose {{ formatMoney(limits.leftToday) }} more.
    </template>
    <template v-else>No limit set. {{ today }}.</template>
  </p>

  <p v-if="limits.pendingFrom" class="pending">
    {{
      limits.pendingLossLimit === null
        ? 'Removing the limit'
        : `Changing the limit to ${formatMoney(limits.pendingLossLimit)}`
    }}
    from {{ formatDateTime(limits.pendingFrom) }}.
  </p>

  <form class="row" novalidate @submit.prevent="submit">
    <label class="field">
      Daily loss limit (€)
      <input v-model="amount" type="number" min="1" step="0.01" inputmode="decimal" />
    </label>
    <button type="submit" class="btn btn-gold">Save limit</button>
    <button
      v-if="hasLimit && !(limits.pendingFrom && limits.pendingLossLimit === null)"
      type="button"
      class="btn btn-outline"
      @click="save(null)"
    >
      Remove limit
    </button>
  </form>

  <p v-if="saved" class="success" role="status">{{ saved }}</p>
  <p v-if="error" class="error" role="alert">{{ error }}</p>
</template>

<style scoped>
.status,
.pending {
  margin-top: 0.75rem;
  font-size: 0.9rem;
}

.pending {
  color: var(--gold);
}

.row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  flex: 1 1 10rem;
  font-size: 0.85rem;
}

.success,
.error {
  margin-top: 0.75rem;
}
</style>
