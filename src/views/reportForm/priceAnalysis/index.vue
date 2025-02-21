<template>
  <div class="app-container">
    <div class="context flex flex-column">

    <el-radio-group
        v-model="radioType"
        size="small"
        style="margin-left: 15px; margin-top: 15px;"
      >
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in radioList"
          :key="dict.value"
          >{{ dict.label }}</el-radio-button
        >
      </el-radio-group>
    <el-dialog title="选择材料" :visible.sync="dialogVisible" width="75%">
      <div class="flex">
        <div style="width: 200px">
          <el-tree
            :data="treeOptions"
            class="tree_expert"
            :props="defaultProps"
            :expand-on-click-node="false"
            ref="tree"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          />
        </div>
        <div class="flex1">
          <el-form
            ref="form"
            :model="queryForm_Item"
            label-width="80px"
            size="small"
            :inline="true"
          >
            <el-form-item label="物料编号">
              <el-input
                v-model="queryForm_Item.costItemCode"
                placeholder="请输入物料编号"
              ></el-input>
            </el-form-item>
            <el-form-item label="物料名称">
              <el-input
                v-model="queryForm_Item.costItemName"
                placeholder="请输入物料名称"
              ></el-input>
            </el-form-item>
            <el-form-item label="规格">
              <el-input
                v-model="queryForm_Item.costItemSpecification"
                placeholder="请输入规格"
              ></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table
            :data="childItemList"
            :header-cell-style="{ background: '#F3F2F8', textAlign: 'center' }"
            style="width: 100%"
          >
            <el-table-column type="index" label="序号" width="50">
            </el-table-column>
            <el-table-column label="选择" align="center" width="70">
              <template scope="scope">
                <el-radio
                  :label="scope.row"
                  v-model="radio"
                  @click.native="handleSelectionChange($event, scope.row)"
                >
                  <span></span>
                </el-radio>
              </template>
            </el-table-column>
            <el-table-column prop="costItemCode" label="清单编码" width="200">
            </el-table-column>
            <el-table-column
              prop="costItemName"
              label="清单名称"
              align="center"
            >
            </el-table-column>
            <el-table-column
              prop="costItemSpecification"
              width="200"
              label="规格型号/项目特征"
            >
            </el-table-column>
<!--            <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>-->
<!--            <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules" />-->
<!--            <el-table-column label="工作内容" align="center" prop="workContent" />-->
            <el-table-column
              prop="costItemUnit"
              label="单位"
              align="center"
              width="60"
            >
            </el-table-column>
          </el-table>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="getCostItemCode">确 定</el-button>
      </span>
    </el-dialog>

    <ShowTable
      ref="showTable"
      :table-header-list="tableHeaderList"
      :table-data="tableData"
      :queryItemList="queryItemList"
      :radioType="radioType"
      @query="handleQuery"
      :loading="loading"
      :exportFlag=true
      @export="handleExport"
    >

    </ShowTable>
  </div>
</div>
</template>

<script>
import ShowTable from "@/views/reportForm/components/ShowTable.vue";
import { mixin } from "@/views/reportForm/mixins/mixin";
import { priceAnalysisReport,getPriceAnalysisReportByCon } from "@/api/reportForm/priceAnalysisReport";

