<template>
  <div class="cashier-container">
    <!-- Page Header -->
    <div class="cashier-header">
      <div class="header-info">
        <h2 class="header-title">
          <el-icon class="header-title-icon"><ShoppingCartFull /></el-icon>
          收银台
        </h2>
        <p class="header-desc">选择货架 → 浏览商品 → 加入购物车 → 结算</p>
      </div>
      <div class="header-badge" v-if="cart.length > 0">
        <el-icon><ShoppingCartFull /></el-icon>
        <span>{{ totalItems }} 件</span>
      </div>
    </div>

    <el-row :gutter="14">
      <!-- LEFT: 货架列表 -->
      <el-col :span="5">
        <div class="panel panel-shelves">
          <div class="panel-header">
            <h4>
              <el-icon class="panel-title-icon"><Grid /></el-icon>
              货架列表
            </h4>
            <el-tag size="small" round>{{ shelves.length }} 个</el-tag>
          </div>
          <div class="shelf-list" v-loading="loading">
            <div
              v-for="shelf in shelves"
              :key="shelf.shelfId"
              class="shelf-card"
              :class="{ active: selectedShelf?.shelfId === shelf.shelfId }"
              @click="selectShelf(shelf)"
            >
              <div class="shelf-icon">
                <el-icon :size="18"><Grid /></el-icon>
              </div>
              <div class="shelf-info">
                <strong class="shelf-code">{{ shelf.shelfCode }}</strong>
                <span class="shelf-area">{{ shelf.shelfArea }}</span>
              </div>
              <el-tag size="small" type="success" effect="plain">{{ shelf.totalItems }}</el-tag>
            </div>
            <el-empty v-if="!loading && shelves.length === 0" description="暂无货架" :image-size="60" />
          </div>
        </div>
      </el-col>

      <!-- MIDDLE: 层 → 商品 -->
      <el-col :span="11">
        <div class="panel panel-items" v-loading="loading">
          <template v-if="selectedShelf">
            <div class="panel-header">
              <h4>{{ selectedShelf.shelfCode }} · {{ selectedShelf.shelfArea }}</h4>
              <span class="panel-sub">{{ (selectedShelf.layers || []).reduce((s, l) => s + (l.itemCount || 0), 0) }} 件在售</span>
            </div>
            <div class="layer-list">
              <div v-for="layer in (selectedShelf.layers || [])" :key="layer.layer" class="layer-group">
                <div class="layer-label">
                  <span class="layer-num">第 {{ layer.layer }} 层</span>
                  <span class="layer-count">{{ layer.itemCount }} 件</span>
                </div>
                <div class="item-grid">
                  <div
                    v-for="item in layer.items"
                    :key="item.batchId"
                    class="item-card"
                    :class="{ selected: cartItemIds.has(item.batchId) }"
                    @click="toggleCartItem(item)"
                  >
                    <div class="item-image">
                      <PositionedImage
                        v-if="item.imageUrl"
                        :src="getImageUrl(item.imageUrl)"
                        :cropData="item.cropData"
                        :aspectRatio="(16/9)"
                        style="width:100%"
                      />
                      <div v-else class="image-placeholder">
                        <el-icon :size="22"><PictureFilled /></el-icon>
                      </div>
                    </div>
                    <div class="item-body">
                      <div class="item-name" :title="item.shelfDisplayName || item.productName">
                        {{ item.shelfDisplayName || item.productName }}
                      </div>
                      <div class="item-price">
                        <template v-if="item.discountPrice">
                          <span class="price-original">¥{{ (item.salePrice || 0).toFixed(2) }}</span>
                          <span class="price-discount">¥{{ Number(item.discountPrice).toFixed(2) }}</span>
                        </template>
                        <template v-else>
                          <span class="price-normal">¥{{ (item.salePrice || 0).toFixed(2) }}</span>
                        </template>
                      </div>
                    </div>
                    <div class="item-check" v-if="cartItemIds.has(item.batchId)">
                      <el-icon><Check /></el-icon>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="panel-empty">
            <el-icon :size="48" color="#c0c4cc"><ArrowLeft /></el-icon>
            <p>请选择左侧货架浏览商品</p>
          </div>
        </div>
      </el-col>

      <!-- RIGHT: 购物车 -->
      <el-col :span="8">
        <div class="panel panel-cart">
          <div class="panel-header cart-panel-header">
            <div>
              <h4>
                <el-icon class="panel-title-icon"><ShoppingCartFull /></el-icon>
                购物车
              </h4>
              <span v-if="cart.length > 0" class="cart-sub-text">{{ totalItems }} 件商品</span>
            </div>
            <div class="cart-header-actions">
              <el-button text type="danger" size="small" @click="clearCart" :disabled="cart.length === 0">
                <el-icon><Delete /></el-icon> 清空
              </el-button>
              <el-button
                v-if="cart.length > 0"
                text type="warning"
                size="small"
                @click="removeSelected"
                :disabled="cartSelected.size === 0"
              >
                移除选中
              </el-button>
            </div>
          </div>

          <div class="cart-items" v-if="cart.length > 0">
            <div
              v-for="(item, idx) in cart"
              :key="item.batchId"
              class="cart-item"
              :class="{ selected: cartSelected.has(idx) }"
              @click="toggleCartSelect(idx)"
            >
              <div class="cart-item-check">
                <div class="check-circle" :class="{ checked: cartSelected.has(idx) }">
                  <el-icon v-if="cartSelected.has(idx)" :size="12"><Check /></el-icon>
                </div>
              </div>
              <div class="cart-item-info">
                <div class="cart-item-name">{{ item.shelfDisplayName || item.productName }}</div>
                <div class="cart-item-price">
                  <template v-if="item.discountPrice">
                    <span class="cart-discount">¥{{ Number(item.discountPrice).toFixed(2) }}</span>
                    <span class="cart-original">¥{{ (item.salePrice || 0).toFixed(2) }}</span>
                  </template>
                  <template v-else>¥{{ (item.price || 0).toFixed(2) }}</template>
                </div>
              </div>
              <div class="cart-item-subtotal">
                ¥{{ (item.discountPrice || item.price || 0).toFixed(2) }}
              </div>
            </div>
          </div>
          <div v-else class="cart-empty-state">
            <el-icon :size="40" color="#c0c4cc"><ShoppingCartFull /></el-icon>
            <p>购物车是空的</p>
            <span>点击左侧商品即可添加</span>
          </div>

          <div class="cart-footer" v-if="cart.length > 0">
            <div class="cart-summary-row">
              <span>合计</span>
              <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
            </div>
            <el-button type="primary" size="large" @click="submitOrder" class="submit-btn" :loading="submitting">
              结算下单 · ¥{{ totalAmount.toFixed(2) }}
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, ArrowLeft, PictureFilled, ShoppingCartFull, Grid } from '@element-plus/icons-vue'
import { getForSaleList, reserveCartItem, releaseCartItem } from '../api/product'
import { createOrder } from '../api/order'
import PositionedImage from '../components/PositionedImage.vue'

