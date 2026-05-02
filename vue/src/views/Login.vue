<template>
    <div class="login-wrapper">
        <!-- 背景图片 -->
        <div class="background-image"></div>
        
        <!-- 内容区域 -->
        <div class="login-container">
            <!-- 校徽和学校名称 -->
            <div class="school-header">
                <img src="/logo.png" alt="校徽" class="school-logo" />
                <div class="school-info">
                    <div class="school-name-cn">上海大学宿舍管理系统</div>
                    <div class="school-name-en">Shanghai University Dormitory Management System</div>
                </div>
            </div>
            
            <!-- 登录表单 -->
            <div class="login-form-wrapper">
                <div class="form-title">登录</div>
                <el-form ref="form" :model="form" :rules="rules" size="large" class="login-form">
                    <el-form-item prop="username">
                        <el-input v-model="form.username" clearable prefix-icon="User" placeholder="请输入用户名"></el-input>
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input v-model="form.password" prefix-icon="Lock" show-password placeholder="请输入密码"></el-input>
                    </el-form-item>
                    <el-form-item :model="form" prop="identity" class="identity-radio">
                        <el-radio v-model="form.identity" label="stu">学生</el-radio>
                        <el-radio v-model="form.identity" label="dormManager">宿舍管理员</el-radio>
                        <el-radio v-model="form.identity" label="admin">后勤管理中心</el-radio>
                    </el-form-item>
                    <el-form-item style="text-align: center; margin-top: 20px;">
                        <el-button :disabled="!disabled" type="primary" style="width: 100%; font-size: 16px;" @click="login">登 录</el-button>
                    </el-form-item>
                </el-form>
            </div>
        </div>
        
        <!-- 版权信息 -->
        <div class="footer">
            ©Copyright 2025-2026 上海大学 11小组
        </div>
    </div>
</template>
<script src="@/assets/js/Login.js"></script>
<style scoped>
.login-wrapper {
    position: relative;
    width: 100%;
    height: 100vh;
    overflow: hidden;
}

.background-image {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image: url('/login-background.jpg');
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    z-index: 0;
}

.login-container {
    position: relative;
    z-index: 1;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 40px;
}

.school-header {
    display: flex;
    align-items: center;
    margin-bottom: 40px;
    padding: 20px 40px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 15px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);
}

.school-logo {
    width: 80px;
    height: 80px;
    margin-right: 20px;
}

.school-info {
    display: flex;
    flex-direction: column;
}

.school-name-cn {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 5px;
}

.school-name-en {
    font-size: 14px;
    color: #606266;
}

.login-form-wrapper {
    background: rgba(255, 255, 255, 0.95);
    padding: 40px;
    border-radius: 15px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);
    width: 100%;
    max-width: 500px;
}

.form-title {
    font-size: 28px;
    font-weight: bold;
    color: #303133;
    text-align: center;
    margin-bottom: 30px;
}

.login-form {
    width: 100%;
}

.identity-radio {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-bottom: 10px;
}

.identity-radio :deep(.el-radio) {
    color: #606266;
    font-size: 16px;
}

.footer {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    text-align: center;
    color: #fff;
    padding: 20px 0;
    background: rgba(0, 0, 0, 0.3);
    backdrop-filter: blur(5px);
    font-size: 14px;
    z-index: 2;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .login-container {
        padding: 20px;
    }
    
    .school-header {
        flex-direction: column;
        text-align: center;
        padding: 20px;
    }
    
    .school-logo {
        margin-right: 0;
        margin-bottom: 15px;
    }
    
    .login-form-wrapper {
        padding: 30px 20px;
    }
    
    .identity-radio {
        flex-direction: column;
        gap: 10px;
    }
}
</style>
