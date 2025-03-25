<template>
  <div class="app-container">
    <!--<BackButton path="/vendor/vendor-base" title="供应商合作记录详情"/>-->
    <BackButton :path="path" :title="titleMy"/>
    <div class="context">
      <el-radio-group v-model="queryParams.expenditureBusinessType" size="small" style="padding: 15px 0;">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button :label="dict.value" :name="dict.value" v-for="dict in dict.type.procurement_plan_type"
                         :key="dict.value"
        >{{ dict.label }}
        </el-radio-button>

      </el-radio-group>
      <div style="float: right;padding: 15px 0px;">单位：元</div>
      <el-table v-loading="vendorLoading" :data="vendorList"
                highlight-current-row
                border
                stripe
                row-key="id"
                default-expand-all
                :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
      >
        <el-table-column label="合作单位" min-width="200" prop="cooperativePartnerName" show-overflow-tooltip/>
        <!--<el-table-column label="供应商名称" min-width="250" align="center" prop="vendorName"/>-->
        <!--<el-table-column label="合同名称" min-width="250" prop="agreementName" show-overflow-tooltip/>-->
        <el-table-column label="合同名称" min-width="250"  prop="agreementName" show-overflow-tooltip>
          <template slot-scope="scope" >
            <a v-if="scope.row.children == undefined"
              class="link-type"
              @click="goDetail(scope.row.agreementId, scope.row.expenditureBusinessType)"
            >
              {{ scope.row.agreementName }}
            </a>
            <span style="display: inline-block; text-align: center; width: 250px;" v-else>{{ scope.row.childrenNum }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合同签订日期" min-width="140" align="center" prop="agreementSignDate"/>
        <el-table-column label="合同金额" min-width="140" align="right" prop="totalAmountIncTaxText"/>
        <el-table-column label="支出业务类型" min-width="140" align="right" prop="expenditureBusinessTypeText"/>
        <el-table-column label="甲方名称" min-width="140" align="right" prop="partyAName"/>
        <el-table-column label="甲方联系人" min-width="140" align="right" prop="partyAContactName"/>
        <el-table-column label="甲方联系人电话" min-width="140" align="right" prop="partyAContactPhone"/>
        <!-- <el-table-column label="已结算金额" min-width="140" align="right" prop="settledAmountText"/>
        <el-table-column label="已付款金额" min-width="140" align="right" prop="paidAmountText"/>
        <el-table-column label="未付款金额" min-width="140" align="right" prop="unpaidAmountText"/>
        <el-table-column label="履约评价（优）" min-width="120" align="center" prop="excellentNum"/>
        <el-table-column label="履约评价（良）" min-width="120" align="center" prop="goodNum"/>
        <el-table-column label="履约评价（合格）" min-width="140" align="center" prop="qualifiedNum"/>
        <el-table-column label="履约评价（差）" min-width="120" align="center" prop="badNum"/> -->
      </el-table>
    </div>
  </div>
</template>

<script>
import { Base64 } from 'js-base64'
import { getCooperationList,getVendorDetail } from '@/api/vendor/vendor'
import BackButton from '@/components/BackButton/index.vue'

export default {
  name: 'vendor-record-detail',
  dicts: ['procurement_plan_type'],
  data() {
    return {
      vendorLoading: false,
      vendorList: [],
      titleMy: '供应商合作记录详情',
      path:'/vendor/vendor-base',
      // 查询参数
      queryParams: {
        expenditureBusinessType: 'all',
        vendorId: ''
      }
    }
  },
  components: {
    BackButton
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params))
    if(param?.id){
      this.queryParams.vendorId = param.id;
      this.vendorClass=param.vendorClass;
      this.path=this.path+'?vendorClass='+this.vendorClass;
    }else if(param?.vendorId){
      this.queryParams.vendorId = param.vendorId;
      this.vendorClass=param.vendorClass;
      this.path=this.path+'?vendorClass='+this.vendorClass;
    }else {
      this.queryParams.vendorId = param;
    }
    this.getCooperationList()
  },
  methods: {
    /** 查询采购计划列表 */
    async getCooperationList() {
      this.vendorLoading = true
      try {
        const query = {
          ...this.queryParams,
          expenditureBusinessType: this.queryParams.expenditureBusinessType === 'all' ? undefined : this.queryParams.expenditureBusinessType
        }
        const res2 = await getVendorDetail(this.queryParams.vendorId)
        this.titleMy = '供应商合作记录详情-'+res2.data.vendor.enterpriseName;
        console.log(query, 'qqqqqqqqq')
        const res = await getCooperationList(query)
        this.vendorLoading = false
        console.log(res, '详情~~~')
        this.vendorList = res.data
      } catch (err) {
        this.vendorLoading = false
        console.log(err)
      }
    },
    goDetail(id, type) {
      let myPath = this.$route.path;
      let param = Base64.encode(JSON.stringify({ id, type ,myPath}));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/contract-detail/${param}`);
    },
  },
  watch: {
    /** 监控类型切换 */
    'queryParams.expenditureBusinessType': {
      handler(val) {
        this.getCooperationList()
      }
    }
  }
}
</script>
