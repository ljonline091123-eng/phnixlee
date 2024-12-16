<template>
  <div class="app-container">
    <div class="context">
      <el-radio-group
        v-model="queryParams.vendorClass"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in vendorState"
          :key="dict.value"
        >
          {{ dict.label }}
        </el-radio-button>
      </el-radio-group>

      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
      >
        <el-form-item
          label="供应商名称"
          prop="enterpriseName"
          label-width="90px"
        >
          <el-input
            v-model="queryParams.enterpriseName"
            placeholder="请输入供应商名称"
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
      <el-radio-group
        v-if="queryParams.vendorClass == '1'"
        v-model="queryParams.processType"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in pendingVendorState"
          :key="dict.value"
        >
          {{ dict.label }}
        </el-radio-button>
      </el-radio-group>
      <el-table
        v-loading="vendorLoading"
        :data="vendorList"
        highlight-current-row
        border
        stripe
        size="small"
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="供应商名称"
          align="left"
          width="250"
          prop="enterpriseName"
          show-overflow-tooltip
        >
          <template slot-scope="{ row }">
            <a class="link-type" @click="goDetail(row.id,queryParams.vendorClass)">
              {{ row.enterpriseName }}
            </a>
          </template>
        </el-table-column>
        <el-table-column
          label="合作记录"
          min-width="100"
          align="center"
          v-if="['0', '2', '3'].includes(queryParams.vendorClass)"
        >
          <template
            slot-scope="{ row }"
            v-if="['0', '2', '3'].includes(queryParams.vendorClass)"
          >
            <el-button type="text" size="small" @click="viewRecord(row.id)"
              >查看详情</el-button
            >
          </template>
        </el-table-column>
        <el-table-column
          label="统一社会信用代码"
          min-width="200"
          align="center"
          prop="socialCreditCode"
        />
        <el-table-column
          width="120"
          label="注册资金(万元)"
          align="right"
          prop="registeredCapitalText"
        />
        <el-table-column
          width="150"
          label="企业所在地"
          align="center"
          prop="vendorLocation"
        />
        <el-table-column
          width="150"
          label="企业分类"
          prop="enterpriseTypeText"
          show-overflow-tooltip
        />
        <el-table-column
          width="180"
          label="提交申请时间"
          align="center"
          prop="createTime"
        />
        <el-table-column
          width="120"
          label="供应商库类型"
          align="center"
          prop="vendorLibraryText"
        />
        <el-table-column
          width="180"
          label="首次注册合作单位"
          align="center"
          prop="firstCooperationCompanyName"
          show-overflow-tooltip
        />
        <el-table-column
          width="120"
          label="供应商合作金额"
          align="right"
          prop="cooperationAmountText"
          v-if="['0', '2', '3'].includes(queryParams.vendorClass)"
        />
        <el-table-column
          width="120"
          label="供应商评价"
          align="center"
          v-if="['0', '2', '3'].includes(queryParams.vendorClass)"
        >
          <template
            slot-scope="{ row }"
            v-if="['0', '2', '3'].includes(queryParams.vendorClass)"
          >
            <a class="link-type" @click="viewVendorPerformance(row.id)">
              {{ row.excellentNum }}
            </a>
          </template>
        </el-table-column>
        <el-table-column
          min-width="100"
          label="合作单位"
          align="center"
          v-if="['0', '2', '3'].includes(queryParams.vendorClass)"
        >
          <template
            slot-scope="{ row }"
            v-if="['0', '2', '3'].includes(queryParams.vendorClass)"
          >
            <el-button type="text" size="small" @click="viewCompany(row.id)"
              >查看详情</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination_item">
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
        @pagination="getVendorList"
      />
    </div>

    <!-- 供应商合作单位 -->
    <el-dialog
      title="供应商合作单位"
      :visible.sync="companyVisible"
      width="60%"
    >
      <el-table
        v-loading="companyLoading"
        :data="companyList"
        size="small"
        border
        stripe
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="供应商名称"
          prop="vendorName"
          show-overflow-tooltip
        />
        <el-table-column
          label="合作单位"
          prop="cooperativePartnerName"
          show-overflow-tooltip
        />
        <el-table-column
          label="合同名称"
          prop="agreementName"
          show-overflow-tooltip
        />
        <el-table-column
          label="合同签订日期"
          width="180"
          align="center"
          prop="agreementSignDate"
        />
      </el-table>
      <pagination
        v-show="companyTotal > 0"
        :total="companyTotal"
        :page.sync="companyParams.pageNumber"
        :limit.sync="companyParams.pageSize"
        @pagination="getVendorCooperativePartner"
      />
    </el-dialog>

    <el-dialog
      title="供应商履约评价"
      :visible.sync="performanceVisible"
      width="60%"
    >
      <el-table
        v-loading="performanceLoading"
        :data="performanceList"
        border
        stripe
        show-summary
        highlight-current-row
        size="small"
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="供应商名称"
          prop="vendorName"
          show-overflow-tooltip
        />
        <el-table-column
          label="合作单位"
          prop="cooperator"
          show-overflow-tooltip
        />
        <el-table-column
          label="合同名称"
          prop="agreementName"
          show-overflow-tooltip
        />
        <el-table-column
          label="优"
          align="center"
          prop="excellentCount"
          width="80"
        />
        <el-table-column
          label="良"
          align="center"
          prop="goodCount"
          width="80"
        />
        <el-table-column
          label="合格"
          align="center"
          prop="qualifiedCount"
          width="80"
        />
        <el-table-column label="差" align="center" prop="badCount" width="80" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import {
  getVendorList,
  getVendorCooperativePartner,
  listVendorPerformance,
} from "@/api/vendor/vendor";

