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
  letter-spacing: 0.02em;
}

.status-badge--enabled {
  color: #dcfff4;
  border-color: rgba(100, 216, 190, 0.26);
  background: rgba(100, 216, 190, 0.16);
}

.status-badge--disabled {
  color: #ffe2c5;
  border-color: rgba(255, 181, 108, 0.22);
  background: rgba(255, 181, 108, 0.14);
}
</style>
