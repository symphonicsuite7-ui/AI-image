<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { Clock, Link, MagicStick, Picture, Refresh, UploadFilled } from '@element-plus/icons-vue'
import HistoryCard from '@/components/HistoryCard.vue'
import { deleteImage, editImage, editImageByUrl, generateImage, getImageHistory } from '@/api/image'
import type {
  EditSourceMode,
  GenerateImageRequest,
  ImageHistory,
  ImageMode,
  ImageQuality,
  ImageSize,
} from '@/types/image'

const form = reactive<GenerateImageRequest>({
  stylePrompt: '',
  prompt: '',
  size: '1024x1024',
  quality: 'low',
})

const qualityOptions: Array<{ label: string; value: ImageQuality; description: string }> = [
  { label: '快速', value: 'low', description: '速度优先' },
  { label: '均衡', value: 'medium', description: '质量与速度平衡' },
  { label: '精细', value: 'high', description: '更高细节' },
]

const sizeOptions: Array<{ label: string; value: ImageSize; description: string }> = [
  { label: '1:1 正方形', value: '1024x1024', description: '头像、封面、通用配图' },
  { label: '2:3 竖版', value: '1024x1536', description: '海报、人像、商品图' },
  { label: '3:2 横版', value: '1536x1024', description: '摄影、文章头图' },
  { label: '9:16 手机竖屏', value: '1152x2048', description: '短视频、手机壁纸' },
  { label: '16:9 宽屏', value: '2048x1152', description: '视频封面、桌面壁纸' },
]

const imageMode = ref<ImageMode>('generate')
const editSourceMode = ref<EditSourceMode>('upload')
const sourceFile = ref<File | null>(null)
const maskFile = ref<File | null>(null)
const sourcePreviewUrl = ref('')
const maskPreviewUrl = ref('')
const sourceImageUrl = ref('')
const generating = ref(false)
const loadingHistory = ref(false)
const elapsedSeconds = ref(0)
const resultUrl = ref('')
const resultElapsed = ref(0)
const history = ref<ImageHistory[]>([])
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const previewVisible = ref(false)
const previewItem = ref<ImageHistory | null>(null)
let timer: number | undefined

const hasEditSource = computed(() => {
  if (editSourceMode.value === 'upload') return Boolean(sourceFile.value)
  return /^https?:\/\/.+/i.test(sourceImageUrl.value.trim())
})
const canGenerate = computed(() => {
  const hasPrompt = form.prompt.trim().length > 0
  const sourceReady = imageMode.value === 'generate' || hasEditSource.value
  return hasPrompt && sourceReady && !generating.value
})
const displayElapsed = computed(() => {
  const seconds = generating.value ? elapsedSeconds.value : resultElapsed.value / 1000
  return `${seconds.toFixed(generating.value ? 0 : 1)} 秒`
})

async function handleGenerate() {
  if (!form.prompt.trim()) {
    ElMessage.warning('请先输入图片提示词')
    return
  }
  if (imageMode.value === 'edit' && !hasEditSource.value) {
    ElMessage.warning(editSourceMode.value === 'upload' ? '请先上传需要修改的原图' : '请输入有效的公网图片 URL')
    return
  }

  generating.value = true
  resultUrl.value = ''
  elapsedSeconds.value = 0
  const startedAt = Date.now()
  timer = window.setInterval(() => {
    elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000)
  }, 250)

  try {
    const stylePrompt = form.stylePrompt.trim()
    const prompt = form.prompt.trim()
    const result = imageMode.value === 'generate'
      ? await generateImage({ ...form, stylePrompt, prompt })
      : editSourceMode.value === 'upload'
        ? await editImage({
            ...form,
            stylePrompt,
            prompt,
            image: sourceFile.value as File,
            mask: maskFile.value || undefined,
          })
        : await editImageByUrl({ ...form, stylePrompt, prompt, imageUrl: sourceImageUrl.value.trim() })
    resultUrl.value = result.imageUrl
    resultElapsed.value = result.elapsedMillis
    ElMessage.success(imageMode.value === 'generate' ? '图片生成成功' : '图片修改成功')
    currentPage.value = 1
    await loadHistory()
  } catch {
    // The Axios interceptor has already shown a user-facing error message.
  } finally {
    generating.value = false
    if (timer) window.clearInterval(timer)
    elapsedSeconds.value = Math.floor((Date.now() - startedAt) / 1000)
  }
}

