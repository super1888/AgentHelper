<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { BarChart3, Copy, ExternalLink, Link2, MousePointerClick, RefreshCw, ShieldCheck, Trash2, Users } from 'lucide-vue-next'
import AppFeedbackDialog from '@/components/AppFeedbackDialog.vue'
import MainShell from '@/components/MainShell.vue'
import {
  createShortLink,
  disableShortLink,
  enableShortLink,
  fetchShortLinkStats,
  queryShortLinkLogs,
  queryShortLinks,
  removeShortLink,
} from '@/api/link'
import type { ShortLinkAccessLogItem, ShortLinkItem, ShortLinkStatistics } from '@/types/link'
import { getErrorMessage } from '@/utils/errors'

type FeedbackTone = 'success' | 'error' | 'info'

interface FeedbackState {
  tone: FeedbackTone
  message: string
}

const loading = ref(false)
const saving = ref(false)
const actionLoading = ref(false)
const selectedCode = ref('')
const links = ref<ShortLinkItem[]>([])
const logs = ref<ShortLinkAccessLogItem[]>([])
const feedback = ref<FeedbackState | null>(null)

const stats = ref<ShortLinkStatistics>({
  totalCount: 0,
  enabledCount: 0,
  expiredCount: 0,
  totalVisitCount: 0,
  uniqueVisitorCount: 0,
  uniqueIpCount: 0,
})

const filters = reactive({
  keyword: '',
  status: 'ALL',
})

const form = reactive({
  longUrl: '',
  title: '',
  description: '',
  customCode: '',
  domain: '',
  expireTime: '',
})

const filteredLinks = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return links.value.filter((item) => {
    const matchKeyword = !keyword
      || [item.shortCode, item.title, item.longUrl, item.domain]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    const matchStatus = filters.status === 'ALL' || item.status === filters.status
    return matchKeyword && matchStatus
  })
})

const selectedLink = computed(() => links.value.find((item) => item.shortCode === selectedCode.value) ?? links.value[0] ?? null)

function showFeedback(tone: FeedbackTone, message: string) {
  feedback.value = { tone, message }
}

function clearFeedback() {
  feedback.value = null
}

function formatDateTime(value?: string | null) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatNumber(value?: number | null) {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

function buildPayload() {
  return {
    longUrl: form.longUrl.trim(),
    title: form.title.trim() || null,
    description: form.description.trim() || null,
    customCode: form.customCode.trim() || null,
    domain: form.domain.trim() || null,
    expireTime: form.expireTime ? `${form.expireTime}:00` : null,
  }
}

function resetForm() {
  form.longUrl = ''
  form.title = ''
  form.description = ''
  form.customCode = ''
  form.domain = ''
  form.expireTime = ''
}

async function loadData() {
  loading.value = true
  try {
    const [linkResult, statsResult] = await Promise.all([
      queryShortLinks(filters.keyword),
      fetchShortLinkStats(),
    ])
    links.value = linkResult
    stats.value = statsResult
    if (!selectedCode.value && linkResult.length > 0) {
      selectedCode.value = linkResult[0].shortCode
    }
    await loadLogs(selectedCode.value)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '短链接数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function loadLogs(shortCode?: string) {
  logs.value = await queryShortLinkLogs(shortCode || undefined)
}

async function handleCreate() {
  if (!form.longUrl.trim()) {
    showFeedback('info', '请先填写需要转换的长链接')
    return
  }
  saving.value = true
  try {
    const created = await createShortLink(buildPayload())
    showFeedback('success', `短链接已生成：${created.shortUrl}`)
    resetForm()
    selectedCode.value = created.shortCode
    await loadData()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '短链接生成失败'))
  } finally {
    saving.value = false
  }
}

async function toggleStatus(item: ShortLinkItem) {
  actionLoading.value = true
  try {
    if (item.status === 'ENABLED') {
      await disableShortLink(item.id)
      showFeedback('success', '短链接已停用')
    } else {
      await enableShortLink(item.id)
      showFeedback('success', '短链接已启用')
    }
    await loadData()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '状态更新失败'))
  } finally {
    actionLoading.value = false
  }
}

