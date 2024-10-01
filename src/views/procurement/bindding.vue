<template>
  <div class="app-container">
    <div class="context flex flex-column">
      <el-radio-group
        v-model="queryParams.procurementType"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in dict.type.procurement_type"
          :key="dict.value"
          >{{ dict.label }}</el-radio-button
        >
      </el-radio-group>

      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
        label-width="68px"
      >
        <el-form-item label="编号" prop="schemeCode" label-width="50px">
          <el-input
            v-model="queryParams.procurementSchemeCode"
            placeholder="请输入编号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="方案名称" prop="schemeName">
          <el-input
            v-model="queryParams.procurementSchemeName"
            placeholder="请输入方案名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="采购人" prop="operator">
          <el-input
            v-model="queryParams.procurementOfficerName"
            placeholder="请输入采购人"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="招标进展" prop="projectName">
          <el-select
            v-model="queryParams.noticeStatus"
            placeholder="招标进展"
            clearable
            style="width: 240px"
          >
            <el-option
              v-for="dict in dict.type.bindding_step"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="el-icon-search"
            size="small"
            @click="handleQuery"
            >查询</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="repealHandle"
            :disabled="!currentBid.id || currentBid.noticeStatus == 0 || !isPurchaseOfficer"
            v-hasPermi="['procurement:bindding:repeal']"
            >废标</el-button
          >
          <el-button
            type="success"
            size="small"
            @click="againHandle"
            :disabled="!isPurchaseOfficer || currentBid.noticeStatus !== 0"
            v-hasPermi="['procurement:bindding:anew']"
            >重新招标</el-button
          >
        </el-form-item>
      </el-form>

      <!-- <div class="tabs-box">
          <el-tabs v-model="queryParams.procurementType" @tab-click="handleTypeClick">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane :label="dict.label" :name="dict.value" v-for="dict in dict.type.procurement_type" :key="dict.value"/>
          </el-tabs>
          <div class="tabs-box-right">
            <el-button type="primary" size="mini" @click="repealHandle" :disabled="!currentBid.id">废标</el-button>
            <el-button type="success" size="mini" @click="againHandle" :disabled="currentBid.noticeStatus === 0? false : true">重新招标</el-button>
          </div>
        </div> -->

      <el-table
        v-loading="loading"
        :data="schemeList"
        highlight-current-row
        border
        stripe
        @row-click="selectBidding"
      >
        <el-table-column label="" width="30" align="center">
          <template slot-scope="scope">
            <el-radio
              class="table_radio"
              v-model="currentBid.id"
              :label="scope.row.id"
            />
          </template>
        </el-table-column>
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="编号"
          min-width="200"
          align="center"
          prop="procurementSchemeCode"
        >
          <template slot-scope="scope">
            <a class="link-type" @click="goDetail(scope.row)">
              {{ scope.row.procurementSchemeCode }}
            </a>
          </template>
        </el-table-column>
        <el-table-column
          label="方案名称"
          min-width="300"
          prop="procurementSchemeName"
          show-overflow-tooltip
        />
        <el-table-column
          min-width="100"
          label="采购方式"
          align="center"
          prop="procurementTypeText"
        />
        <el-table-column
          min-width="100"
          label="采购需求类型"
          prop="procurementPlanTypeText"
        />
        <el-table-column
          min-width="100"
          label="采购人"
          align="center"
          prop="procurementOfficerName"
        />
        <el-table-column
          min-width="100"
          label="创建时间"
          align="center"
          prop="createTime"
        />
        <el-table-column
          min-width="100"
          label="招标进展"
          align="center"
          prop="noticeStatusText"
        />
      </el-table>
    </div>
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNumber"
      :limit.sync="queryParams.pageSize"
      @pagination="getBiddingSchemeList"
    />

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
        size="small"
      >
        <el-row>
          <el-col :span="12" class="grid-cell">
            <el-form-item
              label="方案名称"
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
                ref="uploadFild"
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
        <el-button
          type="primary"
          @click="confirmAbandonBid('abandonBidRef')"
          style="width: 100px"
          size="small"
          >确 定</el-button
        >
        <el-button
          @click="abandonBidVisiable = false"
          style="width: 100px"
          size="small"
          >取 消</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { Base64 } from "js-base64";
