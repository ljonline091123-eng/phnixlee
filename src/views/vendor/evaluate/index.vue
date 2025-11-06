<template>
  <div>
    <!-- 查询表单 -->
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      @submit.native.prevent
    >
      <el-form-item label="编号" prop="evaluateCode" label-width="100px">
        <el-input
          v-model="queryParams.evaluateCode"
          placeholder="请输入编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商名称" prop="vendorName" label-width="100px">
        <el-input
          v-model="queryParams.vendorName"
          placeholder="请输入供应商名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评价人" prop="updateBy" label-width="100px">
        <el-input
          v-model="queryParams.updateBy"
          placeholder="请输入评价人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="evaluateStatus" label-width="100px">
        <el-select v-model="queryParams.evaluateStatus" placeholder="请选择" style="width: 100%" clearable>
          <el-option v-for="dict in dict.type.evaluate_status" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="评价类型" prop="evaluateType" label-width="100px" >
        <el-select v-model="queryParams.evaluateType" placeholder="请选择" style="width: 100%" clearable>
          <el-option v-for="dict in dict.type.evaluate_type" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="是否合格" prop="isQualified" label-width="100px" >
        <el-select v-model="queryParams.isQualified" placeholder="请选择" style="width: 100%" clearable>
          <el-option v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="评价时间" prop="updateTime" label-width="100px">
        <el-date-picker v-model="queryParams.updateTime" type="date" style="width:215px" placeholder="选择日期"
                        format="yyyy-MM-dd" value-format="yyyy-MM-dd" ></el-date-picker>
      </el-form-item>
      <el-form-item >
        <el-button
          type="primary"
          icon="el-icon-search"
          size="small"
          @click="handleQuery"
          >查询</el-button
        >
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          >新增</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-upload"
          size="mini"
          @click="handleExport"
          >导出</el-button
        >
      </el-col>
    </el-row>


