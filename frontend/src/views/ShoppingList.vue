<template>
  <div class="shopping-list-page">
    <div class="container">
      <div class="page-header">
        <div class="header-title">
          <h1 class="page-title">🛒 购物清单</h1>
          <p class="page-desc">把要买的食材列出来，买菜不遗漏</p>
        </div>
        <div class="header-stats">
          <div class="stat-card">
            <span class="stat-value">{{ unpurchasedCount }}</span>
            <span class="stat-label">待购买</span>
          </div>
          <div class="stat-card purchased">
            <span class="stat-value">{{ purchasedCount }}</span>
            <span class="stat-label">已购买</span>
          </div>
        </div>
      </div>

      <div class="add-section section-card">
        <div class="add-form">
          <div class="form-row">
            <div class="form-item name-field">
              <label>食材名称</label>
              <input
                v-model="newItem.ingredientName"
                type="text"
                class="form-input"
                placeholder="例如：五花肉"
                @keyup.enter="handleAddItem"
              />
            </div>
            <div class="form-item amount-field">
              <label>用量</label>
              <input
                v-model="newItem.amount"
                type="text"
                class="form-input"
                placeholder="例如：500g"
                @keyup.enter="handleAddItem"
              />
            </div>
            <div class="form-item note-field">
              <label>备注</label>
              <input
                v-model="newItem.note"
                type="text"
                class="form-input"
                placeholder="选填，例如：选三层肉"
                @keyup.enter="handleAddItem"
              />
            </div>
            <div class="form-item submit-field">
              <label>&nbsp;</label>
              <button class="btn btn-primary add-btn" @click="handleAddItem">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                添加
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="items.length > 0" class="action-bar section-card">
        <div class="action-left">
          <label class="select-all">
            <input type="checkbox" v-model="selectAllPurchased" @change="toggleSelectAllPurchased" />
            选中已购
          </label>
          <span class="separator"></span>
          <span class="selected-info" v-if="selectedIds.length > 0">
            已选择 {{ selectedIds.length }} 项
          </span>
        </div>
        <div class="action-right">
          <button
            class="btn btn-outline"
            :disabled="purchasedCount === 0"
            @click="handleClearPurchased"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/>
              <path d="M10 11v6"/>
              <path d="M14 11v6"/>
            </svg>
            清空已购 ({{ purchasedCount }})
          </button>
          <button
            class="btn btn-danger"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/>
            </svg>
            批量删除 ({{ selectedIds.length }})
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading section-card">
        <div class="spinner"></div>
        <span>正在加载购物清单...</span>
      </div>

      <div v-else-if="items.length === 0" class="empty-state section-card">
        <div class="empty-icon">🛒</div>
        <h3>购物清单还是空的</h3>
        <p>可以添加食材，或从菜谱一键加入所有食材</p>
        <router-link to="/recipes" class="btn btn-primary">去浏览菜谱</router-link>
      </div>

      <div v-else class="list-section">
        <div v-if="unpurchasedItems.length > 0" class="items-group">
          <div class="group-header">
            <span class="group-title">待购买</span>
            <span class="group-count">{{ unpurchasedItems.length }}项</span>
          </div>
          <div class="items-list">
            <div
              v-for="item in unpurchasedItems"
              :key="item.id"
              class="item-card"
              :class="{ selected: selectedIds.includes(item.id) }"
            >
              <div class="item-check">
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(item.id)"
                  @change="toggleSelect(item.id)"
                />
              </div>
              <div class="item-purchase">
                <label class="check-purchase" :title="item.purchased ? '标记为未购买' : '标记为已购买'">
                  <input
                    type="checkbox"
                    :checked="item.purchased === 1"
                    @change="togglePurchased(item)"
                  />
                  <span class="checkmark"></span>
                </label>
              </div>
              <div class="item-content" v-if="editingId !== item.id">
                <div class="item-name">{{ item.ingredientName }}</div>
                <div class="item-meta">
                  <span v-if="item.amount" class="item-amount">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                      <line x1="3" y1="6" x2="21" y2="6"/>
                      <path d="M16 10a4 4 0 0 1-8 0"/>
                    </svg>
                    {{ item.amount }}
                  </span>
                  <span v-if="item.note" class="item-note">{{ item.note }}</span>
                </div>
              </div>
              <div class="item-edit" v-else>
                <input
                  v-model="editForm.ingredientName"
                  type="text"
                  class="edit-input name-input"
                  placeholder="食材名称"
                />
                <input
                  v-model="editForm.amount"
                  type="text"
                  class="edit-input amount-input"
                  placeholder="用量"
                />
                <input
                  v-model="editForm.note"
                  type="text"
                  class="edit-input note-input"
                  placeholder="备注"
                />
              </div>
              <div class="item-actions">
                <template v-if="editingId !== item.id">
                  <button class="action-btn edit" @click="startEdit(item)" title="编辑">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                    </svg>
                  </button>
                  <button class="action-btn delete" @click="handleDelete(item)" title="删除">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"/>
                      <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/>
                      <path d="M10 11v6"/>
                      <path d="M14 11v6"/>
                    </svg>
                  </button>
                </template>
                <template v-else>
                  <button class="action-btn save" @click="saveEdit(item)" title="保存">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </button>
                  <button class="action-btn cancel" @click="cancelEdit" title="取消">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div v-if="purchasedItems.length > 0" class="items-group purchased-group">
          <div class="group-header">
            <span class="group-title">✓ 已购买</span>
            <span class="group-count">{{ purchasedItems.length }}项</span>
          </div>
          <div class="items-list">
            <div
              v-for="item in purchasedItems"
              :key="item.id"
              class="item-card is-purchased"
              :class="{ selected: selectedIds.includes(item.id) }"
            >
              <div class="item-check">
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(item.id)"
                  @change="toggleSelect(item.id)"
                />
              </div>
              <div class="item-purchase">
                <label class="check-purchase" title="标记为未购买">
                  <input
                    type="checkbox"
                    :checked="item.purchased === 1"
                    @change="togglePurchased(item)"
                  />
                  <span class="checkmark"></span>
                </label>
              </div>
              <div class="item-content" v-if="editingId !== item.id">
                <div class="item-name">{{ item.ingredientName }}</div>
                <div class="item-meta">
                  <span v-if="item.amount" class="item-amount">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                      <line x1="3" y1="6" x2="21" y2="6"/>
                      <path d="M16 10a4 4 0 0 1-8 0"/>
                    </svg>
                    {{ item.amount }}
                  </span>
                  <span v-if="item.note" class="item-note">{{ item.note }}</span>
                </div>
              </div>
              <div class="item-edit" v-else>
                <input
                  v-model="editForm.ingredientName"
                  type="text"
                  class="edit-input name-input"
                  placeholder="食材名称"
                />
                <input
                  v-model="editForm.amount"
                  type="text"
                  class="edit-input amount-input"
                  placeholder="用量"
                />
                <input
                  v-model="editForm.note"
                  type="text"
                  class="edit-input note-input"
                  placeholder="备注"
                />
              </div>
              <div class="item-actions">
                <template v-if="editingId !== item.id">
                  <button class="action-btn edit" @click="startEdit(item)" title="编辑">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                    </svg>
                  </button>
                  <button class="action-btn delete" @click="handleDelete(item)" title="删除">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"/>
                      <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/>
                      <path d="M10 11v6"/>
                      <path d="M14 11v6"/>
                    </svg>
                  </button>
                </template>
                <template v-else>
                  <button class="action-btn save" @click="saveEdit(item)" title="保存">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </button>
                  <button class="action-btn cancel" @click="cancelEdit" title="取消">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shoppingListApi } from '@/utils/api'

