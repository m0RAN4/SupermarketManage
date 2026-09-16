<template>
  <div ref="containerRef" class="pos-img-container" :style="{ aspectRatio: props.aspectRatio }">
    <!-- 有定位数据：按 cropData 精确裁切展示 -->
    <img
      v-if="src && !errored && hasCrop"
      :src="src"
      :style="cropStyle"
      @error="errored = true"
    />
    <!-- 无定位数据：回退为普通 cover 居中展示 -->
    <img
      v-else-if="src && !errored"
      :src="src"
      class="pos-img-fallback"
      @error="errored = true"
    />
    <!-- 错误或无图片 -->
    <div v-if="!src || errored" class="pos-img-placeholder">
      <el-icon :size="24"><PictureFilled /></el-icon>
      <span>暂无图片</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { PictureFilled } from '@element-plus/icons-vue'

const props = defineProps({
  src: { type: String, default: '' },
  cropData: { type: String, default: null },
  aspectRatio: { type: Number, default: null }
})

const containerRef = ref(null)
const errored = ref(false)
const naturalW = ref(0)
const naturalH = ref(0)
const containerW = ref(0)

function parseCrop() {
  if (!props.cropData) return null
  try {
    const d = JSON.parse(props.cropData)
    if (d.x != null && d.y != null && d.w != null && d.h != null) {
      return { x: +d.x, y: +d.y, w: +d.w, h: +d.h }
    }
  } catch {}
  return null
}

const hasCrop = computed(() => {
  const crop = parseCrop()
  return crop && naturalW.value > 0 && containerW.value > 0
})

const cropStyle = computed(() => {
  const crop = parseCrop()
  if (!crop || naturalW.value === 0 || containerW.value === 0) return null
  const cw = containerW.value

  const cropLeft = naturalW.value * crop.x / 100
  const cropTop = naturalH.value * crop.y / 100
  const cropW = naturalW.value * crop.w / 100
  const cropH = naturalH.value * crop.h / 100

  const scale = cw / cropW

  return {
    width: naturalW.value + 'px',
    height: naturalH.value + 'px',
    transform: `translate(${-cropLeft * scale}px, ${-cropTop * scale}px) scale(${scale})`,
    transformOrigin: '0 0',
    display: 'block',
    maxWidth: 'none'
  }
})

function updateSize() {
  if (containerRef.value) {
    containerW.value = containerRef.value.clientWidth
  }
}

function preloadDimensions(src) {
  if (!src) return
  const img = new Image()
  img.onload = () => {
    naturalW.value = img.naturalWidth
    naturalH.value = img.naturalHeight
  }
  img.src = src
}

onMounted(() => {
  updateSize()
  preloadDimensions(props.src)
  nextTick(updateSize)
  window.addEventListener('resize', updateSize)
})

watch(() => props.src, (val) => {
  errored.value = false
  if (val) preloadDimensions(val)
})

watch(() => containerRef.value, updateSize)
</script>

<style scoped>
.pos-img-container {
  width: 100%;
  overflow: hidden;
  position: relative;
  background: #f5f7fa;
}
.pos-img-fallback {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}
.pos-img-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
  gap: 8px;
}
</style>
