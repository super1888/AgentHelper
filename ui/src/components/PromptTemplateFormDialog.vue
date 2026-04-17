<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { FileCode2, FileText, FolderOpen, Hash, ScrollText } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'
import type {
  PromptTemplateItem,
  PromptTemplatePayload,
  PromptTemplateSourceType,
  PromptTemplateStatus,
  PromptTemplateVariable,
} from '@/types/prompt'

type DialogMode = 'create' | 'edit'

interface PromptTemplateFormState {
  templateCode: string
  templateName: string
  description: string
  sourceType: PromptTemplateSourceType
  templateContent: string
  sourcePath: string
  templateStatus: PromptTemplateStatus
  variableDefinitions: PromptTemplateVariable[]
}

type FormField = keyof Omit<PromptTemplateFormState, 'variableDefinitions'>

const props = defineProps<{
  modelValue: boolean
  mode: DialogMode
  template: PromptTemplateItem | null
  submitting: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  submit: [PromptTemplatePayload]
}>()

const form = reactive<PromptTemplateFormState>(createEmptyForm())
const errors = reactive<Partial<Record<FormField, string>>>({})

const dialogTitle = computed(() => (props.mode === 'create' ? '新建提示词模板' : '编辑提示词模板'))
const dialogDescription = computed(() => (
  props.mode === 'create'
    ? '沉淀可复用的系统提示词资产，统一 Agent 的角色边界、输出规范和安全约束。'
    : '维护模板名称、启停状态、变量元数据和提示词来源，让模板资产持续运营。'
))
const inlineLength = computed(() => form.templateContent.trim().length)
const extractedVariableNames = computed(() => {
  if (form.sourceType !== 'INLINE_TEXT') {
    return []
  }
  const matches = form.templateContent.match(/\{\{\s*[a-zA-Z][a-zA-Z0-9_]*\s*}}/g) ?? []
  return [...new Set(matches.map((item) => item.replace(/[{}\s]/g, '')))]
})

function createEmptyForm(): PromptTemplateFormState {
  return {
    templateCode: '',
    templateName: '',
    description: '',
    sourceType: 'INLINE_TEXT',
    templateContent: '',
    sourcePath: '',
    templateStatus: 'ENABLED',
    variableDefinitions: [],
  }
}

function createVariableDefinition(variableName = ''): PromptTemplateVariable {
  return {
    variableName,
    required: true,
    defaultValue: null,
    description: null,
  }
}

function clearErrors() {
  for (const key of Object.keys(errors) as FormField[]) {
    delete errors[key]
  }
}

function syncFormWithProps() {
  clearErrors()

  if (!props.template) {
    Object.assign(form, createEmptyForm())
    return
  }

  form.templateCode = props.template.templateCode
  form.templateName = props.template.templateName
  form.description = props.template.description ?? ''
  form.sourceType = props.template.sourceType
  form.templateContent = props.template.templateContent ?? ''
  form.sourcePath = props.template.sourcePath ?? ''
  form.templateStatus = props.mode === 'edit' ? props.template.templateStatus : 'ENABLED'
  form.variableDefinitions = (props.template.variableDefinitions ?? []).map((item) => ({ ...item }))

  if (props.mode === 'create' && form.variableDefinitions.length === 0) {
    syncVariableDefinitionsWithInlineContent()
  }
}

function syncVariableDefinitionsWithInlineContent() {
  if (form.sourceType !== 'INLINE_TEXT') {
    return
  }
  const existingMap = new Map(form.variableDefinitions.map((item) => [item.variableName, item]))
  form.variableDefinitions = extractedVariableNames.value.map((variableName) => {
    const existing = existingMap.get(variableName)
    return existing ? { ...existing } : createVariableDefinition(variableName)
  })
}

function addManualVariable() {
  form.variableDefinitions.push(createVariableDefinition(''))
}

function removeVariable(index: number) {
  form.variableDefinitions.splice(index, 1)
}

