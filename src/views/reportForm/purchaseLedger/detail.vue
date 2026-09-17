<template>
  <div class="app-container ledger-detail">
    <BackButton :path="backPath" title="采购台账明细" />

    <!-- context：项目通用留白容器，避免内容贴着左侧菜单 -->
    <div class="context">
      <el-tabs v-model="queryParams.status" class="ledger-status-tabs" @tab-click="handleTabClick">
        <!-- 全部页签 name 必须给非空值"all"：element-ui 的 paneName 是 name||index，空串会退化成索引"0"传给后端 -->
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane v-for="st in statusTabs" :key="st.key" :label="st.label" :name="st.key" />
      </el-tabs>

      <el-form :model="queryParams" class="ledger-detail-filter" inline>
        <el-form-item label="组织机构">
          <!-- 参数名用 id：与左树/其它报表一致，后端按第三方部门id(thrid_dept_id)收敛权限 -->
          <el-select v-model="queryParams.id" placeholder="全部" clearable filterable style="width: 180px">
            <el-option v-for="item in orgOptions" :key="item.thridDeptId" :label="item.label" :value="item.thridDeptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="queryParams.projectKeyword" placeholder="请输入项目名称" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="采购类别">
          <el-select v-model="queryParams.demandType" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in demandTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="采购方式">
          <el-select v-model="queryParams.method" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in methodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划/方案编号或名称">
          <el-input v-model="queryParams.keyword" placeholder="编号或名称模糊查询" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="采购经办人">
          <el-input v-model="queryParams.handler" placeholder="采购方案负责人" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="计划完成时间">
          <el-date-picker
            v-model="planFinishRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
          <el-button type="warning" icon="el-icon-download" size="small" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>

      <!-- 列序对齐《采购台账字段字典.xlsx》32 字段；列集按状态页签切换：
           待招采/待开标只显示到"异常情况"；待定标加开标/定标时间；
           已完成/全部/异常显示全部列（含结果发布时间/中标/节约/合同）。
           :key 强制切页签时表格重新渲染，避免 v-if 列切换残留 -->
      <el-table
        :key="queryParams.status"
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column label="序号" width="64" align="center" fixed="left">
          <template slot-scope="scope">{{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="orgName" label="组织机构" width="180" show-overflow-tooltip />
        <el-table-column prop="projectCode" label="项目编号" width="130" show-overflow-tooltip />
        <el-table-column prop="projectName" label="项目名称" width="180" show-overflow-tooltip />
        <el-table-column prop="planCode" label="计划编号" width="150" show-overflow-tooltip />
        <el-table-column prop="planName" label="采购名称" width="180" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.planName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="contractCategory" label="合约类别" width="110" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.contractCategory || '—' }}</template>
        </el-table-column>
        <el-table-column prop="contractName" label="合约名称" width="220" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.contractName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="planAmount" label="计划金额（含税）" width="150" align="right">
          <template slot-scope="scope">{{ formatMoney(scope.row.planAmount) }}</template>
        </el-table-column>
        <el-table-column prop="purchaser" label="采购人" width="110" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.purchaser || '—' }}</template>
        </el-table-column>
        <el-table-column prop="planFinishTime" label="计划完成时间" width="120">
          <template slot-scope="scope">{{ formatDate(scope.row.planFinishTime) }}</template>
        </el-table-column>
        <el-table-column prop="schemeCode" label="任务编号（采购方案）" width="180" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.schemeCode || '—' }}</template>
        </el-table-column>
        <el-table-column prop="schemeName" label="任务名称（采购方案）" width="220" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.schemeName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="demandTypeText" label="采购类别" width="110" />
        <el-table-column prop="methodText" label="采购方式" width="110">
          <template slot-scope="scope">{{ scope.row.methodText || '待确定' }}</template>
        </el-table-column>
        <el-table-column prop="controlAmount" label="控制金额/上限价（含税）" width="180" align="right">
          <template slot-scope="scope">{{ formatMoney(scope.row.controlAmount) }}</template>
        </el-table-column>
        <el-table-column prop="handler" label="采购经办人" width="110" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.handler || '—' }}</template>
        </el-table-column>
        <el-table-column prop="currentStage" label="当前环节" width="140" show-overflow-tooltip />
        <el-table-column prop="statusText" label="采购状态" width="110" align="center">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">{{ scope.row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <!-- ↓ 以下列按状态页签动态显示（待定标起显示开标/定标时间，已完成起显示结果与合同列） -->
        <el-table-column v-if="showOpenAward" prop="openTime" label="开标时间" width="130">
          <template slot-scope="scope">{{ formatDate(scope.row.openTime) }}</template>
        </el-table-column>
        <el-table-column v-if="showOpenAward" prop="awardTime" label="定标时间" width="130">
          <template slot-scope="scope">{{ formatDate(scope.row.awardTime) }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="resultPublishTime" label="结果发布时间" width="140">
          <template slot-scope="scope">{{ formatDate(scope.row.resultPublishTime) }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="supplierName" label="中标/成交单位" width="190" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.supplierName || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="awardAmount" label="中标/成交金额（含税）" width="170" align="right">
          <template slot-scope="scope">{{ formatMoney(scope.row.awardAmount) }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="savingAmount" label="节约金额（含税）" width="150" align="right">
          <template slot-scope="scope">{{ formatMoney(scope.row.savingAmount) }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="savingRate" label="节约率" width="90" align="right">
          <template slot-scope="scope">{{ scope.row.savingRate == null ? '—' : scope.row.savingRate + '%' }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="contractCode" label="合同编号" width="150" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.contractCode || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="agreementName" label="合同名称" width="180" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.agreementName || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="agreementAmount" label="合同金额（含税）" width="150" align="right">
          <template slot-scope="scope">{{ formatMoneyList(scope.row.agreementAmount) }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="contractStateText" label="合同状态" width="110" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.contractStateText || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="showResult" prop="agreementSignDate" label="合同签订日期" width="120">
          <template slot-scope="scope">{{ scope.row.agreementSignDate || '—' }}</template>
        </el-table-column>
        <!-- ↑ 以上列按状态页签动态显示 -->
        <el-table-column prop="exceptionInfo" label="异常情况" width="180" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.exceptionInfo || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="goDetail(scope.row)">查看</el-button>
            <el-button v-if="showResult && scope.row.agreementId" type="text" size="small" @click="goContractDetail(scope.row)">合同详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import BackButton from "@/components/BackButton/index.vue";
import { getDeptTree } from "@/api/system/dept";
import { getPurchaseLedgerList } from "@/api/reportForm/purchaseLedger";
import { formatDate } from "@/utils";

export default {
  name: "PurchaseLedgerDetail",
  components: { BackButton },
  data() {
    return {
      backPath: "/tender-procurement/reportForm/purchaseLedger",
      loading: false,
      tableData: [],
      total: 0,
      statusTabs: [
        { key: "pending", label: "待招采" },
        { key: "preopen", label: "待开标" },
        { key: "preaward", label: "待定标" },
        { key: "completed", label: "已完成" },
        { key: "exception", label: "异常/终止" }
      ],
      orgOptions: [],
      demandTypeOptions: [
        { value: 1, label: "购买材料" },
        { value: 2, label: "租赁材料" },
        { value: 3, label: "租赁机械（设备）" },
        { value: 4, label: "专业分包" },
        { value: 5, label: "劳务分包" },
        { value: 6, label: "其他" }
      ],
      methodOptions: [
        { value: 1, label: "公开招标" },
        { value: 2, label: "邀请招标" },
        { value: 3, label: "询价" },
        { value: 4, label: "单一来源" },
        { value: 5, label: "比选" },
        { value: 6, label: "竞争性谈判" }
      ],
      planFinishRange: [],
      queryParams: {
        status: "all", // 页签值：all=全部(查询时不传)，其余为五类状态
        id: null,
        projectCode: null,
        projectKeyword: null,
        demandType: null,
        method: null,
        keyword: null,
        handler: null,
        pageNum: 1,
        pageSize: 10
      }
    };
  },
  computed: {
    /** 待定标/已完成/全部/异常 显示开标、定标时间列 */
    showOpenAward() {
      return ["preaward", "completed", "exception", "all"].indexOf(this.queryParams.status) !== -1;
    },
    /** 已完成/全部/异常 显示结果发布时间、中标、节约、合同相关列 */
    showResult() {
      return ["completed", "exception", "all"].indexOf(this.queryParams.status) !== -1;
    }
  },
  created() {
    const q = this.$route.query || {};
    // 汇总页点击数字带入：状态/组织orgId/项目编号/需求类型；直接进入时默认"全部"页签
    this.queryParams.status = q.status || "all";
    this.queryParams.id = q.orgId || q.id || null;
    this.queryParams.projectCode = q.projectCode || null;
    this.queryParams.demandType = q.demandType ? Number(q.demandType) : null;
    this.getOrgOptions();
    this.getList();
  },
  methods: {
    /** 组装查询参数：'all'页签不传状态(null 会被序列化跳过)，拼接计划完成时间区间 */
    buildParams() {
      const params = Object.assign({}, this.queryParams);
      if (params.status === "all") {
        params.status = null;
      }
      if (this.planFinishRange && this.planFinishRange.length === 2) {
        params.planFinishBegin = this.planFinishRange[0];
        params.planFinishEnd = this.planFinishRange[1];
      } else {
        params.planFinishBegin = null;
        params.planFinishEnd = null;
      }
      return params;
    },
    getOrgOptions() {
      // 与顶部"单位-项目"选择框用同一个接口，保证组织下拉与顶部/汇总页左树一致
      getDeptTree().then(res => {
        const flat = [];
        const walk = list => (list || []).forEach(n => {
          flat.push(n);
          walk(n.children);
        });
        walk(res.data);
        this.orgOptions = flat;
      });
    },
    handleTabClick() {
      this.queryParams.pageNum = 1;
      // 切到"全部"= 看全量数据：自动清空所有筛选条件（与"重置"等效）
      if (this.queryParams.status === "all") {
        this.clearFilters();
      }
      this.getList();
    },
    /** 清空筛选条件（不动当前页签） */
    clearFilters() {
      Object.assign(this.queryParams, {
        id: null,
        projectCode: null,
        projectKeyword: null,
        demandType: null,
        method: null,
        keyword: null,
        handler: null,
        pageNum: 1
      });
      this.planFinishRange = [];
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.clearFilters();
      this.getList();
    },
    getList() {
      this.loading = true;
      getPurchaseLedgerList(this.buildParams()).then(res => {
        // 后端返回 AjaxResult.success(Map{total,list})，取 res.data
        this.total = (res.data && res.data.total) || 0;
        this.tableData = (res.data && res.data.list) || [];
      }).finally(() => {
        this.loading = false;
      });
    },
    /** 穿透：待招采行→采购计划详情；任务行→招标管理详情 */
    goDetail(row) {
      if (row.grain === "SPLIT") {
        if (!row.planId) return this.$message.info("该记录无关联采购计划");
        const param = Base64.encode(JSON.stringify(row.planId));
        window.open(`/procurement/plan-detail/${encodeURIComponent(param)}`, "_blank");
      } else {
        if (!row.recordId) return this.$message.info("该记录无关联采购任务");
        const obj = {
          id: row.recordId,
          noticeId: row.noticeId,
          procurementType: row.method,
          noticeStatus: row.noticeStatus
        };
        const param = Base64.encode(JSON.stringify(obj));
        window.open(`/procurement/tendering/${encodeURIComponent(param)}`, "_blank");
      }
    },
    /** 穿透合同详情：契约与 sign-contract.vue 一致 {id:合同id, type:合同业务类型}；多合同取第一份 */
    goContractDetail(row) {
      const ids = String(row.agreementId).split(",");
      const types = String(row.expenditureBusinessType || "").split(",");
      if (!ids[0]) return this.$message.info("该记录无关联合同");
      const obj = { id: ids[0], type: types[0] || "" };
      const param = Base64.encode(JSON.stringify(obj));
      window.open(`/procurement/contract-detail/${encodeURIComponent(param)}`, "_blank");
    },
    /** 导出：完整结果不受分页限制（后端导出接口不传 pageNum/pageSize 即全量） */
    handleExport() {
      this.$confirm("确认导出当前条件下的采购台账明细数据吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        // 导出走同一套参数组装；去掉分页即全量
        const params = this.buildParams();
        delete params.pageNum;
        delete params.pageSize;
        this.download(
          "business/report/purchaseLedgerExport",
          { ...params },
          `${formatDate(new Date())}采购台账明细.xlsx`
        );
      });
    },
    statusTagType(status) {
      // element-ui el-tag 合法 type: success/info/warning/danger，其余为默认样式
      const map = { pending: "warning", preopen: "", preaward: "info", completed: "success", exception: "danger" };
      return map[status] || "";
    },
    formatMoney(v) {
      if (v === null || v === undefined) return "—";
      return Number(v).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    /** 多合同金额拼接串（如 "121.0000,89.0000"）：逐个格式化后用中文逗号连接 */
    formatMoneyList(v) {
      if (v === null || v === undefined || v === "") return "—";
      return String(v).split(",").map(item => this.formatMoney(item)).join("，");
    },
    formatDate(v) {
      if (!v) return "—";
      const d = new Date(v);
      const p = n => String(n).padStart(2, "0");
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
    }
  }
};
</script>

<style lang="scss" scoped>
.ledger-detail {
  .ledger-status-tabs {
    margin-bottom: 12px;
  }
  .ledger-detail-filter {
    margin-bottom: 12px;
  }
}
</style>
