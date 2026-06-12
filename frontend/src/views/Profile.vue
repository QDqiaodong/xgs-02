<template>
  <div class="profile-page">
    <div class="container">
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>

      <template v-else>
        <div class="profile-header">
          <div class="profile-card">
            <div class="profile-bg"></div>
            <div class="profile-info">
              <div class="avatar-wrapper">
                <img :src="profile.avatar" :alt="profile.nickname" class="avatar" />
                <div class="avatar-badge">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2z"/>
                  </svg>
                </div>
              </div>
              <div class="user-details">
                <h1 class="nickname">{{ profile.nickname }}</h1>
                <p class="user-bio">热爱生活，热爱美食 🍳</p>
                <div class="user-meta">
                  <span class="meta-item">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                      <circle cx="12" cy="10" r="3"/>
                    </svg>
                    美食厨房
                  </span>
                  <span class="meta-item">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                      <line x1="16" y1="2" x2="16" y2="6"/>
                      <line x1="8" y1="2" x2="8" y2="6"/>
                      <line x1="3" y1="10" x2="21" y2="10"/>
                    </svg>
                    加入于 2024年1月
                  </span>
                </div>
              </div>
              <button class="edit-profile-btn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                编辑资料
              </button>
            </div>
          </div>
        </div>

        <div class="stats-section">
          <div class="stats-grid">
            <div class="stat-card" @click="goToPage('/my-recipes')">
              <div class="stat-icon recipes">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                  <line x1="3" y1="6" x2="21" y2="6"/>
                  <path d="M16 10a4 4 0 0 1-8 0"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ profile.recipeCount || 0 }}</span>
                <span class="stat-label">发布食谱</span>
              </div>
              <div class="stat-arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </div>
            </div>

            <div class="stat-card" @click="goToPage('/favorites')">
              <div class="stat-icon favorites">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ profile.favoriteCount || 0 }}</span>
                <span class="stat-label">我的收藏</span>
              </div>
              <div class="stat-arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon likes">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ profile.totalLikes || 0 }}</span>
                <span class="stat-label">总获赞</span>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon views">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ profile.viewCount || 0 }}</span>
                <span class="stat-label">总浏览</span>
              </div>
            </div>

            <div class="stat-card" @click="goToDrafts">
              <div class="stat-icon drafts">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ profile.draftCount || 0 }}</span>
                <span class="stat-label">草稿箱</span>
              </div>
              <div class="stat-arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div class="quick-section">
          <h2 class="section-title">快捷入口</h2>
          <div class="quick-grid">
            <router-link to="/create" class="quick-item create">
              <div class="quick-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
              </div>
              <span class="quick-label">发布食谱</span>
              <span class="quick-desc">分享你的拿手好菜</span>
            </router-link>

            <router-link to="/my-recipes" class="quick-item recipes">
              <div class="quick-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                  <line x1="3" y1="6" x2="21" y2="6"/>
                  <path d="M16 10a4 4 0 0 1-8 0"/>
                </svg>
              </div>
              <span class="quick-label">我的菜谱</span>
              <span class="quick-desc">管理已发布的内容</span>
            </router-link>

            <router-link to="/favorites" class="quick-item favorites">
              <div class="quick-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
              </div>
              <span class="quick-label">我的收藏</span>
              <span class="quick-desc">收藏的美味随时找</span>
            </router-link>

            <router-link to="/recipes" class="quick-item explore">
              <div class="quick-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </div>
              <span class="quick-label">发现美食</span>
              <span class="quick-desc">探索更多精彩菜谱</span>
            </router-link>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { recipeApi } from '@/utils/api'
import { useRecipeStore } from '@/store/recipe'

const router = useRouter()
const store = useRecipeStore()

const loading = ref(true)
const profile = ref({
  userId: 1,
  nickname: '美食达人',
  avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop',
  recipeCount: 0,
  favoriteCount: 0,
  totalLikes: 0,
  viewCount: 0,
  draftCount: 0
})

