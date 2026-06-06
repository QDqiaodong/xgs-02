import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useRecipeStore = defineStore('recipe', () => {
  const recipes = ref([])
  const currentRecipe = ref(null)
  const favorites = ref([])
  const userRecipes = ref([])
  const hotRecipes = ref([])
  const loading = ref(false)

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
    getRecipeById,
    isFavorite,
    setRecipes,
    setCurrentRecipe,
    setFavorites,
    addFavorite,
    removeFavorite,
    setUserRecipes,
    setHotRecipes,
    setLoading
  }
})