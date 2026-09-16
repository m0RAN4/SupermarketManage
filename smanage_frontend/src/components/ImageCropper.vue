<template>
  <el-dialog
    v-model="visible"
    title="调整图片展示区域"
    width="750px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div style="color:#909399;font-size:13px;margin-bottom:12px">
      拖拽图片或滚轮缩放来调整展示区域。中间亮框 = 最终可见部分，框外变暗 = 不可见。不修改原图。
    </div>

    <!-- 编辑区域 -->
    <div
      ref="containerRef"
      class="editor-container"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseUp"
      @wheel.prevent="onWheel"
    >
      <!-- 加载中占位 -->
      <div v-if="!ready" class="loading-hint">加载图片中...</div>

      <!-- 底层图片（可拖拽+缩放），加载完成后才显示 -->
      <img
        ref="imageRef"
        :src="imageSrc"
        class="editor-image"
        :style="ready ? imageStyle : { display: 'none' }"
        draggable="false"
        @load="onImageLoad"
      />

      <!-- 遮罩层：单元素，box-shadow 外扩形成暗色遮罩，元素本体 = 透明展示框，尺寸严格等同 frameRect -->
      <div class="mask-overlay" :style="maskStyle"></div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  aspectRatio: { type: Number, default: 1 }
})

const visible = ref(false)
const imageSrc = ref('')
const imageRef = ref(null)
const containerRef = ref(null)
const ready = ref(false)

let resolvePromise = null
let originalFile = null

const naturalW = ref(0)
const naturalH = ref(0)
const containerW = ref(660)
const containerH = ref(460)
const offsetX = ref(0)
const offsetY = ref(0)
const scale = ref(1)
let dragging = false
let dragStartX = 0
let dragStartY = 0
let dragStartOffsetX = 0
let dragStartOffsetY = 0

const frameRect = computed(() => {
  const cw = containerW.value
  const ch = containerH.value
  const maxW = cw - 20
  const maxH = ch - 20
  let fw, fh
  if (maxW / maxH > props.aspectRatio) {
    fh = maxH
    fw = fh * props.aspectRatio
  } else {
    fw = maxW
    fh = fw / props.aspectRatio
  }
  return {
    left: (cw - fw) / 2,
    top: (ch - fh) / 2,
    width: fw,
    height: fh
  }
})