const router = useRouter()

function getImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api/picture/')) return url
  return '/api/picture/' + url
}

const shelves = ref([])
const loading = ref(false)
const selectedShelf = ref(null)

const cart = ref([])
const cartSelected = ref(new Set())
const submitting = ref(false)

const cartItemIds = computed(() => new Set(cart.value.map(c => c.batchId)))

const totalItems = computed(() => cart.value.length)
const totalAmount = computed(() => cart.value.reduce((s, i) => s + (i.discountPrice || i.price || 0), 0))

function selectShelf(shelf) {
  selectedShelf.value = shelf
}

async function toggleCartItem(item) {
  const existing = cart.value.findIndex(c => c.batchId === item.batchId)
  if (existing >= 0) {
    // 从购物车移除 → 解锁商品，重新上架
    try {
      await releaseCartItem(item.batchId)
      cart.value.splice(existing, 1)
      // 不移除本地货架数据，靠 cartItemIds 控制选中状态
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  } else {
    // 加入购物车 → 立即锁定商品
    try {
      await reserveCartItem(item.batchId)
      cart.value.push({
        batchId: item.batchId,
        baseProductId: item.baseProductId,
        productName: item.productName,
        shelfDisplayName: item.shelfDisplayName,
        price: item.salePrice || 0,
        discountPrice: item.discountPrice || null
      })
      // 不移除本地货架数据，靠 cartItemIds 控制选中状态；切换货架再回来也能看到
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '该商品已被他人抢先选中')
    }
  }
}

function toggleCartSelect(idx) {
  const s = new Set(cartSelected.value)
  if (s.has(idx)) s.delete(idx); else s.add(idx)
  cartSelected.value = s
}

async function removeSelected() {
  const indices = [...cartSelected.value].sort((a, b) => b - a)
  for (const i of indices) {
    try { await releaseCartItem(cart.value[i].batchId) } catch {}
    cart.value.splice(i, 1)
  }
  cartSelected.value = new Set()
}

async function clearCart() {
  for (const item of cart.value) {
    try { await releaseCartItem(item.batchId) } catch {}
  }
  cart.value = []; cartSelected.value = new Set()
}

async function loadShelves() {
  loading.value = true
  try {
    const res = await getForSaleList()
    shelves.value = res.data || []
  } catch { shelves.value = [] }
  finally { loading.value = false }
}

async function submitOrder() {
  if (cart.value.length === 0) { ElMessage.warning('购物车为空'); return }
  try {
    await ElMessageBox.confirm(`共 ${totalItems.value} 件，合计 ¥${totalAmount.value.toFixed(2)}。确认下单？`, '确认下单', { type: 'info' })
    submitting.value = true
    const items = cart.value.map(item => ({
      baseProductId: item.baseProductId,
      batchId: item.batchId,
      buyQuantity: 1
    }))
    const res = await createOrder({ items })
    if (res.code === 200) {
      const data = res.data
      const oid = data.orderId || data.id
      const finalAmount = totalAmount.value
      cart.value = []
      cartSelected.value = new Set()
      await loadShelves() // 下单成功后刷新，已售出商品从货架移除
      await ElMessageBox.alert(`订单创建成功！订单号：${oid}\n金额：¥${finalAmount.toFixed(2)}`, '下单成功', {
        confirmButtonText: '去支付', type: 'success',
        callback: () => router.push(`/order/${oid}`)
      })
    } else {
      ElMessage.error(res.message || '下单失败')
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '下单失败')
  } finally { submitting.value = false }
}

