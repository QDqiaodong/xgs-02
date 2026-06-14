<template>
  <div class="star-rating" :class="{ 'is-readonly': readonly }">
    <button
      v-for="star in maxStars"
      :key="star"
      type="button"
      class="star-button"
      :class="{ active: star <= displayValue }"
      :disabled="readonly"
      :aria-label="`${star}星`"
      @mouseenter="hoverValue = star"
      @mouseleave="hoverValue = 0"
      @click="selectRating(star)"
    >
      &#9733;
    </button>
    <span v-if="showText" class="rating-text">
      {{ averageScore || '0.0' }}分 ({{ ratingCount || 0 }}人评价)
    </span>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: Number,
    default: 0
  },
  averageScore: {
    type: [Number, String],
    default: '0.0'
  },
  ratingCount: {
    type: Number,
    default: 0
  },
  maxStars: {
    type: Number,
    default: 5
  },
  showText: {
    type: Boolean,
    default: true
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'rate'])

const hoverValue = ref(0)

const displayValue = computed(() => hoverValue.value || props.modelValue || 0)

const selectRating = (score) => {
  if (props.readonly) return
  emit('update:modelValue', score)
  emit('rate', score)
}
</script>

<style scoped lang="scss">
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.star-button {
  appearance: none;
  border: 0;
  background: transparent;
  color: #d1d5db;
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
  padding: 0;
  transition: color 0.2s ease, transform 0.2s ease;

  &.active {
    color: #f59e0b;
  }

  &:hover:not(:disabled) {
    transform: scale(1.08);
  }

  &:disabled {
    cursor: default;
  }
}

.is-readonly .star-button {
  pointer-events: none;
}

.rating-text {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: 6px;
  white-space: nowrap;
}
</style>