const imageStyle = computed(() => ({
  transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${scale.value})`,
  transformOrigin: '0 0',
  width: naturalW.value + 'px',
  height: naturalH.value + 'px'
}))

/** 遮罩样式 — 元素本身 = 展示框位置/尺寸，box-shadow 外扩形成暗色遮罩 */
const maskStyle = computed(() => {
  const f = frameRect.value
  return {
    left: f.left + 'px',
    top: f.top + 'px',
    width: f.width + 'px',
    height: f.height + 'px',
    boxShadow: '0 0 0 9999px rgba(0,0,0,0.55), inset 0 0 0 2px #fff'
  }
})

function open(file) {
  return new Promise((resolve) => {
    originalFile = file
    resolvePromise = resolve
    ready.value = false
    naturalW.value = 0; naturalH.value = 0
    scale.value = 1
    offsetX.value = 0; offsetY.value = 0
    imageSrc.value = URL.createObjectURL(file)
    visible.value = true
  })
}

function onImageLoad() {
  naturalW.value = imageRef.value?.naturalWidth || 0
  naturalH.value = imageRef.value?.naturalHeight || 0
  if (naturalW.value === 0 || naturalH.value === 0) return
  nextTick(() => {
    if (containerRef.value) {
      containerW.value = containerRef.value.clientWidth
      containerH.value = containerRef.value.clientHeight
    }
    // 初始定位：自动缩放至刚好填满框（cover 模式）
    applyCover()
    ready.value = true
  })
}

function getMinScale() {
  if (naturalW.value === 0 || naturalH.value === 0) return 1
  const f = frameRect.value
  return Math.max(f.width / naturalW.value, f.height / naturalH.value)
}

function clampScale(s) {
  // 下限 = 填满框的最小比例，上限 = 2 倍
  return Math.max(getMinScale(), Math.min(2, s))
}

/** 将图片偏移限制在框内不留白的范围内 */
function clampOffsets() {
  const f = frameRect.value
  const imgW = naturalW.value * scale.value
  const imgH = naturalH.value * scale.value
  const minX = f.left - imgW + f.width
  const maxX = f.left
  offsetX.value = Math.max(minX, Math.min(maxX, offsetX.value))
  const minY = f.top - imgH + f.height
  const maxY = f.top
  offsetY.value = Math.max(minY, Math.min(maxY, offsetY.value))
}

/** 初始 cover 定位：缩放至刚好填满框，居中 */
function applyCover() {
  scale.value = getMinScale()
  const f = frameRect.value
  const imgW = naturalW.value * scale.value
  const imgH = naturalH.value * scale.value
  offsetX.value = f.left + (f.width - imgW) / 2
  offsetY.value = f.top + (f.height - imgH) / 2
  clampOffsets()
}

function onMouseDown(e) {
  if (e.target.closest('.mask-overlay')) return
  dragging = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartOffsetX = offsetX.value
  dragStartOffsetY = offsetY.value
}
function onMouseMove(e) {
  if (!dragging) return
  offsetX.value = dragStartOffsetX + (e.clientX - dragStartX)
  offsetY.value = dragStartOffsetY + (e.clientY - dragStartY)
  clampOffsets()
}
function onMouseUp() { dragging = false }

function onWheel(e) {
  const rect = containerRef.value.getBoundingClientRect()
  if (!rect) return
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  const oldScale = scale.value
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  const newScale = clampScale(oldScale + delta)
  if (newScale === oldScale) return

  const ratio = newScale / oldScale
  offsetX.value = mx - ratio * (mx - offsetX.value)
  offsetY.value = my - ratio * (my - offsetY.value)
  scale.value = newScale
  clampOffsets()
}

function handleConfirm() {
  if (!ready.value) return
  const f = frameRect.value
  const cropX = (f.left - offsetX.value) / scale.value
  const cropY = (f.top - offsetY.value) / scale.value
  const cropW = f.width / scale.value
  const cropH = f.height / scale.value

  const nw = naturalW.value
  const nh = naturalH.value
  const cropData = {
    x: Math.round(Math.max(0, Math.min(100, (cropX / nw) * 100)) * 100) / 100,
    y: Math.round(Math.max(0, Math.min(100, (cropY / nh) * 100)) * 100) / 100,
    w: Math.round(Math.max(1, Math.min(100, (cropW / nw) * 100)) * 100) / 100,
    h: Math.round(Math.max(1, Math.min(100, (cropH / nh) * 100)) * 100) / 100
  }

  resolvePromise({
    file: originalFile,
    cropData: JSON.stringify(cropData)
  })
  cleanup()
}

function handleClose() {
  if (resolvePromise) resolvePromise(null)
  cleanup()
}

function cleanup() {
  visible.value = false
  ready.value = false
  if (imageSrc.value) { URL.revokeObjectURL(imageSrc.value); imageSrc.value = '' }
  resolvePromise = null
  originalFile = null
  naturalW.value = 0; naturalH.value = 0
  scale.value = 1
  dragging = false
}

defineExpose({ open })
</script>

<style scoped>
.editor-container {
  width: 100%;
  height: 460px;
  background: #1a1a2e;
  position: relative;
  overflow: hidden;
  cursor: grab;
  border-radius: 4px;
}
.editor-container:active {
  cursor: grabbing;
}
.editor-image {
  position: absolute;
  pointer-events: none;
}
.loading-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.5);
  font-size: 14px;
  z-index: 5;
}

.mask-overlay {
  position: absolute;
  pointer-events: none;
  z-index: 10;
}
</style>
