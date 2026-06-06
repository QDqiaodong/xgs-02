<template>
  <div class="markdown-editor">
    <div class="editor-toolbar">
      <div class="toolbar-group">
        <button type="button" class="tool-btn" @click="insertFormat('**', '**')" title="粗体">
          <strong>B</strong>
        </button>
        <button type="button" class="tool-btn" @click="insertFormat('*', '*')" title="斜体">
          <em>I</em>
        </button>
        <button type="button" class="tool-btn" @click="insertFormat('~~', '~~')" title="删除线">
          <s>S</s>
        </button>
      </div>
      
      <div class="toolbar-group">
        <button type="button" class="tool-btn" @click="insertHeading(1)" title="标题1">H1</button>
        <button type="button" class="tool-btn" @click="insertHeading(2)" title="标题2">H2</button>
        <button type="button" class="tool-btn" @click="insertHeading(3)" title="标题3">H3</button>
      </div>
      
      <div class="toolbar-group">
        <button type="button" class="tool-btn" @click="insertFormat('- ', '')" title="无序列表">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="9" y1="6" x2="20" y2="6"/>
            <line x1="9" y1="12" x2="20" y2="12"/>
            <line x1="9" y1="18" x2="20" y2="18"/>
            <circle cx="4" cy="6" r="1"/>
            <circle cx="4" cy="12" r="1"/>
            <circle cx="4" cy="18" r="1"/>
          </svg>
        </button>
        <button type="button" class="tool-btn" @click="insertFormat('1. ', '')" title="有序列表">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="10" y1="6" x2="21" y2="6"/>
            <line x1="10" y1="12" x2="21" y2="12"/>
            <line x1="10" y1="18" x2="21" y2="18"/>
            <path d="M4 6h1v4H3V7h1z"/>
            <path d="M3 12h2v1H4v1h1v1H3v1h2v1H3v1h2"/>
          </svg>
        </button>
      </div>
      
      <div class="toolbar-group">
        <button type="button" class="tool-btn" @click="insertFormat('> ', '')" title="引用">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </button>
        <button type="button" class="tool-btn" @click="insertFormat('[', '](url)')" title="链接">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
          </svg>
        </button>
        <button type="button" class="tool-btn" @click="insertImage" title="图片">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <circle cx="8.5" cy="8.5" r="1.5"/>
            <polyline points="21 15 16 10 5 21"/>
          </svg>
        </button>
      </div>
      
      <div class="toolbar-group">
        <button type="button" class="tool-btn" :class="{ active: showPreview }" @click="showPreview = !showPreview" title="预览">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
        </button>
      </div>
    </div>
    
    <div class="editor-container" :class="{ split: showPreview }">
      <div class="editor-pane">
        <textarea
          v-model="internalValue"
          :placeholder="placeholder"
          ref="textareaRef"
          @input="handleInput"
          @scroll="syncScroll"
        ></textarea>
      </div>
      
      <div v-if="showPreview" class="preview-pane">
        <div class="markdown-content" v-html="renderedContent" ref="previewRef"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, defineProps, defineEmits, nextTick } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '开始编写你的食谱内容...'
  }
})

const emit = defineEmits(['update:modelValue'])

const internalValue = ref(props.modelValue)
const showPreview = ref(false)
const textareaRef = ref(null)
const previewRef = ref(null)

watch(() => props.modelValue, (val) => {
  internalValue.value = val
})

const renderedContent = computed(() => {
  return marked.parse(internalValue.value || '')
})

const handleInput = () => {
  emit('update:modelValue', internalValue.value)
}

const insertFormat = (before, after) => {
  const textarea = textareaRef.value
  if (!textarea) return
  
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selected = internalValue.value.substring(start, end)
  
  const newValue = 
    internalValue.value.substring(0, start) +
    before + selected + after +
    internalValue.value.substring(end)
  
  internalValue.value = newValue
  emit('update:modelValue', newValue)
  
  nextTick(() => {
    textarea.focus()
    const newPos = start + before.length + selected.length
    textarea.setSelectionRange(newPos, newPos)
  })
}

const insertHeading = (level) => {
  const prefix = '#'.repeat(level) + ' '
  insertFormat(prefix, '')
}

const insertImage = () => {
  const url = prompt('请输入图片URL:')
  if (url) {
    insertFormat('![图片](', url + ')')
  }
}

const syncScroll = () => {
  if (!showPreview.value || !previewRef.value) return
  const textarea = textareaRef.value
  const preview = previewRef.value
  const ratio = textarea.scrollTop / (textarea.scrollHeight - textarea.clientHeight)
  preview.scrollTop = ratio * (preview.scrollHeight - preview.clientHeight)
}
</script>

<style lang="scss" scoped>
.markdown-editor {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: white;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-color);
}

.toolbar-group {
  display: flex;
  gap: 4px;
  padding: 0 8px;
  border-right: 1px solid var(--border-color);
  
  &:last-child {
    border-right: none;
  }
}

.tool-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  
  &:hover, &.active {
    background: var(--primary-color);
    color: white;
  }
  
  svg {
    width: 18px;
    height: 18px;
  }
}

.editor-container {
  display: flex;
  min-height: 400px;
  
  &.split {
    .editor-pane, .preview-pane {
      width: 50%;
    }
  }
}

.editor-pane {
  width: 100%;
  
  textarea {
    width: 100%;
    height: 100%;
    min-height: 400px;
    padding: 16px;
    border: none;
    outline: none;
    resize: none;
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    line-height: 1.8;
    color: var(--text-primary);
  }
}

.preview-pane {
  border-left: 1px solid var(--border-color);
  overflow-y: auto;
  
  .markdown-content {
    padding: 16px;
  }
}
</style>