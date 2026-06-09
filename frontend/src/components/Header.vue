<template>
  <header class="header">
    <div class="container">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <span class="logo-icon">🍳</span>
          <span class="logo-text">家常食谱</span>
        </router-link>
        
        <nav class="nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/recipes" class="nav-link">菜谱大全</router-link>
          <router-link to="/favorites" class="nav-link">我的收藏</router-link>
          <router-link to="/my-recipes" class="nav-link">我的菜谱</router-link>
        </nav>

        <router-link to="/profile" class="avatar-link" title="个人中心">
          <img src="https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop" alt="用户头像" class="user-avatar" />
        </router-link>
        
        <router-link to="/create" class="create-btn">
          <span>+ 发布菜谱</span>
        </router-link>
      </div>
    </div>
  </header>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-color);
  height: 70px;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 70px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
  
  .logo-icon {
    font-size: 32px;
  }
}

.nav {
  display: flex;
  gap: 32px;
}

.nav-link {
  position: relative;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: color 0.2s ease;
  
  &:hover, &.router-link-active {
    color: var(--primary-color);
  }
  
  &.router-link-active::after {
    content: '';
    position: absolute;
    bottom: -4px;
    left: 0;
    right: 0;
    height: 2px;
    background: var(--primary-color);
    border-radius: 1px;
  }
}

.avatar-link {
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover {
    .user-avatar {
      border-color: var(--primary-color);
      transform: scale(1.05);
    }
  }
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid transparent;
  transition: all 0.2s ease;
}

.create-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  border-radius: 25px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(230, 126, 34, 0.2);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(230, 126, 34, 0.3);
  }
}

@media (max-width: 768px) {
  .nav {
    display: none;
  }
}
</style>