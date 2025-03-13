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
    </div>
    <!-- <div style="display: flex;"> -->
    <Drag style="background-color: #ffffff;">
      <template v-slot:left-content>
    <treeMenu :dept-options="deptOptions" :radioType="radioType" ref="customTree"
              @query="getList"  @treeClick="treeClick" @checkItem="checkItem" @addItem="addItem" @editItem="editItem" @deleteItem="deleteItem" :title="radioName+'分类列表'" :loading="loading" :currentNodeKey="currentNodeKey" :isEditable="true"></treeMenu>
    </template>
    <template v-slot:right-content>
    <div class="rightBox">
      <template v-if="isShowData">
      <showMaterial v-if="radioType==='0'" ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList"  :featureList="featureList"  :currentNode="currentNode"></showMaterial>
      <showDevice v-if="radioType==='1'" ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList"  :featureList="featureList"  :currentNode="currentNode"></showDevice>
      <showLabour v-if="radioType==='2'" ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList"  :featureList="featureList"  :currentNode="currentNode"></showLabour>
      <showSubcontracting v-if="radioType==='3'" ref="material" @detailsLis="detailsLisFn" @treeClick="treeClick" :detailsList="materialDetailsList"  :featureList="featureList"  :currentNode="currentNode"></showSubcontracting>
    </template>
      <el-empty v-else description="请选择合适的分类"></el-empty>
    </div>
     </template>
    </Drag>
  <!-- </div> -->
    <el-dialog :title="title+modeName" :visible.sync="openFiles" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="128px">
        <el-row>
          <el-form-item label="所属层级:" prop="label" >
            <span >{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
   
        <el-row>
          <el-form-item label="材料分类名称:" prop="mtrClassName" >
            <el-input v-model="form.mtrClassName" placeholder="请输入产品名称" :disabled="mode === 'view'"/>
          </el-form-item>
        </el-row>

        <el-row>
            <el-form-item label="材料分类编码:" prop="mtrClassCode">
              <span >{{form.mtrClassCode}}</span>
            </el-form-item>
        </el-row>
        <el-row >
          <el-form-item label="单位:" prop="measureUnit">
            <el-input v-model="form.measureUnit" placeholder="请输入产品描述" :disabled="mode === 'view'"/>
          </el-form-item>
      </el-row>
   
      <el-form-item label="是否交易标的物:" prop="subjectMatter">
        
          <el-checkbox disabled v-model="form.subjectMatter" label="" name="1"></el-checkbox>
       
      </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer" >
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    <el-dialog :title="title+modeName" :visible.sync="openFilesDevice" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="148px">
        <el-row>
          <el-form-item label="所属层级:" prop="label" >
            <span >{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
      
        <el-row v-if="mode !== 'view'" >
          <el-form-item label="设备分类名称:" prop="deviceClassName" >
            <el-input v-model="form.deviceClassName" placeholder="请输入产品名称" />
          </el-form-item>
        </el-row>
      
        <el-row >
          <el-form-item label="设备分类编码:" prop="deviceClassCode" >
            <span >{{form.deviceClassCode}}</span>
          </el-form-item>
        </el-row>
        <!-- <el-row>
            <el-form-item label="设备分类编码:" prop="deviceClassCode">
              <el-input v-model="form.deviceClassCode" placeholder="请输入产品描述" disabled/>
            </el-form-item>
        </el-row> -->
       
        
        <el-row >
          <el-form-item label="单位:" prop="measureUnit" >
            <el-input v-model="form.measureUnit" placeholder="请输入产品描述" />
          </el-form-item>
      </el-row>
      <el-form-item label="是否交易标的物:" prop="subjectMatter" >
        
          <el-checkbox disabled v-model="form.subjectMatter" label="" name="1"></el-checkbox>
       
      </el-form-item>
      <el-row >
        <el-form-item label="映射资产分类名称:" prop="subjectMatterName" >
          <el-input v-model="form.subjectMatterName" placeholder="请输入映射资产分类名称" :disabled="mode === 'view'"/>
        </el-form-item>
    </el-row>
 
    <el-row>
        <el-form-item  label="映射资产分类编码:" prop="subjectMatterCode" >
          <el-input v-model="form.subjectMatterCode" placeholder="请输入映射资产分类编码" :disabled="mode === 'view'"/>
        </el-form-item>
    </el-row>

      </el-form>
      <div slot="footer" class="dialog-footer" >
        <el-button type="primary" @click="submitFormDevice">确 定</el-button>
        <el-button @click="openFilesDevice=false">取 消</el-button>
      </div>
    </el-dialog>
    <el-dialog :title="title+modeName" :visible.sync="openFilesLabour" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="128px">
        <el-row > 
          <el-form-item label="所属层级:" prop="label">
            <span >{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
        <el-row >
          <el-form-item label="劳务分类名称:" prop="laborServicesClassName" >
            <el-input v-model="form.laborServicesClassName" placeholder="请输入劳务分类名称" :disabled="mode === 'view'"/>
          </el-form-item>
        </el-row>
     
        <el-row>
            <el-form-item label="劳务分类编码:" prop="laborServicesClassCode" >
              <span >{{form.laborServicesClassCode}}</span>
            </el-form-item>
        </el-row>
    
      <el-row >
        <el-form-item label="单位:" prop="measureUnit">
          <el-input v-model="form.measureUnit" placeholder="请输入单位" :disabled="mode === 'view'"/>
        </el-form-item>
    </el-row>
      <el-form-item label="是否交易标的物:" prop="subjectMatter"  >
        
          <el-checkbox disabled v-model="form.subjectMatter" label="" name="1"></el-checkbox>
       
      </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer" >
        <el-button type="primary" @click="submitFormLabour">确 定</el-button>
        <el-button @click="openFilesLabour=false">取 消</el-button>
      </div>
    </el-dialog>
    <el-dialog :title="title+modeName" :visible.sync="openFilesSubcontracting" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="148px">
        <el-row>
          <el-form-item label="所属层级:" prop="label"  >
            <span >{{form.belongingLevel}}</span>
          </el-form-item>
        </el-row>
      
     

        <el-row >
          <el-form-item label="专业分包分类名称:" prop="majorSubcontractingClassName"  >
            <el-input v-model="form.majorSubcontractingClassName" placeholder="请输入专业分包分类名称" :disabled="mode === 'view'"/>
          </el-form-item>
        </el-row>

        <el-row >
            <el-form-item label="专业分包分类编码:" prop="majorSubcontractingClassCode"  >
              <span >{{form.majorSubcontractingClassCode}}</span>
            </el-form-item>
        </el-row>
     
      <el-row >
        <el-form-item label="单位:" prop="measureUnit"  >
          <el-input v-model="form.measureUnit" placeholder="请输入单位" :disabled="mode === 'view'"/>
        </el-form-item>
    </el-row>
      <el-form-item label="是否交易标的物:" prop="subjectMatter" >
        
          <el-checkbox disabled v-model="form.subjectMatter" label="" name="1"></el-checkbox>
       
      </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer" >
        <el-button type="primary" @click="submitFormSubcontracting">确 定</el-button>
        <el-button @click="openFilesSubcontracting=false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
  import {mapGetters} from "vuex";
import { getMaterialTypeTree,materialInitData,materialDetailsInitData} from "@/api/archivese/dossier/materialType";
import treeMenu from "@/views/archives/dossier/materialType/components/treeMenu.vue";
import showMaterial from "@/views/archives/main/mtrClass/components/showMaterial.vue";
import showDevice from "@/views/archives/main/mtrClass/components/showDevice.vue";
import showLabour from "@/views/archives/main/mtrClass/components/showLabour.vue";
import showSubcontracting from "@/views/archives/main/mtrClass/components/showSubcontracting.vue";
import Drag from "@/components/Drag/index.vue";
import {material} from "@/views/archives/main/mtrClass/archives/mtrClass";
export default {
  components: { treeMenu,showMaterial,showDevice,showSubcontracting,showLabour,Drag},
  mixins: [material],
  name: "MaterialType",
  data() {
    return {
      isShowData:false,//是否可以录特征项特征值
      deptOptions:[],
      loading:false,
      currentNodeKey:undefined,
      isShow:false,
      title:'材料档案',
      modeName:'新增',
      radioName:"材料",
      radioType: "0",
      baseUrlTree:'/archives/mtrClass/getMtrClassTree',
      baseUrl:'/archives/mtrClass',
      baseUrlItem:'/archives/mtrFeature',
      baseUrlDetails:'/archives/mtrArchives',
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
        this.baseUrlTree='/archives/mtrClass/getMtrClassTree'
        this.baseUrl='/archives/mtrClass'
        this.baseUrlItem='/archives/mtrFeature'
        this.baseUrlDetails='/archives/mtrArchives'
        this.title='材料档案'
        }else  if(index==='1'){
        this.baseUrlTree='/archives/deviceClass/getDeviceClassTree'
        this.baseUrl='/archives/deviceClass'
        this.baseUrlItem='/archives/deviceFeature',//特征项目
        this.baseUrlDetails='/archives/deviceArchives'//具体档案
        this.title='设备档案'
        }else  if(index==='2'){
        this.baseUrlTree='/archives/laborClass/getLaborServicesClassTree'
        this.baseUrl='/archives/laborClass'
        this.baseUrlItem='/archives/laborFeature',//特征项目
        this.baseUrlDetails='/archives/laborArchives'//具体档案
        this.title='劳务档案'
        }else  if(index==='3'){
        this.baseUrlTree='/archives/majorClass/getMajorSubcontractingClassTree'
        this.baseUrl='/archives/majorClass'
        this.baseUrlItem='/archives/majorFeature',//特征项目
        this.baseUrlDetails='/archives/majorArchives'//具体档案
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
  },
  methods: {
    handleRadioChange(value) {
     let radio =this.radioList[value]
     this.radioName=radio.label.replace("档案","");
    },
 
    /** 查询材料分类列表 */
    async  getList(params) {
      this.loading = true;
        getMaterialTypeTree(params,this.baseUrlTree).then(response => {
          this.deptOptions=response || []
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
    /* width: calc(100vw - 270px); */
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