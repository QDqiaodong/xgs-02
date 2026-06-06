<template>
  <div class="recipes-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">菜谱大全</h1>
        <p class="page-desc">探索美味，发现烹饪的乐趣</p>
      </div>

      <div class="filter-section">
        <div class="filter-group">
          <label>菜系分类</label>
          <div class="filter-tags">
            <span 
              class="tag" 
              :class="{ active: !filters.cuisine }"
              @click="updateFilter('cuisine', '')"
            >全部</span>
            <span 
              v-for="c in cuisines" 
              :key="c"
              class="tag"
              :class="{ active: filters.cuisine === c }"
              @click="updateFilter('cuisine', c)"
            >{{ c }}</span>
          </div>
        </div>
        
        <div class="filter-group">
          <label>用餐场景</label>
          <div class="filter-tags">
            <span 
              class="tag" 
              :class="{ active: !filters.scene }"
              @click="updateFilter('scene', '')"
            >全部</span>
            <span 
              v-for="s in scenes" 
              :key="s"
              class="tag"
              :class="{ active: filters.scene === s }"
              @click="updateFilter('scene', s)"
            >{{ s }}</span>
          </div>
        </div>
        
        <div class="filter-group">
          <label>制作难度</label>
          <div class="filter-tags">
            <span 
              class="tag" 
              :class="{ active: !filters.difficulty }"
              @click="updateFilter('difficulty', '')"
            >全部</span>
            <span 
              v-for="d in difficulties" 
              :key="d.value"
              class="tag"
              :class="{ active: filters.difficulty === d.value }"
              @click="updateFilter('difficulty', d.value)"
            >{{ d.label }}</span>
          </div>
        </div>

        <div class="search-box">
          <input 
            type="text" 
            v-model="filters.keyword" 
            placeholder="搜索菜谱..."
            @input="handleSearch"
          />
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </div>
      </div>

      <div class="sort-bar">
        <span>共 {{ recipes.length }} 道菜谱</span>
        <div class="sort-options">
          <span 
            class="sort-option" 
            :class="{ active: sortBy === 'hot' }"
            @click="sortBy = 'hot'"
          >最热门</span>
          <span 
            class="sort-option" 
            :class="{ active: sortBy === 'new' }"
            @click="sortBy = 'new'"
          >最新发布</span>
        </div>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>

      <div v-else-if="filteredRecipes.length === 0" class="empty-state">
        <div class="empty-icon">🍳</div>
        <h3>暂无符合条件的菜谱</h3>
        <p>试试调整筛选条件，或者发布第一道菜谱吧</p>
        <router-link to="/create" class="btn btn-primary">发布菜谱</router-link>
      </div>

      <div v-else class="recipe-grid">
        <RecipeCard v-for="recipe in filteredRecipes" :key="recipe.id" :recipe="recipe" />
      </div>

      <div v-if="filteredRecipes.length > 0" class="load-more">
        <button class="btn btn-outline" @click="loadMore">加载更多</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import RecipeCard from '@/components/RecipeCard.vue'

const route = useRoute()

const loading = ref(true)
const sortBy = ref('hot')
const filters = ref({
  cuisine: '',
  scene: '',
  difficulty: '',
  keyword: ''
})

const cuisines = ['川菜', '粤菜', '湘菜', '鲁菜', '苏菜', '浙菜', '闽菜', '徽菜', '家常菜']
const scenes = ['早餐', '午餐', '晚餐', '夜宵', '下午茶', '宴请']
const difficulties = [
  { value: '1', label: '简单' },
  { value: '2', label: '中等' },
  { value: '3', label: '困难' }
]

