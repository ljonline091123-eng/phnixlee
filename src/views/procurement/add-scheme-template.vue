<template>
  <el-dialog :title="title" class="dialogClass" :visible.sync="visible" width="60%">
    <el-table v-loading="loading" :data="list" @row-click="rowClick" size="mini">
      <el-table-column label="" width="30" align="center">
        <template slot-scope="scope">
          <el-radio class="table_radio" v-model="templateId" :label="scope.row.id" />
        </template>
      </el-table-column>
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column label="模板名称" align="center" prop="templateName" width="200" />
      <el-table-column label="维护人" align="center" prop="createBy" />
      <el-table-column label="创建日期" align="center" prop="createTime" />
      <el-table-column label="使用单位" align="center" prop="usingUnitName" />
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNumber"
      :limit.sync="query.pageSize" @pagination="getTemplateList" />
    <div slot="footer" class="dialog-footer">
      <el-button @click="cancel">取 消</el-button>
      <el-button type="primary" @click="confirm">确 定</el-button>
    </div>
  </el-dialog>
</template>
<script>
import { getTemplateList } from '@/api/procurement/scheme'
export default {
  props:{
    visible:{
      type:Boolean,
      default:false
    },
    title:{
      type:String,
      default:'选择模板'
    },
    type:{
      type:String,
      default:''
    }
  },
  data(){
    return {
      templateId:'',
      query:{
        pageNumber:1,
        pageSize:10
      },
      total:0,
      list:[]
    }
  },
  watch:{
    type:{
      handler(val){
        this.getTemplateList()
      },
      // immediate:true
    }
  },
  methods:{
    async getTemplateList(){
      this.loading = true
      try{
        const res = await getTemplateList(this.query)
        this.list = res.data.rows
        this.total = res.data.total
      }catch(err){
        console.log(err);
      }
      this.loading = false
    },
    rowClick(row){

    },
    confirm(){

    },
    cancel(){
      
    }
  }
}
</script>
<style lang="scss" scoped>

</style>