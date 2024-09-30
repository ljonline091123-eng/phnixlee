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
              :options="deptOptions"
              :placeholder="`请选择${item.label}`"
              :disabled="scopeType!='1'"
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
          <el-form-item :label="item.label" :prop="item.prop"   v-if="item.type === 'input'">
            <el-input
              v-model="queryParams[item.prop]"
              :placeholder="`请输入${item.label}`"
              clearable
              @keyup.enter.native="handleQuery"
              size="medium"
              style="height: 34px"
            />
          </el-form-item>
          <el-form-item :label="item.label" :prop="item.prop" v-if="item.type === 'select'">
            <el-select
            v-model="queryParams[item.prop]"
            :placeholder="`请输入${item.label}`"
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
import {mapGetters} from "vuex";
import { deptTree,  } from "@/api/reportForm/managePageReport";
import { listUnderlingDict,  } from "@/api/procurement/contract";
export default {
  components: { Treeselect },
  data() {
    return {
      queryParams: {},
      dictObj: [],
      deptOptions:[],
      scopeType:'',
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
    // 导出
    exportFlag: {
      type: Boolean,
      default: false,
    },
  },
  computed:{
    ...mapGetters(['project','org']),
  },
  watch:{
      '$store.state.app.scopeType': {
        handler(newVal, oldVal) {
          console.log('scopeType-newVal', newVal)
          console.log('scopeType-oldVal', oldVal)
          if(newVal){
            console.log('scopeType-this.$store.state.app.scopeType', this.$store.state.app.scopeType)
            console.log('scopeType-this.project', this.project)
            console.log('scopeType-this.org', this.org)
            if(newVal == 1) {
              this.queryParams.id = '';
              this.queryParams.minAccountCode = this.project.code;
            }else{
              this.queryParams.id =  this.org;
              this.queryParams.minAccountCode = '';
            }
            console.log('scopeType-this.queryParams', this.queryParams)
            this.$emit("query", this.queryParams);
          }
        },
        immediate: true
      },
      'project.id': {
        handler(newVal){
          console.log('project.id-newVal', newVal)
          if(newVal) {
            console.log('project.id-this.$store.state.app.scopeType', this.$store.state.app.scopeType)
            console.log('project.id-this.project', this.project)
            console.log('project.id-this.org', this.org)
            if(this.$store.state.app.scopeType == "1"){
              this.queryParams.id = '';
              this.queryParams.minAccountCode = this.project.code
            } else {
              this.queryParams.id =  this.org;
              this.queryParams.minAccountCode = '';
            }
            console.log('project.id-this.queryParams', this.queryParams)
            this.$emit("query", this.queryParams);
          }
        }
      },
      org: {
        handler(newVal) {
          console.log('org-newVal', newVal)
          if(newVal) {
            console.log('org-this.$store.state.app.scopeType', this.$store.state.app.scopeType)
            console.log('org-this.project', this.project)
            console.log('org-this.org', this.org)
            if(this.$store.state.app.scopeType == "1"){
              this.queryParams.id = '';
              this.queryParams.minAccountCode = this.project.code
            } else {
              this.queryParams.id = newVal;
              this.queryParams.minAccountCode = '';
            }
            console.log('org-this.queryParams', this.queryParams)
            this.$emit("query", this.queryParams);
          }
        }
      }
  },
  mounted() {
    this.scopeType=this.$store.state.app.scopeType
    this.getListUnderlingDict('PROJECT_FORMAT');

  },
  methods: {
     //获取字典
     async getListUnderlingDict(type) {
      const res = await listUnderlingDict(type);
      const resMap = res.data.map((item) => ({
        value: item.dictValue,
        label: item.dictLabel,
      }));
      console.log(JSON.stringify(resMap));
      this.dictObj = resMap;
    },
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
      console.log("showTable-scopeType-查询",this.$store.state.app.scopeType)
      console.log("showTable-queryParams-查询",this.queryParams)
      console.log("showTable-project-查询",this.project)
      console.log("showTable-org-查询",this.org)
      this.$emit("query", this.queryParams);
    },
    handleExport() {
      this.$emit("export", this.queryParams);
    },
      /** 查询部门下拉树结构 */
    //获取组织
    getDeptTree(id) {
      // this.deptTreeLoading = true;
      deptTree({
        thridOrgId: id,
      })
        .then((response) => {
          this.deptOptions = this.handleTree(response.data, "deptId");
          // this.deptTreeLoading = false;
        })
        .then(() => {
          this.initButtons();
        })
        .catch(() => {
          this.deptTreeLoading = false;
        });
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
