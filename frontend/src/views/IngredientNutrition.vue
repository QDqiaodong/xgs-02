<template>
  <div class="ingredient-nutrition-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">🥗 食材营养管理</h1>
        <p class="page-desc">管理食材营养成分数据，用于自动计算食谱营养</p>
      </div>

      <div class="nutrition-goals-card section-card">
        <div class="goals-card-header">
          <div>
            <h2 class="goals-card-title">🎯 每日营养目标设置</h2>
            <p class="goals-card-desc">设定您的每日营养摄入目标，用于查看食谱占比和超标提醒</p>
          </div>
          <button 
            v-if="!showGoalsEditor"
            class="btn btn-primary" 
            @click="openGoalsEditor"
          >
            编辑目标
          </button>
          <div v-else class="editor-actions">
            <button class="btn btn-outline" @click="cancelGoalsEdit">取消</button>
            <button class="btn btn-outline reset-btn" @click="resetGoalsToDefault">恢复默认</button>
            <button class="btn btn-primary" @click="saveGoals" :disabled="savingGoals">
              {{ savingGoals ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>

        <div v-if="!showGoalsEditor" class="goals-display">
          <div class="goal-display-item calories">
            <div class="goal-icon">🔥</div>
            <div class="goal-info">
              <div class="goal-value">{{ goalsDisplay.calories }}</div>
              <div class="goal-unit">千卡/天</div>
              <div class="goal-label">每日热量</div>
            </div>
          </div>
          <div class="goal-display-item protein">
            <div class="goal-icon">💪</div>
            <div class="goal-info">
              <div class="goal-value">{{ goalsDisplay.protein }}</div>
              <div class="goal-unit">克/天</div>
              <div class="goal-label">每日蛋白质</div>
            </div>
          </div>
          <div class="goal-display-item sodium">
            <div class="goal-icon">🧂</div>
            <div class="goal-info">
              <div class="goal-value">{{ goalsDisplay.sodium }}</div>
              <div class="goal-unit">毫克/天</div>
              <div class="goal-label">每日钠摄入</div>
            </div>
          </div>
        </div>

        <div v-else class="goals-editor">
          <div class="goals-editor-grid">
            <div class="form-group editor-group">
              <label>🔥 每日热量目标 (千卡)</label>
              <input 
                v-model.number="goalsEditForm.calories" 
                type="number" 
                min="500" 
                max="8000" 
                step="50"
                placeholder="2000"
              />
              <div class="input-hint">推荐范围：女性 1500-2000，男性 2000-2500</div>
            </div>
            <div class="form-group editor-group">
              <label>💪 每日蛋白质目标 (克)</label>
              <input 
                v-model.number="goalsEditForm.protein" 
                type="number" 
                min="10" 
                max="300" 
                step="5"
                placeholder="60"
              />
              <div class="input-hint">推荐范围：每公斤体重 0.8-1.6 克蛋白质</div>
            </div>
            <div class="form-group editor-group">
              <label>🧂 每日钠摄入目标 (毫克)</label>
              <input 
                v-model.number="goalsEditForm.sodium" 
                type="number" 
                min="200" 
                max="6000" 
                step="100"
                placeholder="2000"
              />
              <div class="input-hint">WHO推荐：不超过 2000mg（约5克盐）</div>
            </div>
          </div>
        </div>
      </div>

      <div class="toolbar">
        <div class="search-bar">
          <input 
            v-model="searchKeyword" 
            type="text" 
            class="search-input" 
            placeholder="搜索食材名称..."
            @keyup.enter="handleSearch"
          />
          <select v-model="selectedCategory" class="category-select" @change="handleSearch">
            <option value="">全部分类</option>
            <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
        </div>
        <button class="btn btn-primary add-btn" @click="openCreateModal">
          <span>+</span> 新增食材
        </button>
      </div>

      <div class="nutrition-table-container" v-loading="loading">
        <table class="nutrition-table">
          <thead>
            <tr>
              <th>食材名称</th>
              <th>分类</th>
              <th>热量<br/>(千卡/100g)</th>
              <th>蛋白质<br/>(g/100g)</th>
              <th>脂肪<br/>(g/100g)</th>
              <th>碳水<br/>(g/100g)</th>
              <th>膳食纤维<br/>(g/100g)</th>
              <th>钠<br/>(mg/100g)</th>
              <th class="action-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in nutritionList" :key="item.id">
              <td>
                <div class="ingredient-name">{{ item.name }}</div>
                <div class="ingredient-alias" v-if="item.alias">{{ item.alias }}</div>
              </td>
              <td>
                <span class="category-tag">{{ item.category || '-' }}</span>
              </td>
              <td class="num-cell">{{ item.calories || 0 }}</td>
              <td class="num-cell">{{ item.protein || 0 }}</td>
              <td class="num-cell">{{ item.fat || 0 }}</td>
              <td class="num-cell">{{ item.carbohydrate || 0 }}</td>
              <td class="num-cell">{{ item.fiber || 0 }}</td>
              <td class="num-cell">{{ item.sodium || 0 }}</td>
              <td class="action-col">
                <button class="action-btn edit-btn" @click="openEditModal(item)">编辑</button>
                <button class="action-btn delete-btn" @click="handleDelete(item)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination" v-if="total > 0">
          <span class="total-text">共 {{ total }} 条</span>
          <button 
            class="page-btn" 
            :disabled="currentPage <= 1" 
            @click="currentPage--; loadData()"
          >上一页</button>
          <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
          <button 
            class="page-btn" 
            :disabled="currentPage >= totalPages" 
            @click="currentPage++; loadData()"
          >下一页</button>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEdit ? '编辑食材营养' : '新增食材营养' }}</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label>食材名称 *</label>
              <input v-model="formData.name" type="text" placeholder="请输入食材名称" />
            </div>
            <div class="form-group">
              <label>分类</label>
              <select v-model="formData.category">
                <option value="">请选择分类</option>
                <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>别名</label>
              <input v-model="formData.alias" type="text" placeholder="多个别名用逗号分隔" />
            </div>
            <div class="form-group">
              <label>计量单位</label>
              <input v-model="formData.unit" type="text" placeholder="默认：g" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>热量 (千卡/100g)</label>
              <input v-model.number="formData.calories" type="number" step="0.01" placeholder="0.00" />
            </div>
            <div class="form-group">
              <label>蛋白质 (g/100g)</label>
              <input v-model.number="formData.protein" type="number" step="0.01" placeholder="0.00" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>脂肪 (g/100g)</label>
              <input v-model.number="formData.fat" type="number" step="0.01" placeholder="0.00" />
            </div>
            <div class="form-group">
              <label>碳水化合物 (g/100g)</label>
              <input v-model.number="formData.carbohydrate" type="number" step="0.01" placeholder="0.00" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>膳食纤维 (g/100g)</label>
              <input v-model.number="formData.fiber" type="number" step="0.01" placeholder="0.00" />
            </div>
            <div class="form-group">
              <label>钠 (mg/100g)</label>
              <input v-model.number="formData.sodium" type="number" step="0.01" placeholder="0.00" />
            </div>
          </div>
          <div class="form-group">
            <label>备注</label>
            <textarea v-model="formData.remark" rows="3" placeholder="备注信息"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="closeModal">取消</button>
          <button class="btn btn-primary" @click="handleSubmit" :disabled="submitting">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ingredientNutritionApi } from '@/utils/api'
