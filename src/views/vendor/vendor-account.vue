<template>
  <div class="app-container">
    <div class="context flex flex-column">
      <el-radio-group
        @change="handleRadioChange"
        v-model="queryParams.state"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in vendorApprove"
          :key="dict.value"
        >
          {{ dict.label }}
        </el-radio-button>
      </el-radio-group>
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
        <el-form-item label="供应商名称" prop="vendorName" label-width="90px">
          <el-input
            v-model="queryParams.vendorName"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName" label-width="68px">
          <el-input
            v-model="queryParams.contactName"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="供应商账号" prop="contactPhone" label-width="90px">
          <el-input
            v-model="queryParams.contactPhone"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="el-icon-search"
            size="small"
            @click="handleQuery"
            >查询</el-button
          >
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="contactList"
        stripe
        highlight-current-row
        border
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          min-width="300"
          label="供应商名称"
          align="left"
          prop="vendorName"
        />
        <el-table-column
          min-width="120"
          label="供应商账号"
          align="center"
          prop="contactPhone"
        />
        <el-table-column
          min-width="100"
          label="联系人"
          align="center"
          prop="contactName"
        />
        <el-table-column
          min-width="120"
          label="注册日期"
          align="center"
          prop="createTime"
        />
        <el-table-column
          min-width="120"
          label="是否企业管理员"
          align="center"
          prop="isManagerText"
        />
        <el-table-column
          min-width="100"
          label="授权书"
          align="center"
          prop="arrivalDate"
        >
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="small"
              @click="getAuthorization(row.id)"
              >查看</el-button
            >
          </template>
        </el-table-column>
        <el-table-column
          min-width="100"
          label="账号状态"
          align="center"
          prop="stateText"
        />
        <el-table-column
          min-width="200"
          label="操作"
          align="center"
          prop="state"
        >
          <template slot-scope="{ row }">
            <el-button
              v-if="['1'].includes(queryParams.state)"
              type="text"
              size="small"
              @click="changeState(row)"
              v-hasPermi="['vendor:account:operation']"
              >{{ Number(row.state) === 0 ? "启用" : "禁用" }}</el-button
            >
            <el-button
              v-if="['1'].includes(queryParams.state)"
              type="text"
              size="small"
              @click="changeManager(row)"
              v-hasPermi="['vendor:account:setting']"
              >{{
                Number(row.isManager) === 0 ? "设置管理员" : "取消管理员"
              }}</el-button
            >
            <el-button
              v-if="['1'].includes(queryParams.state)"
              type="text"
              size="small"
              @click="resetPassword(row)"
              v-hasPermi="['vendor:account:reset']"
              >重置密码</el-button
            >
            <el-button
              v-if="['0'].includes(queryParams.state)"
              type="text"
              size="small"
              @click="confirmApprove(row)"
              >审批</el-button
            >
            <el-button
              type="text"
              size="small"
              @click="handelCalibrationApproval(row)"
              >审批详情</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
        @pagination="getVendorContactList"
      />

      <el-dialog
        title="授权书"
        :visible.sync="authorizationVisible"
        width="30%"
      >
        <el-image
          style="width: 500px; height: 500px; margin: 0px auto"
          :src="authorizationUrl"
        ></el-image>
      </el-dialog>
    </div>

    <!-- 审批和审批详情 -->
    <ApprovalForm
      :visible.sync="vendorVisible"
      :title="'新增联系人审批流程'"
      :formModel="sanctionForm"
      :rejectNodeList="rejectNodeList"
      @update:visible="vendorVisible = $event"
      @submit="handleSubmit"
    />
    <ApprovalDetailsDialog
      :visible.sync="calibrateVisible"
      title="新增联系人审批流程详情"
      :activeStep="calibrateActive"
      :processInformationList="processInformationList"
      :approveLists="approveArr"
      :loading="calibrateLoading"
      @update:visible="calibrateVisible = $event"
    />
  </div>