const router = useRouter()

const loading = ref(false)
const items = ref([])
const selectedIds = ref([])
const editingId = ref(null)

const newItem = reactive({
  ingredientName: '',
  amount: '',
  note: ''
})

const editForm = reactive({
  ingredientName: '',
  amount: '',
  note: ''
})

const unpurchasedItems = computed(() => items.value.filter(i => i.purchased !== 1))
const purchasedItems = computed(() => items.value.filter(i => i.purchased === 1))
const unpurchasedCount = computed(() => unpurchasedItems.value.length)
const purchasedCount = computed(() => purchasedItems.value.length)

const selectAllPurchased = computed({
  get: () => purchasedCount.value > 0 && purchasedItems.value.every(i => selectedIds.value.includes(i.id)),
  set: () => {}
})

onMounted(() => {
  loadItems()
})

const loadItems = async () => {
  loading.value = true
  try {
    const result = await shoppingListApi.getShoppingList()
    items.value = result?.data || []
  } catch (e) {
    console.error('加载购物清单失败', e)
    ElMessage.error('加载购物清单失败')
  } finally {
    loading.value = false
  }
}

const handleAddItem = async () => {
  if (!newItem.ingredientName || !newItem.ingredientName.trim()) {
    ElMessage.warning('请输入食材名称')
    return
  }
  try {
    const result = await shoppingListApi.addShoppingItem({
      ingredientName: newItem.ingredientName.trim(),
      amount: newItem.amount.trim(),
      note: newItem.note.trim(),
      purchased: 0
    })
    if (result?.data) {
      items.value.unshift(result.data)
    }
    newItem.ingredientName = ''
    newItem.amount = ''
    newItem.note = ''
    ElMessage.success('添加成功')
  } catch (e) {
    console.error('添加失败', e)
    ElMessage.error('添加失败')
  }
}