async function handleDelete(item: ShortLinkItem) {
  if (!window.confirm(`确认删除短链接 ${item.shortCode}？`)) return
  actionLoading.value = true
  try {
    await removeShortLink(item.id)
    showFeedback('success', '短链接已删除')
    if (selectedCode.value === item.shortCode) selectedCode.value = ''
    await loadData()
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '删除失败'))
  } finally {
    actionLoading.value = false
  }
}

async function copyShortUrl(item: ShortLinkItem) {
  await navigator.clipboard.writeText(item.shortUrl)
  showFeedback('success', '短链接已复制到剪贴板')
}

async function selectLink(item: ShortLinkItem) {
  selectedCode.value = item.shortCode
  try {
    await loadLogs(item.shortCode)
  } catch (error) {
    showFeedback('error', getErrorMessage(error, '访问日志加载失败'))
  }
}

onMounted(loadData)
</script>

<template>
  <MainShell>
    <div class="short-link-page">
      <section class="hero panel-card">
        <div class="hero__glow" aria-hidden="true"></div>
        <div class="hero__copy">
          <p class="section-kicker">Short Link Operation Deck</p>
          <h1>短链接生成与访问洞察</h1>
          <p>将长链接转换为唯一短码，结合 Redis 缓存、布隆过滤器、访问日志与基础风控，实现高性能跳转和可观测运营。</p>
        </div>
        <div class="hero__metrics">
          <div class="metric-card">
            <Link2 :size="18" />
            <span>链接总数</span>
            <strong>{{ formatNumber(stats.totalCount) }}</strong>
          </div>
          <div class="metric-card">
            <MousePointerClick :size="18" />
            <span>访问次数</span>
            <strong>{{ formatNumber(stats.totalVisitCount) }}</strong>
          </div>
          <div class="metric-card">
            <Users :size="18" />
            <span>访问人数</span>
            <strong>{{ formatNumber(stats.uniqueVisitorCount) }}</strong>
          </div>
          <div class="metric-card">
            <ShieldCheck :size="18" />
            <span>独立 IP</span>
            <strong>{{ formatNumber(stats.uniqueIpCount) }}</strong>
          </div>
        </div>
      </section>

      <section class="workspace-grid">
        <form class="creator panel-card" @submit.prevent="handleCreate">
          <div class="section-heading">
            <div>
              <p class="section-kicker">Create</p>
              <h2>生成短链接</h2>
            </div>
            <button type="submit" class="app-button" :disabled="saving">
              <span v-if="saving" class="button-spinner" aria-hidden="true"></span>
              {{ saving ? '生成中...' : '生成短链' }}
            </button>
          </div>

          <label class="field field--wide">
            <span>长链接</span>
            <input v-model="form.longUrl" type="url" placeholder="https://example.com/landing/campaign?from=agent" required />
          </label>
          <div class="form-grid">
            <label class="field">
              <span>标题</span>
              <input v-model="form.title" type="text" placeholder="活动页 / 文档 / 分享链接" />
            </label>
            <label class="field">
              <span>自定义短码</span>
              <input v-model="form.customCode" type="text" placeholder="可选，如 launch2026" />
            </label>
            <label class="field">
              <span>短链域名</span>
              <input v-model="form.domain" type="text" placeholder="可选，如 go.example.com" />
            </label>
            <label class="field">
              <span>过期时间</span>
              <input v-model="form.expireTime" type="datetime-local" />
            </label>
          </div>
          <label class="field field--wide">
            <span>描述</span>
            <textarea v-model="form.description" rows="3" placeholder="补充链接用途、投放渠道或安全说明"></textarea>
          </label>
        </form>

        <aside class="insight panel-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker">Selected</p>
              <h2>当前短链</h2>
            </div>
          </div>
          <template v-if="selectedLink">
            <div class="selected-code">{{ selectedLink.shortCode }}</div>
            <a :href="selectedLink.shortUrl" target="_blank" rel="noreferrer" class="selected-url">
              {{ selectedLink.shortUrl }}
              <ExternalLink :size="14" />
            </a>
            <div class="selected-stats">
              <span>访问 {{ formatNumber(selectedLink.totalVisitCount) }}</span>
              <span>访客 {{ formatNumber(selectedLink.uniqueVisitorCount) }}</span>
              <span>IP {{ formatNumber(selectedLink.uniqueIpCount) }}</span>
            </div>
            <p class="selected-long">{{ selectedLink.longUrl }}</p>
          </template>
          <p v-else class="empty-state">暂无短链接，创建后可查看实时统计。</p>
        </aside>
      </section>

      <section class="list-panel panel-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker">Manage</p>
            <h2>短链接列表</h2>
          </div>
          <div class="toolbar">
            <input v-model="filters.keyword" type="search" placeholder="搜索短码、标题或长链接" @keyup.enter="loadData" />
            <select v-model="filters.status">
              <option value="ALL">全部状态</option>
              <option value="ENABLED">已启用</option>
              <option value="DISABLED">已停用</option>
            </select>
            <button type="button" class="app-button app-button--secondary" :disabled="loading" @click="loadData">
              <RefreshCw :size="16" />刷新
            </button>
          </div>
        </div>

        <div class="link-table">
          <button
            v-for="item in filteredLinks"
            :key="item.id"
            type="button"
            class="link-row"
            :class="{ 'link-row--active': selectedCode === item.shortCode }"
            @click="selectLink(item)"
          >
            <div class="link-main">
              <strong>{{ item.title || item.shortCode }}</strong>
              <span>{{ item.longUrl }}</span>
            </div>
            <code>{{ item.shortCode }}</code>
            <span class="status-pill" :class="`status-pill--${item.status.toLowerCase()}`">{{ item.status === 'ENABLED' ? '已启用' : '已停用' }}</span>
            <div class="row-metrics">
              <span>{{ formatNumber(item.totalVisitCount) }} 次</span>
              <span>{{ formatNumber(item.uniqueVisitorCount) }} 人</span>
              <span>{{ formatNumber(item.uniqueIpCount) }} IP</span>
            </div>
            <div class="row-actions" @click.stop>
              <button type="button" title="复制" @click="copyShortUrl(item)"><Copy :size="15" /></button>
              <button type="button" :disabled="actionLoading" @click="toggleStatus(item)">{{ item.status === 'ENABLED' ? '停用' : '启用' }}</button>
              <button type="button" class="danger" :disabled="actionLoading" @click="handleDelete(item)"><Trash2 :size="15" /></button>
            </div>
          </button>
          <p v-if="!filteredLinks.length" class="empty-state">没有匹配的短链接。</p>
        </div>
      </section>

      <section class="logs panel-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker">Audit</p>
            <h2>最近访问日志</h2>
          </div>
          <div class="log-badge"><BarChart3 :size="16" />最多展示 100 条</div>
        </div>
        <div class="log-list">
          <div v-for="log in logs" :key="`${log.shortCode}-${log.accessTime}-${log.ipAddress}`" class="log-item">
            <span class="log-dot" :class="{ 'log-dot--fail': log.successFlag !== 1 }"></span>
            <div>
              <strong>{{ log.ipAddress || '未知 IP' }}</strong>
              <p>{{ log.userAgent || '未记录 User-Agent' }}</p>
            </div>
            <span>{{ formatDateTime(log.accessTime) }}</span>
            <em>{{ log.successFlag === 1 ? '跳转成功' : log.failReason || '跳转失败' }}</em>
          </div>
          <p v-if="!logs.length" class="empty-state">暂无访问日志。</p>
        </div>
      </section>
    </div>

    <AppFeedbackDialog
      :model-value="Boolean(feedback)"
      :tone="feedback?.tone ?? 'info'"
      :message="feedback?.message ?? ''"
      @update:model-value="clearFeedback"
    />
  </MainShell>
