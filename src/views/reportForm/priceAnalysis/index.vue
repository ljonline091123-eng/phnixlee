<template>
  <div style="width: 100%; height: 100%">
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
      @query="handleQuery"
      :loading="loading"
    >
      <template #querySlot="scope">
        <el-form-item label="成本子目">
          <el-input
            readonly
            v-model="scope.queryParms['costItemName']"
            :placeholder="`请选择成本子目`"
            @click.native="clickQueryItem"
          ></el-input>
        </el-form-item>
      </template>
    </ShowTable>
  </div>
</template>

<script>
import ShowTable from "@/views/reportForm/components/ShowTable.vue";
import { mixin } from "@/views/reportForm/mixins/mixin";
import { priceAnalysisReport } from "@/api/reportForm/priceAnalysisReport";

export default {
  components: { ShowTable },
  mixins: [mixin],
  data() {
    return {
      radio: "",
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
          prop: "areaName",
          label: "地区",
          type: "input",
          placeholder: "请输入地区",
        },
        {
          prop: "dateRange",
          label: "时间范围",
          type: "dateRange",
        },
      ],
      tableHeaderList: [
        {
          prop: "costItemName",
          label: "成本子目名称",
          width: 240,
          showOverflowTooltip: true,
        },
        {
          prop: "costItemUnit",
          label: "计量单位",
          width: 80,
          showOverflowTooltip: true,
        },
        {
          prop: "costItemSpecification",
          label: "型号",
          width: 120,
          showOverflowTooltip: true,
        },
        {
          prop: "areaName",
          label: "地区",
          width: 100,
          showOverflowTooltip: true,
        },
        {
          prop: "deptName",
          label: "组织机构",
          width: 300,
          showOverflowTooltip: true,
        },
        {
          prop: "projectName",
          label: "项目名称",
          width: 300,
          showOverflowTooltip: true,
        },
        {
          prop: "contractCode",
          label: "合同编号",
          width: 200,
          showOverflowTooltip: true,
        },
        {
          prop: "contractTime",
          label: "合同签约时间",
          width: 160,
          showOverflowTooltip: true,
        },
        {
          prop: "taxUnitPrice",
          label: "签约单价(含税)",
          width: 160,
          align: "right",
        },
        {
          prop: "notTaxUnitPrice",
          label: "签约单价(不含税)",
          width: 160,
          align: "right",
        },
      ],
      tableData: [],
    };
  },
  methods: {
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
      priceAnalysisReport(params)
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
