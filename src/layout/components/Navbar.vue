<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container"
               @toggleClick="toggleSideBar"/>

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav"/>
    <top-nav id="topmenu-container" class="topmenu-container" v-if="topNav"/>

    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <div class="right-menu1">
          <!-- <treeselect
          v-model="deptId"
          style="width: 150px; z-index: 9999;height: 40px;"
          :options="deptOptions"
          placeholder="请选择"
          /> -->
          <!-- <el-cascader
          ref="refHandle"
          v-model="thridDeptId"
          :props="{label:'label',value:'thridDeptId'}"
          :options="deptOptions" :show-all-levels="false"  @change="handleChange"></el-cascader> -->
          <el-cascader
            v-model="thridDeptId"
            :options="deptOptions"
            :show-all-levels="false"
            :props="{
          label: 'label',
          value: 'thridDeptId',
        }"
            @change="handleChange"
          >
          </el-cascader>
        </div>
        <div>
          <el-select @change="bclxChange" v-model="value" placeholder="请选择"
                     style="width: 150px; height: 40px;">
            <el-option
              v-for="item in options"
              :key="item.minAccountCode"
              :label="item.minAccountFullName"
              :value="item.minAccountCode">
            </el-option>
          </el-select>
        </div>
        <search id="header-search" class="right-menu-item"/>

        <!--        <el-tooltip content="源码地址" effect="dark" placement="bottom">-->
        <!--          <ruo-yi-git id="ruoyi-git" class="right-menu-item hover-effect" />-->
        <!--        </el-tooltip>-->

        <!--        <el-tooltip content="文档地址" effect="dark" placement="bottom">-->
        <!--          <ruo-yi-doc id="ruoyi-doc" class="right-menu-item hover-effect" />-->
        <!--        </el-tooltip>-->

        <screenfull id="screenfull" class="right-menu-item hover-effect"/>

        <!--        <el-tooltip content="布局大小" effect="dark" placement="bottom">-->
        <!--          <size-select id="size-select" class="right-menu-item hover-effect" />-->
        <!--        </el-tooltip>-->
        <div class="navbar-message" v-on:click="showList">
          <el-badge :value="xxsl" class="item">
            <i class="el-icon-message-solid" style="color: #2b4acb"></i>
          </el-badge>
        </div>
      </template>

      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar">
          <i class="el-icon-caret-bottom"/>
        </div>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>个人中心</el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setting = true">
            <span>布局设置</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 添加或修改消息信息对话框 -->
    <el-dialog title="消息列表"
               :visible.sync="dialogVisible"
               width="900px"
               :close-on-click-modal="false"
               :close-on-press-escape="false"
               :show-close="true"
               @closed="getXxsl">
      <div>
        <el-table v-loading="loading" :data="jtMsgPerList" class="aaa" style="height: calc(100% - 30px)">
          <!--          <el-table-column label="标题" prop="title"/>-->
          <el-table-column label="内容" prop="messageTitle" align="center"/>
          <!--          <el-table-column label="消息类型" prop="msgType"/>-->
          <!--          <el-table-column label="备注" prop="remark"/>-->
          <el-table-column label="操作" class-name="small-padding fixed-width" align="center" width="180">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                @click="messageHandle(scope.row)"
              >详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="msgList"
        />
      </div>
    </el-dialog>

    <el-dialog title="消息详情"
               :visible.sync="messageFromVisible"
               width="600px"
               :close-on-click-modal="false"
               :close-on-press-escape="false"
               :show-close="true"
               @closed="msgList">
      <div>
        <el-form :model="messageFrom" label-width="100px">
          <el-form-item label="消息标题">
            <el-input v-model="messageFrom.messageTitle" :disabled="true" />
          </el-form-item>
          <el-form-item label="消息内容">
            <el-input v-model="messageFrom.messageContent"  type="textarea" :rows="3" :disabled="true" />
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {mapGetters} from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import RuoYiGit from '@/components/RuoYi/Git'
import RuoYiDoc from '@/components/RuoYi/Doc'
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import {getDeptTree, getManagementOrgId} from "@/api/system/dept";
import {Notification} from "element-ui";
import {getUserProfile,getMyList,messageRead} from "@/api/system/user";
import {getUnifiedLoginUrl, removePortalToken} from "@/utils/auth";

