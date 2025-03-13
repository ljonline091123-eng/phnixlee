<template>
  <div class="app-container">
    <BackButton  :title="title+'信息详情'">
      <div >
      <el-button  v-if="['1'].includes(model.state)" type="primary" size="mini"  @click="handelSanction">审批</el-button>
      <el-button type="primary" size="mini"  @click="handelCalibrationApproval">审批详情</el-button>
      </div>
    </BackButton>
    <el-form label-width="150px" label-suffix=":">
      <el-row>
        <el-col :span="8">
          <el-form-item label="所属层级">{{
            model.belongingLevel
          }}</el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="title+'名称'">{{
            model.materialName
          }}</el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="title+'编码'">{{
            model.materialCode
          }}</el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="单位">{{
            model.unit
          }}</el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="是否交易标的物">{{
            model.isTransaction==='0'?'否':'是'
          }}</el-form-item>
        </el-col>
    
       
      </el-row>
    </el-form>
    <div class="rightBox">
      <template >
      <showMaterial v-if="radioType==='0'" :orgId="organCode"  :wfBatch="wfBatch" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="model"></showMaterial>
      <showDevice v-if="radioType==='1'" :orgId="organCode"  :wfBatch="wfBatch" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="model"></showDevice>
      <showLabour v-if="radioType==='2'" :orgId="organCode"  :wfBatch="wfBatch" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="model"></showLabour>
      <showSubcontracting v-if="radioType==='3'" :orgId="organCode"  :wfBatch="wfBatch" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="model"></showSubcontracting>
    </template>
    <!-- <el-empty  description="请选择合适的分类"></el-empty> -->
    </div>
    <ApprovalForm
    :visible.sync="sanctionVisible"
    :title="title+'审批流程'"
    :formModel="sanctionForm"
    :rejectNodeList="rejectNodeList"
    :nextCandidateList="nextCandidateList"
    :nextAppointable="nextAppointable"
    @update:visible="sanctionVisible = $event"
    @submit="handleSubmit"
  />
    <ApprovalDetailsDialog
    :visible.sync="calibrateVisible"
    title="工料机档案审批流程详情"
    :activeStep="calibrateActive"
    :processInformationList="processInformationList"
    :approveLists="approveArr"
    :loading="calibrateLoading"
    @update:visible="calibrateVisible = $event"
  />
  </div>
</template>

