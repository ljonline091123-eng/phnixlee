<template>
  <div>
    <!-- 查询表单 -->
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      label-width="85px"
      @submit.native.prevent
    >
      <el-form-item label="项目名称" prop="minAccountFullName" style="width: 30%;">
        <el-input
          v-model="queryParams.minAccountFullName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目编码" prop="minAccountCode" style="width: 30%;">
        <el-input
          v-model="queryParams.minAccountCode"
          placeholder="请输入项目编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item prop="deptId" style="width: 30%;">
        <el-row type="flex" align="middle">
          <el-col :span="6">
            <label class="el-form-item__label">归属管理部门</label>
          </el-col>
          <el-col :span="18">
            <treeselect
              v-model="queryParams.deptId"
              :options="deptOptions"
              :show-count="true"
              placeholder="请选择归属部门"
            />
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item style="width: 5%;">
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
          type="primary"
          icon="el-icon-edit"
          size="mini"
          @click="handleUpdate"
          :disabled="!selectProjectData.id ? true : false"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          @click="handleDelete"
          :disabled="!selectProjectData.id ? true : false"
          >删除</el-button
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
      <el-table-column label="" width="30" align="center">
        <template slot-scope="scope">
          <el-radio v-model="selectedId" :label="scope.row.id" @change="handleSelectionChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="项目名称" prop="minAccountFullName" align="center" />
      <el-table-column label="最小核算项目编号" prop="minAccountCode" align="center" />
      <el-table-column label="归属项目部" prop="projectDepartment" align="center" />
      <el-table-column label="项目业态" prop="prjState" align="center">
        <template slot-scope="scope">
          {{ getDictLabel('project_format', scope.row.prjState) }}
        </template>
      </el-table-column>
      <el-table-column label="工程类型" prop="prgTypeText" align="center" />
      <el-table-column label="项目资金来源" prop="moneySec" align="center">
        <template slot-scope="scope">
          {{ getDictLabel('project_funds_source', scope.row.prjState) }}
        </template>
      </el-table-column>
      <el-table-column label="项目管理模式" prop="prjManageModel" align="center" >
        <template slot-scope="scope">
          {{ getDictLabel('project_manage_model', scope.row.prjState) }}
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

    <!-- 新增项目弹窗 -->
    <el-dialog title="新增项目信息" :visible.sync="dialogVisible" width="80%" @before-close="handleDialogClose">
      <el-form :model="form"  ref="form" :rules="rules" label-width="150px" @submit.native.prevent>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最小核算项目编码" prop="minAccountCode">
              <el-input type="text" clearable :readonly="true" disabled v-model="form.minAccountCode" placeholder="系统自动为您生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最小核算项目全称" prop="minAccountFullName">
              <el-input v-model="form.minAccountFullName"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="立项时间" prop="lxDate">
              <el-date-picker v-model="form.lxDate" type="date" style="width:100%" placeholder="选择日期"
              format="yyyy年MM月dd日" value-format="yyyy-MM-dd" ></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目业态" prop="prjState">
              <el-select v-model="form.prjState" placeholder="请选择项目业态" style="width: 100%">
                <el-option v-for="dict in dict.type.project_format" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目管理模式" prop="prjManageModel">
              <el-select v-model="form.prjManageModel" placeholder="请选择项目管理模式" style="width: 100%">
                <el-option v-for="dict in dict.type.project_manage_model" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资质" prop="ziZhiselect">
              <el-cascader
              v-model="form.ziZhiselect"
              :options="zizhiOptions"
              ref="ziZhiselect"
              :props="cascaderProps"
              placeholder="请选择资质类型"
              style="width: 100%"
              clearable
              @change="handleZiZhiChange"
              ></el-cascader>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="建设单位" prop="construtionUnit">
              <el-input v-model="form.construtionUnit"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工程类型" prop="prgType">
              <el-cascader v-model="form.prgType" :options="dictProjectOptions" :props="{ checkStrictly: true, expandTrigger: 'hover', multiple: true, emitPath: false }" placeholder="请选择工程类型" style="width: 100%" clearable></el-cascader>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目负责人" prop="projectLeader">
              <el-input v-model="form.projectLeader"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技术负责人" prop="technicalDirector">
              <el-input v-model="form.technicalDirector"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目负责人电话" prop="projectLeaderPhone">
              <el-input v-model="form.projectLeaderPhone"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技术负责人电话" prop="technicalDirectorPhone">
              <el-input v-model="form.technicalDirectorPhone"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="承包模式" prop="contractingModel">
              <el-select v-model="form.contractingModel" placeholder="请选择承包模式" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_contracting_mode" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资质所属单位" prop="qualifiedUnit">
              <treeselect v-model="form.qualifiedUnit" :options="deptOptions" :show-count="true" placeholder="请选择资质所属单位"></treeselect>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="承揽方式(招标方式)" prop="zbType">
              <el-select v-model="form.zbType" placeholder="请选择承揽方式" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_contracting_method" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属管理组织" prop="managementOrgId">
              <treeselect v-model="form.managementOrgId" :options="deptOptions" :show-count="true" placeholder="请选择归属管理组织"></treeselect>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="归属项目部" prop="projectDepartmentId">
              <treeselect v-model="form.projectDepartmentId" :options="deptOptions" :show-count="true" placeholder="请选择归属项目部"></treeselect>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目资金来源" prop="moneySec">
              <el-select v-model="form.moneySec" placeholder="请选择项目资金来源" style="width: 100%">
                <el-option v-for="dict in dict.type.project_funds_source" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="业务分类" prop="busiType">
              <el-cascader v-model="form.busiType" :options="businessTypeOptions"
              :props="{ checkStrictly: false, expandTrigger: 'hover',emitPath: false  }"
              placeholder="请选择业务分类" style="width: 100%" clearable></el-cascader>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="责任单位" prop="dutyUnit">
              <treeselect v-model="form.dutyUnit" :options="deptOptions" :show-count="true" placeholder="请选择责任单位"></treeselect>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="其中暂列金额(含税)(万元)" prop="provisionalSum">
              <el-input v-model="form.provisionalSum"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标前成本测算利润率(%)" prop="bqcbRate">
              <el-input v-model="form.bqcbRate"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="中标时间" prop="winningTime">
              <el-date-picker v-model="form.winningTime" type="date"
               format="yyyy年MM月dd日" value-format="yyyy-MM-dd"></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目状态" prop="state">
              <el-select v-model="form.state" placeholder="请选择项目状态" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_project_status" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="全费用下浮率(%)" prop="costReductionRate">
              <el-input v-model="form.costReductionRate"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="中标价(含税)(万元)" prop="biddingPrice">
              <el-input v-model="form.biddingPrice"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="招标控制价或下浮率(%)" prop="control">
              <el-input v-model="form.control"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </span>
    </el-dialog>
  </div>



