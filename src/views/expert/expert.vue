<template>
  <div class="app-container">
    <div class="split-page-box flex full">
      <Drag>
        <template v-slot:left-content>
          <div class="left">
            <el-input
              v-model="deptName"
              placeholder="请输入部门名称"
              clearable
              size="small"
              prefix-icon="el-icon-search"
              style="margin-bottom: 12px"
            />
            <el-tree
              :data="deptOptions"
              class="tree_expert"
              :props="defaultProps"
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              ref="tree"
              node-key="id"
              default-expand-all
              highlight-current
              @node-click="handleNodeClick"
            />
          </div>
        </template>

        <template v-slot:right-content>
          <div class="right">
            <el-form
              :model="queryParams"
              ref="queryForm"
              size="small"
              :inline="true"
              v-show="showSearch"
              label-width="68px"
            >
              <el-form-item label="专家姓名" prop="expertName">
                <el-input
                  v-model="queryParams.expertName"
                  placeholder="请输入专家姓名"
                  clearable
                  style="width: 240px"
                  @keyup.enter.native="handleQuery"
                />
              </el-form-item>
              <el-form-item label="手机号码" prop="expertPhone">
                <el-input
                  v-model="queryParams.expertPhone"
                  placeholder="请输入手机号码"
                  clearable
                  style="width: 240px"
                  @keyup.enter.native="handleQuery"
                />
              </el-form-item>
              <el-form-item label="专家类别" prop="status">
                <el-select
                  v-model="queryParams.expertType"
                  placeholder="请选择专家类别"
                  clearable
                  style="width: 100%"
                >
                  <el-option
                    v-for="dict in dict.type.expert_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  icon="el-icon-search"
                  size="small"
                  @click="handleQuery"
                  >查询</el-button
                >
                <el-button
                  type="success"
                  icon="el-icon-plus"
                  size="small"
                  @click="handleAdd"
                  v-hasPermi="['expert:expert:add']"
                  >新增</el-button
                >
              </el-form-item>
            </el-form>

            <el-table
              v-loading="loading"
              :data="expertList"
              @selection-change="handleSelectionChange"
              highlight-current-row
              border
              :header-cell-style="{ background: '#F3F2F8' }"
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column
                label="专家姓名"
                align="center"
                key="expertName"
                prop="expertName"
              />
              <el-table-column
                label="手机号码"
                align="center"
                key="expertPhone"
                prop="expertPhone"
                width="120"
              />
              <el-table-column
                label="组织机构"
                align="center"
                key="belongOrganization"
                prop="belongOrganization"
                show-overflow-tooltip
              />
              <el-table-column
                label="工作部门"
                align="center"
                key="department"
                prop="department"
                show-overflow-tooltip
              />
              <el-table-column
                label="学历"
                align="center"
                key="educationDegreeText"
                prop="educationDegreeText"
              />
              <el-table-column
                label="专业"
                align="center"
                key="major"
                prop="major"
              />
              <el-table-column
                label="业态"
                align="center"
                key="businessTypeText"
                prop="businessTypeText"
              />
              <el-table-column
                label="专家类别"
                align="center"
                key="expertTypeText"
                prop="expertTypeText"
              />
              <el-table-column  width="200px" label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                
              <el-button
                type="text"
                size="small"
                @click="checkExpert(scope.row)"
                >查看</el-button>
              <el-button
              v-if="![1].includes(scope.row.state)"
                type="text"
                size="small"
                @click="editExpert(scope.row)"
                >编辑</el-button>
                <el-button
                v-if="[1].includes(scope.row.state)"
                type="text"
                size="small"
                @click="checkExpert(scope.row)"
                >审批</el-button
              >
                </template>
              </el-table-column>
              <el-table-column
                label="账号状态"
                width="150"
                align="center"
                key="expertState"
                prop="expertState"
              >
                <template slot-scope="{ row }">
                  <el-switch
                    :value="row.expertState === 1 ? true : false"
                    active-text="启用"
                    inactive-text="禁用"
                    @change="updateDeposit(row)"
                    :disabled="
                      !$store.getters.permissions.includes(
                        'expert:expert:setting'
                      ) && !$store.getters.permissions.includes('*:*:*')
                    "
                    v-hasPermi="['expert:expert:setting']"
                  >
                  </el-switch>
                </template>
              </el-table-column>
            </el-table>
            <pagination
              v-show="total > 0"
              :total="total"
              :page.sync="queryParams.pageNumber"
              :limit.sync="queryParams.pageSize"
              @pagination="getExpertList"
            />
          </div>
        </template>
      </Drag>
      <!-- <div class="left" style="width: 300px;">
        <el-input v-model="deptName" placeholder="请输入部门名称" clearable size="small" prefix-icon="el-icon-search"
              style="margin-bottom: 12px" />
        <el-tree  :data="deptOptions" class="tree_expert" :props="defaultProps" :expand-on-click-node="false"
        :filter-node-method="filterNode" ref="tree" node-key="id" default-expand-all highlight-current 
        @node-click="handleNodeClick" />
      </div>
      <div class="right fill">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch"
          label-width="68px">
          <el-form-item label="专家姓名" prop="expertName">
            <el-input v-model="queryParams.expertName" placeholder="请输入专家姓名" clearable style="width: 240px"
              @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="手机号码" prop="expertPhone">
            <el-input v-model="queryParams.expertPhone" placeholder="请输入手机号码" clearable style="width: 240px"
              @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="专家类别" prop="status">
            <el-select v-model="queryParams.expertType" placeholder="请选择专家类别" style="width:100%">
                <el-option v-for="dict in dict.type.expert_type" :key="dict.value" :label="dict.label"
                  :value="dict.value"></el-option>
              </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
            <el-button type="success" icon="el-icon-plus" size="small" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="expertList" @selection-change="handleSelectionChange" 
          highlight-current-row
          border
          :header-cell-style="{background:'#F3F2F8',}">
          <el-table-column label="序号" type="index" width="50" align="center" />
          <el-table-column label="专家姓名" align="center" key="expertName" prop="expertName" />
          <el-table-column label="手机号码" align="center" key="expertPhone" prop="expertPhone" width="120" />
          <el-table-column label="组织机构" align="center" key="belongOrganization" prop="belongOrganization" show-overflow-tooltip/>
          <el-table-column label="工作部门" align="center" key="department" prop="department" show-overflow-tooltip/>
          <el-table-column label="学历" align="center" key="educationDegreeText" prop="educationDegreeText" />
          <el-table-column label="专业" align="center" key="major" prop="major" />
          <el-table-column label="业态" align="center" key="businessTypeText" prop="businessTypeText" />
          <el-table-column label="专家类别" align="center" key="expertTypeText" prop="expertTypeText" />
          <el-table-column label="账号状态" width="150" align="center" key="expertState" prop="expertState" >
            <template slot-scope="{row}">
                <el-switch :value="row.expertState !== 1? true : false" active-text="启用" inactive-text="禁用"
                  @change="updateDeposit(row)">
                </el-switch>
              </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize" @pagination="getExpertList" />
      </div> -->
    </div>
    <!-- 添加专家 -->
    <el-dialog
      title="选择专家入库"
      v-if="open"
      :visible.sync="open"
      width="60%"
      append-to-body
    >
      <div class="split-page-box flex full">
        <Drag box="box1">
          <template v-slot:left-content>
            <div class="left">
              <el-input
                v-model="deptName"
                placeholder="请输入部门名称"
                clearable
                size="small"
                prefix-icon="el-icon-search"
                style="margin-bottom: 12px"
              />
              <el-tree
                :data="deptOptions"
                :props="defaultProps"
                class="tree_expert"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                node-key="id"
                default-expand-all
                highlight-current
                @node-click="handleNodeClickTwo"
              />
            </div>
          </template>
          <template v-slot:right-content>
            <div class="right fill">
              <el-form
                :model="selectQuery"
                ref="vForm"
                label-position="left"
                label-width="80px"
                size="small"
                @submit.native.prevent
              >
                <el-row>
                  <el-col :span="10" class="grid-cell">
                    <el-form-item
                      label="专家姓名"
                      prop="expertName"
                      class="label-right-align"
                    >
                      <el-input
                        v-model="selectQuery.nickName"
                        type="text"
                        clearable
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4" class="grid-cell">
                    <div class="static-content-item" style="margin-left: 12px">
                      <el-button
                        type="primary"
                        icon="el-icon-search"
                        size="small"
                        @click="notLibraryQuery"
                        >搜索</el-button
                      >
                    </div>
                  </el-col>
                </el-row>
              </el-form>
              <el-table
                v-loading="tpExpertLoading"
                :data="tpExpertList"
                stripe
                highlight-current-row
                @row-click="selectExpert"
                border
              >
                <el-table-column label="" width="30" align="center">
                  <template slot-scope="scope">
                    <el-radio
                      class="table_radio"
                      v-model="departmentId"
                      :label="scope.row.userId"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  label="序号"
                  type="index"
                  width="50"
                  align="center"
                />
                <el-table-column
                  label="专家姓名"
                  align="center"
                  key="nickName"
                  prop="nickName"
                />
                <el-table-column
                  label="手机号码"
                  align="center"
                  key="phonenumber"
                  prop="phonenumber"
                />
                <el-table-column
                  label="组织机构"
                  align="center"
                  key="thridOrgName"
                  prop="thridOrgName"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="工作部门"
                  align="center"
                  key="deptName"
                  show-overflow-tooltip
                >
                  <template slot-scope="{ row }">
                    {{ (row.dept && row.dept.deptName) || "" }}
                  </template>
                </el-table-column>
              </el-table>
              <pagination
                v-show="expertTotal > 0"
                :total="expertTotal"
                :page.sync="selectQuery.pageNum"
                :limit.sync="selectQuery.pageSize"
                @pagination="getListUser"
                :page-sizes="[10, 20, 30, 40]"
              />
            </div>
          </template>
        </Drag>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelExpert" size="small" style="width: 100px"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="confirmExpert"
          size="small"
          style="width: 100px"
          >确 定</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getExpertList } from "@/api/expert/expert";
