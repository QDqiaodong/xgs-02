<template>
  <div class="favorites-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">我的收藏</h1>
        <p class="page-desc">收藏的美味，随时可以找到</p>
      </div>

      <div class="favorites-layout">
        <aside class="tags-sidebar">
          <div class="sidebar-header">
            <h3 class="sidebar-title">标签</h3>
            <button class="add-tag-btn" @click="showAddTagDialog = true" title="新建标签">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
          </div>

          <div class="tags-list">
            <div
              class="tag-item"
              :class="{ active: selectedTagId === null }"
              @click="selectedTagId = null"
            >
              <span class="tag-icon-all">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2"/>
                </svg>
              </span>
              <span class="tag-name">全部</span>
              <span class="tag-count">{{ favorites.length }}</span>
            </div>

            <div
              class="tag-item"
              :class="{ active: selectedTagId === 'untagged' }"
              @click="selectedTagId = 'untagged'"
            >
              <span class="tag-icon-untagged">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                </svg>
              </span>
              <span class="tag-name">未分类</span>
              <span class="tag-count">{{ untaggedCount }}</span>
            </div>

            <div
              v-for="tag in tags"
              :key="tag.id"
              class="tag-item"
              :class="{ active: selectedTagId === tag.id, editing: editingTagId === tag.id }"
              @click="!editingTagId && (selectedTagId = tag.id)"
            >
              <span class="tag-dot" :style="{ background: tag.color }"></span>
              <input
                v-if="editingTagId === tag.id"
                ref="tagEditInput"
                v-model="editingTagName"
                class="tag-edit-input"
                @click.stop
                @keyup.enter="confirmRenameTag(tag.id)"
                @keyup.esc="cancelRenameTag"
                @blur="confirmRenameTag(tag.id)"
              />
              <span v-else class="tag-name">{{ tag.name }}</span>
              <span class="tag-count">{{ getTagRecipeCount(tag.id) }}</span>
              <div class="tag-actions" v-if="editingTagId !== tag.id">
                <button class="tag-action-btn" @click.stop="startRenameTag(tag)" title="重命名">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="tag-action-btn delete" @click.stop="handleDeleteTag(tag)" title="删除">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/>
                    <path d="M10 11v6"/>
                    <path d="M14 11v6"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </aside>

        <div class="favorites-main">
          <div v-if="filteredFavorites.length > 0" class="action-bar">
            <label class="select-all">
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
              全选
            </label>
            <button
              class="btn btn-danger"
              :disabled="selectedIds.length === 0"
              @click="batchRemove"
            >
              取消收藏 ({{ selectedIds.length }})
            </button>
          </div>

          <div v-if="loading" class="loading">
            <div class="spinner"></div>
          </div>

          <div v-else-if="filteredFavorites.length === 0" class="empty-state">
            <template v-if="selectedTagId === null">
              <div class="empty-icon">📭</div>
              <h3>还没有收藏任何菜谱</h3>
              <p>去发现美味的菜谱并收藏吧</p>
              <router-link to="/recipes" class="btn btn-primary">浏览菜谱</router-link>
            </template>
            <template v-else-if="selectedTagId === 'untagged'">
              <div class="empty-icon">🏷️</div>
              <h3>没有未分类的菜谱</h3>
              <p>所有收藏的菜谱都已添加了标签</p>
            </template>
            <template v-else>
              <div class="empty-icon">🏷️</div>
              <h3>该标签下没有菜谱</h3>
              <p>可以为菜谱添加此标签</p>
            </template>
          </div>

          <div v-else class="recipe-list">
            <div
              v-for="recipe in filteredFavorites"
              :key="recipe.id"
              class="recipe-item"
              :class="{ selected: selectedIds.includes(recipe.id) }"
            >
              <div class="select-box">
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(recipe.id)"
                  @change="toggleSelect(recipe.id)"
                />
              </div>

              <div class="recipe-cover" @click="goToDetail(recipe.id)">
                <img :src="recipe.coverImage || defaultImage" :alt="recipe.title" />
              </div>

              <div class="recipe-info" @click="goToDetail(recipe.id)">
                <h3 class="recipe-title">{{ recipe.title }}</h3>
                <p class="recipe-desc">{{ recipe.description }}</p>
                <div class="recipe-meta">
                  <span class="meta-tag">{{ getDifficultyLabel(recipe.difficulty) }}</span>
                  <span class="meta-tag">{{ recipe.cookTime }}分钟</span>
                  <span class="meta-author">by {{ recipe.author }}</span>
                </div>
                <div class="recipe-tags-display" v-if="getRecipeTags(recipe.id).length > 0">
                  <span
                    v-for="tag in getRecipeTags(recipe.id)"
                    :key="tag.id"
                    class="custom-tag"
                    :style="{ background: tag.color + '20', color: tag.color, borderColor: tag.color + '40' }"
                  >
                    {{ tag.name }}
                  </span>
                </div>
              </div>

              <div class="recipe-actions">
                <button class="action-btn tag" @click="openTagDialog(recipe)" title="管理标签">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                    <line x1="7" y1="7" x2="7.01" y2="7"/>
                  </svg>
                </button>
                <button class="action-btn view" @click="goToDetail(recipe.id)" title="查看">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                </button>
                <button class="action-btn remove" @click="removeFavorite(recipe.id)" title="取消收藏">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddTagDialog" class="dialog-overlay" @click.self="closeAddTagDialog">
      <div class="dialog-box">
        <h3 class="dialog-title">新建标签</h3>
        <input
          ref="newTagInput"
          v-model="newTagName"
          class="dialog-input"
          placeholder="请输入标签名称"
          @keyup.enter="confirmAddTag"
          @keyup.esc="closeAddTagDialog"
        />
        <div class="dialog-actions">
          <button class="btn btn-outline" @click="closeAddTagDialog">取消</button>
          <button class="btn btn-primary" @click="confirmAddTag">创建</button>
        </div>
      </div>
    </div>

    <div v-if="showTagDialog" class="dialog-overlay" @click.self="closeTagDialog">
      <div class="dialog-box tag-dialog">
        <h3 class="dialog-title">管理标签 - {{ currentTagRecipe?.title }}</h3>
        <div class="tags-select">
          <div
            v-for="tag in tags"
            :key="tag.id"
            class="tag-select-item"
            :class="{ selected: isRecipeHasTag(currentTagRecipe?.id, tag.id) }"
            @click="toggleRecipeTag(currentTagRecipe?.id, tag.id)"
          >
            <span class="tag-dot" :style="{ background: tag.color }"></span>
            <span class="tag-name">{{ tag.name }}</span>
            <svg
              v-if="isRecipeHasTag(currentTagRecipe?.id, tag.id)"
              class="tag-check"
              viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"
            >
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
          <div v-if="tags.length === 0" class="no-tags-hint">
            还没有标签，点击下方按钮创建
          </div>
        </div>
        <div class="quick-add-tag">
          <input
            v-model="quickNewTagName"
            class="dialog-input"
            placeholder="快速创建新标签"
            @keyup.enter="quickAddTag"
          />
          <button class="btn btn-primary" @click="quickAddTag">创建</button>
        </div>
        <div class="dialog-actions">
          <button class="btn btn-primary" @click="closeTagDialog">完成</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRecipeStore } from '@/store/recipe'
