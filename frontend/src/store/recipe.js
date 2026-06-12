import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { recipeApi } from '@/utils/api'

const STORAGE_KEY = 'recipe-timer-store'
const TAGS_STORAGE_KEY = 'recipe-favorite-tags-store'
const RECIPE_TAGS_STORAGE_KEY = 'recipe-favorite-recipe-tags-store'
const SERVINGS_STORAGE_KEY = 'recipe-servings-store'

const loadTimersFromStorage = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) {
    console.error('Failed to load timers from storage', e)
  }
  return {}
}

const loadTagsFromStorage = () => {
  try {
    const saved = localStorage.getItem(TAGS_STORAGE_KEY)
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) {
    console.error('Failed to load tags from storage', e)
  }
  return []
}

const loadRecipeTagsFromStorage = () => {
  try {
    const saved = localStorage.getItem(RECIPE_TAGS_STORAGE_KEY)
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) {
    console.error('Failed to load recipe tags from storage', e)
  }
  return {}
}

const loadServingsFromStorage = () => {
  try {
    const saved = localStorage.getItem(SERVINGS_STORAGE_KEY)
    if (saved) {
      return JSON.parse(saved)
    }
  } catch (e) {
    console.error('Failed to load servings from storage', e)
  }
  return {}
}

export const useRecipeStore = defineStore('recipe', () => {
  const recipes = ref([])
  const currentRecipe = ref(null)
  const favorites = ref([])
  const userRecipes = ref([])
  const hotRecipes = ref([])
  const loading = ref(false)
  const favoriteVersion = ref(0)
  const tagVersion = ref(0)
  const recipeVersion = ref(0)

  const timers = ref(loadTimersFromStorage())
  const tags = ref(loadTagsFromStorage())
  const recipeTags = ref(loadRecipeTagsFromStorage())
  const servings = ref(loadServingsFromStorage())
  const originalServings = ref({})

  const saveTimersToStorage = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(timers.value))
    } catch (e) {
      console.error('Failed to save timers to storage', e)
    }
  }

  const saveTagsToStorage = () => {
    try {
      localStorage.setItem(TAGS_STORAGE_KEY, JSON.stringify(tags.value))
    } catch (e) {
      console.error('Failed to save tags to storage', e)
    }
  }

  const saveRecipeTagsToStorage = () => {
    try {
      localStorage.setItem(RECIPE_TAGS_STORAGE_KEY, JSON.stringify(recipeTags.value))
    } catch (e) {
      console.error('Failed to save recipe tags to storage', e)
    }
  }

  const saveServingsToStorage = () => {
    try {
      localStorage.setItem(SERVINGS_STORAGE_KEY, JSON.stringify(servings.value))
    } catch (e) {
      console.error('Failed to save servings to storage', e)
    }
  }

  watch(timers, saveTimersToStorage, { deep: true })
  watch(tags, saveTagsToStorage, { deep: true })
  watch(recipeTags, saveRecipeTagsToStorage, { deep: true })
  watch(servings, saveServingsToStorage, { deep: true })

  const getTimerState = (recipeId, stepIndex) => {
    const key = `${recipeId}-${stepIndex}`
    return timers.value[key] || {
      duration: 60,
      remaining: 60,
      isRunning: false,
      startTime: null,
      elapsedBeforePause: 0
    }
  }

  const setTimerState = (recipeId, stepIndex, state) => {
    const key = `${recipeId}-${stepIndex}`
    timers.value[key] = { ...state }
    saveTimersToStorage()
  }

  const resetTimerState = (recipeId, stepIndex) => {
    const key = `${recipeId}-${stepIndex}`
    delete timers.value[key]
    saveTimersToStorage()
  }

  const resetAllTimersForRecipe = (recipeId) => {
    const keys = Object.keys(timers.value).filter(k => k.startsWith(`${recipeId}-`))
    keys.forEach(key => {
      delete timers.value[key]
    })
    saveTimersToStorage()
  }

  const getServings = (recipeId) => {
    return servings.value[recipeId] || null
  }

  const setServings = (recipeId, value) => {
    servings.value[recipeId] = value
    saveServingsToStorage()
  }

  const getOriginalServings = (recipeId) => {
    return originalServings.value[recipeId] || 1
  }

  const setOriginalServings = (recipeId, value) => {
    originalServings.value[recipeId] = value
  }

  const resetServings = (recipeId) => {
    if (originalServings.value[recipeId]) {
      servings.value[recipeId] = originalServings.value[recipeId]
      saveServingsToStorage()
    }
  }

  const getRecipeById = computed(() => (id) => {
    return recipes.value.find(r => r.id === id) || null
  })

  const isFavorite = computed(() => (id) => {
    return favorites.value.some(f => f.id === id)
  })

  const setRecipes = (data) => {
    recipes.value = data
  }

  const setCurrentRecipe = (recipe) => {
    currentRecipe.value = recipe
  }

  const setFavorites = (data) => {
    favorites.value = data
  }

  const updateRecipeFavoriteCount = (recipeId, delta) => {
    const updateInList = (list) => {
      const item = list.find(r => r.id === recipeId)
      if (item) {
        item.favoriteCount = Math.max(0, (item.favoriteCount || 0) + delta)
      }
    }
    updateInList(hotRecipes.value)
    updateInList(recipes.value)
    updateInList(userRecipes.value)
    updateInList(favorites.value)
  }

  const addFavorite = async (recipe) => {
    if (favorites.value.find(f => f.id === recipe.id)) {
      return
    }
    try {
      await recipeApi.addFavorite(recipe.id)
    } catch (e) {
      console.warn('调用后端收藏接口失败，继续本地更新', e)
    }
    favorites.value.unshift({ ...recipe })
    updateRecipeFavoriteCount(recipe.id, 1)
    favoriteVersion.value++
  }

  const removeFavorite = async (id) => {
    const existed = favorites.value.find(f => f.id === id)
    if (!existed) {
      return
    }
    try {
      await recipeApi.removeFavorite(id)
    } catch (e) {
      console.warn('调用后端取消收藏接口失败，继续本地更新', e)
    }
    favorites.value = favorites.value.filter(f => f.id !== id)
    const newRecipeTags = { ...recipeTags.value }
    delete newRecipeTags[id]
    recipeTags.value = newRecipeTags
    saveRecipeTagsToStorage()
    updateRecipeFavoriteCount(id, -1)
    favoriteVersion.value++
    tagVersion.value++
  }

  const removeFavoritesBatch = async (ids) => {
    const validIds = ids.filter(id => favorites.value.some(f => f.id === id))
    if (validIds.length === 0) return
    try {
      await recipeApi.removeFavoritesBatch(validIds)
    } catch (e) {
      console.warn('调用后端批量取消收藏接口失败，继续本地更新', e)
    }
    favorites.value = favorites.value.filter(f => !validIds.includes(f.id))
    const newRecipeTags = { ...recipeTags.value }
    validIds.forEach(id => {
      delete newRecipeTags[id]
      updateRecipeFavoriteCount(id, -1)
    })
    recipeTags.value = newRecipeTags
    saveRecipeTagsToStorage()
    favoriteVersion.value++
    tagVersion.value++
  }

  const addTag = (tagName) => {
    const name = tagName.trim()
    if (!name) return null
    const existing = tags.value.find(t => t.name === name)
    if (existing) {
      return existing
    }
    const newTag = {
      id: Date.now().toString(),
      name,
      color: getRandomTagColor()
    }
    tags.value.push(newTag)
    tagVersion.value++
    return newTag
  }

  const renameTag = (tagId, newName) => {
    const name = newName.trim()
    if (!name) return false
    const tag = tags.value.find(t => t.id === tagId)
    if (!tag) return false
    if (tags.value.find(t => t.name === name && t.id !== tagId)) {
      return false
    }
    tag.name = name
    tagVersion.value++
    return true
  }

  const deleteTag = (tagId) => {
    tags.value = tags.value.filter(t => t.id !== tagId)
    const newRecipeTags = { ...recipeTags.value }
    Object.keys(newRecipeTags).forEach(recipeId => {
      newRecipeTags[recipeId] = newRecipeTags[recipeId].filter(id => id !== tagId)
    })
    recipeTags.value = newRecipeTags
    saveRecipeTagsToStorage()
    tagVersion.value++
  }

  const getRandomTagColor = () => {
    const colors = [
      '#e67e22', '#27ae60', '#3498db', '#9b59b6',
      '#e74c3c', '#1abc9c', '#f39c12', '#34495e'
    ]
    return colors[Math.floor(Math.random() * colors.length)]
  }

  const addTagToRecipe = (recipeId, tagId) => {
    const newRecipeTags = { ...recipeTags.value }
    if (!newRecipeTags[recipeId]) {
      newRecipeTags[recipeId] = []
    }
    if (!newRecipeTags[recipeId].includes(tagId)) {
      newRecipeTags[recipeId] = [...newRecipeTags[recipeId], tagId]
      recipeTags.value = newRecipeTags
      saveRecipeTagsToStorage()
      tagVersion.value++
    }
  }

  const removeTagFromRecipe = (recipeId, tagId) => {
    if (recipeTags.value[recipeId]) {
      const newRecipeTags = { ...recipeTags.value }
      newRecipeTags[recipeId] = newRecipeTags[recipeId].filter(id => id !== tagId)
      recipeTags.value = newRecipeTags
      saveRecipeTagsToStorage()
      tagVersion.value++
    }
  }

  const getRecipeTagIds = computed(() => (recipeId) => {
    return recipeTags.value[recipeId] || []
  })

  const getRecipeTags = computed(() => (recipeId) => {
    const tagIds = recipeTags.value[recipeId] || []
    return tags.value.filter(t => tagIds.includes(t.id))
  })

  const getTagRecipeCount = computed(() => (tagId) => {
    return Object.values(recipeTags.value).filter(ids => ids.includes(tagId)).length
  })

  const setUserRecipes = (data) => {
    userRecipes.value = data
  }

  const setHotRecipes = (data) => {
    hotRecipes.value = data
  }

  const addRecipeToStore = (recipe) => {
    userRecipes.value = [recipe, ...userRecipes.value]
    recipes.value = [recipe, ...recipes.value]
    recipeVersion.value++
  }

  const removeRecipeFromStore = (recipeId) => {
    userRecipes.value = userRecipes.value.filter(r => r.id !== recipeId)
    recipes.value = recipes.value.filter(r => r.id !== recipeId)
    recipeVersion.value++
  }

  const bumpRecipeVersion = () => {
    recipeVersion.value++
  }

  const setLoading = (status) => {
    loading.value = status
  }

  return {
    recipes,
    currentRecipe,
    favorites,
    userRecipes,
    hotRecipes,
    loading,
    favoriteVersion,
    tagVersion,
    recipeVersion,
    timers,
    tags,
    recipeTags,
    servings,
    originalServings,
    getRecipeById,
    isFavorite,
    setRecipes,
    setCurrentRecipe,
    setFavorites,
    addFavorite,
    removeFavorite,
    removeFavoritesBatch,
    updateRecipeFavoriteCount,
    setUserRecipes,
    setHotRecipes,
    addRecipeToStore,
    removeRecipeFromStore,
    bumpRecipeVersion,
    setLoading,
    getTimerState,
    setTimerState,
    resetTimerState,
    resetAllTimersForRecipe,
    addTag,
    renameTag,
    deleteTag,
    addTagToRecipe,
    removeTagFromRecipe,
    getRecipeTagIds,
    getRecipeTags,
    getTagRecipeCount,
    getServings,
    setServings,
    getOriginalServings,
    setOriginalServings,
    resetServings
  }
})