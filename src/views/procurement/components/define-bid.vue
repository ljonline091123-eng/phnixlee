<template>
  <div class="app-container">
    <div class="context" style="height: calc(100vh - 116px)">
      <!-- 基本信息 -->
      <PageTitle title="基本信息">
        <div class="page-title-right">
          <el-button
            type="primary"
            size="small"
            v-if="isShowButton"
            @click="handelSanction"
            >审批</el-button
          >
            <!--     撤回操作，这个招标对象 没有审批状态 属性，所以只能根据是否存在流程id或者招标环节阶段值来判断noticeStatus === 5   审批状态为 1 审批中 才能撤回   -->
          <el-button
            type="primary"
            size="small"
            v-if="bpmInitData.revokable && shouldDisableButton  && noticeDetail.tenderNotice.noticeStatus === 5  && noticeDetail.tenderNotice.state === 1"
            @click="
                  revokeBiddingForm(
                    tenantId,
                    scheme.procurementSchemeName
                  )
                "
            >撤回</el-button>
          <el-button
            type="primary"
            size="small"
            @click="handelCalibrationApproval"
            >审批详情</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="submitForm"
            :disabled="shouldDisableButton && ![0,4,5].includes(noticeDetail.tenderNotice.state)"
            :loading="isSubmit"
            >{{ isSubmit ? "提交中..." : "提交" }}</el-button
          >
        </div>
      </PageTitle>
      <el-form :model="scheme" label-width="120px" class="form-container">
        <el-row class="custom-row">
          <el-col :span="8">
            <el-form-item label="任务名称：" class="custom-form-item">
              <span>{{ scheme.procurementSchemeName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务编号：" class="custom-form-item">
              <span>{{ scheme.procurementSchemeCode }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购方式：" class="custom-form-item">
              <span>{{ scheme.procurementTypeText }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row class="custom-row">
          <el-col :span="8">
            <el-form-item label="采购经办人：" class="custom-form-item">
              <span>{{ scheme.procurementOfficerName }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row class="custom-row">
          <el-col :span="24">
            <el-form-item label="决策依据：" class="custom-form-item">
              <div class="file-list">
                <a class="link-type" @click="templateDialogVisible = true"
                  >招标文件</a
                >
                <a
                  href="javascript:;"
                  class="link-type"
                  @click="openBidReport"
                  v-if="scheme.procurementType !== 4"
                >
                  开标报告单
                </a>
                <a href="javascript:;" class="link-type" @click="offerReport">
                  报价汇总
                </a>
                <a
                  href="javascript:;"
                  class="link-type"
                  @click="evaluateReport"
                  v-if="scheme.procurementType !== 4"
                >
                  评标汇总
                </a>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row class="custom-row">
          <el-col :span="24">
            <el-form-item label="上传附件：" class="custom-form-item uploadItem">
              <el-button
                type="primary"
                size="mini"
                v-if="(!scheme.calibrationAttachmentList && !shouldDisableButton) || [0,4,5].includes(noticeDetail.tenderNotice.state)"
                @click="showSecretTips"
                style="margin-top: 8px"
              >上传</el-button
              >
              <el-upload
                class="upload-demo"
                :action="uploadFileUrl"
                :accept="'image/*,.pdf,.doc,.docx'"
                :before-upload="beforeUpload"
                :on-success="handleSuccess"
                :on-error="handleError"
                list-type="text"
                :auto-upload="true"
                :show-file-list="false"
                ref="upload"
              >
              </el-upload>
              <div style="display: flex; align-items: center">
                <el-button
                  type="text"
                  v-if="uploadedFileName"
                  @click="downloadFile"
                >
                  {{ uploadedFileName }}
                </el-button>

                <el-button
                  type="text"
                  v-else-if="attachmentDetails && attachmentDetails.length > 0"
                  @click="downloadFileDetail(attachmentDetails[0])"
                  style="margin-right: 10px"
                >
                  {{ attachmentDetails[0].fileName }}
                </el-button>

                <el-button type="text" v-else disabled> 无附件 </el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <PageTitle title="定标结果" marginBottom="15px">
        <div class="page-title-right">
          <el-button
            type="primary"
            size="small"
            :disabled="shouldDisableButton && ![0,4,5].includes(noticeDetail.tenderNotice.state)"
            @click="handelConfirmBidOpening"
            >确定中标人</el-button
          >
        </div>
      </PageTitle>
      <el-table
        ref="multipleTable"
        size="small"
        :data="evaluateList"
        style="width: 100%"
        v-loading="setBidLoading"
        stripe
        border
        highlight-current-row
        :header-cell-style="{ background: '#F2F2F8' }"
        height="calc(40vh - 15px)"
        @selection-change="handleSelectionChange"
        :row-key="selBidKey"
        :cell-style="cellStyle"
      >
        <el-table-column
          :reserve-selection="true"
          v-if="showCheckbox"
          type="selection"
          width="55"
          align="center"
        />
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          prop="sureBid"
          label="确定中标"
          align="center"
          v-if="shouldDisableButton && ![0,4,5].includes(noticeDetail.tenderNotice.state)"
        >
          <template #default="{ row }">
            <span :style="{ color: row.sureBid === 1 ? 'red' : 'black' }">
              {{ row.sureBid === 1 ? '中标' : '未中标' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="中标候选人"
          width="200"
          align="center"
          prop="candidate"
        />
        <el-table-column
          label="供应商名称"
          min-width="200"
          prop="vendorName"
          show-overflow-tooltip
        />
        <el-table-column
          label="联系人"
          width="150"
          align="center"
          prop="contact"
        />
        <el-table-column
          label="回标详情"
          min-width="80"
          align="center"
          prop="vendorAddress"
        >
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              @click="openBidDetail(row.biddingInfoId)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
        <el-table-column
          width="170"
          label="联系电话"
          align="center"
          prop="phone"
        />
        <el-table-column label="上限价(元)" width="150" align="right" prop="ceilingPrice">
          <template slot-scope="{ row }">
            {{row.scheme && row.scheme.procurementScheme && row.scheme.procurementScheme.ceilingPrice}}
          </template>
        </el-table-column>
        <!-- 把含税总价和上限价比-->
        <el-table-column
          label="是否超上限价"
          min-width="100"
          align="center"
          :formatter="formatterUpProcurementScheme"
        >
        </el-table-column>
        <el-table-column label="含税总价(元)" width="150" align="right" prop="taxPrice">
          <template slot-scope="{ row }">
            {{
              row.quotationDataVOList[row.quotationDataVOList.length - 1]
                .taxPricePattern
            }}
          </template>
        </el-table-column>
        <el-table-column label="不含税总价(元)" width="150" align="right">
          <template slot-scope="{ row }">
            {{
              row.quotationDataVOList[row.quotationDataVOList.length - 1]
                .notTaxPricePattern
            }}
          </template>
        </el-table-column>
        <el-table-column
          label="评标得分"
          align="center"
          v-if="scheme.procurementType !== 4"
        >
          <el-table-column
            label="商务"
            align="center"
            prop="avgBusTotalScore"
          />
          <el-table-column
            label="技术"
            align="center"
            prop="avgTechTotalScore"
          />
        </el-table-column>
        <el-table-column
          prop="score"
          label="综合分"
          align="center"
          v-if="scheme.procurementType !== 4"
        />
        <el-table-column
          prop="rank"
          label="报价排名"
          align="center"
          v-if="scheme.procurementType !== 4"
        />
      </el-table>
      <!-- 开标报告单 -->
      <el-dialog title="开标报告单" :visible.sync="openBidVisible" width="50%">
        <el-form label-width="100px" label-suffix=":">
          <el-row :gutter="40">
            <el-col :span="8">
              <el-form-item label="任务名称" class="custom-form-item">
                <span>{{ scheme.procurementSchemeName }}</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="任务编号" class="custom-form-item">
                <span>{{ scheme.procurementSchemeCode }}</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="采购方式" class="custom-form-item">
                <span>{{ scheme.procurementTypeText }}</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="采购经办人" class="custom-form-item">
                <span>{{ scheme.procurementOfficerName }}</span>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <PageTitle title="投标信息" marginBottom="15px" />
        <el-table size="small" :data="backBidList" border stripe>
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="供应商名称"
            width="200"
            prop="vendorName"
            show-overflow-tooltip
          />
          <el-table-column label="投标时间" align="center" prop="createTime" />
          <el-table-column
            label="含税投标总价(元)"
            align="right"
            prop="taxPricePattern"
          />
          <el-table-column
            label="不含税投标总价(元)"
            align="right"
            prop="notTaxPricePattern"
          />
        </el-table>

        <PageTitle title="开标结果" marginBottom="15px" marginTop="15px" />
        <el-table size="small" :data="peopleList" border>
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="开标人员"
            width="200"
            align="center"
            prop="userName"
          />
          <el-table-column label="是否开标" align="center" prop="isOpenText" />
          <el-table-column label="开标时间" align="center" prop="createTime" />
        </el-table>
      </el-dialog>
      <!-- 报价汇总 -->
      <el-dialog title="报价汇总" :visible.sync="offerVisible" width="50%">
        <el-table size="small" :data="priceSummaryList" border stripe>
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="供应商名称"
            width="200"
            prop="vendorName"
            show-overflow-tooltip
          />
          <el-table-column label="联系人" align="center" prop="contact" />
          <el-table-column label="联系电话" align="center" prop="phone" />
          <el-table-column label="报价汇总" align="center" prop="lastQuotation">
            <!-- <el-table-column
              label="含税总价(元)"
              align="right"
              prop="lastQuotation"
            >
              <template slot-scope="{ row }">
                {{ row.lastQuotation && row.lastQuotation.taxPricePattern }}
              </template>
            </el-table-column>
            <el-table-column
              label="不含税总价(元)"
              align="right"
              prop="lastQuotation"
            >
              <template slot-scope="{ row }">
                {{ row.lastQuotation && row.lastQuotation.notTaxPricePattern }}
              </template>
            </el-table-column> -->
            <el-table-column
              :label="index === 0 ? (scoreLength<=1?'最终轮报价':'首轮报价'):((index!==scoreLength-1)?`${index + 1}轮报价`:'最终轮报价')"
              align="center"
              v-for="(item, index) in scoreLength"
              :key="index"
            >
              <el-table-column width="120" label="含税总价(元)" align="center">
                <template slot-scope="{ row }">
                  {{
                    row.allQuotation[index] &&
                    row.allQuotation[index].taxPricePattern
                      ? row.allQuotation[index].taxPricePattern
                      : "-"
                  }}
                </template>
              </el-table-column>
              <el-table-column
                width="120"
                label="不含税总价(元)"
                align="center"
              >
                <template slot-scope="{ row }">
                  {{
                    row.allQuotation[index] &&
                    row.allQuotation[index].notTaxPricePattern
                      ? row.allQuotation[index].notTaxPricePattern
                      : "-"
                  }}
                </template>
              </el-table-column>
            </el-table-column>
          </el-table-column>
        </el-table>
      </el-dialog>
      <!-- 评标汇总 -->
      <el-dialog title="评标汇总" :visible.sync="evaluateVisible" width="60%">
        <PageTitle title="商务评分" marginBottom="15px" />
        <el-table size="small" :data="businessList" border stripe>
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="供应商名称"
            width="200"
            prop="vendorName"
            show-overflow-tooltip
          />
          <el-table-column label="专家评分" align="center">
            <el-table-column
              :label="item.expertName"
              align="center"
              v-for="(item, index) in business"
              :key="item.expertId"
            >
              <template slot-scope="{ row }">
                {{ row.bidEvaluationExpertScoreVoList[index].score }}
              </template>
            </el-table-column>
          </el-table-column>
          <el-table-column
            label="得分"
            width="200"
            align="center"
            prop="score"
          />
        </el-table>
        <PageTitle title="技术评分" marginBottom="15px" marginTop="15px" />
        <el-table size="small" :data="skillList" border stripe>
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="供应商名称"
            width="200"
            prop="vendorName"
            show-overflow-tooltip
          />
          <el-table-column label="专家评分" align="center">
            <el-table-column
              :label="item.expertName"
              align="center"
              v-for="(item, index) in skill"
              :key="item.expertId"
            >
              <template slot-scope="{ row }">
                {{ row.bidEvaluationExpertScoreVoList[index].score }}
              </template>
            </el-table-column>
          </el-table-column>
          <el-table-column
            label="得分"
            width="200"
            align="center"
            prop="score"
          />
        </el-table>
      </el-dialog>
    </div>
    <!-- 招标文件预览 -->
    <el-dialog
      title="招标文件预览"
      :visible.sync="templateDialogVisible"
      width="80%"
    >
      <!-- <FileModule
        :attachmentId="
          scheme.biddingTemplate && scheme.biddingTemplate.attachmentId
        "
        height="600px"
      /> -->
      <iframe allowfullscreen="true"
        v-if="scheme.biddingTemplate"
        :src= this.viewFileUrl
        width="100%"
        height="500px"
        frameborder="0"
      ></iframe>
    </el-dialog>
    <!-- 定标审批流程详情 -->
    <el-dialog
      title="定标审批流程详情"
      :visible.sync="calibrateVisible"
      width="80%"
    >
      <div class="page-title">
        <span>审批信息</span>
      </div>
      <div class="form-body">
        <el-steps :active="calibrateActive" align-center class="step_item">
          <el-step
            :title="item.nodeName"
            v-for="(item, index) in processInformationList"
            :description="getUserNames(item.userList)"
            :key="index"
          ></el-step>
        </el-steps>
        <el-table
          class="table-body"
          v-loading="calibrateLoading"
          :data="approveLists"
          border
          size="mini"
        >
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column label="姓名" align="center" prop="handlerName" />
          <el-table-column label="操作类型" align="center" prop="operateName" />
          <el-table-column label="审批时间" align="center" prop="endTime" />
          <el-table-column
            label="操作说明"
            align="center"
            prop="operateRemark"
          />
          <el-table-column label="批语" align="center" prop="operateComment">
            <template v-slot="scope">
              <span>
                {{ scope.row.operateComment ? scope.row.operateComment : "-" }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
    <!-- 定标审批流程 -->
    <el-dialog title="定标审批流程" :visible.sync="sanctionVisible" width="50%">
      <el-form ref="form" :model="sanctionForm" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="sanctionForm.pass">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="sanctionForm.pass === false" label="驳回节点">
          <el-select v-model="sanctionForm.rejectTaskKey" placeholder="请选择">
            <el-option
              v-for="option in rejectNodeList"
              :key="option.taskKey"
              :label="option.taskName"
              :value="option.taskKey"
            ></el-option>
          </el-select>
        </el-form-item>
        <!--   选择'通过'显示，'可选审批人'显示   -->
        <el-form-item v-if="sanctionForm.pass && nextAppointable" label="指派人" required>
          <!--  nextAuditUserId下一步审批人  -->
          <el-select v-model="sanctionForm.nextAuditUserId" placeholder="请选择" :clearable="true">
            <el-option
              v-for="candidate in nextCandidateList"
              :key="candidate.userId"
              :label="candidate.userName"
              :value="candidate.userId"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="批语">
          <el-input
            type="textarea"
            v-model="sanctionForm.operateComment"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit">提交</el-button>
          <el-button @click="sanctionVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
    <!-- 查看回标详情 -->
    <el-dialog
      title="回标详情"
      :visible.sync="bidDetailVisiable"
      width="80%"
      append-to-body
    >
      <BackBidDetail
        :id="biddingInfoId"
        :noticeDetail="noticeDetail"
        :scheme="scheme"
      />
    </el-dialog>
  </div>
</template>

<script>
import {
  getCalibrationReportList,
  calibration,
  getBackList,
  getPeopleList,
  getBiddingQuotationSummary,
  getBidEvaluationList,
  getBiddingQuotationList,
  getLoadTaskDef,
  getProcessLogList,
  postAuditProcess, getOrgByUserId, revokeBidding,
} from "@/api/procurement/manage";
import FileModule from "@/components/FileModule/index.vue";
import PageTitle from "@/components/PageTitle/index.vue";
import { uploadFileUrl } from "@/utils/const";
import BackBidDetail from "./back-bid-detail.vue";
import {showSecretRelatedTips} from "@/utils/MyUtils";
import { getViweFileURL } from "@/api/template/file";
import Vue from 'vue'
import {withdrawalPlan} from "@/api/procurement/scheme";
const vm = new Vue();
export default {
  name: "define-bid",
  props: {
    noticeDetail: {
      type: Object,
      default: () => {},
    },
    scheme: {
      type: Object,
      default: () => {},
    },
    bpmInitData: {
      type: Object,
      default: () => {},
    },
    /* 可选审批人列表 */
    nextCandidateList: {
      type: Array,
      required: true,
    },
    /* 是否可指定审批人 */
    nextAppointable: {
      type: Boolean,
      required: true,
    },
    isShowButton: {
      type: Boolean,
      default: false,
    },
    tenantId: {
      type: String,
      default: "1",
    },
    authorityId: {
      type: String,
      default: "1",
    },
    rejectNodeList: {
      type: Array,
      default: [],
    },
    attachmentDetails: {
      type: Array,
      default: [],
    },
    taskPresentId: {
      type: String,
      default: "1",
    },
    isShowApprovalDetails: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      viewFileUrl :"",  //预览招标文件url
      procurementScheme: {},
      isSubmit: false,
      evaluateList: [],
      setBidLoading: false,
      openBidVisible: false,
      evaluateVisible: false,
      offerVisible: false,
      peopleList: [],
      backBidList: [],
      priceSummaryList: [],
      businessList: [],
      skillList: [],
      business: [],
      skill: [],
      templateDialogVisible: false,
      isSuccess: false,
      calibrateVisible: false,
      processInformationList: {},
      calibrateActive: 0,
      approveLists: [],
      calibrateLoading: false,
      sanctionVisible: false,
      sanctionForm: {
        pass: true,
        rejectTaskKey: "",
        operateComment: "",
      },
      uploadFileUrl,
      uploadAttachmentList: [],
      uploadedFileName: "", // 用于存储上传成功的文件名
      uploadedFileUrl: "", // 用于存储上传成功的文件下载链接
      bidDetailVisiable: false,
      biddingInfoId: "",
      scoreLength: 0,
      showCheckbox: false,
      selectedRowList: [],
    };
  },
  components: {
    FileModule,
    PageTitle,
    BackBidDetail,
  },

  created() {
    this.getBiddingQuotationList();
    this.getbiddingTemplate();
  },
  // mounted() {
  //   this.$nextTick(() => {
  //     // 创建一个 Set 来存储 sureBid 为 1 的行的 ID
  //     const selectedRowIds = new Set(
  //       this.evaluateList
  //         .filter((row) => row.sureBid === 1)
  //         .map((row) => row.id)
  //     );
  //     console.log("Selected Row IDs:", selectedRowIds);

  //     // 获取表格组件的引用
  //     const table = this.$refs.multipleTable;
  //     console.log("Table Reference:", table);

  //     if (table) {
  //       // 清除当前选择的行
  //       table.clearSelection();

  //       // 遍历所有行数据
  //       this.evaluateList.forEach((row) => {
  //         // 如果当前行的 ID 在选中的 ID 集合中
  //         if (selectedRowIds.has(row.id)) {
  //           console.log("Selecting Row:", row);
  //           // 将当前行标记为选中
  //           table.toggleRowSelection(row, true);
  //         }
  //       });
  //     }
  //   });
  // },

  methods: {
    cellStyle({ row, column }) {
      debugger
      const ceilingPrice = row.scheme?.procurementScheme?.ceilingPrice;
      const taxPrice = (row?.quotationDataVOList[row.quotationDataVOList.length - 1]?.taxPricePattern || 0).replace(/,/g, '');
      // const taxPrice = row.quotationDataVOList[row.quotationDataVOList.length - 1]?.taxPricePattern;

      // 判断是否是“上限价”或“含税总价”列
      if (column.property === 'ceilingPrice' || column.property === 'taxPrice') {
        if (ceilingPrice !== undefined && taxPrice !== undefined && taxPrice > ceilingPrice) {
          return { color: 'red' };
        }
      }

      // 对于其他列，返回空对象表示不应用任何特殊样式
      return {};
    },
    //获取招标文件的预览url
    async getbiddingTemplate(){
      //解构biddingTemplate，获取招标文件的属性
      if (this.scheme && this.scheme.biddingTemplate) {
        const { attachmentId = '', fileName = '', fileUrl = '' } = this.scheme.biddingTemplate;
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
        console.warn('biddingTemplate 数据未正确加载');
      }
    },

    formatterUpProcurementScheme(row) {
      if(row.scheme && row.scheme.procurementScheme && row.scheme.procurementScheme.ceilingPrice){
        const cleanedString = (row?.quotationDataVOList[row.quotationDataVOList.length - 1]?.taxPricePattern || 0).replace(/,/g, '');
        return Number(cleanedString) > Number(row.scheme?.procurementScheme?.ceilingPrice) ? "是" : "否";
      }
    },
    showSecretTips() {
      showSecretRelatedTips(()=>{
        this.$refs['upload'].$refs['upload-inner'].handleClick()
      })
    },
    async getBiddingQuotationList() {
      this.setBidLoading = true;
      const { id: schemeId } = this.scheme;
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await getBiddingQuotationList({ schemeId, noticeId });
        this.evaluateList = res.data.map((row) => ({
          ...row,
          sureBid: row.sureBid || 0, // 确保 sureBid 适当初始化
        }));
        this.updateSelectedRowList();
      } catch (err) {
        console.log(err);
      }
      this.setBidLoading = false;
    },
    beforeUpload(file) {
      const isValidType = [
        "image/jpeg",
        "image/png",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      ].includes(file.type);
      if (!isValidType) {
        this.$message.error("只能上传图片、Word文档或PDF文档");
      }
      return isValidType;
    },
    handleSuccess(response, file, fileList) {
      // 处理上传成功后的逻辑
      this.uploadedFileName = file.name;
      this.uploadedFileUrl = file.response.data.url || "";
      const attachmentObj = {
        fileName: file.name,
        fileUrl: file.response.data.url,
      };
      this.uploadAttachmentList.push(attachmentObj);
      this.$message.success("上传成功");
    },
    handleError(error, file, fileList) {
      // 处理上传失败后的逻辑
      this.$message.error("上传失败");
    },
    handelConfirmBidOpening() {
      this.showCheckbox = true; // 切换复选框的显示状态
      const selectedItems = this.evaluateList.filter(
        (item) => item.sureBid === 1
      );

      if (this.showCheckbox) {
        this.$nextTick(() => {
          selectedItems.forEach((row) => {
            this.$refs.multipleTable.toggleRowSelection(row, true); // 确保行被选中
          });
        });
      } else {
        this.$nextTick(() => {
          this.$refs.multipleTable.clearSelection(); // 如果复选框隐藏，清除选择
        });
      }
    },
    selBidKey(row) {
      return row.id;
    },
    handleSelectionChange(selectedRows) {
      // 创建一个 Set 来存储选中的行的 ID
      const selectedRowIds = new Set(selectedRows.map((row) => row.id));
      // 更新所有行的 sureBid 属性
      this.evaluateList = this.evaluateList.map((row) => ({
        ...row,
        sureBid: selectedRowIds.has(row.id) ? 1 : 0,
      }));
      // 选中的行列表（如果需要）
      this.selectedRowList = selectedRows.map((row) => ({
        ...row,
        sureBid: 1,
      }));
    },
    updateSelectedRowList() {
      // 通过 sureBid 为 1 的行来更新 selectedRowList
      this.selectedRowList = this.evaluateList.filter(
        (row) => row.sureBid === 1
      );
      console.log("[ d888889999666 ] >", this.selectedRowList);
    },
    downloadFile() {
      const url = this.uploadedFileUrl;
      const name = this.uploadedFileName;
      if (url && name) {
        const a = document.createElement("a");
        a.href = url;
        a.download = name;
        a.target = "_blank";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    },
    downloadFileDetail(param) {
      const url = param.fileUrl;
      const name = param.fileName;
      if (url && name) {
        const a = document.createElement("a");
        a.href = url;
        a.download = name;
        a.target = "_blank";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    },
    async submitForm() {
      debugger
      this.isSubmit = true;
      const detailUrl = this.$route.fullPath;
      // const mergedList = [...this.selectedRowList, ...this.evaluateList];
      // const uniqueList = Array.from(
      //   new Map(mergedList.map((item) => [item.id, item])).values()
      // );
      // this.evaluateList = uniqueList;


      let vendorNames = this.evaluateList.filter(obj => obj.sureBid === 1 && this.formatterUpProcurementScheme(obj) === '是').map(obj => '<br>&nbsp;&nbsp;'+obj.vendorName).toString().replace(/,/g, '');
      console.log('%c👽 超出上限价的供应商： ', `font-size: 20px;background-color: #f00;`, vendorNames);
      if(vendorNames!==null&&vendorNames!==undefined&&vendorNames!==''&&vendorNames.length>0){
        vm.$confirm('<b>您选择的供应商</b>'+vendorNames+'<br><b>投标已经超上限价，是否继续？</b><br><b style="color: red">如需继续请点击取消上传相关说明！</b>', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: true // 启用 HTML 支持
        }).then(async () => {
          try {
            this.revokeLoding = this.$loading({
              lock: true,
              text: "定标提交中...",
              spinner: "el-icon-loading",
              background: "rgba(0, 0, 0, 0.7)",
            });
            const res = await calibration({
              calibrationVOList: this.evaluateList,
              detailUrl: detailUrl,
              calibrationDocAttachList: this.uploadAttachmentList,
            });
            this.revokeLoding.close();
            this.$message.success("定标成功");
            this.isSuccess = true;
            this.$emit("changeState");
          } catch (err) {
            this.revokeLoding.close();
            console.log(err);
          }
        }).catch(() => {
          vm.$message({
            type: 'info',
            message: '已取消'
          });
        });
        this.isSubmit = false;
      }else{
        try {
          const res = await calibration({
            calibrationVOList: this.evaluateList,
            detailUrl: detailUrl,
            calibrationDocAttachList: this.uploadAttachmentList,
          });
          this.$message.success("定标成功");
          this.isSuccess = true;
          this.$emit("changeState");
        } catch (err) {
          console.log(err);
        }
        this.isSubmit = false;
      }
    },
    /* 撤回定标 */
    async revokeBiddingForm(id,name){
      console.log('%c👽 是否确定撤回定标 ', `font-size: 20px;background-color: #f00;`, '是否确定撤回定标');
      this.$confirm("是否确定撤回定标：" + name, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          this.revokeLoding = this.$loading({
            lock: true,
            text: "撤回中...",
            spinner: "el-icon-loading",
            background: "rgba(0, 0, 0, 0.7)",
          });
          revokeBidding(id)
            .then((res) => {
              if (res.code == 200) {
                this.$message.success("撤回成功");
              }
              this.revokeLoding.close();
              /* 刷新页面 */
              location.reload();
            })
            .catch((e) => this.revokeLoding.close());
        } catch (error) {}
      });
    },
    getUserNames(userList) {
      if (!userList || userList.length === 0) {
        return "";
      }
      return userList.map((user) => user.userName).join(", ");
    },
    openBidReport() {
      this.openBidVisible = true;
      this.getBackList();
      this.getPeopleList();
    },
    offerReport() {
      this.offerVisible = true;
      this.getBiddingQuotationSummary();
    },
    evaluateReport() {
      this.evaluateVisible = true;
      this.getBidEvaluationList(this.noticeDetail?.tenderNotice?.id, 1);
      this.getBidEvaluationList(this.noticeDetail?.tenderNotice?.id, 2);
    },
    /** 获取回标列表 */
    async getBackList() {
      const { id: schemeId } = this.scheme;
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await getBackList({ schemeId, noticeId });
        this.backBidList = res.data;
        console.log(res, "rrr");
      } catch (err) {
        console.log(err);
      }
    },
    /** 获取开标人员开标状态 */
    async getPeopleList() {
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await getPeopleList(noticeId);
        this.peopleList = res.data;
      } catch (err) {
        console.log(err);
      }
    },
    // 获取报价汇总
    async getBiddingQuotationSummary() {
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      const { id: schemeId } = this.scheme || {};
      console.log(this.scheme, "scheme");
      try {
        const res = await getBiddingQuotationSummary(noticeId, schemeId);
        this.priceSummaryList = res.data;
        res.data.forEach((item) => {
          const length = item.allQuotation.length;
          if (length > this.scoreLength) {
            this.scoreLength = length;
          }
        });
      } catch (err) {
        console.log(err);
      }
    },
    // 获取评标汇总
    async getBidEvaluationList(noticeId, scoreType) {
      try {
        const res = await getBidEvaluationList(noticeId, scoreType);
        console.log(res, "评分分");
        if (scoreType === 1) {
          this.skillList = res.data;
          this.skill = res.data[0].bidEvaluationExpertScoreVoList;
        } else if (scoreType === 2) {
          this.businessList = res.data;
          this.business = res.data[0].bidEvaluationExpertScoreVoList;
        }
      } catch (err) {
        console.log(err);
      }
    },
    openBidDetail(id) {
      this.bidDetailVisiable = true;
      this.biddingInfoId = id;
    },
    // 审批流程
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        let params = {
          businessId: this.tenantId,
          processId: this.authorityId,
        };
        let res = null;
        if (this.tenantId && this.authorityId && this.noticeDetail.tenderNotice.wfProcessId) {
          res = await getLoadTaskDef(params);
        }else{
          /* 未提交时查看流程执行流程，根据登录人id 获取流程分组 */
          res = await getOrgByUserId(this.$store.state.user.id);
          params = {
            processKey: "jiantou-zhaocai:"+res.data+":ZHAOCAI_TENDER_CALIBRATE",
            businessId: 88882352353245888,
          };
          res = await getLoadTaskDef(params);
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


        if (this.tenantId && this.authorityId && this.noticeDetail.tenderNotice.wfProcessId) {
          const response = await getProcessLogList(params);
          this.approveLists = response.data;
        }
      } catch (error) {
        console.log("[ error ] >", error);
      }
      this.calibrateLoading = false;
    },
    // 提交审批
    handelSanction() {
      this.sanctionVisible = true;
    },
    onSubmit() {
      const params = {
        ...this.sanctionForm,
        businessId: this.tenantId,
        processId: this.authorityId,
        curTaskId: this.taskPresentId,
        processKey: "jiantou-zhaocai:{org}:ZHAOCAI_TENDER_CALIBRATE",
      };
      postAuditProcess(params).then(() => {
        this.$message.success("提交成功");
        this.sanctionVisible = false;
        this.$emit("submitSuccess");
      }).catch(error => {
        /* 关闭遮罩层 */
        this.$modal.closeLoading();
      });
    },
  },
  computed: {
    shouldDisableButton() {
      const tenderNotice = this.noticeDetail.tenderNotice || {};
      const { isSubmit, isSuccess } = this;
      return isSubmit ||
        tenderNotice.noticeStatus > 5 ||
        isSuccess ||
        tenderNotice.wfProcessId ||
        this.noticeDetail.purchaseOfficer === false
        ? true
        : false;
    },
  },
};
</script>
<style lang="scss" scoped>
::v-deep .el-form-item.uploadItem {
  .el-form-item__content {
    .upload-demo {
      height: 0;
    }
    line-height: 0;
  }
}
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
    background: #2b4acb;
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}

.form-body {
  padding: 20px;
}

.file-list {
  line-height: 30px;
  width: 150px;
  padding-top: 6px;
  a {
    display: block;
    flex-basis: 100%;
  }
}
.app-container {
  width: 100%;
  font-family: PingFang SC;
  background-color: #f2f2f8; //主体内容颜色配置
  padding: 0 !important;
}
::v-deep .el-dialog__footer {
  background-color: rgb(255, 255, 255);
  text-align: right;
}
.table-body {
  padding: 20px;
}
.custom-row {
  display: flex;
  flex-wrap: wrap;
  line-height: 36px; /* 设置行高 */
}

.custom-col {
  display: flex;
  align-items: center; /* 垂直居中对齐内容 */
  height: 36px; /* 确保每列的高度与行高一致 */
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
</style>
