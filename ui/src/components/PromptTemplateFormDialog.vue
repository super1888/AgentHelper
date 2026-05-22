<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { FileCode2, FileText, FolderOpen, Hash } from 'lucide-vue-next'
import AppDialog from '@/components/AppDialog.vue'
import type {
  PromptConditionalRule,
  PromptEnterpriseConfig,
  PromptLoopRule,
  PromptTemplateItem,
  PromptTemplatePayload,
  PromptTemplateSourceType,
  PromptTemplateStatus,
  PromptTemplateVariable,
} from '@/types/prompt'
import { createEmptyEnterpriseConfig } from '@/types/prompt'

type DialogMode = 'create' | 'edit'
type EnterpriseTab =
  | 'rendering'
  | 'role'
  | 'workflow'
  | 'security'
  | 'asset'
  | 'output'
  | 'context'
  | 'fallback'
  | 'observability'
  | 'integration'
type FormField = 'templateCode' | 'templateName' | 'templateContent' | 'sourcePath' | 'enterpriseJson'

interface FormState {
  templateCode: string
  templateName: string
  description: string
  sourceType: PromptTemplateSourceType
  templateContent: string
  sourcePath: string
  templateStatus: PromptTemplateStatus
  variableDefinitions: PromptTemplateVariable[]
  enterpriseConfig: PromptEnterpriseConfig
  text: Record<string, string>
}

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

const errors = reactive<Partial<Record<FormField, string>>>({})
const activeTab = ref<EnterpriseTab>('rendering')
const tabs: Array<{ key: EnterpriseTab; label: string }> = [
  { key: 'rendering', label: '动态渲染' },
  { key: 'role', label: '角色职责' },
  { key: 'workflow', label: '流程规则' },
  { key: 'security', label: '安全合规' },
  { key: 'asset', label: '资产管理' },
  { key: 'output', label: '输出标准' },
  { key: 'context', label: '上下文' },
  { key: 'fallback', label: '兜底' },
  { key: 'observability', label: '观测' },
  { key: 'integration', label: '集成' },
]

const form = reactive<FormState>(createEmptyForm())
const dialogTitle = computed(() => (props.mode === 'create' ? '新建企业模板' : '编辑企业模板'))
const inlineLength = computed(() => form.templateContent.trim().length)
const extractedVariableNames = computed(() => {
  if (form.sourceType !== 'INLINE_TEXT') {
    return []
  }
  const matches = form.templateContent.match(/\{\{\s*[a-zA-Z][a-zA-Z0-9_]*\s*}}|\{[a-zA-Z][a-zA-Z0-9_]*}/g) ?? []
  return [...new Set(matches.map((item) => item.replace(/[{}]/g, '').trim()).filter(Boolean))]
})

function createEmptyForm(): FormState {
  return {
    templateCode: '',
    templateName: '',
    description: '',
    sourceType: 'INLINE_TEXT',
    templateContent: '',
    sourcePath: '',
    templateStatus: 'ENABLED',
    variableDefinitions: [],
    enterpriseConfig: createEmptyEnterpriseConfig(),
    text: {
      dynamicVariables: '',
      dataSources: '',
      forbiddenActions: '',
      speechRules: '',
      workflowStages: '',
      hardRules: '',
      toolRulesJson: '[]',
      desensitizationRules: '',
      antiInjectionRules: '',
      complianceBlacklist: '',
      permissionTiers: '',
      commonModules: '',
      businessModules: '',
      categories: '',
      requiredFields: '',
      channelConstraints: '',
      memoryFields: '',
      fallbackMessages: '',
      repeatedRules: '',
      supportedLanguages: '',
      metricKeys: '',
      logBindingFields: '',
      externalSystems: '',
      parameterBindings: '',
      batchScenarios: '',
    },
  }
}

function lineText(values?: string[] | null) {
  return (values ?? []).join('\n')
}

function lineArray(value: string) {
  return value.split('\n').map((item) => item.trim()).filter(Boolean)
}

function cloneConfig(config?: PromptEnterpriseConfig | null) {
  return JSON.parse(JSON.stringify(config ?? createEmptyEnterpriseConfig())) as PromptEnterpriseConfig
}

function cloneVariables(values?: PromptTemplateVariable[] | null) {
  return (values ?? []).map((item) => ({ ...item }))
}

