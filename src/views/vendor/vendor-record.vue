<template>
  <div class="app-container">
    <div class="context flex flex-column">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
        <el-form-item
          label="供应商名称"
          prop="enterpriseName"
          label-width="100px"
        >
          <el-input
            v-model="queryParams.enterpriseName"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName" label-width="68px">
          <el-input
            v-model="queryParams.contactName"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone" label-width="80px">
          <el-input
            v-model="queryParams.contactPhone"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="企业所在地" prop="addressCode" label-width="100px">
          <el-cascader
            separator="-"
            :options="addressOptions"
            placeholder="请选择企业所在地"
            :props="{
              checkStrictly: true,
              value: 'divisionCode',
              label: 'divisionName',
            }"
            @focus="handleAddressFocus"
            @change="handleAddressChange"
            clearable
          >
          </el-cascader>
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
      <el-table
        v-loading="loading"
        :data="recordList"
        stripe
        highlight-current-row
        border
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          min-width="300"
          label="供应商名称"
          prop="vendorName"
          show-overflow-tooltip
        />
        <el-table-column
          min-width="200"
          label="统一社会信用代码"
          align="center"
          prop="socialCreditCode"
        />
        <el-table-column
          min-width="100"
          label="联系人"
          align="center"
          prop="contactName"
        />
        <el-table-column
          min-width="150"
          label="联系电话"
          align="center"
          prop="contactPhone"
        />
        <el-table-column
          min-width="150"
          label="注册资本(万元)"
          align="right"
          prop="registeredCapitalText"
        />
        <el-table-column
          min-width="150"
          label="企业所在地"
          align="center"
          prop="vendorAddress"
        />
        <el-table-column
          min-width="150"
          label="供应商合作金额"
          align="right"
          prop="cooperationAmountText"
        />
        <el-table-column
          min-width="150"
          label="供应商评价"
          align="center"
          prop="excellentNum"
        />
        <el-table-column min-width="100" label="合作单位" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="viewCompany(row.id)"
              >查看详情</el-button
            >
          </template>
        </el-table-column>
        <el-table-column min-width="100" label="合作记录" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="viewRecord(row.id)"
              >查看详情</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
        @pagination="getVendorRecordList"
      />
    </div>
    <!-- 合作单位 -->
    <el-dialog
      title="供应商合作单位"
      :visible.sync="companyVisible"
      width="60%"
    >
      <el-tabs v-model="tabName" @tab-click="changeTabs">
        <el-tab-pane label="合作单位" name="company">
          <el-table
            v-loading="companyLoading"
            :data="companyList"
            border
            stripe
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
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
        </el-tab-pane>
        <el-tab-pane label="履约评价" name="evaluate">
          <el-table
            v-loading="performanceLoading"
            :data="performanceList"
            border
            stripe
            show-summary
            highlight-current-row
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
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
            <el-table-column
              label="差"
              align="center"
              prop="badCount"
              width="80"
            />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>
<script>
import { Base64 } from "js-base64";
import {
  getVendorRecordList,
  getVendorCooperativePartner,
  listVendorPerformance,
} from "@/api/vendor/vendor";
import { listAreaDivisionTree } from "@/api/procurement/manage";
export default {
  data() {
    return {
      queryParams: {
        enterpriseName: undefined,
        contactPhone: undefined,
        contactName: undefined,
        pageNumber: 1,
        pageSize: 10,
        addressCode: undefined,
      },
      recordList: [],
      total: 0,
      loading: false,
      //合作单位
      companyParams: {
        vendorId: undefined,
        pageNumber: 1,
        pageSize: 10,
      },
      tabName: "company",
      companyVisible: false,
      companyList: [],
      companyLoading: false,
      companyTotal: 0,
      //履约评价
      performanceList: [],
      performanceLoading: false,
      addressOptions: [],
    };
  },
  created() {
    this.getVendorRecordList();
  },
  methods: {
    async getVendorRecordList() {
      this.loading = true;
      try {
        const res = await getVendorRecordList(this.queryParams);
        this.recordList = res.data.rows;
        this.total = res.data.total;
      } catch (err) {
        console.log(err);
      }
      this.loading = false;
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getVendorRecordList();
    },
    //查看合作单位
    async viewCompany(id) {
      this.companyVisible = true;
      this.tabName = "company";
      this.companyParams.vendorId = id;
      this.getVendorCooperativePartner();
    },
    //获取合作单位
    async getVendorCooperativePartner() {
      try {
        const res = await getVendorCooperativePartner(this.companyParams);
        this.companyList = res.data.rows;
        this.companyTotal = res.data.total;
        console.log(res, "合作单位");
      } catch (err) {
        console.log(err);
      }
    },
    //获取履约评价
    async listVendorPerformance() {
      try {
        const res = await listVendorPerformance(this.companyParams.vendorId);
        this.performanceList = res.data;
      } catch (err) {
        console.log(err);
      }
    },
    //查看合作记录
    viewRecord(id) {
      let param = Base64.encode(JSON.stringify(id));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/vendor/vendor-record-detail/${param}`);
    },
    //切换tabs
    changeTabs({ name }) {
      console.log(name, "name------------");
      if (name === "company") {
        this.getVendorCooperativePartner();
      }
      if (name === "evaluate") {
        this.listVendorPerformance();
      }
    },
    //获取省市区
    async listAreaDivisionTree() {
      try {
        const res = await listAreaDivisionTree();
        this.addressOptions = res.data.map((item) => ({
          divisionName: item.divisionName,
          divisionCode: item.divisionCode,
          children: item.children.map((subItem) => ({
            divisionName: subItem.divisionName,
            divisionCode: subItem.divisionCode,
          })),
        }));
        console.log(res, "省市树");
      } catch (err) {
        console.log(err);
      }
    },
    handleAddressFocus() {
      this.listAreaDivisionTree();
    },
    handleAddressChange(value) {
      this.queryParams.addressCode = value.join(",");
    },
  },
};
</script>
<style lang="scss" scope></style>
