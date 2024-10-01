<template>
  <div class="app-container">
    <BackButton path="/procurement/bindding" title="招标管理详情" />
    <el-col :span="20" :xs="24" v-loading="loadingDetail">
      <!-- 招标文件-->
      <TenderDocuments
        :noticeDetail="noticeDetail"
        :scheme="scheme"
        v-if="currentState === 'file'"
        @changeState="changeState"
      />
      <!-- 回标情况 -->
      <BackBid
        :noticeDetail="noticeDetail"
        :scheme="scheme"
        :isDisabledDeposit="isDisabledDeposit"
        v-if="currentState === 'back'"
        @changeState="changeState"
      />
      <!-- 评标 -->
      <EvaluateBid
        :noticeDetail="noticeDetail"
        :scheme="scheme"
        v-if="currentState === 'evaluate'"
        @changeState="changeState"
      />
      <!-- 定标 -->
      <DefineBid
        :noticeDetail="noticeDetail"
        :scheme="scheme"
        :isShowButton="isShowButton"
        :tenantId="tenantId"
        :authorityId="authorityId"
        :rejectNodeList="rejectNodeList"
        :taskPresentId="taskPresentId"
        :isShowApprovalDetails="isShowApprovalDetails"
        :attachmentDetails="attachmentDetails"
        v-if="currentState === 'define'"
        @changeState="changeState"
        @submitSuccess="handleSubmitSuccess"
      />
      <!-- 中标公示 -->
      <PublicityBid
        :noticeDetail="noticeDetail"
        :scheme="scheme"
        v-if="currentState === 'publicity'"
        @changeState="changeState"
      />
      <!-- 结果公示 -->
      <ResultBid
        :noticeDetail="noticeDetail"
        :scheme="scheme"
        v-if="currentState === 'result'"
        @changeState="changeState"
      />
    </el-col>

    <el-col :span="4" :xs="24" v-if="currentState">
      <div
        class="context"
        style="height: calc(100vh - 116px); margin-left: 15px"
      >
        <div class="bidding-process">
          <el-steps
            class="step_item"
            direction="vertical"
            :active="currentStep"
            style="padding-top: 10px"
          >
            <el-step
              v-for="(item, index) in stepList[scheme.procurementType]"
              :key="index"
            >
              <template #title>
                <a href="javascript:;" @click="goStep(item.value)">{{
                  item.label
                }}</a>
              </template>
            </el-step>
          </el-steps>
        </div>
      </div>
    </el-col>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import { mapGetters } from "vuex";
