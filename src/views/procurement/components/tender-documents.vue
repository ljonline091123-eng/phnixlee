<template>
  <div class="app-container">
    <div class="context" style="height: calc(100vh - 116px)">
      <el-form
        :model="formData"
        ref="form"
        :rules="rules"
        label-position="right"
        label-width="110px"
        size="medium"
        @submit.native.prevent
      >
        <PageTitle title="基本信息" marginBottom="15px">
          <div class="page-title-right" style="flex: 1;justify-content: flex-end">
<!--          为1的时候展示这个截止时间并且需要显示时-->
            <div v-if="noticeDetail.tenderNotice && noticeDetail.tenderNotice.noticeStatus === 1 && needTime"   style="flex: 1;display: flex;justify-content: center;margin-right: 8px;font-size: 13px;color:#ff0000;font-weight: normal">
              <div style="margin-right: 80px">
                <div style="margin-right: 8px">方案：{{ scheme && scheme.procurementSchemeName  }}</div>
                <!--                noticeDetail.tenderNotice && noticeDetail.tenderNotice.applyTime-->
                <div>当前北京时间：{{formatNowDate}}</div>
              </div>

              <div>
                <div>投标截止时间：{{formatApplyTime}}</div>
                <span>投标截止时间倒计时：{{timeDifferenceElement}}</span>
              </div>

            </div>
            <div>
              <el-button
                type="success"
                icon="el-icon-plus"
                @click="setVendor"
                size="small"
                v-if="setVendorShow"
                :disabled="
                noticeDetail.purchaseOfficer === undefined
                  ? false
                  : !noticeDetail.purchaseOfficer
              "
              >设置供应商范围</el-button
              >
            </div>

            <template v-if="updateTotal > 0 && answerAndChangeShow">
              <el-badge
                :value="updateTotal"
                v-if="formData.id"
                class="page-title-right-item"
              >
                <el-button
                  type="primary"
                  size="small"
                  @click="setModify"
                  :disabled="
                    noticeDetail.purchaseOfficer === undefined
                      ? false
                      : !noticeDetail.purchaseOfficer
                  "
                  >变更</el-button
                >
              </el-badge>
            </template>
            <div class="page-title-right-item" v-else-if="answerAndChangeShow">
              <el-button
                type="primary"
                size="small"
                @click="setModify"
                :disabled="
                  noticeDetail.purchaseOfficer === undefined
                    ? false
                    : !noticeDetail.purchaseOfficer
                "
                >变更</el-button
              >
            </div>
            <template v-if="QATotal > 0 && answerAndChangeShow">
              <el-badge :value="QATotal" class="page-title-right-item">
                <el-button
                  type="primary"
                  size="small"
                  @click="setQA"
                  :disabled="
                    noticeDetail.purchaseOfficer === undefined
                      ? false
                      : !noticeDetail.purchaseOfficer
                  "
                  >答疑</el-button
                >
              </el-badge>
            </template>
            <div class="page-title-right-item" v-else-if="answerAndChangeShow">
              <el-button
                type="primary"
                size="small"
                @click="setQA"
                :disabled="
                  noticeDetail.purchaseOfficer === undefined
                    ? false
                    : !noticeDetail.purchaseOfficer
                "
                >答疑</el-button
              >
            </div>
            <el-button
              type="primary"
              size="small"
              style="margin-left: 8px"
              @click="submitForm('form')"
              :loading="isSubmit"
              v-if="applyButtonShow || isSubmit"
              :disabled="
                noticeDetail.purchaseOfficer === undefined
                  ? false
                  : !noticeDetail.purchaseOfficer
              "
              >{{ isSubmit ? "提交中..." : "发布" }}</el-button
            >
          </div>
        </PageTitle>

        <el-row :gutter="40">
          <el-col :span="8" class="grid-cell">
            <el-form-item
              label="联系人"
              prop="contact"
              class="required label-right-align"
            >
              <el-input
                v-model="formData.contact"
                type="text"
                clearable
                placeholder="请输入联系人"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item
              label="联系电话"
              prop="phone"
              class="required label-right-align"
            >
              <el-input
                type="text"
                clearable
                v-model="formData.phone"
                placeholder="请输入联系电话"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item
              label=" 联系邮箱"
              prop="email"
              class="required label-right-align"
            >
              <el-input
                v-model="formData.email"
                type="text"
                clearable
                placeholder="请输入联系邮箱"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item
              label="招标文件"
              prop="projectHierarchy"
              class="required label-right-align"
            >
              <a
                :href="scheme.biddingTemplate && scheme.biddingTemplate.fileUrl"
                class="link-type"
                target="_blank"
                >{{
                  scheme.biddingTemplate && scheme.biddingTemplate.fileName
                }}</a
              >
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item
              label=" 付款方式"
              prop="email"
              class="required label-right-align"
            >
              <el-select
                style="width: 100%"
                :disabled="formData.paymentType"
                v-model="formData.paymentType"
                placeholder="请选择付款方式"
              >
                <el-option
                  v-for="dict in paymentTypeList"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item
              label="投标截止时间"
              prop="applyTime"
              class="required label-right-align"
            >
              <el-date-picker
                v-model="formData.applyTime"
                type="datetime"
                style="width: 100%"
                placeholder="选择日期"
                :picker-options="expireTimeOption"
                value-format="yyyy-MM-dd HH:mm:ss"
                :disabled="formData.id ? true : false || isSubmit"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="selectVendorsInfo.length && scheme.procurementType != 1">
          <el-form-item
            label="选择供应商"
          >
            <div>
              <el-tag
                v-for="item in selectVendorsInfo" :key="item.id" type="info" style="margin-right: 8px;"
                :closable="!(formData.id ? true : false || isSubmit)" @close="deleteSelectedVendor(item)"
              >
                <span>{{ item.enterpriseName || item.vendorName }}</span>
              </el-tag>
            </div>
          </el-form-item>
        </el-row>

        <PageTitle title="招标文件内容" marginBottom="15px" />
        <!-- <FileModule
          :attachmentId="
            scheme.biddingTemplate && scheme.biddingTemplate.attachmentId
          "
          height="500px"
        /> -->
        <iframe
          v-if="scheme.biddingTemplate"
          :src= this.viewFileUrl
          width="100%"
          height="500px"
          frameborder="0"
        ></iframe>
      </el-form>
      <!-- 选择供应商 -->
      <el-dialog
        title="选择供应商"
        :visible.sync="vendorVisible"
        width="80%"
        @closed="closedVendor"
      >
        <el-row :gutter="20">
          <!--省市区-->
          <el-col :span="4" :xs="24">
            <div class="head-container">
              <el-tree
                :data="addressOptions"
                :props="addressProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                node-key="divisionCode"
                accordion
                highlight-current
                @node-click="handleAddressNodeClick"
                v-loading="addressLoading"
              />
            </div>
          </el-col>
          <!--供应商分类-->
          <el-col :span="6" :xs="24">
            <div class="head-container">
              <el-tree
                :data="categoryOptions"
                :props="categoryProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                node-key="id"
                highlight-current
                @node-click="handleCategoryNodeClick"
                v-loading="categoryLoading"
              />
            </div>
          </el-col>
          <el-col :span="14" :xs="24">
            <el-form
              :model="vendorQuery"
              ref="vForm"
              :rules="rules"
              label-position="left"
              label-width="80px"
              size="small"
              @submit.native.prevent
            >
              <el-row :gutter="10">
                <el-col :span="7" class="grid-cell">
                  <el-form-item
                    label="供应商名称"
                    label-width="90px"
                    prop="enterpriseName"
                    class="label-right-align"
                  >
                    <el-input
                      v-model="vendorQuery.enterpriseName"
                      type="text"
                      clearable
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="7" class="grid-cell">
                  <el-form-item
                    label="注册资金(万元)"
                    label-width="110px"
                    prop="registeredCapital"
                    class="label-right-align"
                  >
                    <el-input
                      v-model="vendorQuery.registeredCapital"
                      type="text"
                      clearable
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="7" class="grid-cell">
                  <el-form-item
                    label="供应商等级"
                    label-width="90px"
                    prop="vendorLevel"
                    class="label-right-align"
                  >
                    <el-select
                      v-model="vendorQuery.vendorLevel"
                      placeholder="请选择"
                      style="width: 100%"
                      clearable
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
                </el-col>
                <el-col :span="3" class="grid-cell">
                  <div class="static-content-item">
                    <el-button
                      type="primary"
                      icon="el-icon-search"
                      size="small"
                      @click="searchVendor"
                      >查询</el-button
                    >
                  </div>
                </el-col>
              </el-row>
            </el-form>
            <el-table
              v-loading="vendorLoading"
              :data="vendorList"
              border
              stripe
              :row-key="selRowKey"
              @selection-change="handleSelectionVendor"
              :header-cell-style="disableSelectAllStyle"
              ref="multipleTable"
            >
              <el-table-column
                type="selection"
                width="55"
                :reserve-selection="true"
                :row-key="(row) => row.id"
                :selectable="() => scheme.procurementType !== 1"
              />
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column label="供应商名称" prop="enterpriseName" />
            </el-table>
            <div class="total-num" v-show="vendorTotal > 0">
              <span class="num-box"
                >已选{{
                  scheme.procurementType === 1 ? vendorTotal : vendors.length
                }}个供应商</span
              >
              <pagination
                :total="vendorTotal"
                :page.sync="vendorQuery.pageNumber"
                :limit.sync="vendorQuery.pageSize"
                @pagination="getVendorList"
              />
            </div>
          </el-col>
        </el-row>
        <div slot="footer" class="dialog-footer">
          <el-button
            @click="vendorVisible = false"
            style="width: 100px"
            size="small"
            >取 消</el-button
          >
          <el-button
            type="primary"
            @click="confirmVendor"
            style="width: 100px"
            size="small"
            >确 定</el-button
          >
        </div>
      </el-dialog>
      <!-- 变更弹出层 -->
      <el-dialog
        title="招标文件变更"
        :visible.sync="modifyVisible"
        width="50%"
        @closed="closeDialog('modifyRef')"
      >
        <el-form
          :model="modifyForm"
          ref="modifyRef"
          :rules="modifyRules"
          label-position="left"
          label-width="80px"
          size="medium"
        >
          <el-row>
            <el-col :span="16" class="grid-cell">
              <el-form-item
                label="变更类型"
                label-width="110px"
                prop="type"
                class="label-right-align"
              >
                <el-select
                  v-model="modifyForm.type"
                  placeholder="请选择变更类型"
                  clearable
                  style="width: 240px"
                >
                  <el-option label="变更时间" value="1" />
                  <el-option label="变更内容" value="2" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24" class="grid-cell">
              <el-form-item
                label="变更前时间"
                label-width="110px"
                prop="updateBefore"
                class="label-right-align"
                v-if="Number(modifyForm.type) !== 2"
              >
                <template>
                  <el-date-picker
                    v-model="modifyForm.updateBefore"
                    type="datetime"
                    style="width: 100%"
                    placeholder="选择日期"
                    :picker-options="expireTimeOption"
                    value-format="yyyy-MM-dd HH:mm:ss"
                    disabled
                  />
                </template>
                <!-- <template v-else>
                <el-input v-model="modifyForm.updateBefore" type="textarea" clearable></el-input>
              </template> -->
              </el-form-item>
            </el-col>
            <el-col :span="24" class="grid-cell">
              <el-form-item
                :label="
                  Number(modifyForm.type) !== 2 ? '变更后时间' : '变更后内容'
                "
                label-width="110px"
                prop="updateAfter"
                class="label-right-align"
              >
                <template v-if="Number(modifyForm.type) === 1">
                  <el-date-picker
                    v-model="modifyForm.updateAfter"
                    type="datetime"
                    style="width: 100%"
                    placeholder="选择日期"
                    :picker-options="expireTimeOption"
                    value-format="yyyy-MM-dd HH:mm:ss"
                  />
                </template>
                <template v-else>
                  <el-input
                    v-model="modifyForm.updateAfter"
                    type="textarea"
                    clearable
                  ></el-input>
                </template>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <el-table v-loading="modifyLoading" :data="modifyList" border stripe>
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column label="变更类型" align="center" prop="typeText" />
          <!-- <el-table-column label="变更前信息" align="center" prop="updateBefore" /> -->
          <el-table-column label="变更内容" align="center" prop="updateAfter" />
          <el-table-column label="经办人" align="center" prop="createBy" />
          <el-table-column label="操作时间" align="center" prop="createTime" />
        </el-table>
        <div slot="footer" class="dialog-footer">
          <el-button
            type="primary"
            @click="confirmModify('modifyRef')"
            style="width: 100px"
            size="small"
            >确 定</el-button
          >
          <el-button
            @click="modifyVisible = false"
            style="width: 100px"
            size="small"
            >取 消</el-button
          >
        </div>
      </el-dialog>
      <!-- 答疑弹出层 -->
      <el-dialog title="招标文件答疑" :visible.sync="QAVisible" width="50%">
        <el-table
          v-loading="QALoading"
          :data="QAList"
          border
          stripe
          @row-click="selectQA"
        >
          <el-table-column label="" width="30" align="center">
            <template slot-scope="scope">
              <el-radio
                class="table_radio"
                v-model="QAId"
                :label="scope.row.id"
              />
            </template>
          </el-table-column>
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="提问供应商"
            align="center"
            prop="vendorName"
          />
          <el-table-column label="提问内容" align="center" prop="question" />
          <el-table-column label="答疑内容" align="center" prop="content" />
        </el-table>
        <el-form
          :model="QAForm"
          ref="QARef"
          :rules="QARules"
          label-position="left"
          label-width="80px"
          size="medium"
        >
          <el-row>
            <el-col :span="16" class="grid-cell">
              <el-form-item
                label="答疑内容"
                prop="content"
                class="label-right-align"
              >
                <el-input
                  v-model="QAForm.content"
                  type="textarea"
                  clearable
                ></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6" class="grid-cell">
              <div
                class="static-content-item"
                style="margin-top: 8px; margin-left: 8px"
              >
                <el-button icon="el-icon-search" @click="submitQA('QARef')"
                  >提交</el-button
                >
              </div>
            </el-col>
          </el-row>
        </el-form>
        <!-- <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmVendor">确 定</el-button>
        <el-button @click="vendorVisible = false">取 消</el-button>
      </div> -->
      </el-dialog>
    </div>
  </div>
