import request from "@/utils/request";

const {ElMessage} = require("element-plus");
export default {
    name: "AdjustRoomInfo",
    data() {
        const checkRoomState = (rule, value, callback) => {
            this.dormRoomId = value
            if (typeof value === "number") {
                request.get("/room/checkRoomState/" + value).then((res) => {
                    request.get("/room/checkRoomExist/" + value).then((result) => {
                        if (result.code === "-1") {
                            callback(new Error(result.msg));
                        }
                        if (res.code === "-1") {
                            callback(new Error(res.msg));
                        }
                        callback();
                    })
                });
            } else {
                callback(new Error("请输入正确的数据"));
            }
        };
        const checkBedState = (rule, value, callback) => {
            request.get("/room/checkBedState/" + this.dormRoomId + '/' + value).then((res) => {
                if (res.code === "0") {
                    callback();
                } else {
                    callback(new Error(res.msg));
                }
            });
        };
        return {
            loading: true,
            dialogVisible: false,
            detailDialog: false,
            search: "",
            currentPage: 1,
            pageSize: 10,
            total: 0,
            tableData: [],
            form: {},
            dormRoomId: 0,
            orderState: false,
            judgeOption: false,
            rules: {
                username: [
                    {required: true, message: "请输入学号", trigger: "blur"},
                    {pattern: /^[a-zA-Z0-9]{4,9}$/, message: "必须由 2 到 5 个字母或数字组成", trigger: "blur",},
                ],
                name: [
                    {required: true, message: "请输入姓名", trigger: "blur"},
                    {pattern: /^(?:[\u4E00-\u9FA5·]{2,10})$/, message: "必须由 2 到 10 个汉字组成", trigger: "blur",},
                ],
                currentRoomId: [
                    {required: true, message: "请输入当前房间号", trigger: "blur"},
                ],
                currentBedId: [
                    {required: true, message: "请输入当前床位号", trigger: "blur"},
                ],
                towardsRoomId: [
                    {validator: checkRoomState, trigger: "blur"},
                ],
                towardsBedId: [
                    {validator: checkBedState, trigger: "blur"},
                ],
            },
        }
    },
    created() {
        this.load();
        this.loading = true;
        setTimeout(() => {
            //设置延迟执行
            this.loading = false;
        }, 1000);
    },
    methods: {
        async load() {
            // 学生只能查看自己的调宿申请
            const user = JSON.parse(sessionStorage.getItem("user"));
            if (!user || !user.username) {
                ElMessage({
                    message: "用户信息获取失败，请重新登录",
                    type: "error",
                });
                this.loading = false;
                return;
            }
            this.loading = true;
            // 使用与报修申请相同的路径格式 /find/{username}
            const url = "/adjustRoom/find/" + encodeURIComponent(user.username);
            console.log("请求URL:", url);
            console.log("请求参数:", {
                pageNum: this.currentPage,
                pageSize: this.pageSize,
                search: this.search || '',
            });
            request.get(url, {
                params: {
                    pageNum: this.currentPage,
                    pageSize: this.pageSize,
                    search: this.search || '',
                },
            }).then((res) => {
                if (res.code === "0") {
                    this.tableData = res.data.records || [];
                    this.total = res.data.total || 0;
                } else {
                    ElMessage({
                        message: res.msg || "查询失败",
                        type: "error",
                    });
                    this.tableData = [];
                    this.total = 0;
                }
                this.loading = false;
            }).catch((error) => {
                console.error("查询调宿申请失败:", error);
                console.error("错误详情:", error.response);
                console.error("请求URL:", error.config?.url);
                console.error("请求方法:", error.config?.method);
                console.error("状态码:", error.response?.status);
                console.error("响应数据:", error.response?.data);
                ElMessage({
                    message: "查询失败，请稍后重试 (错误: " + (error.response?.status || error.message) + ")",
                    type: "error",
                });
                this.tableData = [];
                this.total = 0;
                this.loading = false;
            });
        },
        filterTag(value, row) {
            return row.state === value || this.stateMap(row.state) === this.stateMap(value);
        },
        add() {
            this.dialogVisible = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                this.form = {};
                const user = JSON.parse(sessionStorage.getItem("user"));
                this.form.username = user.username;
                this.form.name = user.name;
                // 数据库表目前只支持中文状态，暂时使用'未处理'，后续可改为英文状态
                this.form.state = '未处理'; // 默认状态为待审核
                request.get("/room/getMyRoom/" + this.form.username).then((res) => {
                    this.form.currentRoomId = res.data.dormRoomId
                    this.form.currentBedId = this.calBedNum(this.form.username, res.data)
                });
                this.judgeOption = true;
            });
        },
        calBedNum(username, data) {
            if (data.firstBed === username) {
                return 1;
            } else if (data.secondBed === username) {
                return 2;
            } else if (data.thirdBed === username) {
                return 3;
            } else if (data.fourthBed === username) {
                return 4;
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
            // 兼容中文状态
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
            // 兼容中文状态
            if (state === '通过') return 'success';
            if (state === '驳回') return 'danger';
            if (state === '未处理') return 'warning';
            if (state === '处理中') return 'info';
            if (state === '已完成') return '';
            if (state === '拒绝执行') return 'danger';
            return typeMap[state] || '';
        },
        judgeOrderState(state) {
            // 兼容旧的中文状态
            if (state === '通过' || state === 'approved') {
                this.orderState = true
            } else if (state === '驳回' || state === 'rejected') {
                this.orderState = false
            } else if (state === '未处理' || state === 'pending') {
                this.orderState = false
            }
        },
        save() {
            this.$refs.form.validate((valid) => {
                if (valid) {
                    if (this.judgeOption === false) {
                        //修改
                        this.judgeOrderState(this.form.state)
                        request.put("/adjustRoom/update/" + this.orderState, this.form).then((res) => {
                            if (res.code === "0") {
                                ElMessage({
                                    message: "修改成功",
                                    type: "success",
                                });
                                this.search = "";
                                this.load();
                                this.dialogVisible = false;
                            } else if (res.msg === "重复操作") {
                                ElMessage({
                                    message: res.msg,
                                    type: "error",
                                });
                                this.search = "";
                                this.load();
                                this.dialogVisible = false;
                            } else {
                                ElMessage({
                                    message: res.msg,
                                    type: "error",
                                });
                            }
                        });
                    } else if (this.judgeOption === true) {
                        //添加
                        // 添加操作：自动设置申请时间和状态
                        const now = new Date();
                        const year = now.getFullYear();
                        const month = String(now.getMonth() + 1).padStart(2, '0');
                        const day = String(now.getDate()).padStart(2, '0');
                        const hours = String(now.getHours()).padStart(2, '0');
                        const minutes = String(now.getMinutes()).padStart(2, '0');
                        const seconds = String(now.getSeconds()).padStart(2, '0');
                        this.form.applyTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
                        // 数据库表目前只支持中文状态，暂时使用'未处理'，后续可改为英文状态
                        this.form.state = '未处理'; // 默认状态为待审核
                        const addForm = JSON.parse(JSON.stringify(this.form));
                        delete addForm.id;
                        request.post("/adjustRoom/add", addForm).then((res) => {
                            if (res.code === "0") {
                                ElMessage({
                                    message: "添加成功",
                                    type: "success",
                                });
                                this.search = "";
                                this.load();
                                this.dialogVisible = false;
                            } else {
                                ElMessage({
                                    message: res.msg,
                                    type: "error",
                                });
                            }
                        });
                    }
                }
            });
        },
        cancel() {
            this.$refs.form.resetFields();
            this.dialogVisible = false;
            this.detailDialog = false;
        },
        showDetail(row) {
            // 查看详情
            this.detailDialog = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                this.form = JSON.parse(JSON.stringify(row));
            });
        },
        handleEdit(row) {
            //修改
            // 生拷贝
            this.dialogVisible = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                this.form = JSON.parse(JSON.stringify(row));
                this.judgeOption = false;
            });
        },
        handleSizeChange(pageSize) {
            //改变每页个数
            this.pageSize = pageSize;
            this.load();
        },
        handleCurrentChange(pageNum) {
            //改变页码
            this.currentPage = pageNum;
            this.load();
        },
    },
}
