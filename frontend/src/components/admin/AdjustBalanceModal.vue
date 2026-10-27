<script setup>
import { ref } from 'vue';
import BaseInput from '../common/BaseInput.vue';
import BaseButton from '../common/BaseButton.vue';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close', 'updated']);

const amount = ref('');
const reason = ref('');
const error = ref('');
const isSubmitting = ref(false);

const handleAdjust = async () => {
  error.value = '';
  const numAmount = parseFloat(amount.value);

  if (isNaN(numAmount) || numAmount === 0) {
    error.value = 'Please enter a valid non-zero adjustment amount.';
    return;
  }

  isSubmitting.value = true;
  try {
    const token = localStorage.getItem('token');
    const res = await fetch(`${API_BASE_URL}/api/admin/users/${props.user.id}/balance`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        amount: numAmount,
        reason: reason.value.trim()
      })
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      error.value = data.message || 'Failed to adjust balance.';
    } else {
      emit('updated', data.data);
      emit('close');
    }
  } catch (err) {
    error.value = 'Network error while contacting admin server.';
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <h3>Adjust Balance: <span class="username">{{ user.username }}</span></h3>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="current-balance">
        Current balance: <strong>{{ Number(user.balance).toFixed(2) }} {{ user.currency }}</strong>
      </div>

      <form @submit.prevent="handleAdjust" class="modal-form">
        <div v-if="error" class="error-box">{{ error }}</div>

        <BaseInput
          id="adjust-amount"
          label="Amount (+ to Credit, - to Debit)"
          type="number"
          step="0.01"
          placeholder="e.g. 50 or -25"
          v-model="amount"
        />

        <BaseInput
          id="adjust-reason"
          label="Reason / Notes (Optional)"
          placeholder="e.g. Manual VIP promotional bonus"
          v-model="reason"
        />

        <div class="modal-actions">
          <button type="button" class="cancel-btn" @click="$emit('close')">Cancel</button>
          <div class="submit-wrapper">
            <BaseButton type="submit" :loading="isSubmitting">Apply</BaseButton>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}

.modal-card {
  background: #111116;
  border: 1px solid rgba(212, 175, 55, 0.4);
  border-radius: 14px;
  width: 100%;
  max-width: 440px;
  padding: 28px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.7);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #ffffff;
}

.username {
  color: #d4af37;
}

.close-btn {
  background: none;
  border: none;
  color: #7d7d8a;
  font-size: 1.2rem;
  cursor: pointer;
}

.close-btn:hover {
  color: #ffffff;
}

.current-balance {
  font-size: 0.9rem;
  color: #a0a0ab;
  margin-bottom: 20px;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-box {
  background: rgba(230, 57, 70, 0.15);
  border: 1px solid rgba(230, 57, 70, 0.4);
  color: #ff6b6b;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
}

.modal-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.cancel-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #a0a0ab;
  padding: 10px 18px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.cancel-btn:hover {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.3);
}

.submit-wrapper {
  min-width: 120px;
}
</style>
