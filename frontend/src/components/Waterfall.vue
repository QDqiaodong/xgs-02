<template>
  <div class="waterfall-container" ref="containerRef">
    <div class="waterfall-columns" :style="columnsStyle">
      <div
        v-for="(column, colIndex) in columns"
        :key="colIndex"
        class="waterfall-column"
      >
        <slot
          v-for="(item, itemIndex) in column"
          :key="item[itemKey]"
          :item="item"
          :index="itemIndex"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    required: true
  },
  columns: {
    type: Number,
    default: 4
  },
  itemKey: {
    type: String,
    default: 'id'
  }
})

const containerRef = ref(null)
const columnHeights = ref([])
const columnItems = ref([])

const columnsStyle = computed(() => ({
  '--columns': props.columns
}))

const columns = computed(() => columnItems.value)

const distributeItems = () => {
  columnHeights.value = new Array(props.columns).fill(0)
  columnItems.value = Array.from({ length: props.columns }, () => [])

  props.items.forEach((item) => {
    const shortestColIndex = columnHeights.value.indexOf(Math.min(...columnHeights.value))
    columnItems.value[shortestColIndex].push(item)

    const estimatedHeight = estimateItemHeight(item)
    columnHeights.value[shortestColIndex] += estimatedHeight
  })
}

const estimateItemHeight = (item) => {
  const baseHeight = 280
  const titleLines = item.title ? Math.ceil(item.title.length / 10) : 1
  const descLines = item.description ? Math.ceil(item.description.length / 20) : 2
  return baseHeight + titleLines * 20 + descLines * 16
}

const handleResize = () => {
  distributeItems()
}

watch(
  () => props.items,
  () => {
    nextTick(() => {
      distributeItems()
    })
  },
  { deep: true, immediate: true }
)

watch(
  () => props.columns,
  () => {
    nextTick(() => {
      distributeItems()
    })
  }
)

onMounted(() => {
  distributeItems()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.waterfall-container {
  width: 100%;
}

.waterfall-columns {
  display: grid;
  grid-template-columns: repeat(var(--columns), 1fr);
  gap: 24px;
}

.waterfall-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (max-width: 1024px) {
  .waterfall-columns {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}
</style>