export default {
  components: { ShowTable },
  mixins: [mixin],
  data() {
    return {
      radio: "",
       // 查询参数
        radioType: "C",
      // * 成本子目表格数据
      childItemList: [
        {
          id: 1,
          costItemCode: "10000030004000200160242",
          costItemName: "圆钢",
          costItemSpecification: "Φ10以外",
          costItemUnit: "吨",
        },
        {
          id: 2,
          costItemCode: "10000030004000200160204",
          costItemName: "圆钢",
          costItemSpecification: "φ10以内（含10mm)",
          costItemUnit: "吨",
        },
      ],
      type: "1",
      radioList: [
        {
          value: 'C',
          label: "物资采购价格",

        },
        {
          value: 'B',
          label: "专业分包价格",

        },
        {
          value: 'A',
          label: "劳务分包价格分",

        },
        {
          value: 'D',
          label: "租赁材料价格",

        },
        {
          value: 'G',
          label: "租赁设备（机械）价格",

        },
        {
          value: 'Z',
          label: "其他价格",

        },
      ],
      // * 成本子目form
      queryForm_Item: {},
      // * 成本子目树
      treeOptions: [
        {
          id: 2,
          label: "圆钢",
          children: [
            {
              id: 5,
              label: "热轧",
              children: [],
            },
            {
              id: 6,
              label: "冷拉",
              children: [],
            },
            {
              id: 7,
              label: "锻制",
              children: [],
            },
          ],
        },
        {
          id: 1,
          label: "户内电缆头",
          children: [
            {
              id: 4,
              label: "干包式",
              children: [],
            },
            {
              id: 9,
              label: "浇注式",
              children: [],
            },
            {
              id: 10,
              label: "热缩式",
              children: [],
            },
          ],
        },
      ],
      selectedRow: {},
      dialogVisible: false,
      defaultProps: {
        children: "children",
        label: "label",
      },
      queryItemList: [
      {
          prop: "subjectDtlCode",
          label: "物料/清单名称",
          type: "input",
          placeholder: "请输入物料/清单名称",
        },
        {
          prop: "specs",
          // label: "规格型号",
          label: "特征值特征项",
          type: "input",
          // placeholder: "请输入规格型号",
          placeholder: "请输入特征值特征项",
        },
        {
          prop: "measureUnit",
          label: "单位",
          type: "input",
          placeholder: "请输入单位",
        },
        {
          prop: "deptName",
          label: "组织机构名称",
          type: "input",
          placeholder: "请输入组织机构名称",
        },
        {
          prop: "areaName",
          label: "合同签订区域",
          type: "input",
          placeholder: "请输入合同签订区域",
        },
        {
          prop: "dateRange",
          label: "合同时间范围",
          type: "dateRange",
        },

      ],
      tableHeaderList: [
        {
          prop: "subjectDtlCode",
          label: "物资编号",
          width: 200,
          showOverflowTooltip: true,
        },
        {
          prop: "subjectDtlName",
          label: "物资名称",
          width: 200,
          showOverflowTooltip: true,
        },
        {
          prop: "specs",
          // label: "规格型号",
          label: "特征值特征项",
          width: 120,
          showOverflowTooltip: true,
        },
        {
          prop: "measureUnit",
          label: "单位",
          width: 200,
          showOverflowTooltip: true,
        },
        {
          prop: "quantity",
          label: "总量",
          width: 100,
          showOverflowTooltip: true,
        },
        {
          prop: "ntaxPrice",
          label: "总价(不含税)",
          width: 100,
          showOverflowTooltip: true,
        },
        {
          prop: "lastPrice",
          label: "最新单价(不含税)",
          width: 100,
          showOverflowTooltip: true,
        },
        {
          prop: "minPrice",
          label: "最低单价(不含税)",
          width: 100,
          showOverflowTooltip: true,
        },
        {
          prop: "maxPrice",
          label: "最高单价(不含税)",
          width: 200,
          align: "right",
        },
        {
          prop: "avgPrice",
          label: "平均单价(不含税)",
          width: 200,
          align: "right",
        },


        {
          prop: "deptName",
          label: "组织机构名称",
          width: 200,
          align: "right",
        },
        {
          prop: "minAccountFullName",
          label: "项目名称",
          width: 200,
          align: "right",
        },
        {
          prop: "conName",
          label: "合同名称",
          width: 200,
          align: "right",
        },
        {
          prop: "signDate",
          label: "合同签订时间",
          width: 200,
          align: "right",
        },
        {
          prop: "notTaxUnitPrice",
          label: "合同签订区域--",
          width: 200,
          align: "right",
        },


        {
          prop: "initialPrice",
          label: "成交单价(不含税)",
          width: 160,
          align: "right",
        },
        // {
        //   prop: "notTaxUnitPrice",
        //   label: "供应商名称",
        //   width: 160,
        //   align: "right",
        // },
      ],
      tableData: [],
    };
  },
  methods: {
    getExport(params) {
      this.download(
        "business/report/priceAnalysisExport",
        {
          ...params,
        },
        `report_${new Date().getTime()}.xlsx`
      );
    },
    /**
     * 选择成本子目后确认
     */
    getCostItemCode() {
      this.$set(
        this.$refs.showTable.queryParams,
        "costItemCode",
        this.selectedRow.costItemCode
      );
      this.$set(
        this.$refs.showTable.queryParams,
        "costItemName",
        this.selectedRow.costItemName
      );
      this.dialogVisible = false;
    },
    /**
     * 单选成本子目
     * @param e
     * @param val
     */
    handleSelectionChange(e, val) {
      if (e.target.tagName === "INPUT") {
        if (this.radio === "") {
          this.selectedRow = val;
        } else {
          this.radio = "";
          this.selectedRow = {};
        }
      }
    },
    /**
     * 成本子目树点击事件
     * @param data
     */
    handleNodeClick(data) {
      console.log(data);
    },
    /**
     * 点击成本子目
     */
    clickQueryItem() {
      this.dialogVisible = true;
    },
    getList(params) {
      this.loading = true;
      getPriceAnalysisReportByCon(params)
        .then((res) => {
          this.tableData = res.data;
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
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
