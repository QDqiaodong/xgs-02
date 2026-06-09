<template>
  <div class="create-recipe">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">{{ isEdit ? '编辑菜谱' : '发布新菜谱' }}</h1>
        <p class="page-desc">记录你的独家配方，与大家分享</p>
      </div>

      <form @submit.prevent="handleSubmit" class="recipe-form">
        <div class="form-section">
          <h2 class="section-title">📝 基本信息</h2>
          
          <div class="form-row">
            <div class="form-group">
              <label>菜谱标题 *</label>
              <input 
                type="text" 
                v-model="form.title" 
                placeholder="给你的菜谱起个名字"
                required
              />
            </div>
            <div class="form-group">
              <label>烹饪时间(分钟) *</label>
              <input 
                type="number" 
                v-model="form.cookTime" 
                placeholder="如：30"
                required
              />
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>制作难度 *</label>
              <select v-model="form.difficulty" required>
                <option value="1">简单</option>
                <option value="2">中等</option>
                <option value="3">困难</option>
              </select>
            </div>
            <div class="form-group">
              <label>作者名称</label>
              <input type="text" v-model="form.author" placeholder="你的昵称" />
            </div>
          </div>
          
          <div class="form-group">
            <label>菜谱描述 *</label>
            <textarea 
              v-model="form.description" 
              placeholder="简单介绍一下这道菜..."
              rows="3"
              required
            ></textarea>
          </div>
          
          <div class="form-group">
            <label>菜系标签</label>
            <div class="tags-selector">
              <span 
                v-for="tag in cuisineTags" 
                :key="tag"
                class="tag-select"
                :class="{ selected: form.tags.includes(tag) }"
                @click="toggleTag(tag)"
              >{{ tag }}</span>
            </div>
          </div>
          
          <div class="form-group">
            <label>封面图片</label>
            <div class="image-upload" @click="triggerUpload">
              <input 
                type="file" 
                ref="fileInput" 
                accept="image/*" 
                style="display: none"
                @change="handleImageUpload"
              />
              <div v-if="form.coverImage" class="image-preview">
                <img :src="form.coverImage" alt="封面" />
                <button type="button" class="remove-image" @click.stop="removeImage">×</button>
              </div>
              <div v-else class="upload-placeholder">
                <span class="upload-icon">📷</span>
                <span>点击上传封面图片</span>
              </div>
            </div>
          </div>
        </div>

        <div class="form-section">
          <IngredientsTable v-model="form.ingredients" />
        </div>

        <div class="form-section">
          <h2 class="section-title">👨‍🍳 烹饪步骤</h2>
          <div class="steps-editor">
            <div v-for="(step, index) in form.steps" :key="step._id" class="step-editor">
              <div class="step-header">
                <span class="step-number">步骤 {{ index + 1 }}</span>
                <button type="button" class="delete-step" @click="removeStep(index)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </div>
              <div class="step-image-upload" @click="triggerStepImageUpload(index)">
                <input
                  type="file"
                  :ref="el => setStepFileInput(el, index)"
                  accept="image/*"
                  style="display: none"
                  @change="(e) => handleStepImageUpload(e, index)"
                />
                <div v-if="step.image" class="step-image-preview">
                  <img :src="step.image" :alt="'步骤' + (index + 1)" />
                  <button type="button" class="remove-step-image" @click.stop="removeStepImage(index)">×</button>
                </div>
                <div v-else class="step-upload-placeholder">
                  <span class="upload-icon">📷</span>
                  <span>点击上传步骤图片</span>
                </div>
              </div>
              <textarea
                v-model="step.content"
                placeholder="描述这个步骤..."
                rows="3"
              ></textarea>
            </div>
            <button type="button" class="add-step-btn" @click="addStep">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              添加步骤
            </button>
          </div>
        </div>

        <div class="form-section">
          <h2 class="section-title">💡 小贴士</h2>
          <textarea 
            v-model="form.tips" 
            placeholder="分享一些烹饪小技巧..."
            rows="4"
          ></textarea>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-outline" @click="saveDraft">保存草稿</button>
          <button type="submit" class="btn btn-primary">发布菜谱</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import IngredientsTable from '@/components/IngredientsTable.vue'

const route = useRoute()
const router = useRouter()

const isEdit = ref(!!route.params.id)
const fileInput = ref(null)
const stepFileInputs = ref({})

let stepIdCounter = Date.now()
const genStepId = () => `step_${++stepIdCounter}`

const createStep = (content = '', image = '') => ({
  _id: genStepId(),
  content,
  image
})

const form = reactive({
  title: '',
  description: '',
  cookTime: '',
  difficulty: '2',
  author: '',
  tags: [],
  coverImage: '',
  ingredients: [],
  steps: [createStep()],
  tips: ''
})

const cuisineTags = ['川菜', '粤菜', '湘菜', '鲁菜', '苏菜', '浙菜', '闽菜', '徽菜', '家常菜', '快手菜', '甜点', '汤羹', '凉菜']

onMounted(() => {
  if (isEdit.value) {
    loadRecipe()
  }
})

