<template>
  <div class="favorites-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">我的收藏</h1>
        <p class="page-desc">收藏的美味，随时可以找到</p>
      </div>

      <div v-if="favorites.length > 0" class="action-bar">
        <label class="select-all">
          <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
          全选
        </label>
        <button 
          class="btn btn-danger" 
          :disabled="selectedIds.length === 0"
          @click="batchRemove"
        >
          取消收藏 ({{ selectedIds.length }})
        </button>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>

      <div v-else-if="favorites.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <h3>还没有收藏任何菜谱</h3>
        <p>去发现美味的菜谱并收藏吧</p>
        <router-link to="/recipes" class="btn btn-primary">浏览菜谱</router-link>
      </div>

      <div v-else class="recipe-list">
        <div 
          v-for="recipe in favorites" 
          :key="recipe.id"
          class="recipe-item"
          :class="{ selected: selectedIds.includes(recipe.id) }"
        >
          <div class="select-box">
            <input 
              type="checkbox" 
              :checked="selectedIds.includes(recipe.id)"
              @change="toggleSelect(recipe.id)"
            />
          </div>
          
          <div class="recipe-cover" @click="goToDetail(recipe.id)">
            <img :src="recipe.coverImage || defaultImage" :alt="recipe.title" />
          </div>
          
          <div class="recipe-info" @click="goToDetail(recipe.id)">
            <h3 class="recipe-title">{{ recipe.title }}</h3>
            <p class="recipe-desc">{{ recipe.description }}</p>
            <div class="recipe-meta">
              <span class="meta-tag">{{ getDifficultyLabel(recipe.difficulty) }}</span>
              <span class="meta-tag">{{ recipe.cookTime }}分钟</span>
              <span class="meta-author">by {{ recipe.author }}</span>
            </div>
          </div>
          
          <div class="recipe-actions">
            <button class="action-btn view" @click="goToDetail(recipe.id)" title="查看">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
            <button class="action-btn remove" @click="removeFavorite(recipe.id)" title="取消收藏">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRecipeStore } from '@/store/recipe'

const router = useRouter()
const store = useRecipeStore()

const loading = ref(false)
const selectedIds = ref([])
const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop'

const favorites = ref([
  { id: 1, title: '红烧五花肉', description: '经典家常菜，肥而不腻，入口即化', coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=200&h=150&fit=crop', difficulty: 2, cookTime: 60, author: '美食达人', favoriteCount: 1286 },
  { id: 2, title: '番茄炒蛋', description: '最简单也最困难的国民家常菜', coverImage: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop', difficulty: 1, cookTime: 15, author: '小厨神', favoriteCount: 2341 },
  { id: 3, title: '宫保鸡丁', description: '川菜经典，鸡肉滑嫩，花生酥脆', coverImage: 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=200&h=150&fit=crop', difficulty: 2, cookTime: 30, author: '川菜大师', favoriteCount: 1892 }
])

const selectAll = computed({
  get: () => selectedIds.value.length === favorites.value.length && favorites.value.length > 0,
  set: (val) => {}
})

onMounted(() => {
  store.setFavorites(favorites.value)
})

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const goToDetail = (id) => {
  router.push(`/recipe/${id}`)
}

const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const toggleSelectAll = () => {
  if (selectedIds.value.length === favorites.value.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = favorites.value.map(r => r.id)
  }
}

const removeFavorite = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏这道菜谱吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    favorites.value = favorites.value.filter(r => r.id !== id)
    store.removeFavorite(id)
    ElMessage.success('已取消收藏')
  } catch {
  }
}

const batchRemove = async () => {
  try {
    await ElMessageBox.confirm(`确定要取消收藏选中的 ${selectedIds.value.length} 道菜谱吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    favorites.value = favorites.value.filter(r => !selectedIds.value.includes(r.id))
    selectedIds.value.forEach(id => store.removeFavorite(id))
    selectedIds.value = []
    ElMessage.success('批量取消收藏成功')
  } catch {
  }
}
</script>

<style lang="scss" scoped>
.favorites-page {
  padding: 40px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 32px;
  margin-bottom: 8px;
}

.page-desc {
  color: var(--text-secondary);
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 16px 24px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  
  input {
    cursor: pointer;
  }
}

.btn-danger {
  background: #ef4444;
  color: white;
  
  &:hover:not(:disabled) {
    background: #dc2626;
  }
  
  &:disabled {
    background: #fca5a5;
    cursor: not-allowed;
  }
}

.recipe-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recipe-item {
  display: flex;
  align-items: center;
  gap: 20px;
  background: white;
  padding: 16px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  cursor: pointer;
  
  &:hover {
    box-shadow: var(--shadow-md);
  }
  
  &.selected {
    border: 2px solid var(--primary-color);
  }
}

.select-box {
  flex-shrink: 0;
  
  input {
    width: 18px;
    height: 18px;
    cursor: pointer;
  }
}

.recipe-cover {
  flex-shrink: 0;
  width: 160px;
  height: 100px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.recipe-info {
  flex: 1;
  min-width: 0;
}

.recipe-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.recipe-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.meta-tag {
  padding: 2px 8px;
  background: var(--bg-tertiary);
  border-radius: 10px;
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-author {
  font-size: 12px;
  color: var(--text-light);
}

.recipe-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  
  svg {
    width: 18px;
    height: 18px;
  }
  
  &.view {
    background: var(--bg-tertiary);
    color: var(--text-secondary);
    
    &:hover {
      background: var(--primary-color);
      color: white;
    }
  }
  
  &.remove {
    background: #fef2f2;
    color: #ef4444;
    
    &:hover {
      background: #ef4444;
      color: white;
    }
  }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  
  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }
  
  h3 {
    font-size: 20px;
    margin-bottom: 8px;
  }
  
  p {
    color: var(--text-secondary);
    margin-bottom: 24px;
  }
}

@media (max-width: 768px) {
  .recipe-item {
    flex-wrap: wrap;
  }
  
  .recipe-cover {
    width: 100%;
    height: 150px;
  }
}
</style>