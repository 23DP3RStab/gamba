<script setup>
import { ref, onMounted } from 'vue';
import BalanceHistoryTable from '../wallet/BalanceHistoryTable.vue';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
});

defineEmits(['close']);

const transactions = ref([]);
const isLoading = ref(true);

const fetchUserTransactions = async () => {
  isLoading.value = true;
  const token = localStorage.getItem('token');
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/users/${props.user.id}/transactions`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    const data = await res.json();
    if (data.success) {
      transactions.value = data.data;
    }
  } catch (err) {
    console.error('Failed to load user transactions:', err);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchUserTransactions();
});
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <div>
          <h3>Audit Ledger: <span class="username">{{ user.username }}</span></h3>
          <p class="subtitle">Complete transaction log, game outcomes, and balance adjustments</p>
        </div>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <BalanceHistoryTable :transactions="transactions" :is-loading="isLoading" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

.modal-card {
  background: #111116;
  border: 1px solid rgba(212, 175, 55, 0.4);
  border-radius: 16px;
  width: 100%;
  max-width: 900px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  padding: 28px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.8);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.3rem;
  color: #ffffff;
}

.username {
  color: #d4af37;
}

.subtitle {
  color: #7d7d8a;
  font-size: 0.85rem;
  margin: 4px 0 0;
}

.close-btn {
  background: none;
  border: none;
  color: #7d7d8a;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 4px;
}

.close-btn:hover {
  color: #ffffff;
}

.modal-body {
  overflow-y: auto;
  flex: 1;
}
</style>
