import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export const recipeApi = {
  getHotRecipes: () => api.get('/recipes/hot'),
  getRecipes: (params) => api.get('/recipes', { params }),
  getRecipeDetail: (id) => api.get(`/recipes/${id}`),
  createRecipe: (data) => api.post('/recipes', data),
  updateRecipe: (id, data) => api.put(`/recipes/${id}`, data),
  deleteRecipe: (id) => api.delete(`/recipes/${id}`),
  saveDraft: (data) => api.post('/recipes/draft', data),
  getDrafts: () => api.get('/recipes/drafts'),
  
  getFavorites: () => api.get('/favorites'),
  addFavorite: (recipeId) => api.post('/favorites', { recipeId }),
  removeFavorite: (recipeId) => api.delete(`/favorites/${recipeId}`),
  removeFavoritesBatch: (ids) => api.delete('/favorites/batch', { data: { ids } }),
  
  getUserRecipes: () => api.get('/user/recipes'),
  
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default api