<template>
  <div class="recipe-detail" v-if="recipe">
    <div class="container">
      <div class="detail-header">
        <div class="header-content">
          <h1 class="recipe-title">{{ recipe.title }}</h1>
          <p class="recipe-desc">{{ recipe.description }}</p>
          
          <div class="recipe-meta">
            <div class="meta-item">
              <span class="meta-icon">👨‍🍳</span>
              <span>{{ recipe.author }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-icon">⏱️</span>
              <span>{{ recipe.cookTime }}分钟</span>
            </div>
            <div class="meta-item">
              <span class="meta-icon">📊</span>
              <span>{{ getDifficultyLabel(recipe.difficulty) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-icon">❤️</span>
              <span>{{ recipe.favoriteCount }}收藏</span>
            </div>
            <div class="meta-item rating-item">
              <span class="meta-icon">⭐</span>
              <span>{{ recipe.averageRating || '0.0' }}分 ({{ recipe.ratingCount || 0 }}人评价)</span>
            </div>
          </div>
          
          <div class="recipe-tags">
            <span v-for="tag in recipe.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          
          <div class="action-buttons no-print">
            <button 
              class="btn btn-primary" 
              :class="{ favorited: isFavorited }"
              @click="toggleFavorite"
            >
              <svg v-if="!isFavorited" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              {{ isFavorited ? '已收藏' : '收藏菜谱' }}
            </button>
            <div class="btn-rating-wrapper">
              <span class="rating-label">给这道菜打分：</span>
              <StarRating
                v-if="ratingStats"
                v-model="userRating"
                :average-score="ratingStats.averageScore"
                :rating-count="ratingStats.ratingCount"
                :show-text="false"
                :readonly="submittingRating"
                @rate="handleRate"
              />
              <div v-else-if="loadingRating" class="rating-loading">
                <div class="spinner spinner-sm"></div>
              </div>
            </div>
            <button 
              class="btn btn-outline btn-shopping" 
              @click="addToShoppingList"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="9" cy="21" r="1"/>
                <circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
              加入购物清单
            </button>
            <button class="btn btn-outline" @click="shareRecipe">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="18" cy="5" r="3"/>
                <circle cx="6" cy="12" r="3"/>
                <circle cx="18" cy="19" r="3"/>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
              </svg>
              分享
            </button>
            <button class="btn btn-outline" @click="printRecipe">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 6 2 18 2 18 9"/>
                <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/>
                <rect x="6" y="14" width="12" height="8"/>
              </svg>
              打印
            </button>
          </div>
        </div>
        
        <div class="header-image">
          <img 
            :src="recipe.coverImage || defaultImage" 
            :key="'detail-cover-' + recipe.id + '-' + (recipe.coverImage || defaultImage)"
            :alt="recipe.title" 
            @error="handleImageError"
          />
        </div>
      </div>

      <div class="detail-content">
        <div class="ingredients-section section-card">
          <div class="ingredients-header">
            <div class="section-title-wrapper">
              <h2 class="section-title">🥬 食材清单</h2>
              <span class="print-only servings-print-info">
                ({{ currentServings }}人份)
              </span>
            </div>
            <div class="ingredients-actions no-print">
              <button
                class="btn btn-outline btn-add-shopping"
                @click="addToShoppingList"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="9" cy="21" r="1"/>
                  <circle cx="20" cy="21" r="1"/>
                  <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
                </svg>
                全部加入购物清单
              </button>
            </div>
            <div class="servings-control no-print">
              <div class="servings-label">
                <span class="servings-icon">👥</span>
                <span>份数</span>
              </div>
              <div class="servings-controls-wrapper">
                <button 
                  class="servings-btn minus" 
                  @click="decreaseServings"
                  :disabled="currentServings <= minServings"
                >−</button>
                <input 
                  type="number" 
                  v-model.number="currentServings" 
                  class="servings-input"
                  :min="minServings"
                  :max="maxServings"
                  @change="clampServings"
                />
                <button 
                  class="servings-btn plus" 
                  @click="increaseServings"
                  :disabled="currentServings >= maxServings"
                >+</button>
              </div>
              <div class="servings-slider-wrapper">
                <input 
                  type="range" 
                  v-model.number="currentServings" 
                  :min="minServings" 
                  :max="maxServings" 
                  step="1"
                  class="servings-slider"
                />
                <div class="slider-marks">
                  <span>{{ minServings }}</span>
                  <span>{{ Math.round((minServings + maxServings) / 2) }}</span>
                  <span>{{ maxServings }}</span>
                </div>
              </div>
              <button 
                class="reset-btn" 
                @click="resetServings"
                :disabled="currentServings === originalServings"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
                  <path d="M3 3v5h5"/>
                </svg>
                恢复原始
              </button>
            </div>
          </div>
          <table class="ingredients-table">
            <thead>
              <tr>
                <th>食材</th>
                <th>用量</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in scaledIngredients" :key="index">
                <td>{{ item.name }}</td>
                <td class="amount-cell">
                  <span class="amount-value" :class="{ 'amount-changed': currentServings !== originalServings }">
                    {{ item.amount }}
                  </span>
                </td>
                <td>{{ item.note || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="nutrition-section section-card">
          <div class="nutrition-header">
            <h2 class="section-title">📊 营养成分</h2>
            <span class="print-only nutrition-print-info">
              (每份)
            </span>
          </div>
          
          <div v-if="loadingNutrition" class="nutrition-loading">
            <div class="spinner"></div>
            <span>正在计算营养成分...</span>
          </div>
          
          <div v-else-if="nutritionData" class="nutrition-content">
            <div class="nutrition-summary">
              <div class="nutrition-item calories">
                <div class="nutrition-value">{{ nutritionData.totalCalories || 0 }}</div>
                <div class="nutrition-label">热量 (千卡)</div>
              </div>
              <div class="nutrition-item protein">
                <div class="nutrition-value">{{ nutritionData.totalProtein || 0 }}g</div>
                <div class="nutrition-label">蛋白质</div>
              </div>
              <div class="nutrition-item fat">
                <div class="nutrition-value">{{ nutritionData.totalFat || 0 }}g</div>
                <div class="nutrition-label">脂肪</div>
              </div>
              <div class="nutrition-item carb">
                <div class="nutrition-value">{{ nutritionData.totalCarbohydrate || 0 }}g</div>
                <div class="nutrition-label">碳水化合物</div>
              </div>
              <div class="nutrition-item fiber">
                <div class="nutrition-value">{{ nutritionData.totalFiber || 0 }}g</div>
                <div class="nutrition-label">膳食纤维</div>
              </div>
              <div class="nutrition-item sodium">
                <div class="nutrition-value">{{ nutritionData.totalSodium || 0 }}mg</div>
                <div class="nutrition-label">钠</div>
              </div>
            </div>
            
            <div v-if="nutritionData.unmatchedIngredients && nutritionData.unmatchedIngredients.length > 0" class="nutrition-warning">
              <span class="warning-icon">⚠️</span>
              <span>以下食材暂无营养数据：{{ nutritionData.unmatchedIngredients.join('、') }}</span>
            </div>
          </div>
          
          <div v-else class="nutrition-empty">
            <span class="empty-icon">📋</span>
            <span>暂无营养数据</span>
          </div>
        </div>

        <div class="steps-section section-card">
          <h2 class="section-title">👨‍🍳 烹饪步骤</h2>
          <div class="steps-list">
            <div v-for="(step, index) in recipe.steps" :key="index" class="step-item">
              <div class="step-header">
                <div class="step-number">{{ index + 1 }}</div>
                <div class="no-print">
                  <StepTimer 
                    :recipe-id="recipe.id" 
                    :step-index="index"
                    :default-minutes="getDefaultStepTime(index, step)"
                  />
                </div>
                <span class="print-only step-time-print">
                  ⏱️ 建议时间: {{ getDefaultStepTime(index, step) }}分钟
                </span>
              </div>
              <div class="step-content">
                <div v-if="step.image" class="step-image">
                  <img 
                    :src="step.image" 
                    :key="'detail-step-' + recipe.id + '-' + index + '-' + step.image"
                    :alt="'步骤' + (index + 1)" 
                    @error="handleStepImageError"
                  />
                </div>
                <p class="step-text">{{ step.content }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="tips-section section-card" v-if="recipe.tips">
          <h2 class="section-title">💡 小贴士</h2>
          <div class="tips-content">
            {{ recipe.tips }}
          </div>
        </div>
      </div>

      <div class="similar-section section-card no-print" v-if="similarRecipes.length > 0 || loadingSimilar">
        <div class="similar-header">
          <h2 class="section-title">✨ 猜你喜欢</h2>
          <div class="similar-nav">
            <button class="nav-btn" @click="scrollSimilar('left')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"/>
              </svg>
            </button>
            <button class="nav-btn" @click="scrollSimilar('right')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="similar-scroll-wrapper">
          <div class="similar-scroll-container" v-if="!loadingSimilar">
            <div class="similar-card-wrapper" v-for="item in similarRecipes" :key="item.id">
              <RecipeCard :recipe="item" mode="grid" />
            </div>
          </div>
          <div class="similar-loading" v-else>
            <div class="spinner"></div>
            <span>正在为您推荐相似菜谱...</span>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <div v-else class="loading-full">
    <div class="spinner"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useRecipeStore } from '@/store/recipe'
import StepTimer from '@/components/StepTimer.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import StarRating from '@/components/StarRating.vue'
import { recipeApi, ingredientNutritionApi, shoppingListApi, ratingApi } from '@/utils/api'

const route = useRoute()
const store = useRecipeStore()

const recipe = ref(null)
const similarRecipes = ref([])
const loadingSimilar = ref(false)
const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&h=500&fit=crop'

const nutritionData = ref(null)
const loadingNutrition = ref(false)

const ratingStats = ref(null)
const loadingRating = ref(false)
const userRating = ref(0)
const submittingRating = ref(false)

const minServings = 1
const maxServings = 20

const originalServings = computed(() => store.getOriginalServings(route.params.id) || 1)
const currentServings = computed({
  get: () => store.getServings(route.params.id) || originalServings.value,
  set: (val) => store.setServings(route.params.id, val)
})

const isFavorited = computed(() => store.isFavorite(route.params.id))

const parseRecipeData = (data) => {
  if (!data) return null
  const result = { ...data }
  if (typeof result.tags === 'string') {
    result.tags = result.tags.split(',').map(t => t.trim()).filter(t => t)
  } else if (!result.tags) {
    result.tags = []
  }
  if (typeof result.ingredients === 'string') {
    try {
      result.ingredients = JSON.parse(result.ingredients)
    } catch {
      result.ingredients = []
    }
  } else if (!result.ingredients) {
    result.ingredients = []
  }
  if (typeof result.steps === 'string') {
    try {
      result.steps = JSON.parse(result.steps)
    } catch {
      result.steps = []
    }
  } else if (!result.steps) {
    result.steps = []
  }
  if (result.averageRating === undefined || result.averageRating === null) {
    result.averageRating = '0.0'
  }
  if (result.ratingCount === undefined || result.ratingCount === null) {
    result.ratingCount = 0
  }
  if (result.ingredients && result.ingredients.length > 0) {
    const firstItem = result.ingredients[0]
    if (firstItem.servings) {
      const origServings = parseInt(firstItem.servings) || 1
      store.setOriginalServings(data.id, origServings)
      if (!store.getServings(data.id)) {
        store.setServings(data.id, origServings)
      }
    } else {
      store.setOriginalServings(data.id, 1)
      if (!store.getServings(data.id)) {
        store.setServings(data.id, 1)
      }
    }
  } else {
    store.setOriginalServings(data.id, 1)
    if (!store.getServings(data.id)) {
      store.setServings(data.id, 1)
    }
  }
  return result
}

const normalizeRecipeForCard = (r) => {
  if (!r) return r
  const result = { ...r }
  if (typeof result.tags === 'string') {
    result.tags = result.tags.split(',').map(t => t.trim()).filter(t => t)
  }
  return result
}

const parseAmount = (amountStr) => {
  if (!amountStr || amountStr === '适量') {
    return { value: null, unit: amountStr || '适量', isFraction: false }
  }
  
  const fractionMap = {
    '½': 0.5, '¼': 0.25, '¾': 0.75,
    '⅓': 0.333, '⅔': 0.667,
    '⅕': 0.2, '⅖': 0.4, '⅗': 0.6, '⅘': 0.8,
    '⅙': 0.167, '⅚': 0.833,
    '⅛': 0.125, '⅜': 0.375, '⅝': 0.625, '⅞': 0.875
  }
  
  let trimmed = amountStr.trim()
  
  for (const [frac, val] of Object.entries(fractionMap)) {
    if (trimmed.startsWith(frac)) {
      const rest = trimmed.slice(frac.length).trim()
      return { value: val, unit: rest, isFraction: true }
    }
  }
  
  const mixedFractionMatch = trimmed.match(/^(\d+)\s*(½|¼|¾|⅓|⅔|⅕|⅖|⅗|⅘|⅙|⅚|⅛|⅜|⅝|⅞)\s*(.*)$/)
  if (mixedFractionMatch) {
    const intVal = parseInt(mixedFractionMatch[1])
    const fracVal = fractionMap[mixedFractionMatch[2]]
    const unit = mixedFractionMatch[3].trim()
    return { value: intVal + fracVal, unit, isFraction: true }
  }
  
  const match = trimmed.match(/^(\d+(?:\.\d+)?|\.\d+)\s*(.*)$/)
  if (match) {
    return { value: parseFloat(match[1]), unit: match[2].trim(), isFraction: false }
  }
  
  return { value: null, unit: amountStr, isFraction: false }
}

const formatNumberSmart = (num) => {
  if (num === null || num === undefined) return ''
  
  if (Math.abs(num - Math.round(num)) < 0.0001) {
    return Math.round(num).toString()
  }
  
  const fractionMap = [
    { val: 0, str: '' },
    { val: 0.125, str: '⅛' },
    { val: 0.167, str: '⅙' },
    { val: 0.2, str: '⅕' },
    { val: 0.25, str: '¼' },
    { val: 0.333, str: '⅓' },
    { val: 0.375, str: '⅜' },
    { val: 0.4, str: '⅖' },
    { val: 0.5, str: '½' },
    { val: 0.6, str: '⅗' },
    { val: 0.625, str: '⅝' },
    { val: 0.667, str: '⅔' },
    { val: 0.75, str: '¾' },
    { val: 0.8, str: '⅘' },
    { val: 0.833, str: '⅚' },
    { val: 0.875, str: '⅞' }
  ]
  
  const intPart = Math.floor(num)
  const fracPart = num - intPart
  
  let bestMatch = fractionMap[0]
  let minDiff = Math.abs(fracPart)
  
  for (const frac of fractionMap) {
    const diff = Math.abs(fracPart - frac.val)
    if (diff < minDiff) {
      minDiff = diff
      bestMatch = frac
    }
  }
  
  if (minDiff < 0.08) {
    if (intPart === 0) {
      return bestMatch.str || '0'
    }
    return bestMatch.str ? `${intPart}${bestMatch.str}` : intPart.toString()
  }
  
  if (num < 10) {
    return num.toFixed(1).replace(/\.0$/, '')
  }
  
  return Math.round(num).toString()
}

const scaledIngredients = computed(() => {
  if (!recipe.value || !recipe.value.ingredients) return []
  
  const ratio = currentServings.value / originalServings.value
  
  if (Math.abs(ratio - 1) < 0.0001) {
    return recipe.value.ingredients
  }
  
  return recipe.value.ingredients.map(item => {
    const parsed = parseAmount(item.amount)
    
    if (parsed.value === null) {
      return { ...item }
    }
    
    const newValue = parsed.value * ratio
    const formattedValue = formatNumberSmart(newValue)
    const newAmount = parsed.unit ? `${formattedValue}${parsed.unit}` : formattedValue
    
    return {
      ...item,
      amount: newAmount
    }
  })
})

const increaseServings = () => {
  if (currentServings.value < maxServings) {
    currentServings.value++
    onServingsChanged()
  }
}

const decreaseServings = () => {
  if (currentServings.value > minServings) {
    currentServings.value--
    onServingsChanged()
  }
}

const clampServings = () => {
  let val = currentServings.value
  if (val < minServings) {
    val = minServings
  } else if (val > maxServings) {
    val = maxServings
  }
  val = Math.round(val)
  if (val !== currentServings.value) {
    currentServings.value = val
    onServingsChanged()
  }
}

const resetServings = () => {
  store.resetServings(route.params.id)
  onServingsChanged()
  ElMessage.success('已恢复原始份数')
}

const onServingsChanged = () => {
  if (recipe.value?.id) {
    store.resetAllTimersForRecipe(recipe.value.id)
  }
}

watch(currentServings, clampServings)

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const getDefaultStepTime = (index, step) => {
  const content = (step.content || '').toLowerCase()
  let baseTime = 5
  if (/炖煮|慢炖|小火炖|煮.*分钟|卤|焖/.test(content)) baseTime = 45
  else if (/焯水|汆/.test(content)) baseTime = 5
  else if (/炒.*糖色|炒糖/.test(content)) baseTime = 3
  else if (/翻炒|炒/.test(content)) baseTime = 5
  else if (/收汁/.test(content)) baseTime = 10
  else if (/腌|浸泡/.test(content)) baseTime = 15
  
  const ratio = currentServings.value / originalServings.value
  if (ratio > 1 && /炖煮|慢炖|小火炖|煮.*分钟|卤|焖|腌|浸泡/.test(content)) {
    const adjustedTime = Math.round(baseTime * Math.min(ratio, 2))
    return Math.max(adjustedTime, baseTime)
  }
  
  return baseTime
}

onMounted(() => {
  loadRecipe()
})

const loadRecipe = async () => {
  try {
    const result = await recipeApi.getRecipeDetail(route.params.id)
    recipe.value = parseRecipeData(result?.data)
    if (recipe.value) {
      loadSimilarRecipes()
      loadNutritionData()
      loadRatingStats()
    }
  } catch (err) {
    console.error('加载食谱详情失败', err)
    ElMessage.error('加载食谱失败')
  }
}

const loadRatingStats = async () => {
  if (!recipe.value?.id) return
  loadingRating.value = true
  try {
    const result = await ratingApi.getRatingStats(recipe.value.id)
    ratingStats.value = result?.data || null
    if (ratingStats.value?.userRating) {
      userRating.value = ratingStats.value.userRating
    }
    if (recipe.value && ratingStats.value) {
      recipe.value.averageRating = ratingStats.value.averageScore
      recipe.value.ratingCount = ratingStats.value.ratingCount
    }
  } catch (err) {
    console.error('加载评分数据失败', err)
  } finally {
    loadingRating.value = false
  }
}

const handleRate = async (score) => {
  if (!recipe.value?.id || submittingRating.value) return
  submittingRating.value = true
  try {
    const result = await ratingApi.rateRecipe(recipe.value.id, score)
    ratingStats.value = result?.data || null
    if (ratingStats.value) {
      userRating.value = score
      recipe.value.averageRating = ratingStats.value.averageScore
      recipe.value.ratingCount = ratingStats.value.ratingCount
    }
    ElMessage.success(`评分成功，您给了${score}星`)
  } catch (err) {
    console.error('评分失败', err)
    ElMessage.error('评分失败，请稍后重试')
  } finally {
    submittingRating.value = false
  }
}

const loadNutritionData = async () => {
  if (!recipe.value?.id) return
  loadingNutrition.value = true
  try {
    const result = await ingredientNutritionApi.calculateRecipeNutritionById(recipe.value.id)
    nutritionData.value = result?.data || null
  } catch (err) {
    console.error('加载营养数据失败', err)
    nutritionData.value = null
  } finally {
    loadingNutrition.value = false
  }
}

const loadSimilarRecipes = async () => {
  if (!recipe.value?.id) return
  loadingSimilar.value = true
  try {
    const list = await recipeApi.getSimilarRecipes(recipe.value.id, 6)
    similarRecipes.value = list.map(normalizeRecipeForCard)
  } catch (err) {
    console.error('加载相似食谱失败', err)
    similarRecipes.value = []
  } finally {
    loadingSimilar.value = false
  }
}

const scrollSimilar = (direction) => {
  const container = document.querySelector('.similar-scroll-container')
  if (container) {
    const scrollAmount = container.clientWidth * 0.8
    container.scrollBy({
      left: direction === 'left' ? -scrollAmount : scrollAmount,
      behavior: 'smooth'
    })
  }
}

const toggleFavorite = async () => {
  if (isFavorited.value) {
    await store.removeFavorite(route.params.id)
    if (recipe.value) {
      recipe.value.favoriteCount = Math.max(0, (recipe.value.favoriteCount || 0) - 1)
    }
    ElMessage.success('已取消收藏')
  } else {
    await store.addFavorite(recipe.value)
    if (recipe.value) {
      recipe.value.favoriteCount = (recipe.value.favoriteCount || 0) + 1
    }
    ElMessage.success('收藏成功')
  }
}

const shareRecipe = () => {
  ElMessage.info('分享功能开发中')
}

const printRecipe = () => {
  window.print()
}

const addToShoppingList = async () => {
  if (!recipe.value || !scaledIngredients.value || scaledIngredients.value.length === 0) {
    ElMessage.warning('该菜谱暂无食材信息')
    return
  }
  try {
    await shoppingListApi.addFromRecipe(recipe.value.id, scaledIngredients.value)
    ElMessage.success(`已将《${recipe.value.title}》的${scaledIngredients.value.length}种食材加入购物清单`)
  } catch (e) {
    console.error('加入购物清单失败', e)
    ElMessage.error('加入购物清单失败')
  }
}

const handleImageError = (e) => {
  if (e.target.src !== defaultImage) {
    e.target.src = defaultImage
  }
}

const handleStepImageError = (e) => {
  e.target.style.display = 'none'
}
</script>

<style lang="scss" scoped>
.recipe-detail {
  padding: 40px 0;
}

.detail-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
  align-items: center;
}

.recipe-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
  line-height: 1.3;
}

.recipe-desc {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  line-height: 1.7;
}

.recipe-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  
  .meta-icon {
    font-size: 18px;
  }
}

.recipe-tags {
  margin-bottom: 32px;
}

.action-buttons {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.btn-rating-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--primary-color);
  }
}

