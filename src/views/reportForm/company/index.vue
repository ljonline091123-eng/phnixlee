<template>
  <div class="app-container">
    <el-dialog
      title="高级筛选"
      :visible.sync="advancedDialogVisible"
      width="70%"
    >
      <el-tabs v-model="activeName">
        <el-tab-pane label="按项目筛选" name="first">
          <el-form ref="form" :model="advancedForm" label-width="160px">
            <el-form-item label="项目编码">
              <el-input
                v-model="advancedForm.projectCode"
                placeholder="请输入项目编码"
                style="width: 360px"
              ></el-input>
            </el-form-item>
            <el-form-item label="项目简称">
              <el-input
                v-model="advancedForm.projectName"
                placeholder="请输入项目简称"
                style="width: 360px"
              ></el-input>
            </el-form-item>
            <el-form-item label="合同金额（万元）">
              <el-input
                v-model="advancedForm.contractStartAmount"
                style="width: 220px"
              ></el-input>
              至
              <el-input
                v-model="advancedForm.contractEndAmount"
                style="width: 220px"
              ></el-input>
            </el-form-item>
            <el-form-item label="建设单位">
              <el-input
                v-model="advancedForm.buildUnit"
                placeholder="请输入建设单位"
                style="width: 360px"
              ></el-input>
            </el-form-item>
            <el-form-item label="项目规模">
              <el-input
                v-model="advancedForm.projectScale"
                style="width: 220px"
              ></el-input>
              至
              <el-input
                v-model="advancedForm.projectScale1"
                style="width: 220px"
              ></el-input>
            </el-form-item>
            <el-form-item label="工程类型">
              <el-select
                v-model="advancedForm.engineerTypeName"
                style="margin-right: 8px"
              >
                <el-option
                  v-for="item in [
                    {
                      label: '公路',
                      value: '1',
                    },
                    {
                      label: '房建',
                      value: '2',
                    },
                  ]"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
              <el-select
                v-model="advancedForm.engineerTypeName2"
                style="margin-right: 8px"
              >
                <el-option
                  v-for="item in [
                    {
                      label: '桥梁',
                      value: '1',
                    },
                    {
                      label: '地基',
                      value: '2',
                    },
                  ]"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
              <el-select v-model="advancedForm.engineerTypeName3">
                <el-option
                  v-for="item in [
                    {
                      label: '桩基',
                      value: '1',
                    },
                    {
                      label: '墩柱',
                      value: '2',
                    },
                  ]"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="结构类型">
              <el-select
                v-model="advancedForm.structureTypeName"
                style="width: 360px"
              >
                <el-option
                  v-for="item in [
                    { label: '梁', value: '1' },
                    { label: '板', value: '2' },
                    { label: '柱', value: '3' },
                    { label: '桁架', value: '4' },
                    { label: '拱', value: '5' },
                    { label: '排架', value: '6' },
                    { label: '框架', value: '7' },
                    { label: '折板结构', value: '8' },
                    { label: '壳体结构', value: '9' },
                    { label: '网架结构', value: '10' },
                    { label: '悬索结构', value: '11' },
                    { label: '剪力墙', value: '12' },
                    { label: '筒体结构', value: '13' },
                    { label: '悬吊结构', value: '14' },
                    { label: '板柱结构', value: '15' },
                    { label: '墙板结构', value: '16' },
                    { label: '充气结构', value: '17' },
                  ]"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="实际开工时间">
              <el-date-picker
                v-model="advancedForm['time1']"
                type="daterange"
                style="width: 360px"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              >
              </el-date-picker>
            </el-form-item>
            <el-form-item label="实际竣工时间">
              <el-date-picker
                v-model="advancedForm['time2']"
                type="daterange"
                style="width: 360px"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              >
              </el-date-picker>
            </el-form-item>
            <el-form-item label="项目所属行政区域">
              <el-select
                v-model="advancedForm.province"
                style="margin-right: 8px"
                @change="changeSelect($event, '1')"
              >
                <el-option
                  v-for="item in [
                    {
                      value: '1',
                      label: '湖南省',
                    },
                    {
                      value: '2',
                      label: '广东省',
                    },
                  ]"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
              <el-select
                v-model="advancedForm.city"
                style="margin-right: 8px"
                @change="changeSelect($event, '2')"
              >
                <el-option
                  v-for="item in cityOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
              <el-select v-model="advancedForm.area">
                <el-option
                  v-for="item in areaOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button
          @click="advancedDialogVisible = false"
          style="width: 100px"
          size="small"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="advancedDialogVisible = false"
          style="width: 100px"
          size="small"
          >确 定</el-button
        >
      </span>
    </el-dialog>
    <div class="split-page-box flex full">
      <Drag>
        <template v-slot:left-content>
          <div class="left">
            <el-input
              v-model="deptName"
              placeholder="请输入部门名称"
              clearable
              size="small"
              prefix-icon="el-icon-search"
              style="margin-bottom: 12px"
            />
            <!-- 动态生成圆形按钮 -->
            <div class="circle-buttons" style="margin-bottom: 12px">
              <button
                v-for="n in buttonCount"
                :key="n"
                class="circle-btn"
                @click="expandNodes(n)"
              >
                {{ n }}
              </button>
            </div>
            <el-tree
              v-loading="deptTreeLoading"
              :data="deptOptions"
              class="tree_expert"
              :props="defaultProps"
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              ref="tree"
              node-key="deptId"
              default-expand-all
              highlight-current
              @node-click="handleNodeClick"
            />
          </div>
        </template>
        <template v-slot:right-content>
          <div class="right fill">
            <el-form
              :model="queryParams"
              ref="queryForm"
              size="small"
              :inline="true"
              label-width="68px"
            >
              <el-form-item label="项目业态" prop="projectBusinessName">
                <el-select
                  v-model="queryParams.projectBusinessName"
                  placeholder="请选择项目业态"
                  @keyup.enter.native="handleQuery"
                >
                  <el-option
                    v-for="item in [
                      {
                        label: '房建',
                        value: '1',
                      },
                      {
                        label: '公路',
                        value: '2',
                      },
                      {
                        label: '市政',
                        value: '3',
                      },
                      {
                        label: '水利',
                        value: '4',
                      },
                    ]"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  >
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="项目状态" prop="projectStatusName">
                <el-select
                  v-model="queryParams.projectStatusName"
                  placeholder="请选择项目状态"
                  @keyup.enter.native="handleQuery"
                >
                  <el-option
                    v-for="item in [
                      {
                        label: '停工',
                        value: '1',
                      },
                      {
                        label: '开工',
                        value: '2',
                      },
                      {
                        label: '在建',
                        value: '3',
                      },
                      {
                        label: '续建',
                        value: '4',
                      },
                    ]"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  >
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  size="small"
                  @click="handleQuery"
                  >查询</el-button
                >
                <el-button
                  type="primary"
                  size="small"
                  @click="advancedDialogVisible = true"
                >
                  高级筛选
                </el-button>
                <el-button
                  type="primary"
                  plain
                  icon="el-icon-download"
                  size="small"
                  @click="handleExport"
                  >导出
                </el-button>
              </el-form-item>
            </el-form>
            <el-table
              style="overflow: auto"
              v-loading="loading"
              :data="tableData"
              highlight-current-row
              class="flex1"
              border
              ref="myTable"
              :header-cell-style="{
                background: '#F3F2F8',
                textAlign: 'center',
                padding: '8px 0'
              }"
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column align="center" label="项目信息">
                <el-table-column
                  label="项目编号"
                  key="projectCode"
                  prop="projectCode"
                  width="150"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="项目简称"
                  key="projectName"
                  prop="projectName"
                  width="340"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="合同金额（万元）"
                  align="right"
                  key="contractAmount"
                  prop="contractAmount"
                  width="180"
                >
                  <template #header="scope">
                    <div class="flex">
                      <span>合同金额（万元）</span>
                      <span
                        v-if="foldFlag"
                        class="flex align-center cursor_point"
                        @click.stop="clickFoldButton(false)"
                      >
                        <span class="toggle-box flex">收起</span>
                        <i
                          class="el-icon-caret-left"
                          style="font-size: 14px"
                        ></i>
                      </span>
                      <span
                        v-else
                        class="flex align-center cursor_point"
                        @click.stop="clickFoldButton(true)"
                      >
                        <span class="toggle-box flex">展开</span>
                        <i
                          class="el-icon-caret-right"
                          style="font-size: 14px"
                        ></i>
                      </span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="foldFlag"
                  label="建设单位"
                  :formatter="formatter"
                  key="buildUnit"
                  prop="buildUnit"
                  width="300"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="责任单位"
                  :formatter="formatter"
                  key="responsibleUnit"
                  prop="responsibleUnit"
                  width="300"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="项目规模"
                  :formatter="formatter"
                  key="projectScale"
                  prop="projectScale"
                  width="120"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="项目业态"
                  :formatter="formatter"
                  key="projectBusinessName"
                  prop="projectBusinessName"
                  width="120"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="工程类型"
                  :formatter="formatter"
                  key="engineerTypeName"
                  prop="engineerTypeName"
                  width="120"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="结构类型"
                  :formatter="formatter"
                  key="structureTypeName"
                  prop="structureTypeName"
                  width="120"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="项目状态"
                  :formatter="formatter"
                  key="projectStatusName"
                  prop="projectStatusName"
                  width="120"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="实际开工日期"
                  :formatter="formatter"
                  key="realityBeginDate"
                  prop="realityBeginDate"
                  width="120"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="实际竣工日期"
                  :formatter="formatter"
                  key="realityFinishDate"
                  prop="realityFinishDate"
                  width="120"
                  show-overflow-tooltip
                />
                <el-table-column
                  v-if="foldFlag"
                  label="项目地点"
                  :formatter="formatter"
                  key="address"
                  prop="address"
                />
              </el-table-column>
              <el-table-column align="center" label="招标采购">
                <el-table-column
                  label="采购计划数"
                  :formatter="formatter"
                  align="right"
                  key="planCount"
                  prop="planCount"
                  width="100"
                >
                  <template slot-scope="scope">
                    <a class="link-type" @click="goDetail(scope.row, 'plan')">
                      {{ scope.row.planCount }}
                    </a>
                  </template>
                </el-table-column>
                <el-table-column
                  label="已完成招标"
                  :formatter="formatter"
                  align="right"
                  key="bidCount"
                  prop="bidCount"
                  width="120"
                >
                  <template slot-scope="scope">
                    <a class="link-type" @click="goDetail(scope.row, 'bid')">
                      {{ scope.row.bidCount }}
                    </a>
                  </template>
                </el-table-column>
                <el-table-column
                  label="已签合同数"
                  :formatter="formatter"
                  align="right"
                  key="signCount"
                  width="120"
                  prop="signCount"
                >
                  <template slot-scope="scope">
                    <a
                      class="link-type"
                      @click="goDetail(scope.row, 'contract')"
                    >
                      {{ scope.row.signCount }}
                    </a>
                  </template>
                </el-table-column>
                <el-table-column
                  label="已签合同金额（元）"
                  :formatter="formatter"
                  align="right"
                  key="signContractAmount"
                  prop="signContractAmount"
                  min-width="160"
                />
              </el-table-column>
              <el-table-column
                label="操作"
                align="center"
                class-name="small-padding fixed-width"
              >
                <template slot-scope="scope">
                  <el-button
                    size="mini"
                    type="text"
                    @click="goDetail(scope.row, 'project')"
                    >进入项目</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
            <pagination
              v-show="total > 0"
              :total="total"
              :page.sync="queryParams.pageNum"
              :limit.sync="queryParams.pageSize"
              @pagination="getList"
            />
          </div>
        </template>
      </Drag>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { deptTreeSelect } from "@/api/system/user";
