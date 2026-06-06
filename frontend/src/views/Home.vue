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

    <section class="section">
      <div class="container">
        <h2 class="section-title">🔥 本月热门菜谱</h2>
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
        </div>
        <div v-else class="recipe-grid">
          <RecipeCard v-for="recipe in hotRecipes" :key="recipe.id" :recipe="recipe" />
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
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRecipeStore } from '@/store/recipe'
import { recipeApi } from '@/utils/api'
import RecipeCard from '@/components/RecipeCard.vue'

const router = useRouter()
const store = useRecipeStore()
const loading = ref(true)

const hotRecipes = ref([
  { id: 1, title: '红烧五花肉', description: '经典家常菜，肥而不腻，入口即化', coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=400&h=300&fit=crop', difficulty: 2, cookTime: 60, tags: ['川菜', '家常菜'], author: '美食达人', favoriteCount: 1286 },
  { id: 2, title: '番茄炒蛋', description: '最简单也最困难的国民家常菜', coverImage: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop', difficulty: 1, cookTime: 15, tags: ['家常菜', '快手菜'], author: '小厨神', favoriteCount: 2341 },
  { id: 3, title: '宫保鸡丁', description: '川菜经典，鸡肉滑嫩，花生酥脆', coverImage: 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=400&h=300&fit=crop', difficulty: 2, cookTime: 30, tags: ['川菜', '经典'], author: '川菜大师', favoriteCount: 1892 },
  { id: 4, title: '清蒸鲈鱼', description: '粤式蒸鱼，鲜嫩爽滑，原汁原味', coverImage: 'https://images.unsplash.com/photo-1534604973900-c43ab4c2e0ab?w=400&h=300&fit=crop', difficulty: 2, cookTime: 25, tags: ['粤菜', '海鲜'], author: '粤菜名厨', favoriteCount: 1567 }
])

const categories = ref([
  { value: '川菜', label: '川菜', icon: '🌶️', count: 128 },
  { value: '粤菜', label: '粤菜', icon: '🥢', count: 96 },
  { value: '家常菜', label: '家常菜', icon: '🏠', count: 256 },
  { value: '甜点', label: '甜点', icon: '🍰', count: 64 },
  { value: '汤羹', label: '汤羹', icon: '🍲', count: 72 },
  { value: '凉菜', label: '凉菜', icon: '🥗', count: 48 }
])

onMounted(async () => {
  try {
    await loadHotRecipes()
  } catch (e) {
    console.log('使用模拟数据')
  } finally {
    loading.value = false
  }
})

const loadHotRecipes = async () => {
  const data = await recipeApi.getHotRecipes()
  if (data && data.length) {
    hotRecipes.value = data
    store.setHotRecipes(data)
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

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
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