<template>
  <div class="app-container">
    <div class="context" style="height: calc(100vh - 116px)">
      <PageTitle title="中标结果" marginBottom="15px">
        <div class="page-title-right">
          <el-button type="primary" size="small" @click="preview"
            >预览</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="submitForm"
            :disabled="isSubmit || isOver || !noticeDetail.purchaseOfficer"
            :loading="isSubmit"
            >{{ isSubmit ? "发布中..." : "发布" }}</el-button
          >
        </div>
      </PageTitle>
      <el-table
        size="small"
        :data="evaluateList"
        v-loading="setBidLoading"
        border
        stripe
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="候选单位名称"
          prop="vendorName"
          show-overflow-tooltip
        />
        <el-table-column
          label="中标候选人名次"
          align="center"
          prop="candidate"
        />
        <el-table-column label="联系人" align="center" prop="contact" />
        <el-table-column label="联系电话" align="center" prop="phone" />
        <el-table-column label="中标结果" align="center" prop="bidResultText">
          <template slot-scope="{ row }">
            <span style="color: red" v-if="row.bidResultText === '中标'">{{
              row.bidResultText
            }}</span>
            <span v-else>{{ row.bidResultText }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果通知书" align="center">
          <template slot-scope="{ row }" v-if="Number(row.bidResult) === 1">
            {{
              row.sendNotified && Number(row.sendNotified) === 1
                ? "已发送中标通知书"
                : "未发送中标通知书"
            }}
          </template>
        </el-table-column>
      </el-table>
      <!-- 选择项目合约规划 -->
      <el-dialog title="中标结果发布" :visible.sync="msgVisible" width="50%">
        <!-- <div class="title">中标通知书</div>
        <div class="company-name">{{ company }}</div>
        <div style="text-indent: 4em;line-height: 24px;">{{ content }}</div>
        <div style="text-align: right;line-height: 10px;padding-top: 15px;">{{ ProjectDepartment }}</div>
        <div style="text-align: right;line-height: 24px;padding-top: 10px;">{{ ProjectDepartmentSon }}</div>
        <div style="text-align: right;line-height: 24px;">{{ DateText }}</div> -->
        <div class="notice-box">
          <div class="title">中标通知书</div>
          <div class="notice-num">
            中标编号：{{ scheme.procurementSchemeCode }}
          </div>
          <div class="notice-content">
            <div class="company-title">{{ company }}：</div>
            <div class="notice-text">
              <p>
                贵司于{{ bidTime }}所递交的<span class="text-decoration">{{
                  scheme.procurementSchemeName
                }}</span
                >项目的投标文件，经过评标小组认真评定，已确定为第一中标候选人。
              </p>
              <p>中标金额(元)：{{ taxPricePattern }}</p>
              <p>
                请贵单位在收到本通知书后7
                个工作日内，按招标文件要求与我司办理合同签订事宜。
              </p>
              <p>
                联系人：<span
                  class="text-decoration"
                  v-if="noticeDetail.tenderNotice"
                  >{{ noticeDetail.tenderNotice.contact }}</span
                ><span style="padding-left: 50px">联系电话：</span
                ><span class="text-decoration">{{
                  noticeDetail.tenderNotice.phone
                }}</span>
              </p>
              <p>特此通知</p>
              <p class="signOff">
                {{ $store.state.user.userInfo.thridOrgName }}
              </p>
              <!-- <p class="signOff">深圳市南山区桃源街道珠光村城市更新单元一期A项目项目经理部</p> -->
              <p class="signOff">{{ notifiTime || DateText }}</p>
            </div>
          </div>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import {
  getWinningBidResult,
  winningBidResultRelease,
} from "@/api/procurement/manage";
import PageTitle from "@/components/PageTitle/index.vue";
export default {
  name: "define-bid",
  props: {
    noticeDetail: {
      type: Object,
      default: () => {},
    },
    scheme: {
      type: Object,
      default: () => {},
    },
  },
  components: {
    PageTitle,
  },
  data() {
    return {
      procurementScheme: {},
      isSubmit: false,
      evaluateList: [],
      setBidLoading: false,
      publicityTime: "",
      formData: {},
      rules: {
        publicityTime: [
          {
            required: true,
            message: "请选择公示期",
          },
        ],
      },
      pickerOptions: {
        disabledDate(time) {
          // 获取今天的日期，注意去掉时间部分，只保留年月日
          const today = new Date(new Date().toDateString());
          // 禁用今天之前的所有日期
          return time < today;
        },
      },
      msgVisible: false,
      company: "",
      content:
        "首先，非常感谢贵单位参与我们的投标活动。经过认真评审，我们很高兴地通知贵单位已成功中标，成为该项目的承包方。请贵单位在收到本通知后，于指定日期前与我们签订正式合同，并按合同约定履行相关义务。如有任何问题，请随时与我们联系。",
      ProjectDepartment: "   深圳市南山区桃源街道珠光村",
      ProjectDepartmentSon: "城市更新单元一期A项目项目经理部",
      isOver: false,
      notifiTime: "",
      taxPricePattern: "",
      bidTime: "",
    };
  },
  created() {
    this.getWinningBidResult();
    this.isOver =
      this.noticeDetail?.tenderNotice?.noticeStatus === 8 ? true : false;
    console.log(this.noticeDetail, "noticeDetail");
    console.log(this.scheme, "scheme");
  },
  computed: {
    DateText() {
      // 创建一个 Date 对象
      let currentDate = new Date();

      // 获取年、月、日信息
      let year = currentDate.getFullYear();
      let month = currentDate.getMonth() + 1; // 月份是从 0 开始计数的，所以要加1
      let day = currentDate.getDate();

      // 格式化月和日，确保它们始终是两位数
      month = month < 10 ? "0" + month : month;
      day = day < 10 ? "0" + day : day;

      // 拼接成需要的格式
      return year + "年" + month + "月" + day + "日";
    },
  },
  methods: {
    async getWinningBidResult() {
      const { id } = this.noticeDetail?.tenderNotice || {};
      this.setBidLoading = true;
      try {
        const res = await getWinningBidResult(id);
        this.evaluateList = res.data;
        const current = res.data.find((item) => item.bidResultText === "中标");
        this.company = current.vendorName;
        this.notifiTime = current.notifiTime;
        this.taxPricePattern = current.taxPricePattern;
        let [year, month, day] = current.bidTime.split(" ")[0].split("-");
        this.bidTime = `${year}年${month}月${day}日`;
      } catch (err) {
        console.log(err);
      }
      this.setBidLoading = false;
    },
    async submitForm() {
      this.isSubmit = true;
      const { id } = this.noticeDetail?.tenderNotice || {};
      const formData = {
        noticeId: id,
        notifiContent: `${this.company}-${this.content}-${this.ProjectDepartment}-${this.ProjectDepartmentSon}-%${this.$store.state.user.userInfo.thridOrgName}`,
      };
      console.log(formData, "formData");
      try {
        const res = await winningBidResultRelease(formData);
        this.$message.success("结果发布成功");
        this.isOver = true;
        this.getWinningBidResult();
        this.$emit("changeState", 6);
      } catch (err) {
        console.log(err);
      }
      this.isSubmit = false;
    },
    preview() {
      this.msgVisible = true;
    },
  },
};
</script>
<style lang="scss" scoped>
.page-title {
  width: 100%;
  border-bottom: solid 1px #ccc;
  padding: 10px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;

  font-size: 13px; /* 修改字体大小 */
  font-weight: bolder; /* 修改字体粗细 */
  color: #121735;
  &::before {
    content: "";
    width: 3px;
    height: 14px;
    background: #2b4acb;
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}

.form-body {
  padding: 20px;
}

.title {
  font-size: 18px;
  text-align: center;
  line-height: 60px;
}

.company-name {
  font-size: 16px;
  padding-bottom: 15px;
}
::v-deep .el-table--border .el-table__cell {
  border-right: none !important;
  border: none !important;
}
::v-deep .el-table--group,
.el-table--border {
  border: none !important;
}
::v-deep .el-table--border::after {
  width: 0px;
}
.app-container {
  width: 100%;
  font-family: PingFang SC;
  background-color: #f2f2f8; //主体内容颜色配置
  padding: 0 !important;
}
.notice-box {
  .title {
    font-size: 20px;
    font-weight: bold;
    line-height: 60px;
    width: 100%;
    text-align: center;
  }
  .notice-num {
    font-size: 18px;
    width: 100%;
    text-align: center;
    border-bottom: solid 3px red;
    padding-bottom: 20px;
  }
  .text-decoration {
    text-decoration-line: underline;
    text-decoration-color: #666;
    text-decoration-style: solid;
  }
  .company-title {
    font-size: 18px;
    line-height: 60px;
  }
  .notice-content {
    padding: 20px;
    font-size: 16px;
    p {
      padding: 0;
      text-indent: 2em;
      width: 100%;
      line-height: 30px;
      &.signOff {
        text-align: right;
      }
    }
  }
}
</style>
