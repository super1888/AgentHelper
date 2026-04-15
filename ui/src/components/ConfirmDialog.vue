<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'

withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    description: string
    confirmText?: string
    loading?: boolean
  }>(),
  {
    confirmText: '确认',
    loading: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [boolean]
  confirm: []
}>()
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="title"
    width="compact"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="confirm-dialog">
      <div class="confirm-dialog__icon" aria-hidden="true">
        <AlertTriangle :size="18" />
      </div>

      <p class="confirm-dialog__description">{{ description }}</p>
    </div>

    <template #footer>
      <button
        type="button"
        class="app-button app-button--secondary"
        :disabled="loading"
        @click="emit('update:modelValue', false)"
      >
        取消
      </button>

      <button
        type="button"
        class="app-button app-button--danger"
        :disabled="loading"
        :aria-busy="loading"
        @click="emit('confirm')"
      >
        <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
        {{ loading ? '处理中...' : confirmText }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.confirm-dialog {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.confirm-dialog__icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 38px;
  height: 38px;
  color: #b45309;
  border-radius: 14px;
  background: rgba(245, 158, 11, 0.14);
}

.confirm-dialog__description {
  margin: 0;
  color: var(--color-text-soft);
  line-height: 1.7;
}
</style>
