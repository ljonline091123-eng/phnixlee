<template>
  <div class="app-container">
    <BackButton path="/tender-procurement/expert/expert" title="专家信息新增">
      <div>
<!--        :disabled="isSubmit"-->
        <el-button
          type="primary"
          plain
          size="mini"
          @click="$router.push('/tender-procurement/expert/expert')"
          >取消</el-button
        >
      <el-button
        v-if="type=='edit' && formData.state != 1"
        type="primary"
        size="mini"
        @click="saveForm('form')"
        :disabled="isSubmit"
        :loading="isSubmit"
        >{{ isSubmit ? "保存中..." : "保存" }}</el-button
      >
<!--   审批通过不显示提交     -->
        <el-button
          v-if="type=='edit' && formData.state != 1"
          type="primary"
          size="mini"
          @click="submitForm('form')"
          :disabled="isSubmit"
          :loading="isSubmit"
          >{{ isSubmit ? "提交中..." : "提交" }}</el-button>
        <el-button
          v-if="auditable"
          type="primary"
          size="mini"
          @click="confirmApprove()"
          >审批</el-button>
        <el-button
          type="primary"
          size="mini"
          @click="handelCalibrationApproval()"
          >审批详情</el-button>
      </div>
    </BackButton>
    <div class="context">
      <el-form
        :model="formData"
        ref="form"
        :rules="rules"
        label-position="right"
        label-width="140px"
        size="medium"
        @submit.native.prevent
      >
        <PageTitle title="基本信息" />
        <div class="form-body">
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="专家姓名"
                prop="expertName"
                class="required label-right-align"
              >
                <el-input
                  type="text"
                  clearable
                  :readonly="true"
                  disabled
                  v-model="formData.expertName"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label=" 手机号码"
                prop="expertPhone"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.expertPhone"
                  disabled
                  type="text"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="组织机构"
                prop="belongOrganization"
                class="required label-right-align"
              >
                <el-input
                  type="text"
                  clearable
                  :readonly="true"
                  disabled
                  v-model="formData.belongOrganization"
                  placeholder="系统自动为您生成"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="工作部门"
                prop="department"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.department"
                  type="text"
                  clearable
                  disabled
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="学历"
                prop="educationDegree"
                class="required label-right-align"
              >
                <el-select
                  v-model="formData.educationDegree"
                  placeholder="请选择学历"
                  style="width: 100%"
                  clearable
                  :disabled="isSubmit"
                >
                  <el-option
                    v-for="dict in dict.type.education_degree"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>

            </el-col>

            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="专业"
                prop="major"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.major"
                  type="text"
                  clearable
                  :disabled="isSubmit"
                />
              </el-form-item>

            </el-col>
          </el-row>
          <el-row :gutter="40">
            <!-- <el-col :span="8" class="grid-cell">
              <el-form-item
                label="执业资格证"
                prop="registeredCertificate"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.registeredCertificate"
                  type="text"
                  clearable
                  :disabled="isSubmit"
                />
              </el-form-item>
            </el-col> -->
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="执业资格证"
                prop="registeredCertificate"
                class="required label-right-align"
              >
                <el-select
                  v-model="formData.registeredCertificate"
                  placeholder="请选择执业资格证"
                  style="width: 100%"
                  :disabled="isSubmit"
                >
                  <el-option
                    v-for="dict in dict.type.registered_certificate"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="执业资格证取得时间"
                prop="registeredCertificateDate"
                class="required label-right-align"
              >
                <el-date-picker
                  v-model="formData.registeredCertificateDate"
                  type="date"
                  style="width: 100%"
                  placeholder="选择日期"
                  value-format="yyyy-MM-dd"
                  :disabled="isSubmit"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="现从事专业工作"
                prop="presentJob"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.presentJob"
                  type="text"
                  clearable
                  :disabled="isSubmit"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="本专业参加工作时间"
                prop="presentJobDate"
                class="required label-right-align"
              >
                <el-date-picker
                  v-model="formData.presentJobDate"
                  type="date"
                  style="width: 100%"
                  placeholder="选择日期"
                  value-format="yyyy-MM-dd"
                  :disabled="isSubmit"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="技术职称"
                prop="technicalTitles"
                class="required label-right-align"
              >
                <el-select
                  v-model="formData.technicalTitles"
                  placeholder="请选择专家类别"
                  style="width: 100%"
                  :disabled="isSubmit"
                >
                  <el-option
                    v-for="dict in dict.type.technical_titles"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="技术职称获取时间"
                prop="technicalTitlesDate"
                class="required label-right-align"
              >
                <el-date-picker
                  v-model="formData.technicalTitlesDate"
                  type="date"
                  style="width: 100%"
                  placeholder="选择日期"
                  value-format="yyyy-MM-dd"
                  :disabled="isSubmit"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="现工作单位或者部门"
                prop="presentUnitDept"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.presentUnitDept"
                  type="text"
                  clearable
                  :disabled="isSubmit"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="email"
                prop="email"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.email"
                  type="text"
                  clearable
                  :disabled="isSubmit"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="专家类别"
                prop="expertType"
                class="required label-right-align"
              >
