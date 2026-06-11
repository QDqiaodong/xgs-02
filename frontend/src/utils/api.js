import axios from 'axios'

const MIN_PAGE = 1
const MAX_PAGE = 1000
const MIN_PAGE_SIZE = 1
const MAX_PAGE_SIZE = 100

const normalizePaginationParams = (params) => {
  if (!params) return { page: MIN_PAGE, size: 10 }
  const normalized = { ...params }
  const page = parseInt(normalized.page, 10)
  normalized.page = (!isNaN(page) && page >= MIN_PAGE) ? Math.min(page, MAX_PAGE) : MIN_PAGE
  const size = parseInt(normalized.size, 10)
  normalized.size = (!isNaN(size) && size >= MIN_PAGE_SIZE) ? Math.min(size, MAX_PAGE_SIZE) : 10
  return normalized
}

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
  getHotRecipes: (dimension = 'total') =>
    api.get('/recipes/hot', { params: { dimension } }).then((result) => result?.data || []),
  getRecipes: (params) => api.get('/recipes', { params: normalizePaginationParams(params) }),
  getRecipeDetail: (id) => api.get(`/recipes/${id}`),
  createRecipe: (data) => api.post('/recipes', data),
  updateRecipe: (id, data) => api.put(`/recipes/${id}`, data),
  deleteRecipe: (id) => api.delete(`/recipes/${id}`),
  saveDraft: (data) => api.post('/recipes/draft', data),
  getDrafts: () => api.get('/recipes/drafts'),
  getSimilarRecipes: (id, limit = 6) => {
    const safeLimit = parseInt(limit, 10)
    const finalLimit = (!isNaN(safeLimit) && safeLimit >= 1) ? Math.min(safeLimit, 50) : 6
    return api.get(`/recipes/${id}/similar`, { params: { limit: finalLimit } }).then((result) => result?.data || [])
  },
  
  getFavorites: () => api.get('/favorites'),
  addFavorite: (recipeId) => api.post('/favorites', { recipeId }),
  removeFavorite: (recipeId) => api.delete(`/favorites/${recipeId}`),
  removeFavoritesBatch: (ids) => api.delete('/favorites/batch', { data: { ids } }),
  
  getUserRecipes: () => api.get('/user/recipes'),
  getUserProfile: () => api.get('/user/profile'),
  
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const commentApi = {
  getComments: (recipeId, page = 1, size = 10) => {
    const safePage = parseInt(page, 10)
    const finalPage = (!isNaN(safePage) && safePage >= MIN_PAGE) ? Math.min(safePage, MAX_PAGE) : MIN_PAGE
    const safeSize = parseInt(size, 10)
    const finalSize = (!isNaN(safeSize) && safeSize >= MIN_PAGE_SIZE) ? Math.min(safeSize, MAX_PAGE_SIZE) : 10
    return api.get('/comments', { params: { recipeId, page: finalPage, size: finalSize } })
  },
  addComment: (recipeId, content) => api.post('/comments', { recipeId, content }),
  deleteComment: (id) => api.delete(`/comments/${id}`)
}

export const viewHistoryApi = {
  getViewHistory: (page = 1, size = 10) => {
    const safePage = parseInt(page, 10)
    const finalPage = (!isNaN(safePage) && safePage >= MIN_PAGE) ? Math.min(safePage, MAX_PAGE) : MIN_PAGE
    const safeSize = parseInt(size, 10)
    const finalSize = (!isNaN(safeSize) && safeSize >= MIN_PAGE_SIZE) ? Math.min(safeSize, MAX_PAGE_SIZE) : 10
    return api.get('/view-history', { params: { page: finalPage, size: finalSize } })
  },
  removeViewHistory: (recipeId) => api.delete(`/view-history/${recipeId}`),
  clearAllHistory: () => api.delete('/view-history/clear')
}

export default api
