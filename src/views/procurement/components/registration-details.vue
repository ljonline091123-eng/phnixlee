<template>
  <div class="app-container">
    <div class="context">
      <PageTitle title="报名供应商" marginBottom="15px">
        <template #leftSuffix>
          <span style="margin-left: 16px;color: #ff0000;font-size: 13px;" v-if="(noticeDetail.tenderNotice && noticeDetail.tenderNotice.noticeStatus === 12)">{{timeDifferenceElement}}</span>
        </template>
        <div class="page-title-right">
          <el-button
            type="primary"
            size="small"
            :disabled="
              (noticeDetail.tenderNotice &&
                noticeDetail.tenderNotice.noticeStatus !== 12) ||
              !noticeDetail.purchaseOfficer
            "
            @click="next"
          >进入下一环节</el-button
          >
        </div>
      </PageTitle>
      <el-table
        size="small"
        :data="backBidList"
        border
        @selection-change="handleSelectionChange"
        stripe
      >
        <!-- <el-table-column
          type="selection"
          width="55"
          :selectable="disabledHandle"
        /> -->
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="供应商名称"
          prop="vendorName"
          show-overflow-tooltip
        >
          <template slot-scope="{ row }">
            <a
              class="link-type"
              href="javascript:;"
              @click="getVendorDetail(row.vendorId)"
            >{{ row.vendorName }}</a
            >
          </template>
        </el-table-column>
        <el-table-column
          label="联系人"
          align="center"
          prop="contact"
          width="100"
        />
        <el-table-column
          label="联系电话"
          align="center"
          prop="phone"
          width="120"
        />
        <el-table-column
          label="报名时间"
          align="center"
          prop="createTime"
          width="180"
        />
      </el-table>
      <!-- 供应商详情 -->
      <el-dialog
        title="供应商信息"
        :visible.sync="vendorDetailVisiable"
        width="50%"
      >
        <el-tabs v-model="activeName" @tab-click="handleClick">
          <el-tab-pane label="基本信息" name="baseInfo">
            <el-form label-width="150px" class="form-body" label-suffix=":">
              <el-row :gutter="40">
                <el-col :span="12">
                  <el-form-item label="供应商名称" class="custom-form-item">
                    <span>{{ vendor.enterpriseName }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                    label="统一社会信用代码"
                    class="custom-form-item"
                  >
                    <span>{{ vendor.socialCreditCode }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="注册资金(万元)" class="custom-form-item">
                    <span>{{ vendor.registeredCapitalPattern }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="币种" class="custom-form-item">
                    <span>{{ vendor.currencyCodeText }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="开户支行" class="custom-form-item">
                    <span>{{ vendor.accountBranch }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="银行账号" class="custom-form-item">
                    <span>{{ vendor.bankAccount }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="法定代表人" class="custom-form-item">
                    <span>{{ vendor.legalRepresentative }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="企业分类" class="custom-form-item">
                    <span>{{ vendor.legalPhone }}</span>
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item
                    label="增值税纳税人类型"
                    class="custom-form-item"
                  >
                    <span>{{ vendor.taxpayerTypeText }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="发票类型" class="custom-form-item">
                    <span>{{ vendor.invoiceTypeText }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="企业性质" class="custom-form-item">
                    <span>{{ vendor.enterpriseNatureText }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="企业所在省/市" class="custom-form-item">
                    <span>{{ vendor.enterpriseProvinceName }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="企业所在地市" class="custom-form-item">
                    <span>{{ vendor.enterpriseCityName }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="详细地址" class="custom-form-item">
                    <span>{{ vendor.enterpriseAddress }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="企业联系电话" class="custom-form-item">
                    <span>{{ vendor.contactPhone }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="经营范围" class="custom-form-item">
                    <span>{{ vendor.businessScope }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="企业简介" class="custom-form-item">
                    <span>{{ vendor.enterpriseProfile }}</span>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>

            <el-divider></el-divider>

            <el-form label-width="150px" class="form-body" label-suffix=":">
              <el-row :gutter="40">
                <el-col :span="12">
                  <el-form-item label="联系人" class="custom-form-item">
                    <span>{{ mainContact.contactName }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系人身份证" class="custom-form-item">
                    <span>{{ mainContact.contactIdCard }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系人手机" class="custom-form-item">
                    <span>{{ mainContact.contactPhone }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系人邮箱" class="custom-form-item">
                    <span>{{ mainContact.contactEmail }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item
                    label="联系人是否为法人"
                    class="custom-form-item"
                  >
                    <span>{{ mainContact.isLegalText }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="首次合作单位" class="custom-form-item">
                    <span>{{ vendorState.firstCooperationCompanyName }}</span>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="资质材料" name="aptitude">
            <div v-for="(item, i) in companyAptitude" :key="i">
              <div class="aptitued-title">{{ item.title }}</div>
              <div class="aptitued-img">
                <el-row :gutter="20">
                  <el-col :span="6" v-for="(subItem, k) in item.list" :key="k">
                    <el-image
                      style="width: 230px; height: 100px"
                      fit="cover"
                      :src="subItem.attachmentFileUrl"
                      :preview-src-list="item.srcList"
                    >
                    </el-image>
                  </el-col>
                </el-row>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import {applyNext,} from "@/api/procurement/manage";
import {getVendorDetail} from "@/api/vendor/vendor";
import PageTitle from "@/components/PageTitle/index.vue";

export default {
  name: "back-bid",
  props: {
    noticeDetail: {
      type: Object,
      default: () => {},
    },
    scheme: {
      type: Object,
      default: () => {},
    },
    changeState: {
      typeof: Function,
      default: () => {},
    },
    isDisabledDeposit: {
      type: Boolean,
      default: false,
    },
  },
  components: {
    PageTitle,
  },
  data() {
    return {
      tenderNotice: {},
      timer: null,
      timeDifferenceElement: '',
      backBidList: [],
      abandonBidVisiable: false,
      abandonBidForm: {},
      fileList: [],
      abandonBidRules: {
        reason: [{ required: true, message: "请填写废标原因" }],
        attachmentList: [{ required: true, message: "请上传附件" }],
      },
      ids: [],
      vendorDetailVisiable: false,
      vendor: {},
      vendorState: {},
      mainContact: {},
      contactList: {},
      activeName: "baseInfo",
      companyAptitude: [
        {
          title: "营业执照",
          code: "businessLicense",
          list: [], //营业执照
          srcList: [],
        },
        {
          title: "诚信合规",
          code: "integrity",
          list: [], //诚信合规
          srcList: [],
        },
        {
          title: "法人授权书",
          code: "legalAuthorizationList",
          list: [], //法人授权书
          srcList: [],
        },
        {
          title: "相关资质",
          code: "relevantCertificationList",
          list: [], //相关资质
          srcList: [],
        },
      ],
    };
  },
  created() {
    const { procurementSchemeName } = this.scheme;
    this.abandonBidForm.procurementSchemeName = procurementSchemeName;
    this.tenderNotice=this.noticeDetail?.tenderNotice || {}
    this.getBackList();
    this.abandonBidForm.operator = this.$store.state.user.nickname;
  },
  mounted() {
    if(this.tenderNotice.applyTimeNotice) {
      this.updateTimeDifference();
      // 每秒更新一次
      this.timer = setInterval(this.updateTimeDifference, 1000);
    }else {
      this.$message.error('截止时间取值有误，请联系管理人员！')
      this.$router.replace("/procurement/bindding");
    }
  },
  methods: {
    updateTimeDifference() {
      // 报名截止时间
      const deadline = new Date(this.tenderNotice.applyTimeNotice);
      const now = new Date();
      // 计算时间差
      const diff = deadline - now;
      if(diff<=0){
        this.timeDifferenceElement = `距离报名截止时间还有：00天00小时00分00秒！`;
        if(this.timer) {
          clearInterval(this.timer);
        }
        return
      }

      // 计算天数、小时数、分钟数和秒数
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      // 格式化并显示结果
      this.timeDifferenceElement = `距离报名截止时间还有：${days<10?'0'+days:days}天${hours<10?'0'+hours:hours}小时${minutes<10?'0'+minutes:minutes}分${seconds<10?'0'+seconds:seconds}秒！`;
    },
    /** 获取回标列表 */
    async getBackList() {
      this.backBidList = this.noticeDetail?.tenderApplyList || []
    },
    closeDialog(formName) {
      this.$refs[formName].resetFields();
    },
    /** 已选择计划 */
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.vendorId);
    },
    /** 查看供应商详情 */
    async getVendorDetail(id) {
      this.vendorDetailVisiable = true;
      const res = await getVendorDetail(id);
      console.log(res.data, "供应商详情");
      const { certificationList } = res.data;
      this.vendor = res.data.vendor;
      this.vendorState = res.data.vendorState;
      this.mainContact = res.data.mainContact;
      this.contactList = res.data.certificationList;
      this.companyAptitude.forEach((item) => {
        console.log(item.code, "oiiii");
        if (item.code === "businessLicense") {
          item.list = [certificationList.businessLicense];
          item.srcList = [certificationList.businessLicense.attachmentFileUrl];
        } else if (item.code === "integrity") {
          item.list = [certificationList.integrity];
          item.srcList = [certificationList.integrity.attachmentFileUrl];
        } else if (item.code === "legalAuthorizationList") {
          item.list = certificationList.legalAuthorizationList;
          item.srcList = certificationList.legalAuthorizationList.map(
            (item) => item.attachmentFileUrl
          );
        } else if (item.code === "relevantCertificationList") {
          item.list = certificationList.relevantCertificationList;
          item.srcList = certificationList.relevantCertificationList.map(
            (item) => item.attachmentFileUrl
          );
        }
      });
      console.log(
        this.companyAptitude,
        "this.companyAptitude-this.companyAptitude"
      );
    },
    disabledHandle(row) {
      return !(Number(row.biddingStatus) === 12)
    },
    /** 切换table */
    handleClick() {},
    next() {
      // * 判断是否满足进入下一环节的条件
      if (!this.backBidList.length) {
        return this.$message.error("暂不能进入下一环节");
      }
      if (this.ids.length ===0) {
        return this.$message.error("请选择供应商");
      }


      this.$confirm("确定要进入下一环节吗?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        this.intoBidOpeningStage();
      });
    },
    async intoBidOpeningStage() {
      try {
        const formData = {
          ...this.noticeDetail.tenderNotice,
          schemeId: this.scheme.id,
          vendorApplyIds: this.ids,
        };
        const res = await applyNext(formData);
        this.$message.success("操作成功");
        this.$emit("changeState", 13);
      } catch (err) {
        console.log(err);
      }
    },
  },
  beforeDestroy() {
    if(this.timer) {
      clearInterval(this.timer);
    }
  }
};
</script>
<style lang="scss" scoped>


.textColor {
  color: red;
}

.app-container {
  width: 100%;
  font-family: PingFang SC;
  background-color: #f2f2f8; //主体内容颜色配置
  padding: 0 !important;
}
.aptitued-title {
  width: 100%;
  line-height: 40px;
  font-size: 14px;
}
.aptitued-img {
  display: flex;
  margin: 0;
  padding: 0;
  li {
    list-style: none;
  }
}

.form-body {
  padding: 20px;
  font-size: 13px;
  &::v-deep .el-form-item {
    margin-bottom: 0;
  }
}
</style>
