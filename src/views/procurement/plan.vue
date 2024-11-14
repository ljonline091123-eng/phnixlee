<template>
  <div class="app-container">
    <div class="context flex flex-column">
      <el-radio-group
        v-model="procurementPlanMode"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button label="masterPlan" name="masterPlan">采购总计划</el-radio-button>
        <el-radio-button label="plan" name="plan">采购计划</el-radio-button>
      </el-radio-group>

      <!-- 总计划 -->
      <template v-if="procurementPlanMode === 'masterPlan'">
        <el-form
          :model="queryParamsMaster"
          ref="queryForm"
          size="small"
          inline
          v-show="showSearch"
        >
          <el-form-item
            label="合约类别"
            prop="contractType"
            label-width="68px"
          >
            <el-select
              v-model="queryParamsMaster.contractType"
              placeholder="合约类别"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="dict in dict.type.procurement_plan_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="合约名称"
            prop="contractName"
            label-width="68px"
          >
            <el-input
              v-model="queryParamsMaster.contractName"
              placeholder="请输入采购名称"
              clearable
              style="width: 180px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item
            label="招标方式"
            prop="biddingMethodCode"
            label-width="120"
          >
          <el-select
              v-model="queryParamsMaster.biddingMethodCode"
              placeholder="招标方式"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="dict in dict.type.contract_bidding_method"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="招标责任单位"
            prop="bidResponsibleOrg"
            label-width="120"
          >
          <el-select
              v-model="queryParamsMaster.bidResponsibleOrg"
              placeholder="招标责任单位"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="dict in dict.type.contract_bidding_responsible_org"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="招标状态"
            prop="biddingState"
            label-width="120"
          >
          <el-select
              v-model="queryParamsMaster.biddingState"
              placeholder="招标状态"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="dict in dict.type.contract_bidding_state"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              icon="el-icon-search"
              size="small"
              @click="getMasterPlanningList()"
              >查询</el-button
            >
          </el-form-item>
        </el-form>

        <el-table
            v-loading="planMasterLoading"
            :data="planMasterList"
            stripe
            :summary-method="getSummaries"
            show-summary
            highlight-current-row
            border
            max-height="700"
          >
            <el-table-column label="序号" type="index" width="50" align="center" />
            <el-table-column
              label="合约名称"
              align="left"
              min-width="250"
              prop="contractPlanningName"
              show-overflow-tooltip
            >
              <template slot-scope="scope">
                {{ scope.row.contractPlanningName }}
              </template>
            </el-table-column>
            <el-table-column
              label="合约类别"
              min-width="150"
              prop="contractPlanningCategoryName"
            />
            <el-table-column
              label="策划金额(含税)"
              min-width="150"
              align="right"
              prop="plannedAmountInclTaxText"
            />
            <el-table-column
              label="引用金额(含税)"
              min-width="150"
              align="right"
              prop="incurredPlannedAmountText"
            />
            <el-table-column
              label="余额(元)"
              min-width="150"
              align="right"
              prop="planningBalanceText"
            />
            <el-table-column
              label="拟定招标方式"
              min-width="100"
              align="center"
              prop="biddingMethodName"
            />
            <el-table-column
              label="招标时间"
              min-width="100"
              align="center"
              prop="biddingTime"
            />
            <el-table-column
              label="进场时间"
              min-width="100"
              align="center"
              prop="enterIntoTime"
            />
            <el-table-column
              label="招标责任单位"
              min-width="180"
              align="center"
              prop="bidResponsibleOrgName"
            />
            <el-table-column
              label="招标状态"
              min-width="100"
              align="center"
              prop="biddingStateText"
            />
            <el-table-column
              label="操作"
              width="200"
              align="center"
              fixed="right"
            >
              <template slot-scope="scope">
                <el-button
                  type="text"
                  @click="dbClick(scope.row)"
                  icon="el-icon-s-promotion"
                  size="small"
                  v-hasPermi="['procurement:plan:add']"
                  v-if="scope.row.biddingState == 1 || scope.row.biddingState == 2"
                  >发起采购计划</el-button
                >
                <el-button
                  type="text"
                  @click="pushState(scope.row)"
                  icon="el-icon-finished"
                  size="small"
                  :disabled="scope.row.pushStatus == 1"
                  >
                  {{ scope.row.pushStatus == 1 ? "已推送" : "推送" }}
                  </el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <!-- 推送 -->
        <el-dialog
          title="推送"
          :visible.sync="pushStateDialog"
          v-if="pushStateDialog"
          width="50%"
          @closed="closePushStateDialog"
        >
          <el-form
            :model="pushQuery"
            ref="planForm"
            label-position="left"
            size="small"
            inline
            @submit.native.prevent
          >
          <el-form-item
              label="请选择角色"
              prop="pushRoleList"
              class="label-right-align"
              label-width="86px"
            >
              <el-select
                v-model="pushQuery.role"
                placeholder="请选择"
                style="width: 220px"
                multiple
                clearable
                collapse-tags
              >
              <el-option
                v-for="dict in pushRoleList"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item
              label="姓名"
              prop="pushRoleList"
              class="label-right-align"
              label-width="40px"
            >
            <el-input
                v-model="pushQuery.nickName"
                placeholder="请输入姓名"
                clearable
                style="width: 150px"
                @keyup.enter.native="searchUser"
              />
            </el-form-item>

            <el-form-item
              label="请选择合约拆分"
              prop="contractPlanList"
              class="label-right-align"
              label-width="110px"
            >
              <el-select
                v-model="pushQuery.cpId"
                placeholder="请选择"
                style="width: 200px"
                clearable
                @change="searchUser"
                collapse-tags
              >
                <el-option
                  v-for="dict in contractPlanList"
                  :key="dict.cpId"
                  :label="dict.splitContractName + (dict.pushStatus === 1?' (已推送)':'')"
                  :value="dict.cpId"
                  :disabled="dict.pushStatus === 1"
                ></el-option>
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                icon="el-icon-search"
                size="small"
                style="width: 70px"
                @click="searchUser"
                >查询</el-button
              >
            </el-form-item>

          </el-form>


          <virtual-scroll
            :data="pushUserList"
            :item-size="62"
            key-prop="virtualId"
            ref="virScroll"
            @change="(renderData) => virtualList = renderData">
            <el-table
                v-loading="pushListLoading"
                :data="virtualList"
                stripe
                size="small"
                highlight-current-row
                border
                @selection-change="handleSelectionChange"
                :row-key="selPushKey"
                max-height="400"
                ref="pushTable"
              >
                <el-table-column type="selection" width="50" reserve-selection/>
                <el-table-column label="序号" prop="virtualId" show-overflow-tooltip width="80"/>
                <el-table-column label="用户" prop="nickName" show-overflow-tooltip width="100"/>
                <el-table-column label="归属当前组织名称" prop="belongCurrLvlOrg" show-overflow-tooltip/>
                <el-table-column label="归属管理组织名称" prop="belgDeptName" show-overflow-tooltip/>
                <el-table-column label="合约拆分名称" prop="splitContractName" show-overflow-tooltip/>
                <el-table-column label="合约规划名称" prop="contractPlanningName" show-overflow-tooltip/>
              </el-table>
          </virtual-scroll>

          <div slot="footer" class="dialog-footer">
          <el-button
            @click="pushStateDialog = false"
            style="width: 100px"
            size="small"
            :disabled="confirmPushLoading"
            >取 消</el-button
          >
          <el-button
            type="primary"
            @click="confirmPlan"
            style="width: 100px"
            size="small"
            :loading="confirmPushLoading"
            :disabled="confirmPushLoading"
            >确 定</el-button
          >
        </div>
        </el-dialog>
      </template>
      <!-- 计划 -->
      <template v-if="procurementPlanMode === 'plan'">
        <el-form
          :model="queryParams"
          ref="queryForm"
          size="small"
          :inline="true"
          v-show="showSearch"
        >
          <el-form-item
            label="编号"
            prop="procurementPlanCode"
            label-width="50px"
          >
            <el-input
              v-model="queryParams.procurementPlanCode"
              placeholder="请输入编号"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item
            label="采购名称"
            prop="procurementPlanName"
            label-width="68px"
          >
            <el-input
              v-model="queryParams.procurementPlanName"
              placeholder="请输入采购名称"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item
            label="采购人"
            prop="procurementOfficerName"
            label-width="120"
          >
            <el-input
              v-model="queryParams.procurementOfficerName"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item
            label="招标责任单位"
            prop="bidResponsibleOrg"
            label-width="120"
          >
          <el-select
              v-model="queryParams.bidResponsibleOrg"
              placeholder="招标责任单位"
              clearable
              style="width: 240px"
            >
              <el-option
                v-for="dict in dict.type.contract_bidding_responsible_org"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
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
              v-hasPermi="['procurement:plan:add']"
              >新增</el-button
            >
            <el-button
              type="warning"
              icon="el-icon-setting"
              size="small"
              @click="setContractSplitFlag"
              v-hasPermi="['procurement:plan:add']"
              v-if="splitValue !== '2'"
            >{{splitValue === "1" ? "设置合约不可拆分" : "设置合约可拆分"}}</el-button
            >
          </el-form-item>
        </el-form>

        <el-radio-group
          v-model="queryParams.procurementPlanType"
          size="small"
          style="padding-bottom: 15px"
        >
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button
            :label="dict.value"
            :name="dict.value"
            v-for="dict in dict.type.procurement_plan_type"
            :key="dict.value"
            >{{ dict.label }}</el-radio-button
          >
        </el-radio-group>

        <el-table
          v-loading="planLoading"
          :data="planList"
          stripe
          highlight-current-row
          border

        >
          <el-table-column label="序号" type="index" width="50" align="center" />
          <el-table-column
            label="编号"
            min-width="200"
            align="center"
            prop="procurementPlanCode"
          >
            <template slot-scope="scope">
              <a class="link-type" @click="goDetail(scope.row.id)">
                {{ scope.row.procurementPlanCode }}
              </a>
            </template>
          </el-table-column>
          <el-table-column
            label="采购名称"
            align="left"
            min-width="250"
            prop="procurementPlanName"
            show-overflow-tooltip
          />
          <el-table-column
            label="合约类别"
            min-width="150"
            prop="procurementPlanTypeText"
          />
          <el-table-column
            label="采购层级"
            min-width="100"
            prop="projectHierarchy"
          />
          <el-table-column
            label="开始时间"
            min-width="100"
            align="center"
            prop="beginDate"
          />
          <el-table-column
            label="完成时间"
            min-width="100"
            align="center"
            prop="endDate"
          />
          <el-table-column
            label="进场时间"
            min-width="100"
            align="center"
            prop="arrivalDate"
          />
          <el-table-column
            label="采购人"
            min-width="100"
            align="center"
            prop="procurementOfficerName"
          />
          <el-table-column
            label="填报人"
            min-width="80"
            align="center"
            prop="procurementReporterName"
          />
          <el-table-column
            label="创建时间"
            min-width="180"
            align="center"
            prop="createTime"
          />
          <el-table-column
            label="状态"
            min-width="100"
            align="center"
            prop="stateText"
          >
          <template slot-scope="scope">
            {{ scope.row.stateText }}
          </template>
        </el-table-column>
          <el-table-column
            label="操作"
            width="200"
            align="center"
            fixed="right"
          >
            <template slot-scope="scope">
              <el-button
                type="text"
                @click="goUpdate(scope.row)"
                icon="el-icon-edit"
                size="small"
                v-if="Number(scope.row.state) === 0"
                >修改</el-button
              >
              <el-button
                type="text"
                @click="goSubmit(scope.row.id, scope.row.procurementPlanName)"
                icon="el-icon-s-promotion"
                size="small"
                v-if="Number(scope.row.state) === 0"
                >提交</el-button
              >
              <el-button
                type="text"
                @click="
                  goCancellation(scope.row.id, scope.row.procurementPlanName)
                "
                icon="el-icon-document-delete"
                size="small"
                >作废</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <!-- 选择项目合约规划 -->
        <el-dialog
          title="选择项目合约规划"
          :visible.sync="contractVisible"
          width="70%"
        >
          <el-form
            :model="contractQuery"
            ref="vForm"
            label-position="left"
            label-width="80px"
            size="small"
            @submit.native.prevent
          >
            <el-row :gutter="10">
              <el-col :span="8" class="grid-cell">
                <el-form-item
                  label="合约名称"
                  label-width="68px"
                  prop="contractName"
                  class="label-right-align"
                >
                  <el-input
                    v-model="contractQuery.contractName"
                    type="text"
                    clearable
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="6" class="grid-cell">
                <div class="static-content-item">
                  <el-button
                    type="primary"
                    icon="el-icon-search"
                    size="small"
                    @click="searchContract"
                    >查询</el-button
                  >
                </div>
              </el-col>
            </el-row>
          </el-form>
          <el-table
            v-loading="contractLoading"
            :data="contractList"
            stripe
            highlight-current-row
            border
            size="small"
            @row-dblclick="dbClick"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              min-width="200"
              label="合约名称"
              align="left"
              prop="contractPlanningName"
            />
            <el-table-column
              label="合约类别"
              prop="contractPlanningCategoryName"
            />
            <el-table-column
              label="规划金额(含税)"
              align="right"
              prop="plannedAmountInclTaxText"
            />
            <el-table-column
              width="180"
              label="已发生规划金额(含税)"
              align="right"
              prop="incurredPlannedAmountText"
            />
            <el-table-column
              label="规划余量(元)"
              align="right"
              prop="planningBalanceText"
            />
            <el-table-column
            label="可使用数量"
            align="right"
            prop="surplusQuantityText"
          />
            <el-table-column
              label="拟定招标方式"
              align="center"
              prop="biddingMethodName"
            />
          </el-table>
          <pagination
            v-show="contractTotal > 0"
            :total="contractTotal"
            :page.sync="contractQuery.pageNumber"
            :limit.sync="contractQuery.pageSize"
            @pagination="getContractList"
          />
        </el-dialog>
        <pagination
          v-show="total > 0"
          :total="total"
          :page.sync="queryParams.pageNumber"
          :limit.sync="queryParams.pageSize"
          @pagination="getPlanList"
        />
      </template>
    </div>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import {
  getPlanList,
  getMasterPlanningList,
  getContractPlanningList,
  submitProcurementPlan,
  cancellationProcurementPlan,
  getUsersRoleList,
  pushProcurementPlan,
  setContractPlanSplitFlag,
  getContractPlanSplitFlag,
  getUsersRoleContractPlanList
} from "@/api/procurement/plan";