<script>
  import { Base64 } from "js-base64";
  import { getMaterialApprove,getPermissionButton,materialApprove,archivesLoadTaskDef} from "@/api/archivese/dossier/approve";
  import {getMaterialType,getMaterialDetailsList ,getMaterialItemList } from "@/api/archivese/dossier/materialType";
  import BackButton from "@/components/BackButton/index.vue";
  import showMaterial from "@/views/archives/dossier/materialType/components/showMaterial.vue";
  import showDevice from "@/views/archives/dossier/materialType/components/showDevice.vue";
  import showLabour from "@/views/archives/dossier/materialType/components/showLabour.vue";
  import showSubcontracting from "@/views/archives/dossier/materialType/components/showSubcontracting.vue";
  import ApprovalForm from "@/components/Approval/approvalForm.vue";
  import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
  import {
  getProcessLogList, 
} from "@/api/procurement/manage";
export default {
  components: {BackButton,showMaterial,showDevice,showLabour,showSubcontracting,ApprovalForm,ApprovalDetailsDialog},
  name: "SubcontractingType",
  data() {
    return {
      organCode:"",
      title:"材料档案",
      radioType: "",
      baseUrl:'/archives/materialType',
      baseUrlItem:'/archives/materialItem',
      baseUrlDetails:'/archives/materialDetails',
      isEditable:false,
      sanctionVisible:false,
      isShowAdd:false, //
      wfBatch:'',
      featureList:[],
      materialDetailsList:[],
      model:{
        belongingLevel:''
      },
      sanctionForm: {
        pass: true,
        rejectTaskKey: "",
        operateComment: "",
      },
      rejectNodeList: [],
      nextCandidateList: [],
      approveArr: [],
      nextAppointable: false,
      taskPresentId:'',
      isShowButton: false,
      calibrateLoading: false,
      calibrateVisible:false,
      processKey:'',
      wfProcessId:'',
      businessId:'',
      processInformationList: [],
      calibrateActive: 1,
    };
  },
  watch:{
    radioType: {
   
        handler(index) {
          console.log("param613---------" + this.radioType)
          if(index==='0'){
        this.baseUrl='/archives/materialType'
        this.baseUrlItem='/archives/materialItem'
        this.baseUrlDetails='/archives/materialDetails'
        this.title='材料档案'
        this.processKey = "jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_MATERIAL";
        }else  if(index==='1'){
         
        this.baseUrl='/archives/deviceType'
        this.baseUrlItem='/archives/deviceItem',//特征项目
        this.baseUrlDetails='/archives/deviceDetails'//具体档案
        this.title='设备档案'
        this.processKey = "jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_DEVICE";
        }else  if(index==='2'){
        this.baseUrl='/archives/labourType'
        this.baseUrlItem='/archives/labourItem',//特征项目
        this.baseUrlDetails='/archives/labourDetails'//具体档案
        this.title='劳务档案'
        this.processKey = "jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_LABOUR";
        }else  if(index==='3'){
        this.baseUrl='/archives/subcontractingType'
        this.baseUrlItem='/archives/subcontractingItem',//特征项目
        this.baseUrlDetails='/archives/subcontractingDetails'//具体档案
        this.title='专业分包档案'
        this.processKey = "jiantou-zhaocai:{org}:ZHAOCAI_ASSISTANT_SUBCONTRACTING";
        }
        }
      }
  },
  created() {
  
    const param = JSON.parse(Base64.decode(this.$route.params.params));
    console.log("param613" + param);
    this.businessId = param;
    // this.businessId='3913689259135074304'
    this.getMaterialApproveFn()
    
  },
  methods: {
    treeClick(){

    },
     // 审批逻辑
     async getPermissionButtonFn() {
      try {
        // if (this.purchaserId && this.exampleId) {
          const res = await getPermissionButton({
            businessId: this.businessId, //联系人id
            processId: this.wfProcessId, //流程id
          });
          this.rejectNodeList = res.data.completedTaskList;
          this.taskPresentId = res.data.curTaskId;
          this.isShowButton = res.data.auditable;
        // }
      } catch (error) {}
    },
    handelSanction() {
      this.sanctionVisible = true;
    },
    //审批
    handleSubmit() {
      this.$modal.loading("请稍候...");

      const params = {
        ...this.sanctionForm,
        businessId: this.businessId, //联系人id
        processId: this.wfProcessId, //流程id1890244511259430915
        curTaskId: this.taskPresentId,
        processKey: this.processKey,
      };
      materialApprove(params).then(() => {
        this.$message.success("提交成功");
        this.$router.go(-1);
        this.$modal.closeLoading();
        this.sanctionVisible = false;
        this.getMaterialApproveFn()

      }).catch(error => {
        /* 关闭遮罩层 */
        this.$modal.closeLoading();
      });
    },
    //获取初使化数据
    getMaterialApproveFn(){
      getMaterialApprove(this.businessId).then(response => {
        this.radioType=response.data.type;
        let rData  = response.data || [];
        this.wfBatch=rData.id
        this.wfProcessId=rData.wfProcessId
        this.featureList=rData.materialItemVos
        this.getPermissionButtonFn();
        setTimeout(() => {
          this.getMaterialTypeFn(response.data.joinId)
        }, 200);
      });
    },
    //获取类型详情
    getMaterialTypeFn(id){
      console.log("param613---------" + this.baseUrl)
      getMaterialType(id,this.baseUrl).then(response => {
            console.log(JSON.stringify(response.data))
            this.model=response.data
            this.organCode=this.model.organCode
            if(this.radioType==='1'){
              this.model.materialName=this.model.deviceName
              this.model.materialCode=this.model.deviceCode
            }else if(this.radioType==='2'){
              this.model.materialName=this.model.labourName
              this.model.materialCode=this.model.labourCode
            }else if(this.radioType==='3'){
              this.model.materialName=this.model.subcontractingName
              this.model.materialCode=this.model.subcontractingCode
            }
            this.getData(this.wfBatch);
            });
        },
    getData(wfBatch){
          getMaterialDetailsList({organCode:this.model.organCode,typeId:this.model.id,wfBatch:wfBatch},this.baseUrlDetails).then(response => {
              console.log(JSON.stringify(response.rows))
          let rData1  = response.rows || [];
          this.materialDetailsList=rData1
          });
    },
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        let params = {
          businessId: this.businessId,
          processId: this.wfProcessId,
        };
        let res = null;
        if (this.businessId && this.wfProcessId) {
          res = await archivesLoadTaskDef(params);
        }
          this.processInformationList = res.data;
          function getActive(nodes) {
          let allFalse = true;
          for (let i = 0; i < nodes.length; i++) {
            if (!nodes[i].completed) {
              if (i === 0) {
                return 0;
              } else {
                return i;
              }
            }
            allFalse = false;
          }
          return nodes.length;
        }
        this.calibrateActive = getActive(this.processInformationList);

        if (this.businessId && this.wfProcessId) {
          const response = await getProcessLogList(params);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
  }
};
</script>
<style lang="scss" scoped>
 .el-form-item {
  margin-bottom: 2px !important;
 }
 ::v-deep .el-table__body tr.current-row > td.el-table__cell, .el-table__body tr.selection-row > td.el-table__cell {
  background: rgba(6, 228, 91, 0.4) !important;;
}
  </style>