const allRecipes = [
  { id: 1, title: '红烧五花肉', description: '经典家常菜，肥而不腻，入口即化', coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=400&h=300&fit=crop', difficulty: 2, cookTime: 60, tags: ['川菜', '家常菜', '晚餐'], author: '美食达人', favoriteCount: 1286, createdAt: '2024-01-15' },
  { id: 2, title: '番茄炒蛋', description: '最简单也最困难的国民家常菜', coverImage: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop', difficulty: 1, cookTime: 15, tags: ['家常菜', '快手菜', '午餐'], author: '小厨神', favoriteCount: 2341, createdAt: '2024-01-18' },
  { id: 3, title: '宫保鸡丁', description: '川菜经典，鸡肉滑嫩，花生酥脆', coverImage: 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=400&h=300&fit=crop', difficulty: 2, cookTime: 30, tags: ['川菜', '经典', '晚餐'], author: '川菜大师', favoriteCount: 1892, createdAt: '2024-01-10' },
  { id: 4, title: '清蒸鲈鱼', description: '粤式蒸鱼，鲜嫩爽滑，原汁原味', coverImage: 'https://images.unsplash.com/photo-1534604973900-c43ab4c2e0ab?w=400&h=300&fit=crop', difficulty: 2, cookTime: 25, tags: ['粤菜', '海鲜', '宴请'], author: '粤菜名厨', favoriteCount: 1567, createdAt: '2024-01-12' },
  { id: 5, title: '麻婆豆腐', description: '麻辣鲜香，下饭神器', coverImage: 'https://images.unsplash.com/photo-1582576163090-09d3b6f8a969?w=400&h=300&fit=crop', difficulty: 1, cookTime: 20, tags: ['川菜', '家常菜', '午餐'], author: '四川厨子', favoriteCount: 2103, createdAt: '2024-01-20' },
  { id: 6, title: '白切鸡', description: '粤菜经典，皮爽肉滑', coverImage: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400&h=300&fit=crop', difficulty: 2, cookTime: 45, tags: ['粤菜', '经典', '宴请'], author: '广东阿婆', favoriteCount: 1456, createdAt: '2024-01-08' },
  { id: 7, title: '糖醋排骨', description: '酸甜可口，老少皆宜', coverImage: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=400&h=300&fit=crop', difficulty: 2, cookTime: 40, tags: ['家常菜', '浙菜', '晚餐'], author: '巧手妈妈', favoriteCount: 1789, createdAt: '2024-01-16' },
  { id: 8, title: '水煮鱼', description: '麻辣鲜香，鱼片滑嫩', coverImage: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop', difficulty: 3, cookTime: 50, tags: ['川菜', '海鲜', '夜宵'], author: '辣味人生', favoriteCount: 1234, createdAt: '2024-01-14' }
]

const recipes = ref([...allRecipes])

const filteredRecipes = computed(() => {
  let result = [...recipes.value]
  
  if (filters.value.cuisine) {
    result = result.filter(r => r.tags.includes(filters.value.cuisine))
  }
  
  if (filters.value.scene) {
    result = result.filter(r => r.tags.includes(filters.value.scene))
  }
  
  if (filters.value.difficulty) {
    result = result.filter(r => r.difficulty === parseInt(filters.value.difficulty))
  }
  
  if (filters.value.keyword) {
    const kw = filters.value.keyword.toLowerCase()
    result = result.filter(r => 
      r.title.toLowerCase().includes(kw) || 
      r.description.toLowerCase().includes(kw)
    )
  }
  
  if (sortBy.value === 'hot') {
    result.sort((a, b) => b.favoriteCount - a.favoriteCount)
  } else {
    result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }
  
  return result
})

onMounted(() => {
  if (route.query.category) {
    filters.value.cuisine = route.query.category
  }
  loading.value = false
})

const updateFilter = (key, value) => {
  filters.value[key] = value
}

const handleSearch = () => {
}

const loadMore = () => {
}
</script>

<style lang="scss" scoped>
.recipes-page {
  padding: 40px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 36px;
  margin-bottom: 8px;
}

.page-desc {
  color: var(--text-secondary);
  font-size: 16px;
}

.filter-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.filter-group {
  margin-bottom: 16px;
  
  &:last-child {
    margin-bottom: 0;
  }
  
  label {
    display: block;
    font-weight: 500;
    margin-bottom: 8px;
    font-size: 14px;
  }
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-box {
  position: relative;
  margin-top: 16px;
  
  input {
    width: 100%;
    padding: 12px 44px 12px 16px;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    font-size: 14px;
    transition: border-color 0.2s ease;
    
    &:focus {
      outline: none;
      border-color: var(--primary-color);
    }
  }
  
  svg {
    position: absolute;
    right: 16px;
    top: 50%;
    transform: translateY(-50%);
    width: 20px;
    height: 20px;
    color: var(--text-light);
  }
}

.sort-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  span {
    color: var(--text-secondary);
    font-size: 14px;
  }
}

.sort-options {
  display: flex;
  gap: 16px;
}

.sort-option {
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  transition: color 0.2s ease;
  
  &.active, &:hover {
    color: var(--primary-color);
    font-weight: 500;
  }
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 40px;
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

.load-more {
  text-align: center;
}

@media (max-width: 1024px) {
  .recipe-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>