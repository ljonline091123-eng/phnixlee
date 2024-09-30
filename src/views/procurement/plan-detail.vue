<template>
  <div class="app-container">
    <BackButton path="/procurement/plan" title="采购计划详情">
      <div>
        <el-button
          type="primary"
          size="mini"
          @click="goSubmit"
          :disabled="isSubmit"
          :loading="isSubmit"
          v-if="Number(procurementPlan.state) === 0"
          >{{ isSubmit ? "提交中..." : "提交" }}</el-button
        >
        <el-button type="primary" size="mini" v-if="Number(procurementPlan.state) === 0" @click="goUpdate">修改</el-button>
        <el-button type="primary" size="mini" v-if="Number(procurementPlan.state) !== 2" @click="goCancellation">作废</el-button
        >
      </div>
    </BackButton>
    <div class="context context-no-padding">
      <!-- <el-skeleton :rows="6" animated :loading="skeletonLoading"> -->
      <PageTitle title="基本信息" />
      <el-form :model="procurementPlan" label-width="110px" class="form-body">
        <el-row class="custom-row">
          <el-col :span="8">
            <el-form-item label="项目名称：" class="custom-form-item">
              <span>{{ procurementPlan.projectName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="编号：" class="custom-form-item">
              <span>{{ procurementPlan.procurementPlanCode }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购名称：" class="custom-form-item">
              <span>{{ procurementPlan.procurementPlanName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购层级：" class="custom-form-item">
              <span>{{ procurementPlan.projectHierarchy }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始时间：" class="custom-form-item">
              <span>{{ procurementPlan.beginDate }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="完成时间：" class="custom-form-item">
              <span>{{ procurementPlan.endDate }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="进场时间：" class="custom-form-item">
              <span>{{ procurementPlan.arrivalDate }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="填报人：" class="custom-form-item">
              <span>{{ procurementPlan.procurementReporterName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购人：" class="custom-form-item">
              <span>{{ procurementPlan.procurementOfficerName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="交易标的物：" class="custom-form-item">
              <span>{{ procurementPlan.subjectMatterName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上限价(元)：" class="custom-form-item">
              <span>{{ contractPlanning.plannedAmountInclTaxText }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="isShow">
            <el-form-item label="指导价(元)：" class="custom-form-item">
              <span>对接易料市集</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="付款方式：" v-if="procurementPlan.subjectMatterType == 1" class="custom-form-item">
              <span>{{ procurementPlan.paymentTypeText }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计数方式：" v-if="procurementPlan.subjectMatterType == 1" class="custom-form-item">
              <span>{{ procurementPlan.countingTypeText }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="isShow">
            <el-form-item label="价格类型：" class="custom-form-item">
              <span>{{ procurementPlan.priceTypeText }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="isShow && procurementPlan.priceType == 2">
            <el-form-item label="区域：" class="custom-form-item">
              <span>{{ procurementPlan.regionProvinceName }}/{{ procurementPlan.regionCityName }}</span>
            </el-form-item>
          </el-col>
        </el-row>

      </el-form>

      <PageTitle title="清单" marginBottom="15px" />
      <el-table
        v-loading="loading"
        :data="splitMaterials"
        stripe
        highlight-current-row
        border
        size="small"
      >
        <el-table-column
          label="拆分合约规划名称"
          width="150"
          align="center"
          prop="splitContractName"
          show-overflow-tooltip
        />
        <el-table-column
          label="拟签约合同承包范围"
          width="150"
          align="center"
          prop="contractScope"
          show-overflow-tooltip
        />
        <el-table-column label="清单" align="center">
          <template slot-scope="inventory">
            <el-table
              size="small"
              :data="inventory.row.materialsLists"
              stripe
              highlight-current-row
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column
                min-width="200"
                label="清单编码"
                prop="materialsCode"
                show-overflow-tooltip
              />
              <el-table-column
                label="清单名称"
                min-width="200"
                align="left"
                prop="materialsName"
                show-overflow-tooltip
              />
              <el-table-column
                width="150"
                label="交易标的物"
                prop="subjectMatterName"
                show-overflow-tooltip
              />
              <el-table-column
                width="200"
                label="规格型号"
                prop="specification"
                show-overflow-tooltip
              />
              <el-table-column
                width="100"
                label="计量单位"
                prop="unitMeasurement"
              />
              <el-table-column
                width="100"
                label="租赁方式"
                align="center"
                prop="rentModeText"
                v-if="procurementPlan.procurementPlanType == 2 || procurementPlan.procurementPlanType == 3"
              />
              <el-table-column
                width="100"
                :label="procurementPlan.procurementPlanType == 2 || procurementPlan.procurementPlanType == 3? '工作量' : '清单数量'"
                align="right"
                prop="countText"
              />
              <el-table-column
                width="100"
                label="租赁时间"
                align="center"
                prop="rentTimeText"
                v-if="procurementPlan.procurementPlanType == 2 || procurementPlan.procurementPlanType == 3">
                <template slot-scope="{row}">
                  {{ row.rentMode == 3? '-' : row.rentTimeText }}
                </template>
              </el-table-column>
              <el-table-column
                width="100"
                label="租赁数量"
                align="center"
                prop="rentQuantityText"
                v-if="procurementPlan.procurementPlanType == 2 || procurementPlan.procurementPlanType == 3">
                <template slot-scope="{row}">
                  {{ row.rentMode == 3? '-' : row.rentQuantityText }}
                </template>
              </el-table-column>
              <el-table-column
                width="150"
                label="基价"
                align="right"
                prop="basePriceText"
                v-if="isShow && procurementPlan.priceType == 2"
              />
              <el-table-column
                width="150"
                label="单价(含税)"
                align="right"
                prop="unitPriceInclTaxText"
                v-else
              />
              <el-table-column
                width="150"
                label="浮动价"
                align="right"
                prop="floatingPriceText"
                v-if="isShow && procurementPlan.priceType == 2"
              />
              <el-table-column
                width="150"
                label="卸费"
                align="right"
                prop="unloadingFeeText"
                v-if="isShow && procurementPlan.priceType == 2"
              />
            </el-table>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { Base64 } from "js-base64";
import {
  getPlanDetail,
  submitProcurementPlan,
  cancellationProcurementPlan,
} from "@/api/procurement/plan";
import Roam from "@/components/Roam";
import BackButton from "@/components/BackButton/index.vue";
import PageTitle from "@/components/PageTitle/index.vue";
export default {
  name: "plan-detail",
  dicts: ["purchase_type"],
  data() {
    return {
      loading: false,
      inventoryList: [],
      isSubmit: false,
      procurementPlan: {}, //基本信息
      approveNodeInfos: [], //审批人信息
      approveLists: [], //审批信息
      splitMaterials: [], //拆分清单
      contractPlanning: {},
      skeletonLoading: true,
      param: "",
      wfProcessId: "",
    };
  },
  components: {
    Roam,
    BackButton,
    PageTitle,
  },
  computed: {
    approvalActive() {
      let count = this.approveNodeInfos.reduce(
        (pre, cur) => (cur.state === 1 ? pre + 1 : pre),
        0
      );
      console.log(count, "计算");
      return count;
    },
    ...mapGetters(["project"]),
    isShow() {
      return this.procurementPlan.subjectMatterType == 1 || this.procurementPlan.subjectMatterType == 2;
    }
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params));
    this.param = param;
    console.log(param, "参数");
    this.getPlanDetail();
  },
  methods: {
    async getPlanDetail() {
      try {
        const res = await getPlanDetail(this.param);
        this.skeletonLoading = false;
        console.log(res, "详情");
        const {
          procurementPlan,
          approveNodeInfos,
          approveLists,
          splitMaterials,
          contractPlanning,
        } = res.data;
        Object.assign(this, {
          procurementPlan,
          approveNodeInfos,
          approveLists,
          splitMaterials,
          contractPlanning,
        });
        this.wfProcessId = procurementPlan.wfProcessId;
      } catch (err) {
        console.log(err);
      }
    },
    /** 提交 */
    goSubmit() {
      this.$confirm(
        "确定是否提交采购计划：" + this.procurementPlan.procurementPlanName,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      ).then(async () => {
        try {
          await submitProcurementPlan(this.procurementPlan.id);
          this.$message.success("提交成功");
          this.procurementPlan.state = 2;
        } catch (error) {}
      });
    },
    /** 修改 */
    goUpdate() {
      let row = this.procurementPlan;
      row.type = "update";
      row.contractPlanningCode = this.contractPlanning.contractPlanningCode
      console.log(row,'rrrrrrrrrrrrrrrrr');
      let param = Base64.encode(JSON.stringify(row));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/add-plan/${param}`);
    },
    /** 作废 **/
    goCancellation() {
      this.$confirm(
        "确定要作废采购计划：" + this.procurementPlan.procurementPlanName,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      ).then(async () => {
        try {
          await cancellationProcurementPlan(this.procurementPlan.id);
          this.$message.success("作废成功");
          this.procurementPlan.state = 3;
        } catch (error) {}
      });
    },
  },
  watch: {
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.$router.replace("/procurement/plan");
        }
      },
    },
  },
};
</script>
<style lang="scss" scoped>
.form-body {
  padding: 20px;
  font-size: 13px;
}
.step_item {
  font-size: 14px;
  padding-bottom: 16px;
  margin-top: 32px;
}
::v-deep .step_item .el-step__title.is-finish {
  color: #2b4acb !important;
}
::v-deep .step_item .el-step__description.is-finish {
  color: #2b4acb !important;
}
::v-deep .step_item .el-step__head.is-finish {
  color: #2b4acb;
  border-color: #2b4acb;
}
.custom-line-height {
  height: 36px;
  line-height: 36px;
}
.custom-row {
  line-height: 36px; /* 设置行高为 36px */
}

.custom-form-item {
  margin-bottom: 0; /* 删除底部间距 */
}

.custom-form-item > .el-form-item__content {
  line-height: 36px; /* 设置内容的行高 */
  height: 36px; /* 确保内容高度与行高一致 */
  display: flex;
  align-items: center; /* 垂直居中对齐内容 */
}
</style>
