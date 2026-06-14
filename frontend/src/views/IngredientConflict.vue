<template>
  <div class="ingredient-conflict-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">⚠️ 食材冲突校验</h1>
        <p class="page-desc">根据您的饮食偏好和过敏原，智能检测菜谱食材是否安全</p>
      </div>

      <div class="content-grid">
        <div class="preference-section section-card">
          <h2 class="section-title">
            <span>👤 我的饮食偏好</span>
          </h2>

          <div class="preference-group">
            <h3 class="group-title">
              <span class="title-icon high">🚨</span>
              过敏原
              <span class="group-desc">严重过敏，绝对不能吃</span>
            </h3>
            <div class="tag-input-wrapper">
              <div class="tags-container">
                <span
                  v-for="(allergy, index) in userPreference.allergies"
                  :key="index"
                  class="tag-item high"
                >
                  {{ allergy }}
                  <button class="tag-remove" @click="removeAllergy(index)">×</button>
                </span>
                <input
                  v-model="newAllergy"
                  type="text"
                  class="tag-input"
                  placeholder="添加过敏原..."
                  @keyup.enter="addAllergy"
                />
              </div>
              <div class="quick-tags">
                <span class="quick-label">常见：</span>
                <button
                  v-for="item in commonAllergies"
                  :key="item"
                  class="quick-tag"
                  :class="{ active: userPreference.allergies.includes(item) }"
                  @click="toggleAllergy(item)"
                >
                  {{ item }}
                </button>
              </div>
            </div>
          </div>

          <div class="preference-group">
            <h3 class="group-title">
              <span class="title-icon medium">🥗</span>
              饮食限制
              <span class="group-desc">因健康或宗教原因避免</span>
            </h3>
            <div class="tag-input-wrapper">
              <div class="tags-container">
                <span
                  v-for="(item, index) in userPreference.dietaryRestrictions"
                  :key="index"
                  class="tag-item medium"
                >
                  {{ item }}
                  <button class="tag-remove" @click="removeDietary(index)">×</button>
                </span>
                <input
                  v-model="newDietary"
                  type="text"
                  class="tag-input"
                  placeholder="添加饮食限制..."
                  @keyup.enter="addDietary"
                />
              </div>
              <div class="quick-tags">
                <span class="quick-label">常见：</span>
                <button
                  v-for="item in commonDietary"
                  :key="item"
                  class="quick-tag"
                  :class="{ active: userPreference.dietaryRestrictions.includes(item) }"
                  @click="toggleDietary(item)"
                >
                  {{ item }}
                </button>
              </div>
            </div>
          </div>

          <div class="preference-group">
            <h3 class="group-title">
              <span class="title-icon low">👎</span>
              不喜欢的食材
              <span class="group-desc">个人口味偏好</span>
            </h3>
            <div class="tag-input-wrapper">
              <div class="tags-container">
                <span
                  v-for="(item, index) in userPreference.dislikes"
                  :key="index"
                  class="tag-item low"
                >
                  {{ item }}
                  <button class="tag-remove" @click="removeDislike(index)">×</button>
                </span>
                <input
                  v-model="newDislike"
                  type="text"
                  class="tag-input"
                  placeholder="添加不喜欢的食材..."
                  @keyup.enter="addDislike"
                />
              </div>
              <div class="quick-tags">
                <span class="quick-label">常见：</span>
                <button
                  v-for="item in commonDislikes"
                  :key="item"
                  class="quick-tag"
                  :class="{ active: userPreference.dislikes.includes(item) }"
                  @click="toggleDislike(item)"
                >
                  {{ item }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="check-section">
          <div class="section-card check-method">
            <h2 class="section-title">🔍 选择校验方式</h2>
            <div class="method-tabs">
              <button
                class="method-tab"
                :class="{ active: checkMethod === 'recipe' }"
                @click="checkMethod = 'recipe'"
              >
                <span class="tab-icon">📖</span>
                <span>选择菜谱</span>
              </button>
              <button
                class="method-tab"
                :class="{ active: checkMethod === 'ingredients' }"
                @click="checkMethod = 'ingredients'"
              >
                <span class="tab-icon">🥬</span>
                <span>输入食材</span>
              </button>
            </div>

            <div v-if="checkMethod === 'recipe'" class="method-content">
              <div class="recipe-selector" @click="showRecipePicker = true">
                <div v-if="selectedRecipe" class="selected-recipe">
                  <img :src="selectedRecipe.coverImage || defaultImage" :alt="selectedRecipe.title" class="recipe-thumb" />
                  <div class="recipe-info">
                    <div class="recipe-name">{{ selectedRecipe.title }}</div>
                    <div class="recipe-desc">{{ selectedRecipe.description }}</div>
                  </div>
                  <span class="change-btn">更换</span>
                </div>
                <div v-else class="select-placeholder">
                  <span class="placeholder-icon">🍲</span>
                  <span>点击选择要校验的菜谱</span>
                </div>
              </div>
            </div>

            <div v-else class="method-content">
              <div class="ingredients-input">
                <label class="input-label">食材列表（每行一个，或用逗号分隔）</label>
                <textarea
                  v-model="ingredientsText"
                  class="ingredients-textarea"
                  placeholder="例如：&#10;五花肉&#10;鸡蛋&#10;辣椒, 花椒, 八角"
                  rows="6"
                ></textarea>
                <div class="ingredient-count">
                  已输入 {{ parsedIngredients.length }} 种食材
                </div>
              </div>
            </div>

            <button
              class="btn btn-primary check-btn"
              :disabled="!canCheck"
              @click="checkConflicts"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
              </svg>
              开始校验
            </button>
          </div>

          <div v-if="loading" class="section-card result-card">
            <div class="loading-wrapper">
              <div class="spinner"></div>
              <span>正在检测食材冲突...</span>
            </div>
          </div>

          <div v-else-if="checkResult" class="section-card result-card">
            <div class="result-header" :class="checkResult.riskLevel">
              <div class="result-icon">
                <span v-if="checkResult.riskLevel === 'high'">🚨</span>
                <span v-else-if="checkResult.riskLevel === 'medium'">⚠️</span>
                <span v-else-if="checkResult.riskLevel === 'low'">💡</span>
                <span v-else>✅</span>
              </div>
              <div class="result-info">
                <h3 class="result-title">
                <span v-if="checkResult.riskLevel === 'high'">高风险</span>
                <span v-else-if="checkResult.riskLevel === 'medium'">中等风险</span>
                <span v-else-if="checkResult.riskLevel === 'low'">低风险</span>
                <span v-else>无冲突</span>
              </h3>
                <p class="result-desc">
                  <span v-if="checkResult.hasConflict">
                    检测到 {{ checkResult.conflicts.length }} 项冲突，建议仔细查看
                  </span>
                  <span v-else>
                    所有食材都符合您的饮食偏好，可以放心食用！
                  </span>
                </p>
              </div>
              <div class="result-stats">
                <div class="stat-item high">
                  <span class="stat-number">{{ countByType('allergy') }}</span>
                  <span class="stat-label">过敏</span>
                </div>
                <div class="stat-item medium">
                  <span class="stat-number">{{ countByType('dietary') }}</span>
                  <span class="stat-label">忌口</span>
                </div>
                <div class="stat-item low">
                  <span class="stat-number">{{ countByType('dislike') }}</span>
                  <span class="stat-label">不喜欢</span>
                </div>
              </div>
            </div>

            <div v-if="checkResult.conflicts && checkResult.conflicts.length > 0" class="conflicts-list">
              <h4 class="list-title">冲突详情</h4>
              <div
                v-for="(conflict, index) in checkResult.conflicts"
                :key="index"
                class="conflict-item"
                :class="conflict.riskLevel"
              >
                <div class="conflict-header">
                  <span class="conflict-icon">
                    <span v-if="conflict.conflictType === 'allergy'">🚨</span>
                    <span v-else-if="conflict.conflictType === 'dietary'">⚠️</span>
                    <span v-else>👎</span>
                  </span>
                  <div class="conflict-main">
                    <span class="conflict-name">{{ conflict.ingredientName }}</span>
                    <span class="conflict-type-tag">
                      <span v-if="conflict.conflictType === 'allergy'">过敏原</span>
                      <span v-else-if="conflict.conflictType === 'dietary'">饮食限制</span>
                      <span v-else>不喜欢</span>
                    </span>
                    <span class="conflict-severity">{{ conflict.severity }}</span>
                  </div>
                </div>
                <div class="conflict-reason">
                  <span class="reason-label">原因：</span>
                  {{ conflict.reason }}
                </div>
                <div class="conflict-replacement">
                  <span class="replacement-label">替换建议：</span>
                  {{ conflict.replacementSuggestion }}
                </div>
              </div>
            </div>

            <div v-if="checkResult.suggestions && checkResult.suggestions.length > 0" class="suggestions-section">
              <h4 class="list-title">💡 建议</h4>
              <ul class="suggestions-list">
                <li v-for="(suggestion, index) in checkResult.suggestions" :key="index">
                  {{ suggestion }}
                </li>
              </ul>
            </div>
          </div>

          <div v-else class="section-card empty-result">
            <div class="empty-icon">🔍</div>
            <h3>还没有检测</h3>
            <p>选择菜谱或输入食材，开始检测食材冲突</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showRecipePicker" class="recipe-picker-overlay" @click.self="showRecipePicker = false">
      <div class="recipe-picker-modal">
        <div class="picker-header">
          <h3>选择菜谱</h3>
          <button class="close-btn" @click="showRecipePicker = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="picker-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索菜谱..."
            prefix-icon="Search"
            clearable
            @input="searchRecipes"
          />
        </div>
        <div class="picker-list">
          <div v-if="loadingRecipes" class="picker-loading">
            <div class="spinner spinner-sm"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="recipeList.length === 0" class="picker-empty">
            <span>暂无菜谱</span>
          </div>
          <div
            v-for="recipe in recipeList"
            :key="recipe.id"
            class="picker-item"
            :class="{ selected: selectedRecipe?.id === recipe.id }"
            @click="selectRecipe(recipe)"
          >
            <img :src="recipe.coverImage || defaultImage" :alt="recipe.title" class="picker-item-img" />
            <div class="picker-item-info">
              <div class="picker-item-title">{{ recipe.title }}</div>
              <div class="picker-item-meta">
                <span>⏱️ {{ recipe.cookTime }}分钟</span>
                <span>📊 {{ getDifficultyLabel(recipe.difficulty) }}</span>
              </div>
            </div>
            <div v-if="selectedRecipe?.id === recipe.id" class="picker-check">✓</div>
          </div>
        </div>
        <div class="picker-footer">
          <button class="btn btn-outline" @click="showRecipePicker = false">取消</button>
          <button class="btn btn-primary" :disabled="!selectedRecipe" @click="confirmSelect">确定选择</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { recipeApi, ingredientConflictApi } from '@/utils/api'

const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&h=500&fit=crop'

const userPreference = ref({
  allergies: [],
  dietaryRestrictions: [],
  dislikes: []
})

const newAllergy = ref('')
const newDietary = ref('')
const newDislike = ref('')

const commonAllergies = ['海鲜', '花生', '坚果', '牛奶', '鸡蛋', '小麦', '大豆', '芒果']
const commonDietary = ['素食', '清真', '无麸质', '无乳制品', '低盐', '低糖', '低脂', '低钠']
const commonDislikes = ['香菜', '芹菜', '胡萝卜', '洋葱', '大蒜', '生姜', '苦瓜', '辣椒']

const checkMethod = ref('recipe')
const selectedRecipe = ref(null)
const ingredientsText = ref('')
const showRecipePicker = ref(false)
const searchKeyword = ref('')
const recipeList = ref([])
const loadingRecipes = ref(false)
const loading = ref(false)
const checkResult = ref(null)

const canCheck = computed(() => {
  if (checkMethod.value === 'recipe') {
    return selectedRecipe.value
  }
  return parsedIngredients.value.length > 0
})

const parsedIngredients = computed(() => {
  if (!ingredientsText.value.trim()) {
    return []
  }
  return ingredientsText.value
    .split(/[,\n，、]+/)
    .map(s => s.trim())
    .filter(s => s.length > 0)
})

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const countByType = (type) => {
  if (!checkResult.value?.conflicts) return 0
  return checkResult.value.conflicts.filter(c => c.conflictType === type).length
}

const toggleAllergy = (item) => {
  const index = userPreference.value.allergies.indexOf(item)
  if (index > -1) {
    userPreference.value.allergies.splice(index, 1)
  } else {
    userPreference.value.allergies.push(item)
  }
  savePreference()
}

const addAllergy = () => {
  const val = newAllergy.value.trim()
  if (val && !userPreference.value.allergies.indexOf(val) === -1) {
    userPreference.value.allergies.push(val)
    newAllergy.value = ''
    savePreference()
  }
}

const removeAllergy = (index) => {
  userPreference.value.allergies.splice(index, 1)
  savePreference()
}

const toggleDietary = (item) => {
  const index = userPreference.value.dietaryRestrictions.indexOf(item)
  if (index > -1) {
    userPreference.value.dietaryRestrictions.splice(index, 1)
  } else {
    userPreference.value.dietaryRestrictions.push(item)
  }
  savePreference()
}

const addDietary = () => {
  const val = newDietary.value.trim()
  if (val && userPreference.value.dietaryRestrictions.indexOf(val) === -1) {
    userPreference.value.dietaryRestrictions.push(val)
    newDietary.value = ''
    savePreference()
  }
}

const removeDietary = (index) => {
  userPreference.value.dietaryRestrictions.splice(index, 1)
  savePreference()
}

const toggleDislike = (item) => {
  const index = userPreference.value.dislikes.indexOf(item)
  if (index > -1) {
    userPreference.value.dislikes.splice(index, 1)
  } else {
    userPreference.value.dislikes.push(item)
  }
  savePreference()
}

const addDislike = () => {
  const val = newDislike.value.trim()
  if (val && userPreference.value.dislikes.indexOf(val) === -1) {
    userPreference.value.dislikes.push(val)
    newDislike.value = ''
    savePreference()
  }
}

const removeDislike = (index) => {
  userPreference.value.dislikes.splice(index, 1)
  savePreference()
}

const savePreference = () => {
  try {
    localStorage.setItem('userFoodPreference', JSON.stringify(userPreference.value))
  } catch (e) {
    console.warn('保存偏好失败', e)
  }
}

const loadPreference = () => {
  try {
    const saved = localStorage.getItem('userFoodPreference')
    if (saved) {
      const parsed = JSON.parse(saved)
      userPreference.value = { ...userPreference.value, ...parsed }
    }
  } catch (e) {
    console.warn('加载偏好失败', e)
  }
}

const loadRecipes = async () => {
  loadingRecipes.value = true
  try {
    const result = await recipeApi.getRecipes({ page: 1, size: 20 })
    if (result && result.data && result.data.records) {
      recipeList.value = result.data.records
    }
  } catch (err) {
    console.error('加载菜谱列表失败', err)
    ElMessage.error('加载菜谱列表失败')
  } finally {
    loadingRecipes.value = false
  }
}

const searchRecipes = async () => {
  if (!searchKeyword.value.trim()) {
    loadRecipes()
    return
  }
  loadingRecipes.value = true
  try {
    const result = await recipeApi.getRecipes({ page: 1, size: 20, keyword: searchKeyword.value })
    if (result && result.data && result.data.records) {
      recipeList.value = result.data.records
    }
  } catch (err) {
    console.error('搜索菜谱失败', err)
  } finally {
    loadingRecipes.value = false
  }
}

const selectRecipe = (recipe) => {
  selectedRecipe.value = recipe
}

const confirmSelect = () => {
  if (selectedRecipe.value) {
    showRecipePicker.value = false
  }
}

const checkConflicts = async () => {
  if (!canCheck.value) return

  loading.value = true
  checkResult.value = null

  try {
    let result

    if (checkMethod.value === 'recipe' && selectedRecipe.value) {
      result = await ingredientConflictApi.checkRecipeConflicts(
        selectedRecipe.value.id,
        userPreference.value
      )
    } else {
      result = await ingredientConflictApi.checkIngredientConflicts(
        parsedIngredients.value,
        userPreference.value
      )
    }

    if (result && result.data) {
      checkResult.value = result.data

      if (result.data.hasConflict) {
        const count = result.data.conflicts.length
        const level = result.data.riskLevel
        const levelText = { high: '高风险', medium: '中等风险', low: '低风险' }[level] || ''
        ElMessage.warning(`检测完成：${count} 项冲突，${levelText}`)
      } else {
        ElMessage.success('太棒了！没有检测通过，无食材冲突')
      }
    }
  } catch (err) {
    console.error('校验失败', err)
    ElMessage.error('校验失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPreference()
  loadRecipes()
})
</script>

<style lang="scss" scoped>
.ingredient-conflict-page {
  padding: 40px 0 60px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.page-desc {
  color: var(--text-secondary);
  font-size: 15px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

.section-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-sm);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.preference-section {
  position: sticky;
  top: 20px;
}

.preference-group {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;

  .title-icon {
    font-size: 18px;
  }

  .group-desc {
    font-size: 12px;
    font-weight: normal;
    color: var(--text-light);
    margin-left: auto;
  }
}

.tag-input-wrapper {
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  padding: 12px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  min-height: 36px;
  align-items: center;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;

  &.high {
    background: #fef2f2;
    color: #dc2626;
  }

  &.medium {
    background: #fff7ed;
    color: #ea580c;
  }

  &.low {
    background: #f0fdf4;
    color: #16a34a;
  }
}

.tag-remove {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.1);
  border: none;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: inherit;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.2);
  }
}

