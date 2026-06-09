<template>
  <div class="home-page">
    <section class="hero">
      <div class="container">
        <div class="hero-content">
          <h1 class="hero-title">
            <span class="highlight">家常美味</span>
            <br />
            与家人一起分享
          </h1>
          <p class="hero-desc">
            在这里，每一道菜都有故事。记录你的独家配方，
            <br />
            发现更多美食灵感，让烹饪成为一种享受。
          </p>
          <div class="hero-actions">
            <router-link to="/create" class="btn btn-primary btn-lg">
              开始创作菜谱
            </router-link>
            <router-link to="/recipes" class="btn btn-outline btn-lg">
              浏览全部菜谱
            </router-link>
          </div>
        </div>
        <div class="hero-image">
          <img src="https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=600&h=500&fit=crop" alt="美食" />
        </div>
      </div>
    </section>

    <section class="section" ref="hotSectionRef">
      <div class="container">
        <div class="section-header">
          <div class="section-title-wrapper">
            <h2 class="section-title">🔥 热门菜谱</h2>
            <div class="hot-dimension-tabs">
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
          </div>
          <LayoutSwitcher :layout="hotLayout" @change="hotLayout = $event" />
        </div>
        <div v-if="loading || isSwitchingDimension || !loadedHotDimensions.has(hotDimension)" class="loading">
          <div class="spinner"></div>
        </div>
        <template v-else>
          <div v-if="displayHotRecipes.length === 0" class="empty-state">
            <div class="empty-icon">🔥</div>
            <h3>该时段暂无热门菜谱</h3>
            <p>试试切换其他时间维度查看</p>
          </div>
          <template v-else>
            <div v-if="hotLayout === 'grid'" class="recipe-grid">
              <RecipeCard v-for="recipe in displayHotRecipes" :key="recipe.id" :recipe="recipe" mode="grid" />
            </div>
            <Waterfall v-else :items="displayHotRecipes" :columns="4" v-slot="{ item }">
              <RecipeCard :recipe="item" mode="waterfall" />
            </Waterfall>
          </template>
        </template>
        <div v-if="loadingMore" class="loading loading-more">
          <div class="spinner"></div>
        </div>
      </div>
    </section>

    <section class="section category-section">
      <div class="container">
        <h2 class="section-title">🍽️ 按菜系浏览</h2>
        <div class="category-grid">
          <div
            v-for="cat in categories"
            :key="cat.value"
            class="category-card"
            @click="filterByCategory(cat.value)"
          >
            <span class="category-icon">{{ cat.icon }}</span>
            <span class="category-name">{{ cat.label }}</span>
            <span class="category-count">{{ cat.count }}道菜谱</span>
          </div>
        </div>
      </div>
    </section>

    <section class="section features">
      <div class="container">
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">📝</div>
            <h3>原创编撰</h3>
            <p>支持Markdown富文本编辑，图文并茂记录每一道菜的制作过程</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">⭐</div>
            <h3>收藏管理</h3>
            <p>一键收藏心仪菜谱，打造专属美食收藏夹</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🏷️</div>
            <h3>智能分类</h3>
            <p>按菜系、场景、难度多维度筛选，快速找到想要的菜谱</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">💾</div>
            <h3>草稿保存</h3>
            <p>支持草稿自动保存，随时继续未完成的菜谱创作</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useRecipeStore } from '@/store/recipe'
import { recipeApi } from '@/utils/api'
import RecipeCard from '@/components/RecipeCard.vue'
import LayoutSwitcher from '@/components/LayoutSwitcher.vue'
import Waterfall from '@/components/Waterfall.vue'

const router = useRouter()
const store = useRecipeStore()
const loading = ref(true)
const loadingMore = ref(false)
const hotLayout = ref('grid')
const hotDimension = ref('monthly')
const pageSize = 4
const hotSectionRef = ref(null)
const hotRecipesMap = ref({})
const loadedHotDimensions = ref(new Set())
const hotPagesMap = ref({})
const isSwitchingDimension = ref(false)