function variableItem(variableName = ''): PromptTemplateVariable {
  return { variableName, required: true, defaultValue: null, description: null }
}

function conditionalRuleItem(): PromptConditionalRule {
  return {
    name: null,
    conditionExpression: null,
    trueTemplate: null,
    falseTemplate: null,
  }
}

function loopRuleItem(): PromptLoopRule {
  return {
    listVariable: null,
    itemAlias: 'item',
    emptyTemplate: null,
    itemTemplate: null,
  }
}

function syncText(config: PromptEnterpriseConfig) {
  form.text.dynamicVariables = lineText(config.rendering.dynamicVariables)
  form.text.dataSources = lineText(config.rendering.dataSources)
  form.text.forbiddenActions = lineText(config.rolePolicy.forbiddenActions)
  form.text.speechRules = lineText(config.rolePolicy.speechRules)
  form.text.workflowStages = lineText(config.workflowPolicy.workflowStages)
  form.text.hardRules = lineText(config.workflowPolicy.hardRules)
  form.text.toolRulesJson = JSON.stringify(config.workflowPolicy.toolRules, null, 2)
  form.text.desensitizationRules = lineText(config.securityPolicy.desensitizationRules)
  form.text.antiInjectionRules = lineText(config.securityPolicy.antiInjectionRules)
  form.text.complianceBlacklist = lineText(config.securityPolicy.complianceBlacklist)
  form.text.permissionTiers = lineText(config.securityPolicy.permissionTiers)
  form.text.commonModules = lineText(config.assetPolicy.commonModules)
  form.text.businessModules = lineText(config.assetPolicy.businessModules)
  form.text.categories = lineText(config.assetPolicy.categories)
  form.text.requiredFields = lineText(config.outputPolicy.requiredFields)
  form.text.channelConstraints = lineText(config.outputPolicy.channelConstraints)
  form.text.memoryFields = lineText(config.contextPolicy.memoryFields)
  form.text.fallbackMessages = lineText(config.fallbackPolicy.fallbackMessages)
  form.text.repeatedRules = lineText(config.fallbackPolicy.repeatedRules)
  form.text.supportedLanguages = lineText(config.fallbackPolicy.supportedLanguages)
  form.text.metricKeys = lineText(config.observabilityPolicy.metricKeys)
  form.text.logBindingFields = lineText(config.observabilityPolicy.logBindingFields)
  form.text.externalSystems = lineText(config.integrationPolicy.externalSystems)
  form.text.parameterBindings = lineText(config.integrationPolicy.parameterBindings)
  form.text.batchScenarios = lineText(config.integrationPolicy.batchScenarios)
}

function syncForm() {
  Object.assign(form, createEmptyForm())
  Object.keys(errors).forEach((key) => delete errors[key as FormField])
  activeTab.value = 'rendering'
  if (!props.template) {
    return
  }
  form.templateCode = props.template.templateCode
  form.templateName = props.template.templateName
  form.description = props.template.description ?? ''
  form.sourceType = props.template.sourceType
  form.templateContent = props.template.templateContent ?? ''
  form.sourcePath = props.template.sourcePath ?? ''
  form.templateStatus = props.mode === 'edit' ? props.template.templateStatus : 'ENABLED'
  form.variableDefinitions = cloneVariables(props.template.variableDefinitions)
  form.enterpriseConfig = cloneConfig(props.template.enterpriseConfig)
  syncText(form.enterpriseConfig)
}

function syncInlineVariables() {
  if (form.sourceType !== 'INLINE_TEXT') {
    return
  }
  const existing = new Map(form.variableDefinitions.map((item) => [item.variableName, item]))
  form.variableDefinitions = extractedVariableNames.value.map((name) => existing.get(name) ?? variableItem(name))
}

