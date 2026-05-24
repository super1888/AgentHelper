<template>
  <MainShell>
    <section class="management-page statistics-page">
      <header class="statistics-hero panel-card">
        <div>
          <p class="section-kicker">Traffic Analytics</p>
          <h1>访问统计</h1>
          <p>基于 Redis HyperLogLog 统计 PV、VV、UV 与独立 IP，适合轻量级趋势观测。</p>
        </div>
        <div class="date-filter">
          <label>
            <span>开始日期</span>
            <input v-model="filters.startDate" type="date" />
          </label>
          <label>
            <span>结束日期</span>
            <input v-model="filters.endDate" type="date" />
          </label>
          <button class="app-button" type="button" :disabled="loading" @click="loadOverview">刷新</button>
        </div>
      </header>

      <section class="metric-grid">
        <article v-for="item in metricCards" :key="item.key" class="metric-card panel-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.total }}</strong>
          <em>今日 {{ item.today }}</em>
        </article>
      </section>

      <section class="chart-card panel-card">
        <div class="chart-card__header">
          <div>
            <p class="section-kicker">Trend</p>
            <h2>趋势折线图</h2>
          </div>
          <div class="legend">
            <span v-for="line in chartLines" :key="line.key">
              <i :style="{ background: line.color }"></i>{{ line.label }}
            </span>
          </div>
        </div>

        <div class="chart-wrap">
          <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" role="img" aria-label="访问统计折线图">
            <line v-for="tick in yTicks" :key="tick.y" x1="48" :x2="chartWidth - 18" :y1="tick.y" :y2="tick.y" class="grid-line" />
            <text v-for="tick in yTicks" :key="`label-${tick.y}`" x="8" :y="tick.y + 4" class="axis-label">{{ tick.label }}</text>
            <polyline v-for="line in chartLines" :key="line.key" :points="line.points" :stroke="line.color" class="trend-line" />
            <g v-for="line in chartLines" :key="`${line.key}-points`">
              <circle v-for="point in line.pointList" :key="`${line.key}-${point.x}-${point.y}`" :cx="point.x" :cy="point.y" r="3.5" :fill="line.color" />
            </g>
            <text v-for="label in xLabels" :key="label.text" :x="label.x" :y="chartHeight - 8" class="axis-label axis-label--x">{{ label.text }}</text>
          </svg>
        </div>
      </section>

      <section class="panel-card data-card">
        <div class="chart-card__header">
          <div>
            <p class="section-kicker">Daily Data</p>
            <h2>每日明细</h2>
          </div>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>日期</th>
                <th>PV</th>
                <th>VV</th>
                <th>UV</th>
                <th>IP</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in overview.trends" :key="item.date">
                <td>{{ item.date }}</td>
                <td>{{ item.pv }}</td>
                <td>{{ item.vv }}</td>
                <td>{{ item.uv }}</td>
                <td>{{ item.ip }}</td>
              </tr>
              <tr v-if="!overview.trends.length && !loading">
                <td colspan="5" class="empty-cell">暂无统计数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </section>
  </MainShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MainShell from '@/components/MainShell.vue'
import { fetchStatisticsOverview } from '@/api/statistics'
import type { StatisticsMetricPoint, StatisticsOverview } from '@/types/statistics'

const chartWidth = 860
const chartHeight = 320
const chartPadding = { left: 48, right: 18, top: 26, bottom: 34 }
const loading = ref(false)
const filters = reactive({
  startDate: '',
  endDate: '',
})
const overview = ref<StatisticsOverview>({
  startDate: '',
  endDate: '',
  totalPv: 0,
  totalVv: 0,
  totalUv: 0,
  totalIp: 0,
  todayPv: 0,
  todayVv: 0,
  todayUv: 0,
  todayIp: 0,
  trends: [],
})

const metricCards = computed(() => [
  { key: 'pv', label: 'PV 页面浏览', total: overview.value.totalPv, today: overview.value.todayPv },
  { key: 'vv', label: 'VV 访问次数', total: overview.value.totalVv, today: overview.value.todayVv },
  { key: 'uv', label: 'UV 独立访客', total: overview.value.totalUv, today: overview.value.todayUv },
  { key: 'ip', label: 'IP 独立地址', total: overview.value.totalIp, today: overview.value.todayIp },
])

const maxValue = computed(() => Math.max(1, ...overview.value.trends.flatMap((item) => [item.pv, item.vv, item.uv, item.ip])))
const yTicks = computed(() => [0, 0.25, 0.5, 0.75, 1].map((rate) => ({
  y: chartPadding.top + (1 - rate) * drawableHeight(),
  label: Math.round(maxValue.value * rate),
})).reverse())
const xLabels = computed(() => overview.value.trends.map((item, index) => ({
  x: xPosition(index, overview.value.trends.length),
  text: item.date.slice(5),
})).filter((_, index) => index % Math.max(1, Math.ceil(overview.value.trends.length / 8)) === 0))
const chartLines = computed(() => [
  buildLine('pv', 'PV', '#2563eb'),
  buildLine('vv', 'VV', '#06b6d4'),
  buildLine('uv', 'UV', '#16a34a'),
  buildLine('ip', 'IP', '#f97316'),
])

function drawableWidth() {
  return chartWidth - chartPadding.left - chartPadding.right
}

function drawableHeight() {
  return chartHeight - chartPadding.top - chartPadding.bottom
}

function xPosition(index: number, total: number) {
  if (total <= 1) {
    return chartPadding.left
  }
  return chartPadding.left + (index / (total - 1)) * drawableWidth()
}

function yPosition(value: number) {
  return chartPadding.top + (1 - value / maxValue.value) * drawableHeight()
}

