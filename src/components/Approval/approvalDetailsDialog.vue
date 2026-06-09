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
      <el-steps :active="computedActiveStep" align-center class="step_item">
        <el-step
          v-for="(group, gIndex) in groupedSteps"
          :key="gIndex"
          :class="{ 'is-parallel': group.parallel }"
        >
          <template v-slot:title>
            <span>{{ group.items.map(n => n.nodeName).join(' / ') }}</span>
          </template>
          <template v-slot:description>
            <div v-for="(item, i) in group.items" :key="i" style="margin-bottom: 4px;">
              <div>{{ getUserNames(item.userList) }}</div>
              <div>{{ getOrgName(item.taskPost) }}</div>
            </div>
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
  computed: {
    /**
     * 将 processInformationList 按 level 分组
     * 同一 level 的节点是并行节点，显示在同一个 step 中
     */
    groupedSteps() {
      if (!this.processInformationList || this.processInformationList.length === 0) return [];
      // 检查是否有 level 字段（新格式支持并行网关）
      const hasLevel = this.processInformationList[0].level !== undefined;
      if (hasLevel) {
        const groups = {};
        this.processInformationList.forEach(item => {
          const level = item.level;
          if (!groups[level]) groups[level] = [];
          groups[level].push(item);
        });
        // 按 level 升序排列
        return Object.keys(groups)
          .sort((a, b) => a - b)
          .map(key => ({
            items: groups[key],
            parallel: groups[key].length > 1
          }));
      }
      // 旧格式（无 level 字段）：每个节点单独一个 step
      return this.processInformationList.map(item => ({
        items: [item],
        parallel: false
      }));
    },
    /**
     * 根据完成状态自动计算当前活跃的 step 索引
     * 找到第一个未完全完成的步骤组
     */
    computedActiveStep() {
      for (let i = 0; i < this.groupedSteps.length; i++) {
        const group = this.groupedSteps[i];
        const allCompleted = group.items.every(item =>
          item.userList && item.userList.length > 0 && item.userList.every(u => u.completed)
        );
        if (!allCompleted) return i;
      }
      // 全部完成，active 设为总长度，使所有 step 显示为已完成状态
      return this.groupedSteps.length;
    }
  },
  methods: {
    getUserNames(userList) {
      if (!userList || userList.length === 0) {
        return "";
      }
      /* 已执行的加个 ✔ */
      return userList.map((user) => user.completed ? ("✔ "+user.userName) : user.userName).join(", ");
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