const togglePurchased = async (item) => {
  try {
    const newStatus = item.purchased === 1 ? 0 : 1
    await shoppingListApi.togglePurchased(item.id, newStatus)
    item.purchased = newStatus
    if (newStatus === 1 && !selectedIds.value.includes(item.id)) {
      selectedIds.value.push(item.id)
    }
  } catch (e) {
    console.error('更新状态失败', e)
    ElMessage.error('操作失败')
  }
}

const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const toggleSelectAllPurchased = () => {
  const purchasedItemIds = purchasedItems.value.map(i => i.id)
  const allSelected = purchasedItemIds.every(id => selectedIds.value.includes(id))
  if (allSelected) {
    selectedIds.value = selectedIds.value.filter(id => !purchasedItemIds.includes(id))
  } else {
    purchasedItemIds.forEach(id => {
      if (!selectedIds.value.includes(id)) {
        selectedIds.value.push(id)
      }
    })
  }
}

const startEdit = (item) => {
  editingId.value = item.id
  editForm.ingredientName = item.ingredientName || ''
  editForm.amount = item.amount || ''
  editForm.note = item.note || ''
}

const cancelEdit = () => {
  editingId.value = null
}

const saveEdit = async (item) => {
  if (!editForm.ingredientName || !editForm.ingredientName.trim()) {
    ElMessage.warning('食材名称不能为空')
    return
  }
  try {
    await shoppingListApi.updateShoppingItem(item.id, {
      ingredientName: editForm.ingredientName.trim(),
      amount: editForm.amount.trim(),
      note: editForm.note.trim()
    })
    item.ingredientName = editForm.ingredientName.trim()
    item.amount = editForm.amount.trim()
    item.note = editForm.note.trim()
    editingId.value = null
    ElMessage.success('修改成功')
  } catch (e) {
    console.error('修改失败', e)
    ElMessage.error('修改失败')
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要删除 "${item.ingredientName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await shoppingListApi.deleteShoppingItem(item.id)
    items.value = items.value.filter(i => i.id !== item.id)
    selectedIds.value = selectedIds.value.filter(id => id !== item.id)
    ElMessage.success('删除成功')
  } catch {
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 项吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = [...selectedIds.value]
    await shoppingListApi.deleteBatch(ids)
    items.value = items.value.filter(i => !ids.includes(i.id))
    selectedIds.value = []
    ElMessage.success('批量删除成功')
  } catch {
  }
}