<!-- 项目列表 -->
    <el-table
      v-loading="loading"
      :data="projectList"
      @row-click="selectProject"
      style="width: 100%"
      stripe
      border
    >
      <el-table-column width="55" align="center" type="index" label="序号" />



      <el-table-column label="编号" prop="evaluateCode" align="center" />
      <el-table-column label="供应商名称" prop="vendorName" align="center" show-overflow-tooltip/>
      <el-table-column label="状态" prop="evaluateStatusName" align="center" />
      <el-table-column label="评价类型" prop="evaluateTypeName" align="center" />
      <el-table-column label="评价周期" prop="evaluateTimeTxtName" align="center" />
      <el-table-column label="评价分数" prop="evaluateFraction" align="center" />
      <el-table-column label="是否合格" prop="isQualifiedName" align="center" />
      <!--<el-table-column label="合作数量" prop="evaluateFraction" align="center" />-->
      <el-table-column label="评价时间" prop="updateTime" align="center" />
      <el-table-column label="评价人" prop="updateBy" align="center" />
      <el-table-column
        fixed="right"
        label="操作"
        width="120" align="center">
        <template slot-scope="scope">
          <el-button @click="handleClick(scope.row)" type="text" size="small">查看</el-button>
          <el-button type="text" size="small" @click="handleUpdate(scope.row)" >编辑</el-button>
          <el-button type="text" size="small" v-if="scope.row.evaluateStatus == '0'" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页器 -->
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="queryParams.pageNumber"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="queryParams.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    />

    <!-- 新增弹窗 -->
    <el-dialog title="评价信息" :visible.sync="dialogVisible" width="80%" @before-close="handleDialogClose">
      <el-form :model="form"  ref="form" :rules="rules" label-width="150px" @submit.native.prevent :disabled="isSubmit">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="编号" prop="evaluateCode">
              <el-input type="text" clearable :readonly="true" disabled v-model="form.evaluateCode" placeholder="系统自动为您生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="vendorName">
              <el-autocomplete
                style="width:100%;"
                ref="autoCompleteRef"
                v-model="form.vendorName"
                :fetch-suggestions="querySearchAsync"
                placeholder="请输入内容"
                @select="handleSelect"
                @focus="getBackupValue"
                @blur="handleBlur"
              ></el-autocomplete>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="评价类型" prop="evaluateType">
              <el-select v-model="form.evaluateType" placeholder="请选择" style="width: 100%" @change="typeChange">
                <el-option v-for="dict in dict.type.evaluate_type" :key="dict.value"
                           :label="dict.label" :value="dict.value" ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评价周期" prop="evaluateTime">
              <el-date-picker v-model="form.evaluateTime" type="year" style="width:50%" placeholder="选择年"
                              v-if="form.evaluateType == '2'"
              ></el-date-picker>
              <el-select v-model="form.evaluateTimeTxt" placeholder="选择季度" style="width: 50%" v-if="form.evaluateType == '2'">
                <el-option v-for="dict in dict.type.evaluate_time" :key="dict.value"
                           :label="dict.label" :value="dict.value"></el-option>
              </el-select>
              <el-date-picker v-model="form.evaluateTime" type="year" style="width:100%" placeholder="选择年"
                               v-else-if="form.evaluateType == '3'"></el-date-picker>
              <el-date-picker v-model="form.evaluateTime" type="month" style="width:100%" placeholder="选择月"
                               v-else></el-date-picker>
            </el-form-item>
          </el-col>

        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="评价分数" prop="evaluateFraction">
              <el-input v-model="form.evaluateFraction"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评价人" prop="updateBy">
              <el-input type="text" clearable :readonly="true" disabled v-model="form.updateBy" placeholder="系统自动为您生成" />
            </el-form-item>
          </el-col>

        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="评价时间" prop="updateTime">
              <el-input type="text" clearable :readonly="true" disabled v-model="form.updateTime" placeholder="系统自动为您生成" />
            </el-form-item>
          </el-col>

        </el-row>
        <el-row :gutter="40">
          <el-col :span="12" class="grid-cell">
            <el-form-item
              label="评分附件"
              prop="fileList"
              class="required label-right-align uploadItem"
            >
              <el-button size="small" type="primary" style="margin-top: 8px;" @click="showSecretTips" :disabled="isSubmit">点击上传</el-button>
              <el-upload
                :action="uploadFileUrl"
                :limit="1"
                accept=".pdf"
                :on-success="fileSuccess"
                :file-list="form.fileList"
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
      </el-form>
      <span slot="footer" class="dialog-footer" v-if="!isSubmit">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" v-if="form.evaluateStatus == '0'">保存</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </span>
    </el-dialog>
  </div>



</template>

<script>
import {AlldeptTreeSelect} from "@/api/system/project";
import {deptTreeSelect} from "@/api/system/user";
import {listProject,saveMinProjectInfo,getMinProjectById,deleteProject,BusinessTypeTreeSelect,CertificationTypeTreeSelect,dictProjectTypeTreeSelect,listAreaDivisionTree} from "@/api/system/project";
import { getDicts as getDicts } from '@/api/system/dict/data'
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { uploadFileUrl } from "@/utils/const";
import {showSecretRelatedTips} from "@/utils/MyUtils";
import { getVendorName } from "@/api/vendor/vendor";
import { getlist,save,submit,addBean,getById,deleteById } from "@/api/vendor/evaluate";