import { deptTree, managePageReport } from "@/api/reportForm/managePageReport";
import Drag from "@/components/Drag/index.vue";

export default {
  name: "Company",
  data() {
    return {
      areaOptions: [],
      cityOptions: [],
      activeName: "first",
      advancedForm: {},
      advancedDialogVisible: false,
      foldFlag: false,
      tableData: [],
      options: [],
      // 遮罩层
      loading: false,
      deptTreeLoading: false,
      // 总条数
      total: 0,
      // 部门树选项
      deptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 部门名称
      deptName: undefined,
      // 日期范围
      dateRange: [],
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "deptName",
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
      buttonCount: 1,
    };
  },
  components: {
    Drag,
  },
  computed: {
    ...mapGetters(["project"]),
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    },
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.queryParams.projectCode = newVal.code;
          this.getDeptTree();
        }
      },
      immediate: true,
    },
  },
  methods: {
    /**
     * 跳转页面
     * @param row
     * @param type
     */
    goDetail(row, type) {
      if (type === "project") {
        this.$router.push({
          path: "/procurement/plan",
        });
        return;
      }
      let path = "";
      let query = {
        projectCode: row.projectCode,
      };
      if (type === "plan") {
        path = "/procurement/plan";
      } else if (type === "bid") {
        path = "/procurement/bindding";
        query.noticeStatus = "8";
      } else {
        path = "/procurement/sign-contract";
        query.agreementState = "3";
      }
      this.$router.push({
        path,
        query,
      });
    },
    changeSelect(e, type) {
      if (type === "1") {
        this.advancedForm.city = "";
        this.advancedForm.area = "";
        if (e === "1") {
          this.cityOptions = [
            { label: "长沙市", value: "1" },
            { label: "株洲市", value: "2" },
            { label: "湘潭市", value: "3" },
            { label: "衡阳市", value: "4" },
            { label: "邵阳市", value: "5" },
            { label: "岳阳市", value: "6" },
            { label: "常德市", value: "7" },
            { label: "张家界市", value: "8" },
            { label: "益阳市", value: "9" },
            { label: "郴州市", value: "10" },
            { label: "永州市", value: "11" },
            { label: "怀化市", value: "12" },
            { label: "娄底市", value: "13" },
            { label: "湘西土家族苗族自治州", value: "14" },
          ];
        } else {
          this.cityOptions = [
            { label: "广州市", value: "15" },
            { label: "韶关市", value: "16" },
            { label: "深圳市", value: "17" },
            { label: "珠海市", value: "18" },
            { label: "汕头市", value: "19" },
            { label: "佛山市", value: "20" },
            { label: "江门市", value: "21" },
            { label: "湛江市", value: "22" },
            { label: "茂名市", value: "23" },
            { label: "肇庆市", value: "24" },
            { label: "惠州市", value: "25" },
            { label: "梅州市", value: "26" },
            { label: "汕尾市", value: "27" },
            { label: "河源市", value: "28" },
            { label: "阳江市", value: "29" },
            { label: "清远市", value: "30" },
            { label: "东莞市", value: "31" },
            { label: "中山市", value: "32" },
          ];
        }
      } else if (type === "2") {
        // * 静态数据
        this.areaOptions = [
          { label: "岳麓区", value: "1" },
          { label: "芙蓉区", value: "2" },
          { label: "天心区", value: "3" },
          { label: "开福区", value: "4" },
          { label: "雨花区", value: "5" },
          { label: "望城区", value: "6" },
          { label: "长沙县", value: "7" },
          { label: "浏阳市", value: "8" },
          { label: "宁乡市", value: "9" },
        ];
      }
    },
    formatter(row, column) {
      return row[column.property] || "/";
    },
    handleExport() {
      this.download(
        "business/report/managePageReportExport",
        {
          ...this.queryParams,
        },
        `report_${new Date().getTime()}.xlsx`
      );
    },
    clickFoldButton(flag) {
      this.foldFlag = flag;
      this.$nextTick(() => {
        this.$refs.myTable.doLayout();
      });
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      this.deptTreeLoading = true;
      deptTree({
        deptId: this.$store.state.user?.userInfo?.dept?.deptId,
        thridOrgId: this.$store.state.user?.userInfo?.thridOrgId,
      })
        .then((response) => {
          this.deptOptions = this.handleTree(response.data, "deptId");

          // * 默认取第一个，查询表格
          this.queryParams.deptId =
            this.$store.state.user?.userInfo?.dept?.deptId;
          this.deptTreeLoading = false;
          this.getList();
        })
        .then(() => {
          this.initButtons();
        })
        .catch(() => {
          this.deptTreeLoading = false;
        });
    },
    // 递归计算最大层级
    calculateMaxLevel(nodes, level = 0) {
      let maxLevel = level;

      nodes.forEach((node) => {
        if (node.children && node.children.length > 0) {
          maxLevel = Math.max(
            maxLevel,
            this.calculateMaxLevel(node.children, level + 1)
          );
        }
      });

      return maxLevel;
    },
    // 初始化按钮
    initButtons() {
      const maxLevel = this.calculateMaxLevel(this.deptOptions);
      this.buttonCount = maxLevel + 1;
    },

    // 展开到指定层级，并收起超出层级的节点
    expandNodes(level) {
      const tree = this.$refs.tree;
      const allNodes = Object.values(tree.store.nodesMap);

      allNodes.forEach((node) => {
        const nodeLevel = this.getNodeLevel(node); // 计算节点层级
        if (level === 1) {
          // 关闭所有父节点
          if (node.data.parentId === "0") {
            tree.store.getNode(node.key).expanded = false;
          }
        } else if (level === 2) {
          // 展开所有父节点，关闭子节点
          if (node.data.parentId === "0") {
            tree.store.getNode(node.key).expanded = true; // 展开父节点
          } else {
            tree.store.getNode(node.key).expanded = false; // 收起子节点
          }
        } else if (level === 3) {
          // 展开所有父节点和子节点，关闭孙节点
          if (node.data.parentId === "0" || this.isChildOfParent(node)) {
            tree.store.getNode(node.key).expanded = true; // 展开父节点和子节点
          } else {
            tree.store.getNode(node.key).expanded = false; // 收起孙节点
          }
        } else if (level === 4) {
          // 展开所有父节点、子节点和孙节点，关闭更深层次的节点
          if (this.isDescendantOfParent(node, 3)) {
            tree.store.getNode(node.key).expanded = true; // 展开父节点、子节点和孙节点
          } else {
            tree.store.getNode(node.key).expanded = false; // 收起更深层次的节点
          }
        } else if (level === 5) {
          // 展开所有节点
          tree.store.getNode(node.key).expanded = true;
        }
      });
    },
    // 计算节点层级
    getNodeLevel(node) {
      let level = 0;
      let currentNode = node;

      // 计算层级
      while (currentNode.parent) {
        level++;
        currentNode = currentNode.parent;
      }

      return level;
    },
    // 判断节点是否是某个节点的子节点
    isChildOfParent(node) {
      const parentNode = this.$refs.tree.store.getNode(node.data.parentId);
      return parentNode && parentNode.data.parentId === "0";
    },
    // 判断节点是否是某个节点的某一层的后代
    isDescendantOfParent(node, maxLevel) {
      const nodeLevel = this.getNodeLevel(node);
      return nodeLevel <= maxLevel;
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.deptId;
      this.handleQuery();
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    getList() {
      this.loading = true;
      managePageReport(this.queryParams)
        .then((res) => {
          this.tableData = res.data.list;
          this.total = res.data.total;
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
    },
  },
};
</script>
<style lang="scss" scoped>
//.el-dialog
::v-deep .el-dialog__header {
  border-bottom: solid 1px #dcdfe6;
}

::v-deep .el-dialog__title {
  font-size: 15px;
  font-weight: bolder;
  color: #121735;

  &::before {
    content: "";
    width: 3px;
    height: 15px;
    background: #2b4acb;
    position: absolute;
    left: 8px;
    top: 25px;
    // transform: translateY(-5%);
  }
}

//::v-deep .el-table--border .el-table__cell {
//  border-right: none !important;
//  border: none !important;
//}
//
//::v-deep .el-table--group, .el-table--border {
//  border: none !important;
//}
//
//::v-deep .el-table--border::after {
//  width: 0px;
//}

::v-deep {
  .head-container1 .el-tree {
    height: calc(100vh - 175px);
    overflow: auto;
  }

  .head-container1 .el-tree-node.is-expanded > .el-tree-node__children {
    overflow: visible;
  }
}

::v-deep {
  .head-container .el-tree {
    // min-height: calc(72vh - 115px);
    overflow: auto;
    z-index: 999;
  }

  .head-container .el-tree-node.is-expanded > .el-tree-node__children {
    overflow: visible;
  }
}

::v-deep .el-dialog .el-dialog__body {
  margin: 0 auto !important;
  max-height: 72vh;
  overflow: auto;
}

.toggle-box {
  font-weight: normal;
  font-size: 12px;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  position: relative;
  color: #2b4acb;
}
.el-icon-caret-right:before {
  content: "\e791";
  color: #2b4acb;
}
.el-icon-caret-left:before {
  content: "\e792";
  color: #2b4acb;
}

.cursor_point {
  margin-left: 15px;
}
/* 圆形按钮样式 */
.circle-buttons {
  display: flex;
}

.circle-btn {
  background-color: #e8e8ef;
  color: #999;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  text-align: center;
  line-height: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  margin-right: 10px;
}

/* 鼠标移上去时的效果 */
.circle-btn:hover {
  background-color: #2b4acb;
  color: #666;
}
</style>
