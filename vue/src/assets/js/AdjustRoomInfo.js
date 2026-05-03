import request from "@/utils/request";
const { ElMessage } = require("element-plus");
export default {
    name: "AdjustRoomInfo",
        data() {
        const checkApplyState = (rule, value, callback) => {
            if (value === "通过" || value === "驳回") {
                callback();
            } else {
                callback(new Error("请选择处理状态（通过/驳回）"));
            }
        };
        return {
            loading: true,
            dialogVisible: false,
            detailDialog: false,
            reviewDialog: false,
            search: "",
            currentPage: 1,
            pageSize: 10,
            total: 0,
            tableData: [],
            form: {},
            reviewForm: {},
            dormRoomId: 0,
            orderState: false,
            user: {},
            identity: '',
            dormBuildId: null,
            rules: {
                username: [
                    { required: true, message: "请输入学号", trigger: "blur" },
                    { pattern: /^[a-zA-Z0-9]{4,9}$/, message: "必须由 4 到 9 个字母或数字组成", trigger: "blur" },
                ],
                name: [
                    { required: true, message: "请输入姓名", trigger: "blur" },
                    { pattern: /^(?:[\u4E00-\u9FA5·]{2,10})$/, message: "必须由 2 到 10 个汉字组成", trigger: "blur" },
                ],
                currentRoomId: [
                    { required: true, message: "请输入当前房间号", trigger: "blur" },
                ],
                currentBedId: [
                    { required: true, message: "请输入当前床位号", trigger: "blur" },
                ],
                towardsRoomId: [
                    { required: true, message: "请输入目标房间号", trigger: "blur" },
                ],
                towardsBedId: [
                    { required: true, message: "请输入目标床位号", trigger: "blur" },
                ],
            },
        }
    },
    created() {
        this.init();
        this.load();
        this.loading = true;
        setTimeout(() => {
            this.loading = false;
        }, 1000);
    },
    methods: {
        init() {
            const identityStr = sessionStorage.getItem("identity");
            const userStr = sessionStorage.getItem("user");
            if (identityStr) {
                this.identity = JSON.parse(identityStr);
            }
            if (userStr) {
                this.user = JSON.parse(userStr);
                if (this.identity === 'dormManager' && this.user.dormBuildId) {
                    this.dormBuildId = this.user.dormBuildId;
                }
            }
        },
        judgeIdentity() {
            if (this.identity === 'stu') {
                return 0;
            } else if (this.identity === 'dormManager') {
                return 1;
            } else {
                return 2;
            }
        },
        // 状态映射：英文到中文
        stateMap(state) {
            const map = {
                'pending': '待审核',
                'approved': '审核通过',
                'rejected': '审核不通过',
                'in_progress': '处理中',
                'completed': '已完成',
                'execution_rejected': '拒绝执行'
            };
            // 兼容旧的中文状态
            if (!map[state]) {
                // 如果已经是中文状态，直接返回
                if (state === '未处理') return '待审核';
                if (state === '通过') return '审核通过';
                if (state === '驳回') return '审核不通过';
                if (state === '处理中') return '处理中';
                if (state === '已完成') return '已完成';
                if (state === '拒绝执行') return '拒绝执行';
                return state; // 返回原状态
            }
            return map[state];
        },
        // 获取状态标签类型（颜色）
        getStateType(state) {
            const typeMap = {
                'pending': 'warning',        // 待审核 - 橙色
                'approved': 'success',       // 审核通过 - 绿色
                'rejected': 'danger',        // 审核不通过 - 红色
                'in_progress': 'info',       // 处理中 - 蓝色
                'completed': '',             // 已完成 - 默认灰
                'execution_rejected': 'danger' // 拒绝执行 - 红色
            };
            // 兼容旧的中文状态
            if (state === '通过') return 'success';
            if (state === '驳回') return 'danger';
            if (state === '未处理') return 'warning';
            if (state === '处理中') return 'info';
            if (state === '已完成') return '';
            if (state === '拒绝执行') return 'danger';
            return typeMap[state] || '';
        },
        judgeIdentityForTemplate() {
            return this.judgeIdentity();
        },
        async load() {
            const params = {
                pageNum: this.currentPage,
                pageSize: this.pageSize,
                search: this.search,
            };

            // 根据身份查询不同数据
            if (this.judgeIdentity() === 1 && this.dormBuildId) {
                // 宿管：查询管辖楼栋的申请
                params.dormBuildId = this.dormBuildId;
                request.get("/adjustRoom/find", { params }).then((res) => {
                    if (res.code === "0") {
                        this.tableData = res.data.records || [];
                        this.total = res.data.total || 0;
                    } else {
                        this.tableData = [];
                        this.total = 0;
                        ElMessage({
                            message: res.msg || "查询失败",
                            type: "error",
                        });
                    }
                    this.loading = false;
                }).catch((error) => {
                    console.error("查询失败:", error);
                    ElMessage({
                        message: "查询失败，请稍后重试",
                        type: "error",
                    });
                    this.tableData = [];
                    this.total = 0;
                    this.loading = false;
                });
            } else if (this.judgeIdentity() === 2) {
                // 管理员：查询所有申请
                request.get("/adjustRoom/find", { params }).then((res) => {
                    if (res.code === "0") {
                        this.tableData = res.data.records || [];
                        this.total = res.data.total || 0;
                    } else {
                        this.tableData = [];
                        this.total = 0;
                        ElMessage({
                            message: res.msg || "查询失败",
                            type: "error",
                        });
                    }
                    this.loading = false;
                }).catch((error) => {
                    console.error("查询失败:", error);
                    ElMessage({
                        message: "查询失败，请稍后重试",
                        type: "error",
                    });
                    this.tableData = [];
                    this.total = 0;
                    this.loading = false;
                });
            } else {
                // 默认查询所有
                request.get("/adjustRoom/find", { params }).then((res) => {
                    if (res.code === "0") {
                        this.tableData = res.data.records || [];
                        this.total = res.data.total || 0;
                    } else {
                        this.tableData = [];
                        this.total = 0;
                        ElMessage({
                            message: res.msg || "查询失败",
                            type: "error",
                        });
                    }
                    this.loading = false;
                }).catch((error) => {
                    console.error("查询失败:", error);
                    ElMessage({
                        message: "查询失败，请稍后重试",
                        type: "error",
                    });
                    this.tableData = [];
                    this.total = 0;
                    this.loading = false;
                });
            }
        },
        reset() {
            this.search = '';
            this.load();
        },
        filterTag(value, row) {
            return row.state === value || this.stateMap(row.state) === this.stateMap(value);
        },
        judgeOrderState(state) {
            // 判断是否需要执行调宿操作（更新房间表）
            // 只有"通过"状态时才需要执行调宿操作（更新房间表）
            if (state === '通过' || state === 'approved') {
                this.orderState = true;
            } else {
                this.orderState = false;
            }
        },
        // 宿管审核通过
        reviewApproved() {
            const reviewTime = this.getCurrentTime();
            request.put("/adjustRoom/review/" + this.reviewForm.id, null, {
                params: {
                    state: 'approved',
                    reviewTime: reviewTime
                }
            }).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "审核通过",
                        type: "success",
                    });
                    this.reviewDialog = false;
                    this.load();
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        // 宿管审核驳回
        reviewRejected() {
            const reviewTime = this.getCurrentTime();
            request.put("/adjustRoom/review/" + this.reviewForm.id, null, {
                params: {
                    state: 'rejected',
                    reviewTime: reviewTime
                }
            }).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "审核不通过",
                        type: "success",
                    });
                    this.reviewDialog = false;
                    this.load();
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        // 统一获取系统时间的方法
        getCurrentTime() {
            const now = new Date();
            const year = now.getFullYear();
            const month = String(now.getMonth() + 1).padStart(2, '0');
            const day = String(now.getDate()).padStart(2, '0');
            const hours = String(now.getHours()).padStart(2, '0');
            const minutes = String(now.getMinutes()).padStart(2, '0');
            const seconds = String(now.getSeconds()).padStart(2, '0');
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        },
        // 管理员执行调宿操作
        save() {
            this.$refs.form.validate((valid) => {
                if (valid) {
                    // 管理员执行调宿：后端会在床位更新成功后把申请状态改为处理中
                    this.form.state = '处理中';
                    this.judgeOrderState('通过'); // 需要是"通过"状态才能执行调宿
                    // 提交修改请求
                    request.put("/adjustRoom/update/" + this.orderState, this.form).then((res) => {
                        if (res.code === "0") {
                            ElMessage({
                                message: "调宿执行成功，状态已变更为处理中",
                                type: "success",
                            });
                            this.load();
                            this.dialogVisible = false;
                        } else if (res.msg === "重复操作") {
                            ElMessage({
                                message: res.msg,
                                type: "error",
                            });
                            this.load();
                            this.dialogVisible = false;
                        } else {
                            ElMessage({
                                message: res.msg || "处理失败",
                                type: "error",
                            });
                        }
                    }).catch(() => {
                        ElMessage({
                            message: "网络错误，请重试",
                            type: "error",
                        });
                    });
                }
            });
        },
        // 管理员确认完成
        handleComplete(row) {
            const form = JSON.parse(JSON.stringify(row));
            form.state = '已完成';
            form.finishTime = this.getCurrentTime();
            request.put("/adjustRoom/update/false", form).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "调宿已完成",
                        type: "success",
                    });
                    this.load();
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        // 管理员拒绝执行已审核通过的调宿申请
        handleRejectExecute(row) {
            const form = JSON.parse(JSON.stringify(row));
            form.state = '拒绝执行';
            form.finishTime = this.getCurrentTime();
            request.put("/adjustRoom/update/false", form).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "已拒绝执行调宿",
                        type: "success",
                    });
                    this.load();
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        handleReview(row) {
            this.reviewDialog = true;
            this.reviewForm = {
                id: row.id,
                username: row.username,
                name: row.name
            };
        },
        closeReview() {
            this.reviewDialog = false;
        },
        cancel() {
            this.$refs.form.resetFields();
            this.dialogVisible = false;
            this.detailDialog = false;
        },
        showDetail(row) {
            this.detailDialog = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                this.form = JSON.parse(JSON.stringify(row));
            });
        },
        handleEdit(row) {
            // 管理员执行调宿操作
            this.dialogVisible = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                this.form = JSON.parse(JSON.stringify(row));
            });
        },
        async handleDelete(id) {
            request.delete("/adjustRoom/delete/" + id).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "删除成功",
                        type: "success",
                    });
                    this.load();
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        handleSizeChange(pageSize) {
            this.pageSize = pageSize;
            this.load();
        },
        handleCurrentChange(pageNum) {
            this.currentPage = pageNum;
            this.load();
        },
    },
}
