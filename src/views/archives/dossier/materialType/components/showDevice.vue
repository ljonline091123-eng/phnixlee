<template>
    <div class="container">
      <div >
        <el-radio-group
        style="margin:10px 0 0 10px;"
        v-model="radioTypeM"
        size="small"
      >
          <el-radio-button
          :label="dict.value"
          :name="dict.value" 
          v-for="dict in radioListM"
          :key="dict.value"
          >{{ dict.label }}</el-radio-button>
      </el-radio-group>
      </div>
      <div v-if="radioTypeM=='1M'"  style="display: flex;width: 99%;">
      <div  style="width: 50%;">
        <commonTitle>
          <div slot="name">
            {{currentNode.label}}{{radioTypeM=='1M'?' 特征项':''}}
          </div>
          <div  v-if="isShowAdd"  slot="right">
            <el-button size="small" type="primary" icon="el-icon-plus"  @click="featureAdd" >新增</el-button>
          </div>
        </commonTitle>
        <el-table  :data="featureList" stripe @row-click="clickRow"  :highlight-current-row="true"  style="width: 99%;margin-top: 8px;">
          <el-table-column width="150" label="特征项编号" align="center" prop="itemCode" show-overflow-tooltip/>
          <el-table-column label="特征项名称" align="center" prop="itemName" show-overflow-tooltip/>
          <el-table-column
          v-if="isShowAdd" 
            label="操作"
            align="center"
            width="150"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope" >
              <el-tooltip v-if="scope.row.state!='1' && scope.row.state!='3'" content="编辑" placement="top" effect="dark">
                <i
                  class=" el-icon-edit " style="color: #2b4acb;"
                  @click.stop="handleEdit(scope.row)"
                />
              </el-tooltip>
              <el-tooltip v-else content="查看" placement="top" effect="dark" >
                <i
                  class=" el-icon-view " style="color: #2b4acb;"
                  @click.stop="handleCheck(scope.row)"
                />
              </el-tooltip>
              <el-tooltip v-if="scope.row.state!='1' && scope.row.state!='3'" content="删除" placement="top" effect="dark">
                <i
                  class="el-icon-delete" style="color: #ff5454;margin-left: 12px;"
                  @click.stop="handleDelete(scope.row)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
          v-if="isMain" 
          label="操作"
          align="center"
          width="200"
          class-name="small-padding fixed-width" 
        >
        <template slot-scope="scope" v-if="scope.row.isMain!=='Y'">
        <el-tooltip v-if="scope.row.mainId==='0'" content="新增至主库" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-circle-plus-outline" style="color: #2b4acb;"
            @click.stop="mainMaterialAdd(scope.row,'material')"
          />
        </el-tooltip>
        <el-tooltip v-if="scope.row.mainId==='0'" content="关联至主库" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-document-copy " style="color: #2b4acb;margin-left: 6px;"
            @click.stop="mainMaterialEdit(scope.row,'material')"
          />
        </el-tooltip>
        <el-tooltip content="取消关联" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-circle-close" style="color: #ff5454;margin-left: 6px;"
            @click.stop="mainMaterialDelete(scope.row,'material')"
          />
        </el-tooltip>
        </template>
        </el-table-column>
        </el-table>
      </div>
      <div style="width: 50%;border-left: solid 1px #E7E7E7;">
        <commonTitle>
          <div slot="name">
            {{currentNode.label}}{{radioTypeM=='1M'?' 特征值':''}}
          </div>
          <div  v-if="isShowAdd"  slot="right">
            <el-button size="small" type="primary" icon="el-icon-plus"  @click="eigenAdd" >新增</el-button>
          </div>
        </commonTitle>
        <el-table  :data="eigenvalueList" stripe  :highlight-current-row="true"  style="width: 99%; margin-left: 1%;margin-top: 8px;">
          <el-table-column width="150" label="特征值编号" align="center" prop="eigenvalueCode" show-overflow-tooltip/>
          <el-table-column label="特征值名称" align="center" prop="eigenvalueName" show-overflow-tooltip/>
          <el-table-column
          v-if="isShowAdd" 
            label="操作"
            align="center"
            width="150"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope">
              <el-tooltip  v-if="scope.row.state!='1' && scope.row.state!='3'" content="编辑" placement="top" effect="dark">
                <i
                  class=" el-icon-edit " style="color: #2b4acb;"
                  @click.stop="handleEditEigen(scope.row)"
                />
              </el-tooltip>
              <el-tooltip v-else content="查看" placement="top" effect="dark" >
                <i
                  class=" el-icon-view " style="color: #2b4acb;"
                  @click.stop="handleCheckEigen(scope.row)"
                />
              </el-tooltip>
              <el-tooltip  v-if="scope.row.state!='1' && scope.row.state!='3'" content="删除" placement="top" effect="dark">
                <i
                  class="el-icon-delete" style="color: #ff5454;margin-left: 12px;"
                  @click.stop="handleDeleteEigen(scope.row)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
          v-if="isMain" 
          label="操作"
          align="center"
          width="200"
          class-name="small-padding fixed-width"
        >
        <template slot-scope="scope" v-if="scope.row.isMain!=='Y'">
        <el-tooltip v-if="scope.row.mainId==='0'" content="新增至主库" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-circle-plus-outline" style="color: #2b4acb;"
            @click.stop="mainMaterialAdd(scope.row,'eigenvalue')"
          />
        </el-tooltip>
        <el-tooltip v-if="scope.row.mainId==='0'" content="关联至主库" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-document-copy " style="color: #2b4acb;margin-left: 6px;"
            @click.stop="mainMaterialEdit(scope.row,'eigenvalue')"
          />
        </el-tooltip>
        <el-tooltip content="取消关联" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-circle-close" style="color: #ff5454;margin-left: 6px;"
            @click.stop="mainMaterialDelete(scope.row,'eigenvalue')"
          />
        </el-tooltip>
        </template>
  </el-table-column>
        </el-table>
   
      </div>
      </div>
      <div v-if="radioTypeM=='2M'"  style="width: 99%;">
        <commonTitle>
          <div slot="name">
            {{currentNode.label}}{{radioTypeM=='1M'?'':'具体档案'}}
          </div>
          <div  v-if="isShowAdd"  slot="right">
            <el-button size="small" type="primary" icon="el-icon-plus"  @click="detailAdd" >新增明细</el-button>
          </div>
        </commonTitle>
        <el-table  :data="detailsList" stripe   :highlight-current-row="true"  style="width: 99%;margin-top: 8px;">
          <el-table-column width="150" label="设备编号" align="center" prop="deviceCode" show-overflow-tooltip/>
          <el-table-column label="设备名称" align="center" prop="deviceName" show-overflow-tooltip/>
          <el-table-column label="所属分类" align="center" prop="typeName" show-overflow-tooltip/>
          <el-table-column label="特征项" align="center" prop="feature" show-overflow-tooltip/>
          <el-table-column label="单位" align="center" prop="unit" show-overflow-tooltip/>
          <el-table-column
            label="操作"
            v-if="isShowAdd" 
            align="center"
            width="150"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope" >
              <el-tooltip v-if="scope.row.state!='1' && scope.row.state!='3'" content="编辑" placement="top" effect="dark">
                <i
                  class=" el-icon-edit " style="color: #2b4acb;"
                  @click.stop="handleEditDetails(scope.row)"
                />
              </el-tooltip>
              <el-tooltip v-else content="查看" placement="top" effect="dark" >
                <i
                  class=" el-icon-view " style="color: #2b4acb;"
                  @click.stop="handleCheckDetails(scope.row)"
                />
              </el-tooltip>
              <el-tooltip v-if="scope.row.state!='1' && scope.row.state!='3'" content="删除" placement="top" effect="dark">
                <i
                  class="el-icon-delete" style="color: #ff5454;margin-left: 12px;"
                  @click.stop="handleDeleteDetails(scope.row)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
          v-if="isMain" 
          label="操作"
          align="center"
          width="200"
          class-name="small-padding fixed-width"
        >
        <template slot-scope="scope" v-if="scope.row.isMain!=='Y'">
        <el-tooltip v-if="scope.row.mainId==='0'" content="新增至主库" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-circle-plus-outline" style="color: #2b4acb;"
            @click.stop="mainMaterialAdd(scope.row,'details')"
          />
        </el-tooltip>
        <el-tooltip v-if="scope.row.mainId==='0'" content="关联至主库" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-document-copy " style="color: #2b4acb;margin-left: 6px;"
            @click.stop="mainMaterialEdit(scope.row,'details')"
          />
        </el-tooltip>
        <el-tooltip content="取消关联" placement="top" effect="dark">
          <i
            class="small-operation-btn el-icon-circle-close" style="color: #ff5454;margin-left: 6px;"
            @click.stop="mainMaterialDelete(scope.row,'details')"
          />
        </el-tooltip>
      </template>
  </el-table-column>
        </el-table>
        <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getDetailsList"
      />
      </div>
      <el-dialog destroy-on-close :title="mode==='add'?'新增特征项': mode==='view'?'查看特征项':'编辑特征项'" :visible.sync="openFeature" width="500px" append-to-body>
        <el-form ref="form" :model="form" :rules="rules" label-width="128px">
         
          <el-row>
            <el-form-item label="材料特征项名称:" prop="itemName" >
              <span v-if="mode === 'view'">{{form.itemName}}</span>
              <el-input v-else  v-model="form.itemName" placeholder="请输入" />
            </el-form-item>
          </el-row>
  
          <el-row>
              <el-form-item label="材料特征项编号:" prop="itemCode">
                <span v-if="mode === 'view'">{{form.itemCode}}</span>
                <el-input v-else  v-model="form.itemCode" placeholder="请输入" />
              </el-form-item>
          </el-row>
         
      
        </el-form>
        <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
          <el-button type="primary" @click="featureSubmitForm">确 定</el-button>
          <el-button @click="openFeature=false">取 消</el-button>
        </div>
      </el-dialog>
      <el-dialog destroy-on-close :title="modeZ==='add'?'新增特征值': modeZ==='view'?'查看特征值':'编辑特征值'" :visible.sync="openEigenValue" width="500px" append-to-body>
        <el-form ref="formZ" :model="formZ" :rules="rulesZ" label-width="128px">
         
          <el-row>
            <el-form-item label="材料特征值名称:" prop="eigenvalueName" >
              <span v-if="modeZ === 'view'">{{formZ.eigenvalueName}}</span>
              <el-input v-else v-model="formZ.eigenvalueName" placeholder="请输入" />
            </el-form-item>
          </el-row>
  
          <el-row>
              <el-form-item label="材料特征值编号:" prop="eigenvalueCode">
                <span v-if="modeZ === 'view'">{{formZ.eigenvalueCode}}</span>
                <el-input v-else v-model="formZ.eigenvalueCode" placeholder="请输入" />
              </el-form-item>
          </el-row>
         
      
        </el-form>
        <div slot="footer" class="dialog-footer" v-if="modeZ !== 'view'">
          <el-button type="primary" @click="eigenSubmitForm">确 定</el-button>
          <el-button @click="openEigenValue=false">取 消</el-button>
        </div>
      </el-dialog>
      <el-dialog destroy-on-close :title="modeDetail==='add'?'新增明细': modeDetail==='view'?'查看明细':'编辑明细'" :visible.sync="openDetail" width="500px" append-to-body>
        <el-form ref="formDetail" :model="formDetail" :rules="rulesDetail" label-width="128px">
         
          <el-row>
            <el-form-item label="设备编号:" prop="deviceCode" >
              <span v-if="modeDetail === 'view'">{{formDetail.deviceCode}}</span>
              <el-input v-else v-model="formDetail.deviceCode" placeholder="请输入" />
            </el-form-item>
          </el-row>
  
          <el-row>
              <el-form-item label="设备名称:" prop="deviceName">
                <span v-if="modeDetail === 'view'">{{formDetail.deviceName}}</span>
                <el-input v-else v-model="formDetail.deviceName" placeholder="请输入" />
              </el-form-item>
          </el-row>
          <el-row>
            <el-form-item label="特征项:" prop="feature">
              <span v-if="modeDetail === 'view'">{{formDetail.feature}}</span>
              <el-input v-else v-model="formDetail.feature" placeholder="请输入" />
            </el-form-item>
        </el-row>
        <el-row>
          <el-form-item label="单位:" prop="unit">
            <span v-if="modeDetail === 'view'">{{formDetail.unit}}</span>
            <el-input v-else v-model="formDetail.unit" placeholder="请输入" />
          </el-form-item>
      </el-row>
      
        </el-form>
        <div slot="footer" class="dialog-footer" v-if="modeDetail !== 'view'">
          <el-button type="primary" @click="detailSubmitForm">确 定</el-button>
          <el-button @click="openDetail=false">取 消</el-button>
        </div>
      </el-dialog>
      </div>
  </template>
  <script>
    import commonTitle from "@/views/archives/dossier/materialType/components/commonTitle.vue";
    import {addDeviceItem,editDeviceItem,getDeviceItem,delDeviceItem } from "@/api/archivese/dossier/deviceItem";
    import {materialEigenvalueInitData,getMaterialEigenvalueList,materialEigenvalueAdd,materialEigenvalueEdit,getEigenvalue,materialEigenvalueDelete } from "@/api/archivese/dossier/deviceEigenvalue"
    import {materialInitData} from "@/api/archivese/dossier/materialType";
    import {addMaterialDetails , updateMaterialDetails,getMaterialDetails,delMaterialDetails } from "@/api/archivese/dossier/deviceDetails";
  import {mapGetters} from "vuex";
  export default {
    components: { commonTitle},
    name: "treeMenu",
    data() {
      return {
          // 查询参数
          queryParams: {
            pageNum: 1,
            pageSize: 10,
          },
          total:0,
        openFeature:false,
        radioTypeM: "1M",
        form:{},
        mode:'add',//特征项
      radioListM: [
        {
          value: '1M',
          label: "特征值特征项",
        },
        {
          value: '2M',
          label: "具体档案",
        },
      ],
      //特征值
      currentFeature:{},//当前选中的特征项
      rules: {
        itemName: [{required: true, message: '请输入', trigger: 'blur'}],
        itemCode: [{required: true, message: '请输入', trigger: 'change'}],
          },
      formZ:{},
        modeZ:'add',//特征值
        openEigenValue:false,
        eigenvalueList:[],
      rulesZ: {
        eigenvalueName: [{required: true, message: '请输入', trigger: 'blur'}],
        eigenvalueCode: [{required: true, message: '请输入', trigger: 'change'}],
      },

      //明细
      rulesDetail: {
        deviceCode: [{required: true, message: '请输入', trigger: 'blur'}],
        deviceName: [{required: true, message: '请输入', trigger: 'blur'}],
          },
      formDetail:{},
        modeDetail:'add',//特征值
        openDetail:false,
   
      };
    },
    computed:{
    ...mapGetters(['project','org']),
   },
  props: {
    isShowAdd: {
      type: Boolean,
      required: true,
    },
        //是否可操作主库
    isMain: {
      type: Boolean,
      default: false,
    },
    // 表格数据
    featureList: {
      type: Array,
      default: () => [],
    },
    detailsList:{
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    radioType: {
      type: String,
      default: "1",
    },
    orgId: {
      type: String,
      default: "",
    },
    queryType: {
      type: String,
      default: "",
    },
    title: {
      type: String,
      default: "",
    },
    currentNode: {
      type: Object,
      default: {},
    },

},
watch:{
  featureList: {
        handler(newVal,oldVal) {
          if(!newVal || JSON.stringify(newVal) !== JSON.stringify(oldVal)){
            this.eigenvalueList=[]
            this.currentFeature={}
          }else{
            console.log(JSON.stringify(this.currentFeature))
            if(this.currentFeature.id){
              this.getMaterialEigenvalueListFn(this.currentFeature)
            }
           
          }
        }
      }
  },
    created() {
    // this.materialEigenvalueInitDataFn()
 
    },
    mounted() {
     
      
	},
    methods: {
      setTotal(total){
        this.total=total
      },
      getDetailsList(){
        console.log("=================================================================")
        this.$emit('detailsLis', this.queryParams)
      },
       // 点击按钮新增至主库
    mainMaterialAdd(data, type) {
          this.$emit('mainMaterialAdd', data,type)
        },
     // 点击按钮关联主库
     mainMaterialEdit(data, type) {
      this.$emit('mainMaterialEdit', data, type)
    },
     // 点击按钮删除关联
     mainMaterialDelete(data, type) {
      this.$emit('mainMaterialDelete', data, type)
    },
      clickRow(row) {
        this.currentFeature=row || {}
        this.getMaterialEigenvalueListFn(row)
    },
    getMaterialEigenvalueListFn(row){
      if(JSON.stringify(row)!=='{}'){
      let params={organCode:this.orgId!==""?this.orgId:this.org,itemId:row.id}
      if(this.wfBatch!==""){
        params.wfBatch=this.wfBatch
      }
      //queryType 档案表示查询
      if(this.queryType!==""){
        params.queryType=this.queryType
      }
      getMaterialEigenvalueList(params).then(response => {
          console.log(JSON.stringify(response.data))
          let rData  = response.rows || [];
          this.eigenvalueList=rData
          });
        }
    },
      //初始化
      async materialEigenvalueInitDataFn(){
        await materialInitData({organCode:this.org},'/archives/deviceType')
      },
     
      handleEdit(data){
        getDeviceItem(data.id).then(response => {
          console.log(JSON.stringify(response.data))
          this.form={...response.data}
          this.openFeature=true
          this.mode='edit'
          });
      },
      handleCheck(data){
        getDeviceItem(data.id).then(response => {
          console.log(JSON.stringify(response.data))
          this.form={...response.data}
          this.openFeature=true
          this.mode='view'
          });
      },
     
      handleDelete(data){
        this.$confirm('确定删除吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
       this.materialDeleteFn(data)
        }).catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
      },
      materialDeleteFn(data){
        delDeviceItem(data.id).then(response => {
          // 重新加载数据
          this.$emit('treeClick', this.currentNode)
          // 提示
          this.$message({
            type: 'success',
            message: '删除成功!'
          })
          // 置空特征值数据
          this.eigenvalueList=[]
      });},
      async  featureSubmitForm(){
        const valid = await this.$refs.form.validate();
        if (valid) {
          const fetchApi = this.mode==='add' ? addDeviceItem : editDeviceItem;
          this.form.organCode=this.org
          this.form.typeId=this.currentNode.id
          fetchApi(this.form).then(() => {
            this.$message.success("操作成功");
            this.openFeature = false;
            this.$emit('treeClick', this.currentNode)
            this.form={}
              // 重新加载数据
              if(this.currentFeature && this.mode==='edit'){
                this.getMaterialEigenvalueListFn(this.currentFeature)
              }
          }).catch(() => {
            // this.$message.error("操作失败");
            console.log("特征项新增操作失败")
          });
        }
      },
      cancel() {
        this.openFeature = false;
        this.form={};
      },
      //明细
      detailAdd(){
        console.log(JSON.stringify(this.currentNode))
        if(JSON.stringify(this.currentNode)!=='{}' && this.currentNode.children.length==0 ){
        this.formDetail={};
        this.modeDetail='add'
        this.openDetail=true
        }else{
            this.$message.warning("请先选择材料档案类型");

          }
      },
      async  detailSubmitForm(){
        const valid = await this.$refs.formDetail.validate();
        if (valid) {
          const fetchApi = this.modeDetail==='add' ? addMaterialDetails : updateMaterialDetails;
          this.formDetail.organCode=this.org
          this.formDetail.typeId=this.currentNode.id
          this.formDetail.typeName=this.currentNode.label
          console.log(JSON.stringify(this.currentNode))
          fetchApi(this.formDetail).then(() => {
            this.$message.success("操作成功");
            this.openDetail = false;
            this.formDetail={}
             // 重新加载数据
             this.$emit('treeClick', this.currentNode)
          }).catch(() => {
            // this.$message.error("操作失败");
            console.log("详情新增操作失败")
          });
        }
      },
      //明细详情
      handleEditDetails(data){
        getMaterialDetails(data.id).then(response => {
          console.log(JSON.stringify(response.data))
          this.formDetail={...response.data}
          this.openDetail=true
          this.modeDetail='edit'
          });
      },
      handleCheckDetails(data){
        getMaterialDetails(data.id).then(response => {
          console.log(JSON.stringify(response.data))
          this.formDetail={...response.data}
          this.openDetail=true
          this.modeDetail='view'
          });
      },
      handleDeleteDetails(data){
        this.$confirm('确定删除吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
       this.delMaterialDetailsFn(data)
        }).catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
      },
      delMaterialDetailsFn(data){
        delMaterialDetails(data.id).then(response => {
          // 重新加载数据
          this.$emit('treeClick', this.currentNode)
          // 提示
          this.$message({
            type: 'success',
            message: '删除成功!'
          })
      });},
       //特征项新增
      featureAdd(){
        console.log(JSON.stringify(this.currentNode))
        if(JSON.stringify(this.currentNode)!=='{}' && this.currentNode.children.length==0 ){
        this.form={};
        this.mode='add'
        this.openFeature=true
        }else{
            this.$message.warning("请先选择材料档案类型");

          }
      },
      //特征值获取编辑详情
      handleEditEigen(data){
        getEigenvalue(data.id).then(response => {
          console.log(JSON.stringify(response.data))
          this.formZ={...response.data}
          this.openEigenValue=true
          this.modeZ='edit'
          });
      },
      handleCheckEigen(data){
        getEigenvalue(data.id).then(response => {
          console.log(JSON.stringify(response.data))
          this.formZ={...response.data}
          this.openEigenValue=true
          this.modeZ='view'
          });
      },
      //特征值新增
      eigenAdd(){
        if(JSON.stringify(this.currentNode)!=='{}' && JSON.stringify(this.currentFeature)!=='{}'){
            this.formZ={};
            this.modeZ='add'
            this.openEigenValue=true
          }else{
            this.$message.warning("请先选择特征项");

          }
      },
      
      async  eigenSubmitForm(){
        const valid = await this.$refs.formZ.validate();
        if (valid) {
          const fetchApi = this.modeZ==='add' ? materialEigenvalueAdd : materialEigenvalueEdit;
          this.formZ.organCode=this.org
          this.formZ.typeId=this.currentNode.id
          this.formZ.itemId=this.currentFeature.id
          fetchApi(this.formZ).then(() => {
            this.$message.success("操作成功");
            this.openEigenValue = false;
            this.formZ={}
             // 重新加载数据
            this.getMaterialEigenvalueListFn(this.currentFeature)
          }).catch(() => {
            // this.$message.error("操作失败");
            console.log("特征值新增操作失败")
          });
        }
      },

      handleDeleteEigen(data){
        this.$confirm('确定删除吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
       this.materialDeleteEigenFn(data)
        }).catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除'
          })
        })
      },
      materialDeleteEigenFn(data){
        materialEigenvalueDelete(data.id).then(response => {
          // 重新加载数据
          this.getMaterialEigenvalueListFn(this.currentFeature)
          // 提示
          this.$message({
            type: 'success',
            message: '删除成功!'
          })
      });},
    }
  };
  </script>
  <style lang="scss" scoped>
   .container {
    width: 100%;margin:0px 0 0 10px;
      background-color: #ffffff;
      
   }
  </style>
  