function replacePreview(currentUrl: string, file: File) {
  if (currentUrl) URL.revokeObjectURL(currentUrl)
  return URL.createObjectURL(file)
}

function handleSourceChange(uploadFile: UploadFile) {
  if (!uploadFile.raw) return
  sourceFile.value = uploadFile.raw
  sourcePreviewUrl.value = replacePreview(sourcePreviewUrl.value, uploadFile.raw)
}

function handleMaskChange(uploadFile: UploadFile) {
  if (!uploadFile.raw) return
  maskFile.value = uploadFile.raw
  maskPreviewUrl.value = replacePreview(maskPreviewUrl.value, uploadFile.raw)
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const page = await getImageHistory(currentPage.value, pageSize)
    history.value = page.records
    total.value = page.total
  } catch {
    // Keep the previous gallery when a refresh fails.
  } finally {
    loadingHistory.value = false
  }
}

function openPreview(item: ImageHistory) {
  previewItem.value = item
  previewVisible.value = true
}

async function handleDelete(item: ImageHistory) {
  try {
    await ElMessageBox.confirm('将同时删除历史记录和本地图片文件，是否继续？', '删除图片', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteImage(item.id)
    if (resultUrl.value === item.imageUrl) resultUrl.value = ''
    ElMessage.success('删除成功')
    if (history.value.length === 1 && currentPage.value > 1) currentPage.value -= 1
    await loadHistory()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(error)
    }
  }
}

async function handlePageChange(page: number) {
  currentPage.value = page
  await loadHistory()
  document.querySelector('.history-section')?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(loadHistory)
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  if (sourcePreviewUrl.value) URL.revokeObjectURL(sourcePreviewUrl.value)
  if (maskPreviewUrl.value) URL.revokeObjectURL(maskPreviewUrl.value)
})
</script>

