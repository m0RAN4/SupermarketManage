import request from './index'

// ============ Purchase ============
export const getPurchasePage = (params) => request.get('/purchase/page', { params })
export const addPurchase = (data) => request.post('/purchase', data)

// ============ Sale Order ============
export const getOrderPage = (params) => request.get('/order/page', { params })
export const getOrderDetail = (id) => request.get(`/order/${id}`)
export const createOrder = (data) => request.post('/order/create', data)
export const payOrder = (data) => request.post('/order/pay', data)
export const cancelOrder = (data) => request.post('/order/cancel', data)
export const updateOrderPayStatus = (data) => request.put('/order/pay-status', data)

// ============ Refund ============
export const getRefundPage = (params) => request.get('/refund/page', { params })
export const processRefund = (data) => request.post('/refund', data)
export const approveRefund = (data) => request.post('/refund/approve', data)
export const rejectRefund = (data) => request.post('/refund/reject', data)
export const batchApproveRefund = (data) => request.post('/refund/batch-approve', data)
export const batchRejectRefund = (data) => request.post('/refund/batch-reject', data)

// ============ Loss ============
export const getLossPage = (params) => request.get('/loss/page', { params })
export const addLoss = (data) => request.post('/loss', data)
export const getLossMonthlyStats = (params) => request.get('/loss/monthly-stats', { params })

// ============ Profit ============
export const getMonthlyProfit = (params) => request.get('/profit/monthly', { params })

// ============ User Feedback ============
export const getFeedbackPage = (params) => request.get('/feedback/page', { params })
export const submitFeedback = (data) => request.post('/feedback', data)
export const replyFeedback = (data) => request.post('/feedback/reply', data)