function validateForm() {
  clearErrors()

  if (props.mode === 'create' && !form.templateCode.trim()) {
    errors.templateCode = '请输入模板编码'
  }
  if (!form.templateName.trim()) {
    errors.templateName = '请输入模板名称'
  }
  if (form.sourceType === 'INLINE_TEXT' && !form.templateContent.trim()) {
    errors.templateContent = '请输入模板正文'
  }
  if (form.sourceType === 'FILE_PATH' && !form.sourcePath.trim()) {
    errors.sourcePath = '请输入模板文件路径'
  }

  return Object.keys(errors).length === 0
}

function normalizeVariableDefinitions() {
  return form.variableDefinitions
    .map((item) => ({
      variableName: item.variableName.trim(),
      required: Boolean(item.required),
      defaultValue: item.defaultValue?.trim() || null,
      description: item.description?.trim() || null,
    }))
    .filter((item) => item.variableName)
}

function handleSubmit() {
  if (!validateForm()) {
    return
  }

  emit('submit', {
    templateCode: props.mode === 'create' ? form.templateCode.trim() : undefined,
    templateName: form.templateName.trim(),
    description: form.description.trim() || null,
    sourceType: form.sourceType,
    templateContent: form.sourceType === 'INLINE_TEXT' ? form.templateContent.trim() : null,
    sourcePath: form.sourceType === 'FILE_PATH' ? form.sourcePath.trim() : null,
    templateStatus: form.templateStatus,
    variableDefinitions: normalizeVariableDefinitions(),
  })
}

watch(
  () => [props.modelValue, props.mode, props.template] as const,
  ([visible]) => {
    if (visible) {
      syncFormWithProps()
    }
  },
  { immediate: true },
)