function readMetric(item: StatisticsMetricPoint, key: 'pv' | 'vv' | 'uv' | 'ip') {
  return item[key]
}

function buildLine(key: 'pv' | 'vv' | 'uv' | 'ip', label: string, color: string) {
  const pointList = overview.value.trends.map((item, index) => ({
    x: xPosition(index, overview.value.trends.length),
    y: yPosition(readMetric(item, key)),
  }))
  return {
    key,
    label,
    color,
    pointList,
    points: pointList.map((point) => `${point.x},${point.y}`).join(' '),
  }
}

function formatDate(date: Date) {
  return date.toISOString().slice(0, 10)
}

function initDateRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 13)
  filters.startDate = formatDate(start)
  filters.endDate = formatDate(end)
}

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await fetchStatisticsOverview({
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initDateRange()
  void loadOverview()
})
</script>

<style scoped>
.statistics-page { display: grid; gap: 20px; min-width: 0; }
.statistics-page * { box-sizing: border-box; }
.statistics-hero { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; flex-wrap: wrap; }
.statistics-hero h1, .chart-card h2, .data-card h2 { margin: 0; color: var(--color-ink-strong); }
.statistics-hero p { color: var(--color-ink-soft); line-height: 1.7; max-width: 680px; }
.date-filter { display: flex; gap: 12px; align-items: end; flex-wrap: wrap; }
.date-filter label { display: grid; gap: 8px; color: var(--color-ink-soft); font-size: 13px; }
.date-filter input { border: 1px solid #dbe3ef; border-radius: 14px; padding: 10px 12px; background: #fff; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.metric-card { min-width: 0; }
.metric-card span { color: var(--color-ink-soft); font-size: 13px; }
.metric-card strong { display: block; margin-top: 8px; color: var(--color-ink-strong); font-size: clamp(1.45rem, 2.4vw, 2rem); line-height: 1; }
.metric-card em { display: block; margin-top: 10px; color: var(--color-accent-strong); font-style: normal; }
.chart-card, .data-card { min-width: 0; }
.chart-card__header { display: flex; justify-content: space-between; gap: 16px; align-items: center; flex-wrap: wrap; margin-bottom: 18px; }
.legend { display: flex; gap: 14px; flex-wrap: wrap; color: var(--color-ink-soft); font-size: 13px; }
.legend span { display: inline-flex; gap: 6px; align-items: center; }
.legend i { width: 10px; height: 10px; border-radius: 999px; }
.chart-wrap { width: 100%; min-width: 0; overflow-x: auto; }
.chart-wrap svg { width: 100%; min-width: 680px; height: auto; display: block; }
.grid-line { stroke: #e2e8f0; stroke-width: 1; }
.axis-label { fill: var(--color-ink-muted); font-size: 11px; }
.axis-label--x { text-anchor: middle; }
.trend-line { fill: none; stroke-width: 3; stroke-linejoin: round; stroke-linecap: round; }
.table-wrap { width: 100%; overflow-x: auto; }
table { width: 100%; min-width: 620px; border-collapse: collapse; table-layout: fixed; }
th, td { padding: 13px 12px; border-bottom: 1px solid #edf2f7; text-align: left; }
th { color: var(--color-ink-soft); font-size: 12px; }
td { color: var(--color-ink); }
.empty-cell { text-align: center; color: var(--color-ink-muted); padding: 30px; }
@media (max-width: 980px) { .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .metric-grid, .date-filter { grid-template-columns: 1fr; display: grid; width: 100%; } .date-filter .app-button, .date-filter label { width: 100%; } }

/* 局部视觉修正：对齐现有深色玻璃风格，收敛字号并避免外溢 */
.statistics-page > .panel-card,
.statistics-hero,
.metric-card,
.chart-card,
.data-card {
  overflow: hidden;
  border-color: var(--color-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.018)),
    rgba(6, 12, 24, 0.76);
  color: var(--color-ink);
}

.statistics-page .section-kicker {
  color: #b4f5ff;
  font-size: 0.74rem;
  letter-spacing: 0.2em;
}

.statistics-hero h1 {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(1.65rem, 2.1vw, 2.25rem);
  line-height: 1.2;
}

.chart-card h2,
.data-card h2 {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(1.1rem, 1.4vw, 1.35rem);
  line-height: 1.25;
}

.statistics-hero p,
.date-filter label,
.legend,
th,
td {
  color: var(--color-ink-soft);
  font-size: 0.84rem;
}

.metric-card {
  background:
    linear-gradient(180deg, rgba(77, 179, 255, 0.08), rgba(255, 255, 255, 0.018)),
    rgba(6, 12, 24, 0.72);
}

.metric-card span {
  color: var(--color-ink-muted);
  font-size: 0.78rem;
}

.metric-card strong {
  color: var(--color-ink-strong);
  font-size: clamp(1.45rem, 2.4vw, 2rem);
  letter-spacing: -0.02em;
}

.metric-card em {
  color: var(--color-accent-strong);
  font-size: 0.78rem;
}

.date-filter input {
  color: var(--color-ink-strong);
  border-color: rgba(150, 181, 255, 0.16);
  background: rgba(255, 255, 255, 0.06);
}

.chart-wrap,
.table-wrap {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.025);
  overflow: auto;
}

.chart-wrap svg {
  background: transparent;
}

.axis-label {
  fill: rgba(204, 217, 240, 0.82);
  font-size: 0.68rem;
}

.grid-line {
  stroke: rgba(150, 181, 255, 0.14);
}

.data-card table,
.data-card th,
.data-card td {
  font-size: 0.78rem;
}
</style>