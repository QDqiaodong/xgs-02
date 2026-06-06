<template>
  <div class="ingredients-table">
    <div class="table-header">
      <h3>食材清单</h3>
      <button type="button" class="add-btn" @click="addIngredient">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        添加食材
      </button>
    </div>
    
    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th width="40%">食材名称</th>
            <th width="25%">用量</th>
            <th width="25%">备注</th>
            <th width="10%">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in modelValue" :key="index">
            <td>
              <input
                type="text"
                v-model="item.name"
                placeholder="如：五花肉"
                @input="emitChange"
              />
            </td>
            <td>
              <input
                type="text"
                v-model="item.amount"
                placeholder="如：500g"
                @input="emitChange"
              />
            </td>
            <td>
              <input
                type="text"
                v-model="item.note"
                placeholder="可选"
                @input="emitChange"
              />
            </td>
            <td>
              <button type="button" class="delete-btn" @click="removeIngredient(index)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
              </button>
            </td>
          </tr>
          <tr v-if="modelValue.length === 0">
            <td colspan="4" class="empty-row">
              点击上方按钮添加食材
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="quick-add" v-if="suggestions.length">
      <span class="quick-label">常用食材：</span>
      <span 
        v-for="item in suggestions" 
        :key="item" 
        class="quick-tag"
        @click="quickAdd(item)"
      >{{ item }}</span>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const suggestions = ['五花肉', '鸡蛋', '西红柿', '土豆', '青椒', '牛肉', '鸡肉', '豆腐', '葱姜蒜']

const addIngredient = () => {
  const newList = [...props.modelValue, { name: '', amount: '', note: '' }]
  emit('update:modelValue', newList)
}

const removeIngredient = (index) => {
  const newList = props.modelValue.filter((_, i) => i !== index)
  emit('update:modelValue', newList)
}

const quickAdd = (name) => {
  const newList = [...props.modelValue, { name, amount: '', note: '' }]
  emit('update:modelValue', newList)
}

const emitChange = () => {
  emit('update:modelValue', props.modelValue)
}
</script>

<style lang="scss" scoped>
.ingredients-table {
  background: white;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--bg-tertiary);
  
  h3 {
    margin: 0;
    font-size: 18px;
  }
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--primary-color);
  color: white;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  
  &:hover {
    background: var(--primary-dark);
  }
  
  svg {
    width: 18px;
    height: 18px;
  }
}

.table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  
  th, td {
    padding: 12px 16px;
    text-align: left;
    border-bottom: 1px solid var(--border-color);
  }
  
  th {
    background: var(--bg-tertiary);
    font-weight: 500;
    font-size: 13px;
    color: var(--text-secondary);
  }
  
  input {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid var(--border-color);
    border-radius: 6px;
    font-size: 14px;
    transition: border-color 0.2s ease;
    
    &:focus {
      outline: none;
      border-color: var(--primary-color);
    }
  }
  
  .empty-row {
    text-align: center;
    padding: 40px;
    color: var(--text-light);
  }
}

.delete-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #fef2f2;
  color: #ef4444;
  transition: all 0.2s ease;
  
  &:hover {
    background: #fee2e2;
  }
  
  svg {
    width: 18px;
    height: 18px;
  }
}

.quick-add {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.quick-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.quick-tag {
  padding: 6px 12px;
  background: var(--bg-tertiary);
  border-radius: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: var(--primary-color);
    color: white;
  }
}
</style>