watch(
  () => [form.templateContent, form.sourceType] as const,
  () => {
    if (form.sourceType === 'INLINE_TEXT') {
      syncVariableDefinitionsWithInlineContent()
    }
  },
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
    <form class="template-form" @submit.prevent="handleSubmit">
      <section class="panel">
        <div class="grid">
          <label class="field">
            <span class="field__label">模板编码</span>
            <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.templateCode) }">
              <span class="input-shell__icon"><Hash :size="16" /></span>
              <input
                v-model="form.templateCode"
                class="app-input"
                type="text"
                maxlength="64"
                :disabled="submitting || mode === 'edit'"
                placeholder="例如 CUSTOMER_SERVICE_STANDARD"
                @input="delete errors.templateCode"
              />
            </div>
            <span v-if="errors.templateCode" class="field__error">{{ errors.templateCode }}</span>
          </label>

          <label class="field">
            <span class="field__label">模板名称</span>
            <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.templateName) }">
              <span class="input-shell__icon"><FileCode2 :size="16" /></span>
              <input
                v-model="form.templateName"
                class="app-input"
                type="text"
                maxlength="128"
                :disabled="submitting"
                placeholder="例如 客服标准应答模板"
                @input="delete errors.templateName"
              />
            </div>
            <span v-if="errors.templateName" class="field__error">{{ errors.templateName }}</span>
          </label>

          <label class="field field--full">
            <span class="field__label">模板描述</span>
            <div class="input-shell input-shell--textarea">
              <span class="input-shell__icon input-shell__icon--textarea"><ScrollText :size="16" /></span>
              <textarea
                v-model="form.description"
                class="app-textarea"
                rows="3"
                maxlength="500"
                :disabled="submitting"
                placeholder="说明模板适用业务、输出风格、边界约束和建议绑定的 Agent 类型。"
              />
            </div>
          </label>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <strong>来源策略</strong>
            <p>模板变量统一使用 <code>&#123;&#123;variableName&#125;&#125;</code> 规范。</p>
          </div>
        </div>

        <fieldset class="field field--full field--stacked">
          <legend class="field__label">模板来源</legend>
          <div class="source-grid">
            <label class="source-card" :class="{ 'source-card--active': form.sourceType === 'INLINE_TEXT' }">
              <input v-model="form.sourceType" type="radio" value="INLINE_TEXT" />
              <FileText :size="15" />
              <span>内联文本</span>
            </label>
            <label class="source-card" :class="{ 'source-card--active': form.sourceType === 'FILE_PATH' }">
              <input v-model="form.sourceType" type="radio" value="FILE_PATH" />
              <FolderOpen :size="15" />
              <span>文件路径</span>
            </label>
          </div>
        </fieldset>

        <label v-if="form.sourceType === 'INLINE_TEXT'" class="field">
          <div class="field__topline">
            <span class="field__label">模板正文</span>
            <span class="field__hint">当前 {{ inlineLength }} / 20000 字符</span>
          </div>
          <div class="input-shell input-shell--textarea" :class="{ 'input-shell--invalid': Boolean(errors.templateContent) }">
            <span class="input-shell__icon input-shell__icon--textarea"><FileText :size="16" /></span>
            <textarea
              v-model="form.templateContent"
              class="app-textarea app-textarea--large"
              rows="10"
              :disabled="submitting"
              placeholder="输入系统提示词模板正文。"
              @input="delete errors.templateContent"
            />
          </div>
          <span v-if="errors.templateContent" class="field__error">{{ errors.templateContent }}</span>
          <span v-else class="field__hint">模板中出现的 <code>&#123;&#123;variableName&#125;&#125;</code> 会自动同步到变量元数据区域。</span>
        </label>

        <label v-else class="field">
          <span class="field__label">模板文件路径</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.sourcePath) }">
            <span class="input-shell__icon"><FolderOpen :size="16" /></span>
            <input
              v-model="form.sourcePath"
              class="app-input"
              type="text"
              :disabled="submitting"
              placeholder="例如 D:/code/springAi/prompts/customer-service.md"
              @input="delete errors.sourcePath"
            />
          </div>
          <span v-if="errors.sourcePath" class="field__error">{{ errors.sourcePath }}</span>
          <span v-else class="field__hint">文件模板需要手动维护变量元数据，后端会校验文件可读且占位符合法。</span>
        </label>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <strong>变量元数据</strong>
            <p>为每个变量补充必填、默认值和说明，供 Agent 创建页动态生成表单。</p>
          </div>
          <button
            v-if="form.sourceType === 'FILE_PATH'"
            type="button"
            class="app-button app-button--ghost"
            :disabled="submitting"
            @click="addManualVariable"
          >
            新增变量
          </button>
        </div>

        <div v-if="form.variableDefinitions.length === 0" class="empty-state">
          当前没有变量。内联模板可直接在正文里输入 <code>&#123;&#123;variableName&#125;&#125;</code>，文件模板可手动新增变量。
        </div>

        <div v-else class="variable-grid">
          <article v-for="(item, index) in form.variableDefinitions" :key="`${item.variableName}-${index}`" class="variable-card">
            <div class="variable-card__head">
              <strong>变量 {{ index + 1 }}</strong>
              <button
                v-if="form.sourceType === 'FILE_PATH'"
                type="button"
                class="app-button app-button--ghost app-button--danger-ghost"
                :disabled="submitting"
                @click="removeVariable(index)"
              >
                删除
              </button>
            </div>

            <div class="grid">
              <label class="field">
                <span class="field__label">变量名</span>
                <input
                  v-model="item.variableName"
                  class="app-input"
                  type="text"
                  :readonly="form.sourceType === 'INLINE_TEXT'"
                  placeholder="例如 userName"
                />
              </label>

              <label class="field">
                <span class="field__label">默认值</span>
                <input v-model="item.defaultValue" class="app-input" type="text" placeholder="未传值时自动使用" />
              </label>

              <label class="field">
                <span class="field__label">业务说明</span>
                <input v-model="item.description" class="app-input" type="text" placeholder="说明变量业务含义和建议取值" />
              </label>

              <label class="field">
                <span class="field__label">是否必填</span>
                <label class="checkbox-pill">
                  <input v-model="item.required" type="checkbox" />
                  <span>{{ item.required ? '必填' : '可选' }}</span>
                </label>
              </label>
            </div>
          </article>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <strong>发布控制</strong>
            <p>禁用后模板不会在 Agent 创建页中作为可选资产展示。</p>
          </div>
        </div>

        <fieldset class="field field--full field--stacked">
          <legend class="field__label">模板状态</legend>
          <div class="source-grid">
            <label class="source-card" :class="{ 'source-card--active': form.templateStatus === 'ENABLED' }">
              <input v-model="form.templateStatus" type="radio" value="ENABLED" />
              <span>启用</span>
            </label>
            <label class="source-card" :class="{ 'source-card--active': form.templateStatus === 'DISABLED' }">
              <input v-model="form.templateStatus" type="radio" value="DISABLED" />
              <span>禁用</span>
            </label>
          </div>
        </fieldset>
      </section>
    </form>

    <template #footer>
      <button type="button" class="app-button app-button--secondary" :disabled="submitting" @click="emit('update:modelValue', false)">
        取消
      </button>
      <button type="button" class="app-button" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : mode === 'create' ? '创建模板' : '保存修改' }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.template-form { display: flex; flex-direction: column; gap: 18px; }
