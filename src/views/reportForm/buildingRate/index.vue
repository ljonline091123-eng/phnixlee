<template>
  <ShowTable :table-header-list="tableHeaderList" :table-data="tableData" :queryItemList="queryItemList"
             @query="handleQuery" :loading="loading"></ShowTable>
</template>

<script>
import ShowTable from "@/views/reportForm/components/ShowTable.vue";
import {tenderingRateReport} from "@/api/reportForm/tenderingRateReport";
import {mixin} from "@/views/reportForm/mixins/mixin";

export default {
  components: { ShowTable},
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
        prop: 'dateRange',
        label: '时间范围',
        type: 'dateRange',

      }],
      tableHeaderList: [
        {prop: 'deptName', label: '组织机构',width: 300,showOverflowTooltip: true},
        {prop: 'projectName', label: '项目名称',width: 300,showOverflowTooltip: true},
        {prop: 'procurementTypeName', label: '采购需求类型',width: 100},
        {prop: 'procurementCount', label: '采购次数',align: 'right',clickMethod:(row)=>{
          this.$router.push({path:'/procurement/plan',query:{projectCode:row.projectCode,noticeStatus:'8',procurementPlanType:row.procurementTypeCode}})
          }
        },
        // {prop: 'openBidCount', label: '公开招标次数'},
        {prop: 'inviteBidCount', label: '邀请招标次数',width: 100,align: 'right',clickMethod:(row)=>{
            this.$router.push({path:'/procurement/bindding',query:{projectCode:row.projectCode,noticeStatus:'8',procurementType:'2'}})
          }},
        {prop: 'enquiryProcurementCount', label: '询问采购次数',width: 100,align: 'right',clickMethod:(row)=>{
            this.$router.push({path:'/procurement/bindding',query:{projectCode:row.projectCode,noticeStatus:'8',procurementType:'3'}})
          }},
        {prop: 'onlySourceCount', label: '单一来源次数',width: 100,align: 'right',clickMethod:(row)=>{
            this.$router.push({path:'/procurement/bindding',query:{projectCode:row.projectCode,noticeStatus:'8',procurementType:'4'}})
          }},
        {prop: 'openBidTotalCount', label: '公开招标总次数',width: 120,align: 'right'},
        {prop: 'noOpenBidTotalCount', label: '非公开招标总次数',width: 130,align: 'right'},
        {prop: 'noOpenBidRate', label: '非公开招标率',width: 100,align: 'right'},
        {prop: 'openBidRate', label: '公开招标率',width: 100,align: 'right'},
      ],
      tableData: [],
    }
  },
  methods: {
    getList(params) {
      this.loading = true;
      tenderingRateReport(params).then(res => {
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
</style>
