<template>
  <div class="containerMy" style="overflow-x: hidden;">
    <!-- <Drag style="background-color: #ffffff;"> -->
      <!-- <template v-slot:left-content> -->
    <treeMenu class="treeMenu" style="margin: 12px; " :dept-options="waitDeptOptions" :radioType="radioType" ref="orgTree"
   :levelExpand="2"  @treeClick="treeClickWait" 
    title="责任单位" :defaultExpandedKeys="defaultExpandedKeys" :loading="loading" :currentNodeKey="waitCurrentNodeKey"  ></treeMenu>
        <!-- </template> -->
        <!-- <template v-slot:right-content> -->
    <div style="width: 100%;">
      <div style="   width: 100%;height: 53px;margin: 10px 10px 10px 0; background-color: #ffffff;">
        <el-radio-group
        style="margin:10px 0 0 10px;"
        v-model="radioType"
        size="small"
        @change="handleRadioChange"
      >
          <el-radio-button
          :disabled="loading===true"
          :label="dict.value"
          :name="dict.value" 
          v-for="dict in radioList"
          :key="dict.value"
          >{{ dict.label }}</el-radio-button>
      </el-radio-group>
      </div>
    <div style="display: flex;">
      <Drag style="background-color: #ffffff;">
     <template v-slot:left-content>
         <treeMenu  class="treeMenuFq" :dept-options="deptOptionsTree" :radioType="radioType" ref="customTree1" @query="getListFq"  :queryType="true"
              @treeClick="treeClick" @mainAdd="mainAdd" @mainEdit="mainEdit" @mainDelete="mainDelete" :isMain="true"
              :title="radioName+'分类列表'" :loading="loadingtree"  :currentNodeKey="currentNodeKeyFq"   ></treeMenu>
              </template>
   <template v-slot:right-content>
    <div class="rightBox">
      <template >
      <showMaterial v-if="radioType==='0'" ref="material" :queryType="queryType"  @detailsLis="detailsLisFn" :orgId="org" @treeClick="treeClick" @mainMaterialAdd="mainMaterialAdd"  @mainMaterialEdit="mainMaterialEdit" @mainMaterialDelete="mainMaterialDelete" :isMain="true"  :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showMaterial>
      <showDevice v-if="radioType==='1'"  ref="material" :queryType="queryType"  @detailsLis="detailsLisFn" :orgId="org" @treeClick="treeClick" @mainMaterialAdd="mainMaterialAdd"  @mainMaterialEdit="mainMaterialEdit" @mainMaterialDelete="mainMaterialDelete"  :isMain="true"  :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showDevice>
      <showLabour v-if="radioType==='2'" ref="material" :queryType="queryType"  @detailsLis="detailsLisFn" :orgId="org" @treeClick="treeClick" @mainMaterialAdd="mainMaterialAdd"  @mainMaterialEdit="mainMaterialEdit" @mainMaterialDelete="mainMaterialDelete"  :isMain="true"  :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showLabour>
      <showSubcontracting v-if="radioType==='3'" ref="material" :queryType="queryType"  @detailsLis="detailsLisFn" :orgId="org" @treeClick="treeClick" @mainMaterialAdd="mainMaterialAdd"  @mainMaterialEdit="mainMaterialEdit" @mainMaterialDelete="mainMaterialDelete"  :isMain="true"  :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showSubcontracting>
    </template>
    </div>
    </template>
    </Drag>
  </div>
</div>
<!-- </template> -->
<!-- </Drag> -->
    <el-dialog class="custom-height" :title="title" @close="close" destroy-on-close :visible.sync="openMain" width="65%"  append-to-body>
      <div style="display: flex;">
        <treeMenu class="mainTreeMenu" :dept-options="deptOptions" :radioType="radioType" ref="customTree"
                  @query="getListMain"  @treeClick="treeClickMain" :title="radioName+'分类列表'" :loading="loadingMain" :currentNodeKey="currentNodeKey" ></treeMenu>
       
        <div  class="rightBoxMain">
          <template v-if="isShowData">
          <showMaterialMain v-if="radioType==='0'" ref="material" :modeMian="modeMian" :isShowType="isShowType" @treeClick="treeClick" @clickRowMain="clickRowMain" :detailsList="materialDetailsListMain" :isShowAdd="isShowAdd" :featureList="featureListMain"  :currentNode="currentNode"></showMaterialMain>
          <showDeviceMain v-if="radioType==='1'" ref="material" :isShowType="isShowType" @treeClick="treeClick" @clickRowMain="clickRowMain" :detailsList="materialDetailsListMain" :isShowAdd="isShowAdd" :featureList="featureListMain"  :currentNode="currentNode"></showDeviceMain>
          <showLabourMain v-if="radioType==='2'" ref="material" :isShowType="isShowType" @treeClick="treeClick" @clickRowMain="clickRowMain" :detailsList="materialDetailsListMain" :isShowAdd="isShowAdd" :featureList="featureListMain"  :currentNode="currentNode"></showLabourMain>
          <showSubcontractingMain v-if="radioType==='3'" ref="material" :isShowType="isShowType" @treeClick="treeClick" @clickRowMain="clickRowMain" :detailsList="materialDetailsListMain" :isShowAdd="isShowAdd" :featureList="featureListMain"  :currentNode="currentNode"></showSubcontractingMain>
        </template>
          <el-empty v-else description="请选择合适的分类"></el-empty>
        </div>
      </div>
      <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
        <el-button type="primary" @click="submitMain">确 定</el-button>
        <el-button @click="openMain=false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="编码已存在,请重新填写编码！" :visible.sync="renumberVisible"  width="400px" append-to-body>
      <el-form :model="form" label-width="68px">
        <el-form-item label="编码" >
          <el-input v-model="renumber" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="renumberVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmRenumber">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
  import {mapGetters} from "vuex";