.panel { padding: 20px; border: 1px solid rgba(255,255,255,.08); border-radius: 24px; background: rgba(255,255,255,.03); }
.grid,.variable-grid,.source-grid { display: grid; gap: 16px; }
.grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.variable-grid { grid-template-columns: 1fr; }
.source-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.field { display: flex; flex-direction: column; gap: 8px; }
.field--full,.field--stacked { grid-column: 1 / -1; }
.field--stacked { padding: 0; margin: 0; border: 0; }
.field__label,strong { color: var(--color-ink-strong); }
.field__topline,.panel-head,.variable-card__head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.field__hint,.panel-head p,.empty-state { margin: 0; color: var(--color-ink-soft); line-height: 1.6; }
.field__error { color: #ffb7b7; font-size: .85rem; }
.input-shell--textarea { align-items: flex-start; min-height: 132px; }
.input-shell__icon--textarea { padding-top: 18px; }
.app-textarea { width: 100%; min-height: 132px; padding: 16px 18px 16px 14px; color: var(--color-ink-strong); background: transparent; border: 0; outline: 0; resize: vertical; }
.app-textarea--large { min-height: 220px; }
.source-card,.checkbox-pill,.variable-card,.empty-state { border-radius: 18px; background: rgba(255,255,255,.04); }
.source-card { position: relative; display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 54px; padding: 0 18px; border: 1px solid rgba(255,255,255,.08); color: var(--color-ink-soft); cursor: pointer; }
.source-card--active { color: var(--color-ink-strong); border-color: rgba(83,184,255,.28); background: rgba(83,184,255,.12); }
.source-card input { position: absolute; opacity: 0; pointer-events: none; }
.empty-state,.variable-card { padding: 16px; }
.checkbox-pill { display: inline-flex; align-items: center; gap: 10px; min-height: 48px; padding: 0 14px; color: var(--color-ink-strong); }
.checkbox-pill input { accent-color: #53b8ff; }
.app-button--ghost { color: var(--color-ink-strong); background: rgba(255,255,255,.06); box-shadow: inset 0 0 0 1px rgba(255,255,255,.08); }
.app-button--danger-ghost { color: #ffd8d8; background: rgba(180,57,68,.14); box-shadow: inset 0 0 0 1px rgba(244,140,140,.16); }
@media (max-width: 720px) { .grid,.source-grid { grid-template-columns: 1fr; } .panel-head,.field__topline,.variable-card__head { flex-direction: column; align-items: stretch; } }
</style>
