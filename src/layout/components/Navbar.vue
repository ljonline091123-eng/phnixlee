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
              :key="item.belongingOrgId"
              :label="item.minAccountFullName"
              :value="item.belongingOrgId">
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
    // org: {
    //   handler(val) {
    //     console.log(val)
    //     this.thridDeptId = val
    //   },
    //   immediate: true
    // },
    thridDeptId: {
      handler(val) {
        this.value = null
        this.getManagementOrgId(val)
        this.$store.commit("SET_ORG", val);
      },
      immediate: false
    }
  },
  methods: {
    //下拉选择监听
    bclxChange(val) {
      let obj = {};
      obj = this.options.find((item) => {
        return item.belongingOrgId === val;
      });
      this.$store.commit("SET_PROJECT", {
        code: obj.minAccountCode,
        id: obj.belongingOrgId,
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
        this.value = this.options[0]?.belongingOrgId;
        // * 同步要去加到vuex
        this.$store.commit("SET_PROJECT", {
          code: this.options[0]?.minAccountCode,
          id: this.value,
          name: this.options[0]?.minAccountFullName
        });
      });
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
          location.href = '/zhaocai';
        })
      }).catch(() => {
      });
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
  }
}
</style>