</template>
<script>
import ApprovalForm from "@/components/Approval/approvalForm.vue";
import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
import { Base64 } from "js-base64";
import {
  getPermissionButton,
  postAuditProcess,
  getLoadTaskDef,
  getProcessLogList,
} from "@/api/procurement/manage";
import {
  getVendorContactList,
  updateContactState,
  getAuthorization,
  updateContactManager,
} from "@/api/vendor/vendor";
import { resetUserPwd } from "@/api/system/user";
export default {
  data() {
    return {
      vendorVisible: false,
      calibrateVisible: false,
      calibrateLoading: false,
      id: "",
      businessId: "",
      processId: "",
      approveArr: [],
      calibrateActive: 1,
      taskPresentId: "",
      processInformationList: [],
      vendorApprove: [
        { label: "待审", value: "0" },
        { label: "已审", value: "1" },
      ],
      queryParams: {
        vendorName: undefined,
        contactPhone: undefined,
        contactName: undefined,
        pageNumber: 1,
        pageSize: 10,
        state: "0",
        id: "",
      },
      sanctionForm: {
        pass: true,
        rejectTaskKey: "",
        operateComment: "",
      },
      rejectNodeList: [],
      taskPresentId: "",
      contactList: [],
      total: 0,
      loading: false,
      authorizationVisible: false,
      authorizationUrl: "",
    };
  },
  created() {},

  mounted() {
    if (this.$route.query.id) {
      this.id = Base64.decode(this.$route.query.id);
      console.log(this.id);
      //  this.id= this.$route.query.id;
    }
    this.getVendorContactList();
  },
  methods: {
    async handelCalibrationApproval(row) {
      this.businessId = row.id;
      this.processId = row.wfProcessId;
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        const params = {
          businessId: this.businessId,
          processId: this.processId,
        };
        if (this.businessId && this.processId) {
          const res = await getLoadTaskDef(params);
          this.processInformationList = res.data;
          function getActive(nodes) {
            let allFalse = true;
            for (let i = 0; i < nodes.length; i++) {
              if (!nodes[i].completed) {
                if (i === 0) {
                  return 0;
                } else {
                  return i;
                }
              }
              allFalse = false;
            }
            return nodes.length;
          }
          this.calibrateActive = getActive(this.processInformationList);
          const response = await getProcessLogList(params);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
    async getVendorContactList() {
      this.loading = true;
      this.queryParams.id = this.id || "";
      try {
        const res = await getVendorContactList(this.queryParams);
        this.contactList = res.data.rows;
        this.total = res.data.total;
      } catch (err) {
        console.log(err);
      }
      this.loading = false;
    },
    handleRadioChange(value) {
      this.queryParams.state = value;
      this.getVendorContactList();
      console.log("Selected value:", value);
      // 在这里处理点击事件
    },

    handleSubmit() {
      this.$modal.loading("请稍候...");
      const params = {
        ...this.sanctionForm,
        businessId: this.businessId,
        processId: this.processId,
        curTaskId: this.taskPresentId,
        processKey: "jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_ADDCONTACT",
      };
      postAuditProcess(params).then(() => {
        this.$message.success("提交成功");
        this.$modal.closeLoading();
        this.vendorVisible = false;
        this.getVendorContactList();
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getVendorContactList();
    },
    confirmApprove(row) {
      console.log(JSON.stringify(row));
      console.log(JSON.stringify(row.id));
      this.vendorVisible = true;
      this.getPermissionButton(row);
    },
    // 审批逻辑
    async getPermissionButton(row) {
      this.businessId = row.id;
      this.processId = row.wfProcessId;
      try {
        if (row.id) {
          const res = await getPermissionButton({
            businessId: row.id, //联系人id
            processId: row.wfProcessId, //流程id
          });
          this.rejectNodeList = res.data.completedTaskList;
          this.taskPresentId = res.data.curTaskId;
          // this.isShowButton = res.data.auditable;
        }
      } catch (error) {}
    },
    //修改状态
    async changeState(row) {
      const { id, state } = row;
      this.$confirm(
        `你确定要${Number(state) === 0 ? "启用" : "禁用"}该账号吗?`,
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      ).then(async () => {
        try {
          const res = await updateContactState({
            contactId: id,
            state: Number(state) === 0 ? 1 : 0,
          });
          this.$message.success("操作成功");
          this.getVendorContactList();
        } catch (err) {
          console.log(err);
        }
      });
    },
    async getAuthorization(id) {
      try {
        const res = await getAuthorization(id);
        if (res.data?.fileUrl) {
          this.authorizationUrl = res.data.fileUrl;
          this.authorizationVisible = true;
        }
      } catch (err) {
        console.log(err);
      }
    },

    async changeManager(row) {
      const { id, isManager } = row;
      this.$confirm(
        `你确定要${
          Number(isManager) === 0
            ? "设置该用户为管理员吗？"
            : "取消该用户管理员吗？"
        }`,
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      ).then(async () => {
        try {
          const res = await updateContactManager({
            contactId: id,
            isManager: Number(isManager) === 0 ? 1 : 0,
          });
          this.$message.success("操作成功");
          this.getVendorContactList();
        } catch (err) {
          console.log(err);
        }
      });
    },
    //重置密码
    async resetPassword(row) {
      this.$confirm(`您确定要重置${row.contactName}的密码吗？`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          const res = await resetUserPwd(row.userId);
          this.$message.success("密码重置成功");
        } catch (err) {
          console.log(err);
        }
      });
    },
  },
  components: {
    ApprovalForm,
    ApprovalDetailsDialog,
  },
};
</script>
<style lang="scss" scope></style>