export default {
  dicts: ['sys_yes_no','evaluate_status','evaluate_type','evaluate_time'],
  components: { Treeselect },
  data() {
    return {
      //  遮罩层
      loading: true,
      // 查询参数
      queryParams: {
        evaluateCode: "",
        vendorName: "",
        updateBy: "",
        evaluateStatus: "",
        evaluateType: "",
        isQualified: "",
        updateTime: null,
        pageNumber: 1,
        pageSize: 10,
      },
      dictData: {}, // 将字典数据存储到 data 中
      // 选中项目数据
      selectProjectData: {},
      selectedId: null, // 用于绑定单选框
      // 项目列表
      projectList: [],
      // 总记录数
      total: 0,
      // 部门树选项
      deptOptions: [],
      // 业务分类树选项
      businessTypeOptions: [],
      // 资质类别树选项
      zizhiOptions: [],
      //行政区划
      prjAddrOptions:[],
      // 工程分类树选项
      dictProjectOptions: [],
      //新增弹窗显示
      dialogVisible: false,
      //资质树选项
      cascaderProps: {
        checkStrictly: false,
        expandTrigger: 'hover',
        emitPath: false,
        value: 'value', // 假设节点的值是 `id`
        label: 'label', // 假设节点的标签是 `name`
        children: 'children' // 假设子节点是 `children`
      },
        //行政区划选项
        prjAddrProps: {
          value: 'value', // 假设节点的值是 `id`
        label: 'label', // 假设节点的标签是 `name`
        children: 'children' // 假设子节点是 `children`
      },
      //新增项目表单
      form: {
        id: null,
        evaluateCode: null,
        vendorId: null,
        vendorName: null,
        evaluateType: null,
        evaluateTime: null,
        evaluateFraction: null,
        createBy: null,
        createId: null,
        createTime: null,
        updateBy: null,
        updateId: null,
        updateTime: null,
        delFlag: null,
        evaluateStatus: "0",
        isQualified: null,
        evaluateTimeTxt: null,
        fileList:[],
      },

      rules: {
        vendorName: [
          { required: true, message: '不能为空', trigger: 'blur' }
        ],
        evaluateType: [
          { required: true, message: '不能为空', trigger: 'change' }
        ],
        evaluateTime: [
          { required: true, message: '不能为空', trigger: 'change' }
        ],
        evaluateFraction: [
          { required: true, message: '不能为空', trigger: 'change' },
          {
            pattern: /^(100(\.0{1,2})?|(\d{1,2}(\.\d{1,2})?))$/,
            trigger: 'blur',
            message: '请输入0-100的数字，小数位不超过2位',
          },
        ],
      },
      expireTimeOption: {
        disabledDate(time) {
          return time.getTime() < Date.now() - 8.64e7;  // 禁用小于当前日期的日期
        }
      },
      expireTimeOverOttion:{
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
        }
      },
      isSubmit: false,
      uploadFileUrl,
      backupValue: '', // 用于存储之前选择的值
      hasSelected: false // 选择状态标记

    };
  },
  created() {
    this.getProjectList();
  },
  methods: {
    getBackupValue() {
      this.backupValue = this.form.vendorName;
      this.hasSelected = false;
    },
    handleBlur() {
      if (!this.hasSelected) {
        this.form.vendorName = this.backupValue;
        // 清空建议列表（需操作DOM）
        /*this.$refs.autoComplete.suggestions = [];
        this.$refs.autoComplete.highlightedIndex = -1;*/
      }
    },
    // 保存项目信息
    handleSave: function() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          console.log("保存信息：", this.form);
          save(this.form).then(response => {
            this.$message.success("保存成功");
            this.dialogVisible = false;
            this.resetForm();
            this.getProjectList();
          }).catch(error => {
            this.$message.error("保存失败：" + error.message);
          });
        } else {
          this.$message.error("表单校验未通过，请检查输入");
          return false;
        }
      });
    },

    handleSubmit: function() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          console.log("确认信息：", this.form);
          submit(this.form).then(response => {
            this.$message.success("确认成功");
            this.dialogVisible = false;
            this.resetForm();
            this.getProjectList();
          }).catch(error => {
            this.$message.error("确认失败：" + error.message);
          });
        } else {
          this.$message.error("表单校验未通过，请检查输入");
          return false;
        }
      });
    },

    // 清空表单
    resetForm() {
      this.form = {
        id: null,
        evaluateCode: null,
        vendorId: null,
        vendorName: null,
        evaluateType: null,
        evaluateTime: null,
        evaluateFraction: null,
        createBy: null,
        createId: null,
        createTime: null,
        updateBy: null,
        updateId: null,
        updateTime: null,
        delFlag: null,
        evaluateStatus: "0",
        isQualified: null,
        evaluateTimeTxt: null,
        fileList:null,
      };
    },

    handleChange(value, selectedData) {
      console.log('Selected Value:', value); // 当前选中的值数组

    },
    handleXZChange(value,selectedData) {
      if (value) {
        const checkedNodes = this.$refs['prjAddr'].getCheckedNodes();
        if (checkedNodes && checkedNodes.length > 0) {
          this.form.prjAddr = checkedNodes[0].path ? checkedNodes[0].path : [];
        } else {
          this.form.prjAddr = '';
          console.log("没有选中的节点");
        }
        console.log("资质变化的value：",checkedNodes);
        console.log("资质变化的this.form.prjAddr",this.form.prjAddr);
      }
    },
    // 处理资质选择变化
    handleZiZhiChange(value,selectedData) {
      debugger;
      if (value) {
        const checkedNodes = this.$refs['ziZhiselect'].getCheckedNodes();
        if (checkedNodes && checkedNodes.length > 0) {
          this.form.ziZhi = checkedNodes[0].path ? checkedNodes[0].path.join(',') : '';
          console.log("资质变化的value：", checkedNodes);
          console.log("资质变化zizhiselect：", this.form.ziZhiselect);
          console.log("资质变化的this.form.ziZhi：", this.form.ziZhi);
        } else {
          this.form.ziZhi = '';
          console.log("没有选中的节点");
        }
        console.log("资质变化的value：",checkedNodes);
        console.log("资质变化zizhiselect：",this.form.ziZhiselect);
        console.log("资质变化的this.form.ziZhi：",this.form.ziZhi);
      }
    },
    // 递归查找节点
    findNodeByValue(options, value, path = []) {
      for (const option of options) {
        path.push(option.value );
        if (option.value === value && path.length==3) {
          option.level = path.length;
          option.pathValue = path.join(',');
          return option;
        }
        if (option.children) {
          const result = this.findNodeByValue(option.children, value, path);
          if (result) {
            return result;
          }
        }
        path.pop();
      }
      return null;
    },

    // 获取数据字典
    loadDictData() {
      const dictTypes = ['project_format', 'project_funds_source', 'project_manage_model'];
      const promises = dictTypes.map(dictType => {
        return getDicts(dictType).then(response => {
          if (!this.dictData) {
            this.dictData = {}; // 初始化 dictData 为空对象
          }
          this.dictData[dictType] = response.data;
          console.log(`字典 ${dictType} 加载完成`, response.data);
        });
      });
      Promise.all(promises).then(() => {
        this.getProjectList();
      });
    },
    /**
     * 根据字典类型和 dictValue 值获取 label
     */
    getDictLabel(dictType, dictValue) {
      const dict = this.dictData[dictType] || [];
      const item = dict.find(d => d.dictValue == dictValue);
      return item ? item.dictLabel : dictValue;
    },

    selectProject(row) {
      this.selectProjectData= { id: row.id, name: row.minAccountFullName };
      this.selectedId = row.id; // 更新单选框的选中状态
      console.log(this.selectProjectData, "选中了selectProjectData");
    },


    // 获取项目列表
    getProjectList() {
      // 模拟获取数据
      this.loading = true;
      getlist(this.queryParams).then(
        (response) => {
          this.projectList = response.data.rows;
          this.total = response.data.total;
          this.loading = false;
          console.log("查询项目列表list", response.data.rows);
        }
      );
    },
    // 查询按钮操作
    handleQuery() {
      this.queryParams.pageNumber = 1;
      console.log(this.queryParams);
      debugger
      this.getProjectList();
    },
    // 新增按钮操作
    handleAdd() {
      console.log("新增");
      this.resetForm(); // 重置表单
      this.isSubmit = false;
      this.dialogVisible = true;
    },
    async handleUpdate(row) {
      this.resetForm();
      try {
        const res = await getById(row.id);
        this.form = res.data;
        this.isSubmit = false;
        this.dialogVisible = true;
      } catch (err) {
        console.log(err);
      }
    },
    async handleClick(row) {
      this.resetForm();
      try {
        const res = await getById(row.id);
        this.form = res.data;
        this.isSubmit = true;
        this.dialogVisible = true;
      } catch (err) {
        console.log(err);
      }
    },

    // 新增项目弹窗的关闭事件
    handleDialogClose() {
      this.resetForm(); // 关闭弹窗时清空表单
    },
    // 删除按钮操作
    handleDelete(row) {
      this.$modal
        .confirm('是否确认删除数据项？')
        .then(async () => {
          try {
            const res = await deleteById(row.id);
            this.getProjectList();
            this.$message.success("删除成功");
          } catch (err) {
            console.log(err);
          }
        });
    },

    // 选中项变化
    handleSelectionChange(selection) {

    },
    // 分页大小变化
    handleSizeChange(val) {
      this.queryParams.pageSize = val;
      this.getProjectList();
    },
    // 当前页变化
    handleCurrentChange(val) {
      this.queryParams.pageNumber = val;
      this.getProjectList();
    },
    async fileSuccess(res) {
      this.form.fileList = null;
      const { url, name } = res.data;
      this.form.fileList = [{ fileName: name, fileUrl: url,name: name, url: url }];
    },
    fileRemove() {
      this.$set(this.form, "fileList", []);
    },
    async handlePreview(file) {
      console.log(file)
      if (file.url) {
        // 在新标签页打开文件
        window.open(file.url, "_blank");
      } else {
        this.$message.error("文件无法预览，缺少 URL");
      }
    },
    /** 下载模板文件 */
    downloadFile(file) {
      console.log("下载文件url：",file.fileUrl);
      if (file.id) {
        window.open(file.fileUrl, '_blank');
      } else {
        console.error('附件数据未正确加载');
      }
    },
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
    showSecretTips() {
      showSecretRelatedTips(()=>{
        this.$refs['upload'].$refs['upload-inner'].handleClick()
      })
    },
    querySearchAsync(name, callback) {
      var list = [{}];
      //从后台获取到对象数组
      getVendorName(name).then((response)=>{
        //在这里为这个数组中每一个对象加一个value字段, 因为autocomplete只识别value字段并在下拉列中显示
        for(let i of response.data){
          i.value = i.dictLabel;  //将想要展示的数据作为value
        }
        list = response.data;
        callback(list);
      }).catch((error)=>{
        console.log(error);
      });
    },
    handleSelect(item) {
      console.log(item);
      debugger;
      this.hasSelected = true;
      this.form.vendorId=item.dictValue;
      this.form.vendorName=item.dictLabel;
      //do something
    },
    typeChange(val) {
      debugger;
      this.form.evaluateTime=null;
      this.form.evaluateTimeTxt=null;
    },
    clearSearch(val) {
      console.log(val);
      this.form.vendorId=null;
      this.form.vendorName=null;
    },
    // 将月份值格式化为季度显示（如 "2025-01" → "2025年Q1"）
    formatQuarter(date) {
      debugger;
      const year = new Date(date).getFullYear();
      const month = new Date(date).getMonth() + 1;
      const quarter = Math.ceil(month / 3);
      return `${year}年Q${quarter}`;
    },
    // 将季度显示解析回月份值（如 "2025年Q1" → "2025-01"）
    parseQuarter(displayText) {
      const [year, q] = displayText.match(/(\d+)年Q(\d)/).slice(1);
      return `${year}-${(parseInt(q) - 1) * 3 + 1}`.padStart(2, '0');
    },
    // 处理实际季度值
    handleQuarterChange(date) {
      const month = new Date(date).getMonth() + 1;
      this.form.evaluateTimeTxt = `Q${Math.ceil(month / 3)}`;
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(
        "business/vendor/evaluate/export",
        {
          ...this.queryParams,
        },
        `供应商评价_${new Date().getTime()}.xlsx`
      );
      /*const queryParams = this.queryParams;
      this.$confirm('是否确认导出所有数据项?', "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(function() {
        this.download(
          "business/vendor/evaluate/export",
          {
            ...this.queryParams,
          },
          `供应商评价_${new Date().getTime()}.xlsx`
        );
      })*/
    },
  },


};
</script>

<style scoped>
.el-form-item__label {
  float: left;
  line-height: 32px; /* 根据你的输入框高度调整 */
}
</style>