export default {
  name: "vendor-base",
  data() {
    return {
      vendorLoading: false,
      vendorList: [],
      vendorState: [
        { label: "供应商名录", value: "0" },
        { label: "战略供应商", value: "3" },
        { label: "合格供应商", value: "2" },
        { label: "待审供应商", value: "1" },
        { label: "黑名单", value: "4" },
      ],
      pendingVendorState: [
        { label: "注册待审", value: "10" },
        { label: "修改信息待审", value: "2" },
        { label: "修改等级", value: "9" },
        { label: "移入移出黑名单待审", value: "3" },
      ],
      contractList: [],
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 1,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        enterpriseName: undefined,
        contactName: undefined,
        contactPhone: undefined,
        vendorClass: "0",
        processType: "10",
      },
      //合约规划查询参数
      contractQuery: {
        pageNumber: 1,
        pageSize: 10,
        contractName: undefined,
      },
      contractTotal: 1,
      //合作单位
      companyVisible: false,
      companyList: [],
      companyLoading: false,
      companyTotal: 0,
      companyParams: {
        vendorId: undefined,
        pageNumber: 1,
        pageSize: 10,
      },
      //履约评价
      performanceVisible: false,
      performanceList: [],
      performanceLoading: false,
    };
  },
  created() {
    if(this.$route.query?.vendorClass){
      this.queryParams.vendorClass=this.$route.query?.vendorClass || ""
    }
    this.getVendorList();
  },
  methods: {
    /** 查询采购计划列表 */
    async getVendorList() {
      console.log(JSON.stringify(this.queryParams) + "processType");
      console.log(JSON.stringify(this.queryParams.processType) + "processType");
      this.vendorLoading = true;
      try {
        const query = { ...this.queryParams };
        const res = await getVendorList(query);
        this.vendorLoading = false;
        this.vendorList = res.data.rows;
        this.total = res.data.total;
      } catch (err) {
        this.vendorLoading = false;
        console.log(err);
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getVendorList();
    },
    /** 跳转方案详情 */
    goDetail(id,vendorClass) {
      let param = Base64.encode(JSON.stringify({id,vendorClass}));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/vendor/vendor-detail/${param}`);
    },
    /** 合作记录 **/
    viewRecord(id) {
      let param = Base64.encode(JSON.stringify(id));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/vendor/vendor-record-detail/${param}`);
    },
    /** 合作单位 */
    viewCompany(id) {
      this.companyVisible = true;
      this.companyParams.vendorId = id;
      this.getVendorCooperativePartner();
    },
    //获取合作单位
    async getVendorCooperativePartner() {
      try {
        this.companyLoading = true;
        const res = await getVendorCooperativePartner(this.companyParams);
        this.companyLoading = false;
        this.companyList = res.data.rows;
        this.companyTotal = res.data.total;
      } catch (err) {
        console.log(err);
      }
    },
    /** 供应商评价 */
    viewVendorPerformance(id) {
      this.performanceVisible = true;
      this.listVendorPerformance(id);
    },
    async listVendorPerformance(id) {
      try {
        this.performanceLoading = true;
        const res = await listVendorPerformance(id);
        this.performanceLoading = false;
        this.performanceList = res.data;
      } catch (err) {
        console.log(err);
      }
    },
  },
  watch: {
    /** 监控类型切换 */
    "queryParams.vendorClass": {
      handler(val) {
        console.log(JSON.stringify(val) + "vendorClass");
        this.getVendorList();
      },
    },
    "queryParams.processType": {
      handler(val) {
        console.log(JSON.stringify(val) + "processType");
        this.getVendorList();
      },
    },
  },
};
</script>
