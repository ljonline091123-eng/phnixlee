<template>
  <div class="app-container" style="overflow: auto; height: calc(100vh - 4px)">
    <div
      style="height: 100%; background-color: #fff; padding: 16px; box-sizing: border-box"
      class="flex flex-column"
    >
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
        <slot name="querySlot" :queryParms="queryParams"></slot>
        <span v-for="(item, index) in queryItemList" :key="index">
          <!-- 组织机构 / 合作单位 -->
          <el-form-item
            :label="item.label"
            :prop="item.prop"
            v-if="item.type === 'treeSelect'"
          >
            <treeselect
              style="width: 300px; height: 32px"
              :normalizer="normalize(item)"
              v-model="queryParams.deptId"
              :options="deptOptions"
              :placeholder="`请选择${item.label}`"
            >
              <label
                slot="option-label"
                slot-scope="{ node, labelClassName }"
                :class="labelClassName"
                :title="node.label"
              >
                {{ node.label }}
              </label>
            </treeselect>
          </el-form-item>
          <!-- 日期区间 -->
          <el-form-item
            :label="item.label"
            :prop="item.prop"
            v-else-if="item.type === 'dateRange'"
          >
            <el-date-picker
              style="height: 34px"
              size="small"
              v-model="queryParams[item.prop]"
              value-format="yyyy-MM-dd"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            >
            </el-date-picker>
          </el-form-item>
          <!-- 文本 -->
          <el-form-item :label="item.label" :prop="item.prop" v-if="item.type === 'input'">
            <el-input
              v-model="queryParams[item.prop]"
              :placeholder="`请输入${item.label}`"
              clearable
              @keyup.enter.native="handleQuery"
              size="medium"
              style="height: 34px"
            />
          </el-form-item>
          <!-- 字典下拉(如项目业态) -->
          <el-form-item :label="item.label" :prop="item.prop" v-if="item.type === 'select'">
            <el-select
              v-model="queryParams[item.prop]"
              :placeholder="`请选择${item.label}`"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="dict in dictObj"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <!-- 页面里写死的下拉选项 -->
          <el-form-item :label="item.label" :prop="item.prop" v-if="item.type === 'selectArr'">
            <el-select
              v-model="queryParams[item.prop]"
              :placeholder="`请选择${item.label}`"
              clearable
              style="width: 180px"
            >
              <el-option
                v-for="dict in item.arr"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </span>
        <el-form-item>
          <el-button
            type="primary"
            icon="el-icon-search"
            size="small"
            @click="handleQuery"
            v-show="queryFlag"
            >查询</el-button
          >
        </el-form-item>
        <el-button
          type="primary"
          plain
          icon="el-icon-download"
          size="small"
          @click="handleExport"
          v-show="exportFlag"
          >导出
        </el-button>
      </el-form>
      <el-table
        :data="tableData"
        height="100%"
        class="flex1"
        ref="tableMy"
        border
        lazy
        :load="load"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :header-cell-style="{ background: '#F3F2F8', textAlign: 'center' }"
        default-expand-all
        v-loading="loading"
      >
        <el-table-column :fixed="radioType != '1'" type="index" align="center" label="序号">
        </el-table-column>
        <template v-for="item in tableHeaderList">
          <!-- 合并表头 -->
          <el-table-column
            :label="item.label"
            v-if="item.type === 'multiple'"
            align="center"
          >
            <el-table-column
              v-for="secItem in item.children"
              :key="secItem.prop"
              :prop="secItem.prop"
              :label="secItem.label"
              :show-overflow-tooltip="secItem.showOverflowTooltip"
              :width="secItem.width"
              :align="secItem.align"
            >
            </el-table-column>
          </el-table-column>

          <el-table-column
            :key="item.prop"
            :label="item.label"
            :width="item.width"
            :show-overflow-tooltip="item.showOverflowTooltip"
            :prop="item.prop"
            :align="item.align"
            :fixed="item.fixed"
            v-else
          >
            <!-- 表头问号提示 -->
            <template slot="header" v-if="item.headerSlot">
              <el-tooltip effect="dark" placement="top">
                <template #content>
                  <div v-html="item.headerSlot"></div>
                </template>
                <i class="el-icon-question" style="margin-left: 4px; cursor: pointer; color: #909399;"></i>
              </el-tooltip>
              <span>{{ item.label }}</span>
            </template>
            <template slot-scope="scope">
              <a
                v-if="item.clickMethod"
                class="link-type"
                @click="goDetail(scope.row, item.clickMethod)"
              >
                {{ scope.row[item.prop] }}
              </a>
              <span v-else>{{ scope.row[item.prop] }}</span>
            </template>
          </el-table-column>
        </template>
      </el-table>
    </div>
    <pagination
      v-if="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="handleQuery"
    />
  </div>
