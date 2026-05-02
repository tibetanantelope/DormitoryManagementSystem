import request from "@/utils/request";

const {ElMessage} = require("element-plus");

export default {
    name: "ApplyRepairInfo",
    components: {},
    data() {
        return {
            loading: true,
            dialogVisible: false,
            detailDialog: false,
            search: "",
            currentPage: 1,
            pageSize: 10,
            total: 0,
            tableData: [],
            detail: {},
            name: '',
            username: '',
            form: {
                repairer: '',
                dormBuildId: '',
                dormRoomId: '',
                title: '',
                content: '',
            },
            room: {
                dormRoomId: '',
                dormBuildId: '',
            },
            rules: {
                title: [{required: true, message: "请输入标题", trigger: "blur"}],
                content: [{required: true, message: "请输入内容", trigger: "blur"}],
            },
        };
    },
    created() {
        this.init()
        this.getInfo()
        this.load()
        this.loading = true
        setTimeout(() => {
            //设置延迟执行
            this.loading = false
        }, 1000);
    },
    methods: {
        init() {
            this.form = JSON.parse(sessionStorage.getItem("user"));
            this.name = this.form.name;
            this.username = this.form.username;
        },
        async load() {
            request.get("/repair/find/" + this.name, {
                params: {
                    pageNum: this.currentPage,
                    pageSize: this.pageSize,
                    search: this.search,
                },
            }).then((res) => {
                console.log(res);
                this.tableData = res.data.records;
                this.total = res.data.total;
                this.loading = false;
            });
        },
        getInfo() {
            request.get("/room/getMyRoom/" + this.username).then((res) => {
                if (res.code === "0") {
                    this.room = res.data;
                    console.log(this.room);
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        filterTag(value, row) {
            return row.state === value;
        },
        // 状态映射：英文到中文
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
        // 获取状态标签类型（颜色）
        getStateType(state) {
            const typeMap = {
                'pending': 'warning',        // 待审核 - 橙色
                'approved': 'success',       // 审核通过 - 绿色
                'rejected': 'danger',        // 审核不通过 - 红色
                'in_progress': 'info',       // 处理中 - 蓝色
                'completed': ''              // 已完成 - 默认灰
            };
            return typeMap[state] || '';
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
        add() {
            this.dialogVisible = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                this.form.repairer = this.name
                this.form.dormBuildId = this.room.dormBuildId
                this.form.dormRoomId = this.room.dormRoomId
            });
        },
        save() {
            this.$refs.form.validate(async (valid) => {
                if (valid) {
                    //自动设置报修申请时间
                    this.form.applyTime = new Date().toISOString().slice(0, 19).replace('T', ' ');
                    console.log(this.form)
                    await request.post("/repair/add", this.form).then((res) => {
                        if (res.code === "0") {
                            ElMessage({
                                message: "新增成功",
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
            })
        },
        cancel() {
            this.$refs.form.resetFields();
            this.dialogVisible = false;
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
};