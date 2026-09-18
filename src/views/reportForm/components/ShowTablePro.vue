<template>
  <div class="app-container" style="height: 100%">
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
            :show-overflow-tooltip="item.tooltipChunk ? false : item.showOverflowTooltip"
            :prop="item.prop"
            :align="item.align"
            :fixed="item.fixed"
            :className="item.className"
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
              <!-- 自定义悬浮提示：tooltipChunk 指定每行几个，列表只显示一行省略 -->
              <el-tooltip
                v-if="item.tooltipChunk && scope.row[item.prop]"
                placement="top"
                popper-class="report-chunk-tooltip"
              >
                <div slot="content" class="report-chunk-tooltip-content">
                  <div
                    v-for="(group, gi) in chunkBy(scope.row[item.prop], item.tooltipChunk)"
                    :key="gi"
                    class="report-chunk-tooltip-line"
                  >
                    <span
                      v-for="(piece, pi) in group"
                      :key="pi"
                      class="report-chunk-tooltip-item"
                    >{{ piece }}</span>
                  </div>
                </div>
                <span class="report-cell-ellipsis">{{ scope.row[item.prop] }}</span>
              </el-tooltip>
              <a
                v-else-if="item.clickMethod"
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
 * 3. 组织树改走 /system/dept/getDeptTree(与采购台账左树、顶部"单位-项目"选择框同源，按登录用户
 *    数据权限返回单位+部门)，不再调 listDept() 拉全量部门。
 *
 * 老报表页面(合同台账/价格分析等)请继续使用 components/ShowTable.vue。
 */
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { listUnderlingDict } from "@/api/procurement/contract";
import { getDeptTree } from "@/api/system/dept";
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
    this.loadDeptTree();
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
     * 获取组织树(单位 + 单位下所有部门)
     * 与采购台账左树/顶部"单位-项目"选择框同源(/system/dept/getDeptTree)，
     * 返回的已是 TreeSelect 树(label/children/thridDeptId)，无需再组树
     */
    loadDeptTree() {
      getDeptTree().then((response) => {
        this.deptOptions = response.data || [];
      });
    },
    /**
     * 把换行分隔的字符串按每行 n 个分组（配合 tooltipChunk 自定义悬浮提示）
     * @param value 后端返回的换行分隔文本
     * @param n 每行个数
     * @returns {[]}
     */
    chunkBy(value, n) {
      const arr = String(value || "")
        .split("\n")
        .filter((s) => s !== "");
      const result = [];
      for (let i = 0; i < arr.length; i += n) {
        result.push(arr.slice(i, i + n));
      }
      return result;
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

<style lang="scss">
/* 自定义悬浮提示（tooltip 渲染到 body 上，需要全局样式）：
   按 tooltipChunk 个一组换行显示，列表单元格只显示一行省略 */
.report-chunk-tooltip-content {
  line-height: 1.6;
}
.report-chunk-tooltip-line {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 16px;
}
.report-chunk-tooltip-item {
  white-space: nowrap;
}
.report-cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  vertical-align: bottom;
}
</style>
