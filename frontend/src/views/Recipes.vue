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
            ref="searchInputRef"
            type="text" 
            v-model="filters.keyword" 
            placeholder="搜索菜谱..."
            @input="handleSearch"
            @keydown="handleKeydown"
            @blur="handleSearchBlur"
            @focus="handleSearchFocus"
            autocomplete="off"
          />
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <transition name="fade">
            <div 
              v-if="showSuggestions && searchSuggestions.length > 0" 
              class="suggestions-dropdown"
              ref="suggestionListRef"
            >
              <div
                v-for="(recipe, index) in searchSuggestions"
                :key="recipe.id"
                class="suggestion-item"
                :class="{ active: activeSuggestionIndex === index }"
                @mousedown.prevent="goToRecipeDetail(recipe)"
                @mouseenter="handleSuggestionHover(index)"
              >
                <span class="suggestion-icon">🍽️</span>
                <span class="suggestion-title" v-html="highlightMatch(recipe.title)"></span>
              </div>
            </div>
          </transition>
        </div>
      </div>

      <div class="sort-bar">
        <span>共 {{ allFilteredRecipes.length }} 道菜谱</span>
        <div class="sort-bar-right">
          <div class="sort-options">
            <span 
              class="sort-option" 
              :class="{ active: sortBy === 'hot' }"
              @click="handleSortChange('hot')"
            >最热门</span>
            <span 
              class="sort-option" 
              :class="{ active: sortBy === 'new' }"
              @click="handleSortChange('new')"
            >最新发布</span>
          </div>
          <div v-if="sortBy === 'hot'" class="hot-dimension-tabs">
            <span 
              class="tab-item" 
              :class="{ active: hotDimension === 'weekly' }"
              @click="changeHotDimension('weekly')"
            >周榜</span>
            <span 
              class="tab-item" 
              :class="{ active: hotDimension === 'monthly' }"
              @click="changeHotDimension('monthly')"
            >月榜</span>
            <span 
              class="tab-item" 
              :class="{ active: hotDimension === 'total' }"
              @click="changeHotDimension('total')"
            >总榜</span>
          </div>
          <LayoutSwitcher :layout="layout" @change="handleLayoutChange" />
        </div>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>

      <div v-else-if="displayRecipes.length === 0" class="empty-state">
        <div class="empty-icon">🍳</div>
        <h3>暂无符合条件的菜谱</h3>
        <p>试试调整筛选条件，或者发布第一道菜谱吧</p>
        <router-link to="/create" class="btn btn-primary">发布菜谱</router-link>
      </div>

      <template v-else>
        <div v-if="layout === 'grid'" class="recipe-grid">
          <RecipeCard v-for="recipe in displayRecipes" :key="recipe.id" :recipe="recipe" mode="grid" />
        </div>
        <Waterfall v-else :items="displayRecipes" :columns="4" v-slot="{ item }">
          <RecipeCard :recipe="item" mode="waterfall" />
        </Waterfall>
      </template>

      <div v-if="loadingMore" class="loading loading-more-inline">
        <div class="spinner"></div>
      </div>

      <div v-if="!loadingMore && displayRecipes.length > 0 && hasMore" class="load-more">
        <button class="btn btn-outline" @click="loadMore">加载更多</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecipeCard from '@/components/RecipeCard.vue'
import LayoutSwitcher from '@/components/LayoutSwitcher.vue'
import Waterfall from '@/components/Waterfall.vue'
import { recipeApi } from '@/utils/api'
import { useRecipeStore } from '@/store/recipe'

const route = useRoute()
const router = useRouter()
const store = useRecipeStore()

const loading = ref(true)
const loadingMore = ref(false)
const sortBy = ref('hot')
const hotDimension = ref('total')
const layout = ref('grid')
const pageSize = 4
const currentPage = ref(1)
const hotRecipesMap = ref({})
const loadedHotDimensions = ref(new Set())

