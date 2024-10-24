import { mapGetters } from 'vuex'
import {listDept} from "@/api/system/dept";
import { deptTree,  } from "@/api/reportForm/managePageReport";

export  const mixin = {
  data() {
    return {
      queryParams:{},
      deptOptions: [],
      loading:false
    }
  },
  mounted() {
    listDept().then(response => {
      this.$set(this.queryItemList[0], "options", this.handleTree(response.data, "deptId"));
    });
  },
  methods: {
    /**
     * 查询
     * @param params
     */
    handleQuery(params) {
      // * 拆分dateRange
      console.log("mixin-scopeType-查询",this.$store.state.app.scopeType)
      console.log("mixin-queryParams-查询",this.params)
      console.log("mixin-project-查询",this.project)
      console.log("mixin-org-查询",this.org)
      console.log("mixin-org-radioType",this.radioType)

      params.startTime = params.dateRange && params.dateRange[0]
      params.endTime =  params.dateRange && params.dateRange[1]
      this.queryParams = Object.assign({}, this.queryParams, params)
      if(this.radioType){
        this.queryParams.conType=this.radioType
      }
    
      this.getList(this.queryParams)
    },
    /**
     * 下载
     * @param params
     */
    handleExport(params) {
      params.startTime = params.dateRange && params.dateRange[0]
      params.endTime =  params.dateRange && params.dateRange[1]
      this.queryParams = Object.assign({}, this.queryParams, params)
      this.getExport(this.queryParams)
    },

  },



}
