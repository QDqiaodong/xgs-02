<template>
  <div class="step-timer">
    <button class="timer-toggle-btn" @click="toggleExpand" :class="{ active: expanded, running: state.isRunning }">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <polyline points="12 6 12 12 16 14"/>
      </svg>
      <span v-if="state.isRunning && state.remaining > 0" class="timer-badge">{{ formatTime(state.remaining) }}</span>
    </button>

    <Transition name="timer-slide">
      <div v-if="expanded" class="timer-panel">
        <div class="timer-display">
          <div class="time-text" :class="{ finished: state.remaining <= 0, running: state.isRunning }">
            {{ formatTime(displayRemaining) }}
          </div>
          <div class="time-progress-ring">
            <svg viewBox="0 0 100 100">
              <circle class="progress-bg" cx="50" cy="50" r="45" fill="none"/>
              <circle 
                class="progress-bar" 
                cx="50" cy="50" r="45" fill="none"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="progressOffset"
              />
            </svg>
          </div>
        </div>

        <div class="timer-duration" v-if="!state.isRunning">
          <span class="duration-label">设置时长：</span>
          <div class="duration-buttons">
            <button 
              v-for="min in durationOptions" 
              :key="min"
              class="duration-btn"
              :class="{ active: state.duration === min * 60 }"
              @click="setDuration(min)"
            >
              {{ min }}分
            </button>
          </div>
          <div class="duration-custom">
            <button class="dur-adjust-btn" @click="adjustDuration(-30)">-30秒</button>
            <span class="dur-custom-value">{{ Math.floor(state.duration / 60) }}分{{ state.duration % 60 }}秒</span>
            <button class="dur-adjust-btn" @click="adjustDuration(30)">+30秒</button>
          </div>
        </div>

        <div class="timer-controls">
          <button class="ctrl-btn reset-btn" @click="resetTimer" :disabled="state.remaining === state.duration && !state.isRunning">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="1 4 1 10 7 10"/>
              <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
            </svg>
            <span>重置</span>
          </button>
          <button 
            class="ctrl-btn main-btn" 
            :class="state.isRunning ? 'pause-btn' : 'start-btn'"
            @click="state.isRunning ? pauseTimer() : startTimer()"
          >
            <svg v-if="!state.isRunning" viewBox="0 0 24 24" fill="currentColor">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="4" width="4" height="16"/>
              <rect x="14" y="4" width="4" height="16"/>
            </svg>
            <span>{{ state.isRunning ? '暂停' : (state.remaining === state.duration ? '开始' : '继续') }}</span>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRecipeStore } from '@/store/recipe'

const props = defineProps({
  recipeId: {
    type: [String, Number],
    required: true
  },
  stepIndex: {
    type: Number,
    required: true
  },
  defaultMinutes: {
    type: Number,
    default: 5
  }
})

const store = useRecipeStore()
const expanded = ref(false)
const displayRemaining = ref(0)
let tickInterval = null
let audioCtx = null

const durationOptions = [1, 3, 5, 10, 15, 30]

const defaultDuration = props.defaultMinutes * 60

const state = ref({
  duration: defaultDuration,
  remaining: defaultDuration,
  isRunning: false,
  startTime: null,
  elapsedBeforePause: 0
})

const circumference = 2 * Math.PI * 45

const progressOffset = computed(() => {
  if (state.value.duration === 0) return circumference
  const progress = state.value.remaining / state.value.duration
  return circumference * (1 - progress)
})

const loadState = () => {
  const saved = store.getTimerState(props.recipeId, props.stepIndex)
  if (saved.duration !== 60 || saved.remaining !== 60 || saved.isRunning || saved.elapsedBeforePause > 0) {
    state.value = { ...saved }
  } else {
    state.value = {
      duration: defaultDuration,
      remaining: defaultDuration,
      isRunning: false,
      startTime: null,
      elapsedBeforePause: 0
    }
  }
  displayRemaining.value = state.value.remaining
}

const saveState = () => {
  store.setTimerState(props.recipeId, props.stepIndex, state.value)
}