const syncLocalHotRecipesFavoriteCounts = () => {
  const dimensions = Object.keys(hotRecipesMap.value)
  dimensions.forEach(dim => {
    const list = hotRecipesMap.value[dim]
    if (!list) return
    list.forEach(recipe => {
      const storeRecipe = store.hotRecipes.find(r => r.id === recipe.id)
        || store.recipes.find(r => r.id === recipe.id)
        || store.favorites.find(r => r.id === recipe.id)
      if (storeRecipe && typeof storeRecipe.favoriteCount === 'number') {
        recipe.favoriteCount = storeRecipe.favoriteCount
      }
    })
  })
  allRecipes.value.forEach(recipe => {
    const storeRecipe = store.hotRecipes.find(r => r.id === recipe.id)
      || store.recipes.find(r => r.id === recipe.id)
      || store.favorites.find(r => r.id === recipe.id)
    if (storeRecipe && typeof storeRecipe.favoriteCount === 'number') {
      recipe.favoriteCount = storeRecipe.favoriteCount
    }
  })
}

watch(
  () => store.favoriteVersion,
  () => {
    syncLocalHotRecipesFavoriteCounts()
  }
)

const showSuggestions = ref(false)
const activeSuggestionIndex = ref(-1)
const searchInputRef = ref(null)
const suggestionListRef = ref(null)

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

