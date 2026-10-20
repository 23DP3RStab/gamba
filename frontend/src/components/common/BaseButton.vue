<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: 'button'
  }
});
</script>

<template>
  <button :type="type" :disabled="loading" class="gold-button">
    <span v-if="loading" class="spinner"></span>
    <span :class="{ 'transparent-text': loading }">
      <slot />
    </span>
  </button>
</template>

<style scoped>
.gold-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 13px 20px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #d4af37 0%, #aa771c 100%);
  color: #0b0b0e;
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(212, 175, 55, 0.2);
}

.gold-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #e5c158 0%, #c59b27 100%);
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.35);
  transform: translateY(-1px);
}

.gold-button:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 2px 10px rgba(212, 175, 55, 0.2);
}

.gold-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.transparent-text {
  opacity: 0;
}

.spinner {
  position: absolute;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(11, 11, 14, 0.3);
  border-top-color: #0b0b0e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
