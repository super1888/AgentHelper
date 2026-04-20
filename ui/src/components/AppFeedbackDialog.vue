<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'

type FeedbackTone = 'success' | 'error' | 'info'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    tone?: FeedbackTone
    title?: string
    message: string
    actionText?: string
  }>(),
  {
    tone: 'info',
    title: '',
    actionText: '知道了',
  },
)

const emit = defineEmits<{
  'update:modelValue': [boolean]
}>()

const dialogTitle = computed(() => {
  if (props.title) return props.title
  if (props.tone === 'success') return '操作成功'
  if (props.tone === 'error') return '操作失败'
  return '提示'
})

const dialogDescription = computed(() => {
  if (props.tone === 'success') return '系统已完成本次操作，请确认结果后继续。'
  if (props.tone === 'error') return '操作没有按预期完成，请根据提示修正后重试。'
  return '请查看本次操作提示。'
})

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="dialogTitle"
    :description="dialogDescription"
    width="compact"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="feedback-dialog" :class="`feedback-dialog--${tone}`">
      <div class="feedback-dialog__halo" aria-hidden="true"></div>
      <div class="feedback-dialog__icon" aria-hidden="true">
        <CheckCircle2 v-if="tone === 'success'" :size="28" />
        <XCircle v-else-if="tone === 'error'" :size="28" />
        <AlertTriangle v-else-if="tone === 'info'" :size="28" />
        <Info v-else :size="28" />
      </div>
      <p>{{ message }}</p>
    </div>

    <template #footer>
      <button type="button" class="app-button" @click="close">
        {{ actionText }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.feedback-dialog {
  position: relative;
  display: grid;
  gap: 18px;
  min-height: 170px;
  padding: 26px;
  overflow: hidden;
  border-radius: 26px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.035)),
    radial-gradient(circle at 12% 0%, rgba(102, 186, 255, 0.22), transparent 34%);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.feedback-dialog__halo {
  position: absolute;
  inset: auto -54px -72px auto;
  width: 190px;
  height: 190px;
  border-radius: 999px;
  filter: blur(4px);
  opacity: 0.38;
}

.feedback-dialog__icon {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 22px;
  color: var(--color-ink-strong);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.feedback-dialog p {
  position: relative;
  z-index: 1;
  margin: 0;
  color: var(--color-ink-strong);
  font-size: 1rem;
  line-height: 1.8;
  word-break: break-word;
}

.feedback-dialog--success .feedback-dialog__halo,
.feedback-dialog--success .feedback-dialog__icon {
  background: rgba(84, 214, 160, 0.22);
  color: #79e7b8;
}

.feedback-dialog--error .feedback-dialog__halo,
.feedback-dialog--error .feedback-dialog__icon {
  background: rgba(255, 112, 112, 0.22);
  color: #ff8b8b;
}

.feedback-dialog--info .feedback-dialog__halo,
.feedback-dialog--info .feedback-dialog__icon {
  background: rgba(102, 186, 255, 0.22);
  color: #8fd0ff;
}
</style>
