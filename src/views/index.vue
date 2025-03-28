<template>
  <div class="app-container">
    <div class="context flex flex-column">
      <el-radio-group
      style="margin:10px 0 20px 10px;"
      v-model="queryRadioType"
      size="small"
    >
        <el-radio-button
        :label="dict.value"
        :name="dict.value" 
        v-for="dict in radioList"
        :key="dict.value"
        >{{ dict.label }}</el-radio-button>
    </el-radio-group>
    
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
      >
        <el-form-item label="消息标题" prop="schemeCode" label-width="80px">
          <el-input
            v-model="queryParams.processTitle"
            placeholder="请输入消息标题"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <!-- <el-form-item label="消息内容" prop="schemeName" label-width="80px">
          <el-input
            v-model="queryParams.procurementSchemeName"
            placeholder="请输入消息内容"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item> -->
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
        v-loading="loading"
        :data="todoList"
        highlight-current-row
        stripe
        border
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
       
        <el-table-column
          label="菜单名称"
          show-overflow-tooltip
        >
        <template slot-scope="scope">
          <span v-if="scope.row.processTitle === 'null' || scope.row.processTitle === 'undefined'">无名称</span>
          <span v-else>{{ scope.row.processTitle }}</span>
        </template>
        </el-table-column>
        <el-table-column
          label="项目名称"
          prop="projectCode"
          show-overflow-tooltip
        />
        <el-table-column
          label="消息内容"
          min-width="250"
          align="center"
          prop="businessContent"
        />
        <el-table-column
        align="center"
          label="消息状态"
          prop="messageStatus"
        />
        <el-table-column
          label="上一审批人"
          min-width="150"
          align="center"
          prop="previousApprover"
        />
        <el-table-column
          label="提交时间"
          min-width="150"
          align="center"
          prop="createTime"
        />
    
        <el-table-column
          label="操作"
          align="center"
          fixed="right"
        >
          <template slot-scope="scope">
              <el-button
              @click="goDetail(scope.row.detailUrl)"
                type="text"
                size="small"
                >处理</el-button>
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
   
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import { mapGetters } from "vuex";

import {geTaskTodoList,getFinishedList} from "@/api/index";
import {  getSplitPlanList } from "@/api/procurement/plan";
import {abandonBidMore} from "@/api/procurement/manage";

export default {
  name: "Scheme",
  dicts: ["procurement_plan_type"],
  data() {
    return {
      queryRadioType: "untreated",
      radioList: [
        {
          value: 'untreated',
          label: "未处理",
        },
        {
          value: 'processed',
          label: "已处理",
        },
        // {
        //   value: 'all',
        //   label: "全部",
        // },
      ],
      loading: false,
      total: 0,
      todoList: [],
       // 显示搜索条件
      showSearch: true,
         // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,

      },
    };
  },
  watch:{
    queryRadioType: {
        handler(value) {
          console.log(value)
          this.getList();
        }
      }
    },
  mounted(){
    this.getList();
  },
  methods: {
        /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 获取需求列表 */
    async getList() {
      this.loading = true;
      const query = {
        ...this.queryParams,
      };
      try {
        var res ={}
        console.log(JSON.stringify(this.queryRadioType))
        if(this.queryRadioType=='processed'){
          res = await getFinishedList(query);

        }else {
           res = await geTaskTodoList(query);
        }
      
        this.loading = false;
        if (res.data) {
          this.todoList = res.data || [];
          this.total = res.total;
        }
      } catch (err) {
        this.loading = false;
        console.log(err);
      }
    },

      /** 跳转方案详情 */
      goDetail(url) {
        if(url!=null){
          this.$router.push(url);
        }
      
    },

  },
  computed: {
    ...mapGetters(["project"]),
  },

};
</script>
<style lang="scss" scoped>
.hxwd_table_item_center_first {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hxwd_table_item_center {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #eaeaea;
}

.hxwd_table_item_left_first {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
}

.hxwd_table_item_left {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  border-top: 1px solid #eaeaea;
}
.required {
  color: rgb(245, 108, 108);
  margin-right: 4px;
}
</style>
