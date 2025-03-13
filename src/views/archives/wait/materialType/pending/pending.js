
import {getMaterialDetailsList ,getMaterialItemList } from "@/api/archivese/dossier/materialType";
import { addToMain,associationToMain,unAssociationToMain} from "@/api/archivese/wait/waitPending";
export  const pending = {
  data() {
    return {
      level:null,//当前主库树所选的层级
      featureList:[],
      materialDetailsList:[],
      featureListMain:[],
      materialDetailsListMain:[],
      openMain:false,
      isShowCurrentType:'material', //操作后弹框对应类型
      currentNodeMain:{}, //当前主库点中节点
      currentNodeItem:{},//当前副库选中节点
      currentNodeMainTZX:{},//当前主库选中特征项
      currentNodeMainTZZ:{},//当前主库选中特征值
   
      url:'',
    }
  },
  mounted() {
  
  },

  methods: {
    close(){
        this.materialDetailsListMain=[]
        this.featureListMain=[]
        this.currentNodeMainTZX={}
        this.currentNodeMainTZZ={}
        this.isShowData=false //显示特征项
    },
    detailsLisFn(queryParams){
      queryParams.organCode=this.org
      queryParams.typeId=this.currentNode.id
      queryParams.queryType=this.queryType
      getMaterialDetailsList(queryParams,this.baseUrlDetails).then(response => {
      let rData1  = response.rows || [];
      this.materialDetailsList=rData1
      this.$refs.material.setTotal(response.total);
      });
    },
    //责任单位树的点击事件
    treeClickWait(data,node){
      this.org=data.code
      this.getMaterialTypeTreeFn(data.code,this.queryType)
    },
    //待审库树的点击事件
    treeClick(data,node){
      console.log(JSON.stringify(data)+"当前选中")
        let rData  = data || {};
        if(JSON.stringify(rData?.children)==='[]'  && rData?.children.length==0){
            this.currentNode=rData
         getMaterialItemList({organCode:this.org,typeId:this.currentNode.id,queryType:this.queryType},this.baseUrlItem).then(response => {
          console.log(JSON.stringify(response.rows))
          let rData  = response.rows || [];
          this.featureList=rData
          });
          getMaterialDetailsList({organCode:this.org,typeId:this.currentNode.id,queryType:this.queryType},this.baseUrlDetails).then(response => {
              console.log(JSON.stringify(response.rows))
          let rData1  = response.rows || [];
          this.materialDetailsList=rData1
          this.$refs.material.setTotal(response.total);
          });
        }else{
            this.featureList=[]
        }
    },
    //选择主库的点击事件
    treeClickMain(data,node){
      let rData  = data || {};
      this.currentNodeMain=rData
      this.level=node.level
      // console.log(node.level==5)
      console.log(JSON.stringify(this.currentNodeMain))
      let queryParams={}
      if(this.radioType==='0'){
          queryParams.mtrClassId=this.currentNodeMain.id
      }else  if(this.radioType==='1'){
        queryParams.deviceClassId=this.currentNodeMain.id
      }else  if(this.radioType==='2'){
        queryParams.laborServicesClassId=this.currentNodeMain.id
      }else  if(this.radioType==='3'){
        queryParams.majorSubcontractingClassId=this.currentNodeMain.id
      }
       getMaterialItemList(queryParams,this.baseUrlItemMain).then(response => {
        console.log(JSON.stringify(response.rows))
        let rData  = response.rows || [];
        this.featureListMain=rData
        });
      getMaterialDetailsList(queryParams,this.baseUrlDetailsMain).then(response => {
          console.log(JSON.stringify(response.rows))
      let rData1  = response.rows || [];
      this.materialDetailsListMain=rData1
      this.$refs.material.setTotal(response.total);
      });
    
  }, 
    //材料新增特征项
    mainMaterialAdd(data, type){
      this.isShowType=type
      //新增至主库
      this.openMain=true //显示弹框
      this.currentNodeItem=data  //当前选中要新增至主库的数据
      this.modeMian='add'  
      if(type=='material'){
        this.url=this.baseUrlItem
      }else if(type=='details'){
        this.url=this.baseUrlDetails
      }else if(type=='eigenvalue'){
        this.isShowData=true //显示特征项
        if(this.radioType==='0'){
          this.url='/archives/materialEigenvalue'
          }else  if(this.radioType==='1'){
            this.url='/archives/deviceEigenvalue'
          }else  if(this.radioType==='2'){
            this.url='/archives/labourEigenvalue'
          }else  if(this.radioType==='3'){
            this.url='/archives/subcontractingEigenvalue'
          }
      }
      console.log(JSON.stringify(this.url,this.radioType))
    },
     //关联至主库
     mainMaterialEdit(data, type){
      this.isShowData=true //关联显示特征值特征项
      this.isShowType=type
      console.log(JSON.stringify(this.isShowType,'mainMaterialEdit'))
      this.openMain=true
      this.currentNodeItem=data
      this.modeMian='edit'
      if(type=='material'){
        this.url=this.baseUrlItem
      }else if(type=='details'){
        this.url=this.baseUrlDetails
      }else if(type=='eigenvalue'){
        if(this.radioType==='0'){
          this.url='/archives/materialEigenvalue'
          }else  if(this.radioType==='1'){
            this.url='/archives/deviceEigenvalue'
          }else  if(this.radioType==='2'){
            this.url='/archives/labourEigenvalue'
          }else  if(this.radioType==='3'){
            this.url='/archives/subcontractingEigenvalue'
          }
      }
        //特征值
        // if(type==='eigenvalue'){
        //   this.currentNodeMainTZZ=data
        // }
    },
    mainMaterialDelete(data, type){
      //取消关联
      this.url=this.baseUrlDetails
      this.deleteItem(data)
    },
     //关联至主库列表的点击事件
    clickRowMain(row,type){
      console.log(JSON.stringify(row))
      let rData  = row || {};
      this.isShowCurrentType=type
      //特征值
      if(type==='eigenvalue'){
        this.currentNodeMainTZZ=rData
      }else{
        // if(type==='details'){ //详情
        //   this.url=this.baseUrlDetails
        // }else if(type==='material'){
        //   this.url=this.baseUrlItem //特征项
        // }
        this.currentNodeMainTZX=rData
      }
      console.log(type+"==========================")
      console.log(rData+"==========================")
    },
    
    //类型新增至主库
    mainAdd(data){
      this.url=this.baseUrl
      this.modeMian='add'
      this.openMain=true
      this.currentNodeItem=data
      console.log(JSON.stringify(this.currentNodeItem))
    },
    //类型关联至主库
    mainEdit(data){
      this.url=this.baseUrl
      this.modeMian='edit'
      this.currentNodeItem=data
      this.openMain=true
    },
     //类型取消关联
    mainDelete(data){
      this.url=this.baseUrl
      this.currentNodeItem=data
      this.deleteItem(data)
    },
    deleteItem(data){
      this.$confirm('确定取消关联吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
      this.mainDeleteFn(data)
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消关联'
        })
      })
    },
    mainDeleteFn(data){
      unAssociationToMain({id:data.id},this.url).then(() => {
        this.$message.success("操作成功----------");
        this.$refs.customTree1.refreshTree(this.currentNodeItem.id);
        this.treeClick(this.currentNodeItem)
        this.openMain = false;
      }).catch(() => {
        this.$message.error("操作失败");
      });
    },
    //确认编码
    confirmRenumber(){
        if(this.renumber!='' && this.renumber){
          this.submitMain()
          this.renumberVisible = false
        }else{
          this.$message({
            type: 'info',
            message: '请填写编码！'
          })
        }
    },
    submitMain(){
      //this.modeMian==='edit' 关联至主库 this.modeMian==='add' 新增主库
      console.log(JSON.stringify(this.isShowType))
      if(this.modeMian==='edit'){
        if(this.level>3 || this.radioType!=='0'){
          if(this.level>2 || this.radioType!=='1'){
          if(this.level>1){
          if(this.isShowType=='material' && JSON.stringify(this.currentNodeMainTZX)==='{}'){
            this.$message.error("未选择特征项");
            this.currentNodeMain={}
          } else if(this.isShowType=='details' && JSON.stringify(this.currentNodeMainTZX)==='{}'){
            this.$message.error("未选择特征项");
            this.currentNodeMain={}
          }else {
            if(JSON.stringify(this.currentNodeMainTZX)!=='{}'){
              this.currentNodeMain=this.currentNodeMainTZX
            }
          }
          if(this.isShowType=='eigenvalue' && JSON.stringify(this.currentNodeMainTZZ)==='{}'){
            this.$message.error("未选择特征值");
            this.currentNodeMain={}
          }else{
            if(JSON.stringify(this.currentNodeMainTZZ)!=='{}'){
              this.currentNodeMain=this.currentNodeMainTZZ
            }
          }
        }else{
          this.$message.error("关联至主库不能选择顶层!");
          return
        }
        }else{
          this.$message.error("关联至主库不能选择第1、2层级！");
          return
        }
        }else{
          this.$message.error("关联至主库不能选择第1、2、3层级！");
          return
        }
      }else{
        if(this.level>2 || this.radioType!=='0'){
          if(this.level>1){
        if(this.isShowType=='eigenvalue' && JSON.stringify(this.currentNodeMainTZX)==='{}'){
          this.$message.error("未选择特征项");
          this.currentNodeMain={}
        }else {
          if(JSON.stringify(this.currentNodeMainTZX)!=='{}'){
            this.currentNodeMain=this.currentNodeMainTZX
          }else if(this.level==5 && this.baseUrl===this.url){ //this.baseUrl===this.url说明是类型新增
            this.$message.error("新增主库不能选择第5层级！");
            return
          }
        }
        }else{
            this.$message.error("关联至主库不能选择顶层!");
            return
          }
      }else{
        this.$message.error("新增主库不能选择第1、2层级！");
        return
      }
      }
      console.log(JSON.stringify(this.currentNodeMain))
      console.log(JSON.stringify(this.currentNodeItem))
      console.log(JSON.stringify(this.renumber))
      if (JSON.stringify(this.currentNodeItem)!=='{}' && JSON.stringify(this.currentNodeMain)!=='{}' ) {
          const fetchApi = this.modeMian==='add' ? addToMain : associationToMain;
          let params={id:this.currentNodeItem.id,hostId:this.currentNodeMain.id}
          if(this.isShowType=='material' && this.renumber){
            params.itemCode=this.renumber
          }else  if(this.isShowType=='eigenvalue' && this.renumber!==''){
            params.eigenvalueCode=this.renumber
          }else  if(this.isShowType=='details' && this.renumber!==''){
            if(this.radioType==='0'){
              params.materialCode=this.renumber
              }else  if(this.radioType==='1'){
                params.deviceCode=this.renumber
              }
          }
          fetchApi(params,this.url).then(res => {
            if(res.msg=='编码已存在'){
              this.renumberVisible=true
            }else{
              this.$message.success("操作成功");
              if(this.renumber!==''){
                this.renumber=''
              }
              this.openMain = false;
              //选择的类型清空
              this.isShowType=''
              //组织刷新
              this.$refs.customTree1.refreshTree(this.currentNode.id);
              this.treeClick(this.currentNode)
            }
          }).catch(error => {
            this.$message.error("操作失败");
          });
      }else{
        this.$message.error("未选择对应的类型");
      }
    },

  }
}
