import Layout from '../layout/Layout.vue'
import {createRouter, createWebHistory} from "vue-router";

export const constantRoutes = [
    {
        path: '/Login',
        name: 'Login',
        component: () => import("@/views/Login"),
        meta: { title: '登录 - 宿舍管理系统' } // 登录页标签
    },
    {
        path: '/Layout',
        name: 'Layout',
        component: Layout,
        meta: { title: '宿舍管理系统' }, // 父路由默认标题（子路由会覆盖）
        children: [
            {path: '/home', name: 'Home', component: () => import("@/views/Home"), meta: { title: '首页 - 宿舍管理系统' }},
            {path: '/stuInfo', name: 'StuInfo', component: () => import("@/views/StuInfo"), meta: { title: '学生信息 - 宿舍管理系统' }},
            {path: '/dormManagerInfo', name: 'DormManagerInfo', component: () => import("@/views/DormManagerInfo"), meta: { title: '宿管信息 - 宿舍管理系统' }},
            {path: '/buildingInfo', name: 'BuildingInfo', component: () => import("@/views/BuildingInfo"), meta: { title: '楼宇信息 - 宿舍管理系统' }},
            {path: '/roomInfo', name: 'RoomInfo', component: () => import("@/views/RoomInfo"), meta: { title: '房间信息 - 宿舍管理系统' }},
            {path: '/noticeInfo', name: 'NoticeInfo', component: () => import("@/views/NoticeInfo"), meta: { title: '通知公告 - 宿舍管理系统' }},
            {path: '/adjustRoomInfo', name: 'AdjustRoomInfo', component: () => import("@/views/AdjustRoomInfo"), meta: { title: '调宿管理 - 宿舍管理系统' }},
            {path: '/repairInfo', name: 'RepairInfo', component: () => import("@/views/RepairInfo"), meta: { title: '维修管理 - 宿舍管理系统' }},
            {path: '/visitorInfo', name: 'VisitorInfo', component: () => import("@/views/VisitorInfo"), meta: { title: '访客登记 - 宿舍管理系统' }},
            {path: '/myRoomInfo', name: 'MyRoomInfo', component: () => import("@/views/MyRoomInfo"), meta: { title: '我的宿舍 - 宿舍管理系统' }},
            {path: '/applyRepairInfo', name: 'ApplyRepairInfo', component: () => import("@/views/ApplyRepairInfo"), meta: { title: '维修申请 - 宿舍管理系统' }},
            {path: '/applyChangeRoom', name: 'ApplyChangeRoom', component: () => import("@/views/ApplyChangeRoom"), meta: { title: '调宿申请 - 宿舍管理系统' }},
            {path: '/selfInfo', name: 'SelfInfo', component: () => import("@/views/SelfInfo"), meta: { title: '个人中心 - 宿舍管理系统' }},
        ]
    },
]

const router = createRouter({
    routes: constantRoutes,
    history: createWebHistory(process.env.BASE_URL)
})
// 路由守卫
router.beforeEach((to, from, next) => {
    const user = window.sessionStorage.getItem('user')

    // 动态设置页面标签标题
    if (to.meta.title) {
        document.title = to.meta.title; // 优先使用路由配置的标题
    } else {
        document.title = '宿舍管理系统'; // 默认标题兜底
    }

    // 原有鉴权逻辑保持不变
    if (to.path === '/Login') {
        return next();
    }
    if (!user) {
        return next('/Login')
    }
    if (to.path === '/' && user) {
        return next('/home')
    }
    next()
})

export default router