.rating-label {
  font-size: 14px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.rating-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 24px;
}

.spinner-sm {
  width: 18px;
  height: 18px;
  border-width: 2px;
}

.rating-item {
  color: #f59e0b !important;
  font-weight: 500;
}

.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  
  svg {
    width: 18px;
    height: 18px;
  }
}

.favorited {
  background: linear-gradient(135deg, #ef4444, #f87171) !important;
}

.btn-shopping,
.btn-add-shopping {
  &:hover {
    border-color: #8b5cf6 !important;
    background: #8b5cf6 !important;
    color: white !important;
  }
}

.header-image img {
  width: 100%;
  height: 350px;
  object-fit: cover;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.section-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 24px;
}

.ingredients-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;

  .section-title-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .section-title {
    margin-bottom: 0;
  }

  .print-only {
    display: none;
  }
}

.ingredients-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-add-shopping {
  padding: 8px 16px;
  font-size: 13px;
}

.servings-control {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  min-width: 320px;
}

.servings-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;

  .servings-icon {
    font-size: 18px;
  }
}

.servings-controls-wrapper {
  display: flex;
  align-items: center;
  gap: 0;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.servings-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 500;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: var(--primary-color);
    color: white;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &.minus {
    border-right: 1px solid var(--border-color);
  }

  &.plus {
    border-left: 1px solid var(--border-color);
  }
}