import { getBiddingSchemeList, abandonBidMore } from "@/api/procurement/manage";
export default {
  name: "Bindding",
  dicts: ["procurement_type", "bindding_step"],
  data() {
    return {
      schemeList: [],
      // 遮罩层
      loading: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        procurementSchemeName: undefined,
        procurementSchemeCode: undefined,
        procurementOfficer: undefined,
        procurementOfficerName: undefined,
        noticeStatus: undefined,
        procurementType: "all",
        projectCode: undefined,
        type: undefined,
        projectCodeList: undefined,
      },
      report:'',
      currentBid: {}, //当前选中条目
      /** 废标 */
      abandonBidVisiable: false,
      abandonBidForm: {},
      fileList: [],
      abandonBidRules: {
        reason: [{ required: true, message: "请填写废标原因" }],
        attachmentList: [{ required: true, message: "请上传附件" }],
      },
      isPurchaseOfficer: false,
      procurementSchemeName:'',
      noticeId:''
    };
  },
  created() {
    // this.queryParams = {...this.$route.query}
    // this.getBiddingSchemeList();
    this.queryParams.procurementType = this.$route.query.procurementType
    this.queryParams.projectCode = this.$route.query.projectCode
    this.queryParams.projectCodeList = this.$route.query.projectCodeList
    this.queryParams.type = this.$route.query.type
    this.queryParams.noticeStatus = this.$route.query.noticeStatus
    this.getDicts("plan_type").then((res) => {
      this.bindding_type = res.data;
    });
    if(this.$route.query.report){
      this.report=this.$route.query.report
    }
  },
  computed: {
    ...mapGetters(["project"]),
  },
  methods: {
    /** 获取需求列表 */
    async getBiddingSchemeList() {
      this.loading = true;
      const query = {
        ...this.queryParams,
        procurementType:
          this.queryParams.procurementType === "all"
            ? undefined
            : this.queryParams.procurementType,
      };
      try {
        const res = await getBiddingSchemeList(query);
        if (res.data) {
          //报表跳转过来时，项目请求数据不赋值
          if(this.report=="report" && this.schemeList.length == 0){
            this.schemeList = res.data.rows;
            this.total = res.data.total;
          }else if(this.report=="report" && this.schemeList.length > 0){
            this.report=""
          }else{
            this.schemeList = res.data.rows;
            this.total = res.data.total;
          }
        
        }
      } catch (err) {
        console.log(err);
      }
      this.loading = false;
    },
    //切换tab类型
    handleTypeClick(tab) {
      this.queryParams.procurementType = tab.name;
    },
    handleClick(tab, event) {
      console.log(tab, event, "aaa");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      console.log(this.queryParams, "this.queryParams");
      this.getBiddingSchemeList();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加任务";
    },
    /** 跳转方案详情 */
    goDetail(row) {
      console.log(row, "rrrr");
      let param = Base64.encode(JSON.stringify(row));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/tendering/${param}`);
    },
    /* 选择招标管理 */
    selectBidding(row) {
      console.log(row, "raaaaa");
      this.currentBid = { id: row.id, noticeStatus: row.noticeStatus };
      this.procurementSchemeName = row.procurementSchemeName;
      this.noticeId = row.noticeId;
      this.isPurchaseOfficer = row.purchaseOfficer;
    },
    /** 废标 */
    repealHandle() {
      this.abandonBidVisiable = true;
      this.abandonBidForm.operator = this.$store.state.user.nickname;
      this.abandonBidForm.procurementSchemeName = this.procurementSchemeName;
      this.abandonBidForm.noticeId = this.noticeId;
    },
    /** 重新招标 */
    againHandle() {
      const { id, noticeStatus } = this.currentBid;
      const current = this.schemeList.find((item) => item.id === id);
      console.log(current, "current");
      this.goDetail(current);
    },

    /** 文件上传回调 */
    handleSuccess(res) {
      const { url, name } = res.data;
      console.log(url, name, "a");
      this.abandonBidForm.attachmentList = [{ fileUrl: url, fileName: name }];
      this.$refs.abandonBidRef.clearValidate("attachmentList");
    },
    /** 关闭废标弹层 */
    closeDialog(formName) {
      this.$refs[formName].resetFields();
      this.$refs.uploadFild.clearFiles();
      this.abandonBidForm = {}
    },
    /** 提交废标 */
    confirmAbandonBid(formName) {
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          const { reason, attachmentList, noticeId } = this.abandonBidForm;
          const { id } = this.currentBid;
          let formData = {
            reason,
            attachmentList,
            abandonMoreVOList: [
              {
                noticeId,
                schemeId: id,
              },
            ],
          };
          console.log(formData, "formData");
          try {
            const res = await abandonBidMore(formData);
            this.$message.success("废标成功");
            this.abandonBidVisiable = false;
            this.currentBid = {};
            this.getBiddingSchemeList();
            console.log(res, "废标");
          } catch (err) {
            console.log(err);
          }
        }
      });
    },
  },
  watch: {
    /** 监控类型切换 */
    "queryParams.procurementType": {
      handler(val) {
        this.getBiddingSchemeList();
      },
    },
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.queryParams = {
            pageNumber: 1,
            pageSize: 10,
            procurementSchemeName: undefined,
            procurementSchemeCode: undefined,
            procurementOfficer: undefined,
            procurementOfficerName: undefined,
            noticeStatus: undefined,
            procurementType: "all",
            projectCode: newVal.code,
          };
            this.getBiddingSchemeList();
       
        }
      },
      immediate: true,
    },
  },
};
</script>
<style scoped lang="scss">
.tabs-box {
  position: relative;
  .tabs-box-right {
    position: absolute;
    top: 0;
    right: 0;
  }
}
</style>
