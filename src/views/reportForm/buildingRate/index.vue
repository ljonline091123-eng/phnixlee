<template>
  <ShowTable :table-header-list="tableHeaderList" :table-data="tableData" :queryItemList="queryItemList"
             @query="handleQuery" :loading="loading" :exportFlag=true @export="handleExport"></ShowTable>
</template>

<script>
import ShowTable from "@/views/reportForm/components/ShowTable.vue";
import {bidCountReport, getProjectCode, tenderingRateReport} from "@/api/reportForm/tenderingRateReport";
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
        valueKey: 'thridDeptId'
      }, {
        prop: 'minAccountFullName',
        label: '项目名称',
        type: 'input',
      }, {
        prop: 'prjState',
        label: '项目业态',
        type: 'select',
      }],
      tableHeaderList: [
        {prop: 'deptName', label: '组织机构',width: 200,showOverflowTooltip: true},
        {prop: 'minAccountFullName', label: '项目名称',width: 200,showOverflowTooltip: true},
        {prop: 'prjStateName', label: '项目业态',width: 100}, 
        {prop: 'cgNum', label: '采购次数',align: 'right',clickMethod:(row)=>{
          if (row.type === "G") {
            getProjectCode(row.id).then(res=>{
              this.$router.push({path:'/procurement/bindding',query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'all',report:'report'}})
              // window.$wujie?.bus.$emit('routeChange', {path: `/zbcg/procurement/procurement$bindding`,query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'all',report:'report'}})
            })
          } else {
            this.$router.push({path:'/procurement/bindding',query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'all',report:'report'}})
            // window.$wujie?.bus.$emit('routeChange', { path: `/zbcg/procurement/procurement$bindding`,query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'all',report:'report'}})
          }
          }},
        {prop: 'gkNum', label: '公开次数',align: 'right',clickMethod:(row)=>{
            if (row.type === "G") {
              getProjectCode(row.id).then(res=>{
                this.$router.push({path:'/procurement/bindding',query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'1',report:'report'}})
                // window.$wujie?.bus.$emit('routeChange', {path: `/zbcg/procurement/procurement$bindding`,query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'1',report:'report'}})
              })
            } else {
              this.$router.push({path:'/procurement/bindding',query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'1',report:'report'}})
              // window.$wujie?.bus.$emit('routeChange', { path: `/zbcg/procurement/procurement$bindding`,query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'1',report:'report'}})
            }
          }},
        {prop: 'yqNum', label: '邀标次数',width: 100,align: 'right',clickMethod:(row)=>{
            if (row.type === "G") {
              getProjectCode(row.id).then(res=>{
                this.$router.push({path:'/procurement/bindding',query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'2',report:'report'}})
                // window.$wujie?.bus.$emit('routeChange', {path: `/zbcg/procurement/procurement$bindding`,query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'2',report:'report'}})
              })
            } else {
              this.$router.push({path:'/procurement/bindding',query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'2',report:'report'}})
              // window.$wujie?.bus.$emit('routeChange', { path: `/zbcg/procurement/procurement$bindding`,query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'2',report:'report'}})
            }
          }},
        {prop: 'xjNum', label: '询价次数',width: 100,align: 'right',clickMethod:(row)=>{
            if (row.type === "G") {
              getProjectCode(row.id).then(res=>{
                this.$router.push({path:'/procurement/bindding',query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'3',report:'report'}})
                // window.$wujie?.bus.$emit('routeChange', {path: `/zbcg/procurement/procurement$bindding`,query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'3',report:'report'}})
              })
            } else {
              this.$router.push({path:'/procurement/bindding',query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'3',report:'report'}})
              // window.$wujie?.bus.$emit('routeChange', { path: `/zbcg/procurement/procurement$bindding`,query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'3',report:'report'}})
            }
          }},
        {prop: 'dyNum', label: '单一次数',width: 100,align: 'right',clickMethod:(row)=>{
            if (row.type === "G") {
              getProjectCode(row.id).then(res=>{
                this.$router.push({path:'/procurement/bindding',query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'4',report:'report'}})
                // window.$wujie?.bus.$emit('routeChange', {path: `/zbcg/procurement/procurement$bindding`,query:{projectCodeList:res.data,type:'buildingRate',noticeStatus:'8',procurementType:'4',report:'report'}})
              })
            } else {
              this.$router.push({path:'/procurement/bindding',query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'4',report:'report'}})
              // window.$wujie?.bus.$emit('routeChange', { path: `/zbcg/procurement/procurement$bindding`,query:{projectCode:row.id,type:'buildingRate',noticeStatus:'8',procurementType:'4',report:'report'}})
            }
          }},
        {prop: 'gkTotalNum', label: '公开总次数',width: 120,align: 'right'},
        {prop: 'ngkTotalNum', label: '非公开总次数',width: 130,align: 'right'},
        {prop: 'nBidTotalNum', label: '非招标总数',width: 100,align: 'right'},
        {prop: 'gkRatio', label: '公开率（%）',width: 100,align: 'right'},
      ],
      tableData: [],
    }
  },
  methods: {
    getList(params) {
      this.loading = true;
      // tenderingRateReport(params).then(res => {
      //   this.tableData = res.data
      //   this.loading = false;
      // }).catch(() => {
      //   this.loading = false;
      // })
      // console.log("window.localStorage.getItem(scopeType)building",window.localStorage.getItem("scopeType"))
      // console.log("paramsbuilding1",params)
      // if(window.localStorage.getItem("scopeType") == "1"){
      //   params.id = '';
      // } else {
      //   params.minAccountCode = '';
      // }
      // console.log("paramsbuilding2",params)
      console.log("building-scopeType-查询",this.$store.state.app.scopeType)
      console.log("building-queryParams-查询",this.params)
      console.log("building-project-查询",this.project)
      console.log("building-org-查询",this.org)
      bidCountReport(params).then(res => {
        this.tableData = res.data
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      })
    },
    getExport(params) {
      this.download(
        "business/report/bidCountReportExport",
        {
          ...params,
        },
        `report_${new Date().getTime()}.xlsx`
      );
    },
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
