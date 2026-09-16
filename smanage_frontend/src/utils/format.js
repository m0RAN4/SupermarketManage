/**
 * 格式化时间字符串，移除中间的 T
 * @param {string} timeStr - 时间字符串
 * @returns {string} 格式化后的时间
 */
export function formatTime(timeStr) {
  if (!timeStr) return '-'
  return String(timeStr).replace('T', ' ')
}

/**
 * 格式化金额
 * @param {number} amount - 金额
 * @returns {string} 格式化后的金额字符串
 */
export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '-'
  return '¥' + Number(amount).toFixed(2)
}