function normalizedEnterpriseConfig() {
  let toolRulesJson: unknown
  try {
    toolRulesJson = JSON.parse(form.text.toolRulesJson)
  } catch {
    errors.enterpriseJson = '企业配置中的 JSON 字段格式不正确'
    return null
  }
  return {
    rendering: {
      dynamicVariables: lineArray(form.text.dynamicVariables),
      dataSources: lineArray(form.text.dataSources),
      conditionalBranches: normalizeConditionalRules(form.enterpriseConfig.rendering.conditionalBranches),
      loopRenderers: normalizeLoopRules(form.enterpriseConfig.rendering.loopRenderers),
    },
    rolePolicy: {
      agentRole: form.enterpriseConfig.rolePolicy.agentRole?.trim() || null,
      dutyScope: form.enterpriseConfig.rolePolicy.dutyScope?.trim() || null,
      forbiddenActions: lineArray(form.text.forbiddenActions),
      tone: form.enterpriseConfig.rolePolicy.tone?.trim() || null,
      speechRules: lineArray(form.text.speechRules),
    },
    workflowPolicy: {
      workflowStages: lineArray(form.text.workflowStages),
      hardRules: lineArray(form.text.hardRules),
      toolRules: Array.isArray(toolRulesJson) ? toolRulesJson : [],
    },
    securityPolicy: {
      desensitizationRules: lineArray(form.text.desensitizationRules),
      antiInjectionRules: lineArray(form.text.antiInjectionRules),
      complianceBlacklist: lineArray(form.text.complianceBlacklist),
      permissionTiers: lineArray(form.text.permissionTiers),
    },
    assetPolicy: {
      commonModules: lineArray(form.text.commonModules),
      businessModules: lineArray(form.text.businessModules),
      versionStrategy: form.enterpriseConfig.assetPolicy.versionStrategy?.trim() || null,
      permissionStrategy: form.enterpriseConfig.assetPolicy.permissionStrategy?.trim() || null,
      categories: lineArray(form.text.categories),
    },
    outputPolicy: {
      outputFormat: form.enterpriseConfig.outputPolicy.outputFormat?.trim() || null,
      requiredFields: lineArray(form.text.requiredFields),
      maxLength: form.enterpriseConfig.outputPolicy.maxLength ? Number(form.enterpriseConfig.outputPolicy.maxLength) : null,
      channelConstraints: lineArray(form.text.channelConstraints),
    },
    contextPolicy: {
      historyStrategy: form.enterpriseConfig.contextPolicy.historyStrategy?.trim() || null,
      memoryFields: lineArray(form.text.memoryFields),
      sessionIsolation: Boolean(form.enterpriseConfig.contextPolicy.sessionIsolation),
      retentionStrategy: form.enterpriseConfig.contextPolicy.retentionStrategy?.trim() || null,
    },
    fallbackPolicy: {
      fallbackMessages: lineArray(form.text.fallbackMessages),
      repeatedRules: lineArray(form.text.repeatedRules),
      supportedLanguages: lineArray(form.text.supportedLanguages),
      resilienceStrategy: form.enterpriseConfig.fallbackPolicy.resilienceStrategy?.trim() || null,
    },
    observabilityPolicy: {
      traceEnabled: Boolean(form.enterpriseConfig.observabilityPolicy.traceEnabled),
      metricKeys: lineArray(form.text.metricKeys),
      logBindingFields: lineArray(form.text.logBindingFields),
      grayReleaseStrategy: form.enterpriseConfig.observabilityPolicy.grayReleaseStrategy?.trim() || null,
    },
    integrationPolicy: {
      externalSystems: lineArray(form.text.externalSystems),
      parameterBindings: lineArray(form.text.parameterBindings),
      batchScenarios: lineArray(form.text.batchScenarios),
      editorMode: form.enterpriseConfig.integrationPolicy.editorMode?.trim() || null,
    },
  } satisfies PromptEnterpriseConfig
}

function normalizeConditionalRules(values: PromptConditionalRule[]) {
  return values
    .map((item) => ({
      name: item.name?.trim() || null,
      conditionExpression: item.conditionExpression?.trim() || null,
      trueTemplate: item.trueTemplate?.trim() || null,
      falseTemplate: item.falseTemplate?.trim() || null,
    }))
    .filter((item) => item.name || item.conditionExpression || item.trueTemplate || item.falseTemplate)
}

function normalizeLoopRules(values: PromptLoopRule[]) {
  return values
    .map((item) => ({
      listVariable: item.listVariable?.trim() || null,
      itemAlias: item.itemAlias?.trim() || 'item',
      emptyTemplate: item.emptyTemplate?.trim() || null,
      itemTemplate: item.itemTemplate?.trim() || null,
    }))
    .filter((item) => item.listVariable || item.emptyTemplate || item.itemTemplate)
}