</template>

<script>
import {AlldeptTreeSelect} from "@/api/system/project";
import {deptTreeSelect} from "@/api/system/user";
import {listProject,saveMinProjectInfo,getMinProjectById,deleteProject,BusinessTypeTreeSelect,CertificationTypeTreeSelect,dictProjectTypeTreeSelect} from "@/api/system/project";
import { getDicts as getDicts } from '@/api/system/dict/data'
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
export default {
  dicts: ['project_format', 'project_funds_source','project_manage_model','sys_contracting_mode','sys_contracting_method','sys_project_status'],
  components: { Treeselect },
  data() {
    return {
      //  遮罩层
      loading: true,
      // 查询参数
      queryParams: {
        minAccountFullName: "",
        deptId: null,
        minAccountCode: "",
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
      //新增项目表单
      form: {
        minAccountCode: '',
        minAccountFullName: '',
        lxDate: null,
        prjState: '',
        prjManageModel: '',
        ziZhi: null,
        ziZhiselect: null,
        construtionUnit: '',
        prgType: null,
        projectLeader: '',
        technicalDirector: '',
        projectLeaderPhone: '',
        technicalDirectorPhone: '',
        contractingModel: '',
        qualifiedUnit: null,
        zbType: '',
        projectDepartment: null,
        moneySec: '',
        busiType: null,
        dutyUnit: null,
        provisionalSum: '',
        bqcbRate: '',
        winningTime: null,
        state: '',
        costReductionRate: '',
        biddingPrice: '',
        control: ''
      },
      //表单校验
      rules: {
        minAccountFullName: [
          { required: true, message: '项目名称不能为空', trigger: 'blur' }
        ],
        prjState: [
          { required: true, message: '项目业态不能为空', trigger: 'change' }
        ],
        lxDate: [
          { required: true, message: '立项时间不能为空', trigger: 'change' }
        ],
        prjManageModel: [
          { required: true, message: '项目管理模式不能为空', trigger: 'change' }
        ],
        ziZhiselect: [
          { required: true, message: '资质不能为空', trigger: 'change' }
        ],
        construtionUnit: [
          { required: true, message: '建设单位不能为空', trigger: 'blur' }
        ],
        prgType: [
          { required: true, message: '工程类型不能为空', trigger: 'change' }
        ],
        projectLeader: [
          { required: true, message: '项目负责人不能为空', trigger: 'blur' }
        ],
        technicalDirector: [
          { required: true, message: '技术负责人不能为空', trigger: 'blur' }
        ],
        projectLeaderPhone: [
          { required: true, message: '项目负责人电话不能为空', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
        ],
        technicalDirectorPhone: [
          { required: true, message: '技术负责人电话不能为空', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
        ],
        contractingModel: [
          { required: true, message: '承包模式不能为空', trigger: 'change' }
        ],
        qualifiedUnit: [
          { required: true, message: '资质所属单位不能为空', trigger: 'change' }
        ],
        zbType: [
          { required: true, message: '承揽方式(招标方式)不能为空', trigger: 'change' }
        ],
        projectDepartmentId: [
          { required: true, message: '归属项目部不能为空', trigger: 'change' }
        ],
        managementOrgId: [
          { required: true, message: '归属管理组织不能为空', trigger: 'change' }
        ],
        moneySec: [
          { required: true, message: '项目资金来源不能为空', trigger: 'change' }
        ],
        busiType: [
          { required: true, message: '业务分类不能为空', trigger: 'change' }
        ],
        dutyUnit: [
          { required: true, message: '责任单位不能为空', trigger: 'change' }
        ],
        provisionalSum: [
          { required: true, message: '其中暂列金额(含税)(万元)不能为空', trigger: 'blur' }
        ],
        bqcbRate: [
          { required: true, message: '标前成本测算利润率(%)不能为空', trigger: 'blur' }
        ],
        winningTime: [
          { required: true, message: '中标时间不能为空', trigger: 'change' }
        ],
        state: [
          { required: true, message: '项目状态不能为空', trigger: 'change' }
        ],
        costReductionRate: [
          { required: true, message: '全费用下浮率(%)不能为空', trigger: 'blur' }
        ],
        biddingPrice: [
          { required: true, message: '中标价(含税)(万元)不能为空', trigger: 'blur' }
        ],
        control: [
          { required: true, message: '招标控制价或下浮率(%)不能为空', trigger: 'blur' }
        ]
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


    };
  },
  created() {
    this.getDeptTree();
    this.loadDictData();
    this.getBusinessTypeTree();
    this.getDictProjectTypeTree();
    this.getZizhiTypeTree();
  },
  methods: {
    // 保存项目信息
    handleSave: function() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          // 将 prgType 从数组转换为逗号分隔的字符串
          this.form.prgType = this.form.prgType ? this.form.prgType.join(',') : '';
          console.log("保存项目信息：", this.form);
          saveMinProjectInfo(this.form).then(response => {
            this.$message.success("项目保存成功");
            this.dialogVisible = false;
            this.resetForm();
            this.getProjectList();
          }).catch(error => {
            this.$message.error("项目保存失败：" + error.message);
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
        minAccountCode: '',
        minAccountFullName: '',
        lxDate: null,
        prjState: '',
        prjManageModel: '',
        ziZhi: null,
        ziZhiselect:null,
        construtionUnit: '',
        prgType: null,
        projectLeader: '',
        technicalDirector: '',
        projectLeaderPhone: '',
        technicalDirectorPhone: '',
        contractingModel: '',
        qualifiedUnit: null,
        zbType: '',
        projectDepartmentId: null,
        moneySec: '',
        busiType: null,
        dutyUnit: null,
        provisionalSum: '',
        bqcbRate: '',
        winningTime: null,
        state: '',
        costReductionRate: '',
        biddingPrice: '',
        control: ''
      };
    },

    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then((response) => {
        this.deptOptions = this.mapDeptTreeData(response.data);
        console.log("deptOptions:",this.deptOptions);
      });
    },

    // 递归映射树结构数据（资质树、业务分类、工程分类）
    mapTreeData(data) {
      return data.map(item => {
        return {
          id: item.id,
          label: item.name,
          value: item.code,
          children: item.children ? this.mapTreeData(item.children) : null
        };
      });
    },
    // 递归映射树结构数据（资质树、业务分类、工程分类）
    mapDeptTreeData(data) {
      return data.map(item => {
        return {
          id: item.thridDeptId ,
          label: item.label,
          value: item.thridDeptId,
          children: item.children ? this.mapDeptTreeData(item.children) : null
        };
      });
    },
    /** 查询业务分类下拉树结构 */
    getBusinessTypeTree() {
      BusinessTypeTreeSelect().then((response) => {
        this.businessTypeOptions = this.mapTreeData(response.data);
        console.log("businessTypeOptions:", this.businessTypeOptions);
      });
    },
    /** 查询资质分类下拉树结构 */
    getZizhiTypeTree() {
      CertificationTypeTreeSelect().then((response) => {
        this.zizhiOptions = this.mapTreeData(response.data);
        console.log("zizhiOptions:",this.zizhiOptions);
      });
    },
    /** 查询工程分类下拉树结构 */
    getDictProjectTypeTree() {
      dictProjectTypeTreeSelect().then((response) => {
        this.dictProjectOptions = this.mapTreeData(response.data);
        console.log("dictProjectOptions:",this.dictProjectOptions);
      });
    },
    handleChange(value, selectedData) {
      console.log('Selected Value:', value); // 当前选中的值数组
      console.log('Selected Data:', selectedData); // 当前选中项的详细数据对象数组
      this.buildHierarchy(selectedData); // 构建层级关系
    },
    // 处理资质选择变化
    handleZiZhiChange(value,selectedData) {
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
      listProject(this.queryParams).then(
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
      this.getProjectList();
    },
    // 新增按钮操作
    handleAdd() {
      console.log("新增项目");
      this.dialogVisible = true;
      this.resetForm(); // 重置表单
    },
    // 新增项目弹窗的关闭事件
    handleDialogClose() {
      this.resetForm(); // 关闭弹窗时清空表单
    },
    // 修改按钮操作
    async handleUpdate(row) {
      console.log("修改项目", row);
      const { id, name } = this.selectProjectData;
      try {
        const res = await getMinProjectById(id);
        this.form = res.data;
        this.form.prgType = this.form.prgType.split(",");
        let ziZhiStr = this.form.ziZhi.split(",");
        this.form.ziZhiselect = ziZhiStr;
        console.log("修改项目this.form", this.form);
        this.dialogVisible = true;
      } catch (err) {
        console.log(err);
      }
    },
    // 删除按钮操作
    handleDelete() {
      console.log("删除项目", this.selectProjectData);
      const { id, name } = this.selectProjectData;
      this.$modal
        .confirm('是否确认删除项目名为"' + name + '"的数据项？')
        .then(async () => {
          try {
            const res = await deleteProject(id);
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
  },
};
</script>

<style scoped>
.el-form-item__label {
  float: left;
  line-height: 32px; /* 根据你的输入框高度调整 */
}
</style>
