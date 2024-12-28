<template>
  <div class="app-container">
    <BackButton path="/procurement/scheme" title="采购方案详情">
      <div class="back-container">
        <div
          v-if="
            Number(procurementScheme.state) === 0 ||
            Number(procurementScheme.state) === 5
          "
          class="button-group"
        >
          <el-button
            type="primary"
            size="mini"
            @click="goSubmit"
            :disabled="isSubmit"
            :loading="isSubmit"
            >{{ isSubmit ? "提交中..." : "提交" }}</el-button
          >
          <el-button type="primary" size="mini" @click="goUpdate"
            >修改</el-button
          >
          <el-button type="primary" size="mini" @click="goCancellation"
            >作废</el-button
          >
        </div>
        <div v-if="Number(procurementScheme.state) === 1">
          <el-button type="primary" size="mini" @click="handelWithdrawalPlan"
            >撤回</el-button
          >
        </div>
        <div class="permissionButton button-group">
          <el-button
            type="primary"
            size="mini"
            v-if="isShowButton"
            @click="handelSanction"
            >审批</el-button
          >
          <el-button
            type="primary"
            size="mini"
            @click="handelCalibrationApproval"
            >审批详情</el-button
          >
        </div>
      </div>
    </BackButton>
    <el-dialog
      title="提交"
      :visible.sync="submitDialogVisible"
      @close="resetForm"
    >
      <div>
        <div class="tags-container">
          <span class="required">*</span>
          <span class="tagsComments">批语：</span>
          <el-tag
            v-for="tag in tags"
            :key="tag"
            @click="setTag(tag)"
            :type="tag === selectedTag ? 'info' : ''"
            style="margin-right: 8px"
          >
            {{ tag }}
          </el-tag>
        </div>
        <el-input
          type="textarea"
          v-model="reviewText"
          placeholder="请输入批语"
          :rows="4"
          required
          style="margin-top: 8px"
        />
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认</el-button>
      </div>
    </el-dialog>
    <div class="context" style="height: calc(100vh - 116px)">
      <el-tabs v-model="activeTabs" @tab-click="handleTypeClick">
        <el-tab-pane label="基本信息" name="base">
          <el-form
            :model="procurementScheme"
            label-width="120px"
            class="form-container"
            label-suffix=":"
          >
            <el-row class="custom-row" type="flex" justify="start">
              <el-col :span="8">
                <el-form-item label="方案名称" class="custom-form-item">
                  <span>{{ procurementScheme.procurementSchemeName }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="编号" class="custom-form-item">
                  <span>{{ procurementScheme.procurementSchemeCode }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="采购方式" class="custom-form-item">
                  <span>{{ procurementScheme.procurementTypeText }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="上限价(元)" class="custom-form-item">
                  <span>{{ procurementScheme.ceilingPriceText }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="采购人" class="custom-form-item">
                  <span>{{ procurementScheme.procurementOfficerName }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="交易标的物" class="custom-form-item">
                  <span>{{ procurementScheme.subjectMatterName }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8" v-if="procurementScheme.securityDeposit">
                <el-form-item label="保证金" class="custom-form-item">
                  <span>
                    {{
                      procurementScheme.isReceiveDeposit == 1
                        ? "收取"
                        : "不收取"
                    }}
                  </span>
                </el-form-item>
              </el-col>
              <el-col :span="8" v-if="procurementScheme.securityDeposit">
                <el-form-item label="保证金金额(元)" class="custom-form-item">
                  <span>{{ procurementScheme.securityDepositText }}</span>
                </el-form-item>
              </el-col>
              <el-col
                v-if="
                  procurementScheme.isReceiveDeposit == 1 &&
                  procurementScheme.securityDeposit
                "
                :span="8"
              >
                <el-form-item label="财务确认人员" class="custom-form-item">
                  <span>{{ procurementScheme.financeConfirmName }}</span>
                </el-form-item>
              </el-col>
              <el-col
                :span="8"
                v-if="
                  procurementScheme.subjectMatterType == 1 ||
                  procurementScheme.subjectMatterType == 2
                "
              >
                <el-form-item label="价格类型" class="custom-form-item">
                  <span>{{ procurementScheme.priceTypeText }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8" v-if="procurementScheme.subjectMatterType == 1">
                <el-form-item label="计数方式" class="custom-form-item">
                  <span>{{ procurementScheme.countingTypeText }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8" v-if="procurementScheme.subjectMatterType == 1">
                <el-form-item label="付款方式" class="custom-form-item">
                  <span>{{ procurementScheme.paymentTypeText }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="创建时间" class="custom-form-item">
                  <span>{{ procurementScheme.createTime }}</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <!-- 清单 -->
          <PageTitle title="清单" marginBottom="15px" />

          <el-table
            v-loading="loading"
            :data="contractPlanList"
            stripe
            highlight-current-row
            border
            size="small"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              label="合约规划名称"
              align="center"
              prop="contractPlanningName"
            />
            <el-table-column
              width="200"
              label="规划金额(含税)"
              align="right"
              prop="plannedAmountInclTaxText"
            />
            <el-table-column
              width="180"
              label="已发生规划金额(含税)"
              align="right"
              prop="incurredPlannedAmountText"
            />
            <el-table-column
              width="150"
              label="规划余量(元)"
              align="right"
              prop="planningBalanceText"
            />
            <el-table-column
              width="150"
              label="拟定招标方式"
              align="center"
              prop="biddingMethod"
            />
            <el-table-column label="清单" align="center" width="120">
              <template slot-scope="{ row }">
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handelInventory(row)"
                  >查看清单</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="招标文件" name="file">
          <!-- 招标文件 -->
          <!-- <div class="page-title">
              <span>招标文件</span>
            </div> -->
          <el-form
            :model="procurementSchemeBidding"
            label-width="120px"
            class="form-container"
            label-suffix=":"
          >
            <el-row class="custom-row">
              <el-col :span="24" class="custom-col">
                <el-form-item
                  label="计划投标截止时间"
                  label-width="140px"
                  class="custom-form-item"
                >
                  <span>{{ procurementSchemeBidding.bidDeadline }}</span>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row class="custom-row">
              <el-col :span="8" class="custom-col">
                <el-form-item
                  label="联系人"
                  label-width="140px"
                  class="custom-form-item"
                >
                  <span>{{ procurementSchemeBidding.bidContactPerson }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="custom-col">
                <el-form-item label="联系电话" class="custom-form-item">
                  <span>{{ procurementSchemeBidding.bidContactPhone }}</span>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="custom-col">
                <el-form-item label="联系邮箱" class="custom-form-item">
                  <span>{{ procurementSchemeBidding.bidContactEmail }}</span>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row class="custom-row">
              <el-col :span="8" class="custom-col">
                <el-form-item label="评分模板" class="custom-form-item">
                  <a
                    class="link-type"
                    @click="
                      handleCheck(
                        procurementSchemeBidding.evaluationTemplate.templateId
                      )
                    "
                  >
                    {{
                      procurementSchemeBidding.evaluationTemplate &&
                      procurementSchemeBidding.evaluationTemplate.templateName
                    }}
                  </a>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="custom-col">
                <el-form-item
                  label="招标文件"
                  label-width="140px"
                  class="custom-form-item"
                >
                  <a
                    class="link-type"
                    @click="
                      showTemplate(procurementSchemeBidding.biddingTemplate,'biddingTemplate')
                    "
                  >
                    {{
                      procurementSchemeBidding.biddingTemplate &&
                      procurementSchemeBidding.biddingTemplate.templateName
                    }}
                  </a>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="custom-col">
                <el-form-item label="合同模板" class="custom-form-item">
                  <a
                    class="link-type"
                    @click="
                      showTemplate(procurementSchemeBidding.contractTemplate,'contractTemplate')
                    "
                    href="javascript:;"
                  >
                    {{
                      procurementSchemeBidding.contractTemplate &&
                      procurementSchemeBidding.contractTemplate.templateName
                    }}
                  </a>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <!-- <el-tab-pane label="审批信息" name="approval">
          审批信息
          <div class="page-title">
              <span>审批信息</span>
            </div>
          <div class="form-body">
            <el-steps :active="approvalActive" class="step_item">
              <el-step :title="item.approveName" :description="i === 0 ? '发起人' : '审批岗位' + i"
                v-for="(item, i) in approveNodeInfos" :key="i"></el-step>
            </el-steps>
            <el-table v-loading="loading" :data="approveLists" border size="mini">
              <el-table-column label="序号" type="index" width="50" align="center" />
              <el-table-column label="姓名" align="center" prop="approvePerson" />
              <el-table-column label="审批时间" align="center" prop="approveTime" />
              <el-table-column label="审批状态" align="center" prop="operateType" />
              <el-table-column label="审批意见" align="center" prop="approvalOpinion" />
            </el-table>
          </div>
          <Roam
            v-if="procurementScheme.wfProcessId"
            :wfProcessId="procurementScheme.wfProcessId"
          />
        </el-tab-pane> -->
      </el-tabs>
      <!-- 选择项目合约规划 -->
      <el-dialog title="清单" :visible.sync="inventoryVisible" width="70%">
        <el-table
          v-loading="loading"
          :data="inventoryList"
          stripe
          highlight-current-row
          border
          size="small"
        >
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="拆分合约规划名称"
            v-if="isAll"
            width="200"
            prop="splitContractName"
            show-overflow-tooltip
          />
          <el-table-column
            label="拟签约合同拆包范围"
            v-if="isAll"
            width="200"
            prop="contractScope"
            show-overflow-tooltip
          />
          <!-- <el-table-column label="清单" align="center">
            <template slot-scope="inventory">
              <el-table
                size="small"
                :data="inventory.row.materialsLists"
                stripe
              >
                <el-table-column
                  label="序号"
                  type="index"
                  width="50"
                  align="center"
                />
                <el-table-column
                  label="清单编码"
                  width="200"
                  align="center"
                  prop="materialsCode"
                />
                <el-table-column
                  label="清单名称"
                  width="200"
                  align="center"
                  prop="materialsName"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="规格型号"
                  align="center"
                  prop="specification"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="计量单位"
                  align="center"
                  prop="unitMeasurement"
                />
                <el-table-column label="清单数量" align="center" prop="count" />
                <el-table-column
                  label="单价（含税）"
                  align="center"
                  prop="unitPriceInclTaxText"
                />
              </el-table>
            </template>
          </el-table-column> -->
          <el-table-column label="清单" align="center">
            <template slot-scope="inventory">
              <el-table
                size="small"
                :data="inventory.row.materialsLists"
                border
              >
                <el-table-column
                  label="序号"
                  type="index"
                  width="50"
                  align="center"
                />
                <el-table-column
                  label="清单编码"
                  width="150"
                  prop="materialsCode"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="清单名称"
                  width="150"
                  prop="materialsName"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="交易标的物"
                  width="150"
                  prop="subjectMatterName"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="规格型号"
                  prop="specification"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="计量单位"
                  align="center"
                  prop="unitMeasurement"
                />
                <el-table-column
                  v-if="isLease"
                  label="租赁方式"
                  prop="rentModeText"
                />
                <el-table-column
                  :label="isLease ? '工作量' : '清单数量'"
                  align="right"
                  prop="countText"
                />
                <el-table-column
                  label="基价(元)"
                  v-if="procurementScheme.priceType == 2"
                  align="right"
                  prop="basePriceText"
                />
                <el-table-column
                  label="单价(含税)"
                  v-else
                  align="right"
                  prop="unitPriceInclTaxText"
                  width="100"
                />
                <el-table-column
                  label="浮动价(元)"
                  v-if="procurementScheme.priceType == 2"
                  align="right"
                  prop="floatingPriceText"
                />
                <el-table-column
                  label="装卸费(元)"
                  v-if="procurementScheme.priceType == 2"
                  align="right"
                  prop="unloadingFeeText"
                />
                <el-table-column
                  v-if="isLease"
                  label="租赁时间"
                  align="right"
                  prop="rentTimeText"
                >
                  <template slot-scope="{ row }">
                    {{ row.rentMode == 3 ? "-" : row.rentTimeText }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isLease"
                  label="租赁数量"
                  align="right"
                  prop="rentQuantityText"
                >
                  <template slot-scope="{ row }">
                    {{ row.rentMode == 3 ? "-" : row.rentQuantityText }}
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>
    </div>
    <!-- 详细 -->
    <el-drawer
      :title="title"
      :visible.sync="openView"
      size="50%"
      direction="rtl"
      @close="handleClose"
    >
      <div class="drawer-rating">
        <el-form
          :model="formData"
          ref="form"
          label-position="right"
          label-width="110px"
          size="small"
          @submit.native.prevent
        >
          <PageTitle title="基本信息" />

          <el-row :gutter="40">
            <el-col :span="12" class="grid-cell">
              <el-form-item
                label="模板名称"
                prop="name"
                class="required label-right-align"
              >
                <template slot-scope>
                  {{ formData.name }}
                </template>
              </el-form-item>
            </el-col>
            <el-col :span="12" class="grid-cell">
              <el-form-item label="评分类型" prop="selectedTypes">
                <el-checkbox-group v-model="formData.selectedTypes">
                  <el-checkbox
                    v-for="dict in dict.type.mark_item_type"
                    :label="dict.value"
                    :key="dict.value"
                    disabled
                    >{{ dict.label }}</el-checkbox
                  >
                </el-checkbox-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="12" class="grid-cell" prop="createUser">
              <el-form-item
                label="维护人"
                prop="projectCode"
                class="required label-right-align"
              >
                <template slot-scope>
                  {{ formData.createUser }}
                </template>
              </el-form-item>
            </el-col>
            <!-- <el-col :span="8" class="grid-cell">
                <el-form-item label="使用单位" prop="useUnit">
                  <template slot-scope>
                    {{formData.useUnitName}}
                  </template>
                </el-form-item>
              </el-col> -->
          </el-row>

          <PageTitle title="评分模板内容" />

          <div
            v-for="(table, index) in formData.biddingMarkCategoryVOList"
            :key="index"
            class="table-section"
          >
            <div class="form-body">
              <el-row :gutter="40">
                <el-col :span="12" class="grid-cell">
                  <el-form-item
                    label="评分项类型"
                    class="required label-right-align"
                  >
                    <template slot-scope>
                      {{ table.itemTypeName }}
                    </template>
                  </el-form-item>
                </el-col>
                <el-col :span="12" class="grid-cell">
                  <el-form-item label="总分">
                    <template slot-scope>
                      {{ table.totalScore }}
                    </template>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="40">
                <el-col :span="24" class="grid-cell">
                  <el-form-item
                    label="评分项"
                    class="required label-right-align"
                  >
                    <el-table :data="table.biddingMarkItemVOList">
                      <!-- <el-table-column type="expand" v-if="table.biddingMarkItemVOList">
                        <template slot-scope="scope">
                          <div class="subItems">
                            <el-table :data="scope.row.subItems">
                              <el-table-column  prop="name"  label="子评分项名称">
                                <template slot-scope>
                                  {{ scope.row.subItems.name }}
                                </template>
                              </el-table-column>
                              <el-table-column  label="最低分">
                                <template slot-scope>
                                  {{ scope.row.subItems.minScore }}
                                </template>
                              </el-table-column>
                              <el-table-column  label="最高分">
                                <template slot-scope>
                                  {{ scope.row.subItems.maxScore }}
                                </template>
                              </el-table-column>
                            </el-table>
                          </div>
                        </template>
                      </el-table-column> -->
                      <el-table-column prop="name" label="评分项名称">
                        <template slot-scope="scope">
                          {{ scope.row.name }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="lowRange" label="最低分">
                        <template slot-scope="scope">
                          {{ scope.row.lowRange }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="highRange" label="最高分">
                        <template slot-scope="scope">
                          {{ scope.row.highRange }}
                        </template>
                      </el-table-column>
                    </el-table>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </div>
        </el-form>
      </div>

      <div slot="footer" class="drawer-footer">
        <el-button @click="openView = false">关闭</el-button>
      </div>
    </el-drawer>

    //预览文件弹窗
    <el-dialog
      :title="templateDialogTitle"
      :visible.sync="templateDialogVisible"
      width="80%"
    >
    <!-- <FileModule :attachmentId="templateAttachmentId" height="500px" /> -->
      <iframe allowfullscreen="true"
        :src= this.viewFileUrl
        width="100%"
        height="700px"
        frameborder="0"
      ></iframe>
    </el-dialog>

    <ApprovalForm
      :visible.sync="sanctionVisible"
      title="采购方案审批流程"
      :formModel="sanctionForm"
      :rejectNodeList="rejectNodeList"
      :nextCandidateList="nextCandidateList"
      :nextAppointable="nextAppointable"
      @update:visible="sanctionVisible = $event"
      @submit="handleSubmit"
    />
    <ApprovalDetailsDialog
      :visible.sync="calibrateVisible"
      title="采购方案审批流程详情"
      :activeStep="calibrateActive"
      :processInformationList="processInformationList"
      :approveLists="approveArr"
      :loading="calibrateLoading"
      @update:visible="calibrateVisible = $event"
    />
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { Base64 } from "js-base64";
import { getViewAttachmentURLByID } from "@/api/template/file";
import {
  getSchemeDetail,
  submitProcurementScheme,
  getListMaterials,
  cancellationProcurementScheme,
  withdrawalPlan,
  ViweProcurementSchemeFile,
} from "@/api/procurement/scheme";
import {
  getPermissionButton,
  postAuditProcess,
  getLoadTaskDef,
  getProcessLogList, getOrgByUserId,
} from "@/api/procurement/manage";
import { getRating } from "@/api/template/rating";
import FileModule from "@/components/FileModule/index.vue";
import BackButton from "@/components/BackButton/index.vue";
import Roam from "@/components/Roam";
import PageTitle from "@/components/PageTitle/index.vue";
import ApprovalForm from "@/components/Approval/approvalForm.vue";
import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
export default {
  name: "scheme-detail",
  dicts: ["purchase_type", "mark_item_type"],
  data() {
    return {
      loading: false,
      inventoryList: [],
      isSubmit: false,
      procurementScheme: {}, //基本信息
      procurementSchemeBidding: {}, // 招标文件
      approveNodeInfos: [], //审批人信息
      approveLists: [], //审批信息
      planMaterialsList: [], //拆分清单
      contractSplitIdList: [],
      skeletonLoading: true,
      param: "",
      isAll:false,
      activeTabs: "base",
      contractPlanList: [],
      inventoryVisible: false,
      formData: {},
      openView: false,
      title: "",
      units: [
        { value: "unit1", label: "单位一" },
        { value: "unit2", label: "单位二" },
        { value: "unit3", label: "单位三" },
      ],
      /* 合同模板联想文档预览 */
      templateDialogTitle: "",
      templateDialogVisible: false,
      templateAttachmentId: "",
      /* 招标文件模板联想文档预览 */
      templateBiddingDialogTitle: "",
      templateBiddingDialogVisible: false,
      templateBiddingAttachmentId: "",
      sanctionVisible: false,
      sanctionForm: {
        pass: true,
        rejectTaskKey: "",
        operateComment: "",
      },
      rejectNodeList: [],
      /* 下一步审批人列表 */
      nextCandidateList: [],
      /* 下一步审批人 */
      nextAppointable: false,
      purchaserId: "",
      exampleId: "",
      taskPresentId: "",
      calibrateVisible: false,
      calibrateActive: 1,
      processInformationList: [],
      approveArr: [],
      calibrateLoading: false,
      isShowButton: false,
      isShowApprovalDetails: false,
      submitDialogVisible: false,
      reviewText: "",
      selectedTag: null,
      tags: ["拟同意", "同意", "请修改, 再传至我处理", "阅"],
      //预览招标文件和合同模板的Url
      viewFileUrl:"",
    };
  },
  components: {
    FileModule,
    BackButton,
    Roam,
    PageTitle,
    ApprovalForm,
    ApprovalDetailsDialog,
  },
  computed: {
    approvalActive() {
      let count = this.approveNodeInfos.reduce(
        (pre, cur) => (cur.state === 1 ? pre + 1 : pre),
        0
      );
      console.log(count, "计算");
      return count;
    },
    ...mapGetters(["project"]),
    isLease() {
      return (
        this.procurementScheme.procurementPlanType == 2 ||
        this.procurementScheme.procurementPlanType == 3
      );
    },
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params));
    this.param = param;
    this.getSchemeDetail();
  },
  methods: {
    async getSchemeDetail() {
      try {
        const res = await getSchemeDetail(this.param);
        this.skeletonLoading = false;
        console.log(res, "详情");
        const {
          procurementScheme,
          procurementSchemeBidding,
          contractPlanList,
          approveNodeInfos,
          approveLists,
          contractSplitIdList,
        } = res.data;
        this.purchaserId = procurementScheme.id;
        this.exampleId = procurementScheme.wfProcessId;
        this.isShowApprovalDetails = procurementScheme.wfProcessId
          ? true
          : false;
        Object.assign(this, {
          procurementScheme,
          procurementSchemeBidding,
          contractPlanList,
          approveNodeInfos,
          approveLists,
          contractSplitIdList,
        });
        this.getPermissionButton();
      } catch (err) {
        console.log(err);
      }
    },
    handleTypeClick(tab) {
      this.activeTabs = tab.name;
    },
    async handelInventory(row) {
      this.inventoryVisible = true;
      const formData = {
        planId: row.planId,
        contractSpiltIdList: this.contractSplitIdList,
      };
      try {
        const res = await getListMaterials(formData);
        this.inventoryList = res.data;
        this.isAll = this.inventoryList.every(item => item.splitContractName && item.splitContractName!="null" && item.contractScope && item.contractScope!="null")
        console.log(res, "清单");
      } catch (err) {
        console.log(err);
      }
    },
    /** 查看按钮操作 */
    handleCheck(id) {
      getRating({ id }).then((res) => {
        res.data.selectedTypes = res.data.markCategoryDatailVOList.map((obj) =>
          String(obj.itemType)
        );
        res.data.useUnit = this.units[0].value;
        res.data.useUnitName = this.units[0].label;
        res.data.biddingMarkCategoryVOList =
          res.data.markCategoryDatailVOList.map((obj, index) => {
            res.data.markCategoryDatailVOList[index].itemType = String(
              obj.itemType
            );
            const itemTypeFind = this.dict.type.mark_item_type.find(
              (item) => item.value === String(obj.itemType)
            );
            res.data.markCategoryDatailVOList[index].itemTypeName =
              itemTypeFind.label;
            res.data.markCategoryDatailVOList[index].biddingMarkItemVOList =
              obj.markItemDetailVOList;
            return obj;
          });
        this.formData = res.data;
        this.openView = true;
        this.title = "查看评分模板";
      });
    },
    handleClose() {
      console.log("已关闭");
    },
    //展示预览文件方法
    async showTemplate(row,tmp) {
      this.templateDialogTitle = row.fileName + "预览";
      this.templateAttachmentId = row.attachmentId;
      this.templateDialogVisible = true;
      console.log("this.templateAttachmentId",this.templateAttachmentId);

      //获取附件的预览URL
      if (this.templateAttachmentId) {
        console.log('预览的Attachment ID:', this.templateAttachmentId);
        //获取文档中台的文档编辑URL
        try {
          // const res = await getViewAttachmentURLByID({ attachmentId: this.templateAttachmentId }); //无修订记录
          const res = await ViweProcurementSchemeFile({ attachmentId: this.templateAttachmentId }); //有修订记录
          this.viewFileUrl = res.data;
          console.log("viewFileUrl:",this.viewFileUrl);
        } catch (err) {
          console.log(err);
        }
      } else {
        console.warn('attachmentId 数据未正确加载');
      }
    },
    //修改按钮
    goUpdate() {
      const { procurementType, id } = this.procurementScheme;
      let param = Base64.encode(
        JSON.stringify({ id, procurementType, type: "update" })
      );
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/add-scheme/${param}`);
    },
    setTag(tag) {
      this.selectedTag = tag;
      this.reviewText = tag; // 将选中的标签文本填入文本框
    },
    //提交
    goSubmit() {
      this.submitDialogVisible = true;
      // this.$confirm("确定是否提交采购方案：" + procurementSchemeName, "提示", {
      //   confirmButtonText: "确定",
      //   cancelButtonText: "取消",
      //   type: "warning",
      // }).then(async () => {
      //   try {
      //     await submitProcurementScheme(id, detailUrl);
      //     this.$message.success("提交成功");
      //     this.getSchemeDetail();
      //   } catch (error) {}
      // });
    },
    async submitReview() {
      if (!this.reviewText.trim()) {
        ElMessage.error("批语不能为空");
        return;
      }
       const loading = this.$loading({
        lock: true,
        text: '正在提交...',
        background: 'rgba(0, 0, 0, 0.7)',
      });
      try {
        const { id } = this.procurementScheme;
        const detailUrl = this.$route.fullPath;
        await submitProcurementScheme({
          id,
          detailUrl,
          operateComment: this.reviewText,
        });
        this.$message.success("提交成功");
        this.getSchemeDetail();
        this.resetForm(); // 重置表单
      } catch (error) {
        this.$message.error("提交失败");
      }finally {
      loading.close();
     }
    },
    resetForm() {
      this.submitDialogVisible = false;
      this.reviewText = "";
      this.selectedTag = null;
    },
    /** 作废 **/
    goCancellation() {
      const { procurementSchemeName, id } = this.procurementScheme;
      this.$confirm("确定要作废采购方案：" + procurementSchemeName, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          await cancellationProcurementScheme(id);
          this.$message.success("作废成功");
          this.getSchemeDetail();
        } catch (error) {}
      });
    },
    // 撤回
    handelWithdrawalPlan() {
      const { procurementSchemeName, id } = this.procurementScheme;
      this.$confirm("是否确定撤回采购方案：" + procurementSchemeName, "提示", {
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
          withdrawalPlan(id)
            .then((res) => {
              if (res.code == 200) {
                this.$message.success("撤回成功");
              }
              this.getSchemeDetail();
              this.revokeLoding.close();
            })
            .catch((e) => this.revokeLoding.close());
        } catch (error) {}
      });
    },
    async getPermissionButton() {
      try {
        if (this.purchaserId && this.exampleId) {
          const res = await getPermissionButton({
            businessId: this.purchaserId,
            processId: this.exampleId,
          });
          this.rejectNodeList = res.data.completedTaskList;
          /* 下一步审批人列表 */
          this.nextCandidateList = res.data.nextCandidateList;
          /* 下一步审批人是否可选 */
          this.nextAppointable = res.data.nextAppointable;
          this.taskPresentId = res.data.curTaskId;
          this.isShowButton = res.data.auditable;
        }
      } catch (error) {}
    },
    handelSanction() {
      this.sanctionVisible = true;
      this.getPermissionButton();
    },
    handleSubmit() {
      const params = {
        ...this.sanctionForm,
        businessId: this.purchaserId,
        processId: this.exampleId,
        curTaskId: this.taskPresentId,
        processKey: "jiantou-zhaocai:{org}:ZHAOCAI_PROCUREMENT_SCHEME",
      };
      postAuditProcess(params).then(() => {
        this.$message.success("提交成功");
        this.sanctionVisible = false;
        this.getSchemeDetail();
      }).catch(error => {
        /* 关闭遮罩层 */
        this.$modal.closeLoading();
      });
    },
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        let params = {
          businessId: this.purchaserId,
          processId: this.exampleId,
        };
        let res = null;
        if (this.purchaserId && this.exampleId) {
          res = await getLoadTaskDef(params);
        }else{
          /* 未提交时查看流程执行流程，根据登录人id 获取流程分组 */
          res = await getOrgByUserId(this.$store.state.user.id);
          params = {
            processKey: "jiantou-zhaocai:"+res.data+":ZHAOCAI_PROCUREMENT_SCHEME",
            businessId: 8888888888,
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

        if (this.purchaserId && this.exampleId) {
          const response = await getProcessLogList(params);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
  },
  watch: {
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.$router.replace("/procurement/scheme");
        }
      },
    },
  },
};
</script>
<style lang="scss" scoped>
.form-body {
  padding: 20px;
}

.step_item {
  font-size: 14px;
  padding-bottom: 32px;
  margin-top: 16px;
}

::v-deep .step_item .el-step__title.is-finish {
  color: #2b4acb !important;
}

::v-deep .step_item .el-step__description.is-finish {
  color: #2b4acb !important;
}

::v-deep .step_item .el-step__head.is-finish {
  color: #2b4acb;
  border-color: #2b4acb;
}
.back-container {
  display: flex;
  flex-direction: row;
  justify-content: flex-end; /* 控制子元素之间的空间分布 */
  align-items: center; /* 垂直方向居中对齐 */
  gap: 10px; /* 子元素之间的间距 */
}

.button-group {
  display: flex;
  gap: 10px; /* 按钮之间的间距 */
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
.required {
  color: rgb(245, 108, 108);
  margin-right: 4px;
}
</style>
