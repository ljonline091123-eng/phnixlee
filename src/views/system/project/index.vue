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
      <el-table-column label="工程类型" prop="prgType" align="center" />
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
      :current-page="queryParams.pageNum"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="queryParams.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
    />

    <!-- 新增项目弹窗 -->
    <el-dialog title="新增项目信息" :visible.sync="dialogVisible" width="50%">
      <el-form :model="form" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最小核算项目编码">
              <el-input v-model="form.minAccountCode"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最小核算项目全称">
              <el-input v-model="form.minAccountFullName"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="立项时间">
              <el-date-picker v-model="form.lxDate" type="date"></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目业态">
              <el-select v-model="form.prjState">
                <el-option label="房建" value="房建"></el-option>
                <!-- 其他选项 -->
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目管理模式">
              <el-select v-model="form.prjManageModel">
                <el-option label="自营" value="自营"></el-option>
                <!-- 其他选项 -->
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资质">
              <el-select v-model="form.ziZhi" multiple>
                <el-option label="工民建板块" value="工民建板块"></el-option>
                <el-option label="建筑类(总承包)" value="建筑类(总承包)"></el-option>
                <el-option label="建筑工程施工总承包" value="建筑工程施工总承包"></el-option>
                <!-- 其他选项 -->
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="建设单位">
              <el-input v-model="form.construtionUnit"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目负责人电话">
              <el-input v-model="form.projectLeaderPhone"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="技术负责人电话">
              <el-input v-model="form.technicalDirectorPhone"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工程类型">
              <el-select v-model="form.prgType" multiple>
                <el-option label="教学楼" value="教学楼"></el-option>
                <el-option label="实验楼" value="实验楼"></el-option>
                <el-option label="学校礼堂" value="学校礼堂"></el-option>
                <el-option label="学生食堂" value="学生食堂"></el-option>
                <el-option label="其他" value="其他"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="承包模式">
              <el-select v-model="form.contractingModel">
                <el-option label="设计-采购-施工工程总承包模式 (EPC)" value="EPC"></el-option>
                <!-- 其他选项 -->
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资质所属单位">
              <el-tree-select v-model="form.qualifiedUnit" :data="qualificationUnits"></el-tree-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="承揽方式(招标方式)">
              <el-input v-model="form.zbType"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="归属项目部">
              <el-tree-select v-model="form.projectDepartment" :data="projectDepartments"></el-tree-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目资金来源">
              <el-select v-model="form.moneySec">
                <el-option label="国有-政府投资" value="国有-政府投资"></el-option>
                <!-- 其他选项 -->
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="业务分类">
              <el-select v-model="form.busiType" multiple>
                <el-option label="建安类/工业民用建筑/房建工程项目/建筑防水工程" value="建安类/工业民用建筑/房建工程项目/建筑防水工程"></el-option>
                <!-- 其他选项 -->
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="责任单位">
              <el-tree-select v-model="form.dutyUnit" :data="responsibleUnits"></el-tree-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目负责人">
              <el-input v-model="form.projectLeader"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技术负责人">
              <el-input v-model="form.technicalDirector"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="其中暂列金额(含税)(万元)">
              <el-input v-model="form.provisionalSum"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标前成本测算利润率(%)">
              <el-input v-model="form.bqcbRate"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="中标日期">
              <el-date-picker v-model="form.bidSubmissionDate" type="date"></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目状态">
              <el-input v-model="form.projectStatus"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="全费用下浮率(%)">
              <el-input v-model="form.totalCostReductionRate"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="中标价(含税)(万元)">
              <el-input v-model="form.bidAmount"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="招标控制价或下浮率(%)">
              <el-input v-model="form.bidControlRate"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同金额(万元)">
              <el-input v-model="form.contractAmount"></el-input>
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
import {deptTreeSelect} from "@/api/system/user";
import {listProject} from "@/api/system/project";
import { getDicts as getDicts } from '@/api/system/dict/data'
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
export default {
  dicts: ['project_format', 'project_funds_source','project_manage_model'],
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
        pageNum: 1,
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
      //新增弹窗显示
      dialogVisible: false,

      //新增项目表单
      form: {
        projectCode: '',
        fullProjectName: '',
        abbreviatedProjectName: '',
        projectType: '',
        projectManagementModel: '',
        qualifications: [],
        constructionUnit: '',
        projectManagerPhone: '',
        technicalManagerPhone: '',
        constructionType: [],
        contractingModel: '',
        qualificationUnit: '',
        designUnit: '',
        biddingMethod: '',
        projectDepartment: '',
        fundingSource: '',
        businessCategory: [],
        responsibleUnit: '',
        projectManager: '',
        technicalManager: '',
        contingencyAmount: '',
        preliminaryProfitMargin: '',
        bidSubmissionDate: '',
        projectStatus: '',
        totalCostReductionRate: '',
        bidAmount: '',
        creator: '',
        projectStartDate: '',
        bidControlRate: '',
        contractAmount: '',
        creationTime: ''
      },
      qualificationUnits: [
        {
          label: '湖南建工集团有限公司(总承包)',
          value: '湖南建工集团有限公司(总承包)'
        }
        // 其他选项
      ],
      projectDepartments: [
        {
          label: '湖南建工集团有限公司建投向江徕建设项目工程总承包项目经理部',
          value: '湖南建工集团有限公司建投向江徕建设项目工程总承包项目经理部'
        }
        // 其他选项
      ],
      responsibleUnits: [
        {
          label: '直属三公司',
          value: '直属三公司'
        }
        // 其他选项
      ]

    };
  },
  created() {
    this.getDeptTree();
    this.loadDictData();
  },
  methods: {
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then((response) => {
        this.deptOptions = response.data;
        console.log("deptOptions:",this.deptOptions);
      });
      
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
        this.loadingDicts = false;
        this.dictLoaded = true;
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
    // handleSelectionChange(row) {
    //   this.selectProjectData= { id: row.id, name: row.minAccountFullName };
    //   this.selectedId = row.id; // 更新单选框的选中状态
    //   console.log(this.selectProjectData, "change选中了selectProjectData");
    // },

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
      this.queryParams.pageNum = 1;
      this.getProjectList();
    },
    // 新增按钮操作
    handleAdd() {
      console.log("新增项目");
      this.dialogVisible = true;
    },
    // 修改按钮操作
    handleUpdate(row) {
      console.log("修改项目", row);
    },
    // 删除按钮操作
    handleDelete(row) {
      console.log("删除项目", row);
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
      this.queryParams.pageNum = val;
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