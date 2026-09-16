<template>
  <div class="app-container workbench">

    <!-- 顶部欢迎栏 -->
    <div class="welcome">
      <h1>{{ greeting }}，{{ name }}</h1>
      <p>欢迎使用招标采购管理系统，祝您工作顺利！</p>
      <span class="date">{{ todayText }}</span>
    </div>

    <!-- 五张状态卡（采购台账五类状态计数，点击跳采购台账页） -->
    <div class="status-cards">
      <div v-for="card in statusCards" :key="card.key" class="scard" @click="goLedger">
        <span class="ic" :class="card.iconClass">{{ card.icon }}</span>
        <span>
          <div class="lbl">{{ card.label }}</div>
          <div class="num" :class="{ warn: card.warn }">{{ card.value }}<small>次</small></div>
        </span>
        <i>›</i>
      </div>
    </div>

    <div class="grid">
      <!-- 我的待办（数据来自 flowable 待办接口，与待办事项页一致） -->
      <section class="panel">
        <header>
          <h2>我的待办 <span class="badge">{{ todoTotal }}</span></h2>
          <a class="more" @click="$router.push('/index')">查看更多 ›</a>
        </header>
        <el-table :data="todoList" size="mini" class="todo-table">
          <el-table-column label="菜单名称" show-overflow-tooltip>
            <template slot-scope="scope">
              <span v-if="scope.row.processTitle === 'null' || scope.row.processTitle === 'undefined'">无名称</span>
              <span v-else>{{ scope.row.processTitle }}</span>
            </template>
          </el-table-column>
          <el-table-column label="项目名称" prop="projectCode" show-overflow-tooltip />
          <el-table-column label="消息内容" prop="businessContent" show-overflow-tooltip min-width="180" />
          <el-table-column label="上一审批人" prop="previousApprover" align="center" min-width="90" />
          <el-table-column label="提交时间" prop="createTime" align="center" min-width="140" />
          <el-table-column label="操作" align="center" width="60">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="goDetail(scope.row.detailUrl)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <!-- 采购方式分析（招标率统计合计：公开/邀标/询价/单一，只统计已完成采购） -->
      <section class="panel">
        <header><h2>采购方式分析</h2></header>
        <div class="donut-wrap">
          <div ref="donut" class="donut-chart"></div>
          <div class="legend">
            <div v-for="item in methodLegend" :key="item.name">
              <span class="dot" :style="{ background: item.color }"></span>
              {{ item.name }}
              <span class="pct">{{ item.pct }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="grid">
      <!-- 招采概览（采购台账已完成数据；单位树+年份筛选只作用于本面板） -->
      <section class="panel">
        <header>
          <h2>招采概览</h2>
          <div class="tools">
            <el-select v-model="overviewOrgId" size="small" class="org-select" @change="loadStats">
              <el-option label="全部单位" value="" />
              <el-option v-for="org in orgOptions" :key="org.value" :label="org.label" :value="org.value" />
            </el-select>
            <el-select v-model="overviewYear" size="small" class="year-select" @change="loadStats">
              <el-option label="全部年份" value="" />
              <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
            </el-select>
          </div>
        </header>
        <div class="metrics">
          <div class="m">
            <div class="ml"><span class="mi" style="background:#3b82f6">✕</span>采购次数</div>
            <div class="mv">{{ overview.purchaseCount }}<small>次</small></div>
          </div>
          <div class="m">
            <div class="ml"><span class="mi" style="background:#10b981">🪙</span>采购预算</div>
            <div class="mv">{{ formatAmount(overview.budgetAmount) }}<small>元</small></div>
          </div>
          <div class="m">
            <div class="ml"><span class="mi" style="background:#f59e0b">📄</span>采购金额</div>
            <div class="mv">{{ formatAmount(overview.awardAmount) }}<small>元</small></div>
          </div>
          <div class="m">
            <div class="ml"><span class="mi" style="background:#8b5cf6">📈</span>成本节约率</div>
            <div class="mv">{{ overview.savingRate == null ? '-' : overview.savingRate }}<small>%</small></div>
          </div>
        </div>
        <div ref="trend" class="trend-chart"></div>
      </section>

      <!-- 合作金额TOP5供应商（供应商报表按供应商汇总合同签订金额(含税)取前5） -->
      <section class="panel">
        <header>
          <h2>合作金额TOP5供应商</h2>
          <a class="more" @click="$router.push('/tender-procurement/reportForm/vender')">查看更多 ›</a>
        </header>
        <el-table :data="topVendors" size="mini" class="todo-table">
          <el-table-column label="排名" width="60" align="center">
            <template slot-scope="scope">
              <span class="rank" :class="'r' + (scope.$index + 1)">{{ scope.$index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="供应商名称" prop="vendorName" show-overflow-tooltip />
          <el-table-column label="合作金额（万元）" align="right">
            <template slot-scope="scope">{{ toWan(scope.row.contractAmount) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import * as echarts from "echarts";
import { getWorkbenchStats } from "@/api/workbench";
import { geTaskTodoList } from "@/api/index";
import { getDeptTree } from "@/api/system/dept";

export default {
  name: "Workbench",
  data() {
    return {
      loading: false,
      // 五张状态卡
      statusCounts: { pending: 0, preopen: 0, preaward: 0, completed: 0, exception: 0 },
      // 采购方式分析
      methodCounts: { gkNum: 0, yqNum: 0, xjNum: 0, dyNum: 0, cgNum: 0 },
      // 招采概览
      overview: { purchaseCount: 0, budgetAmount: 0, awardAmount: 0, savingRate: null },
      // 月度金额趋势（12 个月）
      monthlyTrend: [],
      // 合作金额TOP5供应商
      topVendors: [],
      // 我的待办
      todoList: [],
      todoTotal: 0,
      // 概览筛选（仅作用于招采概览）
      overviewOrgId: "",
      overviewYear: new Date().getFullYear(),
      orgOptions: [],
      donutChart: null,
      trendChart: null
    };
  },
  computed: {
    ...mapGetters(["name"]),
    greeting() {
      const hour = new Date().getHours();
      if (hour < 12) return "上午好";
      if (hour < 18) return "下午好";
      return "晚上好";
    },
    todayText() {
      const now = new Date();
      const weeks = ["日", "一", "二", "三", "四", "五", "六"];
      return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日  星期${weeks[now.getDay()]}`;
    },
    // 年份下拉：近5年
    yearOptions() {
      const current = new Date().getFullYear();
      const list = [];
      for (let y = current; y > current - 5; y--) list.push(y);
      return list;
    },
    statusCards() {
      return [
        { key: "pending", label: "待招采", value: this.statusCounts.pending, icon: "📄", iconClass: "ic-blue" },
        { key: "preopen", label: "待开标", value: this.statusCounts.preopen, icon: "🕐", iconClass: "ic-cyan" },
        { key: "preaward", label: "待定标", value: this.statusCounts.preaward, icon: "📋", iconClass: "ic-indigo" },
        { key: "completed", label: "已完成", value: this.statusCounts.completed, icon: "✅", iconClass: "ic-green" },
        { key: "exception", label: "异常/终止", value: this.statusCounts.exception, icon: "⚠️", iconClass: "ic-red", warn: true }
      ];
    },
    // 采购方式图例（含占比），与甜甜圈同色
    methodLegend() {
      const total = Number(this.methodCounts.cgNum) || 0;
      const items = [
        { name: "公开招标", value: this.methodCounts.gkNum, color: "#2563eb" },
        { name: "邀请招标", value: this.methodCounts.yqNum, color: "#10b981" },
        { name: "询价采购", value: this.methodCounts.xjNum, color: "#f59e0b" },
        { name: "单一来源", value: this.methodCounts.dyNum, color: "#93c5fd" }
      ];
      return items.map(item => ({
        ...item,
        pct: total > 0 ? ((Number(item.value) || 0) / total * 100).toFixed(1) + "%" : "0%"
      }));
    }
  },
  mounted() {
    this.loadStats();
    this.loadTodo();
    this.loadDeptTree();
    window.addEventListener("resize", this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
    if (this.donutChart) this.donutChart.dispose();
    if (this.trendChart) this.trendChart.dispose();
  },
  methods: {
    /** 工作台统计数据（单位树+年份仅作为概览筛选条件传给后端） */
    loadStats() {
      this.loading = true;
      getWorkbenchStats({ year: this.overviewYear || "", orgId: this.overviewOrgId || "" }).then(res => {
        const data = res.data || {};
        this.statusCounts = data.statusCards || this.statusCounts;
        this.methodCounts = data.methodAnalysis || this.methodCounts;
        this.overview = data.overview || this.overview;
        this.monthlyTrend = data.monthlyTrend || [];
        this.topVendors = data.topVendors || [];
        this.$nextTick(() => {
          this.renderDonut();
          this.renderTrend();
        });
      }).finally(() => {
        this.loading = false;
      });
    },
    /** 我的待办：取前5条，角标=未处理总数 */
    loadTodo() {
      geTaskTodoList({ pageNum: 1, pageSize: 5 }).then(res => {
        if (res.data) {
          this.todoList = res.data || [];
          this.todoTotal = res.total || 0;
        }
      });
    },
    /** 概览单位筛选：与采购台账左树同一个接口，返回已是树（label/children/thridDeptId） */
    loadDeptTree() {
      getDeptTree().then(res => {
        const tree = res.data || [];
        const options = [];
        const walk = (nodes, depth) => {
          (nodes || []).forEach(node => {
            options.push({
              value: node.thridDeptId || String(node.id),
              label: "　".repeat(depth) + node.label
            });
            if (node.children && node.children.length) walk(node.children, depth + 1);
          });
        };
        walk(tree, 0);
        this.orgOptions = options;
      });
    },
    /** 采购方式分析-甜甜圈 */
    renderDonut() {
      if (!this.donutChart) {
        this.donutChart = echarts.init(this.$refs.donut);
      }
      const m = this.methodCounts;
      this.donutChart.setOption({
        tooltip: { trigger: "item", formatter: "{b}: {c}次 ({d}%)" },
        series: [{
          type: "pie",
          radius: ["58%", "80%"],
          center: ["50%", "50%"],
          label: {
            show: true,
            position: "center",
            formatter: `${Number(m.cgNum) || 0}\n采购次数`,
            fontSize: 22,
            fontWeight: 700,
            color: "#0f2b66",
            lineHeight: 30
          },
          data: [
            { value: Number(m.gkNum) || 0, name: "公开招标", itemStyle: { color: "#2563eb" } },
            { value: Number(m.yqNum) || 0, name: "邀请招标", itemStyle: { color: "#10b981" } },
            { value: Number(m.xjNum) || 0, name: "询价采购", itemStyle: { color: "#f59e0b" } },
            { value: Number(m.dyNum) || 0, name: "单一来源", itemStyle: { color: "#93c5fd" } }
          ]
        }]
      });
    },
    /** 月度金额趋势（金额换算为万元展示） */
    renderTrend() {
      if (!this.trendChart) {
        this.trendChart = echarts.init(this.$refs.trend);
      }
      const months = this.monthlyTrend.map(row => row.monthNo + "月");
      const budget = this.monthlyTrend.map(row => this.toWan(row.budgetAmount));
      const award = this.monthlyTrend.map(row => this.toWan(row.awardAmount));
      this.trendChart.setOption({
        tooltip: { trigger: "axis" },
        legend: { data: ["采购预算", "采购金额"], right: 10, top: 0, textStyle: { fontSize: 12, color: "#47586e" } },
        grid: { left: 50, right: 14, top: 34, bottom: 24 },
        xAxis: { type: "category", boundaryGap: false, data: months, axisLine: { lineStyle: { color: "#cbd5e1" } }, axisLabel: { color: "#64748b" } },
        yAxis: { type: "value", splitLine: { lineStyle: { color: "#eef2f8" } }, axisLabel: { color: "#64748b" } },
        series: [
          { name: "采购预算", type: "line", smooth: true, symbol: "circle", symbolSize: 6, data: budget, lineStyle: { width: 2.2, color: "#2563eb" }, itemStyle: { color: "#2563eb" }, areaStyle: { color: "rgba(37,99,235,.06)" } },
          { name: "采购金额", type: "line", smooth: true, symbol: "circle", symbolSize: 6, data: award, lineStyle: { width: 2.2, color: "#10b981" }, itemStyle: { color: "#10b981" }, areaStyle: { color: "rgba(16,185,129,.06)" } }
        ]
      });
    },
    handleResize() {
      if (this.donutChart) this.donutChart.resize();
      if (this.trendChart) this.trendChart.resize();
    },
    /** 金额格式化（元，千分位，最多2位小数） */
    formatAmount(value) {
      const num = Number(value) || 0;
      return num.toLocaleString("zh-CN", { maximumFractionDigits: 2 });
    },
    /** 元 → 万元（保留2位） */
    toWan(value) {
      return ((Number(value) || 0) / 10000).toFixed(2);
    },
    /** 状态卡 → 采购台账页 */
    goLedger() {
      this.$router.push("/tender-procurement/reportForm/purchaseLedger");
    },
    /** 待办处理 → 各业务详情页（与待办事项页一致） */
    goDetail(url) {
      if (url != null) {
        this.$router.push(url);
      }
    }
  }
};
</script>

<style scoped>
.workbench { min-width: 1200px; }
.welcome { display: flex; align-items: center; margin-bottom: 12px; }
.welcome h1 { color: #0f2b66; font-size: 20px; margin: 0 14px 0 0; }
.welcome p { color: #5b6f8f; font-size: 13px; margin: 0; }
.welcome .date { margin-left: auto; color: #5b6f8f; font-size: 13px; }

/* 五张状态卡 */
.status-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 14px; }
.scard { position: relative; display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid #e5eaf3; border-radius: 10px; padding: 18px 20px; cursor: pointer; box-shadow: 0 1px 3px rgba(15, 43, 102, .04); }
.scard .ic { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; font-size: 20px; color: #fff; flex: none; }
.ic-blue { background: #3b82f6; }
.ic-cyan { background: #22d3ee; }
.ic-indigo { background: #818cf8; }
.ic-green { background: #10b981; }
.ic-red { background: #fca5a5; }
.scard .lbl { font-size: 14px; color: #64748b; }
.scard .num { font-size: 30px; font-weight: 700; color: #0f2b66; line-height: 1.1; }
.scard .num small { font-size: 13px; font-weight: 400; color: #94a3b8; margin-left: 2px; }
.scard .num.warn { color: #f97316; }
.scard > i { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); color: #c3cddf; font-style: normal; font-size: 16px; }

/* 面板布局：左宽右窄 */
.grid { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(380px, 1fr); gap: 14px; }
.grid + .grid { margin-top: 14px; }
.panel { background: #fff; border: 1px solid #e5eaf3; border-radius: 10px; box-shadow: 0 1px 3px rgba(15, 43, 102, .04); padding: 0 0 10px; }
.panel header { display: flex; align-items: center; padding: 16px 18px 10px; }
.panel h2 { font-size: 16px; color: #1e3a6e; font-weight: 600; margin: 0; display: flex; align-items: center; gap: 8px; }
.badge { display: inline-grid; place-items: center; min-width: 34px; height: 20px; padding: 0 7px; border-radius: 10px; background: #e8f0fe; color: #2563eb; font-size: 12px; font-weight: 600; }
.panel .more { margin-left: auto; color: #2563eb; font-size: 13px; cursor: pointer; }
.todo-table { width: calc(100% - 36px); margin: 0 18px 6px; }

/* 采购方式分析 */
.donut-wrap { display: flex; align-items: center; padding: 4px 18px 8px; }
.donut-chart { width: 55%; height: 240px; }
.legend { flex: 1; }
.legend div { display: flex; align-items: center; gap: 8px; padding: 7px 0; font-size: 13px; color: #334155; }
.legend .dot { width: 10px; height: 10px; border-radius: 50%; flex: none; }
.legend .pct { margin-left: auto; font-weight: 600; color: #0f2b66; }

/* 招采概览 */
.tools { margin-left: auto; display: flex; align-items: center; gap: 10px; }
.org-select { width: 170px; }
.year-select { width: 110px; }
.metrics { display: grid; grid-template-columns: repeat(4, 1fr); padding: 4px 18px 0; }
.metrics .m { padding: 10px 14px; border-left: 1px solid #eef2f8; }
.metrics .m:first-child { border-left: 0; }
.metrics .ml { display: flex; align-items: center; gap: 8px; color: #64748b; font-size: 13px; }
.metrics .mi { width: 30px; height: 30px; border-radius: 8px; display: grid; place-items: center; font-size: 15px; color: #fff; flex: none; }
.metrics .mv { font-size: 22px; font-weight: 700; color: #0f2b66; margin-top: 6px; }
.metrics .mv small { font-size: 13px; font-weight: 400; color: #94a3b8; margin-left: 2px; }
.trend-chart { width: calc(100% - 36px); height: 230px; margin: 0 18px; }

/* TOP5 排名角标 */
.rank { display: inline-grid; place-items: center; width: 20px; height: 20px; border-radius: 5px; font-size: 12px; font-weight: 600; color: #64748b; background: #eef2f8; }
.rank.r1 { background: #f59e0b; color: #fff; }
.rank.r2 { background: #94a3b8; color: #fff; }
.rank.r3 { background: #fb923c; color: #fff; }
</style>
