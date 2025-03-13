import { mapGetters } from 'vuex'
import {listDept} from "@/api/system/dept";
import { deptTree,  } from "@/api/reportForm/managePageReport";
import {getMaterialType,initMaterialType,materialAdd,materialEdit,materialDelete,getMaterialDetailsList ,getMaterialItemList } from "@/api/archivese/dossier/materialType";
export  const material = {
  data() {
    return {
        form: {
            organCode: '',
            id: '',
            materialName: '',//名称
            materialType:'',//类型
            materialCode: '', //编码
            unit: '', //单位
            isTransaction: false,//是否交易标的物
            label:''
          }, 
           // 查询参数
          queryParams: {
            pageNum: 1,
            pageSize: 10,
          },
          featureList:[],
          materialDetailsList:[],
          mode: 'add',
          openFiles:false,
          openFilesDevice:false,
          openFilesLabour:false,
          openFilesSubcontracting:false,
          currentNode:{}, //当前点中节点
          rules: {
            materialName: [{required: true, message: '请输入材料分类名称', trigger: 'blur'}],
            materialCode: [{required: true, message: '请输入材料分类编码', trigger: 'change'}],
            labourName: [{required: true, message: '请输入劳务分类编码', trigger: 'blur'}],
            labourCode: [{required: true, message: '请输入劳务分类编码', trigger: 'blur'}],
            subcontractingName: [{required: true, message: '请输入专业分包编码', trigger: 'blur'}],
            subcontractingCode: [{required: true, message: '请输入专业分包编码', trigger: 'blur'}],
            deviceName: [{required: true, message: '请输入设备名称', trigger: 'blur'}],
            deviceCode: [{required: true, message: '请输入设备编码', trigger: 'blur'}],
          },
    }
  },
  mounted() {
  
  },
  methods: {
    //通过树点击事件获取特征项数据
    treeClick(data,node){
     console.log(JSON.stringify(data))
        if(this.radioType=='1' ||this.radioType=='2' || this.radioType=='3'){
          if(node && node.level>1){
            this.isShowData=true
          }else if(!node){
            this.isShowData=true
          }else{
            this.isShowData=false
          }
        }else{
          if(node && node.level>3){
            this.isShowData=true
          }else if(!node){
            this.isShowData=true
          }else{
            this.isShowData=false
          }
        }
        
        let rData  = data || {};
        if(JSON.stringify(rData?.children)==='[]'  && rData?.children.length==0){
         
            this.currentNode=rData
         getMaterialItemList({organCode:this.org,typeId:this.currentNode.id},this.baseUrlItem).then(response => {
          console.log(JSON.stringify(response.rows))
          let rData  = response.rows || [];
          this.featureList=rData
          });
          this.queryParams.organCode=this.org
          this.queryParams.typeId=this.currentNode.id
          getMaterialDetailsList(this.queryParams,this.baseUrlDetails).then(response => {
              console.log(JSON.stringify(response.rows))
          let rData1  = response.rows || [];
          this.materialDetailsList=rData1
          this.$refs.material.setTotal(response.total);
          });
        }else{
            this.featureList=[]
        }
    },
    detailsLisFn(queryParams){
      queryParams.organCode=this.org
      queryParams.typeId=this.currentNode.id
      getMaterialDetailsList(queryParams,this.baseUrlDetails).then(response => {
      let rData1  = response.rows || [];
      this.materialDetailsList=rData1
      this.$refs.material.setTotal(response.total);
      });
    },
    checkItem(data){
      this.mode='view'
      if(this.radioType==='0'){
        this.openFiles=true
        }else  if(this.radioType==='1'){
        this.openFilesDevice=true
        }else  if(this.radioType==='2'){
          this.openFilesLabour=true
        }else  if(this.radioType==='3'){
          this.openFilesSubcontracting=true
        }
        getMaterialType(data.id,this.baseUrl).then(response => {
          console.log(JSON.stringify(response.data))
          this.form={...response.data}
          this.form.label=data.parentLabel 
          this.form.isTransaction=this.form.isTransaction=="1"?true:false
          });
    },
    editItem(data,node){
        this.mode='edit'
        if(this.radioType==='0'){
            this.openFiles=true
        }else  if(this.radioType==='1'){
        this.openFilesDevice=true
        }else  if(this.radioType==='2'){
          this.openFilesLabour=true
        }else  if(this.radioType==='3'){
          this.openFilesSubcontracting=true
        }
        getMaterialType(data.id,this.baseUrl).then(response => {
          console.log(JSON.stringify(response.data))
          this.form={...response.data}
          this.form.label=data.parentLabel 
          this.form.isTransaction=this.form.isTransaction=="1"?true:false
          });
      },
      deleteItem(data){
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
        materialDelete(data.id,this.baseUrl).then(response => {
              // 删除树节点
          this.$refs.customTree.treeDeleteItem(data)
          // 提示
          this.$message({
            type: 'success',
            message: '删除成功!'
          })
          this.featureList=[]
          this.materialDetailsList=[]
      });},
      // 删除树节点
      deleteTreeItem(data) {
          this.$confirm('确定删除吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
          }).then(() => {
          // 删除树节点
          this.$refs.customTree.treeDeleteItem(data)
          // 提示
          this.$message({
              type: 'success',
              message: '删除成功!'
          })
          }).catch(() => {
          this.$message({
              type: 'info',
              message: '已取消删除'
          })
          })
    },
      addItem(data){
        this.reset();
        this.mode='add'
        // this.form.label=data.label || '顶级'
        this.form.belongingLevel=data.label || '顶级'
        this.form.pid=data.id 
        let url=''
        if(this.radioType==='0'){
        url='/archives/materialType/initMaterialType'
        this.openFiles=true
        }else  if(this.radioType==='1'){
        url='/archives/deviceType/initDeviceType'
        this.openFilesDevice=true
        }else  if(this.radioType==='2'){
        url='/archives/labourType/initLabourType'
        this.openFilesLabour=true
        }else  if(this.radioType==='3'){
        url='/archives/subcontractingType/initSubcontractingType'
        this.openFilesSubcontracting=true
        }

        this.initMaterialTypeFn(data,url)
      },
      initMaterialTypeFn(data,url){
        initMaterialType({organCode:this.org,id:data.id},url).then(response => {
            this.form={...this.form,...response.data}
          });
      },
      //档案新增
      async submitForm(){
        const valid = await this.$refs.form.validate();
        if (valid) {
          const fetchApi = this.mode==='add' ? materialAdd : materialEdit;
          this.form.organCode=this.org
          this.form.isTransaction=this.form.isTransaction==true?'1':'0'
         
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFiles = false;
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
            //   this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.materialName,code:this.form.materialCode,pid:this.form.pid})
            //   this.currentNodeKey=this.form.id
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.materialName,code:this.form.materialCode})
            // }
           
            this.reset();
          }).catch(() => {
            console.log("档案操作失败")
          });
        }
      },
      //设备新增
      async submitFormDevice(){
        const valid = await this.$refs.form.validate();
        if (valid) {
          const fetchApi = this.mode==='add' ? materialAdd : materialEdit;
          this.form.organCode=this.org
          this.form.isTransaction=this.form.isTransaction==true?'1':'0'
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFilesDevice=false
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
            //   this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.deviceName,code:this.form.deviceCode,pid:this.form.pid})
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.deviceName,code:this.form.deviceCode})
            // }
            this.reset();
          }).catch(() => {
            // this.$message.error("操作失败");
            console.log("设备操作失败")
          });
        }
      },
      //劳务
      async submitFormLabour(){
        const valid = await this.$refs.form.validate();
        if (valid) {
          const fetchApi = this.mode==='add' ? materialAdd : materialEdit;
          this.form.organCode=this.org
          this.form.isTransaction=this.form.isTransaction==true?'1':'0'
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFilesLabour=false
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
            //   this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.labourName,code:this.form.labourCode,pid:this.form.pid})
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.labourName,code:this.form.labourCode})
            // }
            this.reset();
          }).catch(() => {
            // this.$message.error("操作失败");
            console.log("劳务操作失败")
          });
        }
      },
      //专业分包
      async submitFormSubcontracting(){
        const valid = await this.$refs.form.validate();
        if (valid) {
          const fetchApi = this.mode==='add' ? materialAdd : materialEdit;
          this.form.organCode=this.org
          this.form.isTransaction=this.form.isTransaction==true?'1':'0'
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFilesSubcontracting=false
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
               // this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.subcontractingName,code:this.form.subcontractingCode,pid:this.form.pid})
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.subcontractingName,code:this.form.subcontractingCode})
            // }
            this.reset();
          }).catch(() => {
            // this.$message.error("操作失败");
            console.log("专业分包操作失败")
          });
        }
      },
      cancel() {
        this.openFiles = false;
        this.reset();
      },
      reset() {
        this.form = {
          organCode: '',
          id: '',
          materialName: '',//名称
          materialType:'',//类型
          materialCode: '', //编码
          unit: '', //单位
          isTransaction: false,//是否交易标的物
        };
      },

  }
}
