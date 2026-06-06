<template>
  <div class="layout-switcher">
    <button
      class="layout-btn"
      :class="{ active: layout === 'grid' }"
      @click="$emit('change', 'grid')"
      title="网格视图"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="3" width="7" height="7" />
        <rect x="14" y="3" width="7" height="7" />
        <rect x="14" y="14" width="7" height="7" />
        <rect x="3" y="14" width="7" height="7" />
      </svg>
    </button>
    <button
      class="layout-btn"
      :class="{ active: layout === 'waterfall' }"
      @click="$emit('change', 'waterfall')"
      title="瀑布流视图"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="3" width="7" height="10" />
        <rect x="14" y="3" width="7" height="6" />
        <rect x="14" y="13" width="7" height="8" />
        <rect x="3" y="17" width="7" height="4" />
      </svg>
    </button>
  </div>
</template>

<script setup>
defineProps({
  layout: {
    type: String,
    default: 'grid'
  }
})

defineEmits(['change'])
</script>

<style lang="scss" scoped>
.layout-switcher {
  display: inline-flex;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  padding: 4px;
  gap: 4px;
}

.layout-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  transition: all 0.2s ease;

  svg {
    width: 18px;
    height: 18px;
  }

  &:hover {
    color: var(--text-primary);
    background: var(--bg-secondary);
  }

  &.active {
    background: var(--primary-color);
    color: white;
  }
}
</style>
