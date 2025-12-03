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
          v-if="Number(procurementPlan.state) === 0 || Number(procurementPlan.state) === 5"
        >{{ isSubmit ? "提交中..." : "提交" }}
        </el-button
        >
        <el-button type="primary" size="mini" v-if="Number(procurementPlan.state) === 0" @click="goUpdate">修改
        </el-button>
        <el-button type="primary" size="mini" v-if="Number(procurementPlan.state) !== 2" @click="goCancellation">作废
        </el-button
        >
        <el-button v-if="bpmInitData.revokable" type="primary" size="mini" @click="handelWithdrawalPlan"
        >撤回
        </el-button
        >

        <el-button
          type="primary"
          size="mini"
          v-if="bpmInitData.auditable"
          @click="handelSanction"
        >审批
        </el-button
        >
        <el-button
          type="primary"
          size="mini"
          @click="handelCalibrationApproval"
        >审批详情
        </el-button
        >
        <el-tooltip
          effect="dark"
          content="生成pdf需要时间请等待一会..."
          placement="top"
        >
          <el-button type="primary" size="mini" @click="printPdf" :loading="downloading"
          >{{ downloading ? '正在生成...' : '下载详情Pdf' }}</el-button>
        </el-tooltip>
      </div>
    </BackButton>
    <div class="context context-no-padding">
      <!-- <el-skeleton :rows="6" animated :loading="skeletonLoading"> -->
      <PageTitle title="基本信息"/>
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
            <el-form-item label="付款方式：" v-if="procurementPlan.procurementType == 1" class="custom-form-item">
              <span>{{ procurementPlan.paymentTypeText }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计数方式：" v-if="procurementPlan.procurementType == 1" class="custom-form-item">
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

      <PageTitle title="清单" marginBottom="15px"/>
      <el-table v-loading="loading" row-key="projectCode" :data="planList" ref="tableRef" size="small" border
                default-expand-all :expand-row-keys="planList[0].projectCode">
        <el-table-column type="expand" v-if="planList[0] && planList[0].children && planList[0].children.length">
          <template slot-scope="props">
            <el-table :data="props.row.children" size="small" border>
              <!-- <el-table-column type="selection"></el-table-column> -->
              <!-- <el-table-column  v-if="planList[0].children.length>1"  label="拆分合约规划名称" prop="splitContractName" width="150">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.splitContractName" :disabled="isSubmit"/>
                </template>
              </el-table-column>
              <el-table-column v-if="planList[0].children.length>1"  label="拟签约合同承包范围" prop="contractScope" width="150">
                <template slot-scope="scope">
                  <div  style="position: absolute;top: 5px;right: 40px;">
                    <el-button  type="danger" size="small"  @click="handleDelete(scope.$index)">删除标包</el-button>
                  </div>
                  <el-input v-model="scope.row.contractScope" :disabled="isSubmit"/>
                </template>
              </el-table-column> -->
              <el-table-column label="清单" align="center">
                <template slot-scope="inventory">
                  <!-- <virtual-scroll
                    :data="inventory.row.materialsLists"
                    :item-size="62"
                    key-prop="materialsId"
                    ref="virScrollRefDialog"
                    @change="(renderData) => virtualData = renderData"> -->
                  <el-table
                    size="small"
                    :data="inventory.row.materialsLists"
                    stripe
                    highlight-current-row
                    show-summary
                    :summary-method="getSummaries"
                    height="380"
                  >
                    <el-table-column label="序号" type="index" width="50" align="center"/>

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
                    <!--              <el-table-column-->
                    <!--                width="150"-->
                    <!--                label="交易标的物"-->
                    <!--                prop="subjectMatterName"-->
                    <!--                show-overflow-tooltip-->
                    <!--              />-->

                    <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>
                    <el-table-column label="计量规则" align="center" prop="measurementRules" show-overflow-tooltip/>
                    <el-table-column label="工作内容" align="center" prop="workContent" show-overflow-tooltip/>
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
                        {{ row.rentMode == 3 ? '-' : row.rentTimeText }}
                      </template>
                    </el-table-column>
                    <el-table-column
                      width="100"
                      label="租赁数量"
                      align="center"
                      prop="rentQuantityText"
                      v-if="procurementPlan.procurementPlanType == 2 || procurementPlan.procurementPlanType == 3">
                      <template slot-scope="{row}">
                        {{ row.rentMode == 3 ? '-' : row.rentQuantityText }}
                      </template>
                    </el-table-column>
                    <el-table-column
                      width="150"
                      label="基价"
                      align="right"
                      prop="basePriceText"
                      v-if="isShow && [2,3,4,5,6,7].includes(procurementPlan.priceType)"
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
                      v-if="isShow && [2,3,6,7].includes(procurementPlan.priceType)"
                    />
                    <el-table-column
                      width="150"
                      label="浮动率"
                      align="right"
                      prop="floatingRateText"
                      v-if="isShow && [4,5,6,7].includes(procurementPlan.priceType)"
                    />
                    <el-table-column
                      v-if="contractPlanning.contractPlanningCategory == 1"
                      label="易料商品编码"
                      align="center"
                      min-width="150" prop="skuId" show-overflow-tooltip
                    >
                      <template slot-scope="scope">
                        <a class="link-type" @click="goDetail(scope.row.skuId)">
                          {{ scope.row.skuId }}
                        </a>
                      </template>
                    </el-table-column>
                    <el-table-column v-if="contractPlanning.contractPlanningCategory == 1" label="易料商品名称"
                                     prop="name" width="150">
                      <template slot-scope="scope">
                        {{ scope.row.name }}
                      </template>
                    </el-table-column>

                    <el-table-column v-if="contractPlanning.contractPlanningCategory == 1" label="易料品牌"
                                     min-width="120" prop="offerBrand" show-overflow-tooltip/>
                    <el-table-column v-if="contractPlanning.contractPlanningCategory == 1" label="易料初始报价"
                                     width="150" prop="offerPrice"/>

                    <el-table-column label="合计(含税)" align="right" prop="totalPriceText" min-width="150"/>
                    <el-table-column label="备注" align="center" prop="remark"/>
                  </el-table>
                  <!-- </virtual-scroll> -->
                </template>
              </el-table-column>


            </el-table>
          </template>
        </el-table-column>
        <el-table-column label="序号" type="index" width="50" align="center"/>
        <!-- <el-table-column label="合约名称" min-width="300" prop="contractPlanningName" show-overflow-tooltip/>
        <el-table-column label="计划金额" align="right" prop="plannedAmountInclTaxText" /> -->

        <el-table-column label="合约名称" align="center" prop="contractPlanningName" min-width="300">

        </el-table-column>
        <el-table-column label="计划金额" align="center" prop="plannedAmountInclTax" min-width="300">

        </el-table-column>


        <!-- <el-table-column label="已发生规划金额（含税）" align="right" prop="incurredPlannedAmountText" />
        <el-table-column label="规划余量(元)" align="right" prop="planningBalanceText" />
        <el-table-column label="拟定招标方式" align="center" prop="biddingMethodName" /> -->
        <!-- <el-table-column label="清单" align="center" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-view" @click="handelInventory()">查看清单</el-button>
          </template>
        </el-table-column> -->

      </el-table>

    </div>
    <ApprovalForm
      :visible.sync="sanctionVisible"
      title="采购计划审批流程"
      :formModel="sanctionForm"
      :rejectNodeList="rejectNodeList"
      :nextCandidateList="nextCandidateList"
      :nextAppointable="nextAppointable"
      @update:visible="sanctionVisible = $event"
      @submit="handleSubmit"
    />
    <ApprovalDetailsDialog
      :visible.sync="calibrateVisible"
      title="采购计划审批流程详情"
      :activeStep="calibrateActive"
      :processInformationList="processInformationList"
      :approveLists="approveArr"
      :loading="calibrateLoading"
      @update:visible="calibrateVisible = $event"
    />
  </div>
</template>

<script>
import {mapGetters} from "vuex";
import {Base64} from "js-base64";
import {
  cancellationProcurementPlan,
  getPermissionButtonPlan,
  getPlanDetail,
  getYjtUrl,
  submitProcurementPlan,
  withdrawalPlan
} from "@/api/procurement/plan";
import Roam from "@/components/Roam";
import BackButton from "@/components/BackButton/index.vue";
import PageTitle from "@/components/PageTitle/index.vue";
import ApprovalForm from "@/components/Approval/approvalForm.vue";
import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
import VirtualScroll from "el-table-virtual-scroll";
import {getLoadTaskDefPlan, getOrgByUserId, getProcessLogList, postAuditProcessPlan} from "@/api/procurement/manage";

export default {
  name: "plan-detail",
  dicts: ["purchase_type"],
  data() {
    return {
      downloading: false,// 打印
      nextAppointable: false,
      nextCandidateList: [],
      rejectNodeList: [],
      sanctionForm: {
        pass: true,
        rejectTaskKey: "",
        operateComment: "",
      },
      processInformationList: [],
      approveArr: [],
      sanctionVisible: false,
      loading: false,
      isAll: false,
      inventoryList: [],
      isSubmit: false,
      procurementPlan: {}, //基本信息
      approveNodeInfos: [], //审批人信息
      approveLists: [], //审批信息
      splitMaterials: [], //拆分清单
      virtualData: [], // 虚拟列表渲染的数据
      contractPlanning: {},
      skeletonLoading: true,
      param: "",
      wfProcessId: "",
      exampleId: null,
      purchaserId: null,
      planList: [],
      bpmInitData: {},
      calibrateVisible: false,
      calibrateLoading: false,
      calibrateActive: 1,
      taskPresentId: "",
    };
  },
  components: {
    VirtualScroll,
    Roam,
    BackButton,
    PageTitle,
    ApprovalForm,
    ApprovalDetailsDialog,
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
    handelWithdrawalPlan() {
      const {procurementPlanName, id} = this.procurementPlan;
      this.$confirm("是否确定撤回采购计划：" + procurementPlanName, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          this.revokeLoding = this.$loading({
            lock: true,
            text: "撤回中...",
            spinner: "el-icon-loading",
            background: "rgba(0, 0, 0, 0.7)",
          });
          /* 请求流程撤回方法 */
          withdrawalPlan(id)
            .then((res) => {
              if (res.code == 200) {
                this.$message.success("撤回成功");
              }
              this.getPlanDetail();
              this.revokeLoding.close();
            })
            .catch((e) => this.revokeLoding.close());
        } catch (error) {
        }
      });
    },
    /* 审批按钮 */
    handelSanction() {
      this.sanctionVisible = true;
      this.getPermissionButtonPlan();
    },
    /* 审批提交 */
    handleSubmit() {
      const params = {
        ...this.sanctionForm,
        businessId: this.purchaserId,
        processId: this.exampleId,
        curTaskId: this.taskPresentId,
        processKey: "jiantou-zhaocai:{org}:ZHAOCAI_PROCUREMENT_PLAN",
      };
      postAuditProcessPlan(params).then(() => {
        this.$message.success("提交成功");
        this.sanctionVisible = false;
        this.getPlanDetail();
      }).catch(error => {
        /* 关闭遮罩层 */
        this.$modal.closeLoading();
      });
    },
    /* 合计列计算 */
    getSummaries(param) {
      const {columns, data} = param;
      const sums = [];
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = '合计';
          return;
        }
        /* 只显示合计 */
        if (column.property === "totalPriceText") {
          const {add, bignumber} = this.mathjs;
          this.splitMaterials.forEach((item) => {
            /* 计算表合计列合计计算合计列 */
            let totalPriceTable = bignumber(0.0);
            if (item.materialsLists && Array.isArray(item.materialsLists)) {
              item.materialsLists.forEach((children, i) => {
                totalPriceTable = add(children.totalPrice ? children.totalPrice : 0.0, totalPriceTable);
              });
            }
            let totalPriceTableText = this.formatNumberDynamicDecimalWithSeparator(totalPriceTable, 2);
            sums[index] = totalPriceTableText;
          })
        } else {
          sums[index] = '';
        }

      });

      return sums;
    },
    /**
     * 格式化数字：动态保留小数位数并添加千分位分隔符
     * @param {number|string} num - 要格式化的数字
     * @param {number} maxDecimalPlaces - 最大保留的小数位数（例如 2 位）
     * @returns {string} - 格式化后的字符串
     */
    formatNumberDynamicDecimalWithSeparator(num, maxDecimalPlaces = 2) {
      // 将数字转换为字符串
      const numStr = num.toString();

      // 找到小数点的位置
      const decimalIndex = numStr.indexOf('.');

      // 截取整数部分和小数部分
      let integerPart = numStr;
      let decimalPart = '';

      if (decimalIndex !== -1) {
        integerPart = numStr.slice(0, decimalIndex);
        decimalPart = numStr.slice(decimalIndex + 1);
      }

      // 如果小数位数超过最大位数，则截取
      if (decimalPart.length > maxDecimalPlaces) {
        decimalPart = decimalPart.slice(0, maxDecimalPlaces);
      }

      // 添加千分位分隔符到整数部分
      integerPart = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');

      // 拼接整数部分和小数部分
      let formattedNumber = integerPart;
      if (decimalPart.length > 0) {
        if (decimalPart.length <= 1) {
          formattedNumber += '.' + decimalPart + '0';
        } else {
          formattedNumber += '.' + decimalPart;
        }
      } else {
        formattedNumber += '.00';
      }

      return formattedNumber;
    },

    /* 审批按钮点击后获取流程基础信息 */
    async getPermissionButtonPlan() {
      try {
        if (this.purchaserId && this.exampleId) {
          /* 大汉要求初始化接口initialize也需要传参数，所以参数在java后台拼接 */
          const res = await getPermissionButtonPlan({
            businessId: this.purchaserId,
            processId: this.exampleId,
          });
          /* 初始化流程基础信息回调数据 */
          this.bpmInitData = res.data;
          /* 驳回节点 */
          this.rejectNodeList = res.data.completedTaskList;
          /* 下一步审批人列表 */
          this.nextCandidateList = res.data.nextCandidateList;
          /* 下一步审批人是否可选 */
          this.nextAppointable = res.data.nextAppointable;
          /* 任务阶段 */
          this.taskPresentId = res.data.curTaskId;
          /* 是否可以审批 */
          this.isShowButton = res.data.auditable;
        }
      } catch (error) {
      }
    },

    /** 跳转方案详情 */
    async goDetail(code) {
      // this.dialogVisible=true
      // console.log(JSON.stringify(code))

      const res = await getYjtUrl(code);
      this.yjtUrl = res.data || ''
      window.open(this.yjtUrl)
      // console.log(JSON.stringify(res))
    },
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
        splitMaterials.forEach((item, index) => {
          item.$index = index;
          item.planTable = 'planTable' + index
          let children = item.materialsLists.map((child, k) => ({
            ...child,
            $index: k,
            indexNumber: (k + 1),
            planTable: item.planTable
          }))
          item.children = children;
        })
        this.planList[0] = {...contractPlanning, children: splitMaterials};
        console.log("splitMaterials" + JSON.stringify(this.splitMaterials))
        this.isAll = this.splitMaterials.every(item => item.splitContractName && item.splitContractName != "null" && item.contractScope && item.contractScope != "null")
        this.wfProcessId = procurementPlan.wfProcessId;
        this.exampleId = procurementPlan.wfProcessId;
        this.purchaserId = procurementPlan.id;
        this.getPermissionButtonPlan();
      } catch (err) {
        console.log(err);
      }
    },
    /* 审批详情 */
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        let params = {
          businessId: this.purchaserId,
          processId: this.exampleId,
        };
        let res = null;
        /* 判断业务id和流程id是否同时存在(判断是否已经提交工作流) */
        if (this.purchaserId && this.exampleId) {
          /* 存在就去获取x轴列表的工作流执行环节 */
          res = await getLoadTaskDefPlan(params);
        } else {
          /* 采购方案是使用提交人的组织机构来确定走哪个公司层级的流程Key */
          /* 未提交时查看流程执行流程，根据登录人id 获取流程分组 */
          res = await getOrgByUserId(this.$store.state.user.id);
          params = {
            processKey: "jiantou-zhaocai:" + res.data + ":ZHAOCAI_PROCUREMENT_SCHEME",
            businessId: this.purchaserId,
          };
          /* 不存在就去获取x轴列表的工作流执行环节 */
          res = await getLoadTaskDefPlan(params);
        }
        /* 审批流程接口(大汉流程)返回的数据 */
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

        if (this.purchaserId && this.exampleId) {
          /* 流程操作日志 /listProcessLog */
          const response = await getProcessLogList(params);
          this.approveArr = response.data;
        }
      } catch (error) {
      }
      this.calibrateLoading = false;
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
          const loading = this.$loading({
            lock: true,
            text: '正在提交...',
            background: 'rgba(0, 0, 0, 0.7)',
          });
          try {
            const {id} = this.procurementPlan;
            const detailUrl = this.$route.fullPath;
            await submitProcurementPlan({
              id,
              detailUrl,
              // operateComment: this.reviewText,
            });
            this.$message.success("提交成功");
            this.getPlanDetail();
            // this.resetForm(); // 重置表单
          } catch (error) {
            this.$message.error("提交失败");
          } finally {
            loading.close();
          }
        } catch (error) {
        }
      });
    },


    /** 修改 */
    goUpdate() {
      let row = this.procurementPlan;
      row.type = "update";
      row.contractPlanningCode = this.contractPlanning.contractPlanningCode
      console.log(row, 'rrrrrrrrrrrrrrrrr');
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
        } catch (error) {
        }
      });
    },
    async printPdf() {
      this.downloading = true;
      // 模拟接口耗时，如果你使用 a 标签下载，也可以 setTimeout 后恢复状态
      try {
        const link = document.createElement('a');
        /* 网关需要设置白名单 */
        link.href = `http://127.0.0.1/dev-api/business/agreementWord/generatePlan?id=`+this.procurementPlan.id;
        link.download = '';
        link.target = "_blank"
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        // 模拟等待 120 秒后恢复（真实项目中可用事件监听或回调）
        setTimeout(() => {
          this.downloading = false;
        }, 10000);
      } catch (e) {
        this.downloading = false;
      }
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