import { getMaterialTypeTree,materialInitData,materialDetailsInitData} from "@/api/archivese/dossier/materialType";
import { getWaitDeptTree} from "@/api/archivese/wait/waitPending";
import treeMenu from "@/views/archives/dossier/materialType/components/treeMenu.vue";
import showMaterial from "@/views/archives/dossier/materialType/components/showMaterial.vue";
import showDevice from "@/views/archives/dossier/materialType/components/showDevice.vue";
import showLabour from "@/views/archives/dossier/materialType/components/showLabour.vue";
import showSubcontracting from "@/views/archives/dossier/materialType/components/showSubcontracting.vue";
import showMaterialMain from "@/views/archives/main/mtrClass/components/showMaterial.vue";
import showDeviceMain from "@/views/archives/main/mtrClass/components/showDevice.vue";
import showLabourMain from "@/views/archives/main/mtrClass/components/showLabour.vue";
import showSubcontractingMain from "@/views/archives/main/mtrClass/components/showSubcontracting.vue";
import {material} from "@/views/archives/dossier/materialType/archives/material";
import {pending} from "@/views/archives/wait/materialType/pending/pending";
import Drag from "@/components/Drag/index.vue";
export default {
  components: { treeMenu,showMaterial,showDevice,showSubcontracting,showLabour,showMaterialMain,showDeviceMain,showLabourMain,showSubcontractingMain,Drag},
  mixins: [material, pending],
  name: "MaterialType",
  data() {
    return {
      queryType:'untreated',
      isShowData:false,//是否可以录特征项特征值
      deptOptions:[],
      waitDeptOptions:[],
      deptOptionsTree:[],
      isShowType:'', //操作类型
      waitCurrentNodeKey:'1826912577508798466',
      defaultExpandedKeys:[],
      loading:false,
      loadingMain:false,
      loadingtree:false,
      isShowAdd:false,
      renumberVisible:false,//编码重写
      renumber:'',//编码重写的内容
      currentNodeKey:'',
      currentNodeKeyFq:'',
      title:'材料档案',
      radioName:"材料",
      radioType: "0",
      baseUrlTree:'/archives/materialType/getMaterialTypeTree',
      baseUrl:'/archives/materialType',
      baseUrlItem:'/archives/materialItem',
      baseUrlDetails:'/archives/materialDetails',
      baseUrlTreeMain:'/archives/mtrClass/getMtrClassTree',
      baseUrlMain:'/archives/mtrClass',
      baseUrlItemMain:'/archives/mtrFeature',
      baseUrlDetailsMain:'/archives/mtrArchives',
      modeMian:'add',
      org:'',
      radioList: [
        {
          value: '0',
          label: "材料档案",
        },
        {
          value: '1',
          label: "设备档案",
        },
        {
          value: '2',
          label: "劳务档案",
        },
        {
          value: '3',
          label: "专业分包档案",
        },
      ],
     
      
    };
  },
  watch:{
    radioType: {
        handler(index) {
          console.log(index)
          console.log(index==='0')
            if(index==='0'){
              this.baseUrlTree='/archives/materialType/getMaterialTypeTree'
              this.baseUrl='/archives/materialType'
              this.baseUrlItem='/archives/materialItem'
              this.baseUrlDetails='/archives/materialDetails'
              this.title='材料档案'
              this.baseUrlTreeMain='/archives/mtrClass/getMtrClassTree'
              this.baseUrlMain='/archives/mtrClass'
              this.baseUrlItemMain='/archives/mtrFeature'
              this.baseUrlDetailsMain='/archives/mtrArchives'
              }else  if(index==='1'){
              this.baseUrlTree='/archives/deviceType/getDeviceTypeTree'
              this.baseUrl='/archives/deviceType'
              this.baseUrlItem='/archives/deviceItem',//特征项目
              this.baseUrlDetails='/archives/deviceDetails'//具体档案
              this.title='设备档案'
              this.baseUrlTreeMain='/archives/deviceClass/getDeviceClassTree'
              this.baseUrlMain='/archives/deviceClass'
              this.baseUrlItemMain='/archives/deviceFeature',//特征项目
              this.baseUrlDetailsMain='/archives/deviceArchives'//具体档案
              }else  if(index==='2'){
              this.baseUrlTree='/archives/labourType/getLabourTypeTree'
              this.baseUrl='/archives/labourType'
              this.baseUrlItem='/archives/labourItem',//特征项目
              this.baseUrlDetails='/archives/labourDetails'//具体档案
              this.title='劳务档案'
              this.baseUrlTreeMain='/archives/laborClass/getLaborServicesClassTree'
              this.baseUrlMain='/archives/laborClass'
              this.baseUrlItemMain='/archives/laborFeature',//特征项目
              this.baseUrlDetailsMain='/archives/laborArchives'//具体档案
              }else  if(index==='3'){
              this.baseUrlTree='/archives/subcontractingType/getSubcontractingTypeTree'
              this.baseUrl='/archives/subcontractingType'
              this.baseUrlItem='/archives/subcontractingItem',//特征项目
              this.baseUrlDetails='/archives/subcontractingDetails'//具体档案
              this.title='专业分包档案'
              this.baseUrlTreeMain='/archives/majorClass/getMajorSubcontractingClassTree'
              this.baseUrlMain='/archives/majorClass'
              this.baseUrlItemMain='/archives/majorFeature',//特征项目
              this.baseUrlDetailsMain='/archives/majorArchives'//具体档案
              }
        }
      }
  },
  // computed:{
  //   ...mapGetters(['project','org']),
  //  },
  created() {
   this.getListWait()
  },
  methods: {
    handleRadioChange(value) {
     let radio =this.radioList[value]
     this.radioName=radio.label.replace("档案","");
    },
    getWaitDeptTreeFn(){
      this.defaultExpandedKeys=[]
      getWaitDeptTree().then(response => {
          this.waitDeptOptions=response || []
          console.log(JSON.stringify(this.waitDeptOptions[0]))
          this.waitCurrentNodeKey=this.waitDeptOptions[0].id
          this.defaultExpandedKeys.push(this.waitDeptOptions[0].id)
          this.loading=false
          //获取当前组织
          this.org=this.waitDeptOptions[0].code
          this.getMaterialTypeTreeFn(this.org,this.queryType)
        });
    },
    getMaterialTypeTreeFn(code,queryType){
      this.loadingtree=true
      getMaterialTypeTree({organCode:code,queryType:queryType},this.baseUrlTree).then(response => {
          this.loadingtree=false
          this.deptOptionsTree=response || []
          if(this.deptOptionsTree.length>0 ){
            this.currentNodeKeyFq=this.deptOptionsTree[0].id
          }
          
        
        });
    },
     /** 查询副材料分类列表 */
     async  getListFq(params) {
      if(this.org){
        this.queryType=params.queryType?params.queryType:this.queryType
        this.getMaterialTypeTreeFn(this.org,this.queryType)
      }
    },
    /** 查询组织列表 */
    async  getListWait(params) {
      this.loading = true;
      this.getWaitDeptTreeFn();
       
    },

        /** 查询主材料分类列表 */
    async  getListMain(params) {
      this.loadingMain = true;
      if(this.org){
        params.organCode=this.org
      }
      console.log("getListMain"+JSON.stringify(params))
      params.time=new Date().getTime()
        getMaterialTypeTree(params,this.baseUrlTreeMain).then(response => {
          let arr=response || []
          if(arr.length>0 ){
            this.deptOptions=arr
            this.currentNodeKey=this.deptOptions[0].id
          }
          this.loadingMain=false
        });
     
    },
  }
};
</script>
<style lang="scss" scoped>
  .containerMy {
    display: flex;
    /* margin: 0px;padding: 0px; */
  }
  .rightBox {
    width: 100%;
    /* width: calc(100vw - 470px); */
    margin:0px 0 0 10px;
    background-color: #ffffff;
  }
.rightBoxMain {
  width: calc(100vw - 170px);margin:0px 0 0 10px;
      background-color: #ffffff;
}
::v-deep .mainTreeMenu .address-tree{
  margin: 0;
  height: calc(100vh - 415px) !important;
  overflow-y: scroll;
}
::v-deep .treeMenuFq  .address-tree{
  margin: 0;
  height: calc(100vh - 245px) !important;
  overflow-y: scroll;
}
::v-deep .treeMenu  {
  height: calc(100vh - 40px);
      width: 260px !important;
      /* margin:-14px 8px 0 10px; */
    .el-input  {
      width: 200px !important;
      margin-left: 10px;
      }
    }
    ::v-deep .el-table__body tr.current-row > td.el-table__cell, .el-table__body tr.selection-row > td.el-table__cell {
  background: rgba(6, 228, 91, 0.4) !important;;
}
  </style>