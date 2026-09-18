<template>
  <div class="app-container workbench">

    <!-- 顶部欢迎语（样式参考原型 welcome-card：整卡 banner 背景 + 左侧白纱保证文字可读 + 深蓝字） -->
    <section class="welcome-card">
      <div>
        <h1>{{ greeting }}，{{ nickname }}</h1>
        <p>今日有 {{ todoTotal }} 项待处理事项</p>
      </div>
      <time>{{ todayText }}</time>
    </section>

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

    <div class="grid grid-2">
      <!-- 我的待办（数据来自 flowable 待办接口，与待办事项页一致） -->
      <section class="panel">
        <header>
          <h2>我的待办 <span class="badge">{{ todoTotal }}</span></h2>
          <a class="more" @click="$router.push('/index')">查看更多 ›</a>
        </header>
        <el-table :data="todoList" class="todo-table">
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
              <el-button type="text" @click="goDetail(scope.row.detailUrl)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <!-- 采购方式分析（已完成采购按方式计数：公开/邀标/询价/单一；单位+年份筛选用，点击跳招标率统计） -->
      <section class="panel">
        <header>
          <h2>采购方式分析</h2>
          <div class="tools">
            <el-select v-model="methodOrgId" size="small" class="org-select" @change="loadStats">
              <el-option label="全部单位" value="" />
              <el-option v-for="org in orgOptions" :key="org.value" :label="org.label" :value="org.value" />
            </el-select>
            <el-select v-model="methodYear" size="small" class="year-select" @change="loadStats">
              <el-option label="全部年份" value="" />
              <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
            </el-select>
          </div>
        </header>
        <div class="donut-wrap">
          <div ref="donut" class="donut-chart"></div>
          <div class="legend">
            <div v-for="item in methodLegend" :key="item.name" class="legend-item" @click="goBuildingRate">
              <span class="dot" :style="{ background: item.color }"></span>
              {{ item.name }}
              <span class="pct">{{ item.pct }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="grid grid-3">
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
        <div class="metrics" @click="goLedgerDetail">
          <div class="m">
            <div class="ml"><span class="mi" style="background:#3b82f6">✕</span>采购次数</div>
            <div class="mv">{{ overview.purchaseCount }}<small>次</small></div>
          </div>
          <div class="m">
            <div class="ml"><span class="mi" style="background:#10b981">🪙</span>采购预算</div>
            <div class="mv">{{ formatWan(overview.budgetAmount) }}<small>万元</small></div>
          </div>
          <div class="m">
            <div class="ml"><span class="mi" style="background:#f59e0b">📄</span>采购金额</div>
            <div class="mv">{{ formatWan(overview.awardAmount) }}<small>万元</small></div>
          </div>
          <div class="m">
            <div class="ml"><span class="mi" style="background:#8b5cf6">📈</span>成本节约率</div>
            <div class="mv">{{ overview.savingRate == null ? '-' : overview.savingRate }}<small>%</small></div>
          </div>
        </div>
        <div ref="trend" class="trend-chart"></div>
      </section>

      <!-- 合作金额TOP5供应商（供应商报表按供应商汇总合同签订金额(含税)取前5；点击行跳供应商报表） -->
      <section class="panel">
        <header>
          <h2>合作金额TOP5供应商</h2>
          <a class="more" @click="goVendorReport">查看更多 ›</a>
        </header>
        <el-table ref="vendorTable" :data="topVendors" class="vendor-table" :height="vendorTableHeight || undefined" :row-style="vendorRowStyle" @row-click="goVendorReport">
          <!-- 三列固定像素宽度（el-table 的百分比列宽按 px 计算、支持不好，用固定值）：
               排名加宽、名称列固定不再吃满剩余空间（名称往中间移）、金额固定宽度左对齐，
               名称与金额之间不会出现大片空白；长名称省略号+悬浮看全 -->
          <el-table-column label="排名" width="90" align="center">
            <template slot-scope="scope">
              <span class="rank" :class="'r' + (scope.$index + 1)">{{ scope.$index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="供应商名称" prop="vendorName" show-overflow-tooltip width="220" />
          <el-table-column label="合作金额（元）" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.contractAmount) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script>
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
      // 概览筛选（仅作用于招采概览；年份默认"全部年份"，与状态卡口径一致）
      overviewOrgId: "",
      overviewYear: "",
      // 采购方式分析筛选（仅作用于该面板，默认全部）
      methodOrgId: "",
      methodYear: "",
      orgOptions: [],
      // TOP5 表格：高度与行高自适应（保证 5 行全部显示且正好填满面板）
      vendorTableHeight: undefined,
      vendorRowH: 44,
      donutChart: null,
      trendChart: null
    };
  },
  computed: {
    // 登录用户昵称（欢迎语展示用，登录时已存入 store；取不到时回落登录名）
    nickname() {
      return this.$store.state.user.nickname || this.$store.state.user.name;
    },
    greeting() {
      const hour = new Date().getHours();
      if (hour < 12) return "上午好";
      if (hour < 18) return "下午好";
      return "晚上好";
    },
    todayText() {
      const now = new Date();
      const weeks = ["日", "一", "二", "三", "四", "五", "六"];
      // \u00a0 为不换行空格（原型 HTML 里的 &nbsp; 实体），用 fromCharCode 生成，避免源码出现不可见字符
      const nbsp = String.fromCharCode(160);
      return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日${nbsp}${nbsp}星期${weeks[now.getDay()]}`;
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
    this.computeVendorTable();
    window.addEventListener("resize", this.handleResize);
    // 侧边栏收起/展开只改容器宽度、不触发 window resize，用 ResizeObserver 跟随重绘图表
    if (window.ResizeObserver) {
      this.resizeObserver = new ResizeObserver(this.handleResize);
      this.resizeObserver.observe(this.$el);
    }
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
    if (this.resizeObserver) this.resizeObserver.disconnect();
    if (this.donutChart) this.donutChart.dispose();
    if (this.trendChart) this.trendChart.dispose();
  },
  methods: {
    /** 图表跟随容器尺寸重绘（窗口缩放、侧边栏收起/展开都会触发） */
    handleResize() {
      if (this.donutChart) this.donutChart.resize();
      if (this.trendChart) this.trendChart.resize();
      this.computeVendorTable();
    },
    /** TOP5 行内联样式：行高按剩余高度均分，5 行正好填满面板 */
    vendorRowStyle() {
      return { height: this.vendorRowH + "px" };
    },
    /** 计算 TOP5 表格高度与行高：面板高度扣除标题区后均分给数据行 */
    computeVendorTable() {
      this.$nextTick(() => {
        const el = this.$refs.vendorTable && this.$refs.vendorTable.$el;
        if (!el) return;
        const panel = el.closest(".panel");
        // 面板高度扣除：标题区(h2 高约41px) + 面板底部内边距8 + 表格底部外边距4
        const tableRegion = panel
          ? panel.clientHeight - 41 - 8 - 4
          : el.clientHeight;
        const header = el.querySelector(".el-table__header-wrapper");
        const headerH = header ? header.offsetHeight : 40;
        const rows = (this.topVendors || []).length;
        const bodyH = tableRegion - headerH - 2;
        this.vendorTableHeight = tableRegion;
        this.vendorRowH = rows > 0 ? Math.max(40, Math.floor(bodyH / rows)) : 44;
      });
    },
    /** 工作台统计数据（单位树+年份分别作用于招采概览与采购方式分析） */
    loadStats() {
      this.loading = true;
      getWorkbenchStats({
        year: this.overviewYear || "",
        orgId: this.overviewOrgId || "",
        methodYear: this.methodYear || "",
        methodOrgId: this.methodOrgId || ""
      }).then(res => {
        const data = res.data || {};
        this.statusCounts = data.statusCards || this.statusCounts;
        this.methodCounts = data.methodAnalysis || this.methodCounts;
        this.overview = data.overview || this.overview;
        this.monthlyTrend = data.monthlyTrend || [];
        this.topVendors = data.topVendors || [];
        this.$nextTick(() => {
          this.renderDonut();
          this.renderTrend();
          this.computeVendorTable();
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
            fontSize: 24,
            fontWeight: 700,
            color: "#0f2b66",
            lineHeight: 32
          },
          data: [
            { value: Number(m.gkNum) || 0, name: "公开招标", itemStyle: { color: "#2563eb" } },
            { value: Number(m.yqNum) || 0, name: "邀请招标", itemStyle: { color: "#10b981" } },
            { value: Number(m.xjNum) || 0, name: "询价采购", itemStyle: { color: "#f59e0b" } },
            { value: Number(m.dyNum) || 0, name: "单一来源", itemStyle: { color: "#93c5fd" } }
          ]
        }]
      });
      // 点击圆环任意扇区 → 跳转招标率统计页
      this.donutChart.off("click");
      this.donutChart.on("click", () => this.goBuildingRate());
      this.donutChart.resize();
    },
    /** 月度金额趋势（金额统一为万元展示，与上方指标卡片口径一致） */
    renderTrend() {
      if (!this.trendChart) {
        this.trendChart = echarts.init(this.$refs.trend);
      }
      const wan = v => (Number(v) || 0) / 10000;
      const months = this.monthlyTrend.map(row => row.monthNo + "月");
      const budget = this.monthlyTrend.map(row => wan(row.budgetAmount));
      const award = this.monthlyTrend.map(row => wan(row.awardAmount));
      this.trendChart.setOption({
        title: { text: "月度金额趋势（万元）", textStyle: { fontSize: 14, color: "#334155" }, left: 6, top: 4 },
        tooltip: {
          trigger: "axis",
          formatter(params) {
            let res = params[0].axisValue + "<br/>";
            params.forEach(p => {
              res += p.marker + p.seriesName + "：" + p.value.toLocaleString("zh-CN", { maximumFractionDigits: 2 }) + "万元<br/>";
            });
            return res;
          }
        },
        legend: { data: ["采购预算", "采购金额"], right: 10, top: 0, textStyle: { fontSize: 13, color: "#47586e" } },
        grid: { left: 50, right: 14, top: 44, bottom: 24 },
        xAxis: { type: "category", boundaryGap: false, data: months, axisLine: { lineStyle: { color: "#cbd5e1" } }, axisLabel: { color: "#64748b" } },
        yAxis: { type: "value", splitLine: { lineStyle: { color: "#eef2f8" } }, axisLabel: { color: "#64748b" } },
        series: [
          { name: "采购预算", type: "line", smooth: true, symbol: "circle", symbolSize: 6, data: budget, lineStyle: { width: 2.2, color: "#2563eb" }, itemStyle: { color: "#2563eb" }, areaStyle: { color: "rgba(37,99,235,.06)" } },
          { name: "采购金额", type: "line", smooth: true, symbol: "circle", symbolSize: 6, data: award, lineStyle: { width: 2.2, color: "#10b981" }, itemStyle: { color: "#10b981" }, areaStyle: { color: "rgba(16,185,129,.06)" } }
        ]
      });
      // 点击趋势图 → 跳转采购台账明细"已完成"页签
      this.trendChart.off("click");
      this.trendChart.on("click", () => this.goLedgerDetail());
      this.trendChart.resize();
    },
    /** 容器尺寸变化（窗口缩放/侧边栏收展）时重绘图表 */
    handleResize() {
      if (this.donutChart) this.donutChart.resize();
      if (this.trendChart) this.trendChart.resize();
    },
    /** 金额格式化（元，千分位，保留2位小数） */
    formatAmount(value) {
      return (Number(value) || 0).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    /** 金额格式化（元 → 万元，除以10000后千分位保留2位） */
    formatWan(value) {
      return this.formatAmount((Number(value) || 0) / 10000);
    },
    /** 状态卡 → 采购台账页（报表已移到 2067 分析报表目录下，路由为 /analytical/xxx） */
    goLedger() {
      this.$router.push("/analytical/purchaseLedger");
    },
    /** 采购方式分析 → 招标率统计页 */
    goBuildingRate() {
      this.$router.push("/analytical/buildingRate");
    },
    /** TOP5供应商（行/查看更多）→ 供应商报表页 */
    goVendorReport() {
      this.$router.push("/analytical/vender");
    },
    /** 招采概览 → 采购台账明细"已完成"页签（不带筛选条件） */
    goLedgerDetail() {
      this.$router.push({
        path: "/analytical/purchaseLedgerDetail",
        query: { status: "completed" }
      });
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
/* ===== 一屏布局：工作台撑满内容区（AppMain 高度=视口-页头），内部弹性伸缩 =====
   极端小屏/高缩放下内容实在放不下时，工作台内部滚动兜底，绝不截断表格行 */
.workbench { height: 100%; display: flex; flex-direction: column; gap: 10px; padding: 10px 16px; overflow-x: hidden; overflow-y: auto; }

/* 顶部欢迎语：整卡铺开 banner 背景图（center/cover），
   背景分两层：上面是白纱渐变（左实右透），保证左侧文字可读、右侧渐显原图 */
.welcome-card { position: relative; flex: none; display: flex; align-items: center; height: 80px; padding: 0 18px; border: 1px solid #e5eaf3; border-radius: 10px; background: linear-gradient(90deg, #fff 0%, rgba(255, 255, 255, .92) 40%, rgba(255, 255, 255, .3) 62%, rgba(255, 255, 255, 0) 80%), url("../../assets/images/workbench-welcome.jpg") center / cover no-repeat; box-shadow: 0 1px 3px rgba(15, 43, 102, .04); overflow: hidden; }
.welcome-card > div, .welcome-card time { position: relative; z-index: 1; }
.welcome-card h1 { margin: 0 0 3px; color: #092d82; font-size: 28px; line-height: 38px; font-weight: 700; }
.welcome-card p { margin: 0; color: #164291; font-weight: 500; font-size: 15px; }
.welcome-card time { margin: 0 auto; color: #123b91; font-weight: 600; flex: none; font-size: 15px; }

/* 五张状态卡 */
.status-cards { flex: none; display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.scard { position: relative; display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid #e5eaf3; border-radius: 10px; padding: 13px 20px; cursor: pointer; box-shadow: 0 1px 3px rgba(15, 43, 102, .04); }
.scard .ic { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; font-size: 20px; color: #fff; flex: none; }
.ic-blue { background: #3b82f6; }
.ic-cyan { background: #22d3ee; }
.ic-indigo { background: #818cf8; }
.ic-green { background: #10b981; }
.ic-red { background: #fca5a5; }
.scard .lbl { font-size: 15px; color: #64748b; }
.scard .num { font-size: 30px; font-weight: 700; color: #0f2b66; line-height: 1.15; }
.scard .num small { font-size: 14px; font-weight: 400; color: #94a3b8; margin-left: 2px; }
.scard .num.warn { color: #f97316; }
.scard > i { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); color: #c3cddf; font-style: normal; font-size: 16px; }

/* 面板布局：左宽右窄。左侧（我的待办/招采概览）占 2 份，
   右侧（采购方式分析/TOP5）占 1 份，左大右小更合理。
   两行面板弹性铺满剩余高度（不浪费底部空白），min-height 保证内容不被压扁；
   行高由容器高度分配、与内容无关，切换筛选时布局不跳动；
   空间不足时整页滚动而不截断内容 */
.grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(0, 1fr); gap: 10px; }
.grid-2 { flex: 1 1 0; min-height: 260px; }
.grid-3 { flex: 1.15 1 0; min-height: 294px; }
.panel { display: flex; flex-direction: column; min-height: 0; overflow: hidden; background: #fff; border: 1px solid #e5eaf3; border-radius: 10px; box-shadow: 0 1px 3px rgba(15, 43, 102, .04); padding-bottom: 8px; }
.panel header { flex: none; display: flex; align-items: center; padding: 10px 18px 6px; }
.panel h2 { font-size: 18px; color: #1e3a6e; font-weight: 600; margin: 0; display: flex; align-items: center; gap: 8px; }
.badge { display: inline-grid; place-items: center; min-width: 36px; height: 22px; padding: 0 7px; border-radius: 11px; background: #e8f0fe; color: #2563eb; font-size: 13px; font-weight: 600; }
.panel .more { margin-left: auto; color: #2563eb; font-size: 14px; cursor: pointer; }
/* 表格占满面板剩余高度；行高压缩保证小屏也能一屏放下 */
.todo-table { flex: 1; min-height: 0; width: calc(100% - 36px); margin: 0 18px 4px; }
.todo-table::v-deep .el-table__cell { padding: 4px 0; }

/* 采购方式分析 */
.donut-wrap { flex: 1; min-height: 0; display: flex; align-items: center; padding: 0 18px 4px; }
.donut-chart { width: 55%; height: 100%; min-height: 150px; }
.legend { flex: 1; }
.legend div { display: flex; align-items: center; gap: 10px; padding: 7px 0; font-size: 16px; color: #334155; }
.legend-item { cursor: pointer; }
.legend .dot { width: 12px; height: 12px; border-radius: 50%; flex: none; }
/* 占比紧跟名称（去掉原来的 margin-left:auto 推到最右），名称与占比一起放大 */
.legend .pct { margin-left: 8px; font-weight: 600; color: #0f2b66; }
.donut-chart { cursor: pointer; }

/* 招采概览 */
.tools { margin-left: auto; display: flex; align-items: center; gap: 10px; }
/* 下拉框保持 small 高度（一屏布局），仅把文字调大到与其他页面一致 */
.tools ::v-deep .el-input__inner { font-size: 14px; }
.org-select { width: 170px; }
.year-select { width: 110px; }
.metrics { flex: none; display: grid; grid-template-columns: repeat(4, 1fr); padding: 2px 18px 0; cursor: pointer; }
.metrics .m { padding: 8px 14px; border-left: 1px solid #eef2f8; }
.metrics .m:first-child { border-left: 0; }
.metrics .ml { display: flex; align-items: center; gap: 8px; color: #64748b; font-size: 14px; }
.metrics .mi { width: 28px; height: 28px; border-radius: 8px; display: grid; place-items: center; font-size: 14px; color: #fff; flex: none; }
.metrics .mv { font-size: 23px; font-weight: 700; color: #0f2b66; margin-top: 4px; }
.metrics .mv small { font-size: 14px; font-weight: 400; color: #94a3b8; margin-left: 2px; }
.trend-chart { flex: 1; min-height: 0; width: calc(100% - 36px); margin: 0 18px; cursor: pointer; }

/* TOP5：整行可点击，鼠标手型（cursor 会被单元格继承）。
   不共用 todo-table 类，避免其 4px 单元格内边距与这里的行高规则优先级冲突。
   行高由 JS 按面板高度自适应计算（vendorRowStyle），5 行正好填满面板、不会溢出 */
.vendor-table { flex: 1; min-height: 0; width: calc(100% - 36px); margin: 0 18px 4px; cursor: pointer; }
.vendor-table::v-deep .el-table__body td.el-table__cell { padding: 0; }

/* TOP5 排名角标 */
.rank { display: inline-grid; place-items: center; width: 22px; height: 22px; border-radius: 5px; font-size: 13px; font-weight: 600; color: #64748b; background: #eef2f8; }
.rank.r1 { background: #f59e0b; color: #fff; }
.rank.r2 { background: #94a3b8; color: #fff; }
.rank.r3 { background: #fb923c; color: #fff; }
</style>
