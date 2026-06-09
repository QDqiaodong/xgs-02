<template>
  <div class="my-recipes-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">我的菜谱</h1>
        <p class="page-desc">管理你发布的所有菜谱</p>
      </div>

      <div class="tabs">
        <div 
          class="tab" 
          :class="{ active: activeTab === 'published' }"
          @click="activeTab = 'published'"
        >
          已发布 ({{ publishedRecipes.length }})
        </div>
        <div 
          class="tab" 
          :class="{ active: activeTab === 'draft' }"
          @click="activeTab = 'draft'"
        >
          草稿 ({{ draftRecipes.length }})
        </div>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>

      <div v-else-if="currentList.length === 0" class="empty-state">
        <div class="empty-icon">{{ activeTab === 'published' ? '📝' : '📋' }}</div>
        <h3>{{ activeTab === 'published' ? '还没有发布任何菜谱' : '暂无草稿' }}</h3>
        <p>{{ activeTab === 'published' ? '开始创作你的第一道菜谱吧' : '保存的草稿会出现在这里' }}</p>
        <router-link to="/create" class="btn btn-primary">创作菜谱</router-link>
      </div>

      <div v-else class="recipe-list">
        <div 
          v-for="recipe in currentList" 
          :key="recipe.id"
          class="recipe-item"
        >
          <div class="recipe-cover">
            <img :src="recipe.coverImage || defaultImage" :alt="recipe.title" />
            <div v-if="recipe.status === 'draft'" class="draft-badge">草稿</div>
          </div>
          
          <div class="recipe-info">
            <h3 class="recipe-title">{{ recipe.title }}</h3>
            <p class="recipe-desc">{{ recipe.description }}</p>
            <div class="recipe-stats">
              <span class="stat">❤️ {{ recipe.favoriteCount || 0 }} 收藏</span>
              <span class="stat">👁️ {{ recipe.viewCount || 0 }} 浏览</span>
              <span class="stat">📅 {{ recipe.createdAt }}</span>
            </div>
          </div>
          
          <div class="recipe-actions">
            <button class="action-btn view" @click="goToDetail(recipe.id)" title="查看">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
            <button class="action-btn edit" @click="editRecipe(recipe.id)" title="编辑">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
            </button>
            <button class="action-btn delete" @click="deleteRecipe(recipe)" title="删除">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRecipeStore } from '@/store/recipe'

const router = useRouter()
const store = useRecipeStore()

const loading = ref(false)
const route = useRoute()
const activeTab = ref(route.query.tab === 'draft' ? 'draft' : 'published')
const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop'

const publishedRecipes = ref([
  { id: 1, title: '红烧五花肉', description: '经典家常菜，肥而不腻，入口即化', coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=200&h=150&fit=crop', status: 'published', favoriteCount: 1286, viewCount: 5621, createdAt: '2024-01-15' },
  { id: 2, title: '番茄炒蛋', description: '最简单也最困难的国民家常菜', coverImage: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop', status: 'published', favoriteCount: 2341, viewCount: 8932, createdAt: '2024-01-18' }
])

const draftRecipes = ref([
  { id: 101, title: '鱼香肉丝(未完成)', description: '川菜经典，酸甜咸辣', coverImage: '', status: 'draft', favoriteCount: 0, viewCount: 0, createdAt: '2024-01-20' }
])

const currentList = computed(() => {
  return activeTab.value === 'published' ? publishedRecipes.value : draftRecipes.value
})

onMounted(() => {
  store.setUserRecipes(publishedRecipes.value)
})

const goToDetail = (id) => {
  router.push(`/recipe/${id}`)
}

const editRecipe = (id) => {
  router.push(`/edit/${id}`)
}

const deleteRecipe = async (recipe) => {
  const isDraft = recipe.status === 'draft'
  try {
    await ElMessageBox.confirm(
      `确定要${isDraft ? '删除' : '下架'}这道菜谱吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (isDraft) {
      draftRecipes.value = draftRecipes.value.filter(r => r.id !== recipe.id)
    } else {
      publishedRecipes.value = publishedRecipes.value.filter(r => r.id !== recipe.id)
    }
    
    ElMessage.success(isDraft ? '删除成功' : '已下架')
  } catch {
  }
}
</script>

<style lang="scss" scoped>
.my-recipes-page {
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

.tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
  border-bottom: 2px solid var(--border-color);
}

.tab {
  padding: 12px 0;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  transition: color 0.2s ease;
  
  &.active {
    color: var(--primary-color);
    
    &::after {
      content: '';
      position: absolute;
      bottom: -2px;
      left: 0;
      right: 0;
      height: 2px;
      background: var(--primary-color);
    }
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
  
  &:hover {
    box-shadow: var(--shadow-md);
  }
}

.recipe-cover {
  flex-shrink: 0;
  width: 160px;
  height: 100px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.draft-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  background: #f59e0b;
  color: white;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
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

.recipe-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
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
  
  &.edit {
    background: #eff6ff;
    color: #3b82f6;
    
    &:hover {
      background: #3b82f6;
      color: white;
    }
  }
  
  &.delete {
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