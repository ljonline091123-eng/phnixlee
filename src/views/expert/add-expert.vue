<template>
  <div class="app-container">
    <BackButton path="/tender-procurement/expert/expert" title="专家信息新增">
      <div>
        <el-button
          type="primary"
          plain
          size="mini"
          :disabled="isSubmit"
          @click="$tab.closePage()"
          >取消</el-button
        >
      <el-button
        v-if="type=='edit'"
        type="primary"
        size="mini"
        @click="saveForm('form')"
        :disabled="isSubmit"
        :loading="isSubmit"
        >{{ isSubmit ? "保存中..." : "保存" }}</el-button
      >
        <el-button
          v-if="type=='edit'"
          type="primary"
          size="mini"
          @click="submitForm('form')"
          :disabled="isSubmit"
          :loading="isSubmit"
          >{{ isSubmit ? "提交中..." : "提交" }}</el-button>
        <el-button
          v-if="type=='check'"
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
                  :disabled="formData.id ? true : false || isSubmit"
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
                  :disabled="formData.id ? true : false || isSubmit"
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
                  :disabled="formData.id ? true : false || isSubmit"
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
                <el-select
                  v-model="formData.expertType"
                  placeholder="请选择专家类别"
                  style="width: 100%"
                  :disabled="isSubmit"
                >
                  <el-option
                    v-for="dict in dict.type.expert_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="16" class="grid-cell">
              <el-form-item
                label="业态"
                prop="businessType"
                class="required label-right-align"
              >
                <el-radio-group
                  v-model="formData.businessType"
                  :disabled="isSubmit"
                >
                  <el-radio
                    v-for="dict in dict.type.expert_business_type"
                    :key="dict.value"
                    :label="dict.value"
                    >{{ dict.label }}</el-radio
                  >
                </el-radio-group>
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
                class="required label-right-align"
              >
                <el-upload
                  :action="uploadFileUrl"
                  :limit="1"
                  accept=".pdf, .doc, .docx"
                  :on-success="fileSuccess"
                  :file-list="formData.resumeAttachList"
                  :on-remove="fileRemove"
                  :on-preview="handlePreview"
                >
                  <el-button size="small" type="primary">点击上传</el-button>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
    </div>
    <!-- 审批和审批详情 -->
    <ApprovalForm
      :visible.sync="expertVisible"
      :title="'新增专家审批流程'"
      :formModel="sanctionForm"
      :rejectNodeList="rejectNodeList"
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
  getPermissionButton,
  postAuditProcess,
  getLoadTaskDef,
  getProcessLogList,
} from "@/api/procurement/manage";
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
    handleSubmit() {
      this.$modal.loading("请稍候...");
      const params = {
        ...this.sanctionForm,
        businessId: this.businessId,
        processId: this.processId,
        curTaskId: this.taskPresentId,
        processKey: "jiantou-zhaocai:{org}:ZHAOCAI_EXPERT_ADD",
      };
      postAuditProcess(params).then(() => {
        this.$message.success("提交成功");
        this.$modal.closeLoading();
        this.expertVisible = false;
        this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              this.$router.push("/tender-procurement/expert/expert");
            });
      });
    },
    async handelCalibrationApproval(row) {
      this.businessId = this.formData.id;
      this.processId = this.formData.wfProcessId;
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        const params = {
          businessId: this.businessId,
          processId: this.processId,
        };
        if (this.businessId && this.processId) {
          const res = await getLoadTaskDef(params);
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
          const response = await getProcessLogList(params);
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
          const res = await getPermissionButton({
            businessId: this.formData.id, //联系人id
            processId: this.formData.wfProcessId, //流程id
          });
          this.rejectNodeList = res.data.completedTaskList;
          this.taskPresentId = res.data.curTaskId;
          // this.isShowButton = res.data.auditable;
        }
      } catch (error) {}
    },
    //点击文件列表中已上传文件进行下载
    handlePreview(file) {
      var a = document.createElement('a');
      var event = new MouseEvent('click');
      a.download = file.name;
      a.href = file.fileUrl;
      a.dispatchEvent(event);
      console.log(file)
    },

    async getInfoDetail(id) {
        const res = await getInfo(id);
        const data=res.data
        this.formData=data
        this.formData.educationDegree=this.formData.educationDegree+""
        this.formData.registeredCertificate=data.registeredCertificate+""
        this.formData.technicalTitles=data.technicalTitles+""
        this.formData.expertType=data.expertType+""
        this.formData.businessType=data.businessType+""
        this.formData.state=data.state+""

      },
    //保存
    saveForm(formName){
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
      const { url, name } = res.data;
      this.formData.resumeAttachList = [{ fileName: name, fileUrl: url }];
      this.$refs.fileFormRef.clearValidate("fileTemplate");
    },
    fileRemove() {
      this.$set(this.formData, "fileList", []);
      this.$set(this.formData, "fileTemplate", []);
      this.attachmentId = "";
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
