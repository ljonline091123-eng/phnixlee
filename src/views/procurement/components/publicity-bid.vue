<template>
  <div class="app-container">
    <div class="context">
      <!-- 基本信息 -->
      <PageTitle title="基本信息">
        <div class="page-title-right">
          <el-button type="primary" size="small" @click="preview"
            >预览</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="submitForm"
            :disabled="
              isSubmit ||
              (noticeDetail.tenderNotice &&
                (noticeDetail.tenderNotice.noticeStatus !== 6 ||
                  !noticeDetail.purchaseOfficer))
            "
            :loading="isSubmit"
            >{{ isSubmit ? "提交中..." : "提交" }}</el-button
          >
        </div>
      </PageTitle>
      <el-form :model="scheme" label-width="110px" class="form-body">
        <el-row>
          <el-col :span="8">
            <el-form-item label="任务名称：">
              <span>{{ scheme.procurementSchemeName }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="任务编号：">
              <span>{{ scheme.procurementSchemeCode }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="8">
            <el-form-item label="采购方式：">
              <span>{{ scheme.procurementTypeText }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="招标经办人：">
              <span>{{
                noticeDetail.tenderNotice && noticeDetail.tenderNotice.contact
              }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话：">
              <span>{{
                noticeDetail.tenderNotice && noticeDetail.tenderNotice.phone
              }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="24">
            <el-form
              :model="formData"
              ref="form"
              :rules="rules"
              label-position="right"
              label-width="110px"
              size="medium"
              @submit.native.prevent
            >
              <el-form-item
                label="公示期："
                prop="publicityTime"
                label-width="110px"
              >
                <el-date-picker
                  v-model="formData.publicityTime"
                  :disabled="isSubmit || isTime"
                  :picker-options="pickerOptions"
                  value-format="yyyy-MM-dd"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                >
                </el-date-picker>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
      </el-form>

      <PageTitle title="侯选单位" marginBottom="15px" />
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
        <el-table-column
          prop="sureBid"
          label="中标结果"
          align="center"
        >
          <template #default="{ row }">
            <span :style="{ color: row.bidResult === 1 ? 'red' : 'black' }">
              {{ row.bidResult === 1 ? '中标' : row.bidResult === 0 ? '未中标' : ''}}
            </span>
          </template>
        </el-table-column>
      </el-table>
      <!-- 选择项目合约规划 -->
      <el-dialog
        title="中标公示详情"
        :visible.sync="previewVisible"
        width="50%"
      >
        <div class="title">{{ scheme.procurementSchemeName }}中标公示</div>
        <el-descriptions class="form-body" :column="2">
          <el-descriptions-item label="任务名称">{{
            scheme.procurementSchemeName
          }}</el-descriptions-item>
          <el-descriptions-item label="任务编号">{{
            scheme.procurementSchemeCode
          }}</el-descriptions-item>
          <el-descriptions-item label="采购方式">{{
            scheme.procurementTypeText
          }}</el-descriptions-item>
          <el-descriptions-item label="招标经办人">{{
            noticeDetail.tenderNotice && noticeDetail.tenderNotice.contact
          }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{
            noticeDetail.tenderNotice && noticeDetail.tenderNotice.phone
          }}</el-descriptions-item>
        </el-descriptions>
        <!-- <div class="subTitle"></div> -->
        <PageTitle title="招标文件内容" marginBottom="15px" marginTop="15px" />
        <a
          class="link-type"
          :href="scheme.biddingTemplate && scheme.biddingTemplate.fileUrl"
          target="_blank"
          >{{ scheme.biddingTemplate && scheme.biddingTemplate.fileName }}</a
        >
        <PageTitle title="公示期" marginBottom="15px" marginTop="15px" />
        <div>{{ publicityStartTime || formData.publicityTime && formData.publicityTime[0] }} 至 {{ publicityEndTime || formData.publicityTime && formData.publicityTime[1] }}</div>
        <PageTitle title="候选单位" marginBottom="15px" marginTop="15px" />
        <el-table
          size="small"
          :data="evaluateList"
          v-loading="setBidLoading"
          border
          stripe
        >
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column label="候选单位名称" prop="vendorName" />
          <el-table-column
            label="中标候选人名次"
            align="center"
            prop="candidate"
          />
        </el-table>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { getBiddingResult, calibrationRelease } from "@/api/procurement/manage";
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
      previewVisible: false,
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
      isTime: false,
      publicityStartTime: "",
      publicityEndTime: "",
    };
  },
  created() {
    this.getBiddingResult();
    console.log(this.noticeDetail, "noticeDetail");
    console.log(this.scheme, "scheme");
  },
  methods: {
    async getBiddingResult() {
      const { id } = this.noticeDetail?.tenderNotice || {};
      this.setBidLoading = true;
      try {
        const res = await getBiddingResult(id);
        console.log(res, "res~~~~~~~~~~~~~");
        this.evaluateList = res.data;
        let isTime = res.data[0].publicityStartTime ? true : false;
        // console.log(isTime,'isTime');
        // this.$set(this.formData, 'publicityTime', [new Date(res.data[0].publicityStartTime), new Date(res.data[0].publicityEndTime)])
        // this.publicityStartTime = res.data[0].publicityStartTime
        // this.publicityEndTime = res.data[0].publicityEndTime
        // console.log(isTime,'isTime');
        if (isTime) {
          this.$set(this.formData, "publicityTime", [
            new Date(res.data[0].publicityStartTime),
            new Date(res.data[0].publicityEndTime),
          ]);
          this.publicityStartTime = res.data[0].publicityStartTime;
          this.publicityEndTime = res.data[0].publicityEndTime;
          this.isTime = true;
        } else {
          this.isTime = false;
        }
      } catch (err) {
        console.log(err);
      }
      this.setBidLoading = false;
    },
    async submitForm() {
      this.isSubmit = true;
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          const { id } = this.noticeDetail?.tenderNotice || {};
          const { publicityTime } = this.formData;
          const formData = {
            noticeId: id,
            publicityStartTime: publicityTime[0],
            publicityEndTime: publicityTime[1],
          };
          console.log(formData, "formData");
          try {
            const res = await calibrationRelease(formData);
            this.$message.success("公示成功");
            this.$emit("changeState", 5);
          } catch (err) {
            console.log(err);
          }
          this.isSubmit = false;
        } else {
          this.isSubmit = false;
        }
      });
    },
    preview() {
      if(!this.formData.publicityTime || !this.formData.publicityTime.length) return this.$message.error("请先选择公示时间")
      this.previewVisible = true;
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
  text-align: center;
  font-size: 18px;
  line-height: 40px;
  font-weight: bold;
}
.subTitle {
  line-height: 40px;
  font-size: 16;
  font-weight: bold;
}
</style>
