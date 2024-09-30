<template>
  <div class="app-container">
    <div class="context flex flex-column">
      <el-radio-group
        v-model="queryParams.procurementPlanType"
        size="small"
        style="padding-bottom: 15px"
      >
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button
          :label="dict.value"
          :name="dict.value"
          v-for="dict in dict.type.procurement_plan_type"
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
      >
        <el-form-item label="编号" prop="schemeCode" label-width="50px">
          <el-input
            v-model="queryParams.procurementSchemeCode"
            placeholder="请输入编号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="方案名称" prop="schemeName" label-width="68px">
          <el-input
            v-model="queryParams.procurementSchemeName"
            placeholder="请输入方案名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
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
            type="success"
            icon="el-icon-plus"
            size="small"
            @click="handleAdd"
            >新增
          </el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="loading"
        :data="schemeList"
        highlight-current-row
        stripe
        border
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="编号"
          min-width="200"
          align="center"
          prop="procurementSchemeCode"
        >
          <template slot-scope="scope">
            <a class="link-type" @click="goDetail(scope.row.id)">
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
          min-width="150"
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
          min-width="180"
          label="创建时间"
          align="center"
          prop="createTime"
        />
        <el-table-column
          min-width="100"
          label="状态"
          align="center"
          prop="stateText"
        />
        <el-table-column
          label="操作"
          min-width="200"
          align="center"
          fixed="right"
        >
          <template slot-scope="scope">
            <div
              v-if="
                Number(scope.row.state) === 0 || Number(scope.row.state) === 5
              "
            >
              <el-button
                type="text"
                @click="goUpdate(scope.row.id, scope.row.procurementType)"
                icon="el-icon-edit"
                size="small"
                >修改</el-button
              >
              <el-button
                type="text"
                @click="goSubmit(scope.row.id)"
                icon="el-icon-s-promotion"
                size="small"
                >提交</el-button
              >
              <el-button
                type="text"
                @click="
                  goCancellation(scope.row.id, scope.row.procurementSchemeName)
                "
                icon="el-icon-document-delete"
                size="small"
                >作废</el-button
              >
            </div>
            <div v-else-if="Number(scope.row.state) === 1">
              <el-button
                type="text"
                @click="
                  handelWithdrawalPlan(
                    scope.row.id,
                    scope.row.procurementSchemeName
                  )
                "
                icon="el-icon-document-delete"
                size="small"
                >撤回</el-button
              >
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <!-- 提交 -->
      <el-dialog
        title="提交"
        :visible.sync="submitDialogVisible"
        @close="resetForm"
      >
        <div>
          <div class="tags-container">
            <span class="required">*</span>
            <span class="tagsComments">批语：</span>
            <el-tag
              v-for="tag in tags"
              :key="tag"
              @click="setTag(tag)"
              :type="tag === selectedTag ? 'info' : ''"
              style="margin-right: 8px"
            >
              {{ tag }}
            </el-tag>
          </div>
          <el-input
            type="textarea"
            v-model="reviewText"
            placeholder="请输入批语"
            :rows="4"
            required
            style="margin-top: 8px"
          />
        </div>
        <div slot="footer" class="dialog-footer">
          <el-button @click="submitDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReview">确认</el-button>
        </div>
      </el-dialog>
      <!-- 选择采购计划 -->
      <el-dialog
        title="选择采购计划"
        :visible.sync="planVisible"
        width="55%"
        @closed="confirmPlanLoading = false"
      >
        <el-form
          :model="planQuery"
          ref="planForm"
          label-position="left"
          label-width="80px"
          size="small"
          @submit.native.prevent
        >
          <el-row :gutter="10">
            <el-col :span="10" class="grid-cell">
              <el-form-item
                label="采购名称"
                label-width="68px"
                prop="procurementPlanName"
                class="label-right-align"
              >
                <el-input
                  v-model="planQuery.procurementPlanName"
                  type="text"
                  clearable
                ></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6" class="grid-cell">
              <div class="static-content-item">
                <el-button
                  type="primary"
                  icon="el-icon-search"
                  size="small"
                  @click="searchPlan"
                  >查询</el-button
                >
              </div>
            </el-col>
          </el-row>
        </el-form>
        <el-table
          v-loading="planLoading"
          :data="planList"
          stripe
          size="small"
          highlight-current-row
          border
          @selection-change="handleSelectionChange"
        >
          <el-table-column
            type="selection"
            width="55"
            :selectable="() => !confirmPlanLoading"
          />
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="采购名称"
            prop="procurementPlanName"
            show-overflow-tooltip
          />
          <el-table-column label="采购类别" prop="procurementPlanTypeText" />
          <el-table-column
            label="价格类型"
            align="center"
            prop="priceTypeText"
          />
          <el-table-column
            label="采购方式"
            align="center"
            prop="procurementTypeText"
          />
          <el-table-column
            label="拆分合约规划名称"
            prop="splitContractName"
            show-overflow-tooltip
          />
          <el-table-column
            label="拟签约合同承包范围"
            prop="contractScope"
            show-overflow-tooltip
          />
        </el-table>
        <pagination
          v-show="planTotal > 0"
          :total="planTotal"
          :page.sync="planQuery.pageNumber"
          :limit.sync="planQuery.pageSize"
          @pagination="getPlanList"
        />
        <div slot="footer" class="dialog-footer">
          <el-button
            @click="planVisible = false"
            style="width: 100px"
            size="small"
            :disabled="confirmPlanLoading"
            >取 消</el-button
          >
          <el-button
            type="primary"
            @click="confirmPlan"
            style="width: 100px"
            size="small"
            :loading="confirmPlanLoading"
            :disabled="confirmPlanLoading"
            >确 定</el-button
          >
        </div>
      </el-dialog>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
        @pagination="getSchemeList"
      />
    </div>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import { mapGetters } from "vuex";