<template>
  <main class="app-shell">
    <div class="ambient ambient-one" />
    <div class="ambient ambient-two" />

    <header class="topbar">
      <div class="brand-mark"><MagicStick /></div>
      <div>
        <h1>AI 绘图助手</h1>
        <p>把想象变成画面</p>
      </div>
    </header>

    <section class="hero-section">
      <div class="intro-copy">
        <span class="eyebrow">CREATE WITH AI</span>
        <h2 v-if="imageMode === 'generate'">描述你的灵感，<br /><em>生成独一无二的画面。</em></h2>
        <h2 v-else>上传一张图片，<br /><em>让 AI 重新演绎。</em></h2>
        <p v-if="imageMode === 'generate'">输入自然语言提示词，选择画质，然后交给 AI 完成创作。</p>
        <p v-else>支持本地图片、公网图片链接与可选遮罩图，描述你希望保留和改变的内容。</p>
      </div>

      <div class="generator-card">
        <el-radio-group v-model="imageMode" class="mode-switch" size="large">
          <el-radio-button value="generate">文生图</el-radio-button>
          <el-radio-button value="edit">改图 / 图生图</el-radio-button>
        </el-radio-group>

        <div v-if="imageMode === 'edit'" class="edit-source-panel">
          <div class="source-heading">
            <span class="field-label">原图来源</span>
            <el-radio-group v-model="editSourceMode" size="small">
              <el-radio-button value="upload">本地上传</el-radio-button>
              <el-radio-button value="url">图片链接</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="editSourceMode === 'upload'" class="upload-grid">
            <div>
              <span class="upload-label">原图 · 必填</span>
              <el-upload
                class="image-uploader"
                accept="image/png,image/jpeg,image/webp"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleSourceChange"
              >
                <img v-if="sourcePreviewUrl" :src="sourcePreviewUrl" alt="原图预览" />
                <div v-else class="upload-placeholder">
                  <el-icon><UploadFilled /></el-icon>
                  <span>选择原图</span>
                  <small>PNG / JPG / WebP，最大 20MB</small>
                </div>
              </el-upload>
            </div>
            <div>
              <span class="upload-label">遮罩图 · 可选</span>
              <el-upload
                class="image-uploader"
                accept="image/png,image/jpeg,image/webp"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleMaskChange"
              >
                <img v-if="maskPreviewUrl" :src="maskPreviewUrl" alt="遮罩图预览" />
                <div v-else class="upload-placeholder">
                  <el-icon><Picture /></el-icon>
                  <span>选择遮罩图</span>
                  <small>用于指定局部修改区域</small>
                </div>
              </el-upload>
            </div>
          </div>

          <el-input
            v-else
            v-model="sourceImageUrl"
            clearable
            :prefix-icon="Link"
            placeholder="https://example.com/input.png"
          />
        </div>

        <label class="field-label" for="style-prompt">统一风格提示词 <span class="optional-label">可选</span></label>
        <el-input
          id="style-prompt"
          v-model="form.stylePrompt"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          resize="none"
          placeholder="例如：统一使用电影级概念艺术风格，冷色调，高细节，保持相同的光影和视觉语言"
          @keydown.ctrl.enter="handleGenerate"
        />
        <p class="field-tip">会作为全局风格要求，与下方每次的图片描述一起发送给模型。</p>

        <label class="field-label prompt-label" for="prompt">
          {{ imageMode === 'generate' ? '图片描述' : '修改要求' }}
        </label>
        <el-input
          id="prompt"
          v-model="form.prompt"
          type="textarea"
          :rows="5"
          maxlength="4000"
          show-word-limit
          resize="none"
          :placeholder="imageMode === 'generate'
            ? '例如：一个漂浮在天空中的机械城市，巨大的远古遗迹，电影级概念艺术'
            : '例如：把这张图改成国潮海报风格，保留主体和构图'"
          @keydown.ctrl.enter="handleGenerate"
        />

        <div class="parameter-grid">
          <div>
            <span class="field-label">图片尺寸</span>
            <el-select v-model="form.size" class="full-width">
              <el-option
                v-for="option in sizeOptions"
                :key="option.value"
                :label="`${option.label} · ${option.value} · ${option.description}`"
                :value="option.value"
              />
            </el-select>
          </div>
          <div>
            <span class="field-label">生成质量</span>
            <el-select v-model="form.quality" class="full-width">
              <el-option
                v-for="option in qualityOptions"
                :key="option.value"
                :label="`${option.label} · ${option.description}`"
                :value="option.value"
              />
            </el-select>
          </div>
        </div>

        <el-button
          class="generate-button"
          type="primary"
          size="large"
          :loading="generating"
          :disabled="!canGenerate"
          @click="handleGenerate"
        >
          <el-icon v-if="!generating"><MagicStick /></el-icon>
          {{ generating
            ? `${imageMode === 'generate' ? '正在生成' : '正在改图'} · ${elapsedSeconds}s`
            : imageMode === 'generate' ? '开始生成' : '开始改图' }}
        </el-button>
        <p class="shortcut-tip">提示：按 Ctrl + Enter 快速生成</p>
      </div>
    </section>

    <section v-if="generating || resultUrl" class="result-section">
      <div class="section-heading">
        <div>
          <span class="eyebrow">LATEST CREATION</span>
          <h3>{{ imageMode === 'generate' ? '本次生成' : '本次改图' }}</h3>
        </div>
        <span class="elapsed"><el-icon><Clock /></el-icon>{{ displayElapsed }}</span>
      </div>

      <div v-if="generating" class="generating-stage">
        <div class="orb"><MagicStick /></div>
        <strong>{{ imageMode === 'generate' ? 'AI 正在绘制你的画面' : 'AI 正在重新演绎原图' }}</strong>
        <span>通常需要几十秒，请不要关闭页面</span>
      </div>
      <el-image v-else :src="resultUrl" fit="contain" class="result-image" :preview-src-list="[resultUrl]" />
    </section>

    <section class="history-section">
      <div class="section-heading">
        <div>
          <span class="eyebrow">YOUR GALLERY</span>
          <h3>创作历史</h3>
        </div>
        <el-button text :icon="Refresh" :loading="loadingHistory" @click="loadHistory">刷新</el-button>
      </div>

      <div v-loading="loadingHistory" class="history-content">
        <div v-if="history.length" class="history-grid">
          <HistoryCard
            v-for="item in history"
            :key="item.id"
            :item="item"
            @preview="openPreview"
            @remove="handleDelete"
          />
        </div>
        <el-empty v-else :image-size="120" description="还没有创作记录，生成第一张图片吧">
          <el-icon size="44"><Picture /></el-icon>
        </el-empty>

        <el-pagination
          v-if="total > pageSize"
          class="pagination"
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog v-model="previewVisible" width="min(900px, 92vw)" align-center destroy-on-close>
      <div v-if="previewItem" class="preview-dialog">
        <el-image :src="previewItem.imageUrl" fit="contain" />
        <p>{{ previewItem.prompt }}</p>
      </div>
    </el-dialog>
  </main>