</template>

<script>
import {
  addNotice,
  getNoticeUpdateList,
  addUpdateNotice,
  getQAList,
  addAnswer,
  listAreaDivisionTree,
  getVendorClassifyTree,
  aNewAdd, twiceBidFinish,
} from "@/api/procurement/manage";
import { deptTreeSelect } from "@/api/system/user";
import { getVendorList } from "@/api/vendor/vendor";
import FileModule from "@/components/FileModule/index.vue";
import PageTitle from "@/components/PageTitle/index.vue";
import { get } from "lodash";
import { getViweFileURL } from "@/api/template/file";
export default {
  name: "tender-documents",
  dicts: ["vendor_level"],
  props: {
    noticeDetail: {
      type: Object,
      default: () => {},
    },
    scheme: {
      type: Object,
      default: () => {},
    },
    changeState: {
      typeof: Function,
      default: () => {},
    },
  },
  data() {
    let checkNum = (rule, value, callback) => {
      if (!/^[1-9]\d*$/.test(value)) {
        callback(new Error("请输入正整数"));
      } else {
        callback();
      }
    };
    return {
      viewFileUrl:"",  //预览招标文件url
      // * 方案名称
      schemeName: '',
      formatApplyTime: '',
      formatNowDate: '',
      needTime: true,
      timeDifferenceElement: '',
      setVendorShow: false,
      answerAndChangeShow: false,
      applyButtonShow: false,
      formData: {}, //form表单数据
      planList: [],
      inventoryList: [],
      rules: {
        applyTime: [
          {
            required: true,
            message: "投标截止时间不能为空",
          },
        ],
      },
      // 遮罩层
      loading: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        procurementPlanCode: undefined,
        procurementPlanName: undefined,
        projectName: undefined,
        operator: undefined,
        procurementPlanType: "all",
      },
      currentContract: {},
      isSubmit: false,
      indexs: [],
      expireTimeOption: {
        // 设置日期时间显示格式，只显示年月日时分
        format: "yyyy-MM-dd HH:mm:ss",
        // 设置可选的时间范围
        selectableRange: "00:00:00 - 23:59:59",
        // disabledDate(time) {
        //   return time.getTime() < Date.now() - 8.64e7; // 禁用小于当前日期的日期
        // }
        disabledDate(time) {
          // 获取今天的时间戳
          const today = new Date();
          today.setHours(0, 0, 0, 0); // 设置为当天的零点

          // 明天的时间戳
          const tomorrow = new Date(today);
          tomorrow.setDate(today.getDate() + 1);

          // 将传入的时间戳转为日期对象
          const date = new Date(time);

          // 只能选择明天及之后的日期
          return date <= today || date < tomorrow;
        },
      },
      deptOptions: undefined,
      modifyVisible: false,
      modifyForm: {},
      modifyLoading: false,
      modifyList: [],
      modifyRules: {
        type: [{ required: true, message: "请选择变更类型", trigger: "blur" }],
        updateBefore: [
          { required: true, message: "请输入变更前信息", trigger: "blur" },
        ],
        updateAfter: [
          { required: true, message: "请输入变更后信息", trigger: "blur" },
        ],
      },
      addressProps: {
        children: "children",
        label: "divisionName",
      },
      categoryProps: {
        children: "children",
        label: "name",
      },
      vendorVisible: false, //是否显示供应商弹窗
      vendorQuery: {
        enterpriseName: undefined,
        pageSize: 10,
        pageNumber: 1,
        enterpriseProvinceCode: undefined,
        enterpriseCityCode: undefined,
        enterpriseType: undefined,
        registeredCapital: undefined,
        vendorLevel: undefined,
        vendorClass: 0,
      },
      vendorLoading: false,
      vendorList: [],
      vendorTotal: 0,
      vendors: [], //table选择的供应商
      vendorsSelection: [], //table选择的供应商-完整信息
      addressLoading: false,
      categoryLoading: false,
      selectVendors: [],
      selectVendorsInfo: [], //确定选择的供应商-完整信息
      QAVisible: false, //是否显示答疑弹窗
      QAForm: {},
      QAList: [],
      QALoading: false,
      QAId: "",
      QARules: {
        content: [{ required: true, message: "请输入内容", trigger: "blur" }],
      },
      addressOptions: [],
      categoryOptions: [],
      QATotal: 0,
      updateTotal: 0,
      paymentTypeList: [
        { label: "现付", value: 1 },
        { label: "旬付", value: 2 },
        { label: "月付", value: 3 },
        { label: "资金利息", value: 4 },
      ],
    };
  },
  components: {
    FileModule,
    PageTitle,
  },
  mounted() {
    if(this.noticeDetail?.tenderNotice?.noticeStatus === 1 && this.needTime) {
      if(this.noticeDetail?.tenderNotice?.applyTime) {
        // * 投标截止时间
        this.formatApplyTime = new Date(this.noticeDetail?.tenderNotice?.applyTime).format('yyyy年MM月dd日 HH:mm:ss')
        this.updateTimeDifference();
        // 每秒更新一次
        if(this.needTime) {
          this.timer = setInterval(this.updateTimeDifference, 1000);
        }
      }else {
        this.$message.error('截止时间取值有误，请联系管理人员！')
        this.$router.replace("/procurement/bindding");
      }
    }
    if (this.noticeDetail?.rangeList) {
      // console.warn("选择供应商",this.noticeDetail.rangeList);
      this.selectVendorsInfo = this.noticeDetail.rangeList || [];
    }


  },
  created() {
    const { bidDeadline,procurementType } = this.scheme;
    if (!this.noticeDetail.tenderNotice) {
      this.$set(this.formData, "applyTime", bidDeadline);
    }
    // * 如果procurementType 为1 （公开招标），就要判断当前状态是否是13【处于发布文件（编辑）状态】，反之就要判断是否有id【formData.id没有则出现发布按钮】
    if(procurementType === 1){
      this.applyButtonShow = (this.noticeDetail.tenderNotice.noticeStatus === 13)
      this.answerAndChangeShow = (this.noticeDetail.tenderNotice.noticeStatus !== 13 && this.formData.id)
      this.setVendorShow = false
    }else {
      this.applyButtonShow = !this.formData.id
      this.answerAndChangeShow = this.formData.id
      this.setVendorShow = !this.formData.id
    }
    this.getDeptTree();
    this.getQAList(0);
    this.getNoticeUpdateList();

    //获取招标文件的预览url
    this.getbiddingTemplate();
  },
  methods: {
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

    async updateTimeDifference() {
      // 投标截止时间
      const deadline = new Date(this.noticeDetail?.tenderNotice?.applyTime);
      const now = new Date();
      // * 当前北京时间
      this.formatNowDate = new Date().format('yyyy年MM月dd日')
      // 计算时间差
      const diff = deadline - now;
      if(diff<=0){
        this.needTime = false;
        this.timeDifferenceElement = `00天00小时00分00秒`;
        if(this.timer) {
          clearInterval(this.timer);
        }
        if(this.noticeDetail?.tenderNotice?.twiceQuotState === 1){
          const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
          try {
            const res = await twiceBidFinish(noticeId);
          } catch (err) {
            console.log(err);
          }
        }
        return
      }

      // 计算天数、小时数、分钟数和秒数
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      // 格式化并显示结果
      this.timeDifferenceElement = `${days<10?'0'+days:days}天${hours<10?'0'+hours:hours}小时${minutes<10?'0'+minutes:minutes}分${seconds<10?'0'+seconds:seconds}秒`;
    },
    //提交公告
    submitForm(formName) {
      const { procurementType } = this.scheme;
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          const loading = this.$loading({
            lock: true,
            text: "数据提交中...",
            background: "rgba(0, 0, 0, 0.7)",
          });
          console.log(this.noticeDetail, "this.noticeDetail");
          let formData = {};
          if (procurementType === 1) {
            delete this.vendorQuery.pageNumber;
            delete this.vendorQuery.pageSize;
            formData = {
              ...this.formData,
              schemeId: this.scheme.id,
              schemeType: this.scheme.procurementType,
              biddingDocAttachList: [this.scheme.biddingTemplate],
              vendorQueryParam: Object.fromEntries(
                Object.entries(this.vendorQuery).filter(
                  ([key, value]) => value !== undefined
                )
              ),
            };
          } else {
            formData = {
              ...this.formData,
              schemeId: this.scheme.id,
              schemeType: this.scheme.procurementType,
              vendorIds: this.selectVendors,
              biddingDocAttachList: [this.scheme.biddingTemplate],
            };
          }

          console.log(formData, "formData");
          try {
            if (this.scheme.noticeStatus === 0) {
              await aNewAdd(formData);
            } else {
              await addNotice(formData);
            }
            loading.close();
            this.$message({
              message: "保存成功",
              type: "success",
            });
            this.$emit("changeState", 1);
            // this.$tab.closePage();
          } catch (err) {
            console.log(err);
            loading.close();
            this.isSubmit = false;
          }
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },
    /** 获取供应商列表 */
    async getVendorList() {
      this.vendorLoading = true;
      const { procurementType } = this.scheme;
      try {
        const res = await getVendorList(this.vendorQuery);
        this.vendorList = res.data.rows;
        this.vendorTotal = res.data.total;
        this.vendorLoading = false;
        //公开招标全选供应商
        if (procurementType === 1) {
          this.selectAllRows();
        }
      } catch (err) {
        console.log(err);
        this.vendorLoading = false;
      }
    },
    /** 获取文件修改列表 */
    async getNoticeUpdateList() {
      const { id } = this.noticeDetail.tenderNotice
        ? this.noticeDetail.tenderNotice
        : {};
      console.log(this.noticeDetail, "this.noticeDetail");
      const res = await getNoticeUpdateList(id);
      this.modifyList = res.data;
      this.updateTotal = res.data.length;
      console.log(res, "修改列表");
    },
    /** 新增文件公告修改 */
    async addUpdateNotice() {
      const { id } = this.noticeDetail.tenderNotice
        ? this.noticeDetail.tenderNotice
        : {};
      const { modifyForm } = this;
      const formData = {
        noticeId: id,
        ...modifyForm,
      };
      try {
        const res = await addUpdateNotice(formData);
        this.$message.success("修改成功");
        this.getNoticeUpdateList();
      } catch (err) {
        console.log(err);
      }
      this.modifyVisible = false;
    },
    //设置供应商
    setVendor() {
      this.vendorVisible = true;
      this.vendorList.forEach((row) => {
        if (this.selectVendors.indexOf(row.id)>-1) {
          // console.log("需要勾选",row.enterpriseName);
          this.$refs.multipleTable.toggleRowSelection(row, true);
        } else {
          this.$refs.multipleTable.toggleRowSelection(row, false);
        }
      });
      this.getVendorList();
      this.listAreaDivisionTree();
      this.getVendorClassifyTree();
    },
    /* 搜索供应商 */
    searchVendor() {
      this.vendorQuery.pageNumber = 1;
      this.getVendorList();
    },
    // 省市节点的点击事件
    handleAddressNodeClick(data) {
      console.log(data, "ddd-ddd");
      if (data.divisionCode.endsWith("0000")) {
        this.vendorQuery.enterpriseProvinceCode = data.divisionCode;
        this.vendorQuery.enterpriseCityCode = "";
      } else {
        this.vendorQuery.enterpriseCityCode = data.divisionCode;
        this.vendorQuery.enterpriseProvinceCode = "";
      }
      this.searchVendor();
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    /** 已选择的供应商 */
    handleSelectionVendor(selection) {
      this.vendors = selection.map((item) => item.id);
      this.vendorsSelection = selection;
    },
    /** 确认供应商 */
    confirmVendor() {
      const { vendors } = this;
      if (vendors.length === 0) return this.$message.error("请选择供应商");
      this.vendorVisible = false;
      this.selectVendors = vendors;
      this.selectVendorsInfo = this.vendorsSelection;
      console.log(this.vendors, "this.vendors");
    },
    /** 删除供应商 */
    deleteSelectedVendor(item) {
      // console.log("删除供应商",item);
      if (!item.enterpriseName) return;
      for (let i = 0; i < this.selectVendorsInfo.length; i++) {
        if (this.selectVendorsInfo[i] == item) {
          this.selectVendorsInfo.splice(i, 1);
          break;
        }
      }
      if (this.selectVendorsInfo.length) {
        this.selectVendors = this.selectVendorsInfo.map((item) => item.id);
      } else {
        this.selectVendors = [];
      }
      this.vendors = this.selectVendors;
    },
    setModify() {
      this.modifyVisible = true;
      this.getNoticeUpdateList();
    },
    /** 确认修改内容 */
    confirmModify(formName) {
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          this.addUpdateNotice();
        }
      });
    },
    /** 获取问答列表 */
    async getQAList(answerType) {
      try {
        const { id } = this.noticeDetail.tenderNotice
          ? this.noticeDetail.tenderNotice
          : {};
        const res = await getQAList(id, answerType);
        this.QAList = res.data;
        if (answerType === 0) {
          this.QATotal = res.data.length;
        }
        console.log(res, "问答列表");
      } catch (err) {
        console.log(err);
      }
    },
    /** 打开答疑弹出层 */
    setQA() {
      this.QAVisible = true;
      this.getQAList();
    },
    /** 选择答疑问题 */
    selectQA(row) {
      this.QAId = row.id;
      // this.checkedItem = this.tpExpertList.filter(item => item.departmentId === row.departmentId)[0]
    },
    async addAnswer() {
      try {
        const { QAId } = this;
        const { content } = this.QAForm;
        const formData = {
          id: QAId,
          content,
        };
        await addAnswer(formData);
        this.$message.success("提交成功");
        this.QAId = "";
        this.QAForm.content = "";
        this.getQAList();
      } catch (err) {
        console.log(err);
      }
    },
    submitQA(formName) {
      const { QAId } = this;
      if (!QAId) return this.$message.error("请选择要回复的问题");
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          this.addAnswer();
        }
      });
    },
    /* 关闭弹出层 */
    closeDialog(formName) {
      console.log(formName, "formName");
      this.$refs[formName].resetFields();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$router.push("");
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then((response) => {
        this.deptOptions = response.data;
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleCategoryNodeClick(data) {
      console.log(data, "分类");
      this.vendorQuery.enterpriseType = data.id;
      this.searchVendor();
    },
    //获取省市区
    async listAreaDivisionTree() {
      this.addressLoading = true;
      try {
        const res = await listAreaDivisionTree();
        this.addressOptions = res.data.map((item) => ({
          divisionName: item.divisionName,
          divisionCode: item.divisionCode,
          children: item.children.map((subItem) => ({
            divisionName: subItem.divisionName,
            divisionCode: subItem.divisionCode,
          })),
        }));
        console.log(res, "省市树");
      } catch (err) {
        console.log(err);
      }
      this.addressLoading = false;
    },
    //获取供应商分类
    async getVendorClassifyTree() {
      this.categoryLoading = true;
      try {
        const res = await getVendorClassifyTree();
        this.categoryOptions = res.data;
      } catch (err) {
        console.log(err);
      }
      this.categoryLoading = false;
    },
    closedVendor() {
      this.vendorQuery = {
        enterpriseName: undefined,
        pageSize: 10,
        pageNumber: 1,
        enterpriseProvinceCode: undefined,
        enterpriseCityCode: undefined,
        enterpriseType: undefined,
        vendorClass: 0,
      };
    },
    selRowKey(row) {
      return row.id;
    },
    selectAllRows() {
      this.vendorList.forEach((row) => {
        console.log(row, "rrrr");
        this.$refs.multipleTable.toggleRowSelection(row, true);
      });
    },
  },
  watch: {
    noticeDetail: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          if (newVal.bidEndTime) {
            newVal.tenderNotice.applyTime = newVal.bidEndTime;
          }
          let value = newVal.tenderNotice ? newVal.tenderNotice : {};
          const formData = {
            contact: this.scheme.bidContactPerson,
            phone: this.scheme.bidContactPhone,
            email: this.scheme.bidContactEmail,
            ...value,
          };
          this.formData = formData;
        }
      },
      deep: true,
      immediate: true,
    },
    "modifyForm.type": {
      handler(val) {
        this.$refs.modifyRef.clearValidate();
        if (Number(val) === 1) {
          this.$set(
            this.modifyForm,
            "updateBefore",
            this.noticeDetail.bidEndTime
              ? this.noticeDetail.bidEndTime
              : this.formData.applyTime
          );
          this.$set(this.modifyForm, "updateAfter", "");
        } else if (Number(val) === 2) {
          this.$set(this.modifyForm, "updateBefore", "");
          this.$set(this.modifyForm, "updateAfter", "");
        }
      },
    },
  },
  computed: {
    disableSelectAllStyle() {
      let obj = {};
      if (this.scheme.procurementType === 1) {
        obj = {
          "pointer-events": "none", // 禁用鼠标事件，使得表头全选不可点击
        };
      }
      return obj;
    },
  },
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
  margin-bottom: 15px;
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
  .page-title-right {
    display: flex;
    .page-title-right-item {
      margin-left: 10px;
    }
  }
}

.form-body {
  padding: 20px;
}
.app-container {
  width: 100%;
  font-family: PingFang SC;
  background-color: #f2f2f8; //主体内容颜色配置
  padding: 0 !important;
}
.head-container {
  height: calc(72vh - 50px);
  overflow: scroll;
  width: 100%;
}
.total-num {
  width: 100%;
  position: relative;
  .num-box {
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    z-index: 1001;
  }
}
::v-deep .el-form-item__content{
  word-break: break-all !important;
}
</style>
