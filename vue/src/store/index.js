import {createStore} from 'vuex'

export default createStore({
    state: {
        isLogin: false,
        identity: ''
    },
    mutations: {
        login(state) {
            state.isLogin = true
        }
    },
    actions: {},
    modules: {},
    computed: {
        userRole() {
            return this.$store.state.user.role; // 'dormManager' 或其他
        },
        isDormManager() {
            return this.userRole === 'dormManager';
        }
    }

})
