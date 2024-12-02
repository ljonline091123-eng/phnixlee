<template>
  <div class="app-container">
    <div class="context" style="height: calc(100vh - 116px)">
      <el-form
        :model="formData"
        ref="form"
        :rules="rules"
        label-position="right"
        label-width="140px"
        size="medium"
        @submit.native.prevent
      >
        <PageTitle title="基本信息" marginBottom="15px">
          <div class="page-title-right">
            <el-button
              type="success"
              icon="el-icon-plus"
              @click="setVendor"
              size="small"
              v-if="!formData.id"
              :disabled="
                noticeDetail.purchaseOfficer === undefined
                  ? false
                  : !noticeDetail.purchaseOfficer
              "
            >设置供应商范围
            </el-button
            >
            <template v-if="updateTotal > 0 && formData.id">
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
                >变更
                </el-button
                >
              </el-badge>
            </template>
            <div class="page-title-right-item" v-else-if="formData.id">
              <el-button
                type="primary"
                size="small"
                @click="setModify"
                :disabled="
                  noticeDetail.purchaseOfficer === undefined
                    ? false
                    : !noticeDetail.purchaseOfficer
                "
              >变更
              </el-button
              >
            </div>
            <template v-if="QATotal > 0 && formData.id">
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
                >答疑
                </el-button
                >
              </el-badge>
            </template>
            <div class="page-title-right-item" v-else-if="formData.id">
              <el-button
                type="primary"
                size="small"
                @click="setQA"
                :disabled="
                  noticeDetail.purchaseOfficer === undefined
                    ? false
                    : !noticeDetail.purchaseOfficer
                "
              >答疑
              </el-button
              >
            </div>
            <el-button
              type="primary"
              size="small"
              @click="submitForm('form')"
              :loading="isSubmit"
              v-if="!formData.id || isSubmit"
              :disabled="
                noticeDetail.purchaseOfficer === undefined
                  ? false
                  : !noticeDetail.purchaseOfficer
              "
            >{{ isSubmit ? "提交中..." : "发布" }}
            </el-button
            >
          </div>
        </PageTitle>
        <el-row :gutter="40">
          <el-col :span="12" class="grid-cell">
            <el-form-item
              label="报名截止时间"
              prop="applyTimeNotice"
              class="required label-right-align"
            >
              <template #label>
                报名截止时间
                <el-tooltip content="报名截止时间从当天24点开始计算至少120个小时(5天)！" placement="top">
                  <i class="el-icon-question"></i>
                </el-tooltip>
              </template>

              <el-date-picker
                v-model="formData.applyTimeNotice"
                type="datetime"
                style="width: 100%"
                placeholder="选择日期"
                :picker-options="expireTimeOption"
                value-format="yyyy-MM-dd HH:mm:ss"
                :disabled="!!(formData.id || isSubmit)"
                class="date_picker"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12" class="grid-cell">
            <el-form-item
              label="联系人"
              prop="contactNotice"
              class="required label-right-align"
            >
              <el-input
                v-model="formData.contactNotice"
                type="text"
                clearable
                placeholder="请输入联系人"
                :disabled="!!(formData.id || isSubmit)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="grid-cell">
            <el-form-item
              label="联系电话"
              prop="phoneNotice"
              class="required label-right-align"
            >
              <el-input
                type="text"
                clearable
                v-model="formData.phoneNotice"
                placeholder="请输入联系电话"
                :disabled="!!(formData.id || isSubmit)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="grid-cell">
            <el-form-item
              label=" 联系邮箱"
              prop="emailNotice"
              class="required label-right-align"
            >
              <el-input
                v-model="formData.emailNotice"
                type="text"
                clearable
                placeholder="请输入联系邮箱"
                :disabled="!!(formData.id || isSubmit)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="grid-cell">
            <el-form-item label=" 招标公告" prop="fileTemplate" class="uploadItem">
              <el-button size="small" type="primary" :disabled="!!(formData.id || isSubmit)" @click="showSecretTips">点击上传</el-button>
              <el-upload
                :action="uploadFileUrl"
                :limit="1"
                :on-success="fileSuccess"
                :file-list="formData.fileList"
                :on-remove="fileRemove"
                :disabled="(!!(formData.id || isSubmit))"
                ref="upload"
              >
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- <el-row v-if="selectVendorsInfo.length">
          <el-form-item
            label="选择供应商"
          >
            <div>
              <el-tag
                v-for="item in selectVendorsInfo" :key="item.id" type="info" style="margin-right: 8px;"
                :closable="!(formData.id ? true : false || isSubmit) && scheme.procurementType !== 1" @close="deleteSelectedVendor(item)"
              >
                <span>{{ item.enterpriseName || item.vendorName }}</span>
              </el-tag>
            </div>
          </el-form-item>
        </el-row> -->

        <PageTitle title="招标公告内容" marginBottom="15px"/>
        <!-- <FileModule
          :attachmentId="attachmentId"
          v-if="attachmentId"
          height="500px"
        /> -->
        <iframe
            v-if="attachmentId"
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
                    >查询
                    </el-button
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
              <el-table-column label="供应商名称" prop="enterpriseName"/>
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
          >取 消
          </el-button
          >
          <el-button
            type="primary"
            @click="confirmVendor"
            style="width: 100px"
            size="small"
          >确 定
          </el-button
          >
        </div>
      </el-dialog>
      <!-- 变更弹出层 -->
      <el-dialog
        title="招标公告变更"
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
                  style="width: 240px"
                >
                  <el-option label="变更时间" value="1"/>
                  <el-option label="变更内容" value="2"/>
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
                    :picker-options="updateAfterTimeOption"
                    value-format="yyyy-MM-dd HH:mm:ss"
                    :popper-class="'currentDatePickerClass'"
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
          <el-table-column label="变更类型" align="center" prop="typeText"/>
          <!-- <el-table-column label="变更前信息" align="center" prop="updateBefore" /> -->
          <el-table-column label="变更内容" align="center" prop="updateAfter"/>
          <el-table-column label="经办人" align="center" prop="createBy"/>
          <el-table-column label="操作时间" align="center" prop="createTime"/>
        </el-table>
        <div slot="footer" class="dialog-footer">
          <el-button
            type="primary"
            @click="confirmModify('modifyRef')"
            style="width: 100px"
            :loading="isUpdate"
            size="small"
          >{{ isUpdate ? "变更中..." : "确定" }}
          </el-button
          >
          <el-button
            @click="modifyVisible = false"
            style="width: 100px"
            size="small"
          >取 消
          </el-button
          >
        </div>
      </el-dialog>
      <!-- 答疑弹出层 -->
      <el-dialog title="招标公告答疑" :visible.sync="QAVisible" width="50%">
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
          <el-table-column label="提问内容" align="center" prop="question"/>
          <el-table-column label="答疑内容" align="center" prop="content"/>
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
                >提交
                </el-button
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
  addAnswerNotice,
  addTenderNotice,
  addUpdateTenderNotice,
  aNewAdd,
  getNoticeUpdateListNotice,
  getQAListNotice,
  getVendorClassifyTree,
  listAreaDivisionTree,
} from "@/api/procurement/manage";
import {getVendorList} from "@/api/vendor/vendor";
import FileModule from "@/components/FileModule/index.vue";
import PageTitle from "@/components/PageTitle/index.vue";
import {offerRepo, offerService, uploadFileUrl} from "@/utils/const";
import {isvalidatemobile, validEmail} from "@/utils/validate";
import {addAttachment, getViweFileURL} from "@/api/template/file";
import {showSecretRelatedTips} from "@/utils/MyUtils";