</template>

<style scoped>
.short-link-page {
  display: grid;
  gap: 22px;
}

.hero,
.creator,
.insight,
.list-panel,
.logs {
  position: relative;
  overflow: hidden;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 26px;
  padding: 30px;
  background:
    radial-gradient(circle at 10% 0%, rgba(57, 214, 180, 0.18), transparent 30%),
    linear-gradient(135deg, rgba(12, 26, 33, 0.94), rgba(18, 31, 48, 0.86));
}

.hero__glow {
  position: absolute;
  right: -90px;
  top: -120px;
  width: 340px;
  height: 340px;
  border-radius: 999px;
  background: conic-gradient(from 120deg, rgba(77, 226, 178, 0.45), rgba(79, 142, 255, 0.2), transparent);
  filter: blur(12px);
}

.hero__copy,
.hero__metrics {
  position: relative;
  z-index: 1;
}

.hero h1,
.section-heading h2 {
  margin: 0;
  color: var(--color-ink-strong);
}

.hero h1 {
  max-width: 720px;
  font-size: clamp(2rem, 5vw, 4.2rem);
  letter-spacing: -0.08em;
  line-height: 0.94;
}

.hero p {
  max-width: 680px;
  color: var(--color-ink-muted);
  line-height: 1.8;
}

.hero__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.metric-card svg {
  color: #52f0c2;
}

