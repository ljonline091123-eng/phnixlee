<template>
  <div class="app-container">
    <div class="context flex flex-column" style="height: 100%">
      <el-radio-group
        v-model="radioType"
        size="small"
        style="margin-left: 15px; margin-top: 15px"
      >
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in radioList"
          :key="dict.value"
          >{{ dict.label }}</el-radio-button
        >
      </el-radio-group>

      <!-- 异常报表 -->
      <ShowTablePro
        v-if="radioType == '1'"
        ref="showTable"
        reportType="problem"
        :table-header-list="tableHeaderList"
        :table-data="tableData"
        :queryItemList="queryItemList"
        :radioType="radioType"
        :loading="loading"
        :exportFlag="true"
        :queryFlag="true"
        @query="handleQuery"
        @export="handleExport"
      >
      </ShowTablePro>

      <!-- 供应商评价不合格情况 -->
      <ShowTablePro
        v-if="radioType == '2'"
        ref="showTable"
        :total="total"
        reportType="problem"
        :table-header-list="tableHeaderList1"
        :table-data="tableData"
        :queryItemList="queryItemList1"
        :radioType="radioType"
        :loading="loading"
        :exportFlag="true"
        :queryFlag="false"
        @query="handleQuery"
        @export="handleExport"
      >
      </ShowTablePro>
    </div>
  </div>
</template>

<script>
import ShowTablePro from "@/views/reportForm/components/ShowTablePro.vue";
import { mixinReport } from "@/views/reportForm/mixins/mixinReport";
import { getEvaluationBadReport, getProblemReport } from "@/api/reportForm/vendorReport";
import { formatDate } from "@/utils";

export default {
  components: { ShowTablePro },
  mixins: [mixinReport],
  data() {
    return {
      total: 0,
      radioType: "1",
      radioList: [
        { value: "1", label: "异常报表" },
        { value: "2", label: "供应商评价不合格的情况" },
      ],
      queryItemList: [
        {
          prop: "deptId",
          label: "组织机构",
          type: "treeSelect",
          labelKey: "label",
          valueKey: "thridDeptId",
        },
        {
          prop: "area",
          label: "区域",
          type: "input",
        },
        {
          prop: "type",
          label: "出现情况",
          type: "selectArr",
          arr: [
            { value: "1", label: "供应商不足三家开标情况" },
            { value: "2", label: "第一名未中标情况" },
            { value: "3", label: "供应商投标IP一致情况" },
            { value: "4", label: "评标不合规" },
          ],
        },
        {
          prop: "dateRange",
          label: "时间范围",
          type: "dateRange",
        },
      ],
      // tab2 无筛选条件(供应商评价表没有单位/项目字段，无法按组织机构筛选)
      queryItemList1: [],
      tableHeaderList: [
        {
          prop: "deptName",
          label: "组织机构",
          width: 250,
          showOverflowTooltip: true,
          headerSlot:
            "即所属组织，采购层级为公司的就显示公司名称、分公司的为分公司名称、项目的为项目所属上级单位公司名字",
        },
        {
          prop: "minAccountFullName",
          label: "项目",
          width: 240,
          showOverflowTooltip: true,
          headerSlot: "获取项目立项的项目简称字段",
        },
        {
          prop: "num",
          label: "笔数",
          width: 80,
          showOverflowTooltip: true,
          headerSlot:
            "分别统计不够三家、评标不合规、供应商投标IP一致情况、第一名未中标情况的具体笔数",
        },
        {
          prop: "procurementSchemeName",
          label: "采购名称",
          width: 200,
          showOverflowTooltip: true,
          headerSlot: "采购方案名称，同一项目同类型的多条异常对应的多个采购名称之间用逗号间隔",
        },
        {
          prop: "procurementOfficerName",
          label: "经办人",
          width: 120,
          showOverflowTooltip: true,
          headerSlot: "采购经办人名字，如有多个标包则需要拼接",
        },
        {
          prop: "vendorInfo",
          label: "投标人",
          width: 200,
          showOverflowTooltip: true,
          tooltipChunk: 3,
          headerSlot: "投标供应商名字+联系人名字，列表只显示一行（超出省略），鼠标悬浮查看全部（每行3个）",
        },
        {
          prop: "createTime",
          label: "招标时间",
          width: 170,
          showOverflowTooltip: true,
          headerSlot: "公开招标的获取招标公告发布时间；其他的类型获取招标文件发布的时间",
        },
        {
          prop: "typeName",
          label: "出现情况",
          showOverflowTooltip: true,
          headerSlot:
            "1、供应商不足三家开标情况；2、第一名未中标情况；3、供应商投标IP一致情况；4、评标不合规",
        },
      ],
      // tab2 表头：去掉"评价项目""评价合同"，新增"评价周期"
      tableHeaderList1: [
        {
          prop: "vendorName",
          label: "供应商名称",
          width: 250,
          showOverflowTooltip: true,
          headerSlot: "获取供应商名字（全称）",
        },
        {
          prop: "typeName",
          label: "评价类型",
          width: 250,
          showOverflowTooltip: true,
          headerSlot: "获取评价时选择的评价类型",
        },
        {
          prop: "evaluateFraction",
          label: "评价分数",
          width: 150,
          showOverflowTooltip: true,
          headerSlot: "获取供应商评价的分数",
        },
        {
          prop: "evaluateTimeTxtName",
          label: "评价周期",
          width: 250,
          showOverflowTooltip: true,
          headerSlot: "取供应商评价里的评价周期",
        },
        {
          prop: "createBy",
          label: "评价人员",
          width: 250,
          showOverflowTooltip: true,
          headerSlot: "获取评价人",
        },
        {
          prop: "createTime",
          label: "评价时间",
          width: 250,
          showOverflowTooltip: true,
          headerSlot: "获取评价时间",
        },
      ],
      tableData: [],
    };
  },
  mounted() {
    // 默认查登录用户数据权限范围内所有单位的数据(后端按登录用户所属单位收敛)
    this.getList(this.queryParams);
  },
  watch: {
    // 切换页签重新查询，并把页码恢复到第一页
    radioType() {
      this.queryParams.pageNum = 1;
      this.getList(this.queryParams);
    },
  },
  methods: {
    getList(params) {
      this.loading = true;
      if (this.radioType == "1") {
        getProblemReport(params)
          .then((res) => {
            this.tableData = res.data || [];
            // 第二层默认还有下一层，点击时懒加载
            if (this.tableData.length > 0 && this.tableData[0].children) {
              this.tableData[0].children.forEach((item) => {
                item.hasChildren = true;
              });
            }
            this.loading = false;
          })
          .catch(() => {
            this.loading = false;
          });
      } else {
        getEvaluationBadReport(params)
          .then((res) => {
            this.tableData = res.data.list;
            this.total = res.data.total;
            this.loading = false;
          })
          .catch(() => {
            this.loading = false;
          });
      }
    },
    getExport(params) {
      const url =
        this.radioType == "1"
          ? "business/report/problemReportExport"
          : "business/report/evaluationBadReportExport";
      const fileName =
        this.radioType == "1"
          ? "问题报表-异常报表"
          : "问题报表-供应商评价不合格情况报表";
      this.download(
        url,
        {
          ...params,
        },
        `${formatDate(new Date())}${fileName}.xlsx`
      );
    },
  },
};
</script>

<style lang="scss" scoped>
::v-deep .vue-treeselect {
  .vue-treeselect__control {
    height: 32px;
  }
}
</style>
