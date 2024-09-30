<template>
  <div class="app-container">
    <div class="context flex flex-column">

      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
      >
        <el-form-item label="易料清单编号" prop="schemeCode" label-width="100px">
          <el-input
            v-model="queryParams.businessCode"
            placeholder="请输入易料清单编号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="关联商务策划编号" prop="schemeName" label-width="140px">
          <el-input
            v-model="queryParams.materialsUniqueId"
            placeholder="请输入关联商务策划编号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="项目负责人" prop="schemeName" label-width="100px">
          <el-input
            v-model="queryParams.procurementSchemeName"
            placeholder="请输入项目负责人"
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
      <el-table
        v-loading="loading"
        :data="marketList"
        highlight-current-row
        stripe
        border
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="易料清单编号"
          min-width="200"
          prop="businessCode"
          show-overflow-tooltip
          />
        <el-table-column
          label="关联商务策划编号"
          min-width="200"
          prop="materialsUniqueId"
          show-overflow-tooltip
        />
        <el-table-column
          min-width="200"
          label="采购名称"
          prop="procurementPlanName"
          show-overflow-tooltip
        />
        <el-table-column
          min-width="150"
          label="合约类别"
          prop="procurementPlanTypeText"
        />
        <el-table-column
          min-width="100"
          label="采购层级"
          align="center"
          prop="projectHierarchy"
        />
        <el-table-column
          min-width="100"
          label="项目负责人"
          align="center"
          prop="purchaseLevel"
        />
        <el-table-column
          min-width="180"
          label="创建时间"
          align="center"
          prop="createTime"
        />
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
        @pagination="getMarkeyList"
      />
    </div>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import { mapGetters } from "vuex";
import { getMarkeyList } from "@/api/procurement/plan";

export default {
  name: "Market",
  data() {
    return {
      marketList: [],
      // 遮罩层
      loading: false,
      // 总条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        materialsUniqueId: undefined,
        businessCode: undefined,
        projectCode: undefined,
      }
    };
  },
  methods: {
    /** 获取易料列表 */
    async getMarkeyList() {
      this.loading = true;
      try {
        const res = await getMarkeyList(this.queryParams);
        this.loading = false;
        if (res.data) {
          this.marketList = res.data.rows;
          this.total = res.data.total;
        }
      } catch (err) {
        this.loading = false;
        console.log(err);
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getMarkeyList();
    },
    /** 跳转方案详情 */
    goDetail(id) {
      let param = Base64.encode(JSON.stringify(id));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/scheme-detail/${param}`);
    }
  },
  computed: {
    ...mapGetters(["project"]),
  },
  watch: {
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.queryParams = {
            pageNumber: 1,
            pageSize: 10,
            materialsUniqueId: undefined,
            businessCode: undefined,
            projectCode: newVal.code,
          };
          this.getMarkeyList();
        }
      },
      immediate: true,
    },
  },
};
</script>
<style lang="scss" scoped>
.hxwd_table_item_center_first {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hxwd_table_item_center {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #eaeaea;
}

.hxwd_table_item_left_first {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
}

.hxwd_table_item_left {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  border-top: 1px solid #eaeaea;
}
</style>
