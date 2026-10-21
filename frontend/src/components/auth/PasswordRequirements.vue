<script setup>
import { computed } from 'vue';

const props = defineProps({
  password: {
    type: String,
    default: ''
  }
});

const rules = computed(() => [
  {
    label: 'At least 8 characters',
    valid: props.password.length >= 8
  },
  {
    label: 'At least 1 uppercase letter (A-Z)',
    valid: /[A-Z]/.test(props.password)
  },
  {
    label: 'At least 1 lowercase letter (a-z)',
    valid: /[a-z]/.test(props.password)
  },
  {
    label: 'At least 1 number (0-9)',
    valid: /[0-9]/.test(props.password)
  },
  {
    label: 'At least 1 special character (@#$%^&+=!)',
    valid: /[@#$%^&+=!]/.test(props.password)
  }
]);
</script>

<template>
  <div class="requirements-box">
    <div
      v-for="(rule, index) in rules"
      :key="index"
      class="requirement-item"
      :class="{ 'is-met': rule.valid }"
    >
      <span class="icon">{{ rule.valid ? '✓' : '○' }}</span>
      <span class="text">{{ rule.label }}</span>
    </div>
  </div>
</template>

<style scoped>
.requirements-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 14px;
  background: rgba(20, 20, 24, 0.7);
  border: 1px solid rgba(212, 175, 55, 0.15);
  border-radius: 8px;
  margin-top: -6px;
}

.requirement-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.78rem;
  color: #6c6c78;
  transition: all 0.25s ease;
}

.requirement-item.is-met {
  color: #d4af37;
}

.icon {
  font-weight: 700;
  font-size: 0.85rem;
  width: 14px;
  text-align: center;
}

.requirement-item.is-met .icon {
  color: #f5d77f;
  text-shadow: 0 0 6px rgba(212, 175, 55, 0.6);
}

.text {
  letter-spacing: 0.02em;
}
</style>