import { useRecipeStore } from '@/store/recipe'

const store = useRecipeStore()

const loading = ref(false)
const submitting = ref(false)

const showGoalsEditor = ref(false)
const savingGoals = ref(false)
const goalsEditForm = reactive({
  calories: 2000,
  protein: 60,
  sodium: 2000
})

const goalsDisplay = computed(() => {
  const goals = store.getNutritionGoals()
  return {
    calories: goals.calories || 2000,
    protein: goals.protein || 60,
    sodium: goals.sodium || 2000
  }
})

const openGoalsEditor = () => {
  const current = store.getNutritionGoals()
  goalsEditForm.calories = current.calories || 2000
  goalsEditForm.protein = current.protein || 60
  goalsEditForm.sodium = current.sodium || 2000
  showGoalsEditor.value = true
}

const cancelGoalsEdit = () => {
  showGoalsEditor.value = false
}

const resetGoalsToDefault = () => {
  goalsEditForm.calories = 2000
  goalsEditForm.protein = 60
  goalsEditForm.sodium = 2000
  ElMessage.info('已恢复默认目标值')
}

const saveGoals = () => {
  if (!goalsEditForm.calories || goalsEditForm.calories < 500 || goalsEditForm.calories > 8000) {
    ElMessage.warning('请输入有效的热量目标 (500-8000千卡)')
    return
  }
  if (!goalsEditForm.protein || goalsEditForm.protein < 10 || goalsEditForm.protein > 300) {
    ElMessage.warning('请输入有效的蛋白质目标 (10-300克)')
    return
  }
  if (!goalsEditForm.sodium || goalsEditForm.sodium < 200 || goalsEditForm.sodium > 6000) {
    ElMessage.warning('请输入有效的钠摄入目标 (200-6000毫克)')
    return
  }

  savingGoals.value = true
  try {
    store.setNutritionGoals({
      calories: goalsEditForm.calories,
      protein: goalsEditForm.protein,
      sodium: goalsEditForm.sodium
    })
    ElMessage.success('营养目标保存成功！')
    showGoalsEditor.value = false
  } catch (err) {
    console.error('保存目标失败', err)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    savingGoals.value = false
  }
}
const nutritionList = ref([])
const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const selectedCategory = ref('')

