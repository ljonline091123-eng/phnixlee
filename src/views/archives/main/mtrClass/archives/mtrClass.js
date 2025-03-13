
import {getMaterialType,initMaterialType,materialAdd,materialEdit,materialDelete,getMaterialDetailsList ,getMaterialItemList } from "@/api/archivese/dossier/materialType";
export  const material = {
  data() {
    return {
        form: {
            organCode: '',
            id: '',
            // materialName: '',//名称
            // materialType:'',//类型
            // materialCode: '', //编码
            // unit: '', //单位
            subjectMatter: false,//是否交易标的物
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
            mtrClassName: [{required: true, message: '请输入材料分类名称', trigger: 'blur'}],
            mtrClassCode: [{required: true, message: '请输入材料分类编码', trigger: 'change'}],
            deviceClassName: [{required: true, message: '请输入设备分类名称', trigger: 'blur'}],
            deviceClassCode: [{required: true, message: '请输入设备分类编码', trigger: 'blur'}],
            laborServicesClassName: [{required: true, message: '请输入劳务分类名称', trigger: 'blur'}],
            laborServicesClassCode: [{required: true, message: '请输入劳务分类编码', trigger: 'blur'}],
            majorSubcontractingClassName: [{required: true, message: '请输入专业分包名称', trigger: 'blur'}],
            majorSubcontractingClassCode: [{required: true, message: '请输入专业分包编码', trigger: 'blur'}],
          },
    }
  },
  mounted() {
  
  },
  methods: {
    //通过树点击事件获取特征项数据
    treeClick(data,node){
      if(this.radioType=='1' || this.radioType=='2' || this.radioType=='3'){
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
            let queryParams={}
          if(this.radioType==='0'){
              queryParams.mtrClassId=this.currentNode.id
          }else  if(this.radioType==='1'){
            queryParams.deviceClassId=this.currentNode.id
          }else  if(this.radioType==='2'){
            queryParams.laborServicesClassId=this.currentNode.id
          }else  if(this.radioType==='3'){
            queryParams.majorSubcontractingClassId=this.currentNode.id
          }
         getMaterialItemList(queryParams,this.baseUrlItem).then(response => {
          console.log(JSON.stringify(response.rows))
          let rData  = response.rows || [];
          this.featureList=rData
          });
          this.queryParams=queryParams
          queryParams.pageNum= 1,
          queryParams.pageSize= 10,

          getMaterialDetailsList(queryParams,this.baseUrlDetails).then(response => {
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
      this.queryParams.pageNum= queryParams.pageNum,
      this.queryParams.pageSize= queryParams.pageSize,
      getMaterialDetailsList(this.queryParams,this.baseUrlDetails).then(response => {
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
          this.form.subjectMatter=this.form.subjectMatter=="1"?true:false
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
        url='/archives/mtrClass/initCode'
        this.openFiles=true
        }else  if(this.radioType==='1'){
        url='/archives/deviceClass/initCode'
        this.openFilesDevice=true
        }else  if(this.radioType==='2'){
        url='/archives/laborClass/initCode'
        this.openFilesLabour=true
        }else  if(this.radioType==='3'){
        url='/archives/majorClass/initCode'
        this.openFilesSubcontracting=true
        }

        this.initMaterialTypeFn(data,url)
      },
      initMaterialTypeFn(data,url){
        initMaterialType({id:data.id},url).then(response => {
            this.form={...this.form,...response.data}
          });
      },
      //档案新增
      async submitForm(){
        const valid = await this.$refs.form.validate();
        if (valid) {
          const fetchApi = this.mode==='add' ? materialAdd : materialEdit;
          this.form.organCode=this.org
          this.form.subjectMatter=this.form.subjectMatter==true?'1':'0'
         
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFiles = false;
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
            //   this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.mtrClassName,code:this.form.mtrClassCode,pid:this.form.pid})
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.mtrClassName,code:this.form.mtrClassCode})
            // }
           
            this.reset();
          }).catch(() => {
            // this.$message.error("操作失败");
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
          this.form.subjectMatter=this.form.subjectMatter==true?'1':'0'
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFilesDevice=false
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
            //   this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.deviceClassName,code:this.form.deviceClassCode,pid:this.form.pid})
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.deviceClassName,code:this.form.deviceClassCode})
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
          this.form.subjectMatter=this.form.subjectMatter==true?'1':'0'
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFilesLabour=false
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
            //   this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.laborServicesClassName,code:this.form.laborServicesClassCode,pid:this.form.pid})
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.laborServicesClassName,code:this.form.laborServicesClassCode})
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
          this.form.subjectMatter=this.form.subjectMatter==true?'1':'0'
          fetchApi(this.form,this.baseUrl).then(() => {
            this.$message.success("操作成功");
            this.openFilesSubcontracting=false
            this.$refs.customTree.refreshTree(this.form.id);
            // if(this.mode==='add' ){
            //   this.$refs.customTree.treeAddItem({id:this.form.id,label:this.form.majorSubcontractingClassName,code:this.form.majorSubcontractingClassCode,pid:this.form.pid})
            // }else{
            //   this.$refs.customTree.treeEditItem({id:this.form.id,label:this.form.majorSubcontractingClassName,code:this.form.majorSubcontractingClassCode})
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
          subjectMatter: false,//是否交易标的物
        };
      },

  }
}