const loadRecipe = () => {
  const mockData = {
    title: '红烧五花肉',
    description: '经典家常菜，肥而不腻，入口即化',
    cookTime: 60,
    difficulty: '2',
    author: '美食达人',
    tags: ['川菜', '家常菜'],
    coverImage: 'https://images.unsplash.com/photo-1623689046286-01d812ba6d10?w=800&h=500&fit=crop',
    ingredients: [
      { name: '五花肉', amount: '500g', note: '选三层肉最佳' },
      { name: '生抽', amount: '2勺', note: '' }
    ],
    steps: [
      createStep('五花肉切块，冷水下锅焯水', ''),
      createStep('锅中放少许油，加入冰糖小火炒出糖色', '')
    ],
    tips: '炒糖色要用小火'
  }
  Object.assign(form, mockData)
}

const toggleTag = (tag) => {
  const idx = form.tags.indexOf(tag)
  if (idx > -1) {
    form.tags.splice(idx, 1)
  } else {
    form.tags.push(tag)
  }
}

const addStep = () => {
  form.steps.push(createStep())
}

const removeStep = (index) => {
  if (form.steps.length > 1) {
    form.steps.splice(index, 1)
  } else {
    ElMessage.warning('至少保留一个步骤')
  }
}

const triggerUpload = () => {
  fileInput.value.click()
}

const handleImageUpload = (e) => {
  const file = e.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      form.coverImage = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

const removeImage = () => {
  form.coverImage = ''
}

const setStepFileInput = (el, index) => {
  if (el) {
    stepFileInputs.value[index] = el
  }
}

const triggerStepImageUpload = (index) => {
  const input = stepFileInputs.value[index]
  if (input) {
    input.click()
  }
}

const handleStepImageUpload = (e, index) => {
  const file = e.target.files[0]
  if (file && form.steps[index]) {
    const reader = new FileReader()
    reader.onload = (ev) => {
      form.steps[index].image = ev.target.result
    }
    reader.readAsDataURL(file)
  }
  e.target.value = ''
}

const removeStepImage = (index) => {
  if (form.steps[index]) {
    form.steps[index].image = ''
  }
}

const saveDraft = () => {
  ElMessage.success('草稿已保存')
}

const prepareStepsForSubmit = () => {
  return form.steps.map(step => ({
    content: step.content,
    image: step.image || ''
  }))
}

const handleSubmit = () => {
  if (!form.title || !form.description || !form.cookTime) {
    ElMessage.error('请填写必填项')
    return
  }
  
  if (form.ingredients.length === 0) {
    ElMessage.error('请添加至少一种食材')
    return
  }

  const stepsData = prepareStepsForSubmit()
  console.log('提交的步骤数据:', stepsData)
  
  ElMessage.success(isEdit.value ? '菜谱已更新' : '菜谱发布成功')
  setTimeout(() => {
    router.push('/my-recipes')
  }, 1000)
}
</script>

<style lang="scss" scoped>
.create-recipe {
  padding: 40px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 32px;
  margin-bottom: 8px;
}

.page-desc {
  color: var(--text-secondary);
}

.recipe-form {
  max-width: 900px;
  margin: 0 auto;
}

.form-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
  
  label {
    display: block;
    font-weight: 500;
    margin-bottom: 8px;
    font-size: 14px;
  }
  
  input, select, textarea {
    width: 100%;
    padding: 12px 16px;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-family: inherit;
    transition: border-color 0.2s ease;
    
    &:focus {
      outline: none;
      border-color: var(--primary-color);
    }
  }
  
  textarea {
    resize: vertical;
  }
}

.tags-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-select {
  padding: 6px 14px;
  background: var(--bg-tertiary);
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  
  &.selected {
    background: var(--primary-color);
    color: white;
  }
}

.image-upload {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-md);
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: var(--primary-color);
  }
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  
  .upload-icon {
    font-size: 48px;
  }
}

.image-preview {
  position: relative;
  display: inline-block;
  
  img {
    max-width: 300px;
    border-radius: var(--radius-sm);
  }
}

.remove-image {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #ef4444;
  color: white;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.steps-editor {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.step-editor {
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  padding: 20px;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.step-number {
  font-weight: 600;
  color: var(--primary-color);
}

.delete-step {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #fef2f2;
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  
  svg {
    width: 18px;
    height: 18px;
  }
}

.step-editor textarea {
  background: white;
}

.step-image-upload {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-sm);
  padding: 20px;
  text-align: center;
  cursor: pointer;
  margin-bottom: 12px;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--primary-color);
  }
}

.step-upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);

  .upload-icon {
    font-size: 32px;
  }
}

.step-image-preview {
  position: relative;
  display: inline-block;

  img {
    max-width: 240px;
    max-height: 180px;
    border-radius: var(--radius-sm);
    object-fit: cover;
  }
}

.remove-step-image {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ef4444;
  color: white;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
}

.add-step-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  
  &:hover {
    border-color: var(--primary-color);
    color: var(--primary-color);
  }
  
  svg {
    width: 20px;
    height: 20px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 16px 0;
}
</style>