</template>

<script>
/**
 * 报表表格(左树右表 / 树形表 / 分页表)
 *
 * 来源：成控项目(prod)的 ShowTable.vue，供 招标率统计 / 供应商报表 / 问题报表 三个页面使用。
 * 与 components/ShowTable.vue(老报表页面在用) 的区别：
 * 1. 支持 el-table 懒加载树(:load + reportType)、分页、页面写死的下拉选项(selectArr)、表头提示(headerSlot)；
 * 2. 不再监听顶部"单位-项目"选择框(vuex 的 org/project/scopeType)——这三个报表的数据范围由后端按
 *    登录用户数据权限收敛，页面里的树/下拉只做二次筛选；
 * 3. 组织树改走 /business/report/getOrgList(后端已按数据权限收敛)，不再调 listDept() 拉全量部门。
 *
 * 老报表页面(合同台账/价格分析等)请继续使用 components/ShowTable.vue。
 */
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { listUnderlingDict } from "@/api/procurement/contract";
import { getOrgList } from "@/api/reportForm/tenderingRateReport";
import { getBidCountNext, getProblemNext } from "@/api/reportForm/tenderingRateReport";

export default {
  components: { Treeselect },
  data() {
    return {
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
      dictObj: [],
      deptOptions: [],
      // 懒加载树已加载过的节点，用于刷新(重新查询)时重放
      maps: new Map(),
    };
  },
  props: {
    // 查询条件
    queryItemList: {
      type: Array,
      default: () => [],
    },
    // 表格数据
    tableData: {
      type: Array,
      default: () => [],
    },
    // 表头
    tableHeaderList: {
      type: Array,
      default: () => [],
    },
    // 总条数(不传或为 0 时不显示分页)
    total: {
      type: Number,
      default: -1,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    radioType: {
      type: String,
      default: "1",
    },
    // 懒加载树的数据来源：building=招标率统计，problem=问题报表
    reportType: {
      type: String,
      default: "",
    },
    // 是否显示导出按钮
    exportFlag: {
      type: Boolean,
      default: false,
    },
    // 是否显示查询按钮
    queryFlag: {
      type: Boolean,
      default: false,
    },
  },
  mounted() {
    // 项目业态字典
    this.getListUnderlingDict("PROJECT_FORMAT");
    // 组织树按登录用户数据权限加载
    this.getDeptTree();
  },
  methods: {
    /**
     * 懒加载树的下一层
     * @param tree 当前行
     * @param treeNode 树节点
     * @param resolve 回调，必须调用，否则展开图标一直转圈
     */
    load(tree, treeNode, resolve) {
      this.maps.set(tree.id, { tree, treeNode, resolve });
      // 已加载过的节点直接用缓存
      if (tree.children && tree.children.length > 0) {
        resolve(tree.children);
        return;
      }
      const queryParamsChildren = Object.assign({}, this.queryParams, { id: tree.id });
      const query = this.reportType === "problem"
        ? getProblemNext(queryParamsChildren)
        : getBidCountNext(queryParamsChildren);
      query
        .then((res) => {
          const arr = res.data || [];
          // 默认都当成还有下一层，没有数据时展开为空
          arr.forEach((item) => {
            item.hasChildren = true;
          });
          resolve(arr);
        })
        .catch(() => {
          treeNode.loading = false;
          resolve([]);
        });
    },
    /**
     * 重新查询前先收起/重放已展开的懒加载节点
     */
    refresh() {
      this.maps.forEach((value) => {
        const { tree, treeNode, resolve } = value;
        if (tree) {
          this.load(tree, treeNode, resolve);
        }
      });
    },
    /**
     * 获取项目业态等业务字典(本地字典，不走平台接口)
     * @param type 字典类型
     */
    async getListUnderlingDict(type) {
      const res = await listUnderlingDict(type);
      this.dictObj = res.data.map((item) => ({
        value: item.dictValue,
        label: item.dictLabel,
      }));
    },
    /**
     * 获取当前登录用户有数据权限的组织树
     */
    getDeptTree() {
      getOrgList().then((response) => {
        this.deptOptions = this.handleTree(response.data, "deptId");
      });
    },
    goDetail(row, method) {
      method(row);
    },
    normalize(item) {
      return (node) => {
        if (node.children && !node.children.length) {
          delete node.children;
        }
        return {
          id: node[item.valueKey],
          // 将 name 转换成必填的 label 键
          label: node[item.labelKey],
          children: node.children,
        };
      };
    },
    handleQuery() {
      this.refresh();
      this.$emit("query", this.queryParams);
    },
    handleExport() {
      this.$emit("export", this.queryParams);
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

::v-deep .el-input .el-input__inner {
  height: 34px;
}
</style>
