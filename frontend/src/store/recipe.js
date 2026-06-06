import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

const STORAGE_KEY = 'recipe-timer-store'

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

export const useRecipeStore = defineStore('recipe', () => {
  const recipes = ref([])
  const currentRecipe = ref(null)
  const favorites = ref([])
  const userRecipes = ref([])
  const hotRecipes = ref([])
  const loading = ref(false)

  const timers = ref(loadTimersFromStorage())

  const saveTimersToStorage = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(timers.value))
    } catch (e) {
      console.error('Failed to save timers to storage', e)
    }
  }

  watch(timers, saveTimersToStorage, { deep: true })

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

  const addFavorite = (recipe) => {
    if (!favorites.value.find(f => f.id === recipe.id)) {
      favorites.value.unshift(recipe)
    }
  }

  const removeFavorite = (id) => {
    favorites.value = favorites.value.filter(f => f.id !== id)
  }

  const setUserRecipes = (data) => {
    userRecipes.value = data
  }

  const setHotRecipes = (data) => {
    hotRecipes.value = data
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
    timers,
    getRecipeById,
    isFavorite,
    setRecipes,
    setCurrentRecipe,
    setFavorites,
    addFavorite,
    removeFavorite,
    setUserRecipes,
    setHotRecipes,
    setLoading,
    getTimerState,
    setTimerState,
    resetTimerState
  }
})