import { recipeApi } from '@/utils/api'

const router = useRouter()
const store = useRecipeStore()

const loading = ref(false)
const selectedIds = ref([])
const selectedTagId = ref(null)
const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop'

const showAddTagDialog = ref(false)
const newTagName = ref('')
const newTagInput = ref(null)

const editingTagId = ref(null)
const editingTagName = ref('')
const tagEditInput = ref(null)

const showTagDialog = ref(false)
const currentTagRecipe = ref(null)
const quickNewTagName = ref('')

const favorites = ref([])

const loadFavorites = async () => {
  loading.value = true
  try {
    const result = await recipeApi.getFavorites()
    favorites.value = result?.data || []
    store.setFavorites(favorites.value)
  } catch (e) {
    console.error('加载收藏列表失败', e)
    ElMessage.error('加载收藏列表失败')
  } finally {
    loading.value = false
  }
}

const tags = computed(() => {
  store.tagVersion
  return store.tags
})

const filteredFavorites = computed(() => {
  store.tagVersion
  if (selectedTagId.value === null) {
    return favorites.value
  }
  if (selectedTagId.value === 'untagged') {
    return favorites.value.filter(r => store.getRecipeTagIds(r.id).length === 0)
  }
  return favorites.value.filter(r => store.getRecipeTagIds(r.id).includes(selectedTagId.value))
})

const untaggedCount = computed(() => {
  store.tagVersion
  return favorites.value.filter(r => store.getRecipeTagIds(r.id).length === 0).length
})

const selectAll = computed({
  get: () => selectedIds.value.length === filteredFavorites.value.length && filteredFavorites.value.length > 0,
  set: () => {}
})

watch(() => store.favoriteVersion, () => {
  loadFavorites()
})

onMounted(() => {
  loadFavorites()
})

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const getRecipeTags = (recipeId) => {
  store.tagVersion
  return store.getRecipeTags(recipeId)
}