.metric-card span,
.selected-stats span,
.log-badge {
  color: var(--color-ink-muted);
  font-size: 0.82rem;
}

.metric-card strong {
  color: var(--color-ink-strong);
  font-size: 1.9rem;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.6fr);
  gap: 22px;
}

.creator,
.insight,
.list-panel,
.logs {
  padding: 24px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
  color: var(--color-ink-muted);
  font-size: 0.86rem;
}

.field--wide {
  margin-bottom: 16px;
}

.field input,
.field textarea,
.toolbar input,
.toolbar select {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 12px 14px;
  color: var(--color-ink-strong);
  background: rgba(255, 255, 255, 0.06);
  outline: none;
}

.field textarea {
  resize: vertical;
}

.selected-code {
  display: inline-flex;
  padding: 10px 14px;
  border-radius: 18px;
  color: #07120f;
  background: #52f0c2;
  font-size: 1.25rem;
  font-weight: 800;
}

.selected-url {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  color: var(--color-primary);
  word-break: break-all;
}

.selected-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.selected-stats span {
  padding: 8px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
}

.selected-long {
  margin-top: 18px;
  color: var(--color-ink-muted);
  line-height: 1.7;
  word-break: break-all;
}

.toolbar,
.row-actions,
.log-badge {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar input {
  width: min(320px, 40vw);
}

.link-table,
.log-list {
  display: grid;
  gap: 10px;
}

.link-row {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 110px 86px 170px auto;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px;
  border: 0;
  border-radius: 20px;
  text-align: left;
  color: inherit;
  background: rgba(255, 255, 255, 0.045);
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}

.link-row:hover,
.link-row--active {
  transform: translateY(-1px);
  background: rgba(82, 240, 194, 0.1);
}

.link-main {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.link-main strong {
  color: var(--color-ink-strong);
}

.link-main span {
  overflow: hidden;
  color: var(--color-ink-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-row code {
  color: #52f0c2;
}

.status-pill {
  justify-self: start;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  color: #52f0c2;
  background: rgba(82, 240, 194, 0.12);
}

.status-pill--disabled {
  color: #ffba7a;
  background: rgba(255, 186, 122, 0.12);
}

.row-metrics {
  display: flex;
  gap: 10px;
  color: var(--color-ink-muted);
  font-size: 0.82rem;
}

.row-actions button {
  border: 0;
  border-radius: 12px;
  padding: 8px 10px;
  color: var(--color-ink-strong);
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
}

.row-actions .danger {
  color: #ff8d8d;
}

.log-item {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr) 170px 120px;
  gap: 12px;
  align-items: center;
  padding: 13px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.045);
}

.log-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #52f0c2;
  box-shadow: 0 0 0 6px rgba(82, 240, 194, 0.12);
}

.log-dot--fail {
  background: #ff8d8d;
  box-shadow: 0 0 0 6px rgba(255, 141, 141, 0.12);
}

.log-item strong,
.log-item em {
  color: var(--color-ink-strong);
  font-style: normal;
}

.log-item p,
.log-item span,
.empty-state {
  margin: 0;
  color: var(--color-ink-muted);
}

.log-item p {
  overflow: hidden;
  max-width: 680px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .hero,
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .link-row {
    grid-template-columns: 1fr;
  }

  .row-actions,
  .row-metrics {
    flex-wrap: wrap;
  }
}

@media (max-width: 720px) {
  .form-grid,
  .hero__metrics,
  .log-item {
    grid-template-columns: 1fr;
  }

  .toolbar input {
    width: 100%;
  }
}
</style>
