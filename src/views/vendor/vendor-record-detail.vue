<template>
  <div class="app-container">
    <!--<BackButton path="/vendor/vendor-base" title="供应商合作记录详情"/>-->
    <BackButton path="/vendor/vendor-base" :title="titleMy"/>
    <div class="context">
      <el-radio-group v-model="queryParams.expenditureBusinessType" size="small" style="padding: 15px 0;">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button :label="dict.value" :name="dict.value" v-for="dict in dict.type.procurement_plan_type"
                         :key="dict.value"
        >{{ dict.label }}
        </el-radio-button>
      </el-radio-group>

      <el-table v-loading="vendorLoading" :data="vendorList"
                highlight-current-row
                border
                stripe
                row-key="id"
                default-expand-all
                :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
      >
        <el-table-column label="合作单位" min-width="200" prop="cooperativePartnerName" show-overflow-tooltip/>
        <el-table-column label="供应商名称" min-width="250" align="center" prop="vendorName"/>
        <el-table-column label="合同名称" min-width="250" prop="agreementName" show-overflow-tooltip/>
        <el-table-column label="履约评价（优）" min-width="120" align="center" prop="excellentNum"/>
        <el-table-column label="履约评价（良）" min-width="120" align="center" prop="goodNum"/>
        <el-table-column label="履约评价（合格）" min-width="140" align="center" prop="qualifiedNum"/>
        <el-table-column label="履约评价（差）" min-width="120" align="center" prop="badNum"/>
        <el-table-column label="合同金额(元)" min-width="140" align="right" prop="totalAmountIncTaxText"/>
        <el-table-column label="已结算金额(元)" min-width="140" align="right" prop="settledAmountText"/>
        <el-table-column label="已付款金额(元)" min-width="140" align="right" prop="paidAmountText"/>
        <el-table-column label="未付款金额(元)" min-width="140" align="right" prop="unpaidAmountText"/>
        <el-table-column label="合同签订日期" min-width="140" align="center" prop="agreementSignDate"/>
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
    this.queryParams.vendorId = param
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
    }
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
