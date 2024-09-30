<template>
  <div class="app-container">
    <div class="context"> 
    <el-table :data="evaluateList" class="no-right-border" stripe border
      highlight-current-row>
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column label="任务编号" width="200" align="center" prop="procurementSchemeCode" />
      <el-table-column label="任务名称" width="200" prop="procurementSchemeName" show-overflow-tooltip/>
      <el-table-column label="上限价(元)" align="right" prop="ceilingPricePattern" />
      <el-table-column label="招标方式" align="center" prop="procurementTypeText" />
       <el-table-column label="投标供应商" width="300" prop="evalTaskContentVOList" show-overflow-tooltip>
        <template slot-scope="{row}">
          <div  v-for="(item,index) in row.evalTaskContentVOList" :key="index" class="splitClass left">
            <a class="link-type" href="javascript:;" @click="goEvauateBids(item, row.schemeId, row.noticeId)">{{ item.vendorName }}</a>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="含税总价(元)" align="right" prop="evalTaskContentVOList">
        <template slot-scope="{row}">
          <div v-for="(item,index)  in row.evalTaskContentVOList" :key="index" class="splitClass right">{{ item.taxPricePattern }}</div>
        </template>
      </el-table-column>
      <el-table-column label="评分" align="center" prop="evalTaskContentVOList">
        <el-table-column prop="taxPrice" label="商务" align="center">
          <template slot-scope="{row}">
          <div  v-for="(item,index)  in row.evalTaskContentVOList" :key="index" class="splitClass">{{ item.busScore? item.busScore : '未评分' }}</div>
        </template>
        </el-table-column>
        <el-table-column prop="notTaxPrice" label="技术" align="center">
          <template slot-scope="{row}">
            <div  v-for="(item,index)  in row.evalTaskContentVOList" :key="index" class="splitClass">{{ item.techScore? item.techScore : '未评分' }}</div>
          </template>
        </el-table-column>
      </el-table-column>
      <el-table-column label="评标" align="center" fixed="right">
        <template slot-scope="{row}">
          <div class="splitClass" v-for="item in row.evalTaskContentVOList">
            已评分
          </div>
        </template>
      </el-table-column> 
    </el-table>
    <div class="pagination_item">
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNumber" :limit.sync="queryParams.pageSize"
      @pagination="getDoneEvalTaskPage" />
  </div>
    </div>
  </div>
</template>

<script>
import { Base64 } from 'js-base64';
import { getDoneEvalTaskPage} from "@/api/evaluate-expert/evaluate-bids"
export default {
  name: "evaluate-bids-end",
  data() {
    return {
      evaluateList:[],
      total:0,
      loading:false,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
      }
    };
  },
  created() {
    this.getDoneEvalTaskPage()
  },
  methods: {
    /** 获取评标列表 */
    async getDoneEvalTaskPage() {
      this.loading = true;
      try {
        const res = await getDoneEvalTaskPage(this.queryParams)
        this.loading = false;
        this.evaluateList = res.data.rows
        this.total = res.data.total
        console.log(res,'res--res');
      } catch (err) {
        this.loading = true;
        console.log(err)
      }
    },
    goEvauateBids(item,schemeId,noticeId){
      let param = Base64.encode(JSON.stringify({item,schemeId,noticeId}))
      param = encodeURIComponent(param)
      this.$router.push(`/evaluate-expert/evaluate-bids-end-detail/${param}`);
    },
  }
};
</script>
<style lang="scss" scoped>
.form-body {
  padding: 20px;

  .textColor {
    color: red;
  }
}
.splitClass {
  display: flex;align-items: center;justify-content: center;padding: 5px 0;
  // line-height: 40px;
  &.left{
    justify-content: flex-start;
  }
  &.right{
    justify-content: flex-end;
  }
}
</style>