</template>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: #f5f7ff;
}

.ambient {
  position: fixed;
  z-index: -1;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  filter: blur(110px);
  opacity: 0.22;
  pointer-events: none;
}

.ambient-one {
  top: -180px;
  left: -120px;
  background: #7148ff;
}

.ambient-two {
  right: -180px;
  bottom: 8%;
  background: #00b8d9;
}

.topbar,
.hero-section,
.result-section,
.history-section {
  width: min(1180px, calc(100% - 40px));
  margin-inline: auto;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 28px 0 20px;
}

.brand-mark {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 14px;
  color: white;
  background: linear-gradient(135deg, #855dff, #4d6cff);
  box-shadow: 0 10px 30px rgba(105, 82, 255, 0.34);
}

.brand-mark :deep(svg) {
  width: 23px;
}

.topbar h1,
.topbar p {
  margin: 0;
}

.topbar h1 {
  font-size: 18px;
}

.topbar p {
  margin-top: 3px;
  color: #828aa5;
  font-size: 12px;
}

.hero-section {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 68px;
  align-items: center;
  padding: 72px 0 84px;
}

.eyebrow {
  color: #8e80ff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.intro-copy h2 {
  margin: 16px 0 22px;
  font-size: clamp(38px, 5vw, 68px);
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.intro-copy h2 em {
  color: transparent;
  font-style: normal;
  background: linear-gradient(90deg, #9b78ff, #51c8ff);
  background-clip: text;
  -webkit-background-clip: text;
}

.intro-copy p {
  max-width: 500px;
  margin: 0;
  color: #959db5;
  font-size: 16px;
  line-height: 1.8;
}

.generator-card,
.result-section,
.history-section {
  border: 1px solid rgba(255, 255, 255, 0.09);
  background: rgba(17, 20, 34, 0.72);
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(24px);
}

.generator-card {
  padding: 28px;
  border-radius: 24px;
}

.mode-switch {
  display: flex;
  width: 100%;
  margin-bottom: 24px;
}

.mode-switch :deep(.el-radio-button) {
  flex: 1;
}

.mode-switch :deep(.el-radio-button__inner) {
  width: 100%;
}

.edit-source-panel {
  margin-bottom: 22px;
  padding: 16px;
  border: 1px solid rgba(124, 92, 255, 0.2);
  border-radius: 16px;
  background: rgba(10, 12, 22, 0.55);
}

.source-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.source-heading .field-label {
  margin: 0;
}

.upload-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.upload-label {
  display: block;
  margin-bottom: 8px;
  color: #9ba3ba;
  font-size: 12px;
}

.image-uploader :deep(.el-upload) {
  display: block;
  width: 100%;
  overflow: hidden;
  border: 1px dashed rgba(255, 255, 255, 0.16);
  border-radius: 13px;
  background: rgba(8, 10, 18, 0.76);
  transition: border-color 0.2s ease, background 0.2s ease;
}

.image-uploader :deep(.el-upload:hover) {
  border-color: #765cff;
  background: rgba(118, 92, 255, 0.08);
}

.image-uploader img {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  min-height: 128px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  color: #c5cada;
}

.upload-placeholder .el-icon {
  color: #8e80ff;
  font-size: 25px;
}

.upload-placeholder small {
  color: #687088;
  font-size: 10px;
}

.prompt-label {
  margin-top: 18px;
}

.field-label {
  display: block;
  margin: 0 0 10px;
  color: #dfe3f2;
  font-size: 13px;
  font-weight: 600;
}

.optional-label {
  color: #737b93;
  font-size: 11px;
  font-weight: 400;
}

.field-tip {
  margin: 7px 0 0;
  color: #69718a;
  font-size: 11px;
}

.parameter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 20px;
}

.full-width {
  width: 100%;
}

.generate-button {
  width: 100%;
  height: 50px;
  margin-top: 22px;
  border: 0;
  border-radius: 12px;
  font-weight: 700;
  background: linear-gradient(100deg, #7c5cff, #526cff);
  box-shadow: 0 14px 35px rgba(91, 92, 255, 0.28);
}

.shortcut-tip {
  margin: 10px 0 0;
  color: #69718a;
  font-size: 11px;
  text-align: center;
}

.result-section,
.history-section {
  padding: 28px;
  border-radius: 24px;
}

.result-section {
  margin-bottom: 34px;
}

.history-section {
  margin-bottom: 64px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.section-heading h3 {
  margin: 7px 0 0;
  font-size: 25px;
}

.elapsed {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8b93aa;
  font-size: 13px;
}

.generating-stage {
  display: flex;
  min-height: 420px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border-radius: 18px;
  color: #f3f5ff;
  background: radial-gradient(circle at center, rgba(111, 79, 255, 0.16), transparent 48%), #0e111d;
}

.generating-stage span {
  color: #7f879f;
  font-size: 13px;
}

.orb {
  display: grid;
  width: 70px;
  height: 70px;
  margin-bottom: 8px;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #8d65ff, #426dff);
  box-shadow: 0 0 60px rgba(111, 78, 255, 0.65);
  animation: pulse 1.5s ease-in-out infinite;
}

.result-image {
  display: block;
  width: 100%;
  max-height: 780px;
  border-radius: 18px;
  background: #0d101b;
}

.history-content {
  min-height: 260px;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.pagination {
  justify-content: center;
  margin-top: 30px;
}

.preview-dialog :deep(.el-image) {
  display: block;
  width: 100%;
  max-height: 74vh;
}

.preview-dialog p {
  margin: 18px 4px 0;
  color: #b4bbcf;
  line-height: 1.6;
}

@keyframes pulse {
  0%, 100% { transform: scale(0.95); opacity: 0.75; }
  50% { transform: scale(1.06); opacity: 1; }
}

@media (max-width: 860px) {
  .hero-section {
    grid-template-columns: 1fr;
    gap: 38px;
    padding: 44px 0 64px;
  }

  .intro-copy {
    text-align: center;
  }

  .intro-copy p {
    margin-inline: auto;
  }

  .history-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .topbar,
  .hero-section,
  .result-section,
  .history-section {
    width: min(100% - 24px, 1180px);
  }

  .parameter-grid,
  .history-grid,
  .upload-grid {
    grid-template-columns: 1fr;
  }

  .source-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .generator-card,
  .result-section,
  .history-section {
    padding: 18px;
    border-radius: 18px;
  }

  .intro-copy h2 {
    font-size: 38px;
  }
}
</style>
