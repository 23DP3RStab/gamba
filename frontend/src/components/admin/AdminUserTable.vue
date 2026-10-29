<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '../../stores/auth';

const props = defineProps({
  users: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(['toggle-status', 'adjust-balance', 'view-transactions']);

const searchQuery = ref('');
const authStore = useAuthStore();

const filteredUsers = computed(() => {
  const query = searchQuery.value.toLowerCase().trim();
  if (!query) return props.users;

  return props.users.filter(
    (u) =>
      u.username.toLowerCase().includes(query) ||
      u.email.toLowerCase().includes(query) ||
      u.role.toLowerCase().includes(query)
  );
});
</script>

<template>
  <div class="table-container">
    <div class="table-header-controls">
      <h3>Player Directory ({{ filteredUsers.length }})</h3>
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search by username or email..."
        class="search-input"
      />
    </div>

    <div class="table-wrapper">
      <table class="user-table">
        <thead>
          <tr>
            <th>Username</th>
            <th>Email</th>
            <th>Role</th>
            <th>Balance</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td class="user-cell">{{ user.username }}</td>
            <td class="email-cell">{{ user.email }}</td>
            <td>
              <span class="role-pill" :class="user.role.toLowerCase()">
                {{ user.role }}
              </span>
            </td>
            <td class="balance-cell">
              {{ Number(user.balance).toFixed(2) }} {{ user.currency }}
            </td>
            <td>
              <span class="status-badge" :class="user.active ? 'active' : 'suspended'">
                {{ user.active ? 'ACTIVE' : 'SUSPENDED' }}
              </span>
            </td>
            <td class="actions-cell">
              <button
                class="action-btn history"
                title="View Balance Ledger & History"
                @click="$emit('view-transactions', user)"
              >
                Ledger
              </button>
              <button
                class="action-btn adjust"
                title="Adjust Balance"
                @click="$emit('adjust-balance', user)"
              >
                ± Balance
              </button>
              <button
                v-if="String(user.id) !== String(authStore.user?.id)"
                class="action-btn"
                :class="user.active ? 'suspend' : 'activate'"
                @click="$emit('toggle-status', user.id)"
              >
                {{ user.active ? 'Suspend' : 'Activate' }}
              </button>
            </td>
          </tr>
          <tr v-if="filteredUsers.length === 0">
            <td colspan="6" class="no-results">No players found matching your criteria.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.table-container {
  background: #111116;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  overflow: hidden;
}

.table-header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(212, 175, 55, 0.15);
}

.table-header-controls h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #ffffff;
}

.search-input {
  background: #181820;
  border: 1px solid rgba(212, 175, 55, 0.25);
  border-radius: 8px;
  padding: 8px 14px;
  color: #f1f1f1;
  font-size: 0.9rem;
  outline: none;
  width: 260px;
}

.search-input:focus {
  border-color: #d4af37;
}

.table-wrapper {
  overflow-x: auto;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.9rem;
}

.user-table th {
  background: #141419;
  padding: 14px 20px;
  color: #c5a059;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
}

.user-table td {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  color: #dddddd;
}

.user-cell {
  font-weight: 600;
  color: #ffffff;
}

.email-cell {
  color: #8c8c9a;
}

.balance-cell {
  color: #d4af37;
  font-weight: 700;
}

.role-pill {
  font-size: 0.75rem;
  padding: 3px 8px;
  border-radius: 4px;
  text-transform: uppercase;
  font-weight: 700;
}

.role-pill.admin {
  background: rgba(212, 175, 55, 0.2);
  color: #d4af37;
  border: 1px solid rgba(212, 175, 55, 0.4);
}

.role-pill.player {
  background: rgba(255, 255, 255, 0.08);
  color: #a0a0ab;
}

.status-badge {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
}

.status-badge.active {
  background: rgba(38, 166, 154, 0.15);
  color: #4db6ac;
}

.status-badge.suspended {
  background: rgba(230, 57, 70, 0.15);
  color: #ff6b6b;
}

.actions-cell {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.action-btn.history {
  background: rgba(212, 175, 55, 0.1);
  color: #d4af37;
  border: 1px solid rgba(212, 175, 55, 0.25);
}

.action-btn.history:hover {
  background: rgba(212, 175, 55, 0.25);
}

.action-btn.adjust {
  background: rgba(212, 175, 55, 0.15);
  color: #d4af37;
  border: 1px solid rgba(212, 175, 55, 0.3);
}

.action-btn.adjust:hover {
  background: rgba(212, 175, 55, 0.3);
}

.action-btn.suspend {
  background: rgba(230, 57, 70, 0.12);
  color: #ff6b6b;
  border: 1px solid rgba(230, 57, 70, 0.3);
}

.action-btn.suspend:hover {
  background: rgba(230, 57, 70, 0.25);
}

.action-btn.activate {
  background: rgba(38, 166, 154, 0.12);
  color: #4db6ac;
  border: 1px solid rgba(38, 166, 154, 0.3);
}

.action-btn.activate:hover {
  background: rgba(38, 166, 154, 0.25);
}

.no-results {
  text-align: center;
  color: #7d7d8a;
  padding: 32px 0;
}
</style>