import VirtualScroll from 'el-table-virtual-scroll'
import { mapGetters } from "vuex";
import {getBiddingSchemeList} from "@/api/procurement/manage";
export default {
  name: "Plan",
  dicts: ["procurement_plan_type","contract_bidding_method","contract_bidding_responsible_org","contract_bidding_state"],
  components: {
    VirtualScroll
  },
  data() {
    return {
      virtualList: [],
      planList: [],
      contractList: [],
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 1,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        procurementPlanCode: undefined,
        procurementPlanName: undefined,
        projectName: undefined,
        procurementOfficer: undefined,
        procurementPlanType: "all",
        projectCode: undefined,
        bidResponsibleOrg:''
      },
      contractVisible: false, //是否展示合约规划选择框
      contractLoading: false, //合约规划选择框loading
      //合约规划查询参数
      contractQuery: {
        pageNumber: 1,
        pageSize: 10,
        contractName: undefined,
        projectId: undefined,
        contractType: undefined,
      },
      contractTotal: 1,
      planLoading: false,
      procurementPlanMode: "masterPlan",
      queryParamsMaster:{
        pageNumber: 1,
        pageSize: 10,
        contractType:'',
        contractName:'',
        biddingMethodCode:'',
        bidResponsibleOrg:'',
        biddingState:''
      },
      planMasterLoading:false,
      planMasterList:[],
      totalAmount:{},
      splitValue:2,
      //推送
      pushStateDialog: false, //是否展示推送状态弹窗
      confirmPushLoading:false,
      pushListLoading:false,
      pushQuery:{
        cpId:undefined,
        role:undefined,
        nickName:undefined
      },
      pushList:[],
      contractPlanList:[],
      pushRoleList:[],
      pushUserList:[],
      selectPushList:[],
      currentData:{}
    };
  },
  mounted(){
    console.log(this.$route.query,'this.$route.query----this.$route.query');
    const param = (this.$route.query.contractPlanningName && JSON.parse(Base64.decode(this.$route.query.contractPlanningName))) || ''
    this.queryParamsMaster.contractName = param
    this.getMasterPlanningList();
    this.queryContractPlanSplitFlag();
  },
  methods: {
    /** 查询采购计划列表 */
    async getPlanList() {
      this.planLoading = true;
      try {
        const query = {
          ...this.queryParams,
          procurementPlanType:
            this.queryParams.procurementPlanType === "all"
              ? undefined
              : this.queryParams.procurementPlanType,
        };
        const res = await getPlanList(query);
        this.planLoading = false;
        this.planList = res.data.rows;
        this.total = res.data.total;
      } catch (err) {
        this.planLoading = false;
        console.log(err);
      }
    },
    /** 查询总采购计划列表 */
    async getMasterPlanningList() {
      this.planMasterLoading = true;
      const { projectId } = this.contractQuery
      try {
        const query = {
          ...this.queryParamsMaster,
          projectId
        };
        const res = await getMasterPlanningList(query);
        this.planMasterLoading = false;
        const { contractPlanningList, totalIncurredPlannedAmountText,totalPlannedAmountInclTaxText,totalPlanningBalanceText } = res.data

        this.planMasterList = contractPlanningList;
        this.totalAmount = {
          totalIncurredPlannedAmountText,
          totalPlannedAmountInclTaxText,
          totalPlanningBalanceText
        }
      } catch (err) {
        this.planMasterLoading = false;
        console.log(err);
      }
    },
    /** 查询项目合约规划列表 */
    async getContractList() {
      this.contractLoading = true;
      try {
        this.contractQuery.contractType = this.contractTypeText;
        const res = await getContractPlanningList(this.contractQuery);
        this.contractLoading = false;
        this.contractList = res.data.rows;
        this.contractTotal = res.data.total;
      } catch (err) {
        this.contractLoading = false;
        console.log(err, "err");
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getPlanList();
    },
    /** 搜索合约规划 */
    searchContract() {
      this.contractQuery.pageNumber = 1;
      this.getContractList();
    },
    /** 新增按钮操作 */
    async handleAdd() {
      this.contractVisible = true;
      this.contractLoading = true;
      this.getContractList();
    },
    //双击选择项目合约规划
    dbClick(row) {
      if(row.surplusQuantity==0){
        this.$message({type:'success',message:"工程量为0，不能新增采购计划"})
      }else{
        this.contractVisible = false;
       let param = Base64.encode(JSON.stringify(row));
       param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
       this.$router.push(`/procurement/add-plan/${param}`);
      }

    },
    /** 跳转方案详情 */
    goDetail(id) {
      let param = Base64.encode(JSON.stringify(id));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/plan-detail/${param}`);
    },
    //切换tab类型
    handleTypeClick(tab) {
      this.queryParams.procurementPlanType = tab.name;
    },
    /** 提交 */
    goSubmit(id, planName) {
      this.$confirm("确定是否提交采购计划：" + planName, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          await submitProcurementPlan(id);
          this.$message.success("提交成功");
          this.getPlanList();
        } catch (error) {}
      });
    },
    /** 作废 **/
    goCancellation(id, planName) {
      this.$confirm("确定要作废采购计划：" + planName, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          await cancellationProcurementPlan(id);
          this.$message.success("作废成功");
          this.getPlanList();
        } catch (error) {}
      });
    },
    /** 修改 */
    goUpdate(row) {
      console.log(row, "row~修改");
      row.type = "update";
      let param = Base64.encode(JSON.stringify(row));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/add-plan/${param}`);
    },
    getSummaries(param){
      const { totalIncurredPlannedAmountText, totalPlannedAmountInclTaxText, totalPlanningBalanceText } = this.totalAmount
      const { columns } = param;
      const sums = [];
      columns.forEach((column, index) => {
        switch(index){
          case 0:
            sums[index] = '合计';
            break;
          case 3:
            sums[index] = totalPlannedAmountInclTaxText;
            break;
          case 4:
            sums[index] = totalIncurredPlannedAmountText;
            break;
          case 5:
            sums[index] = totalPlanningBalanceText;
            break;
          default:
            sums[index] = '';
            break;
        }
      });
      return sums;
    },
    async queryContractPlanSplitFlag(){
      const res = await getContractPlanSplitFlag();
      this.splitValue = res.data;
    },
    async setContractSplitFlag() {
      let flag = this.splitValue === '1' ? "0" : "1";
      const res = await setContractPlanSplitFlag(flag);
      this.splitValue = res.data;
    },
    //推送
    pushState(row){
      this.$nextTick(()=>{
        console.log('%c 🚀 ~ file:plan --method: --line:968 --variable:===>this.$refs.VirtualScroll', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
          this.$refs);
        this.$refs.pushTable.clearSelection()
      })
      this.pushStateDialog = true;
      this.currentData = row;
      // this.getUsersRoleList()
      this.getUsersRoleContractPlanList()
    },
    //搜索用户
    searchUser() {
      this.pushListLoading = true;

      const { role, nickName, cpId } = this.pushQuery;
      let userList = []
      this.pushList.forEach(item => {
        if(!role || !role.length){
          userList.push(...item.userList)
        }else{
          if(role && role.includes(item.roleId)){
            userList.push(...item.userList)
          }
        }
      });
      let mapArr = Array.from(
        new Map(userList.map(item => [item.userId, item])).values()
      );
      // 以 res.data.contractPlanningNoticeVOList 为外层循环
      let combinedList = mapArr.flatMap(user =>
        this.contractPlanList.filter(item => item.cpId == (cpId)).map(notice => ({
          ...notice,
          ...user
        }))
      );
      // 为 this.pushUserList 的每一项添加 uniqueKey
      mapArr = combinedList.map((item, index) => ({
        ...item,
        virtualId: index+1
      }));

      if(nickName){
        this.pushUserList = mapArr.filter(item => item.nickName.includes(nickName))
      }else{
        this.pushUserList = mapArr;
      }
      this.pushListLoading = false;
    },

    //获取角色用户根据合约拆分id和合约拆分code来查询
    async getUsersRoleContractPlanList(){
      this.pushListLoading = true;
      try{
        const res = await getUsersRoleContractPlanList(this.$store.state.project.project.code,this.currentData.contractPlanningCode,this.currentData.contractPlanningId,null)
        this.pushRoleList = res.data.userList.map(item => ({value:item.roleId,label:item.roleName}))
        /* 存储原始的userList */
        this.pushList = res.data.userList
        this.contractPlanList = res.data.contractPlanningNoticeVOList.map((item, index) => ({
          cpId: index+1,
          ...item
        }));
        /* 过滤一遍对应角色下的所有用户 */
        let userList = []
        res.data.userList.forEach(item => {
          /* 获取该角色下的用户列表 */
          userList.push(...item.userList)
        })
        this.pushUserList = Array.from(
          /* 获取该角色下的用户列表 去重 */
          new Map(userList.map(item => [item.userId, item])).values()
        );

        /* 默认选中第一个 */
        let defaultId = this.contractPlanList.filter(item => item.pushStatus == 0)[0].cpId;
        this.$set(this.pushQuery,'cpId',defaultId)



        // 以 res.data.contractPlanningNoticeVOList 为外层循环
        let combinedList = this.pushUserList.flatMap(user =>
          this.contractPlanList.filter(item => item.cpId == (this.pushQuery.cpId)).map(notice => ({
            ...notice,
            ...user
          }))
        );
        // 为 this.pushUserList 的每一项添加 uniqueKey
        this.pushUserList = combinedList.map((item, index) => ({
          ...item,
          virtualId: index+1
        }));

      }catch(err){
        console.log(err);
      }
      this.pushListLoading = false;
      // this.closePushStateDialog()
    },
    //选择推送用户
    handleSelectionChange(selection){
      this.selectPushList = selection.map(item => ({userId:item.userId,nickName:item.nickName}))
      console.log(this.selectPushList,'选择推送用户------------------------');
    },
    //确定选择推送用户
    async confirmPlan(){
      this.confirmPushLoading = true;
      if(!this.selectPushList.length) return this.$message({type:'error',message:"请选择推送用户"});
      console.log(this.currentData,'currentData-~~~~~~~~~~~~~~~~~~~~');
      try{
        const { contractPlanningName, contractPlanningId, contractPlanningCode, enterIntoTime,biddingTime } = this.currentData
        const { splitContractId, procurementSchemeCode, schemeId, noticeId, planId } = this.contractPlanList.filter(item => item.cpId == (this.pushQuery.cpId))[0];
        let url = Base64.encode(JSON.stringify(contractPlanningName));
        url = encodeURIComponent(url); //避免base64编码中出现"/"时路由404


        let paramUrl = Base64.encode(JSON.stringify(planId));
        paramUrl = encodeURIComponent(paramUrl); //避免base64编码中出现"/"时路由404
        paramUrl = `/procurement/plan-detail/${paramUrl}`;



        let formData = {
          userList:this.selectPushList,
          contractPlanningName,/* 这个也填吧 */
          contractPlanningId,/* 这个必填。 */
          contractPlanningCode,/* 这个必填。 */
          splitContractId,/* 合约拆分id这个必填。 */
          procurementSchemeCode,/* 采购方案code */
          schemeId,/* 采购方案id */
          noticeId,/* 招标id */
          enterIntoTime,
          biddingTime,
          projectCode:this.$store.state.project.project.code,
          // redirectUrl:`/procurement/plan?contractPlanningName=${url}`
          redirectUrl: paramUrl
        }
        console.log('%c👽 推送参数', `font-size: 20px;background-color: #f00;`, formData);
        await pushProcurementPlan(formData)
        this.$message.success("推送成功");
        this.pushStateDialog = false;
        this.getMasterPlanningList();
      }catch(err){
        console.log(err);
      }
      this.confirmPushLoading = false;
    },
    selPushKey(row){
      return row.virtualId
    },
    closePushStateDialog(){
      this.selectPushList = [];
      this.pushQuery = {
        cpId:undefined,
        role:undefined,
        nickName:undefined
      }
      this.$nextTick(()=>{
        this.$refs.pushTable.clearSelection()
      })

    }
  },
  computed: {
    contractTypeText() {
      return this.queryParams.procurementPlanType === "all"
        ? undefined
        : this.queryParams.procurementPlanType;
    },
    ...mapGetters(["project"]),
  },
  watch: {
    pushStateDialog(val){
      if(!val){
        this.closePushStateDialog()
      }
    },
    /** 监控类型切换 */
    "queryParams.procurementPlanType": {
      handler(val) {
        this.getPlanList();
      },
    },
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.contractQuery.projectId = newVal.id;
          this.queryParams = {
            pageNumber: 1,
            pageSize: 10,
            procurementPlanCode: undefined,
            procurementPlanName: undefined,
            projectName: undefined,
            procurementOfficer: undefined,
            procurementPlanType: "all",
            projectCode: newVal.code,
            bidResponsibleOrg:''
          };
          this.getPlanList();
        }
      },
      immediate: true,
    },
  },
};
</script>
