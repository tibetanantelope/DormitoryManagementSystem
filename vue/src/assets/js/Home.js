import weather from "@/components/weather";
import Calender from "@/components/Calendar";
import request from "@/utils/request";
import home_echarts from "@/components/home_echarts";

export default {
    name: "Home",
    components: {
        weather,
        Calender,
        home_echarts,
    },
    data() {
        return {
            studentNum: "",
            haveRoomStudentNum: "",
            detailDialog: false,
            repairOrderNum: "",
            noFullRoomNum: "",
            activities: [],
            noticeDetail: {},           // 保留原对象
            showNoticeDialog: false,    // 控制弹窗
            plainTextContent: ''        // 转换后的纯文本内容（带段落换行）

        };
    },
    created() {
        this.getHomePageNotice();
        this.getStuNum();
        this.getHaveRoomNum();
        this.getOrderNum();
        this.getNoFullRoom();
    },
    methods: {
        async getStuNum() {
            request.get("/stu/stuNum").then((res) => {
                if (res.code === "0") {
                    this.studentNum = res.data;
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        async getHaveRoomNum() {
            request.get("/room/selectHaveRoomStuNum").then((res) => {
                if (res.code === "0") {
                    this.haveRoomStudentNum = res.data;
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        async getOrderNum() {
            request.get("/repair/orderNum").then((res) => {
                if (res.code === "0") {
                    this.repairOrderNum = res.data;
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        async getNoFullRoom() {
            request.get("/room/noFullRoom").then((res) => {
                if (res.code === "0") {
                    this.noFullRoomNum = res.data;
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        async getHomePageNotice() {
            request.get("/notice/homePageNotice").then((res) => {
                if (res.code === "0") {
                    this.activities = res.data;
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },

        openNoticeDetail(id) {
            request.get(`/notice/detail/${id}`).then((res) => {
                if (res.code === "0") {
                    const notice = res.data || {};
                    this.noticeDetail = { ...notice };
                    // 把 HTML 转为纯文本并保留段落
                    this.noticeDetail.content = this.htmlToPlainText(notice.content || '');
                    this.showNoticeDialog = true;
                } else {
                    ElMessage({
                        message: res.msg || '获取公告详情失败',
                        type: "error",
                    });
                }
            }).catch(err => {
                console.error(err);
                ElMessage({ message: '请求失败', type: 'error' });
            });
        },

        // 工具：把 HTML 转为纯文本并保留段落换行
        htmlToPlainText(html) {
            if (!html) return '';

            // 1) 把常见的换行/段落标签替换成换行符（先替换 <br> 再替换 </p>、</div>）
            let s = String(html)
                .replace(/<\s*br\s*\/?\s*>/gi, '\n')
                .replace(/<\/p\s*>/gi, '\n\n')
                .replace(/<\/div\s*>/gi, '\n\n');

            // 2) 去掉开始标签 <p ...> <div ...> 等
            s = s.replace(/<\s*(p|div|span|strong|em|b|i|u|h[1-6])[^>]*>/gi, '');

            // 3) 把 HTML 实体先用 DOM 解码（更可靠），再去掉剩余标签
            try {
                const tmp = document.createElement('div');
                tmp.innerHTML = s;
                let text = tmp.textContent || tmp.innerText || '';
                // 4) 合并过多空行（保留最多两个换行）
                text = text.replace(/\n{3,}/g, '\n\n');
                return text.trim();
            } catch (e) {
                // 退化方案：用正则替换实体并去掉标签
                let text = s.replace(/&nbsp;/g, ' ')
                    .replace(/&lt;/g, '<')
                    .replace(/&gt;/g, '>')
                    .replace(/&amp;/g, '&');
                text = text.replace(/<[^>]+>/g, '');
                text = text.replace(/\n{3,}/g, '\n\n');
                return text.trim();
            }
        },

    },
};