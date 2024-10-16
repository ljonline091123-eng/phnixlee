<template>
  <div>
  <ShowTable :table-header-list="tableHeaderList" :table-data="tableData" :queryItemList="queryItemList"
             @query="handleQuery" :loading="loading"></ShowTable>
       <!-- 选择采购计划 -->
       <el-dialog
        title="物料明细"
        :visible.sync="materialShow"
        width="60%" 
        @closed="materialShow = false"
      >
            <el-table
            v-loading="loading"
            :data="detailList"
            stripe
            style="min-height: 400px; overflow: auto;"
            size="small"
            highlight-current-row
            border
            @selection-change="handleSelectionChange"
          >
            <el-table-column
            prop="measureUnit"
            label="成本子目"
            align="center"
            width="100"
            show-overflow-tooltip
            >
                <el-table-column
                prop="subjectName"
                label="名称"
                width="100"
                show-overflow-tooltip
              />
              <el-table-column
                prop="brand"
                label="品牌"
                width="100"
                show-overflow-tooltip
              />
                <el-table-column
                  prop="specs"
                  label="型号"
                  width="100"
                  show-overflow-tooltip
                />
            </el-table-column>
            <el-table-column
            prop="measureUnit"
            label="计量单位"
            width="100"
            show-overflow-tooltip
          />
            <el-table-column
            prop="ntaxPrice"
            label="采购单价"
            width="100"
            show-overflow-tooltip
          />
            <el-table-column
            prop="quantity"
            label="数量"
            width="80"
            show-overflow-tooltip
          />
            <el-table-column
            prop="settledAmount"
            label="已结算金额(万元)"
            width="140"
            show-overflow-tooltip
          />
            <el-table-column
            prop="paidAmount"
            label="已付款金额(万元)"
            width="140"
            show-overflow-tooltip
          />
            <el-table-column
            prop="UnpaidAmount"
            label="未付款金额(万元)"
            width="140"
            show-overflow-tooltip
          />
            <el-table-column
            prop="remark"
            label="备注"
            width="100"
            show-overflow-tooltip
          />
            </el-table>
      </el-dialog>
   
    </div>
</template>

<script>
import ShowTable from "@/views/reportForm/components/ShowTable.vue";
import {mixin} from "@/views/reportForm/mixins/mixin";
import {contractLedgerReport,contractLedgerByConBase,contractLedgerDetails} from "@/api/reportForm/contractLedgerReport";

export default {
  components: {ShowTable},
  mixins: [mixin],
  data() {
    return {
      queryItemList: [{
        prop: 'deptId',
        label: '组织机构',
        type: 'treeSelect',
        options: [],
        labelKey: 'deptName',
        valueKey: 'deptId'
      }, {
        prop: 'contractCode',
        label: '合同编号',
        type: 'input',
        placeholder: '请输入合同编号'
      }, {
        prop: 'dateRange',
        label: '时间范围',
        type: 'dateRange',

      }],
      tableHeaderList: [
        {prop: 'deptName', label: '组织机构',width: 300,showOverflowTooltip: true},
        
        {prop: 'conCode', label: '合同编号',width: 200,showOverflowTooltip: true},
        {prop: 'conName', label: '合同名称',width: 200,showOverflowTooltip: true,clickMethod:(row)=>{
          this.loading = true;
                contractLedgerDetails(row.id).then(res => {
                  this.detailList = res.data
                  this.materialShow=true
                  this.loading = false;
                }).catch(() => {
                  this.loading = false;
      })
          }},
        {prop: 'contractNumber', label: '合同数量(笔数)',width: 200,align: 'center',showOverflowTooltip: true},
        {prop: 'ntaxChangedAmount', label: '合同金额(元)',width: 200,showOverflowTooltip: true},
        {
          prop: 'address',
          label: '供应单位',
          type: 'multiple',
          children: [
            {prop: 'partbName', label: '名称',width: 300,showOverflowTooltip: true},
            {prop: 'partbSiteManager', label: '联系人',width: 100},
            {prop: 'partbSiteManagerPhone', label: '联系电话',width: 200,align: 'right'}
          ]
        },
        // {
        //   prop: 'address',
        //   label: '成本子目',
        //   type: 'multiple',
        //   children: [
        //     {prop: 'costItemName', label: '名称',width: 300,showOverflowTooltip: true},
        //     {prop: 'costItemBrand', label: '品牌',width: 110,showOverflowTooltip: true},
        //     {prop: 'costItemSpecification', label: '型号',width: 160}
        //   ]
        // },
        // {prop: 'costItemUnit', label: '计量单位'},
        // {prop: 'procurementTypeName', label: '采购单位',width: 150},
        // {prop: 'costItemCount', label: '数量',align: 'right'},
        // {prop: 'costItemAmount', label: '已结算金额(万元)',width: 130,align: 'right'},
        // {prop: 'costItemAmount', label: '已付款金额(万元)',width: 130,align: 'right'},
        // {prop: 'costItemAmount', label: '未付款金额(万元)',width: 130,align: 'right'},
        // {prop: 'costItemAmount', label: '备注',width: 130,align: 'right'}
      ],
      tableData: [],
      detailList:[],
      materialShow:false,
      loading:false,
    }
  },
  methods: {
    getList(params) {
      this.loading = true;
      contractLedgerByConBase(params).then(res => {
        this.tableData = res.data
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      })
    }
  },

  getContractLedgerDetails(id){
    let params={}
    params.id =  id;
    contractLedgerDetails(params).then(res => {
        this.detailList = res.data
        this.materialShow=true
        // this.loading = false;
      }).catch(() => {
        // this.loading = false;
      })
  }

}
</script>

<style lang="scss" scoped>
::v-deep .vue-treeselect {
  .vue-treeselect__control {
    height: 32px;
  }
}
//::v-deep .el-table--border .el-table__cell {
//  border: none
//}
</style>
