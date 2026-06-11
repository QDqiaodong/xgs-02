<template>
  <div class="view-history-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">浏览历史</h1>
        <p class="page-desc">记录你看过的每一道美味</p>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>

      <div v-else-if="historyList.length === 0" class="empty-state">
        <div class="empty-icon">📖</div>
        <h3>还没有浏览记录</h3>
        <p>去发现美味的菜谱吧</p>
        <router-link to="/recipes" class="btn btn-primary">浏览菜谱</router-link>
      </div>

      <div v-else>
        <div class="action-bar">
          <span class="history-count">共 {{ total }} 条记录</span>
          <button class="btn btn-danger" @click="clearAll">
            清空历史
          </button>
        </div>

        <div class="history-list">
          <div
            v-for="recipe in historyList"
            :key="recipe.id"
            class="history-item"
          >
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
              <div class="view-time">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
                <span>{{ formatViewTime(recipe.viewTime || recipe.createdAt) }}</span>
              </div>
            </div>

            <div class="recipe-actions">
              <button class="action-btn view" @click="goToDetail(recipe.id)" title="查看">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
              <button class="action-btn remove" @click="removeHistory(recipe.id)" title="删除记录">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/>
                  <path d="M10 11v6"/>
                  <path d="M14 11v6"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="pagination" v-if="total > pageSize">
          <button 
            class="page-btn" 
            :disabled="page <= 1" 
            @click="prevPage"
          >
            上一页
          </button>
          <span class="page-info">第 {{ page }} 页 / 共 {{ totalPages }} 页</span>
          <button 
            class="page-btn" 
            :disabled="page >= totalPages" 
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { viewHistoryApi } from '@/utils/api'

const router = useRouter()

const loading = ref(false)
const historyList = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop'

const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const formatViewTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${minute}`
}

const goToDetail = (id) => {
  router.push(`/recipe/${id}`)
}

const loadHistory = async () => {
  loading.value = true
  try {
    const result = await viewHistoryApi.getViewHistory(page.value, pageSize)
    const data = result?.data
    if (data) {
      historyList.value = data.records || []
      total.value = data.total || 0
    }
  } catch (err) {
    console.error('加载浏览历史失败', err)
    ElMessage.error('加载浏览历史失败')
  } finally {
    loading.value = false
  }
}

const removeHistory = async (recipeId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条浏览记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await viewHistoryApi.removeViewHistory(recipeId)
    historyList.value = historyList.value.filter(r => r.id !== recipeId)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已删除记录')
    
    if (historyList.value.length === 0 && page.value > 1) {
      page.value--
      loadHistory()
    }
  } catch {
  }
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有浏览历史吗？此操作不可恢复。', '提示', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await viewHistoryApi.clearAllHistory()
    historyList.value = []
    total.value = 0
    ElMessage.success('已清空浏览历史')
  } catch {
  }
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadHistory()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadHistory()
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style lang="scss" scoped>
.view-history-page {
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

.history-count {
  color: var(--text-secondary);
  font-size: 14px;
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

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
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
  margin-bottom: 8px;
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

.view-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-light);

  svg {
    width: 14px;
    height: 14px;
  }
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
  background: white;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);

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

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
}

.page-btn {
  padding: 8px 20px;
  background: white;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: var(--primary-color);
    color: white;
    border-color: var(--primary-color);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.page-info {
  color: var(--text-secondary);
  font-size: 14px;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .history-item {
    flex-wrap: wrap;
  }

  .recipe-cover {
    width: 100%;
    height: 150px;
  }

  .action-bar {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
