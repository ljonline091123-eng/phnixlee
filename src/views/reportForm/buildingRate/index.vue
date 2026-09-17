<template>
  <div class="app-container">
    <div class="split-page-box flex full">
      <Drag>
        <template v-slot:left-content>
          <div class="left">
            <el-input
              v-model="deptName"
              placeholder="请输入单位名称"
              clearable
              size="small"
              prefix-icon="el-icon-search"
              style="width: 220px; margin: 10px 0 0 10px"
            />
            <div class="circle-buttons" style="margin: 10px 0 0 10px">
              <button
                v-for="n in buttonCount"
                :key="n"
                class="circle-btn"
                :class="levelExpand == n ? 'circle-btn-selected' : 'circle-btn'"
                @click="expandNodes(n)"
              >
                {{ n }}
              </button>
            </div>
            <el-tree
              v-loading="deptTreeLoading"
              :data="deptOptions"
              :default-expanded-keys="treeData"
              class="address-tree"
              :props="defaultProps"
              :filter-node-method="filterNode"
              ref="tree"
              node-key="id"
              highlight-current
              @node-click="handleNodeClick"
            />
          </div>
        </template>
        <template v-slot:right-content>
          <ShowTablePro
            ref="dataList"
            reportType="building"
            :table-header-list="tableHeaderList"
            :table-data="tableData"
            :queryItemList="queryItemList"
            :loading="loading"
            :exportFlag="true"
            :queryFlag="true"
            @query="handleQuery"
            @export="handleExport"
          >
          </ShowTablePro>
        </template>
      </Drag>
    </div>
  </div>
</template>

<script>
import ShowTablePro from "@/views/reportForm/components/ShowTablePro.vue";
import Drag from "@/components/Drag/index.vue";
import { mixinReport } from "@/views/reportForm/mixins/mixinReport";
import {
  bidCountReport,
  getProjectCode,
} from "@/api/reportForm/tenderingRateReport";
import { getDeptTree } from "@/api/system/dept";
import { formatDate } from "@/utils";

