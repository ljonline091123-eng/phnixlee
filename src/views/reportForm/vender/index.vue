<template>
  <div class="app-container">
    <div class="context flex flex-column" style="height: 100%">
      <ShowTablePro
        ref="showTable"
        :total="total"
        :table-header-list="tableHeaderList"
        :table-data="tableData"
        :queryItemList="queryItemList"
        :loading="loading"
        :exportFlag="true"
        :queryFlag="true"
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
import { getVendorReport } from "@/api/reportForm/vendorReport";
import { formatDate } from "@/utils";

export default {
  components: { ShowTablePro },
  mixins: [mixinReport],
  data() {
    return {
      total: 0,
      queryItemList: [
        {
          prop: "area",
          label: "所属区域",
          type: "input",
        },
        {
          prop: "deptId",
          label: "合作单位",
          type: "treeSelect",
          labelKey: "label",
          valueKey: "thridDeptId",
        },
        {
          prop: "expenditureBusinessType",
          label: "供应商分类",
          type: "selectArr",
          arr: [
            { value: "5", label: "劳务分包" },
            { value: "4", label: "专业分包" },
            { value: "1", label: "购买材料" },
            { value: "2", label: "租赁材料" },
            { value: "3", label: "租赁机械（设备）" },
            { value: "6", label: "其他" },
          ],
        },
        {
          prop: "dateRange",
          label: "投标时间范围",
          type: "dateRange",
        },
      ],
      tableHeaderList: [
        {
          prop: "expenditureBusinessTypeText",
          label: "供应商分类",
          width: 120,
          showOverflowTooltip: true,
          headerSlot: "按供应商注册的一级分类统计",
        },
        {
          prop: "enterpriseLocation",
          label: "供应商所在地",
          width: 120,
          showOverflowTooltip: true,
          headerSlot: "获取供应商信息里边的注册地到市",
        },
        {
          prop: "vendorName",
          label: "供应商名称",
          width: 220,
          showOverflowTooltip: true,
          headerSlot: "获取供应商名字（全称）",
        },
        {
          prop: "partyAName",
          label: "组织机构名称",
          width: 230,
          showOverflowTooltip: true,
          headerSlot: "获取合作单位名字（甲方、采购人）",
        },
        {
          prop: "evaluationTypeName",
          label: "项目评分汇总",
          width: 180,
          showOverflowTooltip: true,
          headerSlot:
            "中湘大连取供应商评价(已确认状态)的合格、不合格次数，按合格次数由多到少排序；供应商评价未与单位/项目关联，同一供应商的各行展示的次数相同",
        },
        {
          prop: "tbiCount",
          label: "参与采购任务笔数",
          width: 80,
          showOverflowTooltip: true,
          headerSlot: "统计参与招标的笔数，发布一次招标文件回应后（投标）算一次",
        },
        {
          prop: "tbrCount",
          label: "中标任务笔数",
          width: 80,
          showOverflowTooltip: true,
          headerSlot: "统计参与招标的笔数，发布一次招标文件中标（结果发布）为准",
        },
        {
          prop: "tbrRate",
          label: "中标率",
          width: 80,
          showOverflowTooltip: true,
          headerSlot: "中标任务笔数/参与采购任务笔数*100%",
        },
        {
          prop: "tbrAmount",
          label: "中标金额（含税元）",
          width: 160,
          align: "right",
          headerSlot: "供应商中标金额",
        },
        {
          prop: "contractAmount",
          label: "合同金额（含税元）",
          width: 200,
          align: "right",
          headerSlot: "供应商合同签订金额",
        },
      ],
      tableData: [],
    };
  },
  mounted() {
    // 默认查登录用户数据权限范围内所有单位的数据(后端按登录用户所属单位收敛)
    this.getList(this.queryParams);
  },
  methods: {
    getList(params) {
      this.loading = true;
      getVendorReport(params)
        .then((res) => {
          this.tableData = res.data.list;
          this.total = res.data.total;
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
    },
    getExport(params) {
      this.download(
        "business/report/vendorReportExport",
        {
          ...params,
        },
        `${formatDate(new Date())}供应商报表.xlsx`
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