function submitForm() {
  Object.keys(errors).forEach((key) => delete errors[key as FormField])
  if (props.mode === 'create' && !form.templateCode.trim()) errors.templateCode = '请输入模板编码'
  if (!form.templateName.trim()) errors.templateName = '请输入模板名称'
  if (form.sourceType === 'INLINE_TEXT' && !form.templateContent.trim()) errors.templateContent = '请输入模板正文'
  if (form.sourceType === 'FILE_PATH' && !form.sourcePath.trim()) errors.sourcePath = '请输入模板文件路径'
  const enterpriseConfig = normalizedEnterpriseConfig()
  if (Object.keys(errors).length > 0 || !enterpriseConfig) {
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
    variableDefinitions: form.variableDefinitions
      .map((item) => ({
        variableName: item.variableName.trim(),
        required: Boolean(item.required),
        defaultValue: item.defaultValue?.trim() || null,
        description: item.description?.trim() || null,
      }))
      .filter((item) => item.variableName),
    enterpriseConfig,
  })
}

watch(() => [props.modelValue, props.mode, props.template] as const, ([visible]) => { if (visible) syncForm() }, { immediate: true })
watch(() => [form.templateContent, form.sourceType] as const, () => { if (form.sourceType === 'INLINE_TEXT') syncInlineVariables() })
</script>