const getTagRecipeCount = (tagId) => {
  store.tagVersion
  return store.getTagRecipeCount(tagId)
}

const isRecipeHasTag = (recipeId, tagId) => {
  if (!recipeId) return false
  store.tagVersion
  return store.getRecipeTagIds(recipeId).includes(tagId)
}

const goToDetail = (id) => {
  router.push(`/recipe/${id}`)
}

const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const toggleSelectAll = () => {
  if (selectedIds.value.length === filteredFavorites.value.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = filteredFavorites.value.map(r => r.id)
  }
}

const removeFavorite = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏这道菜谱吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await store.removeFavorite(id)
    favorites.value = favorites.value.filter(r => r.id !== id)
    selectedIds.value = selectedIds.value.filter(sid => sid !== id)
    ElMessage.success('已取消收藏')
  } catch {
  }
}

const batchRemove = async () => {
  try {
    await ElMessageBox.confirm(`确定要取消收藏选中的 ${selectedIds.value.length} 道菜谱吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const idsToRemove = [...selectedIds.value]
    await store.removeFavoritesBatch(idsToRemove)
    favorites.value = favorites.value.filter(r => !idsToRemove.includes(r.id))
    selectedIds.value = []
    ElMessage.success('批量取消收藏成功')
  } catch {
  }
}

const openAddTagDialog = () => {
  newTagName.value = ''
  showAddTagDialog.value = true
  nextTick(() => {
    newTagInput.value?.focus()
  })
}

const closeAddTagDialog = () => {
  showAddTagDialog.value = false
  newTagName.value = ''
}

const confirmAddTag = () => {
  const name = newTagName.value.trim()
  if (!name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  const existing = tags.value.find(t => t.name === name)
  if (existing) {
    ElMessage.warning('标签已存在')
    return
  }
  store.addTag(name)
  ElMessage.success('标签创建成功')
  closeAddTagDialog()
}

const startRenameTag = (tag) => {
  editingTagId.value = tag.id
  editingTagName.value = tag.name
  nextTick(() => {
    tagEditInput.value?.focus()
    tagEditInput.value?.select()
  })
}

const cancelRenameTag = () => {
  editingTagId.value = null
  editingTagName.value = ''
}

const confirmRenameTag = (tagId) => {
  if (!editingTagId.value) return
  const name = editingTagName.value.trim()
  if (!name) {
    cancelRenameTag()
    return
  }
  const existing = tags.value.find(t => t.name === name && t.id !== tagId)
  if (existing) {
    ElMessage.warning('标签名称已存在')
    editingTagName.value = tags.value.find(t => t.id === tagId)?.name || ''
    return
  }
  store.renameTag(tagId, name)
  ElMessage.success('标签已重命名')
  cancelRenameTag()
}

const handleDeleteTag = async (tag) => {
  try {
    await ElMessageBox.confirm(`确定要删除标签"${tag.name}"吗？该标签下的所有关联将被移除。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    store.deleteTag(tag.id)
    if (selectedTagId.value === tag.id) {
      selectedTagId.value = null
    }
    ElMessage.success('标签已删除')
  } catch {
  }
}

const openTagDialog = (recipe) => {
  currentTagRecipe.value = recipe
  quickNewTagName.value = ''
  showTagDialog.value = true
}

const closeTagDialog = () => {
  showTagDialog.value = false
  currentTagRecipe.value = null
  quickNewTagName.value = ''
}

const toggleRecipeTag = (recipeId, tagId) => {
  if (!recipeId) return
  if (isRecipeHasTag(recipeId, tagId)) {
    store.removeTagFromRecipe(recipeId, tagId)
  } else {
    store.addTagToRecipe(recipeId, tagId)
  }
}

const quickAddTag = () => {
  const name = quickNewTagName.value.trim()
  if (!name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  const existing = tags.value.find(t => t.name === name)
  if (existing) {
    if (currentTagRecipe.value && !isRecipeHasTag(currentTagRecipe.value.id, existing.id)) {
      store.addTagToRecipe(currentTagRecipe.value.id, existing.id)
    }
    quickNewTagName.value = ''
    ElMessage.info('标签已存在，已自动关联')
    return
  }
  const newTag = store.addTag(name)
  if (newTag && currentTagRecipe.value) {
    store.addTagToRecipe(currentTagRecipe.value.id, newTag.id)
  }
  quickNewTagName.value = ''
  ElMessage.success('标签创建并关联成功')
}
</script>

<style lang="scss" scoped>
.favorites-page {
  padding: 40px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 32px;
  margin-bottom: 8px;
}

.page-desc {
  color: var(--text-secondary);
}

.favorites-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.tags-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 20px;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.add-tag-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  svg {
    width: 18px;
    height: 18px;
  }

  &:hover {
    background: var(--primary-color);
    color: white;
  }
}

.tags-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    background: var(--bg-tertiary);

    .tag-actions {
      opacity: 1;
    }
  }

  &.active {
    background: var(--primary-color);

    .tag-name,
    .tag-count {
      color: white;
    }

    .tag-icon-all,
    .tag-icon-untagged {
      color: white;
    }

    .tag-actions {
      opacity: 1;

      .tag-action-btn {
        color: rgba(255, 255, 255, 0.85);

        &:hover {
          background: rgba(255, 255, 255, 0.2);
          color: white;
        }
      }
    }
  }

  &.editing {
    background: var(--bg-tertiary);
    cursor: text;
  }
}

