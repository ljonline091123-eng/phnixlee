<template>
  <div class="app-container">
    <BackButton path="/evaluate-expert/evaluate-bids-end" title="评分详情"/>
     <div class="context">
    <PageTitle title="基本信息"/>
    <el-descriptions class="form-body">
      <el-descriptions-item label="任务名称">{{ procurementScheme.procurementSchemeName }}</el-descriptions-item>
      <el-descriptions-item label="任务编号" :span="2">{{ procurementScheme.procurementSchemeCode
        }}</el-descriptions-item>
      <el-descriptions-item label=" 采购经办人">{{ procurementScheme.procurementOfficerName }}</el-descriptions-item>
      <el-descriptions-item label="招标方式" :span="2">{{ procurementScheme.procurementTypeText }}</el-descriptions-item>
      <el-descriptions-item label="招标文件">
        <a href="javascript:;" class="link-type" @click="downloadFile(procurementSchemeBidding.biddingTemplate.fileUrl)">{{ procurementSchemeBidding.biddingTemplate && procurementSchemeBidding.biddingTemplate.fileName }}</a>
        <!-- <a href="javascript:;" class="link-type" @click="templateDialogVisible = true">{{ procurementSchemeBidding.biddingTemplate && procurementSchemeBidding.biddingTemplate.fileName }}</a> -->
      </el-descriptions-item>
    </el-descriptions>


    <PageTitle title="投标信息"/>
    <el-descriptions class="form-body">
      <el-descriptions-item label="供应商名称" :span="3">{{ bidInfo.vendorName }}</el-descriptions-item>
      <el-descriptions-item label="标书" :span="3">
        <div v-if="bidInfo.attachments && bidInfo.attachments.length>0">
          <div v-for="(item,index) in bidInfo.attachments" :key="index">
            <a :href="item.fileUrl" target="_blank" class="link-type">{{ item.fileName }}</a>
          </div>
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="含税总价(元)">{{ bidInfo.taxPricePattern }}</el-descriptions-item>
      <el-descriptions-item label="不含税总价(元)">{{ bidInfo.notTaxPricePattern }}</el-descriptions-item>
    </el-descriptions>

    <PageTitle title="评分"/>
    <div class="form-body">
      <el-descriptions>
          <el-descriptions-item label="商务评分">{{ formData.business }}</el-descriptions-item>
          <el-descriptions-item label="技术评分" :span="2">{{ formData.technology }}</el-descriptions-item>
          <el-descriptions-item label="评标意见" :span="2">{{ formData.advice }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <PageTitle title="评分记录" marginBottom="15px"/>
    <el-table :data="scoreList" class="no-right-border" stripe border highlight-current-row>
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column label="商务评分" width="200" align="center" prop="busScore" />
      <el-table-column label="技术评分" width="200" align="center" prop="techScore" />
      <el-table-column label="评分时间" width="200" align="center" prop="evaTime" />
      <el-table-column label="专家评标意见" prop="evaOpinion" show-overflow-tooltip/>
    </el-table>


  </div>
  <!-- 文件预览 -->
  <!-- <el-dialog title="招标文件预览" :visible.sync="templateDialogVisible" width="80%">
    <iframe allowfullscreen="true"
        v-if="procurementSchemeBidding.biddingTemplate && procurementSchemeBidding.biddingTemplate.attachmentId"
        :src= this.viewFileUrl
        width="100%"
        height="500px"
        frameborder="0"
      ></iframe>
  </el-dialog> -->
  </div>
</template>

<script>
import { Base64 } from 'js-base64';
import { getSchemeDetail } from "@/api/procurement/scheme";
import { getMarkTempInfo, expertEvaluation, getExpertEvalData, getExpertEvalRecord } from "@/api/evaluate-expert/evaluate-bids"
import { getViweFileURL } from "@/api/template/file";
import FileModule from '@/components/FileModule/index.vue'
import PageTitle from "@/components/PageTitle/index.vue"
import BackButton from '@/components/BackButton/index.vue'
export default {
  name: "evaluate-bids-end-detail",
  data() {
    return {
      viewFileUrl :"",  //预览招标文件url
      loading: false,
      inventoryList: [],
      isSubmit: false,
      procurementScheme: {}, //基本信息
      procurementSchemeBidding: {}, //文件
      skeletonLoading: true,
      param: {},
      bidInfo: {},
      formData:{},
      evaluatedata:{},
      templateDialogVisible: false,
      scoreList:[]
    };
  },
  components:{
    FileModule,
    PageTitle,
    BackButton
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params))
    this.param = param
    this.bidInfo = param.item
    console.log(param, '参数')
    this.getSchemeDetail()
    this.getExpertEvalData()
    this.getExpertEvalRecord({noticeId:param.noticeId,vendorId:param.item.vendorId})
  },
  methods: {
    //获取招标文件的预览url
    async getbiddingTemplate(){
      console.log("开始获取招标文件预览URL-》》******");
      //解构biddingTemplate，获取招标文件的属性
      if (this.procurementSchemeBidding && this.procurementSchemeBidding.biddingTemplate) {
        const { attachmentId = '', fileName = '', fileUrl = '' } = this.procurementSchemeBidding.biddingTemplate;
        if (!fileName || !fileUrl) {
          console.error('招标文件为空(文件名或者文件URL为空)，无法预览');
          // 提示用户招标文件为空无法预览
          ElMessage.error('招标文件为空(文件名或者文件URL为空)，无法预览');
          return; // 终止函数执行
        }
        console.log('Attachment ID:', attachmentId);
        console.log('File Name:', fileName);
        console.log('File URL:', fileUrl);
        //获取文档中台的文档编辑URL
        try {
          const query1 = { fileName: fileName, fileUrl: fileUrl };
          console.log('query1:', query1);
          const res = await getViweFileURL(query1);
          this.viewFileUrl = res.data;
          console.log("viewFileUrl:",this.viewFileUrl);
        } catch (err) {
          console.log(err);
        }
      } else {
        console.error('无法预览招标文件,招标文件模板数据(procurementSchemeBidding.biddingTemplate)未正确加载');
      }
    },

    /** 下载模板文件 */
    downloadFile(fileUrl) {
      if (!fileUrl) {
        ElMessage.error('招标文件为空文件URL为空，无法下载');
        return; // 终止函数执行
      }
      console.log("下载文件url：",fileUrl);
      window.open(fileUrl, '_blank');
    },

    async getSchemeDetail() {
      try {
        const res = await getSchemeDetail(this.param.schemeId);
        this.skeletonLoading = false;
        console.log(res, '详情');
        const { procurementScheme, procurementSchemeBidding} = res.data;
        Object.assign(this, { procurementScheme, procurementSchemeBidding });
        console.log("this.procurementSchemeBidding->",this.procurementSchemeBidding);
        this.getbiddingTemplate(); //获取招标文件预览url
      } catch (err) {
        console.log(err);
      }
    },
     submitForm(formName) {
      this.$refs[formName].validate(async valid => {
        if(valid){
          const { advice } = this.formData
          const {schemeId, item, noticeId} = this.param
          let arr = this.evaluatedata.markCategoryDatailVOList.map(item => {
            return item.markItemDetailVOList.map(subItem => ({itemId:subItem.id,score:Number(subItem.score),itemType:item.itemType}))
          })
          const formData = {
            schemeId,
            vendorId:item.vendorId,
            biddingInfoId:item.biddingInfoId,
            evalItemDTOSList:arr,
            advice,
            noticeId
          }
          const res = await expertEvaluation(formData)
          this.$message.success('评分成功')
          this.$tab.closePage().then(() => {
            // 执行结束的逻辑
            this.$router.push('/evaluate-expert/evaluate-bids');
          })
        }
      })
    },
    /** 获取评分模板 */
    async getMarkTempInfo(){
      try{
        const res = await getMarkTempInfo(this.param.schemeId)
        this.evaluatedata = res.data
        console.log(res,'rrrrrrrr');
      }catch(err){
        console.log(err);
      }
    },
    confirmEvaluate(){
      let arr = []
      this.evaluatedata.markCategoryDatailVOList.forEach(item => {
        arr = arr.concat(item.markItemDetailVOList)
      })
      let isAll = arr.every(item => item.score)
      if(!isAll) return this.$message.error('请填写所有评分项')
      let isAccord = arr.every(item => Number(item.score) >= Number(item.lowRange) && Number(item.score) <= Number(item.highRange))
      console.log(isAccord,'isAccord-isAccord');
      if(!isAccord) return this.$message.error('评分项不在评分范围')

      this.evaluatedata.markCategoryDatailVOList.forEach(item => {
        let count = item.markItemDetailVOList.reduce((p,r) => p + Number(r.score), 0)
        if(item.itemType === 1){
          this.$set(this.formData,'business',count)
        }
        if(item.itemType === 2){
          this.$set(this.formData,'technology',count)
        }
      })
      this.evaluateVisible = false
    },
    goEvaluate(){
      this.evaluateVisible = true
      this.getMarkTempInfo()
    },
    async getExpertEvalData(){
      const { item, noticeId } = this.param
      try{
        const res = await getExpertEvalData({noticeId,biddingInfoId:item.biddingInfoId})
        this.formData.business = res.data.busScore
        this.formData.technology = res.data.techScore
        this.formData.advice = res.data.evaOpinion
        console.log(res,'详情~~~~~');
      }catch(err){
        console.log(err);
      }
    },
    //获取专家评分记录
    async getExpertEvalRecord(params) {
      try{
        const res = await getExpertEvalRecord(params)
        this.scoreList = res.data
        console.log(res,'评分记录');
      }catch(err){
        console.log(err);
      }
    }
  }
};
</script>
<style lang="scss" scoped>
.page-title {
  width: 100%;
  border-bottom: solid 1px #ccc;
  padding: 10px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px; /* 修改字体大小 */
  font-weight: bolder; /* 修改字体粗细 */
  color: #121735;
  &::before {
    content: "";
    width: 3px;
    height: 14px;
    background: #2B4ACB;
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}


.form-body {
  padding: 20px;
}
</style>
