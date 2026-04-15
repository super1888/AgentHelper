<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { X } from 'lucide-vue-next'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    description?: string
    width?: 'compact' | 'regular' | 'wide'
    closeOnBackdrop?: boolean
  }>(),
  {
    description: '',
    width: 'regular',
    closeOnBackdrop: true,
  },
)

const emit = defineEmits<{
  'update:modelValue': [boolean]
}>()

const panelRef = ref<HTMLElement | null>(null)
const widthClass = computed(() => `app-dialog__panel--${props.width}`)

let previousOverflow = ''
let lastFocusedElement: HTMLElement | null = null

function close() {
  emit('update:modelValue', false)
}

function getFocusableElements() {
  const panel = panelRef.value
  if (!panel) {
    return []
  }

  const selectors = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
  ].join(',')

  return Array.from(panel.querySelectorAll<HTMLElement>(selectors)).filter(
    (element) => !element.hasAttribute('disabled') && element.getAttribute('aria-hidden') !== 'true',
  )
}

function focusFirstElement() {
  const [firstElement] = getFocusableElements()
  if (firstElement) {
    firstElement.focus()
    return
  }

  panelRef.value?.focus()
}

function handleKeydown(event: KeyboardEvent) {
  if (!props.modelValue) {
    return
  }

  if (event.key === 'Escape') {
    event.preventDefault()
    close()
    return
  }

  if (event.key !== 'Tab') {
    return
  }

  const focusableElements = getFocusableElements()
  if (focusableElements.length === 0) {
    event.preventDefault()
    panelRef.value?.focus()
    return
  }

  const firstElement = focusableElements[0]
  const lastElement = focusableElements[focusableElements.length - 1]
  const activeElement = document.activeElement

  if (event.shiftKey && activeElement === firstElement) {
    event.preventDefault()
    lastElement.focus()
  } else if (!event.shiftKey && activeElement === lastElement) {
    event.preventDefault()
    firstElement.focus()
  }
}

function openDialog() {
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', handleKeydown)
  void nextTick(() => {
    focusFirstElement()
  })
}

function cleanupDialog() {
  document.body.style.overflow = previousOverflow
  document.removeEventListener('keydown', handleKeydown)
  lastFocusedElement?.focus()
}

watch(
  () => props.modelValue,
  (isOpen, wasOpen) => {
    if (isOpen) {
      openDialog()
      return
    }

    if (wasOpen) {
      cleanupDialog()
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = previousOverflow
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="app-dialog"
      @click.self="closeOnBackdrop ? close() : undefined"
    >
      <div
        ref="panelRef"
        class="app-dialog__panel panel-card"
        :class="widthClass"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="`dialog-title-${title}`"
        tabindex="-1"
      >
        <header class="app-dialog__header">
          <div>
            <h2 :id="`dialog-title-${title}`">{{ title }}</h2>
            <p v-if="description">{{ description }}</p>
          </div>

          <button
            type="button"
            class="app-icon-button"
            aria-label="关闭弹窗"
            @click="close"
          >
            <X :size="18" aria-hidden="true" />
          </button>
        </header>

        <div class="app-dialog__body">
          <slot></slot>
        </div>

        <footer v-if="$slots.footer" class="app-dialog__footer">
          <slot name="footer"></slot>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.app-dialog {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(12px);
}

.app-dialog__panel {
  width: min(100%, 720px);
  max-height: calc(100vh - 48px);
  padding: 28px;
  overflow: auto;
}

.app-dialog__panel--compact {
  width: min(100%, 460px);
}

.app-dialog__panel--wide {
  width: min(100%, 920px);
}

.app-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.app-dialog__header h2 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 1.35rem;
}

.app-dialog__header p {
  margin: 8px 0 0;
  color: var(--color-text-soft);
  line-height: 1.6;
}

.app-dialog__body {
  margin-top: 22px;
}

.app-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .app-dialog {
    padding: 12px;
    align-items: flex-end;
  }

  .app-dialog__panel {
    width: 100%;
    max-height: calc(100vh - 24px);
    padding: 22px;
    border-radius: 24px 24px 0 0;
  }
}
</style>
