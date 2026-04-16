<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { Building2, ContactRound, Hash, Phone, ScrollText } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'
import type { CreateTenantPayload, TenantProfile, UpdateTenantPayload } from '@/types/tenant'
import {
  normalizeOptionalText,
  validateTenantForm,
  type TenantFormState,
} from '@/utils/validation'

type DialogMode = 'create' | 'edit'
type TenantField = keyof TenantFormState

const props = defineProps<{
  modelValue: boolean
  mode: DialogMode
  tenant: TenantProfile | null
  submitting: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  submit: [{ mode: DialogMode; payload: CreateTenantPayload | UpdateTenantPayload }]
}>()

const form = reactive<TenantFormState>(createEmptyForm())
const errors = reactive<Partial<Record<TenantField, string>>>({})

const dialogTitle = computed(() => (props.mode === 'create' ? '新增租户' : '编辑租户'))
const dialogDescription = computed(() =>
  props.mode === 'create'
    ? '创建新的租户边界，用于承载用户、Agent 和会话数据。'
    : '维护租户名称、编码、联系人与启用状态。',
)

function createEmptyForm(): TenantFormState {
  return {
    tenantCode: '',
    tenantName: '',
    contactName: '',
    contactPhone: '',
    description: '',
    status: 1,
  }
}

function clearErrors() {
  for (const key of Object.keys(errors) as TenantField[]) {
    delete errors[key]
  }
}

function syncFormWithProps() {
  clearErrors()

  if (props.mode === 'edit' && props.tenant) {
    form.tenantCode = props.tenant.tenantCode
    form.tenantName = props.tenant.tenantName
    form.contactName = props.tenant.contactName ?? ''
    form.contactPhone = props.tenant.contactPhone ?? ''
    form.description = props.tenant.description ?? ''
    form.status = props.tenant.status
    return
  }

  Object.assign(form, createEmptyForm())
}

function handleSubmit() {
  clearErrors()
  const nextErrors = validateTenantForm(form)
  Object.assign(errors, nextErrors)
  if (Object.keys(nextErrors).length > 0) {
    return
  }

  const payload: CreateTenantPayload | UpdateTenantPayload = {
    tenantCode: form.tenantCode.trim(),
    tenantName: form.tenantName.trim(),
    contactName: normalizeOptionalText(form.contactName),
    contactPhone: normalizeOptionalText(form.contactPhone),
    description: normalizeOptionalText(form.description),
    status: form.status,
  }
  emit('submit', { mode: props.mode, payload })
}

