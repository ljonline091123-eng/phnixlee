<template>
  <div class="app-container">
    <!-- <el-skeleton :rows="6" animated :loading="skeletonLoading"> -->
      <div class="page-title">
        <span>基本信息</span>
        <!-- <div class="page-title-right" v-if="Number(procurementPlan.state) === 0">
          <el-button type="primary" size="mini" @click="submitForm" :disabled="isSubmit" :loading="isSubmit">{{
            isSubmit ? '提交中...' : '提交' }}</el-button>
          <el-button type="primary" size="mini">确定</el-button>
        </div> -->
      </div>
      <el-descriptions class="form-body">
        <el-descriptions-item label="项目编号">{{ procurementPlan.contractCode }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ procurementPlan.projectName }}</el-descriptions-item>
        <el-descriptions-item label=" 计划编号">{{ procurementPlan.procurementPlanCode }}</el-descriptions-item>
        <el-descriptions-item label=" 计划名称">{{ procurementPlan.procurementPlanName }}</el-descriptions-item>
        <el-descriptions-item label="采购层级">{{ procurementPlan.projectHierarchy }}</el-descriptions-item>
        <el-descriptions-item label="计划开始时间">{{ procurementPlan.beginDate }}</el-descriptions-item>
        <el-descriptions-item label=" 计划完成时间">{{ procurementPlan.endDate }}</el-descriptions-item>
        <el-descriptions-item label="计划进场时间">{{ procurementPlan.arrivalDate }}</el-descriptions-item>
        <el-descriptions-item label="填报人">{{ procurementPlan.procurementReporterName }}</el-descriptions-item>
        <el-descriptions-item label=" 采购经办人">{{ procurementPlan.procurementOfficerName }}</el-descriptions-item>
      </el-descriptions>

      <div class="page-title">
        <span>清单</span>
      </div>
      <div class="form-body">
        <el-table v-loading="loading" :data="splitMaterials" border size="mini">
          <el-table-column label="拆分合约规划名称" width="150" align="center" prop="splitContractName" />
          <el-table-column label="拟签约合同承包范围" width="150" align="center" prop="contractScope" />
          <el-table-column label="清单" align="center">
            <template slot-scope="inventory">
              <el-table size="medium" :data="inventory.row.materialsLists">
                <el-table-column label="序号" type="index" width="50" align="center" />
                <el-table-column label="清单编码" width="200" align="center" prop="materialsCode" />
                <el-table-column label="清单名称" width="200" align="center" prop="materialsName" />
                <el-table-column label="规格型号" align="center" prop="specification" />
                <el-table-column label="计量单位" align="center" prop="unitMeasurement" />
                <el-table-column label="清单数量" align="center" prop="count"/>
                <el-table-column label="单价（含税）" align="center" prop="priceIncludingTax" />
              </el-table>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-title">
        <span>审批信息</span>
      </div>
      <div class="form-body">
        <el-steps :active="approvalActive">
          <el-step :title="item.approveName" :description="i === 0 ? '发起人' : '审批岗位' + i"
            v-for="(item, i) in approveNodeInfos" :key="i"></el-step>
        </el-steps>
        <el-table v-loading="loading" :data="approveLists" border size="mini">
          <el-table-column label="序号" type="index" width="50" align="center" />
          <el-table-column label="姓名" align="center" prop="approvePerson" />
          <el-table-column label="审批时间" align="center" prop="approveTime" />
          <el-table-column label="审批状态" align="center" prop="operateType" />
          <el-table-column label="审批意见" align="center" prop="approvalOpinion" />
        </el-table>
      </div>
    <!-- </el-skeleton> -->
  </div>
</template>

<script>
import { Base64 } from 'js-base64';
import { getPlanDetail, submitProcurementPlan } from "@/api/procurement/plan";
export default {
  name: "plan-detail",
  dicts: ['purchase_type'],
  data() {
    return {
      loading: false,
      inventoryList: [],
      isSubmit: false,
      procurementPlan: {}, //基本信息
      approveNodeInfos: [], //审批人信息
      approveLists: [], //审批信息
      splitMaterials: [], //拆分清单
      skeletonLoading: true,
      param:''
    };
  },
  computed: {
    approvalActive() {
      let count = this.approveNodeInfos.reduce((pre, cur) => cur.state === 1 ? pre + 1 : pre, 0);
      console.log(count, '计算');
      return count
    }
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params))
    this.param = param
    console.log(param, '参数')
    this.getPlanDetail()
  },
  methods: {
    async getPlanDetail() {
      try {
        const res = await getPlanDetail(this.param);
        this.skeletonLoading = false;
        console.log(res, '详情');
        const { procurementPlan, approveNodeInfos, approveLists, splitMaterials } = res.data;
        Object.assign(this, { procurementPlan, approveNodeInfos, approveLists, splitMaterials });
      } catch (err) {
        console.log(err);
      }
    },
    async submitForm() {
      try{
        const res = await submitProcurementPlan(this.param)
        this.$message.success('提交成功')
        this.getPlanDetail()
      }catch(err){
        console.log(res);
      }
    }
  }
};
</script>
<style lang="scss" scoped>
.page-title {
  width: 100%;
  border-bottom: solid 1px #ccc;
  padding: 10px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  &::before {
    content: "";
    height: 20px;
    width: 5px;
    background-color: rgba(41, 65, 137, 1);
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}

.form-body {
  padding: 20px;
}
</style>