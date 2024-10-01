<template>
  <div class="app-container" style="overflow: auto; height: calc(100vh - 84px)">
    <div
      style="
        height: 100%;
        background-color: #fff;
        padding: 16px;
        box-sizing: border-box;
      "
      class="flex flex-column"
    >
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
        <slot name="querySlot" :queryParms="queryParams"></slot>
        <span v-for="(item, index) in queryItemList" :key="index">
          <el-form-item
            :label="item.label"
            :prop="item.prop"
            v-if="item.type === 'treeSelect'"
          >
            <treeselect
              style="width: 300px; height: 32px"
              :normalizer="normalize(item)"
              v-model="queryParams.deptId"
              :options="item.options"
              :placeholder="`请选择${item.label}`"
            >
              <label
                slot="option-label"
                slot-scope="{
                  node,
                  shouldShowCount,
                  count,
                  countClassName,
                  labelClassName,
                }"
                :class="labelClassName"
                :title="node.label"
              >
                {{ node.label }}
              </label>
            </treeselect>
          </el-form-item>
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
          <el-form-item :label="item.label" :prop="item.prop" v-else>
            <el-input
              v-model="queryParams[item.prop]"
              :placeholder="`请输入${item.label}`"
              clearable
              @keyup.enter.native="handleQuery"
              size="medium"
              style="height: 34px"
            />
          </el-form-item>
        </span>
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
      <el-table
        :data="tableData"
        height="100%"
        class="flex1"
        border
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :header-cell-style="{ background: '#F3F2F8', textAlign: 'center' }"
        default-expand-all
        v-loading="loading"
      >
        <el-table-column type="index" align="center" label="序号">
        </el-table-column>
        <template v-for="item in tableHeaderList">
          <el-table-column
            :label="item.label"
            v-if="item.type === 'multiple'"
            align="center"
          >
            <el-table-column
              v-for="secItem in item.children"
              :prop="secItem.prop"
              :label="secItem.label"
              :show-overflow-tooltip="secItem.showOverflowTooltip"
              :width="secItem.width"
              :align="secItem.align"
              :formatter="formatter"
            >
            </el-table-column>
          </el-table-column>

          <el-table-column
            :label="item.label"
            :width="item.width"
            :show-overflow-tooltip="item.showOverflowTooltip"
            :prop="item.prop"
            :formatter="formatter"
            :align="item.align"
            v-else
          >
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
  </div>
</template>

<script>
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

export default {
  components: { Treeselect },
  data() {
    return {
      queryParams: {},
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
    loading: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    goDetail(row, method) {
      method(row);
    },
    formatter(row, column) {
      return row[column.property] || "/";
    },
    normalize(item) {
      return (node) => {
        if (node.children && !node.children.length) {
          delete node.children;
        }
        return {
          id: node[item.valueKey],
          //将name转换成必填的label键
          label: node[item.labelKey],
          children: node.children,
        };
      };
    },
    handleQuery() {
      this.$emit("query", this.queryParams);
      console.log("handleQuery");
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
