import request from './index'

// ============ Product Category ============
export const getCategoryPage = (params) => request.get('/product-category/page', { params })
export const getCategoryById = (id) => request.get(`/product-category/${id}`)
export const addCategory = (data) => request.post('/product-category', data)
export const updateCategory = (data) => request.put('/product-category', data)
export const toggleCategory = (id) => request.put(`/product-category/toggle/${id}`)

// ============ Product Base ============
export const getProductBasePage = (params) => request.get('/product-base/page', { params })
export const getProductBaseById = (id) => request.get(`/product-base/${id}`)
export const getProductBaseList = (params) => request.get('/product-base/list', { params })
export const addProductBase = (data) => request.post('/product-base', data)
export const updateProductBase = (data) => request.put('/product-base', data)
export const toggleProductBase = (id) => request.put(`/product-base/toggle/${id}`)

// ============ Shelf ============
export const getShelfPage = (params) => request.get('/shelf/page', { params })
export const getShelfById = (id) => request.get(`/shelf/${id}`)
export const getShelfList = () => request.get('/shelf/list')
export const addShelf = (data) => request.post('/shelf', data)
export const updateShelf = (data) => request.put('/shelf', data)
export const toggleShelf = (id) => request.put(`/shelf/toggle/${id}`)
export const getShelfCapacity = (id) => request.get(`/shelf/capacity/${id}`)

// ============ Shelf On Manage (上架管理) ============
export const getShelfOnBatches = (params) => request.get('/shelf-on/batches', { params })
export const getShelfLayers = (shelfId) => request.get(`/shelf-on/shelf-layers/${shelfId}`)
export const batchShelfOn = (data) => request.post('/shelf-on/batch-on', data)
export const batchShelfOff = (data) => request.post('/shelf-on/batch-off', data)
export const batchUpdatePrice = (data) => request.put('/shelf-on/price', data)
export const getValidStockList = () => request.get('/shelf-on/list-valid')
export const getForSaleList = () => request.get('/shelf-on/for-sale')
export const reserveCartItem = (batchId) => request.post(`/shelf-on/reserve/${batchId}`)
export const releaseCartItem = (batchId) => request.post(`/shelf-on/release/${batchId}`)

// ============ Stock Detail ============
export const getProductStockDetail = (baseProductId) => request.get(`/product-base/stock-detail/${baseProductId}`)

// ============ Dashboard ============
export const getDashboardStats = () => request.get('/dashboard/stats')

// ============ Promotion Image ============
export const getPromotionPage = (params) => request.get('/promotion/page', { params })
export const getActivePromotions = () => request.get('/promotion/active')
export const addPromotion = (data) => request.post('/promotion', data)
export const togglePromotion = (id) => request.put(`/promotion/toggle/${id}`)
export const deletePromotion = (id) => request.delete(`/promotion/${id}`)

// ============ File Upload ============
export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