export default {
  name: "tender-notice",
  components: {
    FileModule,
    PageTitle,
  },
  dicts: ["vendor_level"],
  data() {
    return {
      viewFileUrl:"",  //预览招标公告url
      secretTipsFlag: false,
      attachmentId: '',
      offerService,
      offerRepo,
      uploadFileUrl,
      formData: {}, //form表单数据
      planList: [],
      inventoryList: [],
      rules: {
        fileTemplate:[
          {
            required: true,
            message: "请选择招标文件模板",
            validator: (_rule, _value, callback) => {
              if (!this.formData.fileTemplate) {
                callback(new Error('请上传招标公告'))
              } else {
                callback()
              }

            }
          }
        ],
        applyTimeNotice: [{required: true, message: "请选择报名截止时间", trigger: "blur"}],
        contactNotice: [{required: true, message: "请输入联系人", trigger: "blur"},{
          validator: (rule, value, callback) => {
            const chineseNamePattern = /^[\u4e00-\u9fa5]+$/;
            if (!chineseNamePattern.test(value)) {
              callback(new Error("请输入正确的中文姓名"));
            } else {
              callback();
            }
          },
          trigger: "blur",
        }],
        phoneNotice: [{required: true, message: "请输入联系电话", trigger: "blur"},{
          validator: (rule, value, callback) => {
            if (isvalidatemobile(value)[0]) {
              callback(new Error(isvalidatemobile(value)[1]));
            } else {
              callback();
            }
          },
          trigger: "blur",
        }],
        emailNotice: [{required: true, message: "请输入联系邮箱", trigger: "blur"},{
          validator: (rule, value, callback) => {
            if (!validEmail(value)) {
              callback(new Error("请输入正确的邮箱地址"));
            } else {
              callback();
            }
          },
          trigger: "blur",
        }],
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
      isUpdate: false,
      indexs: [],
      updateAfterTimeOption: {
        // 设置日期时间显示格式，只显示年月日时分
        format: "yyyy-MM-dd HH:mm:ss",
        // 设置可选的时间范围
        selectableRange: "00:00:00 - 23:59:59",
        // disabledDate(time) {
        //   return time.getTime() < Date.now() - 8.64e7; // 禁用小于当前日期的日期
        // }
        disabledDate: (time) => {
          return new Date(time) < new Date(this.formData?.applyTimeNotice);
        }
      },
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
          today.setHours(24, 0, 0, 0); // 设置为当天的24点

          // 过5天的时间戳
          const next = new Date(today);
          next.setDate(today.getDate() + 5);

          // 将传入的时间戳转为日期对象
          const date = new Date(time);

          // 只能选择过5天后的日期【比如今天是24号，则30号及以后可以选择】
          return date < next;
        },
      },
      modifyVisible: false,
      modifyForm: {},
      modifyLoading: false,
      modifyList: [],
      modifyRules: {
        type: [{required: true, message: "请选择变更类型", trigger: "blur"}],
        updateBefore: [
          {required: true, message: "请输入变更前信息", trigger: "blur"},
        ],
        updateAfter: [
          {required: true, message: "请输入变更后信息", trigger: "blur"},
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
        content: [{required: true, message: "请输入内容", trigger: "blur"}],
      },
      addressOptions: [],
      categoryOptions: [],
      QATotal: 0,
      updateTotal: 0,
    };
  },
  props: {
    noticeDetail: {
      type: Object,
      default: () => {
      },
    },
    scheme: {
      type: Object,
      default: () => {
      },
    },
    changeState: {
      typeof: Function,
      default: () => {
      },
    },
  },
  created() {
    const {attachmentNotice} = this.noticeDetail;
    // if (!this.noticeDetail.tenderNotice) {
    //   this.$set(this.formData, "applyTime", bidDeadline);
    // }
    if(attachmentNotice) {
      this.attachmentId = attachmentNotice.id
      this.$set(this.formData, "fileList", [{name: attachmentNotice.fileName,url: attachmentNotice.fileUrl}]);
      this.$set(this.formData, "fileTemplate", [attachmentNotice]);
    }
    this.getQAList(0);
    this.getNoticeUpdateList();
  },
  methods: {
    showSecretTips() {
      showSecretRelatedTips(()=>{
        this.$refs['upload'].$refs['upload-inner'].handleClick()
      })
    },
    fileRemove() {
      this.$set(this.formData, "fileList", []);
      this.$set(this.formData, "fileTemplate", []);
      this.attachmentId = "";
    },
    /**
     * 文件上传后钩子函数
     * @param res
     * @returns {Promise<void>}
     */
    async fileSuccess(res) {
      const { url, name } = res.data;
      this.formData.fileTemplate = [{ fileName: name, fileUrl: url }];
      this.$refs.form.clearValidate("fileTemplate");
      try {
        const res = await addAttachment({ fileName: name, fileUrl: url });
        this.attachmentId = res.data;
        this.formData.attachIdNotice = res.data
      } catch (err) {
        console.log(err);
      }
      //上传文件成功后，获取文档中台的该文件的预览url
      try {
        const query1 = { fileName: name, fileUrl: url };
        const res = await getViweFileURL(query1);
        console.log("fileName URL:",name);
        console.log("URL:",url);
        this.viewFileUrl = res.data;
        console.log("editFileUrl:",this.editFileUrl);
      } catch (err) {
        console.log(err);
      }

    },
    //提交公告
    submitForm(formName) {
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid,object) => {
        if (valid) {
          const loading = this.$loading({
            lock: true,
            text: "数据提交中...",
            background: "rgba(0, 0, 0, 0.7)",
          });
          console.log(this.noticeDetail, "this.noticeDetail");
          const formData = {
            ...this.formData,
            schemeId: this.scheme.id,
            schemeType: this.scheme.procurementType,
            vendorIds: this.selectVendors,
            biddingDocAttachList: this.formData.fileTemplate,
            applyTime: this.scheme.bidDeadline
          };

          console.log(formData, "formData");
          try {
            if (this.scheme.noticeStatus === 0) {
              await aNewAdd(formData);
            } else {
              await addTenderNotice(formData);
            }
            loading.close();
            this.$message({
              message: "发布成功，请等待供应商报名！",
              type: "success",
            });
            this.isSubmit = false;
            return this.$router.push('/procurement/bindding')
            // this.$emit("changeState", 12);
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
      const {procurementType} = this.scheme;
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
      const {id} = this.noticeDetail.tenderNotice
        ? this.noticeDetail.tenderNotice
        : {};
      console.log(this.noticeDetail, "this.noticeDetail");
      const res = await getNoticeUpdateListNotice(id);
      this.modifyList = res.data;
      this.updateTotal = res.data.length;
      this.$set(this.modifyForm, "type", "1");
      console.log(res, "修改列表");
    },
    /** 新增文件公告修改 */
    async addUpdateNotice() {
      this.isUpdate = true
      const {id} = this.noticeDetail.tenderNotice
        ? this.noticeDetail.tenderNotice
        : {};
      const {modifyForm} = this;
      const formData = {
        noticeId: id,
        ...modifyForm,
      };
      try {
        const res = await addUpdateTenderNotice(formData);
        this.$message.success("修改成功");
        this.getNoticeUpdateList();
      } catch (err) {
        console.log(err);
      }
      this.isUpdate = false
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
      const {vendors} = this;
      if (vendors.length === 0) return this.$message.error("请选择供应商");
      this.vendorVisible = false;
      this.selectVendors = vendors;
      this.selectVendorsInfo = this.vendorsSelection;
      console.log(this.vendors, "this.vendors");
    },
    /** 删除供应商 */
    deleteSelectedVendor(item) {
      // console.log("删除供应商",item);
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
        const {id} = this.noticeDetail.tenderNotice
          ? this.noticeDetail.tenderNotice
          : {};
        const res = await getQAListNotice(id, answerType);
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
        const {QAId} = this;
        const {content} = this.QAForm;
        const formData = {
          id: QAId,
          content,
        };
        await addAnswerNotice(formData);
        this.$message.success("提交成功");
        this.QAId = "";
        this.QAForm.content = "";
        this.getQAList();
      } catch (err) {
        console.log(err);
      }
    },
    submitQA(formName) {
      const {QAId} = this;
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
        this.$refs.multipleTable.toggleRowSelection(row, true);
      });
    },
  },
  watch: {
    noticeDetail: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          // if (newVal.bidEndTime) {
          //   newVal.tenderNotice.applyTimeNotice = newVal.bidEndTime;
          // }
          let value = newVal.tenderNotice ? newVal.tenderNotice : {};
          const formData = {
            contactNotice: this.scheme.bidContactPerson,
            phoneNotice: this.scheme.bidContactPhone,
            emailNotice: this.scheme.bidContactEmail,
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
        if(this.$refs.modifyRef) {
          this.$refs.modifyRef.clearValidate();
        }
        if (Number(val) === 1) {
          this.$set(
            this.modifyForm,
            "updateBefore",
            this.formData.applyTimeNotice
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
::v-deep .el-form-item.uploadItem {
  .el-form-item__content {
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

::v-deep .el-form-item__content {
  word-break: break-all !important;
}
::v-deep.date_picker{
  .el-picker-panel__footer .el-button--text:first-child{
    display: none !important;
  }
}
</style>
