import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/recipes',
    name: 'Recipes',
    component: () => import('@/views/Recipes.vue')
  },
  {
    path: '/recipe/:id',
    name: 'RecipeDetail',
    component: () => import('@/views/RecipeDetail.vue')
  },
  {
    path: '/create',
    name: 'CreateRecipe',
    component: () => import('@/views/CreateRecipe.vue')
  },
  {
    path: '/edit/:id',
    name: 'EditRecipe',
    component: () => import('@/views/CreateRecipe.vue')
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/views/Favorites.vue')
  },
  {
    path: '/view-history',
    name: 'ViewHistory',
    component: () => import('@/views/ViewHistory.vue')
  },
  {
    path: '/my-recipes',
    name: 'MyRecipes',
    component: () => import('@/views/MyRecipes.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue')
  },
  {
    path: '/ingredient-nutrition',
    name: 'IngredientNutrition',
    component: () => import('@/views/IngredientNutrition.vue')
  },
  {
    path: '/shopping-list',
    name: 'ShoppingList',
    component: () => import('@/views/ShoppingList.vue')
  },
  {
    path: '/cooking-schedule',
    name: 'CookingSchedule',
    component: () => import('@/views/CookingSchedule.vue')
  },
  {
    path: '/ingredient-conflict',
    name: 'IngredientConflict',
    component: () => import('@/views/IngredientConflict.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

export default router