<template>
  <ShowTable :table-header-list="tableHeaderList" :table-data="tableData" :queryItemList="queryItemList"
             @query="handleQuery" :loading="loading"></ShowTable>
</template>

<script>
import ShowTable from "@/views/reportForm/components/ShowTable.vue";
import {mixin} from "@/views/reportForm/mixins/mixin";
import {contractLedgerReport} from "@/api/reportForm/contractLedgerReport";

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
        {prop: 'procurementTypeName', label: '采购需求类型',width: 100},
        {prop: 'contractCode', label: '合同编号',width: 200,showOverflowTooltip: true,clickMethod:(row)=>{
            this.$router.push({path: '/procurement/contract-detail',query: {getId: row.contractId}})
          }},
        {prop: 'contractName', label: '合同名称',width: 200,showOverflowTooltip: true},
        {
          prop: 'address',
          label: '供应单位',
          type: 'multiple',
          children: [
            {prop: 'vendorName', label: '供应商名称',width: 300,showOverflowTooltip: true},
            {prop: 'vendorMainContact', label: '联系人',width: 100},
            {prop: 'vendorMainContactTel', label: '联系电话',width: 160,align: 'right'}
          ]
        },
        {
          prop: 'address',
          label: '成本子目',
          type: 'multiple',
          children: [
            {prop: 'costItemName', label: '名称',width: 300,showOverflowTooltip: true},
            {prop: 'costItemBrand', label: '品牌',width: 110,showOverflowTooltip: true},
            {prop: 'costItemSpecification', label: '型号',width: 160}
          ]
        },
        {prop: 'costItemUnit', label: '计量单位'},
        {prop: 'costItemUnitPrice', label: '合同单价(元)',width: 130,align: 'right'},
        {prop: 'costItemCount', label: '数量',align: 'right'},
        {prop: 'costItemAmount', label: '合同金额(元)',width: 130,align: 'right'}
      ],
      tableData: [],
    }
  },
  methods: {
    getList(params) {
      this.loading = true;
      contractLedgerReport(params).then(res => {
        this.tableData = res.data
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      })
    }
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