const formatTime = (seconds) => {
  if (seconds < 0) seconds = 0
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const toggleExpand = () => {
  expanded.value = !expanded.value
}

const setDuration = (minutes) => {
  state.value.duration = minutes * 60
  state.value.remaining = minutes * 60
  displayRemaining.value = state.value.remaining
  saveState()
}

const adjustDuration = (seconds) => {
  let newDuration = state.value.duration + seconds
  if (newDuration < 10) newDuration = 10
  if (newDuration > 3600) newDuration = 3600
  state.value.duration = newDuration
  state.value.remaining = newDuration
  displayRemaining.value = state.value.remaining
  saveState()
}

const startTimer = () => {
  if (state.value.remaining <= 0) {
    state.value.remaining = state.value.duration
  }
  state.value.isRunning = true
  state.value.startTime = Date.now()
  saveState()
  startTick()
}

const pauseTimer = () => {
  stopTick()
  const elapsedNow = state.value.startTime ? Math.floor((Date.now() - state.value.startTime) / 1000) : 0
  state.value.elapsedBeforePause = state.value.elapsedBeforePause + elapsedNow
  state.value.isRunning = false
  state.value.startTime = null
  displayRemaining.value = state.value.remaining
  saveState()
}

const resetTimer = () => {
  stopTick()
  state.value.remaining = state.value.duration
  state.value.isRunning = false
  state.value.startTime = null
  state.value.elapsedBeforePause = 0
  displayRemaining.value = state.value.remaining
  store.resetTimerState(props.recipeId, props.stepIndex)
}

const startTick = () => {
  if (tickInterval) clearInterval(tickInterval)
  tickInterval = setInterval(() => {
    updateRemaining()
  }, 200)
}

const stopTick = () => {
  if (tickInterval) {
    clearInterval(tickInterval)
    tickInterval = null
  }
}

const updateRemaining = () => {
  if (!state.value.isRunning || !state.value.startTime) return

  const elapsedNow = Math.floor((Date.now() - state.value.startTime) / 1000)
  const totalElapsed = state.value.elapsedBeforePause + elapsedNow
  const remaining = state.value.duration - totalElapsed

  if (remaining <= 0) {
    displayRemaining.value = 0
    state.value.remaining = 0
    state.value.isRunning = false
    state.value.startTime = null
    state.value.elapsedBeforePause = state.value.duration
    stopTick()
    saveState()
    playAlarm()
    return
  }

  displayRemaining.value = remaining
  if (Math.abs(remaining - state.value.remaining) >= 1) {
    state.value.remaining = remaining
    saveState()
  }
}

const playAlarm = () => {
  try {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    }
    
    ElMessage.success(`步骤 ${props.stepIndex + 1} 计时结束！`)

    const playBeep = (frequency, startTime, duration) => {
      const oscillator = audioCtx.createOscillator()
      const gainNode = audioCtx.createGain()
      
      oscillator.connect(gainNode)
      gainNode.connect(audioCtx.destination)
      
      oscillator.type = 'sine'
      oscillator.frequency.value = frequency
      
      gainNode.gain.setValueAtTime(0, startTime)
      gainNode.gain.linearRampToValueAtTime(0.2, startTime + 0.02)
      gainNode.gain.linearRampToValueAtTime(0, startTime + duration)
      
      oscillator.start(startTime)
      oscillator.stop(startTime + duration)
    }

    const now = audioCtx.currentTime
    playBeep(880, now, 0.15)
    playBeep(660, now + 0.2, 0.15)
    playBeep(880, now + 0.4, 0.2)
  } catch (e) {
    console.warn('Audio playback failed', e)
    if (navigator.vibrate) {
      navigator.vibrate([200, 100, 200, 100, 300])
    }
  }
}

watch(
  () => [props.recipeId, props.stepIndex],
  () => {
    stopTick()
    loadState()
    if (state.value.isRunning) {
      updateRemaining()
      if (state.value.isRunning) startTick()
    }
  }
)

onMounted(() => {
  loadState()
  if (state.value.isRunning) {
    updateRemaining()
    if (state.value.isRunning) startTick()
  }
  
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
      updateRemaining()
      if (state.value.isRunning) startTick()
    } else {
      stopTick()
    }
  })
})

