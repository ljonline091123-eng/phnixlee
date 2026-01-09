<template>
  <div class="app-container">
    <BackButton path="/evaluate-expert/evaluate-bids" title="评分详情"/>
    <div class="context">
      <PageTitle title="基本信息">
        <div class="page-title-right">
          <el-button type="primary" size="small" @click="submitForm('form')" :disabled="isSubmit" :loading="isSubmit">{{
              isSubmit ? '提交中...' : '提交' }}</el-button>
        </div>
      </PageTitle>
      <el-descriptions class="form-body">
        <el-descriptions-item label="任务名称">{{ procurementScheme.procurementSchemeName }}</el-descriptions-item>
        <el-descriptions-item label="任务编号" :span="2">{{ procurementScheme.procurementSchemeCode
          }}</el-descriptions-item>
        <el-descriptions-item label=" 采购经办人">{{ procurementScheme.procurementOfficerName }}</el-descriptions-item>
        <el-descriptions-item label="招标方式" :span="2">{{ procurementScheme.procurementTypeText }}</el-descriptions-item>
        <el-descriptions-item label="招标文件">
          <!-- <a href="javascript:;" class="link-type" @click="templateDialogVisible = true">{{
            procurementSchemeBidding.biddingTemplate && procurementSchemeBidding.biddingTemplate.fileName }}</a> -->
          <a href="javascript:;" class="link-type" @click="downloadFile(procurementSchemeBidding.biddingTemplate.fileUrl)">{{
              procurementSchemeBidding.biddingTemplate && procurementSchemeBidding.biddingTemplate.fileName }}</a>
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

      <PageTitle title="评分" marginBottom="15px">
        <div class="page-title-right">
          <el-button type="primary" size="small" @click="goEvaluate">评分</el-button>
        </div>
      </PageTitle>
      <el-form :model="formData" ref="form" :rules="rules" label-position="right" label-width="100px" size="medium">
        <el-row :gutter="40">
          <el-col :span="8" class="grid-cell">
            <el-form-item label="商务评分" prop="business" class="required label-right-align">
              <el-input type="text" disabled v-model="formData.business" placeholder="根据评分自动计算"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label=" 技术评分" prop="technology" class="required label-right-align">
              <el-input v-model="formData.technology" type="text" disabled placeholder="根据评分自动计算"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label=" 总评分" prop="technology" class="required label-right-align">
              <el-input v-model="formData.total" type="text" disabled placeholder="根据评分自动计算"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="40">
          <el-col :span="16" class="grid-cell">
            <el-form-item label="评标意见" prop="advice" disabled class="required label-right-align">
              <el-input v-model="formData.advice" type="textarea" :rows="4"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <PageTitle title="评分记录" marginBottom="15px"/>
      <el-table :data="scoreList" stripe border highlight-current-row>
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column label="商务评分" width="200" align="center" prop="busScore" />
        <el-table-column label="技术评分" width="200" align="center" prop="techScore" />
        <el-table-column label="合计" width="200" align="center" prop="techScore">
          <template slot-scope="scope">
            {{ scope.row.techScore != null && scope.row.busScore != null ? (scope.row.techScore + scope.row.busScore).toFixed(2) : scope.row.busScore != null ? scope.row.busScore : scope.row.techScore != null ? scope.row.techScore : '' }}
          </template>
        </el-table-column>
        <el-table-column label="评分时间" width="200" align="center" prop="evaTime" />
        <el-table-column label="专家评标意见" prop="evaOpinion" show-overflow-tooltip/>
      </el-table>

      <!-- 评分弹出 -->
      <el-dialog title="评分" :visible.sync="evaluateVisible" width="40%">
        <el-row :gutter="10">
          <el-col :span="24" v-for="(item, key) in markCategoryDatailVOList" :key="key">
            <PageTitle :title="item.itemType === 1 ? '技术评分' : '商务评分'" marginBottom="15px"/>
            <el-table :data="item.markItemDetailVOList" default-expand-all row-key="id" stripe border
                      :tree-props="{ children: 'subBiddingMarkItemDetailVOList' }">
              <el-table-column label="序号" type="index" width="50" align="center" />
              <el-table-column label="评分项" prop="name" min-width="50%" />
              <el-table-column label="分值" align="center" min-width="30%" prop="highRange" />
              <el-table-column label="得分" prop="score" align="center" min-width="20%">
                <template slot-scope="{row}">
                  <el-input v-model="row.score"
                            v-if="!row.subBiddingMarkItemDetailVOList || row.subBiddingMarkItemDetailVOList.length === 0"
                            type="text" clearable />
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-col>
        </el-row>
        <div slot="footer" class="dialog-footer">
          <el-button @click="evaluateVisible = false" style="width: 100px;" size="small">取 消</el-button>
          <el-button type="primary" @click="confirmEvaluate" style="width: 100px;" size="small">确 定</el-button>
        </div>
      </el-dialog>
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
import { getMarkTempInfo, expertEvaluation, getExpertEvalRecord } from "@/api/evaluate-expert/evaluate-bids"
import { getViweFileURL } from "@/api/template/file";
import FileModule from '@/components/FileModule/index.vue'
import PageTitle from "@/components/PageTitle/index.vue"
import BackButton from '@/components/BackButton/index.vue'
export default {
  name: "evaluate-bids-detail",
  data() {
    return {
      viewFileUrl :"",  //预览招标文件url
      loading: false,
      inventoryList: [],
      isSubmit: false,
      procurementScheme: {}, //基本信息
      procurementSchemeBidding: {},
      skeletonLoading: true,
      param: {},
      bidInfo: {},
      formData: {},
      rules: {
        business: [{ required: true, message: '请先评分' }],
        technology: [{ required: true, message: '请先评分' }],
        advice: [{ required: true, message: '请输入评标意见' }]
      },
      evaluateVisible: false,
      evaluatedata: {},
      markCategoryDatailVOList: [],
      selectList: [],
      isEnd: false,
      templateDialogVisible: false,
      scoreList: [],
      expertType: ''
    };
  },
  components: {
    FileModule,
    PageTitle,
    BackButton
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params))
    this.param = param
    this.bidInfo = param.item
    this.expertType = param.expertType
    if (param.type === 'end') {
      this.isEnd = true
    }
    console.log(param, '参数')
    this.getSchemeDetail()
    this.getExpertEvalRecord({ noticeId: param.noticeId, vendorId: param.item.vendorId })
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
        return;
      }
      console.log("下载文件url：",fileUrl);
      window.open(fileUrl, '_blank');
    },

    async getSchemeDetail() {
      try {
        const res = await getSchemeDetail(this.param.schemeId);
        this.skeletonLoading = false;
        console.log(res, '详情');
        const { procurementScheme, procurementSchemeBidding } = res.data;
        Object.assign(this, { procurementScheme, procurementSchemeBidding });
        console.log("this.procurementSchemeBidding->",this.procurementSchemeBidding)
        // this.getbiddingTemplate();
      } catch (err) {
        console.log(err);
      }
    },
    submitForm(formName) {
      this.$refs[formName].validate(async valid => {
        if (valid) {
          const { advice } = this.formData
          const { schemeId, item, noticeId } = this.param
          let arr = this.markCategoryDatailVOList.map(item => {
            return item.markItemDetailVOList.map(subItem => {
              if (subItem.subBiddingMarkItemDetailVOList && subItem.subBiddingMarkItemDetailVOList.length) {
                return subItem.subBiddingMarkItemDetailVOList.map(sSubItem => {
                  return { itemId: sSubItem.id, score: Number(sSubItem.score), itemType: item.itemType }
                })
              } else {
                return [{ itemId: subItem.id, score: Number(subItem.score), itemType: item.itemType }]
              }
            })
          })
          const formData = {
            schemeId,
            vendorId: item.vendorId,
            biddingInfoId: item.biddingInfoId,
            evalItemDTOSList: [arr.flat().flat()],
            advice,
            noticeId
          }
          console.log(formData, 'formData');
          try {
            const res = await expertEvaluation(formData)
            this.$message.success('评分成功')
            this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              this.$router.push('/evaluate-expert/evaluate-bids');
            })
          } catch (err) {
            console.log(err);
          }
        }
      })
    },
    /** 获取评分模板 */
    async getMarkTempInfo() {
      try {
        const res = await getMarkTempInfo(this.param.schemeId)
        this.evaluatedata = res.data
        const { expertType } = this
        if (this.selectList.length) {
          this.markCategoryDatailVOList = JSON.parse(JSON.stringify(this.selectList));
        } else {
          //this.markCategoryDatailVOList = this.evaluatedata.markCategoryDatailVOList.filter(item => item.itemType === expertType)
          this.markCategoryDatailVOList = this.evaluatedata.markCategoryDatailVOList
        }
      } catch (err) {
        console.log(err);
      }
    },
    confirmEvaluate() {
      let arr = []
      console.log(this.markCategoryDatailVOList, 'this.markCategoryDatailVOList-this.markCategoryDatailVOList');
      this.markCategoryDatailVOList.forEach(item => {
        item.markItemDetailVOList && item.markItemDetailVOList.forEach(subItem => {
          if (subItem.subBiddingMarkItemDetailVOList && subItem.subBiddingMarkItemDetailVOList.length) {
            subItem.subBiddingMarkItemDetailVOList.forEach(sSubItem => {
              arr = arr.concat(sSubItem)
            })
          } else {
            delete subItem.subBiddingMarkItemDetailVOList
            arr = arr.concat(subItem)
          }
        })
      })
      console.log(arr, 'arr');
      let isAll = arr.every(item => item.score)
      if (!isAll) return this.$message.error('请填写所有评分项')
      let isAccord = arr.every(item => Number(item.score) >= Number(item.score) > 0 && Number(item.score) <= Number(item.highRange))
      console.log(isAccord, 'isAccord-isAccord');
      if (!isAccord) return this.$message.error('评分项大于0且小于等于分值')

      let business = 0
      let technology = 0
      let count = 0
      let type = 0
      this.markCategoryDatailVOList.forEach(item => {
        type = item.itemType
        item.markItemDetailVOList && item.markItemDetailVOList.forEach(subItem => {
          if (subItem.subBiddingMarkItemDetailVOList && subItem.subBiddingMarkItemDetailVOList.length) {
            subItem.subBiddingMarkItemDetailVOList.forEach(sSubItem => {
              // count += Number(sSubItem.score)
              if (type === 2) {
                business += Number(sSubItem.score)
              } else if (type === 1) {
                technology += Number(sSubItem.score)
              }
            })
          } else {
            // count += Number(subItem.score)
            if (type === 2) {
              business += Number(subItem.score)
            } else if (type === 1) {
              technology += Number(subItem.score)
            }
          }
        })
      })
      /*if (type === 2) {
        this.$set(this.formData, 'business', count)
      }
      if (type === 1) {
        this.$set(this.formData, 'technology', count)
      }*/
      this.$set(this.formData, 'business', business)
      this.$set(this.formData, 'technology', technology)
      this.$set(this.formData, 'total', business + technology)
      this.selectList = JSON.parse(JSON.stringify(this.markCategoryDatailVOList));
      this.evaluateVisible = false
    },
    goEvaluate() {
      this.evaluateVisible = true
      this.getMarkTempInfo()
    },
    //获取专家评分记录
    async getExpertEvalRecord(params) {
      try {
        const res = await getExpertEvalRecord(params)
        this.scoreList = res.data
        console.log(res, '评分记录');
      } catch (err) {
        console.log(err);
      }
    },
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
  font-size: 13px;
  /* 修改字体大小 */
  font-weight: bolder;
  /* 修改字体粗细 */
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