const allRecipes = ref([
  { id: 1, title: '红烧五花肉', description: '经典家常菜，肥而不腻，入口即化', coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=400&h=300&fit=crop', difficulty: 2, cookTime: 60, tags: ['川菜', '家常菜', '晚餐'], author: '美食达人', favoriteCount: 1286, createdAt: '2024-01-15' },
  { id: 2, title: '番茄炒蛋', description: '最简单也最困难的国民家常菜', coverImage: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=450&fit=crop', difficulty: 1, cookTime: 15, tags: ['家常菜', '快手菜', '午餐'], author: '小厨神', favoriteCount: 2341, createdAt: '2024-01-18' },
  { id: 3, title: '宫保鸡丁', description: '川菜经典，鸡肉滑嫩，花生酥脆', coverImage: 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=400&h=280&fit=crop', difficulty: 2, cookTime: 30, tags: ['川菜', '经典', '晚餐'], author: '川菜大师', favoriteCount: 1892, createdAt: '2024-01-10' },
  { id: 4, title: '清蒸鲈鱼', description: '粤式蒸鱼，鲜嫩爽滑，原汁原味', coverImage: 'https://images.unsplash.com/photo-1534604973900-c43ab4c2e0ab?w=400&h=350&fit=crop', difficulty: 2, cookTime: 25, tags: ['粤菜', '海鲜', '宴请'], author: '粤菜名厨', favoriteCount: 1567, createdAt: '2024-01-12' },
  { id: 5, title: '麻婆豆腐', description: '麻辣鲜香，下饭神器，豆腐嫩滑入味', coverImage: 'https://images.unsplash.com/photo-1582576163090-09d3b6f8a969?w=400&h=400&fit=crop', difficulty: 1, cookTime: 20, tags: ['川菜', '家常菜', '午餐'], author: '四川厨子', favoriteCount: 2103, createdAt: '2024-01-20' },
  { id: 6, title: '白切鸡', description: '粤菜经典，皮爽肉滑，原汁原味', coverImage: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400&h=320&fit=crop', difficulty: 2, cookTime: 45, tags: ['粤菜', '经典', '宴请'], author: '广东阿婆', favoriteCount: 1456, createdAt: '2024-01-08' },
  { id: 7, title: '糖醋排骨', description: '酸甜可口，老少皆宜，外酥里嫩', coverImage: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=400&h=380&fit=crop', difficulty: 2, cookTime: 40, tags: ['家常菜', '浙菜', '晚餐'], author: '巧手妈妈', favoriteCount: 1789, createdAt: '2024-01-16' },
  { id: 8, title: '水煮鱼', description: '麻辣鲜香，鱼片滑嫩，香辣过瘾', coverImage: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=420&fit=crop', difficulty: 3, cookTime: 50, tags: ['川菜', '海鲜', '夜宵'], author: '辣味人生', favoriteCount: 1234, createdAt: '2024-01-14' },
  { id: 9, title: '鱼香肉丝', description: '咸甜酸辣兼备，葱姜蒜香浓郁', coverImage: 'https://images.unsplash.com/photo-1563379926898-05f4575a45d8?w=400&h=340&fit=crop', difficulty: 2, cookTime: 25, tags: ['川菜', '家常菜', '午餐'], author: '川菜大师', favoriteCount: 1678, createdAt: '2024-01-05' },
  { id: 10, title: '扬州炒饭', description: '粒粒分明，香气扑鼻，配料丰富', coverImage: 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400&h=290&fit=crop', difficulty: 1, cookTime: 20, tags: ['苏菜', '家常菜', '午餐'], author: '江南小厨', favoriteCount: 1945, createdAt: '2024-01-02' },
  { id: 11, title: '可乐鸡翅', description: '甜香浓郁，肉质鲜嫩，新手必学', coverImage: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400&h=370&fit=crop', difficulty: 1, cookTime: 30, tags: ['家常菜', '快手菜', '晚餐'], author: '新手厨师', favoriteCount: 2234, createdAt: '2024-01-19' },
  { id: 12, title: '蒜蓉西兰花', description: '清淡健康，蒜香浓郁，翠绿爽脆', coverImage: 'https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?w=400&h=310&fit=crop', difficulty: 1, cookTime: 15, tags: ['家常菜', '素菜', '健康'], author: '健康达人', favoriteCount: 1123, createdAt: '2024-01-11' }
])

const allFilteredRecipes = computed(() => {
  if (sortBy.value === 'hot' && loadedHotDimensions.value.has(hotDimension.value)) {
    const hotList = hotRecipesMap.value[hotDimension.value] || []
    return hotList.filter(r => {
      if (filters.value.cuisine && !r.tags?.includes(filters.value.cuisine)) return false
      if (filters.value.scene && !r.tags?.includes(filters.value.scene)) return false
      if (filters.value.difficulty && r.difficulty !== parseInt(filters.value.difficulty)) return false
      if (filters.value.keyword) {
        const kw = filters.value.keyword.toLowerCase()
        if (!r.title?.toLowerCase().includes(kw) && !r.description?.toLowerCase().includes(kw)) return false
      }
      return true
    })
  }

  let result = [...allRecipes.value]

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

const displayRecipes = computed(() => {
  const count = currentPage.value * pageSize
  return allFilteredRecipes.value.slice(0, Math.min(count, allFilteredRecipes.value.length))
})

const hasMore = computed(() => displayRecipes.value.length < allFilteredRecipes.value.length)

const allRecipeTitles = computed(() => {
  if (sortBy.value === 'hot' && loadedHotDimensions.value.has(hotDimension.value)) {
    const hotList = hotRecipesMap.value[hotDimension.value] || []
    return [...hotList, ...allRecipes.value]
  }
  return allRecipes.value
})

const searchSuggestions = computed(() => {
  const kw = filters.value.keyword.trim().toLowerCase()
  if (!kw) return []
  const matched = allRecipeTitles.value.filter(r =>
    r.title?.toLowerCase().includes(kw)
  )
  const seen = new Set()
  return matched.filter(r => {
    if (seen.has(r.id)) return false
    seen.add(r.id)
    return true
  }).slice(0, 8)
})

watch([filters, sortBy], () => {
  currentPage.value = 1
})

watch(() => filters.value.keyword, () => {
  activeSuggestionIndex.value = -1
  showSuggestions.value = filters.value.keyword.trim().length > 0
})

const handleLayoutChange = (newLayout) => {
  layout.value = newLayout
}

const handleSortChange = (sort) => {
  sortBy.value = sort
  currentPage.value = 1
  if (sort === 'hot') {
    loadHotRecipes(hotDimension.value)
  }
}

const changeHotDimension = async (dimension) => {
  if (hotDimension.value === dimension) return
  hotDimension.value = dimension
  currentPage.value = 1
  await loadHotRecipes(dimension)
}

const loadHotRecipes = async (dimension) => {
  if (loadedHotDimensions.value.has(dimension)) {
    return
  }
  try {
    const data = await recipeApi.getHotRecipes(dimension)
    const formatted = (data || []).map(r => ({
      ...r,
      tags: r.tags ? r.tags.split(',') : []
    }))
    hotRecipesMap.value = {
      ...hotRecipesMap.value,
      [dimension]: formatted
    }
    loadedHotDimensions.value = new Set([...loadedHotDimensions.value, dimension])
  } catch (e) {
    console.log('热门榜单加载失败，使用模拟数据')
    loadedHotDimensions.value = new Set([...loadedHotDimensions.value, dimension])
    hotRecipesMap.value = {
      ...hotRecipesMap.value,
      [dimension]: []
    }
  }
}

onMounted(async () => {
  if (route.query.category) {
    filters.value.cuisine = route.query.category
  }
  try {
    await loadHotRecipes(hotDimension.value)
  } catch (e) {
    console.log('使用模拟数据')
  }
  loading.value = false
  window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})

const updateFilter = (key, value) => {
  filters.value[key] = value
}

const handleSearch = () => {
}

const handleKeydown = (e) => {
  if (!showSuggestions.value || searchSuggestions.value.length === 0) return

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeSuggestionIndex.value = (activeSuggestionIndex.value + 1) % searchSuggestions.value.length
    scrollActiveSuggestionIntoView()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (activeSuggestionIndex.value === -1) {
      activeSuggestionIndex.value = searchSuggestions.value.length - 1
    } else {
      activeSuggestionIndex.value = (activeSuggestionIndex.value - 1 + searchSuggestions.value.length) % searchSuggestions.value.length
    }
    scrollActiveSuggestionIntoView()
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (activeSuggestionIndex.value >= 0 && activeSuggestionIndex.value < searchSuggestions.value.length) {
      goToRecipeDetail(searchSuggestions.value[activeSuggestionIndex.value])
    }
  } else if (e.key === 'Escape') {
    showSuggestions.value = false
    activeSuggestionIndex.value = -1
  }
}

const scrollActiveSuggestionIntoView = async () => {
  await nextTick()
  if (!suggestionListRef.value) return
  const activeEl = suggestionListRef.value.querySelector('.suggestion-item.active')
  if (activeEl) {
    activeEl.scrollIntoView({ block: 'nearest' })
  }
}

const goToRecipeDetail = (recipe) => {
  showSuggestions.value = false
  activeSuggestionIndex.value = -1
  router.push(`/recipe/${recipe.id}`)
}

const handleSuggestionHover = (index) => {
  activeSuggestionIndex.value = index
}

const handleSearchBlur = () => {
  setTimeout(() => {
    showSuggestions.value = false
  }, 150)
}

const handleSearchFocus = () => {
  if (filters.value.keyword.trim().length > 0) {
    showSuggestions.value = true
  }
}

const highlightMatch = (text) => {
  const kw = filters.value.keyword.trim()
  if (!kw) return text
  const regex = new RegExp(`(${kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<em class="highlight">$1</em>')
}

const loadMore = () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  setTimeout(() => {
    currentPage.value++
    loadingMore.value = false
  }, 600)
}

const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight
  
  if (scrollTop + windowHeight >= documentHeight - 100) {
    loadMore()
  }
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
    pointer-events: none;
  }
}

.suggestions-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  max-height: 320px;
  overflow-y: auto;
  z-index: 100;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  gap: 10px;

  &:hover,
  &.active {
    background-color: var(--bg-secondary);
  }

  &.active {
    background-color: rgba(255, 107, 53, 0.08);
  }
}

.suggestion-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.suggestion-title {
  font-size: 14px;
  color: var(--text-primary);

  :deep(.highlight) {
    color: var(--primary-color);
    font-style: normal;
    font-weight: 600;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
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

.sort-bar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.sort-options {
  display: flex;
  gap: 16px;
}

.hot-dimension-tabs {
  display: flex;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 4px;
  gap: 2px;
}

.tab-item {
  padding: 6px 14px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;

  &:hover {
    color: var(--text-primary);
  }

  &.active {
    background: white;
    color: var(--primary-color);
    font-weight: 500;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  }
}

.loading-more-inline {
  padding: 20px 0;
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