.tag-input {
  flex: 1;
  min-width: 120px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-primary);
  padding: 6px 4px;

  &::placeholder {
    color: var(--text-light);
  }
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  align-items: center;
}

.quick-label {
  font-size: 12px;
  color: var(--text-light);
}

.quick-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid var(--border-color);
  background: white;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--primary-color);
    color: var(--primary-color);
  }

  &.active {
    background: var(--primary-color);
    color: white;
    border-color: var(--primary-color);
  }
}

.check-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.check-method {
  .section-title {
    margin-bottom: 16px;
  }
}

.method-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.method-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all 0.2s ease;

  .tab-icon {
    font-size: 18px;
  }

  &:hover {
    border-color: var(--primary-light);
  }

  &.active {
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    color: white;
    border-color: transparent;
  }
}

.method-content {
  margin-bottom: 20px;
}

.recipe-selector {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-md);
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 80px;
  display: flex;
  align-items: center;

  &:hover {
    border-color: var(--primary-color);
    background: var(--bg-tertiary);
  }
}

.selected-recipe {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.recipe-thumb {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.recipe-info {
  flex: 1;
  min-width: 0;
}

.recipe-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.recipe-desc {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.change-btn {
  font-size: 13px;
  color: var(--primary-color);
  font-weight: 500;
  flex-shrink: 0;
}

.select-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 8px;
  color: var(--text-light);
}

.placeholder-icon {
  font-size: 32px;
}

.ingredients-input {
  .input-label {
    display: block;
    font-size: 13px;
    color: var(--text-secondary);
    margin-bottom: 8px;
  }
}

.ingredients-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-primary);
  resize: vertical;
  outline: none;
  transition: border-color 0.2s ease;

  &:focus {
    border-color: var(--primary-color);
  }

  &::placeholder {
    color: var(--text-light);
  }
}