const mockHotRecipes = [
  { id: 1, title: '红烧五花肉', description: '经典家常菜，肥而不腻，入口即化', coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=400&h=300&fit=crop', difficulty: 2, cookTime: 60, tags: ['川菜', '家常菜'], author: '美食达人', favoriteCount: 1286 },
  { id: 2, title: '番茄炒蛋', description: '最简单也最困难的国民家常菜', coverImage: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=450&fit=crop', difficulty: 1, cookTime: 15, tags: ['家常菜', '快手菜'], author: '小厨神', favoriteCount: 2341 },
  { id: 3, title: '宫保鸡丁', description: '川菜经典，鸡肉滑嫩，花生酥脆', coverImage: 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=400&h=280&fit=crop', difficulty: 2, cookTime: 30, tags: ['川菜', '经典'], author: '川菜大师', favoriteCount: 1892 },
  { id: 4, title: '清蒸鲈鱼', description: '粤式蒸鱼，鲜嫩爽滑，原汁原味', coverImage: 'https://images.unsplash.com/photo-1534604973900-c43ab4c2e0ab?w=400&h=350&fit=crop', difficulty: 2, cookTime: 25, tags: ['粤菜', '海鲜'], author: '粤菜名厨', favoriteCount: 1567 },
  { id: 5, title: '麻婆豆腐', description: '麻辣鲜香，下饭神器，豆腐嫩滑入味', coverImage: 'https://images.unsplash.com/photo-1582576163090-09d3b6f8a969?w=400&h=400&fit=crop', difficulty: 1, cookTime: 20, tags: ['川菜', '家常菜', '午餐'], author: '四川厨子', favoriteCount: 2103 },
  { id: 6, title: '白切鸡', description: '粤菜经典，皮爽肉滑，原汁原味', coverImage: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400&h=320&fit=crop', difficulty: 2, cookTime: 45, tags: ['粤菜', '经典', '宴请'], author: '广东阿婆', favoriteCount: 1456 },
  { id: 7, title: '糖醋排骨', description: '酸甜可口，老少皆宜，外酥里嫩', coverImage: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=400&h=380&fit=crop', difficulty: 2, cookTime: 40, tags: ['家常菜', '浙菜', '晚餐'], author: '巧手妈妈', favoriteCount: 1789 },
  { id: 8, title: '水煮鱼', description: '麻辣鲜香，鱼片滑嫩，香辣过瘾', coverImage: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=420&fit=crop', difficulty: 3, cookTime: 50, tags: ['川菜', '海鲜', '夜宵'], author: '辣味人生', favoriteCount: 1234 }
]

const getCurrentHotPage = () => hotPagesMap.value[hotDimension.value] || 1

const getCurrentSource = () => {
  if (loadedHotDimensions.value.has(hotDimension.value)) {
    return hotRecipesMap.value[hotDimension.value] || []
  }
  return []
}

const displayHotRecipes = computed(() => {
  const source = getCurrentSource()
  const count = getCurrentHotPage() * pageSize
  return source.slice(0, Math.min(count, source.length))
})

const hasMoreHot = computed(() => {
  const source = getCurrentSource()
  return displayHotRecipes.value.length < source.length
})

const categories = ref([
  { value: '川菜', label: '川菜', icon: '🌶️', count: 128 },
  { value: '粤菜', label: '粤菜', icon: '🥢', count: 96 },
  { value: '家常菜', label: '家常菜', icon: '🏠', count: 256 },
  { value: '甜点', label: '甜点', icon: '🍰', count: 64 },
  { value: '汤羹', label: '汤羹', icon: '🍲', count: 72 },
  { value: '凉菜', label: '凉菜', icon: '🥗', count: 48 }
])

const loadMoreHot = () => {
  if (loadingMore.value || !hasMoreHot.value || isSwitchingDimension.value) return
  loadingMore.value = true
  setTimeout(() => {
    const currentPage = getCurrentHotPage()
    hotPagesMap.value = {
      ...hotPagesMap.value,
      [hotDimension.value]: currentPage + 1
    }
    loadingMore.value = false
  }, 600)
}

const handleScroll = () => {
  if (!hotSectionRef.value || isSwitchingDimension.value) return
  const rect = hotSectionRef.value.getBoundingClientRect()
  if (rect.bottom <= window.innerHeight + 100) {
    loadMoreHot()
  }
}

onMounted(async () => {
  hotPagesMap.value = {
    ...hotPagesMap.value,
    [hotDimension.value]: 1
  }
  try {
    await loadHotRecipes(hotDimension.value)
  } catch (e) {
    console.log('使用模拟数据')
  } finally {
    loading.value = false
  }
  window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})

const changeHotDimension = async (dimension) => {
  if (hotDimension.value === dimension || isSwitchingDimension.value) return
  isSwitchingDimension.value = true
  hotDimension.value = dimension
  hotPagesMap.value = {
    ...hotPagesMap.value,
    [dimension]: hotPagesMap.value[dimension] || 1
  }
  try {
    await loadHotRecipes(dimension)
  } finally {
    isSwitchingDimension.value = false
  }
}

const loadHotRecipes = async (dimension = hotDimension.value) => {
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
    if (formatted.length > 0) {
      store.setHotRecipes(formatted)
    }
  } catch (e) {
    console.log('热门榜单加载失败，使用空列表')
    loadedHotDimensions.value = new Set([...loadedHotDimensions.value, dimension])
    hotRecipesMap.value = {
      ...hotRecipesMap.value,
      [dimension]: []
    }
  }
}

const filterByCategory = (category) => {
  router.push({ path: '/recipes', query: { category } })
}
</script>

<style lang="scss" scoped>
.home-page {
  overflow: hidden;
}

.hero {
  padding: 80px 0;
  background: linear-gradient(135deg, #fef7ed 0%, #fff7ed 50%, #fef3c7 100%);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -20%;
    width: 800px;
    height: 800px;
    background: radial-gradient(circle, rgba(230, 126, 34, 0.1) 0%, transparent 70%);
    border-radius: 50%;
  }
}

.hero .container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 24px;
  
  .highlight {
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.hero-desc {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 32px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 16px;
}

.btn-lg {
  padding: 16px 32px;
  font-size: 16px;
}

.hero-image {
  position: relative;
  
  img {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    width: 100%;
    height: 400px;
    object-fit: cover;
  }
  
  &::before {
    content: '';
    position: absolute;
    inset: -16px;
    border: 3px solid var(--primary-color);
    border-radius: var(--radius-lg);
    opacity: 0.3;
    z-index: -1;
    transform: rotate(3deg);
  }
}

.section {
  padding: 80px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  gap: 24px;
}

.hot-dimension-tabs {
  display: flex;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 4px;
  gap: 2px;
}

.hot-dimension-tabs .tab-item {
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

.loading-more {
  padding-top: 24px;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: var(--radius-md);

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
    margin-bottom: 0;
  }
}

.category-section {
  background: var(--bg-tertiary);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

.category-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 24px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-sm);
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
    
    .category-icon {
      transform: scale(1.1);
    }
  }
}

.category-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 12px;
  transition: transform 0.3s ease;
}

.category-name {
  display: block;
  font-weight: 600;
  margin-bottom: 4px;
}

.category-count {
  font-size: 12px;
  color: var(--text-secondary);
}

.features {
  background: white;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
}

.feature-card {
  text-align: center;
  padding: 32px 24px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  transition: all 0.3s ease;
  
  &:hover {
    background: var(--bg-tertiary);
    transform: translateY(-4px);
  }
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
}

.feature-card p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .hero .container {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .hero-image {
    order: -1;
  }
  
  .recipe-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>