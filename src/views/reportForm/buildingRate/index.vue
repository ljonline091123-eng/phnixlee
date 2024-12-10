<template>
  <Drag>
    <template v-slot:left-content style="background: #fff;">
          <!-- 动态生成圆形按钮 -->
          <div class="left">
          <el-input
              v-model="deptName"
              placeholder="请输入部门名称"
              clearable
              size="small"
              prefix-icon="el-icon-search"
              style="width: 220px;margin:10px 0 0 10px;"
            />
          <div class="circle-buttons" style="margin: 10px 0 0 10px">
            <button
              v-for="n in buttonCount"
              :key="n"
              class="circle-btn"
              :class="levelExpand==n?'circle-btn-selected':'circle-btn'"
              @click="expandNodes(n)"
            >
              {{ n }}
            </button>
          </div>
          <el-tree
            v-loading="deptTreeLoading"
            :data="deptOptions"
            :default-expanded-keys="treeData"
            class="address-tree"
            :props="defaultProps"
            :expand-on-click-node="true"
            :filter-node-method="filterNode"
            ref="tree"
            node-key="deptId"
            highlight-current
            @node-click="handleNodeClick"
          />
     
        </div>
    </template>
    <template v-slot:right-content>
      <ShowTable :table-header-list="tableHeaderList" :table-data="tableData" :queryItemList="queryItemList"
      @query="handleQuery" :loading="loading" :exportFlag=true @export="handleExport"></ShowTable>
      </template>
  </Drag>

</template>

<script>
import ShowTable from "@/views/reportForm/components/ShowTable.vue";
import {bidCountReport, getProjectCode, tenderingRateReport, getOrgList} from "@/api/reportForm/tenderingRateReport";
import {mixin} from "@/views/reportForm/mixins/mixin";
import Drag from "@/components/Drag/index.vue";
export default {
  components: { ShowTable, Drag,},
  mixins: [mixin],
  data() {
    return {
      // 部门树选项
        // 部门名称
      deptName: undefined,
      buttonCount: 1,
      levelExpand:2,
      arrData:[],
      deptOptions: undefined,
      deptTreeLoading: false,
      treeData:['1826912577508798466'],
      defaultProps: {
        children: "children",
        label: "deptName",
      },
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
  mounted() {
    this.getOrgListFn()
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    },
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
      params.minAccountCode = 'SG20012024000002-2';
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
     // 筛选节点
     filterNode(value, data) {
      if (!value) return true;
      return data.deptName.indexOf(value) !== -1;
    },
    expandNodes(level) {
      this.levelExpand=level
      if (level === 1) {
          // 关闭所有默认节点
          const tree = this.$refs.tree;
          const allNodes = Object.values(tree.store.nodesMap);
          allNodes.forEach((node) => {
              // 关闭所有父节点
              if (node.data.parentId === "0") {
                tree.store.getNode(node.key).expanded = false;
              }
          });
        } else if (level === 2) {
          this.treeData=['1826912577508798466']
        }else if (level === 3) {
          this.arrData.forEach(element => {
            if(element.thridOrgLevel==2){
              this.treeData.push(element.deptId)
            }
          });
        }else if (level === 4) {
          this.arrData.forEach(element => {
              this.treeData.push(element.deptId)
          });
        }
    },
       // 节点单击事件
    handleNodeClick(data) {
      // this.queryParams.deptId = data.deptId;
      // this.handleQuery();
    },
    getOrgListFn(){
      this.deptTreeLoading = true;
      getOrgList('1000000000').then(res=>{
        this.arrData=res.data
        this.deptOptions = this.handleTree(res.data, "deptId");
        this.deptTreeLoading = false;
      })
      .then(() => {
          this.initButtons();
        })
    },
       // 递归计算最大层级
       calculateMaxLevel(nodes, level = 0) {
      let maxLevel = level;

      nodes.forEach((node) => {
        if (node.children && node.children.length > 0) {
          maxLevel = Math.max(
            maxLevel,
            this.calculateMaxLevel(node.children, level + 1)
          );
        }
      });

      return maxLevel;
    },
    // 初始化按钮
    initButtons() {
      const maxLevel = this.calculateMaxLevel(this.deptOptions);
      this.buttonCount = maxLevel + 1;
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
.address-tree {
  margin: 0;
  height: calc(100vh - 110px);
  overflow-y: scroll;

  &::-webkit-scrollbar {
    display: none;
  }

  ::v-deep .icon-shouyetianchong:before {
    content: "\E692";
    color: #004ea2;
  }

  ::v-deep .icon-24gf-folderOpen:before {
    content: "\eac5";
    color: #004ea2;
  }

  ::v-deep .el-tree-node {
    .el-tree-node__content {
      height: auto;
      padding: 2px 0;
      margin: 2px 0;
  
      font-size: 13px;
      color: #606266;
    

      .el-tree-node__label {
        white-space: pre-wrap;
        line-height: 20px;
      }
    }
  }
}

::v-deep .address-tree .el-tree-node .is-current >  .el-tree-node__content {
  color: #2b4acb !important;
}
.circle-btn {
  background-color: #e8e8ef;
  color: #999;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  text-align: center;
  line-height: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  margin: 5px;
}
.circle-btn-selected {
  background-color: #2b4acb;
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  text-align: center;
  line-height: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  margin: 5px;
}
/* 鼠标移上去时的效果 */
.circle-btn:hover {
  background-color: #2b4acb;
  color: #ffffff;
}
::v-deep .left_box{
    background: #ffffff;
}
</style>