const fetchProfile = async () => {
  try {
    loading.value = true
    const result = await recipeApi.getUserProfile()
    if (result && result.data) {
      profile.value = result.data
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const goToPage = (path) => {
  router.push(path)
}

const goToDrafts = () => {
  router.push({ path: '/my-recipes', query: { tab: 'draft' } })
}

onMounted(() => {
  fetchProfile()
})

watch(() => store.recipeVersion, () => {
  fetchProfile()
})
</script>

<style lang="scss" scoped>
.profile-page {
  padding: 40px 0 60px;
}

.profile-header {
  margin-bottom: 40px;
}

.profile-card {
  background: white;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  position: relative;
}

.profile-bg {
  height: 180px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 50%, #f1c40f 100%);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: radial-gradient(circle at 20% 80%, rgba(255, 255, 255, 0.15) 0%, transparent 50%),
                      radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
  }
}

.profile-info {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 0 32px 32px;
  margin-top: -60px;
  position: relative;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid white;
  object-fit: cover;
  box-shadow: var(--shadow-md);
}

.avatar-badge {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #f1c40f, #f39c12);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border: 2px solid white;

  svg {
    width: 16px;
    height: 16px;
  }
}

.user-details {
  flex: 1;
  padding-bottom: 8px;
}

.nickname {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.user-bio {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 12px;
}

.user-meta {
  display: flex;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-light);

  svg {
    width: 14px;
    height: 14px;
  }
}

.edit-profile-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border-radius: 25px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-bottom: 8px;

  svg {
    width: 16px;
    height: 16px;
  }

  &:hover {
    background: var(--primary-color);
    color: white;
  }
}

.stats-section {
  margin-bottom: 48px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  svg {
    width: 28px;
    height: 28px;
  }

  &.recipes {
    background: linear-gradient(135deg, #ffeaa7, #fdcb6e);
    color: #d68910;
  }

  &.favorites {
    background: linear-gradient(135deg, #ff7675, #fab1a0);
    color: #c0392b;
  }

  &.likes {
    background: linear-gradient(135deg, #74b9ff, #a29bfe);
    color: #2980b9;
  }

  &.views {
    background: linear-gradient(135deg, #55efc4, #81ecec);
    color: #00b894;
  }

  &.drafts {
    background: linear-gradient(135deg, #dfe6e9, #b2bec3);
    color: #636e72;
  }
}

.stat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.stat-arrow {
  color: var(--text-light);
  opacity: 0;
  transition: all 0.2s ease;

  svg {
    width: 20px;
    height: 20px;
  }

  .stat-card:hover & {
    opacity: 1;
    transform: translateX(4px);
  }
}

.quick-section {
  .section-title {
    margin-bottom: 24px;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.quick-item {
  background: white;
  border-radius: var(--radius-md);
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);

    &::before {
      opacity: 1;
    }
  }

  &.create::before {
    background: linear-gradient(90deg, var(--primary-color), var(--primary-light));
  }
  &.recipes::before {
    background: linear-gradient(90deg, #f39c12, #f1c40f);
  }
  &.favorites::before {
    background: linear-gradient(90deg, #e74c3c, #ff7675);
  }
  &.explore::before {
    background: linear-gradient(90deg, #27ae60, #2ecc71);
  }
}

.quick-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;

  svg {
    width: 32px;
    height: 32px;
  }

  .create & {
    background: linear-gradient(135deg, #fdebd0, #f9e79f);
    color: #e67e22;
  }
  .recipes & {
    background: linear-gradient(135deg, #fef5e7, #fdebd0);
    color: #d68910;
  }
  .favorites & {
    background: linear-gradient(135deg, #fdedec, #fadbd8);
    color: #c0392b;
  }
  .explore & {
    background: linear-gradient(135deg, #eafaf1, #d5f5e3);
    color: #27ae60;
  }
}

.quick-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.quick-desc {
  font-size: 12px;
  color: var(--text-light);
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .profile-info {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 20px 24px;
    margin-top: -50px;
  }

  .user-meta {
    justify-content: center;
    flex-wrap: wrap;
  }

  .edit-profile-btn {
    margin-bottom: 0;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .quick-item {
    padding: 20px 16px;
  }
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px 20px;
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
