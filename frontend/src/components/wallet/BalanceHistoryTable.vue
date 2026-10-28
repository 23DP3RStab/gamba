<script setup>
defineProps({
  transactions: {
    type: Array,
    default: () => []
  },
  isLoading: {
    type: Boolean,
    default: false
  }
});

const formatDate = (isoString) => {
  if (!isoString) return '-';
  const d = new Date(isoString);
  return d.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const getTypeClass = (type) => {
  switch (type) {
    case 'WIN':
      return 'badge-win';
    case 'BET':
      return 'badge-bet';
    case 'ADMIN_ADJUSTMENT':
      return 'badge-admin';
    case 'DEPOSIT':
      return 'badge-deposit';
    case 'WITHDRAWAL':
      return 'badge-withdraw';
    default:
      return 'badge-default';
  }
};
</script>

<template>
  <div class="history-table-container">
    <div v-if="isLoading" class="table-status">Loading ledger history...</div>
    <div v-else-if="transactions.length === 0" class="table-status">No transactions recorded yet.</div>

    <div v-else class="table-wrapper">
      <table class="history-table">
        <thead>
          <tr>
            <th>Date & Time</th>
            <th>Type</th>
            <th>Description / Reason</th>
            <th>Amount</th>
            <th>Balance After</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in transactions" :key="tx.id">
            <td class="date-cell">{{ formatDate(tx.createdAt) }}</td>
            <td>
              <span class="type-badge" :class="getTypeClass(tx.type)">
                {{ tx.type.replace('_', ' ') }}
              </span>
            </td>
            <td class="desc-cell">{{ tx.description || '-' }}</td>
            <td class="amount-cell" :class="tx.amount >= 0 ? 'positive' : 'negative'">
              {{ tx.amount >= 0 ? '+' : '' }}{{ Number(tx.amount).toFixed(2) }} EUR
            </td>
            <td class="balance-after-cell">
              {{ Number(tx.balanceAfter).toFixed(2) }} EUR
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.history-table-container {
  background: #111116;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  overflow: hidden;
  width: 100%;
}

.table-wrapper {
  overflow-x: auto;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.9rem;
}

.history-table th {
  background: #141419;
  padding: 14px 18px;
  color: #c5a059;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  border-bottom: 1px solid rgba(212, 175, 55, 0.15);
}

.history-table td {
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  color: #dddddd;
}

.date-cell {
  color: #8c8c9a;
  white-space: nowrap;
  font-size: 0.85rem;
}

.desc-cell {
  color: #d1d1d6;
  max-width: 280px;
}

.type-badge {
  font-size: 0.7rem;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 4px;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.badge-win {
  background: rgba(38, 166, 154, 0.15);
  color: #4db6ac;
  border: 1px solid rgba(38, 166, 154, 0.3);
}

.badge-bet {
  background: rgba(230, 57, 70, 0.15);
  color: #ff6b6b;
  border: 1px solid rgba(230, 57, 70, 0.3);
}

.badge-admin {
  background: rgba(212, 175, 55, 0.2);
  color: #d4af37;
  border: 1px solid rgba(212, 175, 55, 0.4);
}

.badge-deposit {
  background: rgba(76, 175, 80, 0.15);
  color: #81c784;
}

.badge-withdraw {
  background: rgba(255, 152, 0, 0.15);
  color: #ffb74d;
}

.amount-cell {
  font-weight: 700;
  white-space: nowrap;
}

.amount-cell.positive {
  color: #4db6ac;
}

.amount-cell.negative {
  color: #ff6b6b;
}

.balance-after-cell {
  color: #d4af37;
  font-weight: 600;
  white-space: nowrap;
}

.table-status {
  text-align: center;
  color: #7d7d8a;
  padding: 32px 0;
  font-size: 0.95rem;
}
</style>