onUnmounted(() => {
  stopTick()
  if (audioCtx) {
    audioCtx.close()
  }
})
</script>

<style lang="scss" scoped>
.step-timer {
  position: relative;
  display: inline-block;
}

.timer-toggle-btn {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;

  svg {
    width: 20px;
    height: 20px;
    transition: transform 0.25s ease;
  }

  &:hover {
    background: var(--primary-color);
    color: white;

    svg {
      transform: rotate(20deg);
    }
  }

  &.active {
    background: var(--primary-color);
    color: white;
  }

  &.running {
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    color: white;
    animation: pulse-glow 2s ease-in-out infinite;
  }
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(230, 126, 34, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(230, 126, 34, 0);
  }
}

.timer-badge {
  position: absolute;
  bottom: -6px;
  right: -6px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 5px;
  border-radius: 10px;
  min-width: 36px;
  text-align: center;
  line-height: 1.2;
  font-family: 'SF Mono', 'Menlo', monospace;
}

.timer-panel {
  margin-top: 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-md);
}

.timer-slide-enter-active,
.timer-slide-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.timer-slide-enter-from,
.timer-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
  max-height: 0;
  margin-top: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.timer-slide-enter-to,
.timer-slide-leave-from {
  max-height: 400px;
}

.timer-display {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.time-text {
  font-size: 42px;
  font-weight: 700;
  font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
  color: var(--text-primary);
  z-index: 1;
  letter-spacing: 2px;
  transition: all 0.2s ease;

  &.running {
    color: var(--primary-color);
  }

  &.finished {
    color: #ef4444;
    animation: flash 1s ease-in-out 3;
  }
}

@keyframes flash {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.time-progress-ring {
  position: absolute;
  width: 120px;
  height: 120px;

  svg {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }

  .progress-bg {
    stroke: var(--bg-tertiary);
    stroke-width: 6;
  }

  .progress-bar {
    stroke: url(#timerGradient);
    stroke: var(--primary-color);
    stroke-width: 6;
    stroke-linecap: round;
    transition: stroke-dashoffset 0.3s ease;
  }
}

.timer-duration {
  margin-bottom: 18px;
}

.duration-label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.duration-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.duration-btn {
  padding: 8px 14px;
  border-radius: 20px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  min-height: 36px;

  &:hover {
    background: var(--primary-light);
    color: white;
  }

  &.active {
    background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
    color: white;
  }
}

.duration-custom {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.dur-adjust-btn {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 12px;
  min-height: 34px;
  transition: all 0.2s ease;

  &:hover {
    background: var(--primary-color);
    color: white;
  }
}

.dur-custom-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  min-width: 70px;
  text-align: center;
  font-family: 'SF Mono', 'Menlo', monospace;
}

.timer-controls {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.ctrl-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  min-height: 44px;

  svg {
    width: 18px;
    height: 18px;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.reset-btn {
  background: var(--bg-tertiary);
  color: var(--text-secondary);

  &:hover:not(:disabled) {
    background: #ecf0f1;
    color: var(--text-primary);
  }
}

.main-btn {
  padding: 12px 28px;
}

.start-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(230, 126, 34, 0.4);
  }
}

.pause-btn {
  background: linear-gradient(135deg, #f39c12, #e67e22);
  color: white;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(243, 156, 18, 0.4);
  }
}

@media (max-width: 768px) {
  .timer-toggle-btn {
    width: 44px;
    height: 44px;

    svg {
      width: 22px;
      height: 22px;
    }
  }

  .timer-panel {
    padding: 16px;
  }

  .time-text {
    font-size: 38px;
  }

  .time-progress-ring {
    width: 100px;
    height: 100px;
  }

  .duration-buttons {
    gap: 6px;
  }

  .duration-btn {
    padding: 10px 14px;
    font-size: 14px;
    min-height: 42px;
    flex: 1;
    min-width: 0;
  }

  .dur-adjust-btn {
    padding: 10px 14px;
    font-size: 13px;
    min-height: 42px;
  }

  .ctrl-btn {
    flex: 1;
    padding: 14px 16px;
    font-size: 15px;
    min-height: 50px;
    justify-content: center;
  }
}
</style>
