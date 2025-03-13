<template>
  <div class="containerMy" style="overflow-x: hidden;">
    <div style="   width: 100%;height: 53px;margin: 10px; background-color: #ffffff;">
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
    <div v-if="isShowData" style="float: right;margin: 12px 18px;"> 
      <el-button type="primary" size="small"  @click="handelCalibrationApproval">审批详情</el-button>
      <el-button size="small" type="primary" icon="el-icon-top"  @click="submitLC" >提交</el-button>
  </div>
    </div>
    <!-- <div style="display: flex;"> -->
    <Drag style="background-color: #ffffff;">
    <template v-slot:left-content>
          <treeMenu :dept-options="deptOptions" :radioType="radioType" ref="customTree"
          @query="getList"  @treeClick="treeClick" @addItem="addItem" @editItem="editItem" @deleteItem="deleteItem"  @checkItem="checkItem"
          :title="radioName+'分类列表'" :loading="loading" :currentNodeKey="currentNodeKey" :isEditable="isEditable"  ></treeMenu>
          </template>
    <template v-slot:right-content>
    <div class="rightBox">
      <template v-if="isShowData">
      <showMaterial v-if="radioType==='0'"  ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showMaterial>
      <showDevice v-if="radioType==='1'" ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showDevice>
      <showLabour v-if="radioType==='2'" ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showLabour>
      <showSubcontracting v-if="radioType==='3'" ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList" :isShowAdd="isShowAdd" :featureList="featureList"  :currentNode="currentNode"></showSubcontracting>
    </template>
    <el-empty v-else description="请选择合适的分类"></el-empty>
    </div>
    </template>
    </Drag>
  <!-- </div> -->
    <el-dialog destroy-on-close :title="title+modeName" :visible.sync="openFiles" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="128px">
        <el-row>
          <el-form-item label="所属层级:" prop="label" :class="mode === 'view'?'custom-form-item':''">
            <span >{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
      
        <el-row>
          <el-form-item label="材料分类名称:" prop="materialName" :class="mode === 'view'?'custom-form-item':''">
            <span  v-if="mode === 'view'">{{form.materialName}}</span>
            <el-input v-else v-model="form.materialName" placeholder="请输入产品名称" :disabled="mode === 'view'"/>
          </el-form-item>
        </el-row>

        <el-row>
            <el-form-item label="材料分类编码:" prop="materialCode" :class="mode === 'view'?'custom-form-item':''">
              <!-- <el-input v-model="form.materialCode" placeholder="请输入产品描述" disabled/> -->
              <span >{{form.materialCode}}</span>
            </el-form-item>
        </el-row>
        <el-row>
          <el-form-item label="单位:" prop="unit" :class="mode === 'view'?'custom-form-item':''">
            <span  v-if="mode === 'view'">{{form.unit}}</span>
            <el-input  v-else v-model="form.unit" placeholder="请输入产品描述" :disabled="mode === 'view'"/>
          </el-form-item>
      </el-row>
      <el-form-item label="是否交易标的物:" prop="isTransaction" :class="mode === 'view'?'custom-form-item':''">
        
          <el-checkbox disabled  v-model="form.isTransaction" label="" name="1"></el-checkbox>
       
      </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    <el-dialog  destroy-on-close :title="title+modeName" :visible.sync="openFilesDevice" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="148px">
        <el-row>
          <el-form-item label="所属层级:" prop="label" :class="mode === 'view'?'custom-form-item':''">
            <span>{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
      
        <el-row>
          <el-form-item label="设备分类名称:" prop="deviceName" :class="mode === 'view'?'custom-form-item':''">
            <span  v-if="mode === 'view'">{{form.deviceName}}</span>
            <el-input v-else v-model="form.deviceName" placeholder="请输入产品名称" :disabled="mode === 'view'"/>
          </el-form-item>
        </el-row>

        <el-row>
            <el-form-item label="设备分类编码:" prop="deviceCode" :class="mode === 'view'?'custom-form-item':''">
              <!-- <el-input v-model="form.deviceCode" placeholder="设备分类编码" disabled/> -->
              <span>{{form.deviceCode}}</span>
            </el-form-item>
        </el-row>
        <el-row>
          <el-form-item label="单位:" prop="unit" :class="mode === 'view'?'custom-form-item':''">
            <span  v-if="mode === 'view'">{{form.unit}}</span>
            <el-input v-else v-model="form.unit" placeholder="请输入产品描述" :disabled="mode === 'view'"/>
          </el-form-item>
      </el-row>
      <el-form-item label="是否交易标的物:" prop="isTransaction" :class="mode === 'view'?'custom-form-item':''">
          <el-checkbox disabled v-model="form.isTransaction" label="" name="1"></el-checkbox>
      </el-form-item>
      <el-row  v-if="mode === 'view'">
        <el-form-item label="映射资产分类名称:" prop="subjectMatterName" :class="mode === 'view'?'custom-form-item':''">
          <span >{{form.subjectMatterName}}</span>
        </el-form-item>
    </el-row>
    <el-row  v-if="mode !== 'view'">
      <el-form-item label="映射资产分类名称:" prop="subjectMatterName" :class="mode === 'view'?'custom-form-item':''">
        <el-input v-model="form.subjectMatterName" placeholder="请输入映射资产分类名称" :disabled="mode === 'view'"/>
      </el-form-item>
  </el-row>
    <el-row>
      <el-form-item label="映射资产分类编码:" prop="subjectMatterCode" c:class="mode === 'view'?'custom-form-item':''">
        <span  v-if="mode === 'view'">{{form.subjectMatterCode}}</span>
        <el-input v-else v-model="form.subjectMatterCode" placeholder="请输入映射资产分类编码" :disabled="mode === 'view'"/>
      </el-form-item>
  </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
        <el-button type="primary" @click="submitFormDevice">确 定</el-button>
        <el-button @click="openFilesDevice=false">取 消</el-button>
      </div>
    </el-dialog>
    <el-dialog destroy-on-close :title="title+modeName" :visible.sync="openFilesLabour" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="128px">
        <el-row>
          <el-form-item label="所属层级:" prop="label" :class="mode === 'view'?'custom-form-item':''">
            <span >{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
      
        <el-row>
          <el-form-item label="劳务分类名称:" prop="labourName" :class="mode === 'view'?'custom-form-item':''">
            <span  v-if="mode === 'view'">{{form.labourName}}</span>
            <el-input v-else  v-model="form.labourName" placeholder="请输入劳务分类名称" :disabled="mode === 'view'"/>
          </el-form-item>
        </el-row>

        <el-row>
            <el-form-item label="劳务分类编码:" prop="labourCode" :class="mode === 'view'?'custom-form-item':''">
              <span  >{{form.labourCode}}</span>
              <!-- <el-input v-model="form.labourCode" placeholder="请输入劳务分类编码" disabled/> -->
            </el-form-item>
        </el-row>
        <el-row>
          <el-form-item label="单位:" prop="unit" :class="mode === 'view'?'custom-form-item':''">
            <span  v-if="mode === 'view'" >{{form.unit}}</span>
            <el-input v-else  v-model="form.unit" placeholder="请输入单位" :disabled="mode === 'view'"/>
          </el-form-item>
      </el-row>
      <el-form-item label="是否交易标的物:" prop="isTransaction" :class="mode === 'view'?'custom-form-item':''">
        
          <el-checkbox disabled v-model="form.isTransaction" label="" name="1"></el-checkbox>
       
      </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
        <el-button type="primary" @click="submitFormLabour">确 定</el-button>
        <el-button @click="openFilesLabour=false">取 消</el-button>
      </div>
    </el-dialog>
    <el-dialog destroy-on-close :title="title+modeName" :visible.sync="openFilesSubcontracting" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="148px">
        <el-row>
          <el-form-item label="所属层级:" prop="label" :class="mode === 'view'?'custom-form-item':''">
            <span >{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
      
        <el-row>
          <el-form-item label="专业分包分类名称:" prop="subcontractingName" :class="mode === 'view'?'custom-form-item':''">
            <span v-if="mode === 'view'">{{form.subcontractingName}}</span>
            <el-input v-else v-model="form.subcontractingName" placeholder="请输入专业分包分类名称" :disabled="mode === 'view'"/>
          </el-form-item>
        </el-row>

        <el-row>
            <el-form-item label="专业分包分类编码:" prop="subcontractingCode" :class="mode === 'view'?'custom-form-item':''">
              <span >{{form.subcontractingCode}}</span>
              <!-- <el-input v-model="form.subcontractingCode" placeholder="请输入专业分包分类编码" disabled/> -->
            </el-form-item>
        </el-row>
        <el-row>
          <el-form-item label="单位:" prop="unit" :class="mode === 'view'?'custom-form-item':''">
            <span v-if="mode === 'view'">{{form.unit}}</span>
            <el-input v-else v-model="form.unit" placeholder="请输入单位" :disabled="mode === 'view'"/>
          </el-form-item>
      </el-row>
      <el-form-item label="是否交易标的物:" prop="isTransaction" :class="mode === 'view'?'custom-form-item':''">
        
          <el-checkbox disabled v-model="form.isTransaction" label="" name="1"></el-checkbox>
       
      </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
        <el-button type="primary" @click="submitFormSubcontracting">确 定</el-button>
        <el-button @click="openFilesSubcontracting=false">取 消</el-button>
      </div>
    </el-dialog>
    <ApprovalDetailsDialog
    :visible.sync="calibrateVisible"
    :title="radioName+'审批流程详情'"
    :activeStep="calibrateActive"
    :processInformationList="processInformationList"
    :approveLists="approveArr"
    :loading="calibrateLoading"
    @update:visible="calibrateVisible = $event"
  />
  </div>
</template>

<script>
  import {mapGetters} from "vuex";
  import { getMaterialApprove,getPermissionButton,materialApprove,archivesLoadTaskDef} from "@/api/archivese/dossier/approve";
  import {
  getProcessLogList, 
} from "@/api/procurement/manage";
import { getMaterialTypeTree,materialInitData,materialDetailsInitData,processAuditPass,getSecondaryUnit} from "@/api/archivese/dossier/materialType";
import treeMenu from "@/views/archives/dossier/materialType/components/treeMenu.vue";
import showMaterial from "@/views/archives/dossier/materialType/components/showMaterial.vue";
import showDevice from "@/views/archives/dossier/materialType/components/showDevice.vue";
import showLabour from "@/views/archives/dossier/materialType/components/showLabour.vue";
import showSubcontracting from "@/views/archives/dossier/materialType/components/showSubcontracting.vue";
import {material} from "@/views/archives/dossier/materialType/archives/material";
import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
import Drag from "@/components/Drag/index.vue";
export default {
  components: { treeMenu,showMaterial,showDevice,showSubcontracting,showLabour,Drag,ApprovalDetailsDialog},
  mixins: [material],
  name: "MaterialType",
  data() {
    return {
      isShowData:false,//是否可以录特征项特征值
      deptOptions:[],
      loading:false,
      isEditable:true,
      isShowAdd:true, //
      currentNodeKey:undefined,
      title:'材料档案',
      radioName:"材料",
      modeName:'新增',
      radioType: "0",
      baseUrlTree:'/archives/materialType/getMaterialTypeTree',
      baseUrl:'/archives/materialType',
      baseUrlItem:'/archives/materialItem',
      baseUrlDetails:'/archives/materialDetails',
      processAuditPass:'/archives/materialType/processAuditPass',//提交流程url

      calibrateVisible:false,
      calibrateActive: 1,
      processInformationList: [],
      approveArr: [],
      calibrateLoading: false,
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
        this.processAuditPass='/archives/materialType/processAuditPass'
        this.title='材料档案'
        }else  if(index==='1'){
        this.baseUrlTree='/archives/deviceType/getDeviceTypeTree'
        this.baseUrl='/archives/deviceType'
        this.baseUrlItem='/archives/deviceItem',//特征项目
        this.baseUrlDetails='/archives/deviceDetails'//具体档案
        this.processAuditPass='/archives/deviceType/processAuditPass'
        this.title='设备档案'
        }else  if(index==='2'){
        this.baseUrlTree='/archives/labourType/getLabourTypeTree'
        this.baseUrl='/archives/labourType'
        this.baseUrlItem='/archives/labourItem',//特征项目
        this.baseUrlDetails='/archives/labourDetails'//具体档案
        this.processAuditPass='/archives/labourType/processAuditPass'
        this.title='劳务档案'
        }else  if(index==='3'){
        this.baseUrlTree='/archives/subcontractingType/getSubcontractingTypeTree'
        this.baseUrl='/archives/subcontractingType'
        this.baseUrlItem='/archives/subcontractingItem',//特征项目
        this.baseUrlDetails='/archives/subcontractingDetails'//具体档案
        this.processAuditPass='/archives/subcontractingType/processAuditPass'
        this.title='专业分包档案'
        }
        }
      },
      mode:{
      handler(newVal){
        if(newVal === 'add'){
          this.modeName='新增'
        }else if(newVal === 'edit'){
          this.modeName='编辑'
        }else if(newVal === 'view'){
          this.modeName='查看'
        }
      }
    },
  },
  computed:{
    ...mapGetters(['project','org']),
   },
  created() {
    console.log(this.org+'org')
    console.log(JSON.stringify(this.project)+'project')
    this.getSecondaryUnitFn()
  },
  methods: {
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        let params = {
          businessId: this.currentNode.businessId,
          processId: this.currentNode.processId,
        };
        let res = null;
        if (this.currentNode.businessId && this.currentNode.processId) {
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

        if (this.currentNode.businessId && this.currentNode.processId) {
          const response = await getProcessLogList(params);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
    getSecondaryUnitFn(){
      getSecondaryUnit(this.org).then(response => {
      
        if(response.editable==='N'){
          this.isShowAdd=false
          this.isEditable=false
        }
        });
    },
  
    handleRadioChange(value) {
     let radio =this.radioList[value]
     this.radioName=radio.label.replace("档案","");
    },
    submitLC(){
     this.submitItem();
    },
    submitItem(data){
        this.$confirm('确定提交吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          if(JSON.stringify(this.currentNode)!=='{}' && this.currentNode){
            processAuditPass({joinId:this.currentNode.id,type:this.radioType},'/archives/materialApprove/submit').then(response => {
              this.$message.success("提交成功");
              this.$refs.customTree.refreshTree(this.currentNode.id);
              this.treeClick(this.currentNode)
            });
          }else{
            this.$message.warning("请选择类型");
          }
      
        }).catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
      },
    /** 查询材料分类列表 */
    async  getList(params) {
      this.loading = true;
      // await materialInitData({organCode:this.org},this.baseUrl);
      // await materialDetailsInitData({organCode:this.org},this.baseUrlDetails);
        getMaterialTypeTree(params,this.baseUrlTree).then(response => {
          console.log(this.radioType)
          console.log(this.baseUrlTree)
          let arr=response || []
          this.deptOptions = [...arr];
          this.currentNodeKey=this.deptOptions[0].id
          this.loading=false
        });
     
    },
    
  }
};
</script>
<style lang="scss" scoped>
  .containerMy {
    margin: 0px;padding: 0px;
  }
  .rightBox {
    /* width: calc(100vw - 320px); */
    margin:0px 0 0 10px;
    background-color: #ffffff;
  }
  .custom-form-item {
  margin-bottom: 0; /* 删除底部间距 */
}

.custom-form-item > .el-form-item__content {
  line-height: 36px; /* 设置内容的行高 */
  height: 36px; /* 确保内容高度与行高一致 */
  display: flex;
  align-items: center; /* 垂直居中对齐内容 */
}
::v-deep .el-table__body tr.current-row > td.el-table__cell, .el-table__body tr.selection-row > td.el-table__cell {
  background: rgba(6, 228, 91, 0.4) !important;;
}
  </style>