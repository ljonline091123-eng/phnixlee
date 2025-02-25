<template>
  <div>
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      label-width="68px"
      @submit.native.prevent
    >
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="small"
          @click="handleQuery"
          >查询</el-button
        >
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-edit"
          size="small"
          @click="handleUpdate"
          :disabled="!selectTemplateData.id ? true : false"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          @click="handleDelete"
          :disabled="!selectTemplateData.id ? true : false"
          >删除</el-button
        >
      </el-col>
    </el-row>

    <el-table
      v-loading="fileLoading"
      :data="fileTemplateList"
      @row-click="selectTemplate"
      border
      stripe
    >
      <el-table-column label="" width="30" align="center">
        <template slot-scope="scope">
          <el-radio v-model="selectTemplateData.id" :label="scope.row.id" />
        </template>
      </el-table-column>
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column
        prop="templateName"
        label="模板名称"
        show-overflow-tooltip
      >
        <template slot-scope="{ row }">
          <a
            href="javascript:;"
            class="link-type"
            @click="handleCheck(row.id)"
            >{{ row.templateName }}</a
          >
        </template>
      </el-table-column>
      <el-table-column prop="createBy" label="维护人"></el-table-column>
      <el-table-column prop="createTime" label="创建日期"></el-table-column>
      <el-table-column
        prop="usingUnitName"
        label="使用单位"
        show-overflow-tooltip
      ></el-table-column>
      <el-table-column prop="contractName" label="合同类型"></el-table-column>
    </el-table>
    <div class="pagination_item">
      <pagination
        v-show="total > 0"
        @pagination="getFileTemplate"
        :total="total"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
      />
    </div>
    <!-- 新增模板 -->
    <el-dialog
      title="补充协议模板"
      :visible.sync="fileTemplateVisible"
      width="70%"
      class="view-dialog"
      @closed="handleClose"
    >
      <el-radio-group
        v-model="contractTabs"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button label="baseInfo" name="baseInfo"
          >补充协议基本信息</el-radio-button
        >
        <el-radio-button label="stamp" name="stamp"
          >补充协议盖章位置</el-radio-button
        >
      </el-radio-group>

      <el-form
        :model="fileForm"
        ref="fileFormRef"
        :rules="fileRules"
        label-width="100px"
      >
        <el-row :gutter="20" v-show="contractTabs === 'baseInfo'">
          <el-col :span="8">
            <el-form-item label="模板名称：" prop="templateName">
              <el-input v-model="fileForm.templateName"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用单位：" prop="usingUnitNo">
              <treeselect
                v-model="fileForm.usingUnitNo"
                :options="treeData"
                clearValueText="清除"
                noOptionsText="暂无数据"
                placeholder="请选择使用单位"
                @select="treeSelect"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合同类型：" prop="contractType">
              <el-select
                style="width: 100%"
                v-model="fileForm.contractType"
                placeholder="请选择合同类型"
              >
                <el-option
                  v-for="dict in contractTypeList"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="维护人：">
              <el-input
                v-model="fileForm.name"
                autocomplete="off"
                disabled
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label=" 模板上传：" prop="fileTemplate" class="uploadItem">
              <el-button size="small" type="primary" style="margin-top: 4px;" @click="showSecretTips">点击上传</el-button>
              <el-upload
                :action="uploadFileUrl"
                :limit="1"
                :on-success="fileSuccess"
                :file-list="fileForm.fileList"
                :accept="'.doc,.docx'"
                :on-remove="fileRemove"
                ref="upload"
                :before-upload="handleBeforeUpload"  
              >
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>

        <el-table
          :data="fileForm.agreementSignStamperList"
          default-expand-all
          border
          stripe
          max-height="250"
          v-show="contractTabs === 'stamp'"
          class="stamp-table"
        >
          <el-table-column width="50">
            <template #header>
              <el-button
                type="success"
                @click="handleStampAdd"
                icon="el-icon-plus"
                circle
                size="mini"
              ></el-button>
            </template>
            <template slot-scope="scope">
              <el-button
                type="danger"
                @click="handleStampRemove(scope.$index)"
                icon="el-icon-minus"
                size="mini"
                circle
              ></el-button>
            </template>
          </el-table-column>
          <el-table-column label="序号" type="index" width="50" />
          <el-table-column label="签署方">
            <template slot-scope="scope">
              <el-form-item
                label-width="0"
                :prop="'agreementSignStamperList.' + scope.$index + '.type'"
                :rules="[
                  { required: true, trigger: 'blur', message: '请选择签署方' },
                ]"
              >
                <el-select
                  v-model="scope.row.type"
                  placeholder="请选择"
                  clearable
                >
                  <el-option label="甲方" value="1" />
                  <el-option label="乙方" value="2" />
                </el-select>
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column label="签署类型">
            <template slot-scope="scope">
              <el-form-item
                label-width="0"
                :prop="'agreementSignStamperList.' + scope.$index + '.signType'"
                :rules="[
                  { required: true, trigger: 'blur', message: '请选择签署方' },
                ]"
              >
                <el-select
                  v-model="scope.row.signType"
                  placeholder="请选择"
                  clearable
                >
                  <el-option
                    v-for="dict in dict.type.agreement_stamper_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column label="关键词">
            <template slot-scope="scope">
              <el-form-item
                label-width="0"
                :prop="'agreementSignStamperList.' + scope.$index + '.keyWord'"
                :rules="[
                  { required: true, trigger: 'blur', message: '请输入关键词' },
                ]"
              >
                <el-input
                  v-model="scope.row.keyWord"
                  placeholder="请输入"
                  clearable
                />
              </el-form-item>
            </template>
          </el-table-column>
        </el-table>
      </el-form>

      <div class="file-box" v-if="attachmentId">
        <iframe allowfullscreen="true"
          v-if="attachmentId"
          :src= this.editFileUrl
          width="100%"
          height="500px"
          frameborder="0"
        ></iframe>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button
          @click="fileTemplateVisible = false"
          style="width: 100px"
          size="small"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="confirmTemplate('fileFormRef')"
          style="width: 100px"
          size="small"
          >确 定</el-button
        >
      </div>
    </el-dialog>
    <!-- 详细 -->
    <el-drawer
      title="查看补充协议模板"
      :visible.sync="openView"
      size="60%"
      direction="rtl"
      @closed="handleCloseView"
    >
      <div class="view-box">
        <PageTitle title="基本信息" />
        <div class="form-body">
          <el-form
            label-position="right"
            label-width="110px"
            size="medium"
            label-suffix=":"
          >
            <el-row :gutter="40">
              <el-col :span="8" class="grid-cell" prop="templateName">
                <el-form-item label="模板名称" prop="templateName">{{
                  templateInfo.templateName
                }}</el-form-item>
              </el-col>
              <el-col :span="8" class="grid-cell" prop="usingUnitName">
                <el-form-item label="使用单位" prop="usingUnitName">{{
                  templateInfo.usingUnitName
                }}</el-form-item>
              </el-col>
              <el-col :span="8" class="grid-cell" prop="name">
                <el-form-item label="维护人" prop="name">{{
                  templateInfo.createBy
                }}</el-form-item>
              </el-col>
              <el-col :span="24" class="grid-cell" prop="fileUrl">
                <el-form-item label="文件名" prop="fileUrl"
                  ><a
                    :href="templateInfo.fileUrl"
                    class="link-type"
                    target="_blank"
                    >{{ templateInfo.fileName }}</a
                  ></el-form-item
                >
              </el-col>
            </el-row>
          </el-form>
        </div>

        <PageTitle title="模板内容" marginBottom="15px" />
        <div class="file-box">
          <iframe allowfullscreen="true"
            v-if="templateInfo.attachmentId"
            :src= this.PreviewFileUrl
            width="100%"
            height="700px"
            frameborder="0"
          ></iframe>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  getFileTemplate,
  saveTemplate,
  getTemplateDetail,
  deleteTemplate,
  addAttachment,
  listOrganizationCall,
  getContractTypeList,
  getPreviewFileUrl,
  editFile,
  removerAmendmentRecord,
} from "@/api/template/file";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { uploadFileUrl, offerService, offerRepo } from "@/utils/const";
import { validatenull } from "@/utils/validate";
import FileModule from "@/components/FileModule/index.vue";
import PageTitle from "@/components/PageTitle/index";
import {showSecretRelatedTips} from "@/utils/MyUtils";
export default {
  name: "file-template",
  dicts: ["agreement_stamper_type"],
  data() {
    return {
      //预览合同模板url
      PreviewFileUrl:"",
      editFileUrl:"",  //编辑合同模板URL
      fileTemplateList: [],
      // 总条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        templateType: 3,
        templateName: undefined,
      },
      fileLoading: false,
      title: "",
      fileTemplateVisible: false,
      fileForm: {
        templateName: "",
        usingUnitNo: null,
        contractType: "",
        name: "",
        fileTemplate: [],
        agreementSignStamperList: [],
      },
      fileRules: {
        templateName: [{ required: true, message: "模板名称不能为空" }],
        usingUnitNo: [{ required: true, message: "请选择使用单位" }], //注释
        fileTemplate: [{ required: true, message: "请选择模板文件" }],
        contractType: [{ required: true, message: "请选择合同类型" }],
      },
      attachmentId: "",
      offerService,
      offerRepo,
      uploadFileUrl,
      selectTemplateData: {},
      openView: false,
      templateInfo: {},
      treeData: [],
      unitId: "",
      contractTypeList: [],
      contractTabs: "baseInfo",
    };
  },
  components: {
    FileModule,
    PageTitle,
    Treeselect,
  },
  created() {
    this.getFileTemplate();
    this.fileForm.name = this.$store.state.user.nickname;
    this.getContractTypeList();
  },
  methods: {
    showSecretTips() {
      showSecretRelatedTips(()=>{
        this.$refs['upload'].clearFiles();  // 使用 clearFiles 方法清除文件列表
        this.$refs['upload'].$refs['upload-inner'].handleClick()
      })
    },
    /* 在模板文件上传前处理逻辑 */
    handleBeforeUpload(file) {
      //限制上传的文件名长度
      const fileName = file.name;
        if (fileName.length > 80) {
            this.$message.error('文件名不能超过80个字符');
            return false; // 阻止上传
      }
      //限制上传文件类型
      const allowedTypes = ['application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
      if (!allowedTypes.includes(file.type)) {
        this.$message.error('不支持上传该格式的文件,请上传Word文件');
        return false;
      }
      return true;  // 返回 true 表示允许继续上传
    },
    /** 查询采购计划列表 */
    async getFileTemplate() {
      this.fileLoading = true;
      try {
        const query = { ...this.queryParams };
        const res = await getFileTemplate(query);
        this.fileTemplateList = res.data.rows;
        this.total = res.data.total;
      } catch (err) {
        console.log(err);
      }
      this.fileLoading = false;
    },
    getContractTypeList() {
      getContractTypeList().then((res) => {
        this.contractTypeList = res.data;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getFileTemplate();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.id);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    // 任务状态修改
    handleStatusChange(row) {
      let text = row.state === 1 ? "启用" : "停用";
      this.$modal
        .confirm('确认要"' + text + '""' + row.name + '"模板吗？')
        .then(function () {
          return updateStatus(row.id, row.state);
        })
        .then(() => {
          this.$modal.msgSuccess(text + "成功");
        })
        .catch(function () {
          row.state = row.state === 0 ? 1 : 0;
        });
    },
    /** 新增按钮操作 */
    async handleAdd() {
      // this.$router.push(`/template/add-rating`);
      this.fileTemplateVisible = true;
      this.listOrganization4Company();   //注释
    },
    /** 删除按钮操作 */
    handleDelete() {
      const { id, name } = this.selectTemplateData;
      this.$modal
        .confirm(`是否确认删除名称为"${name}"的合同模板吗？`)
        .then(async () => {
          try {
            const res = await deleteTemplate(id);
            this.getFileTemplate();
            this.$message.success("删除成功");
          } catch (err) {
            console.log(err);
          }
        });
    },
    handleClose() {
      this.$refs.fileFormRef.resetFields();
      this.$set(this.fileForm, "fileList", []);
      this.$set(this.fileForm, "fileTemplate", []);
      this.$set(this.fileForm, "agreementSignStamperList", []);
      this.contractTabs = "baseInfo";
      this.fileForm.id = "";
      this.attachmentId = "";
      this.editFileUrl="";
    },
    async fileSuccess(res) {
      const { url, name } = res.data;
      this.fileForm.fileTemplate = [{ fileName: name, fileUrl: url }];
      this.$refs.fileFormRef.clearValidate("fileTemplate");
      try {
        const res = await addAttachment({ fileName: name, fileUrl: url });
        this.attachmentId = res.data;
      } catch (err) {
        console.log(err);
      }
      //上传文件后，先去除文档原来的修订记录
      try {
        const res = await removerAmendmentRecord({attachmentId: this.attachmentId});
      } catch (err) {
        console.log(err);
      }
      //上传文件后，返回文档中台的文档编辑URL
      try {
        const query1 = {id:this.attachmentId, fileName: name, fileUrl: url };
        const res = await editFile(query1);
        console.log("fileName URL:",name);
        console.log("URL:",url);
        this.editFileUrl = res.data;
        console.log("editFileUrl:",this.editFileUrl);
      } catch (err) {
        console.log(err);
      }
    },
    fileRemove() {
      this.$set(this.fileForm, "fileList", []);
      this.$set(this.fileForm, "fileTemplate", []);
      this.attachmentId = "";
    },
    //保存模板
    confirmTemplate(formName) {
      console.log("保存--this.fileForm",this.fileForm);
      console.log("保存--this.unitId",this.unitId);
      if(!this.unitId) {
        this.$set(this.fileForm, "usingUnitNo", undefined);
      }
      this.$refs[formName].validate(async (valid, obj) => {
        let isNull = validatenull(obj);
        if (!isNull) {
          for (const [key, value] of Object.entries(obj)) {
            this.$message.error(value[0]);
            break;
          }
          return false;
        }
        // if (!this.fileForm.agreementSignStamperList.length)
        //   return this.$message.error("请先设置合同盖章位置");
        if (valid) {
          const { attachmentId } = this;
          const {
            templateName,
            id,
            contractType,
            usingUnitName,
            agreementSignStamperList,
          } = this.fileForm;
          const formData = {
            templateType: 3,
            templateName,
            attachmentId,
            contractType,
            usingUnitNo: this.unitId,
            usingUnitName,
            id: id ? id : undefined,
            agreementSignStamperList,
          };
          console.log(this.fileForm, "this.fileForm-this.fileForm");
          console.log(formData, "formData");
          try {
            const res = await saveTemplate(formData);
            this.$message.success("保存成功");
            this.fileTemplateVisible = false;
            this.getFileTemplate();
          } catch (err) {
            console.log(err);
          }
        }
      });
    },
    //修改按钮
    async handleUpdate() {
      const { id } = this.selectTemplateData;
      this.fileTemplateVisible = true;
      await this.listOrganization4Company(); //注释
      try {
        const res = await getTemplateDetail(id);
        const { templateName, fileName, fileUrl, attachmentId, usingUnitNo } =
          res.data;
        this.$set(this.fileForm, "templateName", templateName);
        this.$set(this.fileForm, "fileList", [
          { url: fileUrl, name: fileName },
        ]);
        this.$set(this.fileForm, "fileTemplate", [
          { url: fileUrl, name: fileName },
        ]);
        this.$set(this.fileForm, "contractType", res.data.contractType);
        this.$set(
          this.fileForm,
          "agreementSignStamperList",
          res.data.agreementSignStamperList
        );
        this.fileForm.usingUnitNo = usingUnitNo;
        this.unitId = usingUnitNo;
        this.attachmentId = attachmentId;
        this.fileForm.id = id;
        console.log(res, "详情");

        //点击修改按钮，返回文档中台的文档编辑URL
        try {
          const query1 = {id:this.attachmentId, fileName: fileName, fileUrl: fileUrl };
          const res = await editFile(query1);
          console.log("fileName URL:",fileName);
          console.log("URL:",fileUrl);
          this.editFileUrl = res.data;
          console.log("editFileUrl:",this.editFileUrl);
        } catch (err) {
          console.log(err);
        }
      } catch (err) {
        console.log(err);
      }
    },
    /** 选择模板 */
    selectTemplate(row) {
      console.log(row, "aaaa");
      this.selectTemplateData = { id: row.id, name: row.templateName };
    },
    /** 查看模板 */
    async handleCheck(id) {
      this.openView = true;
      try {
        const res = await getTemplateDetail(id);
        const {
          templateName,
          fileName,
          fileUrl,
          attachmentId,
          usingUnitName,
          createBy,
        } = res.data;
        this.templateInfo = {
          templateName,
          fileName,
          fileUrl,
          attachmentId,
          usingUnitName,
          createBy,
        };
      } catch (err) {
        console.log(err);
      }
      //获取模板附件预览的url
      try {
        const res2 = await getPreviewFileUrl(id);
        console.log("res2",res2)
        this.PreviewFileUrl = res2.data;
      } catch (err) {
        console.log(err);
      }
    },
    //关闭查看模板
    handleCloseView() {
      console.log("关了");
      this.templateInfo = {};
      this.PreviewFileUrl="";
    },
    normalizeOptions(options) {
      const normalizedOptions = [];
      if (options) {
        for (const option of options) {
          // 创建一个规范化选项对象，将id和label属性映射到该对象中
          const normalizedOption = {
            id: option.organizationId,
            label: option.organizationName,
            disabled: option.organizationType === "2",
            organizationId: option.organizationId,
          };
          // 检查当前选项是否有子选项
          if (option.children && option.children.length > 0) {
            // 如果有子选项，递归调用normalizeOptions方法对子选项进行规范化
            // 并将规范化后的子选项数组赋值给当前选项的children属性
            normalizedOption.children = this.normalizeOptions(option.children);
          }
          // 将规范化后的选项对象添加到normalizedOptions数组中
          normalizedOptions.push(normalizedOption);
        }
      }
      return normalizedOptions;
    },
    //获取使用模板的公司
    async listOrganization4Company() {
      const res = await listOrganizationCall();
      this.treeData = this.normalizeOptions(res.data);
    },
    //清除校验
    treeSelect(value) {
      if (value.disabled) {
        this.$modal.msgError("该单位不能选择");
        // this.fileForm.usingUnitName = "";
        // this.fileForm.usingUnitNo = "";
        this.$set(this.fileForm, "usingUnitName", undefined);
        this.$set(this.fileForm, "usingUnitNo", undefined);
        this.unitId = undefined;
      } else {
        this.fileForm.usingUnitName = value.label;
        this.unitId = value.organizationId;
        this.$refs.fileFormRef.clearValidate("usingUnitNo");
      }
    },
    //添加盖章位置
    handleStampAdd() {
      this.fileForm.agreementSignStamperList.push({});
    },
    handleStampRemove(index) {
      this.fileForm.agreementSignStamperList.splice(index, 1);
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
//弹窗样式
.custom-dialog {
  height: 90vh; /* 设置为视口高度的 90% */
  display: flex;
  flex-direction: column;
}
.view-box {
  padding: 0 20px;
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
    padding: 20px 0;
    .el-form-item {
      margin: 0;
    }
  }
}
::v-deep .vue-treeselect {
  width: 330px;
  height: 38px;
  line-height: 38px;
}
.file-box {
  min-height: 500px;
}
.stamp-table .el-form-item {
  margin-bottom: 0;
}
//弹窗大小
::v-deep  .view-dialog .el-dialog .el-dialog__body {
max-height: 80vh !important;
}
</style>