watch(
  () => [props.modelValue, props.mode, props.tenant] as const,
  ([isOpen]) => {
    if (isOpen) {
      syncFormWithProps()
    }
  },
  { immediate: true },
)
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="dialogTitle"
    :description="dialogDescription"
    width="wide"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <form class="tenant-form" @submit.prevent="handleSubmit">
      <div v-if="mode === 'edit' && tenant" class="tenant-form__identity">
        <div class="tenant-form__identity-card">
          <span class="tenant-form__identity-label">租户 ID</span>
          <strong>#{{ tenant.id }}</strong>
        </div>

        <div class="tenant-form__identity-card">
          <span class="tenant-form__identity-label">当前成员</span>
          <strong>{{ tenant.memberCount }} 人</strong>
        </div>
      </div>

      <div class="tenant-form__grid">
        <label class="field">
          <span class="field__label">租户编码</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.tenantCode) }">
            <span class="input-shell__icon" aria-hidden="true">
              <Hash :size="16" />
            </span>
            <input
              v-model="form.tenantCode"
              class="app-input"
              type="text"
              placeholder="例如 HEADQUARTER"
              :disabled="submitting"
              @input="delete errors.tenantCode"
            />
          </div>
          <span v-if="errors.tenantCode" class="field__error">{{ errors.tenantCode }}</span>
        </label>

        <label class="field">
          <span class="field__label">租户名称</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.tenantName) }">
            <span class="input-shell__icon" aria-hidden="true">
              <Building2 :size="16" />
            </span>
            <input
              v-model="form.tenantName"
              class="app-input"
              type="text"
              placeholder="例如 总部租户"
              :disabled="submitting"
              @input="delete errors.tenantName"
            />
          </div>
          <span v-if="errors.tenantName" class="field__error">{{ errors.tenantName }}</span>
        </label>

        <label class="field">
          <span class="field__label">联系人</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.contactName) }">
            <span class="input-shell__icon" aria-hidden="true">
              <ContactRound :size="16" />
            </span>
            <input
              v-model="form.contactName"
              class="app-input"
              type="text"
              placeholder="可选"
              :disabled="submitting"
              @input="delete errors.contactName"
            />
          </div>
          <span v-if="errors.contactName" class="field__error">{{ errors.contactName }}</span>
        </label>

        <label class="field">
          <span class="field__label">联系电话</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.contactPhone) }">
            <span class="input-shell__icon" aria-hidden="true">
              <Phone :size="16" />
            </span>
            <input
              v-model="form.contactPhone"
              class="app-input"
              type="text"
              placeholder="11 位手机号"
              :disabled="submitting"
              @input="delete errors.contactPhone"
            />
          </div>
          <span v-if="errors.contactPhone" class="field__error">{{ errors.contactPhone }}</span>
        </label>

        <label class="field field--full">
          <span class="field__label">租户描述</span>
          <div
            class="input-shell input-shell--textarea"
            :class="{ 'input-shell--invalid': Boolean(errors.description) }"
          >
            <span class="input-shell__icon input-shell__icon--textarea" aria-hidden="true">
              <ScrollText :size="16" />
            </span>
            <textarea
              v-model="form.description"
              class="app-textarea"
              rows="4"
              placeholder="描述租户用途、组织范围或使用场景"
              :disabled="submitting"
              @input="delete errors.description"
            />
          </div>
          <span v-if="errors.description" class="field__error">{{ errors.description }}</span>
        </label>
      </div>

      <fieldset class="field field--stacked">
        <legend class="field__label">状态</legend>
        <div class="status-toggle">
          <label class="status-toggle__option" :class="{ 'status-toggle__option--active': form.status === 1 }">
            <input v-model="form.status" type="radio" :value="1" :disabled="submitting" />
            <span>启用</span>
          </label>
          <label class="status-toggle__option" :class="{ 'status-toggle__option--active': form.status === 0 }">
            <input v-model="form.status" type="radio" :value="0" :disabled="submitting" />
            <span>禁用</span>
          </label>
        </div>
      </fieldset>
    </form>

    <template #footer>
      <button
        type="button"
        class="app-button app-button--secondary"
        :disabled="submitting"
        @click="emit('update:modelValue', false)"
      >
        取消
      </button>
      <button
        type="button"
        class="app-button"
        :disabled="submitting"
        :aria-busy="submitting"
        @click="handleSubmit"
      >
        <span v-if="submitting" class="button-spinner" aria-hidden="true"></span>
        {{ submitting ? '提交中...' : mode === 'create' ? '创建租户' : '保存修改' }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.tenant-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.tenant-form__identity {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.tenant-form__identity-card {
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.04);
}

.tenant-form__identity-label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-ink-muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.tenant-form__identity strong {
  color: var(--color-ink-strong);
}

.tenant-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field--full {
  grid-column: 1 / -1;
}

.field--stacked {
  padding: 0;
  margin: 0;
  border: 0;
}

.input-shell--textarea {
  align-items: flex-start;
  min-height: 132px;
}

.input-shell__icon--textarea {
  padding-top: 18px;
}

.app-textarea {
  width: 100%;
  min-height: 132px;
  padding: 16px 18px 16px 14px;
  color: var(--color-ink-strong);
  background: transparent;
  border: 0;
  outline: 0;
  resize: vertical;
}

.app-textarea::placeholder {
  color: rgba(166, 183, 211, 0.56);
}

.status-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.status-toggle__option {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 54px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms ease;
}

.status-toggle__option:hover {
  border-color: rgba(83, 184, 255, 0.22);
  background: rgba(255, 255, 255, 0.06);
}

.status-toggle__option--active {
  color: var(--color-ink-strong);
  border-color: rgba(83, 184, 255, 0.28);
  background: rgba(83, 184, 255, 0.12);
  transform: translateY(-1px);
}

.status-toggle__option input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

@media (max-width: 720px) {
  .tenant-form__grid,
  .tenant-form__identity,
  .status-toggle {
    grid-template-columns: 1fr;
  }
}
</style>