<!--                <el-select-->
<!--                  v-model="formData.expertType"-->
<!--                  placeholder="请选择专家类别"-->
<!--                  style="width: 100%"-->
<!--                  :disabled="isSubmit"-->
<!--                >-->
<!--                  <el-option-->
<!--                    v-for="dict in dict.type.expert_type"-->
<!--                    :key="dict.value"-->
<!--                    :label="dict.label"-->
<!--                    :value="dict.value"-->
<!--                  ></el-option>-->
<!--                </el-select>-->

                <el-checkbox-group v-model="expertTypeList" :disabled="isSubmit">
                  <el-checkbox
                    v-for="dict in dict.type.expert_type"
                    :label="dict.value"
                    :key="dict.value"
                  >{{ dict.label }}</el-checkbox
                  >
                </el-checkbox-group>
              </el-form-item>
            </el-col>
            <el-col :span="16" class="grid-cell">
              <el-form-item
                label="业态"
                prop="businessType"
                class="required label-right-align"
              >
                <!-- <el-radio-group
                  v-model="formData.businessType"
                  :disabled="isSubmit"
                >
                  <el-radio
                    v-for="dict in dict.type.expert_business_type"
                    :key="dict.value"
                    :label="dict.value"
                    >{{ dict.label }}</el-radio
                  >
                </el-radio-group> -->
                 <el-checkbox-group v-model="businessTypeList"  :disabled="isSubmit">
                  <el-checkbox
                    v-for="dict in dict.type.expert_business_type"
                    :label="dict.value"
                    :key="dict.value"
                    >{{ dict.label }}</el-checkbox
                  >
                </el-checkbox-group>
                  <!-- <el-checkbox-group v-model="formData.businessType" @change="handleCheckedCitiesChange">
                      <el-checkbox v-for="dict in dict.type.expert_business_type"
                    :key="dict.value"
                    :label="dict.value">{{dict.label}}</el-checkbox>
                    </el-checkbox-group> -->
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="16" class="grid-cell">
              <el-form-item
                label="相关专业工作简历"
                prop="professionResume"
                class="required label-right-align"
              >
                <el-input
                  type="textarea"
                  :rows="4"
                  placeholder="请输入内容"
                  v-model="formData.professionResume"
                  :disabled="isSubmit"
                >
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="16" class="grid-cell">
              <el-form-item
                label="附件"
                prop="resumeAttachList"
                class="required label-right-align uploadItem"
              >
                <el-button size="small" type="primary" style="margin-top: 8px;" @click="showSecretTips" :disabled="isSubmit">点击上传</el-button>
                <el-upload
                  :action="uploadFileUrl"
                  :limit="1"
                  accept=".pdf"
                  :on-success="fileSuccess"
                  :file-list="formData.resumeAttachList"
                  :on-remove="fileRemove"
                  :on-preview="handlePreview"
                  :before-upload="beforeUpload"
                  :disabled="isSubmit"
                  ref="upload"
                >
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
    </div>
    <!-- 审批和审批详情  -->
    <ApprovalForm
      :visible.sync="expertVisible"
      :title="'新增专家审批流程'"
      :formModel="sanctionForm"
      :rejectNodeList="rejectNodeList"
      :nextCandidateList="nextCandidateList"
      :nextAppointable="nextAppointable"
      @update:visible="expertVisible = $event"
      @submit="handleSubmit"
    />
    <ApprovalDetailsDialog
      :visible.sync="calibrateVisible"
      title="新增专家审批流程详情"
      :activeStep="calibrateActive"
      :processInformationList="processInformationList"
      :approveLists="approveArr"
      :loading="calibrateLoading"
      @update:visible="calibrateVisible = $event"
  />

    <el-dialog
      :title="templateDialogTitle"
      :visible.sync="templateDialogVisible"
      width="80%"
    >
      <iframe allowfullscreen="true"
              :src= this.viewFileUrl
              width="100%"
              height="700px"
              frameborder="0"
      ></iframe>
    </el-dialog>
  </div>
