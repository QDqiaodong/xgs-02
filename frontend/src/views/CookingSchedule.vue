<template>
  <div class="cooking-schedule-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">🍳 烹饪排程</h1>
        <p class="page-desc">选择菜谱和开饭时间，自动生成倒排时间轴，让烹饪更有条理</p>
      </div>

      <div class="setup-section section-card">
        <h2 class="section-title">设置排程</h2>
        <div class="setup-form">
          <div class="form-group recipe-select-group">
            <label class="form-label">选择菜谱</label>
            <div class="recipe-selector" @click="showRecipePicker = true">
              <div v-if="selectedRecipe" class="selected-recipe">
                <img :src="selectedRecipe.coverImage || defaultImage" :alt="selectedRecipe.title" class="recipe-thumb" />
                <div class="recipe-info">
                  <div class="recipe-name">{{ selectedRecipe.title }}</div>
                  <div class="recipe-meta">
                    <span>⏱️ {{ selectedRecipe.cookTime }}分钟</span>
                    <span>📊 {{ getDifficultyLabel(selectedRecipe.difficulty) }}</span>
                  </div>
                </div>
                <span class="change-btn">更换</span>
              </div>
              <div v-else class="select-placeholder">
                <span class="placeholder-icon">🍲</span>
                <span>点击选择菜谱</span>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">开饭时间</label>
            <el-date-picker
              v-model="mealTime"
              type="datetime"
              placeholder="选择开饭时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledDate"
              class="time-picker"
            />
          </div>

          <div class="form-group reminder-group">
            <label class="form-label">
              <span>临近提醒</span>
              <el-switch v-model="enableReminders" size="small" />
            </label>
            <div v-if="enableReminders" class="reminder-options">
              <span class="reminder-hint">开启后，每步完成前1分钟提醒</span>
            </div>
          </div>

          <button class="btn btn-primary generate-btn" :disabled="!canGenerate" @click="generateSchedule">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
            </svg>
            生成排程
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-section section-card">
        <div class="spinner"></div>
        <span>正在生成烹饪排程...</span>
      </div>

      <div v-else-if="schedule" class="schedule-section">
        <div class="schedule-header section-card">
          <div class="schedule-info">
            <h2 class="schedule-title">{{ schedule.recipeTitle }}</h2>
            <div class="schedule-meta">
              <div class="meta-item">
                <span class="meta-icon">🍽️</span>
                <span>开饭时间：{{ formatTime(schedule.mealTime) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-icon">⏰</span>
                <span>开始时间：{{ formatTime(schedule.startTime) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-icon">⏱️</span>
                <span>总时长：{{ schedule.totalDuration }}分钟</span>
              </div>
            </div>
          </div>
          <div class="schedule-actions">
            <button class="btn btn-outline" @click="startSchedule">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <polygon points="5 3 19 12 5 21 5 3"/>
              </svg>
              开始执行
            </button>
            <button class="btn btn-outline" @click="resetSchedule">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="1 4 1 10 7 10"/>
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
              </svg>
              重新设置
            </button>
          </div>
        </div>

        <div class="timeline-section section-card">
          <h3 class="section-title">
            <span>时间轴</span>
            <span class="timeline-legend">
              <span class="legend-item prep"><i></i>备菜</span>
              <span class="legend-item heating"><i></i>加热</span>
              <span class="legend-item waiting"><i></i>等待</span>
              <span class="legend-item sync"><i></i>同步</span>
            </span>
          </h3>
          <div class="timeline">
            <div
              v-for="(step, index) in schedule.steps"
              :key="index"
              class="timeline-item"
              :class="[step.stepType, { active: currentStepIndex === index, done: currentStepIndex > index }]"
            >
              <div class="timeline-marker">
                <div class="marker-dot"></div>
                <div class="marker-line" v-if="index < schedule.steps.length - 1"></div>
              </div>
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="step-badge">步骤{{ step.stepIndex + 1 }}</span>
                  <span class="step-type-tag">{{ getStepTypeLabel(step.stepType) }}</span>
                  <span class="step-time">{{ formatTime(step.startTime) }} - {{ formatTime(step.endTime) }}</span>
                </div>
                <div class="step-duration">
                  <span class="duration-icon">⏱️</span>
                  <span>{{ step.duration }}分钟</span>
                  <span v-if="step.isParallel" class="parallel-tag">可同步进行</span>
                </div>
                <p class="step-content">{{ step.content }}</p>
                <div v-if="step.description" class="step-desc">{{ step.description }}</div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="schedule.reminders && schedule.reminders.length > 0" class="reminders-section section-card">
          <h3 class="section-title">🔔 提醒设置</h3>
          <div class="reminders-list">
            <div
              v-for="reminder in schedule.reminders"
              :key="reminder.id"
              class="reminder-item"
              :class="reminder.type"
            >
              <div class="reminder-icon">
                <span v-if="reminder.type === 'start'">🚀</span>
                <span v-else-if="reminder.type === 'step'">⏰</span>
                <span v-else-if="reminder.type === 'meal'">🍽️</span>
              </div>
              <div class="reminder-info">
                <div class="reminder-title">{{ reminder.title }}</div>
                <div class="reminder-content">{{ reminder.content }}</div>
                <div class="reminder-time">{{ formatTime(reminder.remindTime) }} · 提前{{ reminder.advanceMinutes }}分钟</div>
              </div>
              <div class="reminder-status">
                <el-switch v-model="reminder.enabled" size="small" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-section section-card">
        <div class="empty-icon">📋</div>
        <h3>还没有排程</h3>
        <p>选择菜谱和开饭时间，生成你的专属烹饪排程</p>
      </div>
    </div>

    <div v-if="showRecipePicker" class="recipe-picker-overlay" @click.self="showRecipePicker = false">
      <div class="recipe-picker-modal">
        <div class="picker-header">
          <h3>选择菜谱</h3>
          <button class="close-btn" @click="showRecipePicker = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="picker-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索菜谱..."
            prefix-icon="Search"
            clearable
            @input="searchRecipes"
          />
        </div>
        <div class="picker-list">
          <div v-if="loadingRecipes" class="picker-loading">
            <div class="spinner spinner-sm"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="recipeList.length === 0" class="picker-empty">
            <span>暂无菜谱</span>
          </div>
          <div
            v-for="recipe in recipeList"
            :key="recipe.id"
            class="picker-item"
            :class="{ selected: selectedRecipe?.id === recipe.id }"
            @click="selectRecipe(recipe)"
          >
            <img :src="recipe.coverImage || defaultImage" :alt="recipe.title" class="picker-item-img" />
            <div class="picker-item-info">
              <div class="picker-item-title">{{ recipe.title }}</div>
              <div class="picker-item-meta">
                <span>⏱️ {{ recipe.cookTime }}分钟</span>
                <span>📊 {{ getDifficultyLabel(recipe.difficulty) }}</span>
              </div>
            </div>
            <div v-if="selectedRecipe?.id === recipe.id" class="picker-check">✓</div>
          </div>
        </div>
        <div class="picker-footer">
          <button class="btn btn-outline" @click="showRecipePicker = false">取消</button>
          <button class="btn btn-primary" :disabled="!selectedRecipe" @click="confirmSelect">确定选择</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { recipeApi, cookingScheduleApi } from '@/utils/api'

const defaultImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&h=500&fit=crop'

const selectedRecipe = ref(null)
const mealTime = ref('')
const enableReminders = ref(true)
const showRecipePicker = ref(false)
const searchKeyword = ref('')
const recipeList = ref([])
const loadingRecipes = ref(false)
const loading = ref(false)
const schedule = ref(null)
const currentStepIndex = ref(-1)
const timerInterval = ref(null)
let audioCtx = null

const canGenerate = computed(() => {
  return selectedRecipe.value && mealTime.value
})

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

const getDifficultyLabel = (level) => {
  const labels = { 1: '简单', 2: '中等', 3: '困难' }
  return labels[level] || '中等'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const getStepTypeLabel = (type) => {
  const labels = {
    prep: '备菜',
    heating: '加热',
    waiting: '等待',
    sync: '同步'
  }
  return labels[type] || '其他'
}

const loadRecipes = async () => {
  loadingRecipes.value = true
  try {
    const result = await recipeApi.getRecipes({ page: 1, size: 20 })
    if (result && result.data && result.data.records) {
      recipeList.value = result.data.records
    }
  } catch (err) {
    console.error('加载菜谱列表失败', err)
    ElMessage.error('加载菜谱列表失败')
  } finally {
    loadingRecipes.value = false
  }
}

const searchRecipes = async () => {
  if (!searchKeyword.value.trim()) {
    loadRecipes()
    return
  }
  loadingRecipes.value = true
  try {
    const result = await recipeApi.getRecipes({ page: 1, size: 20, keyword: searchKeyword.value })
    if (result && result.data && result.data.records) {
      recipeList.value = result.data.records
    }
  } catch (err) {
    console.error('搜索菜谱失败', err)
  } finally {
    loadingRecipes.value = false
  }
}

const selectRecipe = (recipe) => {
  selectedRecipe.value = recipe
}

const confirmSelect = () => {
  if (selectedRecipe.value) {
    showRecipePicker.value = false
  }
}

const generateSchedule = async () => {
  if (!canGenerate.value) return

  loading.value = true
  try {
    const result = await cookingScheduleApi.generateSchedule(selectedRecipe.value.id, mealTime.value)
    if (result && result.data) {
      schedule.value = result.data
      if (schedule.value.reminders) {
        schedule.value.reminders.forEach(r => {
          r.enabled = true
        })
      }
      ElMessage.success('排程生成成功！')
    }
  } catch (err) {
    console.error('生成排程失败', err)
    ElMessage.error('生成排程失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const startSchedule = () => {
  if (!schedule.value || !schedule.value.steps || schedule.value.steps.length === 0) return

  currentStepIndex.value = 0
  startTimer()
  ElMessage.success('排程已开始，将按步骤提醒您')
}

const startTimer = () => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
  }

  timerInterval.value = setInterval(() => {
    checkReminders()
    checkStepProgress()
  }, 1000)
}

const checkReminders = () => {
  if (!schedule.value || !schedule.value.reminders || !enableReminders.value) return

  const now = new Date().getTime()

  for (const reminder of schedule.value.reminders) {
    if (!reminder.enabled || reminder.notified) continue

    const remindTime = new Date(reminder.remindTime).getTime()
    const diff = remindTime - now

    if (diff <= 0 && diff > -60000) {
      reminder.notified = true
      showReminderNotification(reminder)
    }
  }
}

const checkStepProgress = () => {
  if (!schedule.value || !schedule.value.steps) return

  const now = new Date().getTime()

  for (let i = 0; i < schedule.value.steps.length; i++) {
    const step = schedule.value.steps[i]
    const endTime = new Date(step.endTime).getTime()

    if (now < endTime) {
      currentStepIndex.value = i
      return
    }
  }

  currentStepIndex.value = schedule.value.steps.length
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
    timerInterval.value = null
  }
}

const showReminderNotification = (reminder) => {
  playBeep()
  ElMessage({
    message: `${reminder.title}：${reminder.content}`,
    type: 'warning',
    duration: 5000,
    showClose: true
  })

  if (Notification.permission === 'granted') {
    new Notification(reminder.title, {
      body: reminder.content,
      icon: '🍳'
    })
  }
}

const playBeep = () => {
  try {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    }

    const playTone = (freq, startTime, duration) => {
      const osc = audioCtx.createOscillator()
      const gain = audioCtx.createGain()
      osc.connect(gain)
      gain.connect(audioCtx.destination)
      osc.type = 'sine'
      osc.frequency.value = freq
      gain.gain.setValueAtTime(0, startTime)
      gain.gain.linearRampToValueAtTime(0.3, startTime + 0.05)
      gain.gain.linearRampToValueAtTime(0, startTime + duration)
      osc.start(startTime)
      osc.stop(startTime + duration)
    }

    const now = audioCtx.currentTime
    playTone(880, now, 0.2)
    playTone(660, now + 0.25, 0.2)
    playTone(880, now + 0.5, 0.3)
  } catch (e) {
    console.warn('音频播放失败', e)
    if (navigator.vibrate) {
      navigator.vibrate([200, 100, 200, 100, 300])
    }
  }
}

const resetSchedule = () => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
    timerInterval.value = null
  }
  schedule.value = null
  currentStepIndex.value = -1
}

const requestNotificationPermission = () => {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
}

onMounted(() => {
  loadRecipes()
  requestNotificationPermission()

  const now = new Date()
  now.setMinutes(now.getMinutes() + 30)
  mealTime.value = formatDateTime(now)
})

const formatDateTime = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

onUnmounted(() => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
  }
  if (audioCtx) {
    audioCtx.close()
  }
})
</script>

