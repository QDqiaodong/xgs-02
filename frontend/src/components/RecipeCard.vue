<template>
  <div class="recipe-card" :class="{ 'waterfall-mode': mode === 'waterfall' }" @click="goToDetail">
    <div class="card-image" :class="{ 'auto-height': mode === 'waterfall' }">
      <img :src="recipe.coverImage || defaultImage" :alt="recipe.title" loading="lazy" />
      <div class="card-overlay">
        <span class="difficulty">{{ getDifficultyLabel(recipe.difficulty) }}</span>
        <span class="time">{{ recipe.cookTime }}分钟</span>
      </div>
    </div>
    
    <div class="card-content">
      <h3 class="card-title">{{ recipe.title }}</h3>
      <p class="card-desc">{{ recipe.description }}</p>
      
      <div class="card-tags">
        <span v-for="tag in recipe.tags?.slice(0, 3)" :key="tag" class="tag-mini">{{ tag }}</span>
      </div>
      
      <div class="card-footer">
        <div class="author">
          <span class="avatar">{{ recipe.author?.charAt(0) || '用' }}</span>
          <span class="author-name">{{ recipe.author || '美食达人' }}</span>
        </div>
        <div class="stats">
          <span class="stat-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            {{ recipe.favoriteCount || 0 }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  recipe: {
    type: Object,
    required: true
  },
  mode: {
    type: String,
    default: 'grid'
  }
})

const router = useRouter()

const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop'

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const goToDetail = () => {
  router.push(`/recipe/${props.recipe.id}`)
}
</script>

<style lang="scss" scoped>
.recipe-card {
  background: white;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  cursor: pointer;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
  }
}

.card-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
  }
  
  &:hover img {
    transform: scale(1.05);
  }
  
  &.auto-height {
    height: auto;
    
    img {
      height: auto;
      min-height: 160px;
    }
  }
}

.card-overlay {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
  
  span {
    padding: 4px 10px;
    background: rgba(0, 0, 0, 0.6);
    color: white;
    border-radius: 12px;
    font-size: 12px;
  }
}

.card-content {
  padding: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.card-tags {
  margin-bottom: 12px;
  min-height: 24px;
}

.tag-mini {
  display: inline-block;
  padding: 2px 8px;
  background: var(--bg-tertiary);
  border-radius: 10px;
  font-size: 11px;
  color: var(--text-secondary);
  margin-right: 6px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 500;
  }
  
  .author-name {
    font-size: 13px;
    color: var(--text-secondary);
  }
}

.stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  
  svg {
    width: 16px;
    height: 16px;
  }
}
</style>