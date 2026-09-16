import request from './index'

export const sendSms = (phone) => request.post('/auth/send-sms', { phone })
export const viewSmsCode = (phone) => request.post('/auth/view-sms-code', { phone })
export const registerUser = (data) => request.post('/auth/register-user', data)
export const registerAdmin = (data) => request.post('/auth/register-admin', data)
export const login = (data) => request.post('/auth/login', data)
export const forgetPassword = (data) => request.post('/auth/forget-password', data)
export const changePassword = (data) => request.post('/auth/change-password', data)
export const cancelAccount = () => request.post('/auth/cancel-account')
