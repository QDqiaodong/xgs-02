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
          </div>
          
          <div class="recipe-tags">
            <span v-for="tag in recipe.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          
          <div class="action-buttons">
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
          </div>
        </div>
        
        <div class="header-image">
          <img :src="recipe.coverImage || defaultImage" :alt="recipe.title" />
        </div>
      </div>

      <div class="detail-content">
        <div class="ingredients-section section-card">
          <div class="ingredients-header">
            <h2 class="section-title">🥬 食材清单</h2>
            <div class="servings-control">
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

        <div class="steps-section section-card">
          <h2 class="section-title">👨‍🍳 烹饪步骤</h2>
          <div class="steps-list">
            <div v-for="(step, index) in recipe.steps" :key="index" class="step-item">
              <div class="step-header">
                <div class="step-number">{{ index + 1 }}</div>
                <StepTimer 
                  :recipe-id="recipe.id" 
                  :step-index="index"
                  :default-minutes="getDefaultStepTime(index, step)"
                />
              </div>
              <div class="step-content">
                <div v-if="step.image" class="step-image">
                  <img :src="step.image" :alt="'步骤' + (index + 1)" />
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

const route = useRoute()
const store = useRecipeStore()

const recipe = ref(null)
const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&h=500&fit=crop'

const originalServings = ref(1)
const currentServings = ref(1)
const minServings = 1
const maxServings = 20

const isFavorited = computed(() => store.isFavorite(route.params.id))

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
  }
}

const decreaseServings = () => {
  if (currentServings.value > minServings) {
    currentServings.value--
  }
}

const clampServings = () => {
  if (currentServings.value < minServings) {
    currentServings.value = minServings
  } else if (currentServings.value > maxServings) {
    currentServings.value = maxServings
  }
  currentServings.value = Math.round(currentServings.value)
}

const resetServings = () => {
  currentServings.value = originalServings.value
  ElMessage.success('已恢复原始份数')
}

watch(currentServings, clampServings)

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const getDefaultStepTime = (index, step) => {
  const content = (step.content || '').toLowerCase()
  if (/炖煮|慢炖|小火炖|煮.*分钟|卤|焖/.test(content)) return 45
  if (/焯水|汆/.test(content)) return 5
  if (/炒.*糖色|炒糖/.test(content)) return 3
  if (/翻炒|炒/.test(content)) return 5
  if (/收汁/.test(content)) return 10
  if (/腌|浸泡/.test(content)) return 15
  return 5
}

onMounted(() => {
  loadRecipe()
})

const loadRecipe = () => {
  setTimeout(() => {
    recipe.value = {
      id: route.params.id,
      title: '红烧五花肉',
      description: '经典家常菜，选用优质五花肉，经过慢火炖煮，肥而不腻，入口即化，是一道让人回味无穷的传统美味。',
      coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=800&h=500&fit=crop',
      author: '美食达人',
      cookTime: 60,
      difficulty: 2,
      favoriteCount: 1286,
      tags: ['川菜', '家常菜', '下饭神器'],
      ingredients: [
        { name: '五花肉', amount: '500g', note: '选三层肉最佳' },
        { name: '生抽', amount: '2勺', note: '' },
        { name: '老抽', amount: '1勺', note: '上色用' },
        { name: '冰糖', amount: '30g', note: '' },
        { name: '料酒', amount: '2勺', note: '' },
        { name: '姜片', amount: '5片', note: '' },
        { name: '八角', amount: '2个', note: '' },
        { name: '桂皮', amount: '1小块', note: '' },
        { name: '盐', amount: '适量', note: '' }
      ],
      steps: [
        { content: '五花肉切成2-3厘米见方的块，冷水下锅，加入姜片和料酒，大火烧开后焯水3分钟，捞出洗净沥干。', image: '' },
        { content: '锅中放少许油，加入冰糖小火炒出糖色，注意火候不要太大，避免炒糊。', image: '' },
        { content: '放入焯水后的五花肉，翻炒均匀，让每一块肉都裹上糖色。', image: '' },
        { content: '加入生抽、老抽、料酒，放入八角、桂皮、剩余的姜片，翻炒出香味。', image: '' },
        { content: '加入没过肉的热水，大火烧开后转小火，盖上锅盖炖煮45分钟。', image: '' },
        { content: '最后大火收汁，根据口味加适量盐调味，汤汁浓稠即可出锅。', image: '' }
      ],
      tips: '1. 焯水要冷水下锅，能更好地去除血水和腥味\n2. 炒糖色要用小火，颜色变成枣红色即可\n3. 加水一定要加热水，肉质才不会柴\n4. 最后收汁不要收太干，留一点汤汁拌饭超好吃'
    }
  }, 500)
}

const toggleFavorite = () => {
  if (isFavorited.value) {
    store.removeFavorite(route.params.id)
    ElMessage.success('已取消收藏')
  } else {
    store.addFavorite(recipe.value)
    ElMessage.success('收藏成功')
  }
}

const shareRecipe = () => {
  ElMessage.info('分享功能开发中')
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

  .section-title {
    margin-bottom: 0;
  }
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
</style>