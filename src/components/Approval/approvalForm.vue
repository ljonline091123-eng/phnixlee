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
