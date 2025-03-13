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
          {{radioTypeM=='1M'?' 特征项':''}}
          </div>
          <div v-if="isShowAdd" slot="right">
            <el-button size="small" type="primary" icon="el-icon-plus"  @click="featureAdd" >新增</el-button>
          </div>
        </commonTitle>
        <el-table  :data="featureList" :highlight-current-row="true" stripe @row-click="clickRow"  style="width: 99%;margin-top: 8px;">
          <el-table-column width="150" label="特征项编号" align="center" prop="featureCode" show-overflow-tooltip/>
          <el-table-column label="特征项名称" align="center" prop="featureName" show-overflow-tooltip/>
          <el-table-column
          v-if="isShowAdd" 
            label="操作"
            align="center"
            width="150"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope">
              <el-tooltip content="编辑" placement="top" effect="dark">
                <i
                  class=" el-icon-edit " style="color: #2b4acb;"
                  @click.stop="handleEdit(scope.row)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top" effect="dark">
                <i
                  class="el-icon-delete" style="color: #ff5454;margin-left: 12px;"
                  @click.stop="handleDelete(scope.row)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div style="width: 50%;border-left: solid 1px #E7E7E7;">
        <commonTitle>
          <div slot="name">
            特征值
          </div>
          <div v-if="isShowAdd" slot="right">
            <el-button size="small" type="primary" icon="el-icon-plus"  @click="eigenAdd" >新增</el-button>
          </div>
        </commonTitle>
        <el-table :highlight-current-row="true" :data="eigenvalueList" @row-click="clickRowMainTZZ"  stripe style="width: 99%; margin-left: 1%;margin-top: 8px;">
          <el-table-column width="150" label="特征值编号" align="center" prop="featureValueCode" show-overflow-tooltip/>
          <el-table-column label="特征值名称" align="center" prop="featureValueName" show-overflow-tooltip/>
          <el-table-column
            label="操作"
            v-if="isShowAdd" 
            align="center"
            width="150"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope">
              <el-tooltip content="编辑" placement="top" effect="dark">
                <i
                  class=" el-icon-edit " style="color: #2b4acb;"
                  @click.stop="handleEditEigen(scope.row)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top" effect="dark">
                <i
                  class="el-icon-delete" style="color: #ff5454;margin-left: 12px;"
                  @click.stop="handleDeleteEigen(scope.row)"
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
            {{currentNode.label}}
          </div>
          <div v-if="isShowAdd" slot="right">
            <el-button size="small" type="primary" icon="el-icon-plus"  @click="detailAdd" >新增明细</el-button>
          </div>
        </commonTitle>
        <el-table :highlight-current-row="true"  :data="detailsList" stripe  @row-click="clickRowMain"  style="width: 99%;margin-top: 8px;">
          <el-table-column width="150" label="劳务编号" align="center" prop="laborServicesCode" show-overflow-tooltip/>
          <el-table-column label="劳务名称" align="center" prop="laborServicesName" show-overflow-tooltip/>
          <el-table-column label="所属分类" align="center" prop="laborServicesClassName" show-overflow-tooltip/>
          <el-table-column label="特征项及特征值" align="center" prop="feature" show-overflow-tooltip/>
          <el-table-column label="单位" align="center" prop="measureUnit" show-overflow-tooltip/>
          <el-table-column label="规格" align="center" prop="specs" show-overflow-tooltip/>
          <el-table-column label="计量规则" align="center" prop="metrologicalRules" show-overflow-tooltip/>
          <el-table-column label="基本工作内容" align="center" prop="basicJob" show-overflow-tooltip/>
          <el-table-column
            label="操作"
            v-if="isShowAdd" 
            align="center"
            width="150"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope">
              <el-tooltip content="编辑" placement="top" effect="dark">
                <i
                  class=" el-icon-edit " style="color: #2b4acb;"
                  @click.stop="handleEditDetails(scope.row)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top" effect="dark">
                <i
                  class="el-icon-delete" style="color: #ff5454;margin-left: 12px;"
                  @click.stop="handleDeleteDetails(scope.row)"
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
      <el-dialog destroy-on-close :title="mode==='add'?'新增特征项':'编辑特征项'" :visible.sync="openFeature" width="500px" append-to-body>
        <el-form ref="form" :model="form" :rules="rules" label-width="128px">
         
          <el-row>
            <el-form-item label="劳务特征项名称:" prop="featureName" >
              <el-input v-model="form.featureName" placeholder="请输入" />
            </el-form-item>
          </el-row>
  
          <el-row>
              <el-form-item label="劳务特征项编号:" prop="featureCode">
                <el-input v-model="form.featureCode" placeholder="请输入" />
              </el-form-item>
          </el-row>
         
      
        </el-form>
        <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
          <el-button type="primary" @click="featureSubmitForm">确 定</el-button>
          <el-button @click="openFeature=false">取 消</el-button>
        </div>
      </el-dialog>
      <el-dialog destroy-on-close :title="modeZ==='add'?'新增特征值':'编辑特征值'" :visible.sync="openEigenValue" width="500px" append-to-body>
        <el-form ref="formZ" :model="formZ" :rules="rulesZ" label-width="128px">
         
          <el-row>
            <el-form-item label="劳务特征值名称:" prop="featureValueName" >
              <el-input v-model="formZ.featureValueName" placeholder="请输入" />
            </el-form-item>
          </el-row>
  
          <el-row>
              <el-form-item label="劳务特征值编号:" prop="featureValueCode">
                <el-input v-model="formZ.featureValueCode" placeholder="请输入" />
              </el-form-item>
          </el-row>
         
      
        </el-form>
        <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
          <el-button type="primary" @click="eigenSubmitForm">确 定</el-button>
          <el-button @click="openEigenValue=false">取 消</el-button>
        </div>
      </el-dialog>
      <el-dialog destroy-on-close :title="modeDetail==='add'?'新增明细':'编辑明细'" :visible.sync="openDetail" width="500px" append-to-body>
        <el-form ref="formDetail" :model="formDetail" :rules="rulesDetail" label-width="128px">
         
          <el-row>
            <el-form-item label="劳务编号:" prop="laborServicesCode" >
              <el-input disabled v-model="formDetail.laborServicesCode" placeholder="请输入"  type="textarea"
              maxlength="500"
              show-word-limit/>
            </el-form-item>
          </el-row>
  
          <el-row>
              <el-form-item label="劳务名称:" prop="laborServicesName">
                <el-input v-model="formDetail.laborServicesName" placeholder="请输入" />
              </el-form-item>
          </el-row>
          <el-row>
            <el-form-item label="特征项及特征值:" prop="feature">
              <el-input v-model="formDetail.feature" placeholder="请输入" />
            </el-form-item>
        </el-row>
        <el-row>
          <el-form-item label="单位:" prop="measureUnit">
            <el-input v-model="formDetail.measureUnit" placeholder="请输入" />
          </el-form-item>
      </el-row>
      <el-row>
        <el-form-item label="规格:" prop="specs">
          <el-input v-model="formDetail.specs" placeholder="请输入" />
        </el-form-item>
    </el-row>
      <el-row>
        <el-form-item label="计量规则:" prop="metrologicalRules">
          <el-input v-model="formDetail.metrologicalRules" placeholder="请输入" />
        </el-form-item>
    </el-row>
    <el-row>
      <el-form-item label="基本工作内容:" prop="basicJob">
        <el-input v-model="formDetail.basicJob" placeholder="请输入"  type="textarea"
        maxlength="2000"
        show-word-limit/>
      </el-form-item>
  </el-row>



        </el-form>
        <div slot="footer" class="dialog-footer" v-if="mode !== 'view'">
          <el-button type="primary" @click="detailSubmitForm">确 定</el-button>
          <el-button @click="openDetail=false">取 消</el-button>
        </div>
      </el-dialog>
      </div>
  </template>
  <script>
    import commonTitle from "@/views/archives/dossier/materialType/components/commonTitle.vue";
    import {materialItemEdit,materialItemAdd,getMaterialItem,materialItemDelete } from "@/api/archivese/main/laborFeature";
    import {materialEigenvalueInitData,getMaterialEigenvalueList,materialEigenvalueAdd,materialEigenvalueEdit,getEigenvalue,materialEigenvalueDelete } from "@/api/archivese/main/laborValue"
    import {materialInitData} from "@/api/archivese/dossier/materialType";
    import {addMaterialDetails , updateMaterialDetails,getMaterialDetails,delMaterialDetails,initDetails } from "@/api/archivese/main/laborArchives";
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
        radioTypeM: "2M",
        form:{},
        mode:'add',//特征项
      radioListM: [
      {
          value: '2M',
          label: "具体档案",
        },
        {
          value: '1M',
          label: "特征值特征项",
        },
    
      ],
      //特征值
      currentFeature:{},//当前选中的特征项
      rules: {
        featureName: [{required: true, message: '请输入', trigger: 'blur'}],
        featureCode: [{required: true, message: '请输入', trigger: 'blur'}],
          },
      formZ:{},
        modeZ:'add',//特征值
        openEigenValue:false,
        eigenvalueList:[],
      rulesZ: {
        featureValueName: [{required: true, message: '请输入', trigger: 'blur'}],
        featureValueCode: [{required: true, message: '请输入', trigger: 'blur'}],
      },
      formDetail:{},
        modeDetail:'add',//特征值
        openDetail:false,
        //明细
      rulesDetail: {
        laborServicesCode: [{required: true, message: '请输入', trigger: 'blur'}],
        laborServicesName: [{required: true, message: '请输入', trigger: 'blur'}],
      },
      };
    },
    computed:{
    ...mapGetters(['project','org']),
   },
  props: {
      //确认操作类型
    modeMian: {
      type: String,
      default: "",
    },
    isShowType: {
      type: String,
      default: "",
    },
    //是否可新增编辑
     isShowAdd: {
      type: Boolean,
      default: true,
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
          if(!newVal || newVal !== oldVal){
            this.eigenvalueList=[]
            this.currentFeature={}
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
        this.$emit('detailsLis', this.queryParams)
      },
      clickRow(row,type) {
        this.eigenvalueList=[];
     
        if(this.isShowType=='eigenvalue' && this.modeMian=='edit'){
          this.getMaterialEigenvalueListFn(row)
        }
        if(this.modeMian===""){
          console.log(JSON.stringify(this.isShowType),this.modeMian)
          this.getMaterialEigenvalueListFn(row)
        }
        this.currentFeature=row || {}
        this.$emit('clickRowMain', row,'material')
       },
    clickRowMain(row,type) {
        this.$emit('clickRowMain', row,'details')
      },
    clickRowMainTZZ(row,type) {
      this.$emit('clickRowMain', row,'eigenvalue')
    },
    getMaterialEigenvalueListFn(row){
      getMaterialEigenvalueList({laborServicesFeatureId:row.id}).then(response => {
          console.log(JSON.stringify(response.data))
          let rData  = response.rows || [];
          this.eigenvalueList=rData
          });
    },
      //初始化
      async materialEigenvalueInitDataFn(){
        await materialInitData({organCode:this.org},'/archives/deviceType')
      },
     
      handleEdit(data){
        getMaterialItem(data.id).then(response => {
          console.log(JSON.stringify(response.data))
          this.form={...response.data}
          this.openFeature=true
          this.mode='edit'
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
        materialItemDelete(data.id).then(response => {
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
          const fetchApi = this.mode==='add' ? materialItemAdd : materialItemEdit;
          this.form.organCode=this.org
          this.form.laborServicesClassId=this.currentNode.id
          fetchApi(this.form).then(() => {
            this.$message.success("操作成功");
            this.openFeature = false;
            this.$emit('treeClick', this.currentNode)
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
      initDetailsFn(){
        var params={}
        params.laborServicesClassId=this.currentNode.id
        initDetails(params).then(res => {
          this.formDetail=res.data || {}
          console.log(JSON.stringify(res.data.labourCode))
          this.$set(this.formDetail,'laborServicesCode',res.data.laborServicesCode)
          })
      },
      //明细
      detailAdd(){
      
        console.log(JSON.stringify(this.currentNode))
        if(JSON.stringify(this.currentNode)!=='{}' && this.currentNode.children.length==0 ){
        this.formDetail={};
        this.initDetailsFn()
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
          this.formDetail.laborServicesClassId=this.currentNode.id
          this.formDetail.laborServicesClassName=this.currentNode.label
          console.log(JSON.stringify(this.currentNode))
          fetchApi(this.formDetail).then(() => {
            this.$message.success("操作成功");
            this.openDetail = false;
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
          this.formZ.laborServicesClassId=this.currentNode.id
          this.formZ.laborServicesFeatureId=this.currentFeature.id
          fetchApi(this.formZ).then(() => {
            this.$message.success("操作成功");
            this.openEigenValue = false;
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
  