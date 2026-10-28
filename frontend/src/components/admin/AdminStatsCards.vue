<script setup>
defineProps({
  stats: {
    type: Object,
    default: () => ({
      totalUsers: 0,
      activeUsers: 0,
      suspendedUsers: 0,
      totalPlayerFunds: 0
    })
  }
});
</script>

<template>
  <div class="stats-grid">
    <div class="stat-card">
      <div class="stat-icon">👥</div>
      <div class="stat-content">
        <span class="stat-label">Total Players</span>
        <span class="stat-value">{{ stats.totalUsers }}</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon success">✓</div>
      <div class="stat-content">
        <span class="stat-label">Active Accounts</span>
        <span class="stat-value text-success">{{ stats.activeUsers }}</span>
      </div>
    </div>

    <div class="stat-card">
      <div class="stat-icon warning">⊘</div>
      <div class="stat-content">
        <span class="stat-label">Suspended</span>
        <span class="stat-value text-warning">{{ stats.suspendedUsers }}</span>
      </div>
    </div>

    <div class="stat-card highlight">
      <div class="stat-icon gold">💰</div>
      <div class="stat-content">
        <span class="stat-label">Total Player Funds</span>
        <span class="stat-value gold-text">
          {{ Number(stats.totalPlayerFunds || 0).toFixed(2) }} EUR
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-bottom: 28px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #121217;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  padding: 20px 24px;
}

.stat-card.highlight {
  border-color: rgba(212, 175, 55, 0.5);
  background: radial-gradient(circle at top left, #1a1711, #121217);
}

.stat-icon {
  font-size: 1.8rem;
  background: #1a1a22;
  border-radius: 10px;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.8rem;
  text-transform: uppercase;
  color: #7d7d8a;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ffffff;
  margin-top: 4px;
}

.gold-text {
  color: #d4af37;
}

.text-success {
  color: #4db6ac;
}

.text-warning {
  color: #ff6b6b;
}
</style>