const handleClearPurchased = async () => {
  try {
    await ElMessageBox.confirm(`确定要清空所有已购买的 ${purchasedCount.value} 项吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await shoppingListApi.clearPurchased()
    items.value = items.value.filter(i => i.purchased !== 1)
    selectedIds.value = selectedIds.value.filter(id => {
      const item = items.value.find(i => i.id === id)
      return !!item
    })
    ElMessage.success('已清空已购买项')
  } catch {
  }
}
</script>

<style lang="scss" scoped>
.shopping-list-page {
  padding: 40px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  flex-wrap: wrap;
  gap: 20px;
}

.header-title {
  .page-title {
    font-size: 32px;
    margin-bottom: 8px;
  }
  .page-desc {
    color: var(--text-secondary);
  }
}

.header-stats {
  display: flex;
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 16px 24px;
  box-shadow: var(--shadow-sm);
  text-align: center;
  min-width: 100px;

  &.purchased {
    .stat-value {
      color: #10b981;
    }
  }

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--primary-color);
    display: block;
  }
  .stat-label {
    font-size: 13px;
    color: var(--text-secondary);
  }
}

.section-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.add-section {
  padding: 20px 24px;
}

.add-form .form-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;

  label {
    font-size: 13px;
    color: var(--text-secondary);
    font-weight: 500;
  }
}

.name-field {
  flex: 1.5;
  min-width: 160px;
}

.amount-field {
  flex: 1;
  min-width: 120px;
}

.note-field {
  flex: 1.5;
  min-width: 160px;
}

.submit-field {
  flex-shrink: 0;
}

.form-input {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;

  &:focus {
    border-color: var(--primary-color);
  }
}

.add-btn {
  padding: 10px 20px;
  font-size: 14px;
  height: 40px;

  svg {
    width: 16px;
    height: 16px;
  }
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.action-left, .action-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);

  input {
    cursor: pointer;
  }
}

.separator {
  width: 1px;
  height: 20px;
  background: var(--border-color);
}

.selected-info {
  font-size: 14px;
  color: var(--primary-color);
  font-weight: 500;
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

.btn {
  display: flex;
  align-items: center;
  gap: 6px;

  svg {
    width: 16px;
    height: 16px;
  }
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 60px 20px;
  color: var(--text-secondary);
  font-size: 14px;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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

.items-group {
  margin-bottom: 32px;

  &.purchased-group {
    .group-title {
      color: #10b981;
    }
  }
}

.group-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 0 4px;
}

.group-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.group-count {
  font-size: 13px;
  color: var(--text-light);
  background: var(--bg-tertiary);
  padding: 2px 10px;
  border-radius: 10px;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  border: 2px solid transparent;

  &:hover {
    box-shadow: var(--shadow-md);
  }

  &.selected {
    border-color: var(--primary-color);
  }

  &.is-purchased {
    opacity: 0.75;
    background: #f9fafb;

    .item-name {
      text-decoration: line-through;
      color: var(--text-light);
    }
    .item-meta {
      text-decoration: line-through;
      color: var(--text-light);
    }
  }
}

.item-check {
  flex-shrink: 0;
  input {
    width: 18px;
    height: 18px;
    cursor: pointer;
  }
}

.item-purchase {
  flex-shrink: 0;
}

.check-purchase {
  position: relative;
  display: inline-flex;
  width: 24px;
  height: 24px;
  cursor: pointer;

  input {
    position: absolute;
    opacity: 0;
    cursor: pointer;
  }

  .checkmark {
    width: 24px;
    height: 24px;
    border: 2px solid var(--border-color);
    border-radius: 6px;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: center;

    &::after {
      content: '';
      width: 5px;
      height: 10px;
      border: solid white;
      border-width: 0 2px 2px 0;
      transform: rotate(45deg) scale(0);
      transition: transform 0.15s ease;
      margin-top: -2px;
    }
  }

  input:checked + .checkmark {
    background: #10b981;
    border-color: #10b981;

    &::after {
      transform: rotate(45deg) scale(1);
    }
  }

  input:hover + .checkmark {
    border-color: var(--primary-color);
  }
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-secondary);
}

.item-amount {
  display: inline-flex;
  align-items: center;
  gap: 4px;

  svg {
    width: 14px;
    height: 14px;
  }
}

.item-note {
  color: var(--text-light);
}

.item-edit {
  flex: 1;
  display: flex;
  gap: 10px;
  min-width: 0;
}

.edit-input {
  border: 1px solid var(--primary-color);
  border-radius: var(--radius-sm);
  padding: 6px 10px;
  font-size: 14px;
  outline: none;
  background: white;
}

.name-input {
  flex: 1.5;
  min-width: 100px;
}

.amount-input {
  flex: 1;
  min-width: 80px;
}

.note-input {
  flex: 1.5;
  min-width: 100px;
}

.item-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.action-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  background: var(--bg-tertiary);
  color: var(--text-secondary);

  svg {
    width: 16px;
    height: 16px;
  }

  &.edit:hover {
    background: var(--primary-color);
    color: white;
  }

  &.save {
    background: #dcfce7;
    color: #16a34a;

    &:hover {
      background: #16a34a;
      color: white;
    }
  }

  &.cancel:hover {
    background: #f3f4f6;
    color: var(--text-primary);
  }

  &.delete {
    background: #fef2f2;
    color: #ef4444;

    &:hover {
      background: #ef4444;
      color: white;
    }
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-stats {
    width: 100%;

    .stat-card {
      flex: 1;
    }
  }

  .add-form .form-row {
    flex-direction: column;
    align-items: stretch;
  }

  .form-item {
    width: 100%;
  }

  .submit-field .add-btn {
    width: 100%;
    justify-content: center;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-right {
    .btn {
      flex: 1;
      justify-content: center;
    }
  }

  .item-card {
    padding: 12px;
    gap: 10px;
    flex-wrap: wrap;
  }

  .item-content {
    min-width: calc(100% - 140px);
  }

  .item-edit {
    width: 100%;
    order: 5;
  }

  .section-card {
    padding: 16px;
  }

  .page-title {
    font-size: 26px;
  }
}

@media (max-width: 480px) {
  .item-content {
    min-width: 100%;
    order: 4;
    padding-top: 4px;
    border-top: 1px dashed var(--border-color);
    padding-left: 40px;
  }

  .item-actions {
    margin-left: auto;
  }
}
</style>
