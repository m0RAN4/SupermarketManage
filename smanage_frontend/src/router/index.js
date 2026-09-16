import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册', noAuth: true }
  },
  {
    path: '/forget-password',
    name: 'ForgetPassword',
    component: () => import('../views/ForgetPassword.vue'),
    meta: { title: '忘记密码', noAuth: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '主页', requiresAdmin: true }
      },
      {
        path: 'user-dashboard',
        name: 'UserDashboard',
        component: () => import('../views/UserDashboard.vue'),
        meta: { title: '用户主页' }
      },
      {
        path: 'promotion-manage',
        name: 'PromotionManage',
        component: () => import('../views/PromotionManage.vue'),
        meta: { title: '宣传管理', requiresAdmin: true }
      },
      {
        path: 'category',
        name: 'ProductCategory',
        component: () => import('../views/ProductCategory.vue'),
        meta: { title: '商品分类管理', requiresAdmin: true }
      },
      {
        path: 'product-base',
        name: 'ProductBase',
        component: () => import('../views/ProductBase.vue'),
        meta: { title: '商品档案管理', requiresAdmin: true }
      },
      {
        path: 'shelf',
        name: 'ShelfManage',
        component: () => import('../views/ShelfManage.vue'),
        meta: { title: '货架管理', requiresAdmin: true }
      },
      {
        path: 'shelf-on',
        name: 'ShelfOnManage',
        component: () => import('../views/ShelfOnManage.vue'),
        meta: { title: '上架管理', requiresAdmin: true }
      },
      {
        path: 'purchase',
        name: 'Purchase',
        component: () => import('../views/Purchase.vue'),
        meta: { title: '进货管理', requiresAdmin: true }
      },
      {
        path: 'cashier',
        name: 'Cashier',
        component: () => import('../views/Cashier.vue'),
        meta: { title: '收银台' }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('../views/OrderList.vue'),
        meta: { title: '订单查询' }
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('../views/OrderDetail.vue'),
        meta: { title: '订单详情' }
      },
      {
        path: 'refund-manage',
        name: 'RefundManage',
        component: () => import('../views/RefundManage.vue'),
        meta: { title: '退货管理', requiresAdmin: true }
      },
      {
        path: 'user-feedback',
        name: 'UserFeedback',
        component: () => import('../views/UserFeedback.vue'),
        meta: { title: '用户反馈' }
      },
      {
        path: 'loss',
        name: 'LossRecord',
        component: () => import('../views/LossRecord.vue'),
        meta: { title: '损耗管理', requiresAdmin: true }
      },
      {
        path: 'profit',
        name: 'ProfitStat',
        component: () => import('../views/ProfitStat.vue'),
        meta: { title: '月度盈利', requiresAdmin: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfoStr = localStorage.getItem('userInfo')
  let userInfo = null
  try {
    userInfo = JSON.parse(userInfoStr)
  } catch (e) {
    userInfo = null
  }

  if (to.meta.title) {
    document.title = to.meta.title + ' - 超市管理系统'
  }

  if (to.meta.noAuth) {
    return next()
  }

  if (!token || !userInfo) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  if (to.meta.requiresAdmin && userInfo.userType !== 0) {
    return next({ name: 'UserDashboard' })
  }

  next()
})

export default router
