<template>
  <div class="app-container">
    <div class="context">
      <PageTitle title="投标供应商" marginBottom="15px">
        <div class="page-title-right" style="flex: 1;display: flex;justify-content: flex-end">
          <!--          为1的时候展示这个截止时间-->
          <div v-if="noticeDetail.tenderNotice && noticeDetail.tenderNotice.noticeStatus === 1 && needTime"   style="flex: 1;display: flex;justify-content: center;margin-right: 8px;font-size: 13px;color:#ff0000;font-weight: normal">
            <div style="margin-right: 80px">
              <div style="margin-right: 8px">方案：{{ scheme && scheme.procurementSchemeName  }}</div>
              <!--                noticeDetail.tenderNotice && noticeDetail.tenderNotice.applyTime-->
              <div>当前北京时间：{{formatNowDate}}</div>
            </div>

            <div>
              <div>投标截止时间：{{formatApplyTime}}</div>
              <span>投标截止时间倒计时：{{timeDifferenceElement}}</span>
            </div>

          </div>
          <div>
            <el-button
              type="primary"
              size="small"
              :disabled="
              (noticeDetail.tenderNotice &&
                noticeDetail.tenderNotice.noticeStatus >= 2) ||
              !noticeDetail.purchaseOfficer
            "
              @click="next"
            >进入下一环节</el-button
            >
            <el-button
              type="primary"
              size="small"
              :disabled="
              !ids.length ||
              !noticeDetail.purchaseOfficer ||
              (noticeDetail.tenderNotice &&
                noticeDetail.tenderNotice.noticeStatus >= 2)
            "
              @click="goAbandonBid"
            >
              废标
            </el-button>
          </div>
        </div>
      </PageTitle>
      <el-table
        size="small"
        :data="backBidList"
        border
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column
          type="selection"
          width="55"
          :selectable="disabledHandle"
        />
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
          label="保证金"
          align="center"
          prop="collectDeposit"
          width="100"
        >
          <template slot-scope="{ row }">
            {{ row.collectDeposit !== 2 ? "收取" : "不收取" }}
          </template>
        </el-table-column>
        <el-table-column
          label="投标状态"
          align="center"
          prop="biddingStatusText"
          width="100"
        />
        <el-table-column
          label="投标时间"
          align="center"
          prop="updateTime"
          width="180"
        />
        <el-table-column
          label="操作IP"
          align="center"
          prop="ipAddress"
          width="180"
        >
          <template slot-scope="{ row }">
            <span :class="repeatHandle(row.ipAddress) ? 'textColor' : ''">{{
              row.ipAddress
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="是否已收取保证金"
          align="center"
          prop="collectDeposit"
          v-if="isCollectDeposit"
          width="200"
        >
          <template slot-scope="{ row }">
            <el-switch
              :value="
                row.collectDeposit !== 0 && row.collectDeposit !== 2
                  ? true
                  : false
              "
              active-text="是"
              inactive-text="否"
              @change="updateDeposit(row)"
              :disabled="
                ( noticeDetail.tenderNotice &&
                  noticeDetail.tenderNotice.noticeStatus >= 2 ) ||
                isDisabledDeposit
              "
            >
            </el-switch>
          </template>
        </el-table-column>
      </el-table>
      <!-- 废标弹出 -->
      <el-dialog
        title="废标"
        :visible.sync="abandonBidVisiable"
        width="40%"
        @closed="closeDialog('abandonBidRef')"
      >
        <el-form
          :model="abandonBidForm"
          ref="abandonBidRef"
          :rules="abandonBidRules"
          label-position="right"
          label-width="80px"
          size="mini"
        >
          <el-row>
            <el-col :span="12" class="grid-cell">
              <el-form-item
                label="任务名称"
                label-width="110px"
                prop="procurementSchemeName"
                class="label-right-align"
              >
                <el-input
                  v-model="abandonBidForm.procurementSchemeName"
                  type="text"
                  disabled
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12" class="grid-cell">
              <el-form-item
                label="经办人"
                label-width="110px"
                prop="operator"
                class="label-right-align"
              >
                <el-input
                  v-model="abandonBidForm.operator"
                  type="text"
                  disabled
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24" class="grid-cell">
              <el-form-item
                label="废标原因"
                label-width="110px"
                prop="reason"
                class="label-right-align"
              >
                <el-input
                  v-model="abandonBidForm.reason"
                  type="textarea"
                  clearable
                  rows="4"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12" class="grid-cell">
              <el-form-item
                label="附件"
                label-width="110px"
                prop="attachmentList"
                class="label-right-align"
              >
                <el-upload
                  class="upload-demo"
                  action="/dev-api/file/upload"
                  multiple
                  :limit="1"
                  :on-success="handleSuccess"
                >
                  <el-button size="small" type="primary">点击上传</el-button>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="confirmAbandonBid('abandonBidRef')"
            >确 定</el-button
          >
          <el-button @click="abandonBidVisiable = false">取 消</el-button>
        </div>
      </el-dialog>
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
                  <el-form-item label="企业名称" class="custom-form-item">
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
                  <el-form-item label="法人联系方式" class="custom-form-item">
                    <span>{{ vendor.legalPhone }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="法人身份证号码" class="custom-form-item">
                    <span>{{ vendor.legalIdCard }}</span>
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
                <el-col :span="12">
                  <el-form-item label="企业所在省/市" class="custom-form-item">
                    <span>{{ vendor.enterpriseProvinceName }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="企业所在地市" class="custom-form-item">
                    <span>{{ vendor.enterpriseCityName }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="详细地址" class="custom-form-item">
                    <span>{{ vendor.enterpriseAddress }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="企业联系电话" class="custom-form-item">
                    <span>{{ vendor.contactPhone }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="企业分类" class="custom-form-item">
                    <span>{{ vendor.enterpriseTypeText }}</span>
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
                <el-col :span="12">
                  <el-form-item
                    label="联系人是否为法人"
                    class="custom-form-item"
                  >
                    <span>{{ mainContact.isLegalText }}</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
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
import {
  getBackList,
  updateDeposit,
  abandonBid,
  intoBidOpeningStage, twiceBidFinish,
} from "@/api/procurement/manage";
import { getVendorDetail } from "@/api/vendor/vendor";
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
      // * 方案名称
      schemeName: '',
      formatApplyTime: '',
      formatNowDate: '',
      needTime: true,
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
  mounted() {
    if(this.noticeDetail?.tenderNotice?.noticeStatus === 1 && this.needTime) {
      if(this.noticeDetail?.tenderNotice?.applyTime) {
        // * 投标截止时间
        this.formatApplyTime = new Date(this.noticeDetail?.tenderNotice?.applyTime).format('yyyy年MM月dd日 HH:mm:ss')
        this.updateTimeDifference();
        // 每秒更新一次
        if(this.needTime) {
          this.timer = setInterval(this.updateTimeDifference, 1000);
        }
      }else {
        this.$message.error('截止时间取值有误，请联系管理人员！')
        this.$router.replace("/procurement/bindding");
      }
    }


  },
  created() {
    const { procurementSchemeName } = this.scheme;
    this.abandonBidForm.procurementSchemeName = procurementSchemeName;
    this.getBackList();
    this.abandonBidForm.operator = this.$store.state.user.nickname;
  },
  computed: {
    isCollectDeposit() {
      return this.backBidList[0]?.collectDeposit !== 2;
    },
  },
  methods: {
    async updateTimeDifference() {
      // 投标截止时间
      const deadline = new Date(this.noticeDetail?.tenderNotice?.applyTime);
      const now = new Date();
      // * 当前北京时间
      this.formatNowDate = new Date().format('yyyy年MM月dd日')
      // 计算时间差
      const diff = deadline - now;
      if(diff<=0){
        this.needTime = false;
        this.timeDifferenceElement = `00天00小时00分00秒！`;
        if(this.timer) {
          clearInterval(this.timer);
        }
        if(this.noticeDetail?.tenderNotice?.twiceQuotState === 1){
          const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
          try {
            if(this.noticeDetail.tenderNotice.noticeStatus === 3){
              const res = await twiceBidFinish(noticeId);
            }
          } catch (err) {
            console.log(err);
          }
        }
        return
      }

      // 计算天数、小时数、分钟数和秒数
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      // 格式化并显示结果
      this.timeDifferenceElement = `${days<10?'0'+days:days}天${hours<10?'0'+hours:hours}小时${minutes<10?'0'+minutes:minutes}分${seconds<10?'0'+seconds:seconds}秒`;
    },
    /** 获取回标列表 */
    async getBackList() {
      const { id: schemeId } = this.scheme;
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await getBackList({ schemeId, noticeId });
        this.backBidList = res.data;
        console.log(res, "rrr");
      } catch (err) {
        console.log(err);
      }
    },
    async updateDeposit(row) {
      this.$confirm("您确定要进行操作?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          const res = await updateDeposit({
            id: row.id,
            collectDeposit: Number(row.collectDeposit) === 1 ? 0 : 1,
          });
          this.$message({
            type: "success",
            message: "更新成功!",
          });
          this.getBackList();
          console.log(res, "收取保证金");
        } catch (err) {
          console.log(err);
        }
      });
    },
    /** 去废标 */
    goAbandonBid() {
      if (!this.ids.length) return this.$message.error("请选择要废标的供应商");
      this.abandonBidVisiable = true;
    },
    /** 废标 */
    // async abandonBid() {
    //   try {
    //     const res = abandonBid()
    //     console.log(res, 'res');
    //   } catch (err) {
    //     console.log(err);
    //   }
    // },
    /** 提交废标 */
    confirmAbandonBid(formName) {
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          console.log("验证");
          const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
          const { id: schemeId } = this.scheme;
          const { reason, attachmentList } = this.abandonBidForm;
          const { ids } = this;
          let formData = {
            noticeId,
            schemeId,
            reason,
            attachmentList,
            biddingInfoIds: ids,
          };
          try {
            const res = await abandonBid(formData);
            this.$message.success("废标成功");
            this.abandonBidVisiable = false;
            this.getBackList();
            console.log(res, "废标");
          } catch (err) {
            console.log(err);
          }
        }
      });
    },
    closeDialog(formName) {
      this.$refs[formName].resetFields();
    },
    handleSuccess(res) {
      const { url, name } = res.data;
      console.log(url, name, "a");
      this.abandonBidForm.attachmentList = [{ fileUrl: url, fileName: name }];
      this.$refs.abandonBidRef.clearValidate("attachmentList");
    },
    /** 已选择计划 */
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.id);
    },
    /** IP是否有相同 */
    repeatHandle(ip) {
      const fileList = this.backBidList.filter((item) => item.ipAddress === ip);
      return fileList.length > 1 ? true : false;
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
      return Number(row.biddingStatus) === 3 ? false : true;
    },
    /** 切换table */
    handleClick() {},
    next() {
      if (!this.backBidList.length)
        return this.$message.error("暂不能进入下一环节");
      this.$confirm("确定要进入下一环节吗?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        this.intoBidOpeningStage();
      });
    },
    async intoBidOpeningStage() {
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await intoBidOpeningStage(noticeId);
        this.$message.success("操作成功");
        this.$emit("changeState", 2);
      } catch (err) {
        console.log(err);
      }
    },
  },
};
</script>
<style lang="scss" scoped>
//.page-title {
//  width: 100%;
//  border-bottom: solid 1px #ccc;
//  padding: 10px;
//  position: relative;
//  display: flex;
//  justify-content: space-between;
//  align-items: center;
//  margin-bottom: 15px;
//  &::before {
//    content: "";
//    height: 20px;
//    width: 5px;
//    background-color: rgba(41, 65, 137, 1);
//    position: absolute;
//    left: 0;
//    top: 50%;
//    transform: translateY(-50%);
//  }
//}

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
