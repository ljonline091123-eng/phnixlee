<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    @close="handleClose"
    width="80%"
  >
    <div class="page-title">
      <span>审批信息</span>
    </div>
    <div class="form-body">
      <el-steps :active="activeStep" align-center class="step_item">
        <el-step
          v-for="(item, index) in processInformationList"
          :key="index"
          :title="item.nodeName"
        >
          <template v-slot:description>
            <div>{{ getUserNames(item.userList) }}</div>
            <!-- <div>{{ getPostNames(item.taskPost) }}</div> -->
            <div>{{ getOrgName(item.taskPost) }}</div>
          </template>
        </el-step>
      </el-steps>
      <el-table
        class="table-body"
        v-loading="loading"
        :data="approveLists"
        border
        size="mini"
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column label="姓名" align="center" prop="handlerName" />
        <el-table-column label="操作类型" align="center" prop="operateName" />
        <el-table-column label="审批时间" align="center" prop="endTime" />
        <el-table-column label="操作说明" align="center" prop="operateRemark" />
        <el-table-column label="批语" align="center" prop="operateComment">
          <template v-slot="scope">
            <span>{{ scope.row.operateComment || "-" }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: "ApprovalDetailsDialog",
  props: {
    visible: {
      type: Boolean,
      required: false,
    },
    title: {
      type: String,
      required: true,
    },
    activeStep: {
      type: Number,
      default: 1,
      required: true,
    },
    processInformationList: {
      type: Array,
      required: true,
    },
    approveLists: {
      type: Array,
      required: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    getUserNames(userList) {
      if (!userList || userList.length === 0) {
        return "";
      }
      return userList.map((user) => user.userName).join(", ");
    },
    // getPostNames(taskPost) {
    //   if (!taskPost || taskPost.length === 0) {
    //     return "";
    //   }
    //   return taskPost.map((item) => item.postName).join(", ");
    // },
    getOrgName(taskPost) {
      if (!taskPost || taskPost.length === 0) {
        return "";
      }
      return taskPost.map((item) => item.orgName).join(", ");
    },
    handleClose() {
      this.$emit("update:visible", false);
    },
  },
};
</script>

<style scoped>
.page-title {
  font-weight: bold;
  margin-bottom: 10px;
}
.form-body {
  margin-top: 20px;
}
.step_item {
  margin-bottom: 20px;
}
.table-body {
  margin-top: 20px;
}
</style>