.servings-input {
  width: 60px;
  height: 36px;
  text-align: center;
  border: none;
  outline: none;
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
  background: transparent;
  -moz-appearance: textfield;

  &::-webkit-outer-spin-button,
  &::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }
}

.servings-slider-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}

.servings-slider {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--border-color);
  border-radius: 3px;
  outline: none;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    cursor: pointer;
    box-shadow: 0 2px 6px rgba(230, 126, 34, 0.4);
    transition: transform 0.2s ease;

    &:hover {
      transform: scale(1.15);
    }
  }

  &::-moz-range-thumb {
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    cursor: pointer;
    border: none;
    box-shadow: 0 2px 6px rgba(230, 126, 34, 0.4);
  }
}

.slider-marks {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-light);
}

.reset-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: white;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;

  svg {
    width: 16px;
    height: 16px;
  }

  &:hover:not(:disabled) {
    background: var(--primary-color);
    color: white;
    border-color: var(--primary-color);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.amount-cell {
  .amount-value {
    transition: color 0.3s ease;

    &.amount-changed {
      color: var(--primary-color);
      font-weight: 600;
    }
  }
}

.ingredients-table {
  width: 100%;
  border-collapse: collapse;
  
  th, td {
    padding: 12px 16px;
    text-align: left;
    border-bottom: 1px solid var(--border-color);
  }
  
  th {
    background: var(--bg-tertiary);
    font-weight: 500;
    font-size: 14px;
    color: var(--text-secondary);
  }
  
  td {
    font-size: 15px;
  }
  
  tr:last-child td {
    border-bottom: none;
  }
}

.nutrition-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;

  .section-title {
    margin-bottom: 0;
  }

  .print-only {
    display: none;
  }
}