.ingredient-count {
  text-align: right;
  font-size: 12px;
  color: var(--text-light);
  margin-top: 8px;
}

.check-btn {
  width: 100%;
  height: 48px;
  justify-content: center;
  font-size: 15px;

  svg {
    width: 20px;
    height: 20px;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
  border: none;

  svg {
    width: 18px;
    height: 18px;
  }
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(230, 126, 34, 0.4);
  }
}

.btn-outline {
  background: white;
  color: var(--text-primary);
  border: 1px solid var(--border-color);

  &:hover {
    border-color: var(--primary-color);
    color: var(--primary-color);
  }
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px;
  color: var(--text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.spinner-sm {
  width: 24px;
  height: 24px;
  border-width: 2px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.result-card {
  padding: 0;
  overflow: hidden;
}

.result-header {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid var(--border-color);

  &.high {
    background: linear-gradient(135deg, #fef2f2, #fee2e2);
  }

  &.medium {
    background: linear-gradient(135deg, #fff7ed, #ffedd5);
  }

  &.low {
    background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  }

  &.none {
    background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  }
}

.result-icon {
  font-size: 48px;
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.result-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.result-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  gap: 2px;

  &.high .stat-number { color: #dc2626; }
  &.medium .stat-number { color: #ea580c; }
  &.low .stat-number { color: #16a34a; }
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.conflicts-list {
  padding: 20px 24px;
}

.list-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 14px 0;
}

.conflict-item {
  padding: 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
  border-left: 4px solid;

  &:last-child {
    margin-bottom: 0;
  }

  &.high {
    background: #fef2f2;
    border-left-color: #dc2626;
  }

  &.medium {
    background: #fff7ed;
    border-left-color: #ea580c;
  }

  &.low {
    background: #f0fdf4;
    border-left-color: #16a34a;
  }
}

.conflict-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.conflict-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.conflict-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.conflict-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 15px;
}

.conflict-type-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.08);
  color: var(--text-secondary);
}

.conflict-severity {
  font-size: 12px;
  font-weight: 500;
  margin-left: auto;

  .high & { color: #dc2626; }
  .medium & { color: #ea580c; }
  .low & { color: #16a34a; }
}

.conflict-reason,
.conflict-replacement {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
  margin-top: 6px;
}

.reason-label,
.replacement-label {
  font-weight: 500;
  color: var(--text-primary);
}

.suggestions-section {
  padding: 0 24px 24px;
}

.suggestions-list {
  margin: 0;
  padding-left: 20px;

  li {
    font-size: 13px;
    line-height: 1.8;
    color: var(--text-secondary);
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.empty-result {
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-result h3 {
  font-size: 16px;
  color: var(--text-primary);
  margin: 0 0 6px 0;
}

.empty-result p {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0;
}

.recipe-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  backdrop-filter: blur(4px);
}

.recipe-picker-modal {
  background: white;
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: modalFadeIn 0.3s ease;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
  }
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s ease;

  svg {
    width: 20px;
    height: 20px;
  }

  &:hover {
    background: var(--bg-tertiary);
    color: var(--text-primary);
  }
}

.picker-search {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
}

.picker-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.picker-loading,
.picker-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--text-secondary);
  gap: 12px;
  font-size: 14px;
}

.picker-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    background: var(--bg-tertiary);
  }

  &.selected {
    background: #fef5e7;
  }
}

.picker-item-img {
  width: 50px;
  height: 50px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.picker-item-info {
  flex: 1;
  min-width: 0;
}

.picker-item-title {
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picker-item-meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  gap: 12px;
}

.picker-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.picker-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);

  .btn {
    flex: 1;
    justify-content: center;
  }
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .preference-section {
    position: static;
  }
}

@media (max-width: 600px) {
  .ingredient-conflict-page {
    padding: 20px 0 40px;
  }

  .page-title {
    font-size: 24px;
  }

  .section-card {
    padding: 16px;
  }

  .result-header {
    flex-direction: column;
    text-align: center;
    padding: 20px 16px;
  }

  .result-stats {
    width: 100%;
    justify-content: center;
  }

  .conflicts-list {
    padding: 16px;
  }

  .suggestions-section {
    padding: 0 16px 16px;
  }

  .method-tabs {
    flex-direction: column;
  }

  .picker-header,
  .picker-search,
  .picker-footer {
    padding: 14px 16px;
  }
}
</style>
