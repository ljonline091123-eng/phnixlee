<template>
  <div class="app-container">
    <BackButton path="/vendor/vendor-base" title="供应商信息详情">
      <div v-if="activeName === 'base' || activeName === 'aptitude'">
        <el-button
          type="primary"
          size="mini"
          @click="openDialog('gradeVisible')"
          v-if="!vendor.isBlack && vendor.state != 1"
          v-hasPermi="['vendor:detail:edit']"
          >修改等级</el-button
        >
        <el-button
          type="primary"
          size="mini"
          @click="openDialog('blackVisible')"
          v-if="vendor.state != 1"
          v-hasPermi="['vendor:black:operation']"
          >{{ vendor.isBlack == 1 ? "移出黑名单" : "移入黑名单" }}</el-button
        >

        <el-button
          type="primary"
          size="mini"
          v-if="isShowButton"
          @click="handelSanction"
          >审批</el-button>
        <!-- <el-button
        type="primary"
        size="mini"
        @click="handelSanction"
        >审批</el-button> -->
        <!-- <el-button
          type="primary"
          size="small"
          v-if="vendor.state == 1"
          @click="handelCalibrationApproval"
          >审批详情</el-button
        > -->
        <el-button type="primary" size="mini" @click="handelCalibrationApproval"
          >审批详情</el-button
        >
      </div>
    </BackButton>
    <!-- 审批和审批详情 -->
    <ApprovalForm
      :visible.sync="sanctionVisible"
      :title="'供应商审批流程'"
      :formModel="sanctionForm"
      :rejectNodeList="rejectNodeList"
      :nextCandidateList="nextCandidateList"
      :nextAppointable="nextAppointable"
      @update:visible="sanctionVisible = $event"
      @submit="handleSubmit"
    />
    <ApprovalDetailsDialog
      :visible.sync="calibrateVisible"
      title="供应商审批流程详情"
      :activeStep="calibrateActive"
      :processInformationList="processInformationList"
      :approveLists="approveArr"
      :loading="calibrateLoading"
      @update:visible="calibrateVisible = $event"
    />
    <div class="context flex flex-column">
      <el-tabs v-model="activeName" @tab-click="handleClick">
        <el-tab-pane label="基本信息" name="base">
          <el-form label-width="150px" label-suffix=":">
            <el-row>
              <el-col :span="8">
                <el-form-item label="企业名称">{{
                  vendor.enterpriseName
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="统一社会信用代码">{{
                  vendor.socialCreditCode
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="注册资金(万元)">{{
                  vendor.registeredCapital
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="币种">{{
                  vendor.currencyCodeText
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="开户支行">{{
                  vendor.accountBranch
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="银行账号">{{
                  vendor.bankAccount
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="法定代表人">{{
                  vendor.legalRepresentative
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="法人联系方式">{{
                  vendor.legalPhone
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="法人身份证号码">{{
                  vendor.legalIdCard
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="增值税纳税人类型">{{
                  vendor.taxpayerTypeText
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="发票类型">{{
                  vendor.invoiceTypeText
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="企业性质">{{
                  vendor.enterpriseNatureText
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="企业所在省/市">{{
                  vendor.enterpriseProvinceName
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="企业所在地市">{{
                  vendor.enterpriseCityName
                }}</el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="企业联系电话">{{
                  vendor.contactPhone
                }}</el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="详细地址">{{
                  vendor.enterpriseAddress
                }}</el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="企业分类">{{
                  vendor.enterpriseTypeText
                }}</el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="经营范围">{{
                  vendor.businessScope
                }}</el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="企业简介">{{
                  vendor.enterpriseProfile
                }}</el-form-item>
              </el-col>
            </el-row>
          </el-form>
          <el-divider />
          <el-form label-width="150px" label-suffix=":">
            <el-row>
              <el-col :span="8"
                ><el-form-item label="联系人">{{
                  mainContact.contactName
                }}</el-form-item></el-col
              >
              <el-col :span="8"
                ><el-form-item label="联系人身份证">{{
                  mainContact.contactIdCard
                }}</el-form-item></el-col
              >
              <el-col :span="8"
                ><el-form-item label="联系人手机">{{
                  mainContact.contactPhone
                }}</el-form-item></el-col
              >
              <el-col :span="8"
                ><el-form-item label="联系人邮箱">{{
                  mainContact.contactEmail
                }}</el-form-item></el-col
              >
              <el-col :span="8"
                ><el-form-item label="联系人是否为法人">{{
                  mainContact.isLegalText
                }}</el-form-item></el-col
              >
            </el-row>
          </el-form>
          <el-divider />
          <el-form label-width="150px" label-suffix=":">
            <el-row>
              <el-col :span="8"
                ><el-form-item label="首次注册合作单位">{{
                  vendorState.firstCooperationCompanyName
                }}</el-form-item></el-col
              >
              <el-col :span="8"
                ><el-form-item label="当前类型">{{
                  vendorState.vendorClassText
                }}</el-form-item></el-col
              >
              <el-col :span="8"
                ><el-form-item :label="vendor.isBlack ? '限制期' : '等级'">{{
                  vendor.isBlack
                    ? vendor.blackBeginDate + " 至 " + vendor.blackEndDate
                    : vendorState.vendorLevelText
                }}</el-form-item></el-col
              >
            </el-row>
         
          </el-form>
              <el-divider />
                  <el-table
                  :data="bankList"
                  empty-text="暂无数据"
                  height="calc(100% - 132px)"
                  border

              >
                <el-table-column
                    label="序号"
                    type="index"
                    width="80"
                    align="center"
                />
                <el-table-column
                    label="支行名称"
                    align="center"
                    prop="openingBranch"
                />
                <el-table-column
                    label="银行名称"
                    prop="affiliatedBank"
                     align="center"
                    show-overflow-tooltip
                />
                  <el-table-column
                    label="银行帐号"
                    prop="bankAccount"
                     align="center"
                    show-overflow-tooltip
                />
               <el-table-column label="是否默认账户" align="center">
                  <template #default="{ row }">
                    {{row.status==1?'是':'否'}}
                  </template>
               </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane
          label="资质材料"
          name="aptitude"
          style="overflow: auto; height: auto"
        >
          <el-row>
            <el-col :span="6">
              <div class="img-box">
                <div class="img-title">营业执照</div>
                <template v-if="businessLicense.attachmentFileType">
                  <el-image
                    style="width: 200px; height: 200px"
                    :src="businessLicense.attachmentFileUrl"
                    :preview-src-list="businessLicense.srcList"
                  />
                </template>
                <template v-else>
                  <div style="width: 180px; height: 200px; color: #2b4acb; border: 1px solid #999999; ">
                    <div style="margin: 60px 0 0 15px;">
                        <el-button
                        size="medium"
                        type="primary"
                        @click="checkAttachment(businessLicense.attachmentFileUrl)"
                      >预览</el-button>
                    <el-button
                        type="primary"
                        size="medium"
                        @click="downAttachment(businessLicense.attachmentFileUrl, businessLicense.attachmentFileName)"
                        >下载</el-button>
                        </div>
                       <div style="font-size: 12px;margin: 30px 0 0 8px;">{{ businessLicense.attachmentFileName }}</div>
                    </div>
                </template>
<!--                <el-image-->
<!--                  style="width: 200px; height: 200px"-->
<!--                  :src="businessLicense.attachmentFileUrl"-->
<!--                  :preview-src-list="businessLicense.srcList"-->
<!--                >-->
<!--                </el-image>-->
                <div class="img-text">
                  有效期：{{ businessLicense.effectiveBeginDate }}-{{ businessLicense.effectiveEndDate }}
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="img-box">
                <div class="img-title">诚信合规材料</div>
                <template v-if="integrity.attachmentFileType">
                  <el-image
                    style="width: 200px; height: 200px"
                    :src="integrity.attachmentFileUrl"
                    :preview-src-list="integrity.srcList"
                  >
                  </el-image>
                </template>
                <!-- <template v-else>
                  <div style="width: 200px; height: 200px; color: #0c7fe1;"><br>{{ integrity.attachmentFileName }}</div>
                </template> -->
                 <template v-else>
                  <div style="width: 180px; height: 200px; color: #2b4acb; border: 1px solid #999999; ">
                    <div style="margin: 60px 0 0 15px;">
                        <el-button
                        size="medium"
                        type="primary"
                        @click="checkAttachment(integrity.attachmentFileUrl)"
                      >预览</el-button>
                    <el-button
                        type="primary"
                        size="medium"
                        @click="downAttachment(integrity.attachmentFileUrl, integrity.attachmentFileName)"
                        >下载</el-button>
                        </div>
                       <div style="font-size: 12px;margin: 30px 0 0 8px;">{{ integrity.attachmentFileName }}</div>
                    </div>
                </template>
<!--                <el-image-->
<!--                  style="width: 200px; height: 200px"-->
<!--                  :src="integrity.attachmentFileUrl"-->
<!--                  :preview-src-list="integrity.srcList"-->
<!--                >-->
<!--                </el-image>-->
                <div class="img-text">
                  有效期：{{ integrity.effectiveBeginDate }}-{{
                    integrity.effectiveEndDate
                  }}
                </div>
              </div>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <div class="img-box">
                <div class="img-title">法人授权书</div>
                <div style="display: flex">
                  <div
                    class="img-box-item"
                    v-for="(item, index) in legalAuthorizationList"
                    :key="index"
                    style="margin: 0px 60px 0px 0px"
                  >
                    <template v-if="item.attachmentFileType">
                      <el-image
                        style="width: 200px; height: 200px"
                        :src="item.attachmentFileUrl"
                        :preview-src-list="item.srcList"
                      >
                      </el-image>
                    </template>
                    <!-- <template v-else>
                      <div style="width: 200px; height: 200px; color: #0c7fe1;"><br>{{ item.attachmentFileName }}</div>
                    </template> -->
                  <template v-else>
                  <div style="width: 180px; height: 200px; color: #2b4acb; border: 1px solid #999999; ">
                    <div style="margin: 60px 0 0 15px;">
                        <el-button
                        size="medium"
                        type="primary"
                        @click="checkAttachment(item.attachmentFileUrl)"
                      >预览</el-button>
                    <el-button
                        type="primary"
                        size="medium"
                        @click="downAttachment(item.attachmentFileUrl, item.attachmentFileName)"
                        >下载</el-button>
                        </div>
                       <div style="font-size: 12px;margin: 30px 0 0 8px;">{{ item.attachmentFileName }}</div>
                    </div>
                </template>
<!--                    <el-image-->
<!--                      style="width: 200px; height: 200px"-->
<!--                      :src="item.attachmentFileUrl"-->
<!--                      :preview-src-list="item.srcList"-->
<!--                    >-->
<!--                    </el-image>-->
                    <div class="img-text">
                      有效期：{{ item.effectiveBeginDate }}-{{
                        item.effectiveEndDate
                      }}
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <div class="img-title">相关资质</div>
              <div style="display: flex">
                <div
                  class="img-box"
                  v-for="(item, index) in relevantCertificationList"
                  :key="index"
                  style="margin-right: 20px"
                >
                  <template v-if="item.attachmentFileType">
                    <el-image
                      style="width: 200px; height: 200px"
                      :src="item.attachmentFileUrl"
                      :preview-src-list="item.srcList"
                    >
                    </el-image>
                  </template>
                  <!-- <template v-else>
                    <div style="width: 200px; height: 200px; color: #0c7fe1;"><br>{{ item.attachmentFileName }}</div>
                  </template> -->
                <template v-else>
                  <div style="width: 180px; height: 200px; color: #2b4acb; border: 1px solid #999999; ">
                    <div style="margin: 60px 0 0 15px;">
                        <el-button
                        size="medium"
                        type="primary"
                        @click="checkAttachment(item.attachmentFileUrl)"
                      >预览</el-button>
                    <el-button
                        type="primary"
                        size="medium"
                        @click="downAttachment(item.attachmentFileUrl, item.attachmentFileName)"
                        >下载</el-button>
                        </div>
                       <div style="font-size: 12px;margin: 30px 0 0 8px;">{{ item.attachmentFileName }}</div>
                    </div>
                </template>
<!--                  <el-image-->
<!--                    style="width: 200px; height: 200px"-->
<!--                    :src="item.attachmentFileUrl"-->
<!--                    :preview-src-list="item.srcList"-->
<!--                  >-->
<!--                  </el-image>-->
                  <div class="img-text">
                    有效期：{{ item.effectiveBeginDate }}-{{
                      item.effectiveEndDate
                    }}
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </div>
    <!-- 修改供应商等级 -->
    <el-dialog
      title="供应商修改等级"
      :visible.sync="gradeVisible"
      highlight-current-row
      width="30%"
      @closed="gradeColsed"
    >
      <el-row :gutter="10">
        <el-col :span="18">
          <el-form :model="gradeForm" :rules="gradeRules" ref="gradeRef">
            <el-form-item
              label="供应商名称："
              prop="enterpriseName"
              label-width="140px"
            >
              <el-input
                v-model="gradeForm.enterpriseName"
                autocomplete="off"
                disabled
              ></el-input>
            </el-form-item>
            <el-form-item
              label="合作单位："
              prop="firstCooperationCompanyName"
              label-width="140px"
            >
              <el-input
                v-model="gradeForm.firstCooperationCompanyName"
                autocomplete="off"
                disabled
              ></el-input>
            </el-form-item>
            <el-form-item
              label="入库类别："
              prop="vendorClass"
              label-width="140px"
            >
              <el-select
                v-model="gradeForm.vendorClass"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option
                  v-for="dict in dict.type.vendor_class"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item
              label="供应商等级："
              prop="vendorLevel"
              label-width="140px"
            >
              <el-select
                v-model="gradeForm.vendorLevel"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option
                  v-for="dict in dict.type.vendor_level"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item
              label="操作人员："
              prop="operator"
              label-width="140px"
            >
              <el-input
                v-model="gradeForm.operator"
                autocomplete="off"
                disabled
              ></el-input>
            </el-form-item>
            <el-form-item
              label="附件："
              prop="attachmentList"
              label-width="140px"
              class="uploadItem"
            >
              <el-button size="small" type="primary" @click="showSecretTips('gradeUpload')" style="margin-top: 8px;">点击上传</el-button>
              <el-upload
                class="upload-demo"
                action="/dev-api/file/upload"
                multiple
                :limit="1"
                :on-success="gradeSuccess"
                ref="gradeUpload"
              >

              </el-upload>
            </el-form-item>
            <el-form-item
            label="批注："
            prop="operateComment"
            label-width="140px"
            >
            <el-tag @click="setOperateComment('拟同意')"  type="info" size="mini">拟同意</el-tag>
            <el-tag @click="setOperateComment('同意')"   style="margin-left: 5px;"  type="info" size="mini">同意</el-tag>
            <el-tag @click="setOperateComment('请修改，再传至我处')"   style="margin-left: 5px;"  type="info" size="mini">请修改，再传至我处</el-tag>
            <el-tag @click="setOperateComment('阅')"   style="margin-left: 5px;"  type="info" size="mini">阅</el-tag>
            </el-form-item>
            <el-form-item
            label-width="140px"
            >
              <el-input
                  type="textarea"
                  :rows="5"
                  placeholder="请输入内容"
                  v-model="gradeForm.operateComment">
                </el-input>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="6">
          <el-button type="text" @click="lockPerformance"
            >查看履约评价</el-button
          >
        </el-col>
      </el-row>

      <div slot="footer" class="dialog-footer">
        <el-button
          @click="gradeVisible = false"
          style="width: 100px"
          size="small"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="handleGrade('gradeRef')"
          style="width: 100px"
          size="small"
          >提 交</el-button
        >
      </div>
    </el-dialog>
    <!-- 修改供应商黑名单 -->
    <el-dialog
      title="黑名单操作"
      :visible.sync="blackVisible"
      class="dialogClass"
      width="30%"
      @closed="blackColsed"
    >
      <el-form :model="blackForm" :rules="blackRules" ref="blacKRef">
        <el-form-item
          label="供应商名称："
          prop="enterpriseName"
          label-width="140px"
        >
          <el-input
            v-model="blackForm.enterpriseName"
            autocomplete="off"
            disabled
          ></el-input>
        </el-form-item>
        <el-form-item
          label="合作单位："
          prop="firstCooperationCompanyName"
          label-width="140px"
        >
          <el-input
            v-model="blackForm.firstCooperationCompanyName"
            autocomplete="off"
            disabled
          ></el-input>
        </el-form-item>
        <el-form-item label="操作类别：" prop="blackState" label-width="140px">
          <el-select
            v-model="blackForm.blackState"
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option label="移出黑名单" value="0" v-if="vendor.isBlack" />
            <el-option label="移入黑名单" value="1" v-else />
          </el-select>
        </el-form-item>
        <el-form-item
          label="限制期："
          prop="date"
          label-width="140px"
          v-if="!vendor.isBlack"
        >
          <el-date-picker
            v-model="blackForm.date"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item label="操作人员：" prop="operator" label-width="140px">
          <el-input
            v-model="blackForm.operator"
            autocomplete="off"
            disabled
          ></el-input>
        </el-form-item>
        <el-form-item label="附件：" prop="attachmentList" label-width="140px" class="uploadItem">
          <el-button size="small" type="primary" @click="showSecretTips('blackUpload')" style="margin-top: 8px;">点击上传</el-button>
          <el-upload
            class="upload-demo"
            action="/dev-api/file/upload"
            multiple
            :limit="1"
            :on-success="blackSuccess"
            ref="blackUpload"
          >

          </el-upload>
        </el-form-item>
        <el-form-item
        label="批注："
        prop="operateComment"
        label-width="140px"
        >
        <el-tag @click="setOperateComment1('拟同意')"  type="info" size="mini">拟同意</el-tag>
        <el-tag @click="setOperateComment1('同意')"   style="margin-left: 5px;"  type="info" size="mini">同意</el-tag>
        <el-tag @click="setOperateComment1('请修改，再传至我处')"   style="margin-left: 5px;"  type="info" size="mini">请修改，再传至我处</el-tag>
        <el-tag @click="setOperateComment1('阅')"   style="margin-left: 5px;"  type="info" size="mini">阅</el-tag>
        </el-form-item>
        <el-form-item
        label-width="140px"
        >
          <el-input
              type="textarea"
              :rows="5"
              placeholder="请输入内容"
              v-model="blackForm.operateComment">
            </el-input>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button
          @click="blackVisible = false"
          style="width: 100px"
          size="small"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="handleBlackForm('blacKRef')"
          style="width: 100px"
          size="small"
          >确 定</el-button
        >
      </div>
    </el-dialog>
    <!-- 供应商履约 -->
    <el-dialog
      title="供应商履约评价"
      :visible.sync="performanceVisible"
      class="dialogClass"
      width="50%"
    >
      <el-table
        v-loading="performanceLoading"
        :data="performanceList"
        stripe
        show-summary
        highlight-current-row
        border
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column label="供应商名称" prop="vendorName" />
        <el-table-column label="合作单位" prop="cooperator" />
        <el-table-column label="合同名称" prop="agreementName" />
        <el-table-column label="优" align="center" prop="excellentCount" />
        <el-table-column label="良" align="center" prop="goodCount" />
        <el-table-column label="合格" align="center" prop="passCount" />
        <el-table-column label="差" align="center" prop="poorCount" />
      </el-table>
    </el-dialog>
    <!-- 预览弹窗 -->
    <el-dialog
      :show-close="true"
      :visible.sync="dialogVisible"
      title="附件"
      modal
      center
      :append-to-body="false"
      destroy-on-close
    >
      <!-- 直接用iframe嵌套pdf预览模式 "#toolbar=0"是为了隐藏pdf的按钮  -->
      <div class="dialogtext">
        <iframe width="800"  height="1200"   :src="this.iframeUrls + '#toolbar=0'" />
      </div>
    </el-dialog>

  </div>
</template>
<script>
import {
  getVendorDetail,listBankAccountContact,
  updateVendorLevel,
  saveVendorLevel,
  updateBlackState,
  saveVendorBlack,
  listVendorPerformance,
} from "@/api/vendor/vendor";
import { Base64 } from "js-base64";
import BackButton from "@/components/BackButton/index.vue";
import {
  getPermissionButton,getPermissionButtonVendor,
  postAuditProcess,postAuditProcessVendor,
  getLoadTaskDef,getLoadTaskDefVendor,
  getProcessLogList,
} from "@/api/procurement/manage";
import ApprovalForm from "@/components/Approval/approvalForm.vue";
import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
import {showSecretRelatedTips} from "@/utils/MyUtils";
export default {
  name: "vendor-detail",
  dicts: ["vendor_class", "vendor_level"],
  data() {
    return {
      dialogVisible:false,
      iframeUrls:'',
      activeName: "base",
      vendor: {},
      vendorState: {},
      mainContact: {},
      vendorBlack: "",
      //资质材料
      businessLicense: {}, //营业执照
      integrity: {}, //诚信合规材料
      legalAuthorizationList: [], //法人授权书
      bankList:[],
      relevantCertificationList: [], //相关资质
      //供应商等级
      gradeVisible: false,
      gradeRules: {
        vendorClass: [{ required: true, message: "请选择供应商类型" }],
        vendorLevel: [{ required: true, message: "请选择供应商等级" }],
        attachmentList: [{ required: true, message: "请上传附件" }],
        operateComment: [{ required: true, message: "请填写批注" }],
      },
      gradeForm: {operateComment:'请审批'},
      //黑名单操作
      blackVisible: false,
      blackForm: {operateComment:'请审批'},
      blackRules: {
        blackState: [{ required: true, message: "请选择操作类别" }],
        date: [{ required: true, message: "请选择限制期" }],
        attachmentList: [{ required: true, message: "请上传附件" }],
        operateComment: [{ required: true, message: "请填写批注" }],
      },
      //履约
      performanceVisible: false,
      performanceList: [],
      performanceLoading: false,
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
    };
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params));
    console.log("param613" + param);
    this.param = param;
    this.getVendorDetail();
    this.listBankAccountContactFn();
  },
  methods: {
     downAttachment(file,uelFileName) {
        const a = document.createElement("a")
        a.href = file
        a.download = uelFileName
        a.target = "_blank"
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
    },
    checkAttachment(url){
       this.iframeUrls=url,
       this.dialogVisible=true
    },
    ifPdf(url){
      return url.toLowerCase().endsWith(".pdf")
    },
    showSecretTips(type) {
      showSecretRelatedTips(()=>{
        this.$refs[type].$refs['upload-inner'].handleClick()
      })
    },
    setOperateComment(type){
      this.gradeForm.operateComment=type
    },

    setOperateComment1(type){
      this.blackForm.operateComment=type
    },
    openDialog(type) {
      this[type] = true;
      this[type === "gradeVisible" ? "gradeForm" : "blackForm"].operator =
        this.$store.state.user.nickname;
      this[type === "gradeVisible" ? "gradeForm" : "blackForm"].enterpriseName =
        this.vendor?.enterpriseName || "";
      this[
        type === "gradeVisible" ? "gradeForm" : "blackForm"
      ].firstCooperationCompanyName =
        this.vendorState?.firstCooperationCompanyName || "";
      if (type === "gradeVisible") {
        this.$set(
          this.gradeForm,
          "vendorClass",
          this.vendorState?.vendorClass.toString() || ""
        );
        this.$set(
          this.gradeForm,
          "vendorLevel",
          this.vendorState?.vendorLevel.toString() || ""
        );
      }
    },
    async listBankAccountContactFn() {
          const res = await listBankAccountContact(this.param);
           this.bankList=res.data.rows
     },
    async getVendorDetail() {
      try {
        debugger
        const res = await getVendorDetail(this.param);
        this.purchaserId = res.data.vendor.id;
        this.exampleId = res.data.vendor.wfProcessId;
        this.vendor = res.data.vendor;
        this.vendorBlack = res.data.vendorBlack || {};
        this.vendorState = res.data.vendorState;
        this.mainContact = res.data.mainContact;
        this.businessLicense = res.data.certificationList?.businessLicense
          ?.attachmentFileUrl
          ? {
              ...res.data.certificationList.businessLicense,
              srcList: [
                res.data.certificationList.businessLicense.attachmentFileUrl,
              ],
            }
          : {};
        this.businessLicense.attachmentFileType = this.ifPdf(this.businessLicense.attachmentFileUrl) ? false : true
        this.integrity = res.data.certificationList?.integrity
          ? {
              ...res.data.certificationList.integrity,
              srcList: [res.data.certificationList.integrity.attachmentFileUrl],
            }
          : {};
        this.integrity.attachmentFileType = this.ifPdf(this.integrity.attachmentFileUrl) ? false : true
        this.legalAuthorizationList = res.data.certificationList
          ?.legalAuthorizationList?.length
          ? res.data.certificationList.legalAuthorizationList.map((item) => ({
              ...item,
              srcList: [item.attachmentFileUrl],
            }))
          : [];
        this.legalAuthorizationList.forEach((item,index)=>{
          item.attachmentFileType = this.ifPdf(item.attachmentFileUrl) ? false : true
        })
        this.relevantCertificationList = res.data.certificationList
          ?.relevantCertificationList?.length
          ? res.data.certificationList.relevantCertificationList.map(
              (item) => ({ ...item, srcList: [item.attachmentFileUrl] })
            )
          : [];
        this.relevantCertificationList.forEach((item,index)=>{
          item.attachmentFileType = this.ifPdf(item.attachmentFileUrl) ? false : true
        })
        console.log(res, "res-res");
        if (this.vendor.state === 1) {
          // this.getPermissionButton();
          this.getPermissionButtonVendor();
        }
      } catch (err) {
        console.log(err);
      }
    },
    handleClick(val) {
      this.activeName = val.name;
    },
    // 修改供应商等级
    handleGrade(formName) {
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          console.log(valid, "valid");
          const { id } = this.vendor;
          const { vendorClass, vendorLevel, attachmentList,operateComment } = this.gradeForm;
          const formData = {
            id,
            vendorClass,
            vendorLevel,
            attachmentList: [attachmentList],
            operateComment
          };
          console.log(formData, "formData");
          try {
            const res = await saveVendorLevel(formData);
            this.gradeVisible = false;
            this.$message.success("操作成功");
            this.getVendorDetail();
          } catch (err) {
            console.log(err);
          }
        }
      });
    },
    //查看履约评价
    async lockPerformance() {
      this.performanceVisible = true;
      this.performanceLoading = true;
      try {
        const res = await listVendorPerformance(this.vendor.id);
        this.performanceList = res.data;
        console.log(res, "履约");
      } catch (err) {
        console.log(err);
      }
      this.performanceLoading = false;
    },
    gradeSuccess(res) {
      const { url, name } = res.data;
      this.$set(this.gradeForm, "attachmentList", {
        fileUrl: url,
        fileName: name,
      });
      this.$refs["gradeRef"].clearValidate("attachmentList");
    },
    blackSuccess(res) {
      const { url, name } = res.data;
      this.$set(this.blackForm, "attachmentList", {
        fileUrl: url,
        fileName: name,
      });
      this.$refs["blacKRef"].clearValidate("attachmentList");
    },
    //加入黑名单
    handleBlackForm(formName) {
      console.log(this.blackForm, "blackForm");

      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          this.$modal.loading("请稍候...");
          const { id } = this.vendor;
          const { blackState, date, attachmentList, operateComment} = this.blackForm;
          const formData = {
            id,
            blackState,
            blackBeginDate: date?.length && date[0],
            blackEndDate: date?.length && date[1],
            attachmentList: [attachmentList],
            operateComment
          };
          try {
            // const res = await updateBlackState(formData);
            const res1 = await saveVendorBlack(formData);
            this.$modal.closeLoading();
            this.$message.success("操作成功");
            this.blackVisible = false;
            this.getVendorDetail();
          } catch (err) {
            this.$modal.closeLoading();
            console.log(err);
          }
        }
      });
    },
    blackColsed() {
      this.$refs["blacKRef"].resetFields();
      this.$refs.blackUpload.clearFiles();
      this.blackForm = {};
    },
    gradeColsed() {
      this.$refs["gradeRef"].resetFields();
      this.$refs.gradeUpload.clearFiles();
      this.gradeForm = {};
    },
    // 审批逻辑
    async getPermissionButton() {
      try {
        if (this.purchaserId && this.exampleId) {
          const res = await getPermissionButton({
            businessId: this.purchaserId, //联系人id
            processId: this.exampleId, //流程id
          });
          this.rejectNodeList = res.data.completedTaskList;
          this.taskPresentId = res.data.curTaskId;
          this.isShowButton = res.data.auditable;
        }
      } catch (error) {}
    },
        // 供应商审批逻辑
    async getPermissionButtonVendor() {
      try {
        if (this.purchaserId && this.exampleId) {
          const res = await getPermissionButtonVendor({
            businessId: this.purchaserId, //联系人id
            processId: this.exampleId, //流程id
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
      //  this.getPermissionButton();
    },
    handleSubmit() {
      this.$modal.loading("请稍候...");
      //修改
      if (this.vendor.processType == 2) {
        this.processKey = "jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_UPDATEINFO";
      } else if (this.vendor.processType == 1) {
        //注册
        this.processKey = "jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_REGISTER";
      } else if (this.vendor.processType == 3) {
        //黑名单
        this.processKey =
          "jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK";
      } else if (this.vendor.processType == 4) {
        //修改等级
        this.processKey = "jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_UPDATE_LEVEL";
      }
      const params = {
        ...this.sanctionForm,
        businessId: this.purchaserId,
        processId: this.exampleId,
        curTaskId: this.taskPresentId,
        processKey: this.processKey,
        // processKey: "jiantou-zhaocai:{org}:ZHAOCAI_VENDOR_REGISTER",
      };
      postAuditProcessVendor(params).then(() => {
        this.$message.success("提交成功");
        // this.$router.go(-1);
        this.$modal.closeLoading();
        this.sanctionVisible = false;
        if(this.vendor.processType == 1){
          this.getPermissionButtonVendor()
        }else{
          this.getPermissionButton();
        }

        this.getVendorDetail();
      }).catch(error => {
        /* 关闭遮罩层 */
        this.$modal.closeLoading();
      });
    },
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        const params = {
          businessId: this.vendor.changeId?this.vendor.changeId:this.purchaserId,
          processId: this.exampleId,
        };
        const getProcessLogListParams = {
          businessId: "",
          processId: this.exampleId,
        };
        if (this.purchaserId && this.exampleId) {
          const res = await getLoadTaskDefVendor(params);
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
          const response = await getProcessLogList(getProcessLogListParams);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
  },
  components: {
    BackButton,
    ApprovalForm,
    ApprovalDetailsDialog,
  },
};
</script>
<style scoped lang="scss">
::v-deep .el-form-item.uploadItem {
  .el-form-item__content {
    line-height: 0;
  }
}
.img-box {
  display: flex;
  width: 200px;
  display: flex;
  flex-wrap: wrap;
  .img-text {
    padding-top: 10px;
    color: #666;
    text-align: left;
    width: 100%;
  }
}
.img-title {
  line-height: 40px;
  position: relative;
  padding-left: 10px;
  margin-top: 15px;
  &::before {
    content: "";
    width: 3px;
    height: 14px;
    background: #2b4acb;
    position: absolute;
    left: 0;
    top: 50%;
    -webkit-transform: translateY(-50%);
    transform: translateY(-50%);
  }
}
.el-form-item {
  margin-bottom: 5px !important;
}
.dialogtext {
  width: 100% !important;
  height: 100% !important;
}
</style>