.nutrition-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 30px;
  color: var(--text-secondary);
  font-size: 14px;

  .spinner {
    width: 24px;
    height: 24px;
    border-width: 2px;
  }
}

.nutrition-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 30px;
  color: var(--text-light);
  font-size: 14px;

  .empty-icon {
    font-size: 20px;
  }
}

.nutrition-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.nutrition-item {
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  padding: 20px;
  text-align: center;
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-2px);
  }

  &.calories .nutrition-value {
    color: #e74c3c;
  }

  &.protein .nutrition-value {
    color: #3498db;
  }

  &.fat .nutrition-value {
    color: #f39c12;
  }

  &.carb .nutrition-value {
    color: #27ae60;
  }

  &.fiber .nutrition-value {
    color: #9b59b6;
  }

  &.sodium .nutrition-value {
    color: #1abc9c;
  }
}

.nutrition-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 6px;
}

.nutrition-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.nutrition-warning {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  background: #fff7ed;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: #c2410c;

  .warning-icon {
    flex-shrink: 0;
    font-size: 16px;
  }
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.step-item {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.step-time-print {
  display: none;
}

.step-number {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
}

.step-content {
  flex: 1;
  padding-left: 54px;
}

.step-image {
  margin-bottom: 16px;
  
  img {
    width: 100%;
    max-width: 400px;
    border-radius: var(--radius-md);
  }
}

.step-text {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
}

.tips-content {
  background: var(--bg-tertiary);
  padding: 20px;
  border-radius: var(--radius-sm);
  line-height: 2;
  color: var(--text-secondary);
  white-space: pre-line;
}

.loading-full {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

@media (max-width: 768px) {
  .recipe-detail {
    padding: 20px 0;
  }

  .container {
    padding: 0 16px;
  }

  .section-card {
    padding: 20px 16px;
    margin-bottom: 16px;
  }

  .recipe-title {
    font-size: 26px;
  }

  .section-title {
    font-size: 20px;
  }

  .detail-header {
    grid-template-columns: 1fr;
    gap: 20px;
    margin-bottom: 24px;
  }
  
  .header-image {
    order: -1;
  }

  .header-image img {
    height: 240px;
  }

  .recipe-meta {
    gap: 16px;
  }

  .action-buttons {
    gap: 12px;

    .btn {
      flex: 1;
      justify-content: center;
      padding: 14px 16px;
      min-height: 48px;
    }
  }

  .btn-rating-wrapper {
    width: 100%;
    justify-content: center;
    order: -1;
  }

  .rating-label {
    font-size: 13px;
  }

  .ingredients-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .servings-control {
    min-width: auto;
    padding: 14px;
    gap: 12px;
  }

  .servings-slider-wrapper {
    min-width: 100%;
    order: 5;
  }

  .reset-btn {
    width: 100%;
    justify-content: center;
    padding: 10px 14px;
  }

  .steps-list {
    gap: 20px;
  }

  .step-item {
    gap: 12px;
  }

  .step-header {
    gap: 12px;
  }

  .step-number {
    width: 44px;
    height: 44px;
    font-size: 20px;
  }

  .step-content {
    padding-left: 0;
  }

  .step-text {
    font-size: 15px;
    line-height: 1.75;
  }

  .ingredients-table {
    th, td {
      padding: 10px 12px;
      font-size: 14px;
    }
  }
}

@media (max-width: 480px) {
  .detail-header {
    gap: 16px;
  }

  .recipe-title {
    font-size: 22px;
  }

  .recipe-desc {
    font-size: 14px;
    line-height: 1.6;
    margin-bottom: 16px;
  }

  .recipe-meta {
    gap: 12px;
    margin-bottom: 16px;
  }

  .meta-item {
    font-size: 13px;
    gap: 6px;
  }

  .section-card {
    padding: 16px 14px;
  }

  .section-title {
    font-size: 18px;
    margin-bottom: 16px;
  }

  .servings-control {
    padding: 12px;
    gap: 10px;
  }

  .servings-btn {
    width: 32px;
    height: 32px;
    font-size: 18px;
  }

  .servings-input {
    width: 50px;
    height: 32px;
    font-size: 15px;
  }

  .action-buttons {
    .btn {
      min-height: 50px;
      font-size: 15px;
    }
  }

  .ingredients-table {
    th {
      font-size: 13px;
      padding: 8px 10px;
    }

    td {
      padding: 10px;
      font-size: 14px;
    }
  }
}

.similar-section {
  margin-top: 8px;
}

.similar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .section-title {
    margin-bottom: 0;
  }
}

.similar-nav {
  display: flex;
  gap: 8px;
}

.nav-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: white;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;

  svg {
    width: 18px;
    height: 18px;
  }

  &:hover {
    background: var(--primary-color);
    color: white;
    border-color: var(--primary-color);
  }
}

.similar-scroll-wrapper {
  position: relative;
}

.similar-scroll-container {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 8px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: var(--border-color) transparent;

  &::-webkit-scrollbar {
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: var(--text-light);
  }
}

.similar-card-wrapper {
  flex-shrink: 0;
  width: 260px;
}

.similar-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 14px;

  .spinner {
    width: 32px;
    height: 32px;
    border-width: 3px;
  }
}