</template>

<script>
import ApprovalForm from "@/components/Approval/approvalForm.vue";
import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
import { Base64 } from "js-base64";
import { submitExpert,getInfo,saveExpert } from "@/api/expert/expert";
import BackButton from "@/components/BackButton/index.vue";
import PageTitle from "@/components/PageTitle/index.vue";
import { uploadFileUrl } from "@/utils/const";
import {
  getPermissionButton, getPermissionButtonNew,
  postAuditProcess, postAuditProcessNew,
  getLoadTaskDef, getLoadTaskDefNew, getOrgByUserId,
  getProcessLogList, getProcessLogListNew,
} from "@/api/procurement/manage";
import {showSecretRelatedTips} from "@/utils/MyUtils";
import {getViewAttachmentURLByID, getViweFileURL} from "@/api/template/file";
export default {
  name: "add-expert",
  dicts: [
    "education_degree",
    "expert_business_type",
    "expert_type",
    "technical_titles","registered_certificate"
  ],
  data() {
    let checkNum = (rule, value, callback) => {
      if (!/^[1-9]\d*$/.test(value)) {
        callback(new Error("请输入正整数"));
      } else {
        callback();
      }
    };
    return {
      id:'',
      type:'',
      /* 合同模板联想文档预览 */
      templateDialogTitle: "",
      templateDialogVisible: false,
      viewFileUrl:"",
      businessTypeList:[],
      expertTypeList:[],
      expertVisible:false,
      calibrateVisible: false,
      calibrateLoading: false,
      calibrateActive: 1,
      processInformationList: [],
      approveArr: [],
      taskPresentId: "",
      sanctionForm: {
        pass: true,
        rejectTaskKey: "",
        operateComment: "",
      },
      rejectNodeList: [],
      /* 下一步审批人列表 */
      nextCandidateList: [],
      /* 是否可以审批 */
      auditable: false,
      /* 下一步审批人 */
      nextAppointable: false,
      formData: {
        expertName: "",
        expertPhone: "",
        belongOrganization: "",
        department: "",
        educationDegree: "",
        major: "",
        businessType: "",
        expertType: "",
        registeredCertificate: "",
        technicalTitles:""

      }, //form表单数据
      planList: [],
      rules: {
        expertName: [
          {
            required: true,
            message: "专家姓名不能为空",
          },
        ],
        expertPhone: [
          {
            required: true,
            message: "手机号码不能为空",
          },
        ],
        // belongOrganization: [{
        //   required: true,
        //   message: '组织机构不能为空',
        // }],
        // department: [{
        //   required: true,
        //   message: '工作部门不能为空',
        // }],
        educationDegree: [
          {
            required: true,
            message: "学历不能为空",
          },
        ],
        major: [
          {
            required: true,
            message: "专业不能为空",
          },
        ],
        businessType: [
          {
            required: true,
            message: "业态不能为空",
          },
        ],
        expertType: [
          {
            required: true,
            message: "专家类别不能为空",
          },
        ],
      },
      isSubmit: false,
      uploadFileUrl,
    };
  },
  created() {




    if(this.$route.query.id){
      this.id = Base64.decode(this.$route.query.id);
      this.getInfoDetail(this.id)
      console.log("首页审批"+this.id);
    }else if(this.$route.params.params){
      const param = JSON.parse(Base64.decode(this.$route.params.params));
      this.id=param.id
      if(param.type=='check' || param.type=='edit' ){
        this.getInfoDetail(this.id)
      this.type=param.type
        if(param.type=='check'){
          this.isSubmit = true;
        }
      }else{
          console.log("新增"+this.id);
          this.type='edit'
        const {
          nickName: expertName,
          phonenumber: expertPhone,
          dept,
          userId,
          thridOrgName,
        } = param;
        console.log(JSON.stringify(param), "p---p");
        Object.assign(this.formData, {
          expertName,
          expertPhone,
          department: dept.deptName,
          userId,
          belongOrganization: thridOrgName,
        });
      }

    }
  },
  methods: {
    showSecretTips() {
      showSecretRelatedTips(()=>{
        this.$refs['upload'].$refs['upload-inner'].handleClick()
      })
    },
     handleCheckedCitiesChange(value) {
        console.log(JSON.stringify(value))
      },
    handleSubmit() {
      this.$modal.loading("请稍候...");
      const params = {
        ...this.sanctionForm,
        businessId: this.businessId,
        processId: this.processId,
        curTaskId: this.taskPresentId,
        processKey: "jiantou-zhaocai:{org}:ZHAOCAI_EXPERT_ADD",
      };
      postAuditProcessNew(params).then(() => {
        this.$message.success("提交成功");
        this.$modal.closeLoading();
        this.expertVisible = false;
        this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              this.$router.push("/tender-procurement/expert/expert");
            });
      }).catch(error => {
        /* 关闭遮罩层 */
        this.$modal.closeLoading();
      });
    },
    /* 审批详情 */
    async handelCalibrationApproval(row) {
      this.businessId = this.formData.id;
      this.processId = this.formData.wfProcessId;
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        let params = {
          businessId: this.businessId,
          processId: this.processId,
          /* 流程类型 */
          // EXPERT_ADD(1,"专家新增"),
          // EXPERT_CHANGE(2,"专家修改"),
          processType: this.formData.processType,
        };
        let res = null;
        if (this.businessId && this.processId) {
          res = await getLoadTaskDefNew(params);
        }else{
          /* 未提交时查看流程执行流程，根据专家id 获取流程分组 */
          res = await getOrgByUserId(this.formData.userId);
          params = {
            processKey: "jiantou-zhaocai:"+res.data+":ZHAOCAI_EXPERT_ADD",
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

        if (this.businessId && this.processId) {
          const response = await getProcessLogListNew(params);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
    confirmApprove(row) {
      this.expertVisible = true;
      this.getPermissionButton();
    },
    // 审批逻辑
    async getPermissionButton() {
      this.businessId =this.formData.id;
      this.processId = this.formData.wfProcessId;
      try {
        if (this.formData.id) {
          const res = await getPermissionButtonNew({
            businessId: this.formData.id, //联系人id
            processId: this.formData.wfProcessId, //流程id
          });
          this.rejectNodeList = res.data.completedTaskList;
          /* 下一步审批人列表 */
          this.nextCandidateList = res.data.nextCandidateList;
          /* 下一步审批人是否可选 */
          this.nextAppointable = res.data.nextAppointable;
          this.taskPresentId = res.data.curTaskId;
          // this.isShowButton = res.data.auditable;
          /* 当前登录人是否可以审批 */
          this.auditable = res.data.auditable;
        }
      } catch (error) {}
    },
    //点击文件列表中已上传文件进行下载
    async handlePreview(file) {
      console.log(file)
      // var a = document.createElement('a');
      // var event = new MouseEvent('click');
      // a.download = file.name;
      // a.href = file.fileUrl;
      // a.dispatchEvent(event);
      // console.log(file)


      // if (file.url) {
      //   // 在新标签页打开文件
      //   window.open(file.url, "_blank");
      // } else {
      //   this.$message.error("文件无法预览，缺少 URL");
      // }


      //获取附件的预览URL
      if (file.id) {
        this.templateDialogTitle = file.name + "预览";
        this.templateDialogVisible = true;
        //获取文档中台的文档编辑URL
        try {
          const res = await getViewAttachmentURLByID({attachmentId: file.id});
          this.viewFileUrl = res.data;
          console.log("viewFileUrl:", this.viewFileUrl);
        } catch (err) {
          console.log(err);
        }
      } else if(file.response.code === 200){
        /* 新增时未保存附件无附件id时调用 */
        this.templateDialogTitle = file.response.data.name + "预览";
        this.templateDialogVisible = true;
        try {
          const query = { fileName: file.response.data.name?file.response.data.name:'获取不到文件名', fileUrl: file.response.data.url };
          console.log('%c👽 getViweFileURL:query ', `font-size: 20px;background-color: #f00;`, query);
          const res = await getViweFileURL(query);
          this.viewFileUrl = res.data;
          console.log("viewFileUrl:", this.viewFileUrl);
        } catch (err) {
          console.log(err);
        }
      }else{
        console.error('专家附件 数据未正确加载');
      }

    },
    /* 专家附件文件大小上传限制 */
    beforeUpload(file) {
      const isSizeValid = file.size / 1024 / 1024 < 30; // 限制文件大小为 30MB
      const isFormatValid = file.type === 'application/pdf'; // 限制文件格式为 .pdf

      // 校验文件格式
      if (!isFormatValid) {
        this.$message.error('文件格式限制为 PDF！且大小不能超过 30MB');
        return false; // 返回 false 将停止上传
      }

      // 校验文件大小
      if (!isSizeValid) {
        this.$message.error('文件大小不能超过 30MB！且文件格式限制为 PDF');
        return false; // 返回 false 将停止上传
      }

      return true; // 文件通过校验，允许上传
    },
    async getInfoDetail(id) {
        console.log('%c👽 Base64.encode(JSON.stringify(id)) ', `font-size: 20px;background-color: #f00;`, Base64.encode(JSON.stringify(id)));
        const res = await getInfo(id);
        const data=res.data
        this.formData=data
        this.formData.educationDegree=this.formData.educationDegree+""
        this.formData.expertType=data.expertType+""
        this.formData.businessType=data.businessType+""
        this.businessTypeList=data.businessType.split(",");
        this.expertTypeList=data.expertType.split(",");
        this.formData.state=data.state+""
        if(data.registeredCertificate){
          this.formData.registeredCertificate=data.registeredCertificate+""
        }
        if(data.technicalTitles){
          this.formData.technicalTitles=data.technicalTitles+""
        }

      // 遍历数组，新增 name 属性,给文件组件显示文件名称
      this.formData.resumeAttachList = this.formData.resumeAttachList.map(item => {
        return {
          ...item, // 保留原有属性
          name: item.fileName, // 将 fileName 的值赋给 name
          url: item.fileUrl // 将 fileUrl 的值赋给 url
        };
      });


      // SAVE(0,"保存"),
        // IN_APPROVAL(1,"审批中"),
        // REJECT(2,"审批拒绝"),
        // APPROVE(3,"审批通过")
        if(this.formData.state == '1')
          await this.getPermissionButton()
      },
    //保存
    saveForm(formName){
      this.isSubmit = true;
         this.formData.businessType=this.businessTypeList.join(",");
         this.formData.expertType=this.expertTypeList.join(",");
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          console.log(this.formData, "this.formData");
          const loading = this.$loading({
            lock: true,
            text: "数据提交中...",
            background: "rgba(0, 0, 0, 0.7)",
          });
          try {

            await saveExpert(this.formData);
            this.$message({
              message: "保存成功",
              type: "success",
            });
            this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              this.$router.push("/tender-procurement/expert/expert");
            });
          } catch (err) {
            console.log(err);
          }
          loading.close();
          this.isSubmit = false;
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },
    //提交
    submitForm(formName) {
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          console.log(this.formData, "this.formData");
          const loading = this.$loading({
            lock: true,
            text: "数据提交中...",
            background: "rgba(0, 0, 0, 0.7)",
          });
          try {
            await submitExpert(this.formData);
            this.$message({
              message: "提交成功",
              type: "success",
            });
            this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              this.$router.push("/tender-procurement/expert/expert");
            });
          } catch (err) {
            console.log(err);
          }
          loading.close();
          this.isSubmit = false;
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },
    submit() {
      console.log("sub");
    },
    async fileSuccess(res) {
      this.formData.resumeAttachList = null;
      const { url, name } = res.data;
      this.formData.resumeAttachList = [{ fileName: name, fileUrl: url,name: name, url: url }];
    },
    fileRemove() {
      this.$set(this.formData, "resumeAttachList", []);
    },
  },
  watch: {
    expertTypeList(val){
      this.formData.expertType=this.expertTypeList.join(",");
    },
    businessTypeList(val){
      this.formData.businessType=this.businessTypeList.join(",");
    },
  },
  components: {
    BackButton,
    PageTitle,
    ApprovalForm,
    ApprovalDetailsDialog,
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

  &::before {
    content: "";
    height: 20px;
    width: 5px;
    background-color: rgba(41, 65, 137, 1);
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