import { Base64 } from "js-base64";
import {
  listUser,
  getUser,
  delUser,
  addUser,
  updateUser,
  resetUserPwd,
  changeUserStatus,
  deptTreeSelect,
  deptTreeCondSelect,
  postStatus,
} from "@/api/system/user";
import { getToken } from "@/utils/auth";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import Drag from "@/components/Drag/index.vue";

export default {
  name: "Expert",
  dicts: ["sys_normal_disable", "sys_user_sex", "expert_type"],
  components: { Treeselect, Drag },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 弹出层标题
      title: "",
      // 部门树选项
      deptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 部门名称
      deptName: undefined,
      // 默认密码
      initPassword: undefined,
      // 日期范围
      dateRange: [],
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "label",
      },
      expertList: [],
      tpExpertList: [],
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        expertName: undefined,
        expertPhone: undefined,
        expertType: undefined,
        deptId: undefined,
      },
      selectQuery: {
        nickName: undefined,
        deptId: undefined,
        userType: "purchase",
        pageNum: 1,
        pageSize: 10,
      },
      tpExpertLoading: false,
      departmentId: "",
      checkedItem: {},
      expertTotal: 0,
      isFirstLoad: true, // 标志位，初始值为 true
      isSelectQuery: true,
    };
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    },
  },
  created() {
    this.getExpertList();
    this.getDeptTree();
    // this.getConfigKey("sys.user.initPassword").then(response => {
    //   this.initPassword = response.msg;
    // });
  },
  methods: {
      /** 查看专家 checkExpert，editExpert，confirmApprove*/
      checkExpert(row) {
        console.log(JSON.stringify(row))
        row.type='check'
      let param = Base64.encode(JSON.stringify(row));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/expert/add-expert/${param}`);
    },
    editExpert(row) {
        row.type='edit'
      let param = Base64.encode(JSON.stringify(row));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/expert/add-expert/${param}`);
    },
  
    /** 获取已入库列表 */
    async getExpertList() {
      this.loading = true;
      if (this.isFirstLoad) {
        this.queryParams.deptId =
          this.$store.state.user?.userInfo.thridOrgDeptId;
        this.isFirstLoad = false; // 将标志位设置为 false，确保后续不再赋值
      }
      try {
        const res = await getExpertList(this.queryParams);
        this.loading = false;
        this.expertList = res.data.rows;
        this.total = res.data.total;
      } catch (err) {
        this.loading = true;
        console.log(err);
      }
      // listUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
      //     this.userList = response.rows;
      //     this.total = response.total;
      //     this.loading = false;
      //   }
      // );
    },
    /** 获取未入库列表 */
    async getListUser() {
      this.tpExpertLoading = true;
      if (this.isSelectQuery) {
        this.selectQuery.deptId =
          this.$store.state.user?.userInfo.thridOrgDeptId;
        this.isSelectQuery = false;
      }
      try {
        const res = await listUser(this.selectQuery);
        this.tpExpertList = res.rows;
        this.expertTotal = res.total;
        this.tpExpertLoading = false;
        console.log(res, "三方");
      } catch (err) {
        console.log(err);
        this.tpExpertLoading = false;
      }
    },
    /** 选择专家 */
    selectExpert(row) {
      this.departmentId = row.userId;
      this.checkedItem = row;
    },
    /** 确定选择专家 */
    confirmExpert() {
      const { checkedItem } = this;
      console.log(checkedItem, "checkedItem");
      if (!checkedItem.userId) return this.$message.error("请先选择一位专家");
      console.log(param, "param~~~~~~~~~~~~~");
      getExpertList({ userId: checkedItem.userId }).then((res) => {
        if (res.data.total > 0)
          return this.$message.error("该专家已入库，请重新选择");
      });

      this.open = false;
      console.log(JSON.stringify(checkedItem))
      let param = Base64.encode(JSON.stringify(checkedItem));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/expert/add-expert/${param}`);
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeCondSelect({
        thridDeptId: this.$store.state.user?.userInfo.thridOrgId,
      }).then((response) => {
        this.deptOptions = response.data;
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id;
      this.handleQuery();
    },
    handleNodeClickTwo(data) {
      this.selectQuery.deptId = data.id;
      this.notLibraryQuery();
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal
        .confirm('确认要"' + text + '""' + row.userName + '"用户吗？')
        .then(function () {
          return changeUserStatus(row.userId, row.status);
        })
        .then(() => {
          this.$modal.msgSuccess(text + "成功");
        })
        .catch(function () {
          row.status = row.status === "0" ? "1" : "0";
        });
    },
    // 取消按钮
    cancelExpert() {
      this.open = false;
      this.checkedItem = {};
      this.departmentId = "";
    },
    /** 搜索已入库专家 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getExpertList();
    },
    /** 搜索未入库专家 */
    notLibraryQuery() {
      this.selectQuery.pageNum = 1;
      this.getListUser();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.userId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },

    /** 新增按钮操作 */
    async handleAdd() {
      this.open = true;
      this.getListUser();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const userId = row.userId || this.ids;
      getUser(userId).then((response) => {
        this.form = response.data;
        this.postOptions = response.posts;
        this.roleOptions = response.roles;
        this.$set(this.form, "postIds", response.postIds);
        this.$set(this.form, "roleIds", response.roleIds);
        this.open = true;
        this.title = "修改用户";
        this.form.password = "";
      });
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$prompt('请输入"' + row.userName + '"的新密码', "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        closeOnClickModal: false,
        inputPattern: /^.{5,20}$/,
        inputErrorMessage: "用户密码长度必须介于 5 和 20 之间",
        inputValidator: (value) => {
          if (/<|>|"|'|\||\\/.test(value)) {
            return "不能包含非法字符：< > \" ' \\\ |";
          }
        },
      })
        .then(({ value }) => {
          resetUserPwd(row.userId, value).then((response) => {
            this.$modal.msgSuccess("修改成功，新密码是：" + value);
          });
        })
        .catch(() => {});
    },
    /** 分配角色操作 */
    handleAuthRole: function (row) {
      const userId = row.userId;
      this.$router.push("/system/user-auth/role/" + userId);
    },
    //启用禁用账号
    updateDeposit(row) {
      const userStatus = row.expertState === 1 ? 2 : 1;
      const statusTitle = userStatus === 1 ? "启用" : "禁用";
      this.$confirm(`是否${statusTitle}该账号?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(async () => {
          await postStatus(row.id, userStatus);
          this.$message({
            type: "success",
            message: `${statusTitle}成功!`,
          });
          this.getExpertList();
        })
        .catch(() => {});
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          if (this.form.userId != undefined) {
            updateUser(this.form).then((response) => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addUser(this.form).then((response) => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const userIds = row.userId || this.ids;
      this.$modal
        .confirm('是否确认删除用户编号为"' + userIds + '"的数据项？')
        .then(function () {
          return delUser(userIds);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
  },
};
</script>
<style lang="scss" scoped>
::v-deep .el-dialog .el-dialog__body {
  margin: 0 auto !important;
  height: 72vh;
  overflow: auto;
}
</style>