<template>
  <AppDialog :model-value="modelValue" :title="dialogTitle" description="统一维护变量、规则、权限、安全和集成能力。" width="wide" @update:model-value="emit('update:modelValue', $event)">
    <form class="template-form" @submit.prevent="submitForm">
      <section class="card">
        <div class="grid grid-2">
          <label class="field">
            <span class="field__label">模板编码</span>
            <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.templateCode) }">
              <span class="input-shell__icon"><Hash :size="16" /></span>
              <input v-model="form.templateCode" class="app-input" :disabled="submitting || mode === 'edit'" />
            </div>
            <span v-if="errors.templateCode" class="field__error">{{ errors.templateCode }}</span>
          </label>
          <label class="field">
            <span class="field__label">模板名称</span>
            <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.templateName) }">
              <span class="input-shell__icon"><FileCode2 :size="16" /></span>
              <input v-model="form.templateName" class="app-input" :disabled="submitting" />
            </div>
            <span v-if="errors.templateName" class="field__error">{{ errors.templateName }}</span>
          </label>
          <label class="field field--full">
            <span class="field__label">模板说明</span>
            <div class="input-shell input-shell--textarea"><textarea v-model="form.description" class="app-textarea" rows="3" /></div>
          </label>
        </div>
      </section>

      <section class="card">
        <div class="choice-row">
          <label class="choice" :class="{ 'choice--active': form.sourceType === 'INLINE_TEXT' }"><input v-model="form.sourceType" type="radio" value="INLINE_TEXT" /><FileText :size="16" /><span>内联文本</span></label>
          <label class="choice" :class="{ 'choice--active': form.sourceType === 'FILE_PATH' }"><input v-model="form.sourceType" type="radio" value="FILE_PATH" /><FolderOpen :size="16" /><span>文件路径</span></label>
          <label class="choice" :class="{ 'choice--active': form.templateStatus === 'ENABLED' }"><input v-model="form.templateStatus" type="radio" value="ENABLED" /><span>启用</span></label>
          <label class="choice" :class="{ 'choice--active': form.templateStatus === 'DISABLED' }"><input v-model="form.templateStatus" type="radio" value="DISABLED" /><span>停用</span></label>
        </div>
        <label v-if="form.sourceType === 'INLINE_TEXT'" class="field">
          <div class="section-head"><span class="field__label">模板正文</span><span class="hint">{{ inlineLength }} / 20000</span></div>
          <div class="input-shell input-shell--textarea" :class="{ 'input-shell--invalid': Boolean(errors.templateContent) }"><textarea v-model="form.templateContent" class="app-textarea code-area" rows="10" /></div>
          <span v-if="errors.templateContent" class="field__error">{{ errors.templateContent }}</span>
        </label>
        <label v-else class="field">
          <span class="field__label">模板文件路径</span>
          <div class="input-shell" :class="{ 'input-shell--invalid': Boolean(errors.sourcePath) }"><input v-model="form.sourcePath" class="app-input" /></div>
          <span v-if="errors.sourcePath" class="field__error">{{ errors.sourcePath }}</span>
        </label>
      </section>

      <section class="card">
        <div class="section-head">
          <strong>变量定义</strong>
          <span class="hint">支持 <code>&#123;user_id&#125;</code> 和 <code>&#123;&#123;user_id&#125;&#125;</code></span>
        </div>
        <div v-if="form.variableDefinitions.length === 0" class="empty">当前没有变量</div>
        <div v-else class="stack">
          <article v-for="(item, index) in form.variableDefinitions" :key="`${item.variableName}-${index}`" class="sub-card">
            <div class="grid grid-2">
              <label class="field"><span class="field__label">变量名</span><div class="input-shell"><input v-model="item.variableName" class="app-input" :readonly="form.sourceType === 'INLINE_TEXT'" /></div></label>
              <label class="field"><span class="field__label">默认值</span><div class="input-shell"><input v-model="item.defaultValue" class="app-input" /></div></label>
              <label class="field field--full"><span class="field__label">说明</span><div class="input-shell"><input v-model="item.description" class="app-input" /></div></label>
            </div>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="tabs"><button v-for="tab in tabs" :key="tab.key" type="button" class="tab" :class="{ 'tab--active': activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button></div>

        <div v-if="activeTab === 'rendering'" class="grid grid-2">
          <label class="field"><span class="field__label">动态变量清单</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.dynamicVariables" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">数据来源</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.dataSources" class="app-textarea" rows="6" /></div></label>
          <div class="field field--full rule-section">
            <div class="section-head">
              <div>
                <strong>条件分支</strong>
                <p class="hint">根据业务状态选择不同模板片段，例如 order_status == refund。</p>
              </div>
              <button type="button" class="app-button app-button--ghost" @click="form.enterpriseConfig.rendering.conditionalBranches.push(conditionalRuleItem())">新增条件</button>
            </div>
            <div v-if="form.enterpriseConfig.rendering.conditionalBranches.length === 0" class="empty">还没有条件分支规则</div>
            <div v-else class="stack">
              <article v-for="(item, index) in form.enterpriseConfig.rendering.conditionalBranches" :key="index" class="sub-card">
                <div class="section-head">
                  <strong>条件 {{ index + 1 }}</strong>
                  <button type="button" class="app-button app-button--ghost app-button--danger-ghost" @click="form.enterpriseConfig.rendering.conditionalBranches.splice(index, 1)">删除</button>
                </div>
                <div class="grid grid-2">
                  <label class="field"><span class="field__label">规则名称</span><div class="input-shell"><input v-model="item.name" class="app-input" placeholder="售后退款分支" /></div></label>
                  <label class="field"><span class="field__label">条件表达式</span><div class="input-shell"><input v-model="item.conditionExpression" class="app-input" placeholder="order_status == refund" /></div></label>
                  <label class="field"><span class="field__label">命中片段</span><div class="input-shell input-shell--textarea"><textarea v-model="item.trueTemplate" class="app-textarea" rows="4" placeholder="满足条件时启用的模板片段" /></div></label>
                  <label class="field"><span class="field__label">未命中片段</span><div class="input-shell input-shell--textarea"><textarea v-model="item.falseTemplate" class="app-textarea" rows="4" placeholder="不满足条件时的兜底片段" /></div></label>
                </div>
              </article>
            </div>
          </div>
          <div class="field field--full rule-section">
            <div class="section-head">
              <div>
                <strong>循环 / 列表渲染</strong>
                <p class="hint">用于订单列表、商品清单、审批节点等批量数据遍历。</p>
              </div>
              <button type="button" class="app-button app-button--ghost" @click="form.enterpriseConfig.rendering.loopRenderers.push(loopRuleItem())">新增循环</button>
            </div>
            <div v-if="form.enterpriseConfig.rendering.loopRenderers.length === 0" class="empty">还没有循环渲染规则</div>
            <div v-else class="stack">
              <article v-for="(item, index) in form.enterpriseConfig.rendering.loopRenderers" :key="index" class="sub-card">
                <div class="section-head">
                  <strong>循环 {{ index + 1 }}</strong>
                  <button type="button" class="app-button app-button--ghost app-button--danger-ghost" @click="form.enterpriseConfig.rendering.loopRenderers.splice(index, 1)">删除</button>
                </div>
                <div class="grid grid-2">
                  <label class="field"><span class="field__label">列表变量</span><div class="input-shell"><input v-model="item.listVariable" class="app-input" placeholder="order_list" /></div></label>
                  <label class="field"><span class="field__label">元素别名</span><div class="input-shell"><input v-model="item.itemAlias" class="app-input" placeholder="item" /></div></label>
                  <label class="field"><span class="field__label">单项片段</span><div class="input-shell input-shell--textarea"><textarea v-model="item.itemTemplate" class="app-textarea" rows="4" placeholder="商品：{item.name}，金额：{item.price}" /></div></label>
                  <label class="field"><span class="field__label">空列表片段</span><div class="input-shell input-shell--textarea"><textarea v-model="item.emptyTemplate" class="app-textarea" rows="4" placeholder="没有可展示的数据时输出" /></div></label>
                </div>
              </article>
            </div>
          </div>
        </div>
        <div v-else-if="activeTab === 'role'" class="grid grid-2">
          <label class="field"><span class="field__label">Agent 角色</span><div class="input-shell"><input v-model="form.enterpriseConfig.rolePolicy.agentRole" class="app-input" /></div></label>
          <label class="field"><span class="field__label">统一语气</span><div class="input-shell"><input v-model="form.enterpriseConfig.rolePolicy.tone" class="app-input" /></div></label>
          <label class="field field--full"><span class="field__label">职责边界</span><div class="input-shell input-shell--textarea"><textarea v-model="form.enterpriseConfig.rolePolicy.dutyScope" class="app-textarea" rows="4" /></div></label>
          <label class="field"><span class="field__label">禁止行为</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.forbiddenActions" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">话术规范</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.speechRules" class="app-textarea" rows="6" /></div></label>
        </div>
        <div v-else-if="activeTab === 'workflow'" class="grid grid-2">
          <label class="field"><span class="field__label">流程节点</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.workflowStages" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">硬规则约束</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.hardRules" class="app-textarea" rows="6" /></div></label>
          <label class="field field--full"><span class="field__label">工具规则 JSON</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.toolRulesJson" class="app-textarea code-area" rows="8" spellcheck="false" /></div></label>
        </div>
        <div v-else-if="activeTab === 'security'" class="grid grid-2">
          <label class="field"><span class="field__label">脱敏规则</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.desensitizationRules" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">抗注入规则</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.antiInjectionRules" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">黑名单</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.complianceBlacklist" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">权限分级</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.permissionTiers" class="app-textarea" rows="6" /></div></label>
        </div>
        <div v-else-if="activeTab === 'asset'" class="grid grid-2">
          <label class="field"><span class="field__label">公共模块</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.commonModules" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">业务模块</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.businessModules" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">版本策略</span><div class="input-shell input-shell--textarea"><textarea v-model="form.enterpriseConfig.assetPolicy.versionStrategy" class="app-textarea" rows="4" /></div></label>
          <label class="field"><span class="field__label">权限策略</span><div class="input-shell input-shell--textarea"><textarea v-model="form.enterpriseConfig.assetPolicy.permissionStrategy" class="app-textarea" rows="4" /></div></label>
          <label class="field field--full"><span class="field__label">分类标签</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.categories" class="app-textarea" rows="4" /></div></label>
        </div>
        <div v-else-if="activeTab === 'output'" class="grid grid-2">
          <label class="field"><span class="field__label">输出格式</span><div class="input-shell"><input v-model="form.enterpriseConfig.outputPolicy.outputFormat" class="app-input" /></div></label>
          <label class="field"><span class="field__label">最大长度</span><div class="input-shell"><input v-model="form.enterpriseConfig.outputPolicy.maxLength" class="app-input" type="number" min="0" /></div></label>
          <label class="field"><span class="field__label">必带字段</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.requiredFields" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">渠道约束</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.channelConstraints" class="app-textarea" rows="6" /></div></label>
        </div>
        <div v-else-if="activeTab === 'context'" class="grid grid-2">
          <label class="field"><span class="field__label">长上下文策略</span><div class="input-shell input-shell--textarea"><textarea v-model="form.enterpriseConfig.contextPolicy.historyStrategy" class="app-textarea" rows="4" /></div></label>
          <label class="field"><span class="field__label">保留策略</span><div class="input-shell input-shell--textarea"><textarea v-model="form.enterpriseConfig.contextPolicy.retentionStrategy" class="app-textarea" rows="4" /></div></label>
          <label class="field"><span class="field__label">记忆字段</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.memoryFields" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">会话隔离</span><label class="switch-line"><input v-model="form.enterpriseConfig.contextPolicy.sessionIsolation" type="checkbox" /><span>{{ form.enterpriseConfig.contextPolicy.sessionIsolation ? '开启' : '关闭' }}</span></label></label>
        </div>
        <div v-else-if="activeTab === 'fallback'" class="grid grid-2">
          <label class="field"><span class="field__label">兜底话术</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.fallbackMessages" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">重复强化规则</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.repeatedRules" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">支持语言</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.supportedLanguages" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">鲁棒性策略</span><div class="input-shell input-shell--textarea"><textarea v-model="form.enterpriseConfig.fallbackPolicy.resilienceStrategy" class="app-textarea" rows="6" /></div></label>
        </div>
        <div v-else-if="activeTab === 'observability'" class="grid grid-2">
          <label class="field"><span class="field__label">埋点指标</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.metricKeys" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">日志字段</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.logBindingFields" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">灰度策略</span><div class="input-shell input-shell--textarea"><textarea v-model="form.enterpriseConfig.observabilityPolicy.grayReleaseStrategy" class="app-textarea" rows="4" /></div></label>
          <label class="field"><span class="field__label">埋点开关</span><label class="switch-line"><input v-model="form.enterpriseConfig.observabilityPolicy.traceEnabled" type="checkbox" /><span>{{ form.enterpriseConfig.observabilityPolicy.traceEnabled ? '开启' : '关闭' }}</span></label></label>
        </div>
        <div v-else class="grid grid-2">
          <label class="field"><span class="field__label">外部系统</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.externalSystems" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">参数绑定</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.parameterBindings" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">批量场景</span><div class="input-shell input-shell--textarea"><textarea v-model="form.text.batchScenarios" class="app-textarea" rows="6" /></div></label>
          <label class="field"><span class="field__label">编辑模式</span><div class="input-shell"><input v-model="form.enterpriseConfig.integrationPolicy.editorMode" class="app-input" /></div></label>
        </div>
        <span v-if="errors.enterpriseJson" class="field__error">{{ errors.enterpriseJson }}</span>
      </section>
    </form>
    <template #footer>
      <button type="button" class="app-button app-button--secondary" :disabled="submitting" @click="emit('update:modelValue', false)">取消</button>
      <button type="button" class="app-button" :disabled="submitting" @click="submitForm">{{ submitting ? '提交中...' : mode === 'create' ? '创建模板' : '保存修改' }}</button>
    </template>
  </AppDialog>
