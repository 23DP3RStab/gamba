<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  error: {
    type: String,
    default: ''
  },
  id: {
    type: String,
    required: true
  }
});

defineEmits(['update:modelValue']);
</script>

<template>
  <div class="input-group">
    <label :for="id" class="input-label">{{ label }}</label>
    <div class="input-wrapper" :class="{ 'has-error': error }">
      <input
        :id="id"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        @input="$emit('update:modelValue', $event.target.value)"
        class="base-input"
      />
    </div>
    <span v-if="error" class="error-text">{{ error }}</span>
  </div>
</template>

<style scoped>
.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.input-label {
  font-size: 0.85rem;
  font-weight: 500;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #c5a059;
}

.input-wrapper {
  position: relative;
  border-radius: 8px;
  background: #141417;
  border: 1px solid rgba(197, 160, 89, 0.25);
  transition: all 0.25s ease;
}

.input-wrapper:hover {
  border-color: rgba(212, 175, 55, 0.5);
}

.input-wrapper:focus-within {
  border-color: #d4af37;
  box-shadow: 0 0 12px rgba(212, 175, 55, 0.25);
}

.input-wrapper.has-error {
  border-color: #e63946;
  box-shadow: 0 0 8px rgba(230, 57, 70, 0.25);
}

.base-input {
  width: 100%;
  padding: 12px 16px;
  background: transparent;
  border: none;
  color: #f1f1f1;
  font-size: 0.95rem;
  outline: none;
  box-sizing: border-box;
}

.base-input::placeholder {
  color: #555562;
}

.error-text {
  font-size: 0.75rem;
  color: #e63946;
  margin-top: 2px;
}
</style>