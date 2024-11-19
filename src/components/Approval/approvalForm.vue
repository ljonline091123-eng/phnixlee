<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    @close="
      () => {
        closeDialog();
      }
    "
    width="50%"
  >
    <el-form ref="form" :model="formModel" label-width="80px">
      <el-form-item label="审批结果">
        <el-radio-group v-model="formModel.pass">
          <el-radio :label="true">通过</el-radio>
          <el-radio :label="false">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="formModel.pass === false" label="驳回节点">
        <el-select v-model="formModel.rejectTaskKey" placeholder="请选择">
          <el-option
            v-for="option in rejectNodeList"
            :key="option.taskKey"
            :label="option.taskName"
            :value="option.taskKey"
          ></el-option>
        </el-select>
      </el-form-item>
      <!--   选择'通过'显示，'可选审批人'显示   -->
      <el-form-item v-if="formModel.pass && nextAppointable" label="指派人" required>
                      <!--  nextAuditUserId下一步审批人  -->
        <el-select v-model="formModel.nextAuditUserId" placeholder="请选择" :clearable="true">
          <el-option
            v-for="candidate in nextCandidateList"
            :key="candidate.userId"
            :label="candidate.userName"
            :value="candidate.userId"
          ></el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="批语">
        <el-input type="textarea" v-model="formModel.operateComment"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">提交</el-button>
        <el-button @click="closeDialog">取消</el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script>
export default {
  name: "ApprovalForm",
  props: {
    visible: {
      type: Boolean,
      required: false,
    },
    title: {
      type: String,
      required: true,
    },
    formModel: {
      type: Object,
      required: true,
    },
    rejectNodeList: {
      type: Array,
      required: true,
    },
    /* 可选审批人列表 */
    nextCandidateList: {
      type: Array,
      required: true,
    },
    /* 是否可指定审批人 */
    nextAppointable: {
      type: Boolean,
      required: true,
    },
  },
  methods: {
    submitForm() {
      this.$emit("submit");
    },
    closeDialog() {
      this.$emit("update:visible", false);
    },
  },
};
</script>

<style scoped>
/* 添加样式 */
</style>