</template>

<style scoped>
.template-form,.stack{display:grid;gap:18px}.card,.sub-card{padding:20px;border:1px solid rgba(255,255,255,.08);border-radius:24px;background:rgba(255,255,255,.03)}.grid,.choice-row,.tabs{display:grid;gap:16px}.grid-2,.choice-row{grid-template-columns:repeat(2,minmax(0,1fr))}.field--full{grid-column:1/-1}.section-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.hint,.empty{color:var(--color-ink-soft)}.choice,.tab,.switch-line{position:relative;display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:54px;padding:0 16px;border:1px solid rgba(255,255,255,.08);border-radius:18px;background:rgba(255,255,255,.04);color:var(--color-ink-soft);cursor:pointer}.choice input{position:absolute;opacity:0}.choice--active,.tab--active{color:var(--color-ink-strong);border-color:rgba(83,184,255,.28);background:rgba(83,184,255,.12)}.tabs{grid-template-columns:repeat(5,minmax(0,1fr))}.switch-line{justify-content:flex-start}.switch-line input{accent-color:#53b8ff}.empty{text-align:center;padding:18px}.code-area{font-family:var(--font-mono)}@media (max-width:960px){.grid-2,.choice-row,.tabs{grid-template-columns:1fr}.section-head{flex-direction:column;align-items:stretch}}
</style>
