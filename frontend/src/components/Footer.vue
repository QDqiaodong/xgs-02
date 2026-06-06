<template>
  <footer class="footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-brand">
          <span class="logo-icon">🍳</span>
          <span class="logo-text">家常食谱</span>
          <p class="slogan">分享美食，传递温暖</p>
        </div>
        
        <div class="footer-links">
          <div class="link-group">
            <h4>关于我们</h4>
            <a href="#">平台介绍</a>
            <a href="#">使用帮助</a>
            <a href="#">联系我们</a>
          </div>
          
          <div class="link-group">
            <h4>菜谱分类</h4>
            <a href="#">川菜</a>
            <a href="#">粤菜</a>
            <a href="#">家常菜</a>
          </div>
          
          <div class="link-group">
            <h4>服务条款</h4>
            <a href="#">用户协议</a>
            <a href="#">隐私政策</a>
            <a href="#">版权声明</a>
          </div>
        </div>
      </div>
      
      <div class="footer-bottom">
        <p>© 2024 家常食谱 · 用心做好每一道菜</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.footer {
  background: #2c3e50;
  color: #ecf0f1;
  padding: 60px 0 24px;
  margin-top: 80px;
}

.footer-content {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 60px;
  margin-bottom: 40px;
}

.footer-brand {
  .logo-icon {
    font-size: 40px;
    display: block;
    margin-bottom: 8px;
  }
  
  .logo-text {
    font-family: var(--font-serif);
    font-size: 28px;
    font-weight: 700;
    color: var(--primary-light);
  }
  
  .slogan {
    margin-top: 12px;
    color: #95a5a6;
    font-size: 14px;
  }
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
}

.link-group {
  h4 {
    color: white;
    font-size: 16px;
    margin-bottom: 16px;
  }
  
  a {
    display: block;
    color: #95a5a6;
    font-size: 14px;
    margin-bottom: 10px;
    transition: color 0.2s ease;
    
    &:hover {
      color: var(--primary-light);
    }
  }
}

.footer-bottom {
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  text-align: center;
  color: #7f8c8d;
  font-size: 13px;
}

@media (max-width: 768px) {
  .footer-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  
  .footer-links {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>