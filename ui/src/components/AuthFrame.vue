<script setup lang="ts">
interface HighlightItem {
  label: string
  value: string
  detail?: string
}

defineProps<{
  eyebrow: string
  title: string
  description: string
  panelTitle: string
  panelDescription: string
  highlights: HighlightItem[]
}>()
</script>

<template>
  <div class="auth-scene">
    <div class="auth-scene__glow auth-scene__glow--left" aria-hidden="true"></div>
    <div class="auth-scene__glow auth-scene__glow--right" aria-hidden="true"></div>

    <div class="auth-scene__grid">
      <section class="auth-story panel-card">
        <div class="auth-story__copy">
          <p class="section-kicker">{{ eyebrow }}</p>
          <h1 class="auth-story__title">{{ title }}</h1>
          <p class="auth-story__description">{{ description }}</p>
        </div>

        <div class="auth-visual" aria-hidden="true">
          <div class="auth-visual__ring auth-visual__ring--outer"></div>
          <div class="auth-visual__ring auth-visual__ring--inner"></div>
          <div class="auth-visual__grid"></div>
          <div class="auth-visual__core">
            <span>AH</span>
            <small>Agent Hub</small>
          </div>
          <span class="auth-visual__tag auth-visual__tag--top">智能路由</span>
          <span class="auth-visual__tag auth-visual__tag--left">权限校验</span>
          <span class="auth-visual__tag auth-visual__tag--right">多环境接入</span>
        </div>

        <div class="auth-story__highlights" aria-label="核心亮点">
          <article
            v-for="item in highlights"
            :key="item.label"
            class="auth-story__highlight"
          >
            <div>
              <span class="auth-story__highlight-label">{{ item.label }}</span>
              <strong class="auth-story__highlight-value">{{ item.value }}</strong>
            </div>
            <p v-if="item.detail" class="auth-story__highlight-detail">{{ item.detail }}</p>
          </article>
        </div>
      </section>

      <section class="auth-panel panel-card">
        <div class="auth-panel__header">
          <p class="section-kicker">Workspace Entry</p>
          <h2>{{ panelTitle }}</h2>
          <p>{{ panelDescription }}</p>
        </div>

        <slot></slot>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth-scene {
  position: relative;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  padding: 28px;
}

.auth-scene__glow {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(32px);
}

.auth-scene__glow--left {
  top: 12%;
  left: -8%;
  width: 420px;
  height: 420px;
  background: radial-gradient(circle, rgba(107, 231, 255, 0.18) 0%, transparent 72%);
}

.auth-scene__glow--right {
  right: -6%;
  bottom: 10%;
  width: 460px;
  height: 460px;
  background: radial-gradient(circle, rgba(98, 125, 255, 0.16) 0%, transparent 72%);
}

.auth-scene__grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 430px);
  gap: 24px;
  width: min(1240px, 100%);
  min-height: calc(100vh - 56px);
  min-height: calc(100dvh - 56px);
  margin: 0 auto;
}

.auth-story,
.auth-panel {
  animation: rise-in 480ms ease both;
}

.auth-story {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  padding: 40px;
}

.auth-story__copy {
  max-width: 42rem;
}

.auth-story__title {
  max-width: 14ch;
  margin-top: 20px;
  font-size: clamp(2.8rem, 4.5vw, 4.2rem);
  line-height: 1.02;
  letter-spacing: -0.03em;
  text-wrap: balance;
}

.auth-story__description {
  max-width: 28rem;
  margin-top: 12px;
  color: var(--color-ink-soft);
  font-size: 0.92rem;
  line-height: 1.6;
}

.auth-visual {
  position: relative;
  height: 360px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 50%, rgba(107, 231, 255, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.02)),
    linear-gradient(135deg, rgba(6, 16, 32, 0.95), rgba(11, 20, 38, 0.92));
}

.auth-visual__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: radial-gradient(circle at center, black 32%, transparent 88%);
}

.auth-visual__ring {
  position: absolute;
  inset: 50%;
  border-radius: 999px;
  transform: translate(-50%, -50%);
}

.auth-visual__ring--outer {
  width: 300px;
  height: 300px;
  border: 1px solid rgba(107, 231, 255, 0.14);
  box-shadow: 0 0 0 28px rgba(107, 231, 255, 0.03);
}

.auth-visual__ring--inner {
  width: 188px;
  height: 188px;
  border: 1px solid rgba(107, 231, 255, 0.32);
}

.auth-visual__core {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 136px;
  height: 136px;
  border: 1px solid rgba(107, 231, 255, 0.18);
  border-radius: 26px;
  background:
    radial-gradient(circle at top, rgba(107, 231, 255, 0.24), transparent 52%),
    rgba(10, 22, 40, 0.96);
  box-shadow:
    0 0 40px rgba(107, 231, 255, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  transform: translate(-50%, -50%);
}

.auth-visual__core span {
  color: #eafaff;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.auth-visual__core small {
  margin-top: 6px;
  color: var(--color-ink-muted);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.auth-visual__tag {
  position: absolute;
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  color: var(--color-ink-soft);
  background: rgba(255, 255, 255, 0.05);
  font-size: 0.82rem;
  backdrop-filter: blur(12px);
}

.auth-visual__tag--top {
  top: 42px;
  left: 50%;
  transform: translateX(-50%);
}

.auth-visual__tag--left {
  bottom: 62px;
  left: 38px;
}

.auth-visual__tag--right {
  right: 38px;
  bottom: 102px;
}

.auth-story__highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.auth-story__highlight {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  min-height: 112px;
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.035);
}

.auth-story__highlight-label {
  display: block;
  color: var(--color-ink-muted);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.auth-story__highlight-value {
  display: block;
  margin-top: 8px;
  color: var(--color-ink-strong);
  font-size: 1.12rem;
  line-height: 1.3;
}

.auth-story__highlight-detail {
  color: var(--color-ink-soft);
  font-size: 0.8rem;
  line-height: 1.45;
}

.auth-panel {
  align-self: center;
  width: 100%;
  padding: 32px;
}

.auth-panel__header {
  margin-bottom: 24px;
}

.auth-panel__header h2 {
  margin-top: 14px;
  font-size: clamp(2rem, 3vw, 2.5rem);
  line-height: 1.08;
}

.auth-panel__header p {
  margin-top: 12px;
  color: var(--color-ink-soft);
  font-size: 0.95rem;
  line-height: 1.65;
}

@media (max-width: 1080px) {
  .auth-scene {
    padding: 18px;
  }

  .auth-scene__grid {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-story {
    padding: 28px;
  }

  .auth-story__highlights {
    grid-template-columns: 1fr;
  }

  .auth-panel {
    max-width: none;
  }
}

@media (max-width: 640px) {
  .auth-scene {
    padding: 14px;
  }

  .auth-story,
  .auth-panel {
    padding: 22px;
    border-radius: 24px;
  }

  .auth-story__title {
    max-width: none;
    font-size: clamp(2rem, 10vw, 3rem);
  }

  .auth-visual {
    height: 300px;
  }

  .auth-visual__tag {
    font-size: 0.74rem;
  }

  .auth-visual__tag--left,
  .auth-visual__tag--right {
    bottom: 24px;
  }

  .auth-visual__tag--left {
    left: 18px;
  }

  .auth-visual__tag--right {
    right: 18px;
  }
}
</style>