@media (max-width: 768px) {
  .similar-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 16px;
  }

  .similar-nav {
    align-self: flex-end;
  }

  .similar-card-wrapper {
    width: 220px;
  }

  .similar-scroll-container {
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .similar-card-wrapper {
    width: 200px;
  }
}

@media print {
  @page {
    size: A4;
    margin: 15mm 15mm 15mm 15mm;
  }

  .recipe-detail {
    padding: 0 !important;
    background: white !important;
  }

  .container {
    max-width: 100% !important;
    padding: 0 !important;
    margin: 0 !important;
  }

  .no-print {
    display: none !important;
  }

  .print-only {
    display: inline !important;
  }

  .servings-print-info {
    font-size: 16px !important;
    color: #e67e22 !important;
    font-weight: 600 !important;
  }

  .step-time-print {
    display: inline-block !important;
    font-size: 13px !important;
    color: #666 !important;
    margin-left: 8px !important;
  }

  .detail-header {
    grid-template-columns: 1fr !important;
    gap: 16px !important;
    margin-bottom: 20px !important;
    page-break-inside: avoid;
  }

  .header-image {
    order: -1 !important;
  }

  .header-image img {
    width: 100% !important;
    max-height: 200px !important;
    height: auto !important;
    object-fit: cover !important;
    border-radius: 0 !important;
    box-shadow: none !important;
    page-break-inside: avoid;
  }

  .recipe-title {
    font-size: 24px !important;
    margin-bottom: 8px !important;
  }

  .recipe-desc {
    font-size: 13px !important;
    margin-bottom: 12px !important;
    line-height: 1.5 !important;
  }

  .recipe-meta {
    gap: 12px !important;
    margin-bottom: 12px !important;
  }

  .meta-item {
    font-size: 12px !important;
    gap: 4px !important;
  }

  .meta-icon {
    font-size: 14px !important;
  }

  .recipe-tags {
    margin-bottom: 12px !important;
  }

  .tag {
    font-size: 11px !important;
    padding: 2px 8px !important;
    margin-right: 4px !important;
    margin-bottom: 4px !important;
    border: 1px solid #ccc !important;
    background: transparent !important;
    color: #333 !important;
  }

  .section-card {
    background: white !important;
    border-radius: 0 !important;
    padding: 12px 0 !important;
    margin-bottom: 12px !important;
    box-shadow: none !important;
    border-bottom: 1px solid #e0e0e0;
    page-break-inside: avoid;
  }

  .section-title {
    font-size: 16px !important;
    margin-bottom: 12px !important;
    padding-bottom: 6px;
    border-bottom: 2px solid #e67e22;
  }

  .section-title::after {
    display: none !important;
  }

  .ingredients-header {
    margin-bottom: 12px !important;
  }

  .ingredients-table {
    width: 100% !important;
    font-size: 12px !important;
  }

  .ingredients-table th,
  .ingredients-table td {
    padding: 6px 8px !important;
    font-size: 12px !important;
    border-bottom: 1px solid #e0e0e0 !important;
  }

  .ingredients-table th {
    background: #f5f5f5 !important;
    font-weight: 600 !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .amount-value {
    color: #333 !important;
    font-weight: normal !important;
  }

  .steps-list {
    gap: 12px !important;
  }

  .step-item {
    gap: 8px !important;
    page-break-inside: avoid;
  }

  .step-header {
    gap: 8px !important;
  }

  .step-number {
    width: 28px !important;
    height: 28px !important;
    font-size: 14px !important;
    background: #e67e22 !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .step-content {
    padding-left: 36px !important;
  }

  .step-image img {
    max-width: 300px !important;
    max-height: 200px !important;
    height: auto !important;
    border-radius: 0 !important;
    page-break-inside: avoid;
  }

  .step-text {
    font-size: 13px !important;
    line-height: 1.6 !important;
  }

  .tips-content {
    background: #f9f9f9 !important;
    padding: 12px !important;
    font-size: 12px !important;
    line-height: 1.7 !important;
    border: 1px solid #e0e0e0 !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
    white-space: pre-line !important;
  }

  .detail-content {
    page-break-before: auto;
  }

  .detail-content > * {
    page-break-inside: avoid;
  }
}
</style>