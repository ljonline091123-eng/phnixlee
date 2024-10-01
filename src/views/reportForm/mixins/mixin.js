import { mapGetters } from 'vuex'
import {listDept} from "@/api/system/dept";

export  const mixin = {
  data() {
    return {
      queryParams:{},
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
      params.startTime = params.dateRange && params.dateRange[0]
      params.endTime =  params.dateRange && params.dateRange[1]
      this.queryParams.assign({}, this.queryParams, params)
      this.getList(this.queryParams)
    },
  },
  computed:{
    ...mapGetters(['project']),
  },
  watch:{
    project:{
      handler(newVal,oldVal){
        if(oldVal === undefined || newVal.id !== oldVal.id){
          this.queryParams.projectCode = newVal.code
          this.getList(this.queryParams)
        }
      },
      immediate: true
    }
  }

}