export default {
  components: { ShowTablePro, Drag },
  mixins: [mixinReport],
  data() {
    return {
      // 单位树
      deptName: undefined,
      deptOptions: undefined,
      deptTreeLoading: false,
      treeData: [],
      buttonCount: 1,
      levelExpand: 2,
      arrData: [],
      defaultProps: {
        children: "children",
        label: "label",
      },
      queryItemList: [
        {
          prop: "minAccountFullName",
          label: "项目名称",
          type: "input",
        },
        {
          prop: "prjState",
          label: "项目业态",
          type: "select",
        },
      ],
      tableHeaderList: [
        {
          prop: "deptName",
          label: "组织机构",
          width: 250,
          showOverflowTooltip: true,
          headerSlot:
            "项目端只展示该项目的数据，公司端展示公司（子公司、分公司）所属项目的数据，集团端展示所有的数据；不同层级展示的数据不同。",
        },
        {
          prop: "minAccountFullName",
          label: "项目名称",
          width: 250,
          showOverflowTooltip: true,
          headerSlot: "获取项目立项的项目简称字段",
        },
        {
          prop: "prjStateName",
          label: "项目业态",
          width: 100,
          showOverflowTooltip: true,
          headerSlot: "获取项目立项的项目业态字段",
        },
        {
          prop: "cgNum",
          label: "采购次数",
          showOverflowTooltip: true,
          align: "right",
          headerSlot:
            "统计所有完成招标流程采购类型的汇总，且未废标的（按项目、公司、集团层级分别向上汇总）；数据穿透展示这一类型所有的",
          clickMethod: (row) => {
            this.goBidding(row, "all");
          },
        },
        {
          prop: "gkNum",
          label: "公开次数",
          showOverflowTooltip: true,
          align: "right",
          headerSlot:
            "统计招标流程公开招标的总次数（按项目、公司、集团层级分别向上汇总）；数据穿透展示这一类型所有的",
          clickMethod: (row) => {
            this.goBidding(row, "1");
          },
        },
        {
          prop: "yqNum",
          label: "邀标次数",
          showOverflowTooltip: true,
          align: "right",
          headerSlot:
            "统计招标流程邀请招标的总次数（按项目、公司、集团层级分别向上汇总）；数据穿透展示这一类型所有的",
          clickMethod: (row) => {
            this.goBidding(row, "2");
          },
        },
        {
          prop: "xjNum",
          label: "询价次数",
          showOverflowTooltip: true,
          align: "right",
          headerSlot:
            "统计招标流程询价的总次数（按项目、公司、集团层级分别向上汇总）；数据穿透展示这一类型所有的",
          clickMethod: (row) => {
            this.goBidding(row, "3");
          },
        },
        {
          prop: "dyNum",
          label: "单一次数",
          showOverflowTooltip: true,
          align: "right",
          headerSlot:
            "统计招标流程单一来源的总次数（按项目、公司、集团层级分别向上汇总）；数据穿透展示这一类型所有的",
          clickMethod: (row) => {
            this.goBidding(row, "4");
          },
        },
        {
          prop: "gkTotalNum",
          label: "公开总次数",
          showOverflowTooltip: true,
          align: "right",
          headerSlot: "统计公开招标次数+邀标次数",
        },
        {
          prop: "ngkTotalNum",
          label: "非公开总次数",
          showOverflowTooltip: true,
          align: "right",
          headerSlot: "统计询价次数+单一次数",
        },
        {
          prop: "gkRatio",
          label: "公开率（%）",
          showOverflowTooltip: true,
          align: "right",
          headerSlot: "采用公开招标的笔数/所有采购的笔数",
        },
      ],
      tableData: [],
    };
  },
  watch: {
    // 根据名称筛选单位树
    deptName(val) {
      this.$refs.tree.filter(val);
    },
  },
  mounted() {
    // 单位树按登录用户数据权限加载，不受顶部"单位-项目"选择框限制
    this.getOrgListFn();
    // 默认查登录用户数据权限范围内所有单位的数据
    this.getList(this.queryParams);
  },
  methods: {
    getList(params) {
      this.tableData = [];
      this.loading = true;
      bidCountReport(params)
        .then((res) => {
          this.tableData = res.data || [];
          // 第二层默认还有下一层，点击时懒加载
          if (this.tableData.length > 0 && this.tableData[0].children) {
            this.tableData[0].children.forEach((item) => {
              item.hasChildren = true;
            });
          }
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
    },
    /**
     * 次数列穿透到招标列表
     * 同时联动顶部"单位&项目"选择框：单位行选中该单位，项目行选中该单位+项目
     * @param row 当前行
     * @param procurementType 采购类型：all=全部、1=公开、2=邀标、3=询价、4=单一
     */
    goBidding(row, procurementType) {
      if (row.type === "G") {
        // 公司/集团层级：先取本级及以下所有项目编码
        this.commitTopSelector(row.id);
        getProjectCode(row.id).then((res) => {
          this.$router.push({
            path: "/procurement/bindding",
            query: {
              projectCodeList: res.data,
              type: "buildingRate",
              noticeStatus: "8",
              procurementType: procurementType,
              report: "report",
            },
          });
        });
      } else {
        // 项目层级：联动顶部选择框选中项目及其所属单位
        this.commitTopSelector(row.projectDepartmentId);
        this.$store.commit("SET_PROJECT", {
          code: row.id,
          id: row.id,
          name: row.minAccountFullName,
        });
        this.$router.push({
          path: "/procurement/bindding",
          query: {
            projectCode: row.id,
            type: "buildingRate",
            noticeStatus: "8",
            procurementType: procurementType,
            report: "report",
          },
        });
      }
    },
    /**
     * 联动顶部"单位&项目"选择框：按 thridDeptId 在单位树中找级联路径并写入 vuex
     * @param thridDeptId 单位/部门第三方id
     */
    commitTopSelector(thridDeptId) {
      if (!thridDeptId) return;
      const path = this.findOrgPath(thridDeptId);
      if (path && path.length) {
        this.$store.commit("SET_ORG", path);
      }
    },
    /**
     * 在单位树(与顶部选择框同源的 getDeptTree 树)里找 thridDeptId 的级联路径
     * @param thridDeptId
     * @param nodes
     * @param path
     * @returns {[]}
     */
    findOrgPath(thridDeptId, nodes = this.arrData, path = []) {
      if (!Array.isArray(nodes)) return [];
      for (const n of nodes) {
        const p = path.concat(n.thridDeptId);
        if (n.thridDeptId === thridDeptId) return p;
        if (n.children && n.children.length) {
          const r = this.findOrgPath(thridDeptId, n.children, p);
          if (r && r.length) return r;
        }
      }
      return [];
    },
    /**
     * 点击单位树节点：表格切到该单位的数据
     * @param data 节点数据
     */
    handleNodeClick(data) {
      this.queryParams.id = data.thridDeptId;
      this.handleQuery(this.queryParams);
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label && data.label.indexOf(value) !== -1;
    },
    /**
     * 获取单位树(单位 + 单位下所有部门)
     * 与采购台账左树/顶部"单位-项目"选择框同源(/system/dept/getDeptTree)，
     * 返回的已是 TreeSelect 树(label/children/thridDeptId)，直接使用即可
     */
    getOrgListFn() {
      this.deptTreeLoading = true;
      getDeptTree()
        .then((res) => {
          this.arrData = res.data || [];
          this.deptOptions = this.arrData;
          // 默认展开根节点
          if (this.arrData.length > 0) {
            this.treeData = [this.arrData[0].id];
          }
          this.deptTreeLoading = false;
        })
        .then(() => {
          this.initButtons();
        })
        .catch(() => {
          this.deptTreeLoading = false;
        });
    },
    /**
     * 展开到指定层级：按钮 1 = 全部收起，按钮 N = 展开第 1~N-1 层
     * @param level
     */
    expandNodes(level) {
      this.levelExpand = level;
      const tree = this.$refs.tree;
      const allNodes = Object.values(tree.store.nodesMap);
      allNodes.forEach((node) => {
        // el-tree 的层级从 1 开始(1 为根节点)
        tree.store.getNode(node.key).expanded = node.level < level;
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
    // 初始化层级按钮个数
    initButtons() {
      const maxLevel = this.calculateMaxLevel(this.deptOptions);
      this.buttonCount = maxLevel + 1;
    },
    getExport(params) {
      this.download(
        "business/report/bidCountReportExport",
        {
          ...params,
        },
        `${formatDate(new Date())}招标率统计报表.xlsx`
      );
    },
  },
};
</script>

<style lang="scss" scoped>
::v-deep .vue-treeselect {
  .vue-treeselect__control {
    height: 32px;
  }
}
.address-tree {
  margin: 0;
  height: calc(100vh - 105px);
  overflow-y: scroll;

  &::-webkit-scrollbar {
    display: none;
  }

  ::v-deep .icon-shouyetianchong:before {
    content: "\E692";
    color: #004ea2;
  }

  ::v-deep .icon-24gf-folderOpen:before {
    content: "\eac5";
    color: #004ea2;
  }

  ::v-deep .el-tree-node {
    .el-tree-node__content {
      height: auto;
      padding: 2px 0;
      margin: 2px 0;

      font-size: 13px;
      color: #606266;

      .el-tree-node__label {
        white-space: pre-wrap;
        line-height: 20px;
      }
    }
  }
}

::v-deep .address-tree .el-tree-node .is-current > .el-tree-node__content {
  color: #2b4acb !important;
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
  margin: 5px;
}
.circle-btn-selected {
  background-color: #2b4acb;
  color: #fff;
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
  margin: 5px;
}
/* 鼠标移上去时的效果 */
.circle-btn:hover {
  background-color: #2b4acb;
  color: #ffffff;
}
::v-deep .left_box {
  background: #ffffff;
}
</style>
