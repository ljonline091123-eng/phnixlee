<template>
  <div class="app-container">
    <Drag>
      <template v-slot:left-content>
        <div class="dept-tree-wrap">
          <el-input
            v-model="filterText"
            placeholder="输入名称筛选"
            size="small"
            clearable
            prefix-icon="el-icon-search"
            class="tree-filter"
          />
          <el-tree
            v-loading="treeLoading"
            :data="deptOptions"
            class="tree_expert"
            :props="{ label: 'label', children: 'children' }"
            :filter-node-method="filterNode"
            ref="tree"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          />
        </div>
      </template>

      <template v-slot:right-content>
        <el-form :model="queryParams" class="ledger-summary-filter" inline>
          <el-form-item label="项目名称">
            <el-input
              v-model="queryParams.projectKeyword"
              placeholder="请输入项目名称"
              clearable
              style="width: 180px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="采购需求类型">
            <el-select
              v-model="queryParams.demandType"
              placeholder="全部"
              clearable
              style="width: 160px"
            >
              <el-option
                v-for="item in demandTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">查询</el-button>
            <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
            <el-button type="warning" icon="el-icon-download" size="small" @click="handleExport">导出</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          :height="tableHeight"
        >
          <el-table-column prop="orgName" label="组织机构" min-width="180" show-overflow-tooltip />
          <el-table-column prop="projectCode" label="项目编号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="projectName" label="项目名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="demandTypeText" label="采购需求类型" min-width="120" />
          <el-table-column v-for="st in statusColumns" :key="st.key" :label="st.label" width="100" align="center">
            <template slot-scope="scope">
              <a
                class="link-type summary-count"
                @click="goDetail(scope.row, st.key)"
              >{{ scope.row[st.key] || 0 }}</a>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </Drag>
  </div>
</template>

<script>
import Drag from "@/components/Drag/index.vue";
import { getDeptTree } from "@/api/system/dept";
import { getPurchaseLedgerSummary } from "@/api/reportForm/purchaseLedger";
import { formatDate } from "@/utils";

export default {
  name: "PurchaseLedger",
  components: { Drag },
  data() {
    return {
      filterText: "",
      treeLoading: false,
      loading: false,
      deptOptions: [],
      tableData: [],
      tableHeight: window.innerHeight - 220,
      statusColumns: [
        { key: "pending", label: "待招采" },
        { key: "preopen", label: "待开标" },
        { key: "preaward", label: "待定标" },
        { key: "completed", label: "已完成" },
        { key: "exception", label: "异常/终止" }
      ],
      // 与字典 procurement_plan_type 一致
      demandTypeOptions: [
        { value: 1, label: "购买材料" },
        { value: 2, label: "租赁材料" },
        { value: 3, label: "租赁机械（设备）" },
        { value: 4, label: "专业分包" },
        { value: 5, label: "劳务分包" },
        { value: 6, label: "其他" }
      ],
      queryParams: {
        id: null, // 左树选中组织(thrid_dept_id)
        projectKeyword: null,
        demandType: null
      }
    };
  },
  watch: {
    filterText(val) {
      this.$refs.tree && this.$refs.tree.filter(val);
    }
  },
  created() {
    this.loadDeptTree();
  },
  methods: {
    /**
     * 左树：与顶部"单位-项目"选择框用同一个接口（/system/dept/getDeptTree），
     * 保证两边展示一致；返回已是 TreeSelect 树（label/children/thridDeptId），无需再组树
     */
    loadDeptTree() {
      this.treeLoading = true;
      getDeptTree().then(res => {
        this.deptOptions = res.data || [];
        const userInfo = this.$store.state.user.userInfo || {};
        this.queryParams.id = userInfo.thridOrgId || (this.deptOptions[0] && this.deptOptions[0].thridDeptId);
        this.getList();
      }).finally(() => {
        this.treeLoading = false;
      });
    },
    filterNode(value, data) {
      if (!value) return true;
      return data.label && data.label.indexOf(value) !== -1;
    },
    handleNodeClick(data) {
      this.queryParams.id = data.thridDeptId || data.id;
      this.getList();
    },
    handleQuery() {
      this.getList();
    },
    resetQuery() {
      this.queryParams.projectKeyword = null;
      this.queryParams.demandType = null;
      this.getList();
    },
    getList() {
      this.loading = true;
      getPurchaseLedgerSummary(this.queryParams).then(res => {
        this.tableData = res.data || [];
      }).finally(() => {
        this.loading = false;
      });
    },
    /** 点击统计数字 → 明细宽表，带入 状态/组织/项目/需求类型 */
    goDetail(row, status) {
      const query = {
        status: status,
        orgId: row.orgId,
        projectCode: row.projectCode,
        demandType: row.demandType
      };
      this.$router.push({
        path: "/tender-procurement/reportForm/purchaseLedgerDetail",
        query
      });
    },
    /** 导出：沿用现有报表模式，全局 download 直连后端导出接口 */
    handleExport() {
      this.$confirm("确认导出当前条件下的采购台账汇总数据吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        this.download(
          "business/report/purchaseLedgerSummaryExport",
          { ...this.queryParams },
          `${formatDate(new Date())}采购台账汇总.xlsx`
        );
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.dept-tree-wrap {
  padding: 8px;
  .tree-filter {
    margin-bottom: 8px;
  }
  .tree_expert {
    max-height: calc(100vh - 180px);
    overflow: auto;
  }
}
.ledger-summary-filter {
  margin-bottom: 12px;
}
.summary-count {
  font-weight: 600;
}
</style>
