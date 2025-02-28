<template>
  <div class="app-container">
    <div class="context flex flex-column pd15">
      <el-radio-group
        v-model="procurementPlanType"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in tabList"
          :key="dict.value"
          >{{ dict.label }}</el-radio-button
        >
      </el-radio-group>
      <FileTemplate v-if="procurementPlanType === 'file'" />
      <ContractTemplate v-if="procurementPlanType === 'contract'" />
      <SupplementalTemplate v-if="procurementPlanType === 'supplemental'" />
      <RatingTemplate v-if="procurementPlanType === 'rating'" />
    </div>
  </div>
</template>
<script>
import RatingTemplate from "./rating.vue";
import FileTemplate from "./file/file-template.vue";
import ContractTemplate from "./contract/contract-template.vue";
import SupplementalTemplate from "./supplemental/supplemental-template.vue";
export default {
  name: "template-index",
  components: {
    RatingTemplate,
    FileTemplate,
    ContractTemplate,
    SupplementalTemplate,
  },
  data() {
    return {
      procurementPlanType: "file",
      tabList: [
        { label: "招标文件模板", value: "file" },
        { label: "合同模板", value: "contract" },
        { label: "补充协议", value: "supplemental" },
        { label: "评分模板", value: "rating" },
      ],
    };
  },
  mounted() {
    this.procurementPlanType = this.$route.query.paramName || "file";
    console.log("Query 参数:", this.$route.query.paramName);
  },
  updated() {
    console.log("Query 参数123:", this.$route.query.paramName);
  },
};
</script>
<style lang="scss" scoped></style>