export default {
  components: {
    Breadcrumb,
    TopNav,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    RuoYiGit,
    RuoYiDoc,
    Treeselect
  },
  data() {
    return {
      options: [{
        value: '选项1',
        label: '黄金糕'
      }, {
        value: '选项2',
        label: '双皮奶'
      }, {
        value: '选项3',
        label: '蚵仔煎'
      }, {
        value: '选项4',
        label: '龙须面'
      }, {
        value: '选项5',
        label: '北京烤鸭'
      }],
      value: '',
      thridDeptId: '',
      deptOptions: [],
      // 遮罩层
      loading: false,
      dialogVisible: false,
      // 选中数组
      ids: [],
      // 总条数
      total: 0,
      jtMsgPerList: [],
      userId: null,
      xxsl: null,
      message: "",
      text_content: "",
      ws: null,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        msgMan: null
      },
      // 表单参数
      form: {},
      messageFrom: {},
      messageFromVisible: false,
    };
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'project',
      'org'
    ]),
    // thridDeptId() {
    //   return [1000000，200001]
    // },
    // project:
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'showSettings',
          value: val
        })
      }
    },
    topNav: {
      get() {
        return this.$store.state.settings.topNav
      }
    }
  },
  created() {
    this.getDeptTree()
    // this.thridDeptId=this.org
    // this.getManagementOrgId(this.thridDeptId)
    this.value = this.project.name
    // * 取登录后用户的信息 userInfo // * org ,project
    // * thridDeptId,project

  },
  watch: {
    // 单位&项目选择框联动：页面(如报表穿透)写入 vuex 的 org 后同步级联选择器
    org: {
      handler(val) {
        if (!val) return;
        const path = Array.isArray(val) ? val : this.findDeptPath(this.deptOptions, val, []);
        if (path && path.length && JSON.stringify(path) !== JSON.stringify(this.thridDeptId)) {
          this.thridDeptId = path;
        }
      },
    },
    // 项目选择框联动：页面(如报表穿透)写入 vuex 的 project 后同步下拉
    project: {
      handler(newVal) {
        if (newVal && newVal.code) this.value = newVal.code;
      },
    },
    thridDeptId: {
      handler(val) {
        this.value = null
        this.getManagementOrgId(val)
        this.$store.commit("SET_ORG", val);
      },
      immediate: false
    }
  },
  mounted() {
    getUserProfile().then(response => {
      this.userId = response.data.userId;
      //this.deptId = response.data.deptId;
      let verify = this.$route.query.verify;
      if(verify == 'Y'|| verify == undefined){
        this.getMyUser();
      }
    });
    // 绑定事件
    /*this.$bus.$on('lufei', (data)=>{
      this.queryParams.msgMan = this.userId;
      getMyList(this.queryParams).then(response => {
        if (response.total === 0) {
          this.xxsl = null;
        } else {
          this.xxsl = response.total;
        }
      })
    })*/
  },
  methods: {
    //下拉选择监听
    bclxChange(val) {
      let obj = {};
      obj = this.options.find((item) => {
        return item.minAccountCode === val;
      });
      this.$store.commit("SET_PROJECT", {
        code: obj.minAccountCode,
        id: obj.minAccountCode,
        name: obj.minAccountFullName
      });
      // this.$store.commit("SET_PROJECT", {code:this.options[0].minAccountCode,id:this.value,name:this.options[0].minAccountFullName});
    },
    handleChange(value) {
      this.$store.commit("SET_ORG", value);
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      getDeptTree().then((response) => {
        this.deptOptions = response.data;
        if (response.data[0] != null) {
          if (response.data[0].children != null) {
            this.thridDeptId = [response.data[0]?.thridDeptId, response.data[0]?.children[0]?.thridDeptId];
          } else {
            this.thridDeptId = [response.data[0]?.thridDeptId];
          }
        }
      });
    },
    // 查询项目
    async getManagementOrgId(id) {
      const needId = id[id.length - 1]
      const res = await getManagementOrgId(needId).then((response) => {
        this.options = response.data;
        // 若 store 里已选中项目仍在当前单位项目列表内，则保留，避免报表穿透后被顶掉
        const current = this.$store.state.project.project;
        const keep = current && current.code
          ? response.data.find(o => o.minAccountCode === current.code)
          : null;
        const picked = keep || response.data[0];
        this.value = picked && picked.minAccountCode;
        // * 同步要去加到vuex
        this.$store.commit("SET_PROJECT", {
          code: picked && picked.minAccountCode,
          id: this.value,
          name: picked && picked.minAccountFullName
        });
      });
    },
    // 在单位树里按 thridDeptId 找级联路径(与 el-cascader 的 v-model 数组格式一致)
    findDeptPath(nodes, thridDeptId, path) {
      for (const n of (nodes || [])) {
        const p = path.concat(n.thridDeptId);
        if (n.thridDeptId === thridDeptId) return p;
        if (n.children && n.children.length) {
          const r = this.findDeptPath(n.children, thridDeptId, p);
          if (r && r.length) return r;
        }
      }
      return [];
    },
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          // 统一登录：退出后跳回供应商端统一登录页
          removePortalToken()
          location.href = getUnifiedLoginUrl()
        })
      }).catch(() => {
      });
    },
    showList() {
      this.msgList();
      this.dialogVisible = true; // show(title, obj); obj参数是可传入的参数
    },
    msgList() {
      this.queryParams.msgMan = this.userId;
      //this.queryParams.lockUnit = this.deptId;
      this.loading = true;
      getMyList(this.queryParams).then(response => {
        this.jtMsgPerList = response.rows;
        this.total = response.total;
        this.loading = false;
      })
    },
    getXxsl() {
      const _this = this;
      this.queryParams.msgMan = this.userId;
      //this.queryParams.lockUnit = this.deptId;
      getMyList(this.queryParams).then(response => {
        if (response.total === 0) {
          this.xxsl = null;
        } else {
          if (this.xxsl != null && this.xxsl < response.total) {
            Notification.info({
              title: "新消息",
              dangerouslyUseHTMLString: true,
              message: "您有新的消息,请注意查收。",
              duration: 3000,
              offset: 40,
              onClick: function () {
                _this.showList()
              },
            });
          }
          this.xxsl = response.total;
        }
      })
    },
    getMyUser() {
      this.getXxsl();
      setTimeout(() => {
        this.getMyUser()
      }, 300000)//300秒查一下
    },
    messageHandle(row) {
      this.messageFrom = row;
      messageRead(row.id);
      this.messageFromVisible = true;
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu1 {
    width: 180px;
    margin-right: 8px;
  }

  .right-menu {
    float: right;
    height: 100%;
    display: flex;
    line-height: 50px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025)
        }
      }
    }

    .avatar-container {
      margin-right: 30px;

      .avatar-wrapper {
        margin-top: 5px;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }

    .navbar-message {
      margin-left: 1.3021vw;
      margin-right: 1.3021vw;
      ::v-deep .el-badge__content.is-fixed {
        top: 30%;
      }

      ::v-deep .el-icon-message-solid:before {
        font-size: 1.3021vw;
      }
    }
  }
}
</style>