.tag-icon-all,
.tag-icon-untagged {
  width: 18px;
  height: 18px;
  color: var(--text-secondary);
  flex-shrink: 0;

  svg {
    width: 100%;
    height: 100%;
  }
}

.tag-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tag-name {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-edit-input {
  flex: 1;
  border: 1px solid var(--primary-color);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 14px;
  outline: none;
  background: white;
}

.tag-count {
  font-size: 12px;
  color: var(--text-light);
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: 10px;
  flex-shrink: 0;

  .tag-item.active & {
    background: rgba(255, 255, 255, 0.2);
  }
}

.tag-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
}

.tag-action-btn {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  svg {
    width: 14px;
    height: 14px;
  }

  &:hover {
    background: var(--bg-secondary);
    color: var(--text-primary);
  }

  &.delete:hover {
    background: #fef2f2;
    color: #ef4444;
  }
}

.favorites-main {
  flex: 1;
  min-width: 0;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 16px 24px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
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

.recipe-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recipe-item {
  display: flex;
  align-items: center;
  gap: 20px;
  background: white;
  padding: 16px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    box-shadow: var(--shadow-md);
  }

  &.selected {
    border: 2px solid var(--primary-color);
  }
}

.select-box {
  flex-shrink: 0;

  input {
    width: 18px;
    height: 18px;
    cursor: pointer;
  }
}

.recipe-cover {
  flex-shrink: 0;
  width: 160px;
  height: 100px;
  border-radius: var(--radius-sm);
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.recipe-info {
  flex: 1;
  min-width: 0;
}

.recipe-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.recipe-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.meta-tag {
  padding: 2px 8px;
  background: var(--bg-tertiary);
  border-radius: 10px;
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-author {
  font-size: 12px;
  color: var(--text-light);
}

.recipe-tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.custom-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  border: 1px solid;
}

.recipe-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  svg {
    width: 18px;
    height: 18px;
  }

  &.view {
    background: var(--bg-tertiary);
    color: var(--text-secondary);

    &:hover {
      background: var(--primary-color);
      color: white;
    }
  }

  &.tag {
    background: var(--bg-tertiary);
    color: var(--text-secondary);

    &:hover {
      background: var(--secondary-color);
      color: white;
    }
  }

  &.remove {
    background: #fef2f2;
    color: #ef4444;

    &:hover {
      background: #ef4444;
      color: white;
    }
  }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);

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

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-box {
  background: white;
  border-radius: var(--radius-lg);
  padding: 28px;
  width: 90%;
  max-width: 420px;
  box-shadow: var(--shadow-lg);
}

.tag-dialog {
  max-width: 480px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.dialog-input {
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

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.tags-select {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.tag-select-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--primary-color);
    background: var(--bg-tertiary);
  }

  &.selected {
    background: var(--primary-color);
    border-color: var(--primary-color);

    .tag-name {
      color: white;
    }
  }
}

.tag-check {
  width: 18px;
  height: 18px;
  color: white;
  margin-left: auto;
}

.no-tags-hint {
  text-align: center;
  padding: 24px;
  color: var(--text-secondary);
  font-size: 14px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
}

.quick-add-tag {
  display: flex;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);

  .dialog-input {
    flex: 1;
  }
}

@media (max-width: 900px) {
  .favorites-layout {
    flex-direction: column;
  }

  .tags-sidebar {
    width: 100%;
    position: static;
  }

  .tags-list {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }

  .tag-item {
    padding: 6px 12px;
    border: 1px solid var(--border-color);

    .tag-actions {
      opacity: 1;
    }
  }
}

@media (max-width: 768px) {
  .recipe-item {
    flex-wrap: wrap;
  }

  .recipe-cover {
    width: 100%;
    height: 150px;
  }

  .action-bar {
    flex-wrap: wrap;
    gap: 12px;
  }
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
