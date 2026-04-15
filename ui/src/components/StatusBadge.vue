<script setup lang="ts">
import { computed } from 'vue'
import type { UserStatus } from '@/types/user'

const props = defineProps<{
  status: UserStatus
}>()

const label = computed(() => (props.status === 1 ? '启用' : '禁用'))
</script>

<template>
  <span
    class="status-badge"
    :class="status === 1 ? 'status-badge--enabled' : 'status-badge--disabled'"
  >
    {{ label }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
}

.status-badge--enabled {
  color: #047857;
  border-color: rgba(16, 185, 129, 0.22);
  background: rgba(16, 185, 129, 0.12);
}

.status-badge--disabled {
  color: #b45309;
  border-color: rgba(245, 158, 11, 0.24);
  background: rgba(245, 158, 11, 0.14);
}
</style>