onMounted(() => loadShelves())

onBeforeUnmount(async () => {
  for (const item of cart.value) {
    try { await releaseCartItem(item.batchId) } catch {}
  }
})
</script>

<style scoped>
.cashier-container {
  padding: 0;
  height: calc(100vh - 96px);
}

/* ========== 头部 ========== */
.cashier-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title-icon {
  color: #43a047;
}

.header-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f0f9eb;
  color: #43a047;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

/* ========== 面板通用 ========== */
.panel {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  height: calc(100vh - 160px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
  flex-shrink: 0;
}

.panel-header h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-title-icon {
  color: #43a047;
}

.panel-sub {
  font-size: 12px;
  color: #909399;
}

.panel-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  gap: 8px;
}

.panel-empty p {
  margin: 0;
  font-size: 14px;
}

/* ========== 货架列表 ========== */
.shelf-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.shelf-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.shelf-card:hover {
  background: #f9fafb;
}

.shelf-card.active {
  border-color: #43a047;
  background: #f0f9eb;
}

.shelf-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #43a047;
  flex-shrink: 0;
}

.shelf-card.active .shelf-icon {
  background: #43a047;
  color: #ffffff;
}

.shelf-info {
  flex: 1;
  min-width: 0;
}

.shelf-code {
  display: block;
  font-size: 14px;
  color: #303133;
}

.shelf-area {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* ========== 商品面板 ========== */
.panel-items {
  overflow: hidden;
}

.layer-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
}

.layer-group {
  margin-bottom: 16px;
}

.layer-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 4px 10px;
  background: #f0f9eb;
  border-radius: 6px;
}

.layer-num {
  font-size: 13px;
  font-weight: 600;
  color: #43a047;
}

.layer-count {
  font-size: 12px;
  color: #909399;
}

.item-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.item-card {
  width: 144px;
  background: #ffffff;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
  position: relative;
}

.item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.item-card.selected {
  border-color: #43a047;
  background: #f9fef9;
  box-shadow: 0 0 0 3px rgba(67, 160, 71, 0.1);
}

.item-image {
  width: 100%;
  height: 80px;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 80px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.item-body {
  padding: 8px 10px;
}

.item-name {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.price-normal {
  color: #e6a23c;
  font-weight: 700;
  font-size: 13px;
}

.price-discount {
  color: #f56c6c;
  font-weight: 700;
  font-size: 13px;
}

.price-original {
  text-decoration: line-through;
  color: #c0c4cc;
  font-size: 11px;
  margin-right: 6px;
}

.item-check {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: #43a047;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 11px;
}

/* ========== 购物车面板 ========== */
.panel-cart {
  display: flex;
  flex-direction: column;
}

.cart-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.cart-sub-text {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 2px;
}

.cart-header-actions {
  display: flex;
  gap: 4px;
}

/* 购物车商品 */
.cart-items {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  border-bottom: 1px solid #f8f8f8;
}

.cart-item:hover {
  background: #fafafa;
}

.cart-item.selected {
  background: #f0f9eb;
}

.cart-item-check {
  flex-shrink: 0;
}

.check-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.check-circle.checked {
  background: #43a047;
  border-color: #43a047;
  color: #ffffff;
}

.cart-item-info {
  flex: 1;
  min-width: 0;
}

.cart-item-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #303133;
}

.cart-item-price {
  font-size: 12px;
  color: #43a047;
  font-weight: 600;
  margin-top: 2px;
}

.cart-discount {
  color: #f56c6c;
}

.cart-original {
  text-decoration: line-through;
  color: #c0c4cc;
  font-size: 11px;
  margin-left: 6px;
  font-weight: 400;
}

.cart-item-subtotal {
  font-weight: 700;
  font-size: 13px;
  color: #303133;
  min-width: 50px;
  text-align: right;
  flex-shrink: 0;
}

/* 空购物车 */
.cart-empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.cart-empty-state p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #909399;
}

.cart-empty-state span {
  font-size: 12px;
  color: #c0c4cc;
}

/* 购物车底部 */
.cart-footer {
  padding: 14px 16px;
  border-top: 2px solid #f5f5f5;
  flex-shrink: 0;
}

.cart-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #909399;
}

.total-amount {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #43a047, #66bb6a) !important;
  border: none !important;
  transition: all 0.3s !important;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #388e3c, #43a047) !important;
  box-shadow: 0 6px 20px rgba(67, 160, 71, 0.4) !important;
  transform: translateY(-1px);
}
</style>