const showModal = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const formData = ref({
  name: '',
  alias: '',
  category: '',
  unit: 'g',
  calories: 0,
  protein: 0,
  fat: 0,
  carbohydrate: 0,
  fiber: 0,
  sodium: 0,
  remark: ''
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const loadData = async () => {
  loading.value = true
  try {
    const result = await ingredientNutritionApi.getNutritions({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      category: selectedCategory.value || undefined
    })
    nutritionList.value = result?.data?.records || []
    total.value = result?.data?.total || 0
  } catch (err) {
    console.error('加载食材营养数据失败', err)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const result = await ingredientNutritionApi.getNutritionCategories()
    categories.value = result || []
  } catch (err) {
    console.error('加载分类失败', err)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const openCreateModal = () => {
  isEdit.value = false
  editId.value = null
  formData.value = {
    name: '',
    alias: '',
    category: '',
    unit: 'g',
    calories: 0,
    protein: 0,
    fat: 0,
    carbohydrate: 0,
    fiber: 0,
    sodium: 0,
    remark: ''
  }
  showModal.value = true
}

const openEditModal = (item) => {
  isEdit.value = true
  editId.value = item.id
  formData.value = { ...item }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const handleSubmit = async () => {
  if (!formData.value.name || formData.value.name.trim() === '') {
    ElMessage.warning('请输入食材名称')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await ingredientNutritionApi.updateNutrition(editId.value, formData.value)
      ElMessage.success('更新成功')
    } else {
      await ingredientNutritionApi.createNutrition(formData.value)
      ElMessage.success('创建成功')
    }
    closeModal()
    loadData()
    loadCategories()
  } catch (err) {
    console.error('保存失败', err)
    ElMessage.error(err?.response?.data?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除食材"${item.name}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await ingredientNutritionApi.deleteNutrition(item.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除失败', err)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style lang="scss" scoped>
.ingredient-nutrition-page {
  padding: 40px 0;
  min-height: calc(100vh - 200px);
}

.page-header {
  margin-bottom: 30px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-desc {
  color: var(--text-secondary);
  font-size: 15px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  padding: 10px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 14px;
  width: 240px;
  outline: none;
  transition: border-color 0.2s;

  &:focus {
    border-color: var(--primary-color);
  }
}

.category-select {
  padding: 10px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 14px;
  background: white;
  outline: none;
  cursor: pointer;

  &:focus {
    border-color: var(--primary-color);
  }
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;

  span {
    font-size: 18px;
    line-height: 1;
  }
}

.nutrition-table-container {
  background: white;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.nutrition-table {
  width: 100%;
  border-collapse: collapse;

  th, td {
    padding: 14px 16px;
    text-align: left;
    border-bottom: 1px solid var(--border-color);
  }

  th {
    background: var(--bg-tertiary);
    font-weight: 500;
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.4;
  }

  td {
    font-size: 14px;
  }

  tbody tr {
    transition: background 0.2s;

    &:hover {
      background: var(--bg-tertiary);
    }
  }

  .num-cell {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }

  .action-col {
    width: 140px;
    text-align: center;
  }
}

.ingredient-name {
  font-weight: 500;
  color: var(--text-primary);
}

.ingredient-alias {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 4px;
}

.category-tag {
  display: inline-block;
  padding: 4px 10px;
  background: var(--primary-light);
  color: var(--primary-color);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  margin: 0 4px;

  &.edit-btn {
    background: var(--primary-light);
    color: var(--primary-color);

    &:hover {
      background: var(--primary-color);
      color: white;
    }
  }

  &.delete-btn {
    background: #fee2e2;
    color: #dc2626;

    &:hover {
      background: #dc2626;
      color: white;
    }
  }
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
}

.total-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.page-info {
  font-size: 13px;
  color: var(--text-secondary);
}

.page-btn {
  padding: 6px 14px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: var(--radius-sm);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    border-color: var(--primary-color);
    color: var(--primary-color);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.modal-overlay {
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
}

.modal-content {
  background: white;
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 640px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);

  h3 {
    font-size: 20px;
    font-weight: 600;
    margin: 0;
  }
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    color: var(--text-primary);
  }
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 16px;

  label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 6px;
  }

  input, select, textarea {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    font-size: 14px;
    outline: none;
    transition: border-color 0.2s;
    font-family: inherit;

    &:focus {
      border-color: var(--primary-color);
    }
  }

  textarea {
    resize: vertical;
    min-height: 60px;
  }

  input[type="number"] {
    -moz-appearance: textfield;

    &::-webkit-outer-spin-button,
    &::-webkit-inner-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 768px) {
  .ingredient-nutrition-page {
    padding: 20px 0;
  }

  .page-title {
    font-size: 24px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar {
    width: 100%;
    
    .search-input {
      flex: 1;
      width: auto;
      min-width: 150px;
    }
  }

  .nutrition-table-container {
    overflow-x: auto;
  }

  .nutrition-table {
    min-width: 800px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .modal-content {
    width: 95%;
    max-height: 95vh;
  }

  .modal-body {
    padding: 16px;
  }
}

.section-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 28px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.nutrition-goals-card {
  background: linear-gradient(135deg, #ffffff 0%, #f0fdf4 100%);
  border: 1px solid #bbf7d0;
}

.goals-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.goals-card-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px 0;
  color: var(--text-primary);
}

.goals-card-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.editor-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.reset-btn {
  &:hover {
    border-color: #f59e0b !important;
    color: #f59e0b !important;
  }
}

.goals-display {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.goal-display-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px;
  border-radius: 16px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  &.calories {
    background: linear-gradient(135deg, #fef2f2, #fee2e2);
    border: 1px solid #fecaca;
  }

  &.protein {
    background: linear-gradient(135deg, #eff6ff, #dbeafe);
    border: 1px solid #bfdbfe;
  }

  &.sodium {
    background: linear-gradient(135deg, #f0fdfa, #ccfbf1);
    border: 1px solid #99f6e4;
  }
}

.goal-icon {
  font-size: 48px;
  line-height: 1;
  flex-shrink: 0;
}

.goal-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.goal-value {
  font-size: 36px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--text-primary);

  .calories & {
    color: #dc2626;
  }

  .protein & {
    color: #2563eb;
  }

  .sodium & {
    color: #0d9488;
  }
}

.goal-unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.goal-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: 4px;
}

.goals-editor {
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.goals-editor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.editor-group {
  margin-bottom: 0;

  label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 10px;
  }

  input {
    width: 100%;
    padding: 14px 16px;
    border: 2px solid var(--border-color);
    border-radius: var(--radius-md);
    font-size: 18px;
    font-weight: 600;
    outline: none;
    transition: all 0.3s ease;
    font-family: inherit;

    &:focus {
      border-color: var(--primary-color);
      box-shadow: 0 0 0 4px rgba(230, 126, 34, 0.1);
    }
  }
}

.input-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-light);
  line-height: 1.5;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  font-family: inherit;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  border: none;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(230, 126, 34, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-outline {
  background: white;
  color: var(--text-primary);
  border: 1px solid var(--border-color);

  &:hover:not(:disabled) {
    border-color: var(--primary-color);
    color: var(--primary-color);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

@media (max-width: 768px) {
  .nutrition-goals-card {
    padding: 20px 16px;
    margin-bottom: 20px;
  }

  .goals-card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .editor-actions {
    width: 100%;

    .btn {
      flex: 1;
    }
  }

  .goals-display {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .goal-display-item {
    padding: 18px 20px;
  }

  .goal-icon {
    font-size: 40px;
  }

  .goal-value {
    font-size: 30px;
  }

  .goals-editor-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .editor-group {
    input {
      padding: 12px 14px;
      font-size: 16px;
    }
  }
}

@media (max-width: 480px) {
  .section-card {
    padding: 16px 14px;
  }

  .goals-card-title {
    font-size: 18px;
  }

  .goals-card-desc {
    font-size: 13px;
  }

  .goal-display-item {
    padding: 16px;
    gap: 14px;
  }

  .goal-icon {
    font-size: 36px;
  }

  .goal-value {
    font-size: 26px;
  }
}
</style>