import { getNoticeDetail, getPermissionButton } from "@/api/procurement/manage";
import TenderDocuments from "./components/tender-documents.vue";
import BackBid from "./components/back-bid.vue";
import EvaluateBid from "./components/evaluate-bid.vue";
import DefineBid from "./components/define-bid.vue";
import PublicityBid from "./components/publicity-bid.vue";
import ResultBid from "./components/result-bid.vue";
import BackButton from "@/components/BackButton/index.vue";
export default {
  components: {
    TenderDocuments,
    BackBid,
    EvaluateBid,
    DefineBid,
    PublicityBid,
    ResultBid,
    BackButton,
  },
  data() {
    return {
      noticeDetail: {},
      scheme: {},
      currentStep: 0,
      currentState: "",
      stepList: {
        1: [
          { label: "招标文件", value: 0 },
          { label: "回标情况", value: 1 },
          { label: "评标", value: 2 },
          { label: "定标", value: 3 },
          { label: "中标公示", value: 4 },
          { label: "结果发布", value: 5 },
        ],
        2: [
          { label: "招标文件", value: 0 },
          { label: "回标情况", value: 1 },
          { label: "评标", value: 2 },
          { label: "定标", value: 3 },
          { label: "结果发布", value: 4 },
        ],
        3: [
          { label: "招标文件", value: 0 },
          { label: "回标情况", value: 1 },
          { label: "评标", value: 2 },
          { label: "定标", value: 3 },
          { label: "结果发布", value: 4 },
        ],
        4: [
          { label: "招标文件", value: 0 },
          { label: "回标情况", value: 1 },
          { label: "定标", value: 2 },
          { label: "结果发布", value: 3 },
        ],
      },
      loadingDetail: false,
      tenantId: "1",
      authorityId: "1",
      isShowButton: false,
      rejectNodeList: [],
      taskPresentId: "1",
      isShowApprovalDetails: false,
      isDisabledDeposit: false,
      attachmentDetails: [],
    };
  },
  created() {
    const param = JSON.parse(
      Base64.decode(decodeURIComponent(this.$route.params.params))
    );
    this.scheme = param;
    console.log(this.scheme, "this.scheme.noticeStatus");
    // let status = param.noticeStatus || 0
    // let step = 0
    // if(status === 1){
    //   step = 1
    // }
    if (this.scheme.noticeStatus !== 0) {
      this.getNoticeDetail();
    } else {
      this.goStep(0);
    }
  },
  methods: {
    async getNoticeDetail() {
      this.loadingDetail = true;
      const { id, noticeId, procurementType, noticeStatus } = this.scheme;
      try {
        const res = await getNoticeDetail(
          id,
          noticeStatus === 0 ? undefined : noticeId
        );
        this.tenantId = res.data?.tenderNotice?.id;
        this.authorityId = res.data?.tenderNotice?.wfProcessId;
        this.attachmentDetails = res.data?.calibrationAttachmentList || [];
        this.isDisabledDeposit =
          res.data?.financeConfirmUser === true ? false : true;
        this.isShowApprovalDetails = this.authorityId ? true : false;
        if (
          res.data?.tenderNotice?.noticeStatus === 5 &&
          this.tenantId &&
          this.authorityId
        ) {
          const response = await getPermissionButton({
            businessId: this.tenantId,
            processId: this.authorityId,
          });
          this.isShowButton = response.data.auditable;
          this.rejectNodeList = response.data.completedTaskList;
          this.taskPresentId = response.data.curTaskId;
        }
        this.noticeDetail = res.data || {};
        let status = this.noticeDetail.tenderNotice?.noticeStatus || 0;
        let step = 0;
        if (status === 1) {
          step = 1;
        } else if (status === 2 || status === 3 || status === 4) {
          step = 2;
        } else if (status === 5) {
          if (procurementType === 4) {
            step = 2;
          } else {
            step = 3;
          }
        } else if (status === 6) {
          step = 4;
        } else if (status === 7 || status === 8) {
          if (procurementType === 4) {
            step = 3;
          } else {
            step = 5;
          }
        }
        this.goStep(step);
        // if(step) {
        //   this.currentStep = step
        //   this.goStep(step)
        // }
        console.log(this.noticeDetail, "公告详情");
      } catch (err) {
        console.log(err);
      }
      this.loadingDetail = false;
    },
    goStep(steps) {
      console.log(steps, "步骤111111111111");
      //noticeStatus 4 二次洽商
      //noticeStatus 5 定标报告
      //noticeStatus 6 中标公示
      //noticeStatus 7  结果发布
      //noticeStatus 8  完成
      const noticeStatus = this.noticeDetail?.tenderNotice?.noticeStatus || 0;
      const { procurementType } = this.scheme;
      if (steps === 0) {
        this.currentState = "file";
        this.currentStep = steps;
      } else if (steps === 1) {
        console.log(noticeStatus, "noticeStatus");
        if (noticeStatus < 1) return; //noticeStatus 1 发布
        this.currentState = "back";
        this.currentStep = steps;
      } else if (steps === 2) {
        if (noticeStatus < 2) return; //noticeStatus 2 开标
        if (procurementType === 4) {
          this.currentState = "define";
        } else {
          this.currentState = "evaluate";
        }
        this.currentStep = steps;
      } else if (steps === 3) {
        if (procurementType === 4 && noticeStatus < 6) return;
        if (noticeStatus < 5) return; //noticeStatus 3 评标
        if (procurementType === 4) {
          this.currentState = "result";
        } else {
          this.currentState = "define";
        }
        this.currentStep = steps;
      } else if (steps === 4) {
        if (noticeStatus < 6) return;
        if (procurementType === 2 || procurementType === 3) {
          this.currentState = "result";
        } else {
          this.currentState = "publicity";
        }
        this.currentStep = steps;
      } else if (steps === 5) {
        if (noticeStatus < 7) return;
        this.currentState = "result";
        if (procurementType === 2 || procurementType === 3) {
          this.currentStep = 4;
        } else if (procurementType === 4) {
          this.currentStep = 3;
        } else {
          this.currentStep = steps;
        }
      }
    },
    changeState(step) {
      this.getNoticeDetail(step);
    },
    handleSubmitSuccess() {
      this.getNoticeDetail();
    },
  },
  computed: {
    ...mapGetters(["project"]),
  },
  watch: {
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.$router.replace("/procurement/bindding");
        }
      },
    },
  },
};
</script>
<style scoped>
.bidding-process {
  height: 400px;
  padding-left: 30px;
}
.step_item {
  font-size: 14px;
  padding-bottom: 16px;
}
::v-deep .step_item .el-step__title.is-finish {
  color: #2b4acb !important;
}
::v-deep .step_item .el-step__description.is-finish {
  color: #2b4acb !important;
}
::v-deep .step_item .el-step__head.is-finish {
  color: #2b4acb;
  border-color: #2b4acb;
}
</style>