<style lang="scss" scoped>
.cooking-schedule-page {
  padding: 40px 0 60px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.page-desc {
  color: var(--text-secondary);
  font-size: 15px;
}

.section-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 28px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--text-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.setup-form {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 20px;
  align-items: end;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recipe-selector {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 80px;
  display: flex;
  align-items: center;

  &:hover {
    border-color: var(--primary-color);
    background: var(--bg-tertiary);
  }
}

.selected-recipe {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.recipe-thumb {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.recipe-info {
  flex: 1;
  min-width: 0;
}

.recipe-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recipe-meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  gap: 12px;
}

.change-btn {
  font-size: 13px;
  color: var(--primary-color);
  font-weight: 500;
  flex-shrink: 0;
}

.select-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 8px;
  color: var(--text-light);
}

.placeholder-icon {
  font-size: 28px;
}

.time-picker {
  width: 100%;
}

.reminder-group {
  .form-label {
    margin-bottom: 8px;
  }
}

.reminder-options {
  .reminder-hint {
    font-size: 12px;
    color: var(--text-light);
  }
}

.generate-btn {
  height: 48px;
  justify-content: center;
  font-size: 15px;

  svg {
    width: 20px;
    height: 20px;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.loading-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px;
  color: var(--text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.spinner-sm {
  width: 24px;
  height: 24px;
  border-width: 2px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  flex-wrap: wrap;
}

.schedule-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.schedule-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);

  .meta-icon {
    font-size: 16px;
  }
}

.schedule-actions {
  display: flex;
  gap: 12px;
}

.btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;

  svg {
    width: 18px;
    height: 18px;
  }
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  border: none;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(230, 126, 34, 0.4);
  }
}

.btn-outline {
  background: white;
  color: var(--text-primary);
  border: 1px solid var(--border-color);

  &:hover {
    border-color: var(--primary-color);
    color: var(--primary-color);
  }
}

.timeline-legend {
  display: flex;
  gap: 16px;
  font-size: 13px;
  font-weight: normal;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);

  i {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    display: inline-block;
  }

  &.prep i { background: #3498db; }
  &.heating i { background: #e74c3c; }
  &.waiting i { background: #f39c12; }
  &.sync i { background: #27ae60; }
}

.timeline {
  position: relative;
  padding-left: 10px;
}

.timeline-item {
  position: relative;
  padding: 0 0 24px 30px;

  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-marker {
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 0;
  width: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.marker-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--border-color);
  border: 3px solid white;
  box-shadow: 0 0 0 2px var(--border-color);
  z-index: 1;
  flex-shrink: 0;
}

.marker-line {
  width: 2px;
  flex: 1;
  background: var(--border-color);
  margin-top: 4px;
}

.timeline-item.prep .marker-dot {
  background: #3498db;
  box-shadow: 0 0 0 2px #3498db;
}

.timeline-item.heating .marker-dot {
  background: #e74c3c;
  box-shadow: 0 0 0 2px #e74c3c;
}

.timeline-item.waiting .marker-dot {
  background: #f39c12;
  box-shadow: 0 0 0 2px #f39c12;
}

.timeline-item.sync .marker-dot {
  background: #27ae60;
  box-shadow: 0 0 0 2px #27ae60;
}

.timeline-item.active .marker-dot {
  animation: pulse-dot 1.5s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.timeline-item.done .marker-dot {
  opacity: 0.5;
}

.timeline-item.done .marker-line {
  opacity: 0.5;
}

.timeline-content {
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  transition: all 0.3s ease;
}

.timeline-item.active .timeline-content {
  background: linear-gradient(135deg, #fff5eb, #ffe8d0);
  border: 1px solid var(--primary-color);
}

.timeline-item.done .timeline-content {
  opacity: 0.6;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.step-badge {
  font-size: 12px;
  font-weight: 600;
  color: white;
  background: var(--primary-color);
  padding: 2px 10px;
  border-radius: 12px;
}

.step-type-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;

  .prep & { background: #d6eaf8; color: #2980b9; }
  .heating & { background: #fadbd8; color: #c0392b; }
  .waiting & { background: #fef5e7; color: #d68910; }
  .sync & { background: #d5f5e3; color: #229954; }
}

.step-time {
  font-size: 12px;
  color: var(--text-light);
  margin-left: auto;
}

.step-duration {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;

  .duration-icon {
    font-size: 14px;
  }
}

.parallel-tag {
  font-size: 11px;
  color: #27ae60;
  background: #d5f5e3;
  padding: 2px 6px;
  border-radius: 8px;
  margin-left: 8px;
}

.step-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  margin: 0;
}

.step-desc {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 6px;
}

.reminders-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reminder-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;

  &.start { border-left: 4px solid #3498db; }
  &.step { border-left: 4px solid #f39c12; }
  &.meal { border-left: 4px solid #27ae60; }
}

.reminder-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.reminder-info {
  flex: 1;
  min-width: 0;
}

.reminder-title {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.reminder-content {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.reminder-time {
  font-size: 12px;
  color: var(--text-light);
}

.empty-section {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-section h3 {
  font-size: 18px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-section p {
  color: var(--text-secondary);
  font-size: 14px;
}

.recipe-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  backdrop-filter: blur(4px);
}

.recipe-picker-modal {
  background: white;
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: modalFadeIn 0.3s ease;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
  }
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s ease;

  svg {
    width: 20px;
    height: 20px;
  }

  &:hover {
    background: var(--bg-tertiary);
    color: var(--text-primary);
  }
}

.picker-search {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
}

.picker-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.picker-loading,
.picker-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--text-secondary);
  gap: 12px;
  font-size: 14px;
}

.picker-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    background: var(--bg-tertiary);
  }

  &.selected {
    background: #fef5e7;
  }
}

.picker-item-img {
  width: 50px;
  height: 50px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.picker-item-info {
  flex: 1;
  min-width: 0;
}

.picker-item-title {
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picker-item-meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  gap: 12px;
}

.picker-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.picker-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);

  .btn {
    flex: 1;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .cooking-schedule-page {
    padding: 20px 0 40px;
  }

  .page-title {
    font-size: 24px;
  }

  .section-card {
    padding: 20px 16px;
  }

  .setup-form {
    grid-template-columns: 1fr;
  }

  .schedule-header {
    flex-direction: column;
    align-items: stretch;
  }

  .schedule-actions {
    .btn {
      flex: 1;
      justify-content: center;
    }
  }

  .timeline-legend {
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
  }

  .step-time {
    margin-left: 0;
    width: 100%;
  }

  .picker-header,
  .picker-search,
  .picker-footer {
    padding: 16px;
  }
}
</style>
