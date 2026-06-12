<template>
  <div class="ingredient-nutrition-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">🥗 食材营养管理</h1>
        <p class="page-desc">管理食材营养成分数据，用于自动计算食谱营养</p>
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ingredientNutritionApi } from '@/utils/api'

const loading = ref(false)
const submitting = ref(false)
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
</style>
