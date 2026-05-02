import request from "@/utils/request";
const { ElMessage } = require("element-plus");

export default {
    name: "RepairInfo",
    components: {},
    data() {
        return {
            loading: true,
            detailDialog: false,
            reviewDialog: false,
            search: "",
            currentPage: 1,
            pageSize: 10,
            total: 0,
            tableData: [],
            detail: {},
            reviewForm: {},
            user: {},
            identity: '',
            dormBuildId: null,
        };
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
        // 新增：统一获取系统时间的方法（格式：YYYY-MM-DD HH:mm:ss）
        getCurrentTime() {
            const now = new Date();
            const year = now.getFullYear();
            const month = String(now.getMonth() + 1).padStart(2, '0'); // 月份补0
            const day = String(now.getDate()).padStart(2, '0'); // 日期补0
            const hours = String(now.getHours()).padStart(2, '0'); // 小时补0
            const minutes = String(now.getMinutes()).padStart(2, '0'); // 分钟补0
            const seconds = String(now.getSeconds()).padStart(2, '0'); // 秒数补0
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        },

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

        stateMap(state) {
            const map = {
                'pending': '待审核',
                'approved': '审核通过',
                'rejected': '审核不通过',
                'in_progress': '处理中',
                'completed': '已完成'
            };
            return map[state] || state;
        },

        getStateType(state) {
            const typeMap = {
                'pending': 'warning',
                'approved': 'success',
                'rejected': 'danger',
                'in_progress': 'info',
                'completed': ''
            };
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

            if (this.judgeIdentity() === 1 && this.dormBuildId) {
                params.dormBuildId = this.dormBuildId;
            }

            this.loading = true;
            request.get("/repair/find", { params }).then((res) => {
                this.tableData = res.data.records;
                this.total = res.data.total;
                this.loading = false;
            });
        },

        reset() {
            this.search = ''
            this.load();
        },

        filterTag(value, row) {
            return row.state === value;
        },

        showDetail(row) {
            this.detailDialog = true;
            this.$nextTick(() => {
                this.detail = row;
            });
        },

        closeDetails() {
            this.detailDialog = false;
        },

        handleReview(row) {
            this.reviewDialog = true;
            this.reviewForm = {
                id: row.id,
                title: row.title
            };
        },

        // 宿管审核通过：添加审核时间
        reviewApproved() {
            // 获取当前系统时间作为审核时间
            const reviewTime = this.getCurrentTime();
            request.put("/repair/review/" + this.reviewForm.id, null, {
                params: {
                    state: 'approved',
                    reviewTime: reviewTime // 新增：传递审核时间到后端
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

        // 宿管审核驳回：添加审核时间
        reviewRejected() {
            // 获取当前系统时间作为审核时间
            const reviewTime = this.getCurrentTime();
            request.put("/repair/review/" + this.reviewForm.id, null, {
                params: {
                    state: 'rejected',
                    reviewTime: reviewTime // 新增：传递审核时间到后端
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

        closeReview() {
            this.reviewDialog = false;
        },

        // 管理员开始处理：添加开始处理时间
        handleEdit(row) {
            const form = JSON.parse(JSON.stringify(row));
            form.state = 'in_progress';
            form.processStartTime = this.getCurrentTime(); // 新增：开始处理时间
            request.put("/repair/update", form).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "开始处理订单",
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

        // 管理员标记完成：优化时间格式
        handleComplete(row) {
            const form = JSON.parse(JSON.stringify(row));
            form.state = 'completed';
            form.orderFinishTime = this.getCurrentTime(); // 使用统一时间方法
            request.put("/repair/update", form).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "标记为已完成",
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

        handleDelete(id) {
            request.delete("/repair/delete/" + id).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "删除成功",
                        type: "success",
                    });
                    this.search = "";
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
};