<script setup lang="ts">
import { Delete, ZoomIn } from '@element-plus/icons-vue'
import type { ImageHistory } from '@/types/image'

defineProps<{
  item: ImageHistory
}>()

const emit = defineEmits<{
  preview: [item: ImageHistory]
  remove: [item: ImageHistory]
}>()
</script>

<template>
  <article class="history-card">
    <button class="image-button" type="button" @click="emit('preview', item)">
      <el-image :src="item.imageUrl" fit="cover" lazy>
        <template #placeholder>
          <div class="image-placeholder">加载中...</div>
        </template>
        <template #error>
          <div class="image-placeholder">图片加载失败</div>
        </template>
      </el-image>
      <span class="preview-mask"><el-icon><ZoomIn /></el-icon> 查看大图</span>
    </button>

    <div class="card-body">
      <p class="prompt" :title="item.prompt">{{ item.prompt }}</p>
      <div class="meta-row">
        <span>{{ item.quality }}</span>
        <span>{{ item.size }}</span>
        <span>{{ item.createTime }}</span>
      </div>
      <el-button class="delete-button" text type="danger" :icon="Delete" @click="emit('remove', item)">
        删除
      </el-button>
    </div>
  </article>
</template>

<style scoped>
.history-card {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(20, 24, 39, 0.76);
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.2);
  transition: transform 0.25s ease, border-color 0.25s ease;
}

.history-card:hover {
  transform: translateY(-4px);
  border-color: rgba(124, 92, 255, 0.5);
}

.image-button {
  position: relative;
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  cursor: pointer;
  background: #0f1220;
}

.image-button :deep(.el-image) {
  display: block;
  width: 100%;
  aspect-ratio: 1;
}

.preview-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: white;
  font-size: 14px;
  background: rgba(6, 8, 16, 0.58);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.image-button:hover .preview-mask {
  opacity: 1;
}

.image-placeholder {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: #828aa5;
}

.card-body {
  position: relative;
  padding: 14px 16px 16px;
}

.prompt {
  display: -webkit-box;
  min-height: 44px;
  margin: 0 0 12px;
  overflow: hidden;
  color: #f2f4ff;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-right: 46px;
  color: #8991aa;
  font-size: 12px;
}

.delete-button {
  position: absolute;
  right: 8px;
  bottom: 8px;
}
</style>