import {
  getSchemeList,
  submitProcurementScheme,
  cancellationProcurementScheme,
  checkProcurementSchemeSelect,
  withdrawalPlan,
} from "@/api/procurement/scheme";
import { getPlanList, getSplitPlanList } from "@/api/procurement/plan";

export default {
  name: "Scheme",
  dicts: ["procurement_plan_type"],
  data() {
    return {
      planVisible: false,
      schemeList: [],
      planList: [],
      // 遮罩层
      loading: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        procurementSchemeCode: undefined,
        procurementSchemeName: undefined,
        procurementOfficer: undefined,
        procurementPlanType: "all",
        projectCode: undefined,
      },
      planForm: {},
      planQuery: {
        procurementPlanName: undefined,
        pageNumber: 1,
        pageSize: 10,
        projectCode: undefined,
      },
      planTotal: 1,
      planLoading: false,
      ids: [], //选中的采购计划
      procurementType: "", //招标方式
      confirmPlanLoading: false,
      submitDialogVisible: false,
      schemeId: "",
      reviewText: "",
      selectedTag: null,
      tags: ["拟同意", "同意", "请修改, 再传至我处理", "阅"],
    };
  },
  methods: {
    /** 获取需求列表 */
    async getSchemeList() {
      this.loading = true;
      const query = {
        ...this.queryParams,
        procurementPlanType:
          this.queryParams.procurementPlanType === "all"
            ? undefined
            : this.queryParams.procurementPlanType,
      };
      try {
        const res = await getSchemeList(query);
        this.loading = false;
        if (res.data) {
          this.schemeList = res.data.rows;
          this.total = res.data.total;
        }
      } catch (err) {
        this.loading = false;
        console.log(err);
      }
    },
    /** 获取采购计划列表 */
    async getPlanList() {
      this.planLoading = true;
      try {
        const res = await getSplitPlanList(this.planQuery);
        this.planLoading = false;
        if (res.data) {
          this.planList = res.data.rows;
          this.planTotal = res.data.total;
        }
      } catch (err) {
        this.planLoading = false;
        console.log(err);
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getSchemeList();
    },
    /** 搜索采购计划 */
    searchPlan() {
      this.planQuery.pageNumber = 1;
      this.getPlanList();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.planVisible = true;
      this.getPlanList();
      // getSplitPlanList().then(res => {
      //   console.log(res,'拆分');
      // })
    },
    /** 确定选择计划 */
    async confirmPlan() {
      this.confirmPlanLoading = true;
      const { ids, procurementType } = this;
      if (ids.length === 0) {
        this.confirmPlanLoading = false;
        return this.$message.error("请选择计划");
      }
      console.log(this.ids, "ids-ids-ids");
      console.log(this.planList, "planList--planList");
      let filteredData = this.planList.filter((item) =>
        ids.includes(item.splitContractId)
      );
      let hasSameName = filteredData.every(
        (item, index, filteredData) =>
          item.procurementPlanType === filteredData[0].procurementPlanType &&
          item.procurementType === filteredData[0].procurementType &&
          item.priceType === filteredData[0].priceType
      );
      if (!hasSameName) {
        this.confirmPlanLoading = false;
        return this.$message.error(
          "采购类别、价格类型、采购方式都必须是同一类别"
        );
      }
      try {
        await checkProcurementSchemeSelect(ids);
        this.planVisible = false;
        let param = Base64.encode(JSON.stringify({ ids, procurementType }));
        param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
        this.$router.push(`/procurement/add-scheme/${param}`);
      } catch (err) {
        console.log(err);
      }
      this.confirmPlanLoading = false;
    },
    /** 已选择计划 */
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.splitContractId);
      this.procurementType =
        (selection[0] && selection[0].procurementType) || "";
    },
    /** 跳转方案详情 */
    goDetail(id) {
      let param = Base64.encode(JSON.stringify(id));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/scheme-detail/${param}`);
    },
    goUpdate(id, procurementType) {
      let param = Base64.encode(
        JSON.stringify({ id, procurementType, type: "update" })
      );
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/add-scheme/${param}`);
    },
    setTag(tag) {
      this.selectedTag = tag;
      this.reviewText = tag; // 将选中的标签文本填入文本框
    },
    //提交
    goSubmit(id) {
      this.submitDialogVisible = true;
      this.schemeId = id;
    },
    async submitReview() {
      if (!this.reviewText.trim()) {
        this.$message.error("批语不能为空");
        return;
      }
      const loading = this.$loading({
        lock: true,
        text: "正在提交...",
        background: "rgba(0, 0, 0, 0.7)",
      });
      try {
        let param = Base64.encode(JSON.stringify(this.schemeId));
        param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
        const detailUrl = `/procurement/scheme-detail/${param}`;
        await submitProcurementScheme({
          id: this.schemeId,
          detailUrl,
          operateComment: this.reviewText,
        });
        this.$message.success("提交成功");
        this.getSchemeList();
        this.resetForm(); // 重置表单
      } catch (error) {
        this.$message.error("提交失败");
      } finally {
        loading.close();
      }
    },
    resetForm() {
      this.submitDialogVisible = false;
      this.reviewText = "";
      this.selectedTag = null;
    },
    /** 作废 **/
    goCancellation(id, procurementSchemeName) {
      this.$confirm("确定要作废采购方案：" + procurementSchemeName, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          await cancellationProcurementScheme(id);
          this.$message.success("作废成功");
          this.getSchemeList();
        } catch (error) {}
      });
    },
    //切换tab类型
    handleTypeClick(tab) {
      this.queryParams.procurementPlanType = tab.name;
    },
    // 撤回
    handelWithdrawalPlan(id, name) {
      this.$confirm("是否确定撤回采购方案：" + name, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          this.revokeLoding = this.$loading({
            lock: true,
            text: "撤回中...",
            spinner: "el-icon-loading",
            background: "rgba(0, 0, 0, 0.7)",
          });
          withdrawalPlan(id)
            .then((res) => {
              if (res.code == 200) {
                this.$message.success("撤回成功");
              }
              this.revokeLoding.close();
              this.getSchemeList();
            })
            .catch((e) => this.revokeLoding.close());
        } catch (error) {}
      });
    },
  },
  computed: {
    ...mapGetters(["project"]),
  },
  watch: {
    /** 监控类型切换 */
    "queryParams.procurementPlanType": {
      handler(val) {
        this.getSchemeList();
      },
    },
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.planQuery.projectCode = newVal.code;
          this.queryParams = {
            pageNumber: 1,
            pageSize: 10,
            procurementSchemeCode: undefined,
            procurementSchemeName: undefined,
            procurementOfficer: undefined,
            procurementPlanType: "all",
            projectCode: newVal.code,
          };
          this.getSchemeList();
        }
      },
      immediate: true,
    },
  },
};
</script>
<style lang="scss" scoped>
.hxwd_table_item_center_first {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hxwd_table_item_center {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #eaeaea;
}

.hxwd_table_item_left_first {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
}

.hxwd_table_item_left {
  padding-left: 10px;
  margin-left: -10px;
  margin-right: -10px;
  padding-right: 10px;
  min-height: 50px;
  display: flex;
  align-items: center;
  border-top: 1px solid #eaeaea;
}
.required {
  color: rgb(245, 108, 108);
  margin-right: 4px;
}
</style>
