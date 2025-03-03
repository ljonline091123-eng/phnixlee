<template>
  <div class="app-container">
    <div class="context flex flex-column">
      <el-radio-group
        v-model="queryParams.expenditureBusinessType"
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
        <el-form-item label="合同编号" prop="agreementCode" label-width="68px">
          <el-input
            v-model="queryParams.agreementCode"
            placeholder="请输入合同编号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="合同名称" prop="agreementName" label-width="68px">
          <el-input
            v-model="queryParams.agreementName"
            placeholder="请输入合同名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item
          label="乙方"
          prop="belongAccountingItem"
          label-width="50px"
        >
          <el-input
            v-model="queryParams.vendorName"
            placeholder="请输入乙方名称"
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
          <el-badge :value="total_procurement" :max="99" style="margin-left: 12px;margin-top: -1px;">
          <el-button
            type="success"
            icon="el-icon-plus"
            size="small"
            @click="handleAdd"
            v-hasPermi="['procurement:contract:add']"
            >新增</el-button
          >
          </el-badge>
          <el-badge :value="total_procurement_yl" style="margin-left: 12px;margin-top: -1px;">
              <el-button
              type="success"
              icon="el-icon-plus"
              size="small"
              @click="openSelectYl"
              >新增易料合同</el-button
            >
          </el-badge>

        </el-form-item>
      </el-form>
      <el-table
        v-loading="loading"
        :data="planList"
        highlight-current-row
        stripe
        border
        :header-cell-style="{ background: '#F3F2F8' }"
      >
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="合同编号"
          min-width="200"
          align="center"
          prop="agreementCode"
        >
          <template slot-scope="scope">
            <a
              class="link-type"
              @click="goDetail(scope.row.id, scope.row.expenditureBusinessType)"
            >
              {{ scope.row.agreementCode }}
            </a>
          </template>
        </el-table-column>
        <el-table-column
        label="招标编号"
        min-width="200"
        align="center"
        prop="agreementCode"
      >
        <template slot-scope="scope">
          <a v-if="scope.row.procurementTypeText"
            class="link-type"
            @click="goDetailBid(scope.row.schemeId, scope.row.noticeId, scope.row.procurementType)"
          >
            {{ scope.row.procurementSchemeCode }}
          </a>
          <span v-else>{{ scope.row.procurementSchemeCode }}</span>
        </template>
      </el-table-column>
        <el-table-column
          label="合同名称"
          min-width="200"
          align="left"
          prop="agreementName"
          show-overflow-tooltip
        />
        <el-table-column
          label="甲方"
          align="left"
          prop="partyAName"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
        label="采购方式"
        min-width="200"
        align="center"
        prop="agreementCode"
      >
        <template slot-scope="scope">
          <a v-if="scope.row.procurementTypeText"
            class="link-type"
            @click="goDetailBid(scope.row.schemeId, scope.row.noticeId, scope.row.procurementType)"
          >
            {{ scope.row.procurementTypeText?scope.row.procurementTypeText:'易料采购' }}
          </a>
          <span v-else>{{ scope.row.procurementTypeText?scope.row.procurementTypeText:'易料采购' }}</span>
        </template>
      </el-table-column>
        <el-table-column
          label="乙方"
          align="left"
          prop="partyBName"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          min-width="100"
          label="合同金额(元)"
          align="right"
          prop="totalAmountText"
        />
        <el-table-column
          min-width="100"
          label="填报人"
          align="center"
          prop="reporterName"
        />
        <el-table-column
          min-width="100"
          label="状态"
          align="center"
          prop="agreementStateText"
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
                Number(scope.row.isOperate) === 1 &&
                (Number(scope.row.agreementState) === 0 ||
                  Number(scope.row.agreementState) === 5)
              "
            >
              <el-button
                type="text"
                @click="
                  goEdit(
                    scope.row.id,
                    scope.row.agreementName,
                    scope.row.expenditureBusinessType,
                    scope.row.procurementTypeText
                  )
                "
                icon="el-icon-s-promotion"
                size="small"
                >修改</el-button
              >
              <el-button
                type="text"
                @click="
                  goSubmit(
                    scope.row.id,
                    scope.row.agreementName,
                    scope.row.expenditureBusinessType
                  )
                "
                icon="el-icon-s-promotion"
                size="small"
                >提交</el-button
              >
              <el-button
                type="text"
                @click="goCancellation(scope.row.id, scope.row.agreementName)"
                icon="el-icon-document-delete"
                size="small"
                v-if="
                  Number(scope.row.agreementState) === 0 ||
                  Number(scope.row.agreementState) === 5 ||
                  Number(scope.row.agreementState) === 20
                "
                >作废</el-button
              >
            </div>
            <div
              v-else-if="
                Number(scope.row.isOperate) === 1 &&
                Number(scope.row.agreementState) === 1
              "
            >
              <el-button
                type="text"
                @click="
                  revokeProcess(
                    scope.row.id,
                    scope.row.agreementName,
                    scope.row.wfProcessId
                  )
                "
                icon="el-icon-document-delete"
                size="small"
                >撤回</el-button
              >
            </div>

<!--            -->
<!--            <div-->
<!--              v-else-if="-->
<!--                Number(scope.row.isOperate) === 1 &&-->
<!--                Number(scope.row.agreementState) === 3-->
<!--              "-->
<!--            >-->
<!--              <el-button-->
<!--                type="text"-->
<!--                @click="-->
<!--                  pushToVendor(-->
<!--                    scope.row.id,-->
<!--                    scope.row.agreementName,-->
<!--                    scope.row.partyBName-->
<!--                  )-->
<!--                "-->
<!--                icon="el-icon-s-promotion"-->
<!--                size="small"-->
<!--                >推送至供应商</el-button-->
<!--              >-->
<!--            </div>-->
<!--            <div-->
<!--              v-else-if="-->
<!--                Number(scope.row.isOperate) === 1 &&-->
<!--                Number(scope.row.agreementState) === 7-->
<!--              "-->
<!--            >-->
<!--              <el-button-->
<!--                type="text"-->
<!--                @click="-->
<!--                  pushToSignPlatform(-->
<!--                    scope.row.id,-->
<!--                    scope.row.agreementName,-->
<!--                    scope.row.partyADeptId-->
<!--                  )-->
<!--                "-->
<!--                icon="el-icon-s-promotion"-->
<!--                size="small"-->
<!--                >推送至电子签章平台</el-button-->
<!--              >-->
<!--            </div>-->
<!--            <div-->
<!--              v-else-if="-->
<!--                Number(scope.row.isOperate) === 1 &&-->
<!--                Number(scope.row.agreementState) === 9-->
<!--              "-->
<!--            >-->
<!--              <el-button-->
<!--                type="text"-->
<!--                @click="toSignAgreement(scope.row.id)"-->
<!--                icon="el-icon-paperclip"-->
<!--                size="small"-->
<!--                >签署</el-button-->
<!--              >-->
<!--            </div>-->
<!--            <div-->
<!--              v-else-if="-->
<!--                Number(scope.row.isOperate) === 1 &&-->
<!--                Number(scope.row.agreementState) === 10-->
<!--              "-->
<!--            >-->
<!--              <el-button-->
<!--                type="text"-->
<!--                @click="-->
<!--                  toCancelledSignAgreementDialog(-->
<!--                    scope.row.id,-->
<!--                    scope.row.agreementName-->
<!--                  )-->
<!--                "-->
<!--                icon="el-icon-delete"-->
<!--                size="small"-->
<!--                >作废签署合同</el-button-->
<!--              >-->
<!--            </div>-->



            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
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
    <!-- 选择采购合同 -->
    <el-dialog title="选择采购合同" :visible.sync="dialogVisible" width="55%">
      <el-form
        :model="queryParams"
        ref="queryForm_procurement"
        size="small"
        :inline="true"
        label-width="120px"
      >
        <el-form-item label="采购任务名称" prop="procurementSchemeName">
          <el-input
            v-model="queryParams_procurement.procurementSchemeName"
            placeholder="请输入采购任务名称"
            clearable
            @keyup.enter.native="handleQuery_procurement"
          />
        </el-form-item>
        <el-form-item label="采购任务编码" prop="procurementSchemeCode">
          <el-input
            v-model="queryParams_procurement.procurementSchemeCode"
            placeholder="请输入采购任务编码"
            clearable
            @keyup.enter.native="handleQuery_procurement"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="el-icon-search"
            size="small"
            @click="handleQuery_procurement"
            >查询</el-button
          >
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading_procurement"
        :data="procurementTableList"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        border
        stripe
      >
        <el-table-column label="选择" align="center" width="70">
          <template slot-scope="scope">
            <el-radio
              :label="scope.row"
              v-model="radio"
              @change.native="handleSelectionChange(scope.row)"
            >
              <span></span>
            </el-radio>
          </template>
        </el-table-column>
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="采购任务名称"
          align="left"
          prop="procurementSchemeName"
        />
        <el-table-column
          label="采购任务编号"
          width="250"
          align="center"
          prop="procurementSchemeCode"
        />
        <el-table-column
          label="采购需求类型"
          width="150"
          align="center"
          prop="procurementPlanTypeText"
        />
      </el-table>

      <div class="pagination_item">
        <pagination
          v-show="total_procurement > 0"
          :total="total_procurement"
          :page.sync="queryParams_procurement.pageNumber"
          :limit.sync="queryParams_procurement.pageSize"
          @pagination="getList_procurement"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button
          @click="dialogVisible = false"
          style="width: 100px"
          size="small"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="submitProcurement"
          style="width: 100px"
          size="small"
          >确 定</el-button
        >
      </span>
    </el-dialog>

    <!-- 选择易料订单 -->
    <el-dialog
      title="选择易料订单"
      :visible.sync="marketDialogVisible"
      width="55%"
    >
      <el-form
        :model="queryParams_market"
        ref="queryParams_market"
        size="small"
        :inline="true"
        label-width="120px"
      >
        <el-form-item label="易料订单名称" prop="materialsName">
          <el-input
            v-model="queryParams_market.materialsName"
            placeholder="请输入易料订单名称"
            clearable
            @keyup.enter.native="handleQuery_market"
          />
        </el-form-item>
        <el-form-item label="易料订单编码" prop="materialsCode">
          <el-input
            v-model="queryParams_market.materialsCode"
            placeholder="请输入易料订单编码"
            clearable
            @keyup.enter.native="handleQuery_market"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="el-icon-search"
            size="small"
            @click="handleQuery_market"
            >查询</el-button
          >
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading_market"
        :data="marketTableList"
        @selection-change="handleSelectionChangeMarket"
        style="width: 100%"
        border
        stripe
      >
        <el-table-column label="选择" align="center" width="70">
          <template slot-scope="scope">
            <el-radio
              :label="scope.row"
              v-model="radio"
              @change.native="handleSelectionChangeMarket(scope.row)"
            >
              <span></span>
            </el-radio>
          </template>
        </el-table-column>
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column
          label="易料订单名称"
          width="200"
          prop="materialsName"
          show-overflow-tooltip
        />
        <el-table-column
          label="易料订单编号"
          width="200"
          prop="materialsCode"
          show-overflow-tooltip
        />
        <el-table-column
          label="关联商务策划编码"
          width="200"
          prop="businessCode"
          show-overflow-tooltip
        />
        <el-table-column label="物料数量" prop="quantity" />
        <el-table-column label="含税单价" prop="quantity" />
        <el-table-column label="不含税单价" prop="noTaxPrice" />
      </el-table>

      <div class="pagination_item">
        <pagination
          v-show="total_market > 0"
          :total="total_market"
          :page.sync="queryParams_market.pageNumber"
          :limit.sync="queryParams_market.pageSize"
          @pagination="getList_market"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button
          @click="marketDialogVisible = false"
          style="width: 100px"
          size="small"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="submitMarket"
          style="width: 100px"
          size="small"
          >确 定</el-button
        >
      </span>
    </el-dialog>

    <el-dialog title="新增合同" :visible.sync="open" width="80%">
      <el-form
        :rules="rules"
        ref="form"
        :model="form"
        label-width="120px"
        label-suffix=":"
      >
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="合同来源" prop="jobName"
              >采购任务</el-form-item
            >
          </el-col>
          <!-- <el-col :span="12">
            <el-form-item label="合同编号：" prop="schemeCode">
              <el-input
                v-model="form.schemeCode"
                placeholder="系统自动生成"
                disabled
              />
            </el-form-item>
          </el-col> -->
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="合同名称" prop="schemeName">
              <el-input
                v-model="form.schemeName"
                placeholder="请输入合同名称"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合约规划上限价" prop="upperLimitPriceText">
              <el-input
                v-model="planAmountInfo.upperLimitPriceText"
                placeholder="选择采购方案后自动生成"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="采购任务" prop="procurementSchemeName">
              <el-input
                v-model="form.procurementSchemeName"
                placeholder="请选择采购任务"
                readonly
                @click.native="openSelectProcurement"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="采购上限价"
              prop="procurementUpperLimitPriceText"
            >
              <el-input
                v-model="planAmountInfo.procurementUpperLimitPriceText"
                placeholder="选择采购方案后自动生成"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="采购方案" prop="splitId" ref="splitId">
              <el-select
                v-model="form.splitId"
                placeholder="请选择采购方案"
                style="width: 100%"
              >
                <el-option
                  v-for="item in contractSplitOptions"
                  :key="item.splitId"
                  :label="item.splitContractName ? item.splitContractName : item.schemeName"
                  :value="item.splitId"
                >
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已发生总价" prop="usedTotalAmountText">
              <el-input
                v-model="planAmountInfo.usedTotalAmountText"
                placeholder="选择采购方案后自动生成"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="中标供应商" prop="vendorId" ref="vendorId">
              <el-select
                v-model="form.vendorId"
                placeholder="请选择中标供应商"
                style="width: 100%"
              >
                <el-option
                  v-for="item in biddingVendorOptions"
                  :key="item.vendorId"
                  :label="item.vendorName"
                  :value="item.vendorId"
                >
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="剩余可用总价" prop="surplusTotalAmountText">
              <el-input
                v-model="planAmountInfo.surplusTotalAmountText"
                placeholder="选择采购方案后自动生成"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div>
          <virtual-scroll
            :data="form.vendorBiddingListQuotationList"
            :item-size="62"
            key-prop="materialsId"
            ref="virScrollRef"
            @change="(renderData) => virtualData = renderData">
              <el-table
                v-loading="loading_tax"
                :data="virtualData"
                class="translateYisZero"
                stripe
                highlight-current-row
                border
              >
                <el-table-column
                  label="序号"
                  type="index"
                  width="50"
                  align="center"
                  fixed
                />
                <el-table-column
                  label="清单编码"
                  width="150"
                  prop="materialsCode"
                  show-overflow-tooltip
                  fixed
                />
                <el-table-column
                  label="清单名称"
                  prop="materialsName"
                  width="150"
                  show-overflow-tooltip
                  fixed
                />
    <!--            <el-table-column-->
    <!--              label="交易标的物"-->
    <!--              prop="subjectMatterName"-->
    <!--              width="150"-->
    <!--              show-overflow-tooltip-->
    <!--              fixed-->
    <!--            />-->
    <!--            <el-table-column-->
    <!--              label="规格型号"-->
    <!--              prop="specification"-->
    <!--              show-overflow-tooltip-->
    <!--              fixed-->
    <!--            />-->
                <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>
                <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules"  show-overflow-tooltip/>
                <el-table-column label="工作内容" align="center" prop="workContent"  show-overflow-tooltip/>
                <el-table-column
                  label="计量单位"
                  align="center"
                  prop="unitMeasurement"
                  fixed
                />
                <el-table-column
                  label="投标总量"
                  align="right"
                  prop="countText"
                  :key="'count'"
                  width="150"
                  v-if="procurementType != 2 && procurementType != 3"
                  fixed
                />
                <el-table-column
                  label="剩余可用量"
                  align="right"
                  prop="surplusCountText"
                  width="150"
                />
                <el-table-column
                  header-align="center"
                  align="center"
                  label="合同价"
                >
                        <el-table-column
                          label="签订量"
                          align="center"
                          prop="signCount"
                          width="150"
                          :key="'signCount'"
                        >
                          <template slot-scope="scope">
                            <el-form-item
                              label-width="0"
                              :prop="
                              'vendorBiddingListQuotationList.' +
                              scope.$index +
                              '.signCount'
                            "
                              :rules="[
                              {
                                required: true,
                                trigger: 'blur',
                                message: '请输入签订量',
                              },
                              {
                                pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/,
                                trigger: 'blur',
                                message: '请输入正确的值',
                              },
                              {
                                pattern: /^\d+(\.\d{0,4})?$/,
                                trigger: 'blur',
                                message: '请输入小于4位的小数',
                              },
                            ]"
                            >
                              <el-input
                                v-model="scope.row.signCount"
                                placeholder="请输入"
                                v-thousandth
                              />
                            </el-form-item>
                          </template>
                        </el-table-column>
                        <el-table-column
                          label="签订含税单价(元)"
                          align="center"
                        prop="signUnitPriceInclTax"
                          width="150"
                          :key="'signUnitPriceInclTax'"
                        >
                          <template slot-scope="scope">
                            <el-form-item
                              label-width="0"
                              :prop="
                              'vendorBiddingListQuotationList.' +
                              scope.$index +
                              '.signUnitPriceInclTax'
                            "
                              :rules="[
                              {
                                required: true,
                                trigger: 'blur',
                                message: '请输入签订含税单价',
                              },
                              {
                                pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/,
                                trigger: 'blur',
                                message: '请输入正确的值',
                              },
                              {
                                pattern: /^\d+(\.\d{0,4})?$/,
                                trigger: 'blur',
                                message: '请输入小于4位的小数',
                              },
                            ]"
                            >
                              <el-input
                                v-model="scope.row.signUnitPriceInclTax"
                                placeholder="请输入"
                                v-thousandth
                              />
                            </el-form-item>
                          </template>
                        </el-table-column>
                        <el-table-column
                          label="签订不含税单价(元)"
                          align="right"
                          width="170"
                        >
                          <template slot-scope="scope">
                            {{
                              countComputed(
                                scope.row,
                                scope.row.signUnitPriceInclTax,
                                scope.row.taxRate,
                                scope.row.signCount,
                                "excludingTax"
                              )
                            }}
                          </template>
                        </el-table-column>
                        <el-table-column label="含税总价(元)" align="right" width="150">
                          <template slot-scope="scope">
                            {{
                              countComputed(
                                scope.row,
                                scope.row.signUnitPriceInclTax,
                                scope.row.taxRate,
                                scope.row.signCount,
                                "taxIncludedTotal"
                              )
                            }}
                          </template>
                        </el-table-column>
                        <el-table-column
                          label="不含税总价(元)"
                          align="right"
                          width="150"
                        >
                          <template slot-scope="scope">
                            {{
                              countComputed(
                                scope.row,
                                scope.row.signUnitPriceInclTax,
                                scope.row.taxRate,
                                scope.row.signCount
                              )
                            }}
                          </template>
                        </el-table-column>

                        <el-table-column
                          label="租赁方式"
                          align="right"
                          prop="rentModeText"
                          :key="'rentModeText'"
                          v-if="procurementType == 2 || procurementType == 3"
                        />
                        <el-table-column
                          label="租赁时间"
                          align="right"
                          prop="rentTimeText"
                          width="150"
                          :key="'rentTimeText'"
                          v-if="procurementType == 2 || procurementType == 3"
                        >
                          <template slot-scope="scope">
                            {{ scope.row.rentTimeText || "-" }}
                          </template>
                        </el-table-column>
                        <el-table-column
                          label="租赁数量"
                          align="right"
                          prop="rentQuantityText"
                          width="150"
                          :key="'rentQuantityText'"
                          v-if="procurementType == 2 || procurementType == 3"
                        >
                          <template slot-scope="scope">
                            {{ scope.row.rentQuantityText || "-" }}
                          </template>
                        </el-table-column>
                        <el-table-column
                          label="工作量"
                          align="right"
                          prop="countText"
                          width="150"
                          :key="'countText'"
                          v-if="procurementType == 2 || procurementType == 3"
                        />
                </el-table-column>
                <el-table-column
                  header-align="center"
                  align="center"
                  label="中标价"
                >
                    <el-table-column
                      label="基价(元)"
                      align="right"
                      prop="basePriceText"
                      width="150"
                      v-if="[2,3,4,5,6,7].includes(priceType)"
                      :key="'basePriceText'"
                    />
                    <el-table-column
                      label="浮动价(元)"
                      align="right"
                      width="150"
                      prop="floatingPriceText"
                      v-if="[2,3,6,7].includes(priceType)"
                      :key="'floatingPriceText'"
                    />
                    <el-table-column
                      label="浮动率"
                      align="right"
                      width="150"
                      prop="floatingRateText"
                      :key="'floatingRateText'"
                      v-if="[4,5,6,7].includes(priceType)"
                    />
                      <el-table-column
                        label="含税单价(元)"
                        align="right"
                        width="150"
                        prop="taxUnitPriceText"
                        :key="'taxUnitPriceText'"
                        v-if="subjectMatter != 1 && subjectMatter != 2"
                      />
                      <el-table-column
                        label="不含税单价(元)"
                        align="right"
                        width="150"
                        prop="notTaxUnitPriceText"
                        :key="'notTaxUnitPriceText'"
                        v-if="subjectMatter != 1 && subjectMatter != 2"
                      />
                      <el-table-column
                        label="含税总价(元)"
                        align="right"
                        width="150"
                        prop="taxPriceText"
                      />
                      <el-table-column
                        label="不含税总价(元)"
                        align="right"
                        width="150"
                        prop="notTaxPriceText"
                      />
                      <el-table-column label="税率(%)" align="center" prop="taxRate" />
                </el-table-column>

                <el-table-column
                  label="发票类型"
                  align="center"
                  prop="billTypeText"
                  width="150"
                  show-overflow-tooltip
                />
              </el-table>
          </virtual-scroll>
          <div
            style="display: flex; justify-content: flex-end; margin-top: 20px"
          >
            <!-- <span style="margin-right: 20px">
              本次含税总计：<span
                style="font-weight: bold"
                v-thousands="totalTaxPriceTotal"
              ></span> -->
            <span>
              本次不含税总计：<span
                style="font-weight: bold"
                v-thousands="totalNotTaxPriceTotal"
              ></span>
            </span>
          </div>
        </div>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="open = false" style="width: 100px" size="small"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="submitFirstForm"
          style="width: 100px"
          size="small"
          >确 定</el-button
        >
      </span>
    </el-dialog>

<!--    <el-dialog-->
<!--      title="推送至电子签章平台"-->
<!--      :visible.sync="pushSignDialog"-->
<!--      width="600px"-->
<!--      @closed="clearPushSignFormData"-->
<!--    >-->
<!--      <span-->
<!--        style="-->
<!--          font-size: 16px;-->
<!--          line-height: 30px;-->
<!--          text-align: center;-->
<!--          margin-bottom: 25px;-->
<!--          display: block;-->
<!--        "-->
<!--        >{{ pushSignTitle }}</span-->
<!--      >-->
<!--      <el-form-->
<!--        :model="pushSignFormData"-->
<!--        ref="pushSignForm"-->
<!--        :rules="pushSignFormRules"-->
<!--      >-->
<!--        <el-form-item label="签署人：" prop="partyAUserId">-->
<!--          <el-select-->
<!--            v-model="pushSignFormData.partyAUserId"-->
<!--            placeholder="请选择"-->
<!--            filterable-->
<!--          >-->
<!--            <el-option-->
<!--              v-for="item in partyAUserList"-->
<!--              :key="item.userId"-->
<!--              :label="item.nickName"-->
<!--              :value="item.userId"-->
<!--            >-->
<!--            </el-option>-->
<!--          </el-select>-->
<!--        </el-form-item>-->
<!--      </el-form>-->
<!--      <div slot="footer" class="dialog-footer">-->
<!--        <el-button @click="pushSignDialog = false">取 消</el-button>-->
<!--        <el-button-->
<!--          type="primary"-->
<!--          @click="toPushSignPlatform"-->
<!--          :loading="pushSignFormSubBtn"-->
<!--          >{{ pushSignFormSubBtn ? "推送中..." : "推 送" }}</el-button-->
<!--        >-->
<!--      </div>-->
<!--    </el-dialog>-->

    <el-dialog
      title="签署合同"
      :visible.sync="signAgreementDialog"
      width="1500px"
    >
      <iframe
        v-if="signAgreementUrl"
        :src="signAgreementUrl"
        width="100%"
        height="800px"
      ></iframe>
    </el-dialog>

    <el-dialog
      title="作废签署合同"
      :visible.sync="cancelledSignDialog"
      width="600px"
      @closed="clearCancelledSignFormData"
    >
      <span
        style="
          font-size: 16px;
          line-height: 30px;
          text-align: center;
          margin-bottom: 25px;
          display: block;
        "
        >{{ cancelledSignTitle }}</span
      >
      <el-form
        :model="cancelledSignFormData"
        ref="cancelledSignForm"
        :rules="cancelledSignFormRules"
        label-width="100px"
      >
        <el-form-item label="作废原因：" prop="cancelledReason">
          <el-input
            v-model="cancelledSignFormData.cancelledReason"
            placeholder="请输入作废原因"
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelledSignDialog = false">取 消</el-button>
        <el-button
          type="primary"
          @click="toCancelledSignAgreement"
          :loading="cancelledSignSubBtn"
          >{{ cancelledSignSubBtn ? "作废中..." : "确  认" }}</el-button
        >
      </div>
    </el-dialog>

     <!-- 选择易料单据 -->
     <el-dialog
     title="请选择易料单据"
     :visible.sync="contractVisibleYl"
     width="55%"
     @closed="contractVisibleYl = false"
   >
     <el-form
       :model="queryParams_procurementYl"
       ref="planForm"
       label-position="left"
       size="small"
       @submit.native.prevent
     >
       <el-row :gutter="10">
         <el-col :span="9" class="grid-cell">
           <el-form-item
             label="单据编号："
             label-width="90px"
             prop="agreementCode"
             class="label-right-align"
           >
             <el-input
               v-model="queryParams_procurementYl.agreementCode"
               type="text"
               clearable
             ></el-input>
           </el-form-item>
         </el-col>
         <el-col :span="9" class="grid-cell">
          <el-form-item
            label="单据名称："
            label-width="90px"
            prop="agreementName"
            class="label-right-align"
          >
            <el-input
              v-model="queryParams_procurementYl.agreementName"
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
               @click="searchYl"
               >查询</el-button
             >
           </div>
         </el-col>
       </el-row>
     </el-form>
     <el-table
       v-loading="loading_procurement_yl"
       :data="procurementTableListYl"
       stripe
       size="small"
       highlight-current-row
       border
       @row-click="selectBcTemplate"
       @selection-change="handleSelectionChangeYl"
     >


       <el-table-column label="" width="30" align="center">
        <template slot-scope="scope">
          <el-radio
            class="table_radio"
            v-model="selectedRowYl"
            :label="scope.row.id"
          />
        </template>
      </el-table-column>
       <el-table-column
         label="序号"
         type="index"
         width="50"
         align="center"
       />
       <el-table-column
       label="单据编号"
       prop="agreementCode"
       show-overflow-tooltip
     />
       <el-table-column
         label="单据名称"
         prop="agreementName"
         show-overflow-tooltip
       />

       <el-table-column label="乙方名称" prop="partyBName" />

       <el-table-column
         label="支出业务类型"
         align="center"
         prop="expenditureBusinessTypeText"
       />

     </el-table>

     <pagination
        v-show="total_procurement_yl > 0"
        :total="total_procurement_yl"
        :page.sync="queryParams_procurementYl.pageNumber"
        :limit.sync="queryParams_procurementYl.pageSize"
        @pagination="getList_yl"
      />
     <div slot="footer" class="dialog-footer">
       <el-button
         @click="contractVisibleYl = false"
         style="width: 100px"
         size="small"
         >取 消</el-button
       >
       <el-button
         type="primary"
         @click="submitProcurementYl"
         style="width: 100px"
         size="small"
         >确 定</el-button
       >
     </div>
   </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { Base64 } from "js-base64";
import { create, all } from "mathjs";
import {
  checkAgreementCreateInfo,getAgreementCreateInfoYl,
  listAgreement,
  getAgreementSelectedProcurementInfo,
  listContractSplit,
  listSignAgreementScheme,listMarketMaterialContract,
  listVendorBiddingListQuotation,getCheckAgreementCreateInfo,
  cancellationAgreement,
  revokeAgreement,
  submitAgreement,
  checkAgreementUpdate,
  pushAgreementToVendor,
  pushAgreementToSignPlatform,
  getPartyAUserList,
  signAgreement,
  cancelledSignAgreement,
  getMarketOrder,
} from "@/api/procurement/contract";
import { addUser, updateUser } from "@/api/system/user";
import {
  cancellationProcurementPlan,
  getMinProject,
  submitProcurementPlan,
} from "@/api/procurement/plan";
import { getContractTypeList } from "@/api/template/file";

import { validatenull } from "@/utils/validate";
import VirtualScroll from "el-table-virtual-scroll";

export default {
  name: "sign-contract",
  components: {VirtualScroll},
  dicts: ["plan_type", "procurement_plan_type"],

  data() {
    return {
      templateRow:{},
      contractVisibleYl:false,
      revokeLoding: "",
      form: {
        vendorBiddingListQuotationList: [],
      },
      infoTitle: "合同信息新增",
      infoVisible: false,
      // * 规则
      rules: {
        vendorId: [
          { required: true, message: "中标供应商不能为空", trigger: "change" },
        ],
        splitId: [
          { required: true, message: "采购方案不能为空", trigger: "change" },
        ],
        procurementSchemeName: [
          { required: true, message: "采购任务不能为空", trigger: "input" },
        ],
        schemeName: [
          { required: true, message: "合同名称不能为空", trigger: "change" },
        ],
      },
      subjectMatter: "",
      procurementType: "",
      priceType: "",
      schemeId: null,
      selectedRow: {},
      selectedRowYl: '',
      radio: "",
      policyData: {},
      // * 采购合同列表
      procurementTableList: [],
      //易料
      procurementTableListYl: [],
      total_procurement_yl: 0,
      loading_procurement_yl: false,
      dialogVisible: false,
      // * 采购方案下拉
      contractSplitOptions: [],
      biddingVendorOptions: [],
      rule: {},
      planList: [
        {
          contractCode: "GC-6546466656464",
          contractName: "测试计划",
          partyA: "长沙建投集团",
          partyB: "XX供应商",
          operator: "张三",
          contractAmount: "8888.00",
          createBy: "张三",
          state: "审批中",
        },
      ],
      // 遮罩层
      loading: false,
      loading_procurement: false,
      loading_tax: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total_procurement: 0,
      //易料
      marketForm: {
        vendorBiddingListQuotationList: [],
      },
      marketDialogVisible: false,
      loading_market: false,
      // 查询参数
      queryParams_procurement: {
        pageNumber: 1,
        pageSize: 10,
        projectCode: undefined,
      },
      //易料查询参数
      queryParams_procurementYl: {
        pageNumber: 1,
        pageSize: 10,
        belongAccountingItemCode: undefined,
      },
      //易料查询参数
      queryParams_market: {
        materialsName: undefined,
        materialsCode: undefined,
        pageNumber: 1,
        pageSize: 10,
      },
      selectedRowMarket: {},
      total_market: 0,
      marketTableList: [],

      // 总条数
      total: 0,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      form: {},
      virtualData: [], // 虚拟列表渲染的数据
      // 查询参数
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
        belongAccountingItem: undefined,
        agreementCode: undefined,
        agreementName: undefined,
        projectCode: undefined,
        expenditureBusinessType: "all",
      },
      planAmountInfo: {},
      pushSignDialog: false,
      pushSignTitle: "",
      pushSignFormData: {},
      partyAUserList: [],
      pushSignFormRules: {
        partyAUserId: [
          { required: true, message: "请选择签订人", trigger: "change" },
        ],
      },
      pushSignFormSubBtn: false,
      signAgreementDialog: false,
      signAgreementUrl: "",
      cancelledSignDialog: false,
      cancelledSignTitle: "",
      cancelledSignFormData: {},
      cancelledSignFormRules: {
        cancelledReason: [{ required: true, message: "请输入作废原因" }],
      },
      cancelledSignSubBtn: false,
      contractTypeMode: "inviteBids",
      submitDialogVisible: false,
      contractId: "",
      contractType: "",
      reviewText: "",
      selectedTag: null,
      tags: ["拟同意", "同意", "请修改, 再传至我处理", "阅"],
    };
  },
  created() {

    this.mathjs = create(all);
    this.mathjs.config({
      number: "BigNumber",
    });
  },
  watch: {
    "queryParams.expenditureBusinessType": {
      handler(val) {
        this.getList();
      },
    },
    open: {
      deep: true,
      handler(newV) {
        if (!newV) {
          this.schemeId = null;
          this.form = {
            vendorBiddingListQuotationList: [],
          };
        }
      },
    },
    dialogVisible: {
      deep: true,
      handler(newV) {
        if (!newV) {
          this.radio = null;
        }
      },
    },
    schemeId: {
      handler(newV) {
        if (newV) {
          this.listContractSplit();
        }
      },
    },
    "form.splitId": {
      handler(newV) {
        if (newV) {
          this.$set(this.form, "vendorId", null);
          this.$set(this.form, "vendorBiddingListQuotationList", []);
          this.$nextTick(() => {
            this.$refs["vendorId"].clearValidate();
          });
          this.getAgreementSelectedProcurementInfo();
        }
      },
    },
    "form.vendorId": {
      handler(newV) {
        if (newV) {
          /* 获取供应商的报价清单 */
          this.listVendorBiddingListQuotation();
        }
      },
    },
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          console.log("进入到合同签订列表的project的方法里面-》》》》》》》》》》》》")
          this.queryParams.projectCode = newVal.code;
          this.queryParams_procurement.projectCode = newVal.code;
          this.queryParams_procurementYl.belongAccountingItemCode = newVal.code;
          this.getList();
          this.getList_yl();
        }
      },
      immediate: true,
    },
  },
  methods: {
    selectBcTemplate(row) {
      this.templateRow=row
    },
    goDetail(id, type) {
      // this.$router.push({
      //   path: "/procurement/contract-detail",
      //   query: { getId: id, type },
      // });
      let param = Base64.encode(JSON.stringify({ id, type }));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/contract-detail/${param}`);
    },
    goDetailBid(id, noticeId,procurementType,procurementTypeText) {
      // this.$router.push({
      //   path: "/procurement/contract-detail",
      //   query: { getId: id, type },
      // });
        let param = Base64.encode(JSON.stringify({ id, noticeId,procurementType }));
      param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      this.$router.push(`/procurement/tendering/${param}`);
    },
    submitFirstForm() {
      console.log(this.form, "this.form----0");
      this.$refs["form"].validate((valid, obj) => {
        let isNull = validatenull(obj);
        if (!isNull) {
          console.log(obj, "obj--obj");
          for (const [key, value] of Object.entries(obj)) {
            this.$message.error(value[0]);
            break;
          }
          return false;
        }
        if (valid) {
          const { schemeId, splitId, vendorId } = this.form;
          const formData = {
            schemeId,
            splitId,
            vendorId,
            agreementMaterialsList:
              this.form.vendorBiddingListQuotationList.map((item) => ({
                materialsListId: item.materialsListId,
                signCount: item.signCount,
                signUnitPriceInclTax: item.signUnitPriceInclTax,
              })),
          };
          checkAgreementCreateInfo(formData).then(() => {
            window.sessionStorage.setItem("contract", JSON.stringify(formData));
            this.$router.push({
              path: "/procurement/add-contract",
              query: {
                schemeId: this.form.schemeId,
                splitId: this.form.splitId,
                vendorId: this.form.vendorId,
                schemeName: this.form.schemeName,
              },
            });
            this.open = false;
          });
        }
      });
    },
    /* 获取供应商的报价清单 */
    listVendorBiddingListQuotation() {
      if (this.schemeId && this.form.vendorId && this.form.splitId) {
        this.loading_tax = true;
        this.form.vendorBiddingListQuotationList = [];
        listVendorBiddingListQuotation({
          schemeId: this.schemeId,
          vendorId: this.form.vendorId,
          splitId: this.form.splitId,
        }).then((res) => {
          this.loading_tax = false;
          this.form.vendorBiddingListQuotationList =
            res.data.vendorBiddingListQuotationList.map(item=>{
              this.$set(item,'signCount',item.surplusCount)
              this.$set(item,'signUnitPriceInclTax',item.taxUnitPrice)
              return item
            });
          (this.subjectMatter = res?.data.subjectMatter),
            (this.procurementType = res?.data.procurementType),
            (this.priceType = res?.data.priceType);
        });
      }
    },
    submitProcurementYl(){

        if(!this.selectedRowYl.length) return this.$message({type:'error',message:"请选择易料单据"});
        getCheckAgreementCreateInfo(this.selectedRowYl).then((res) => {
          if(res.code==200 && res.data==true){
            this.$router.push({
              path: "/procurement/add-contract",
              query: {
                id: this.selectedRowYl,
                agreementName: this.templateRow.agreementName,
                type:'add'
              },
            });
          }
        })


    },
    submitProcurement() {
      this.schemeId = this.selectedRow.schemeId;
      this.form.schemeId = this.schemeId;
      this.form.procurementSchemeName = this.selectedRow.procurementSchemeName;
      this.dialogVisible = false;

      this.$set(this.form, "splitId", "");
      this.$set(this.form, "vendorId", "");
      this.$nextTick(() => {
        this.$refs["splitId"].clearValidate();
        this.$refs["vendorId"].clearValidate();
      });

      this.$set(this.form, "vendorBiddingListQuotationList", []);
    },
    getAgreementSelectedProcurementInfo() {
      this.biddingVendorOptions = [];
      getAgreementSelectedProcurementInfo({
        schemeId: this.schemeId,
        contractSplitId: this.form.splitId,
      }).then((res) => {
        this.biddingVendorOptions = res?.data.biddingVendorList;
        const {
          upperLimitPriceText,
          procurementUpperLimitPriceText,
          usedTotalAmountText,
          surplusTotalAmountText,
        } = res?.data;
        this.planAmountInfo = {
          upperLimitPriceText,
          procurementUpperLimitPriceText,
          usedTotalAmountText,
          surplusTotalAmountText,
        };
      });
    },
    listContractSplit() {
      this.contractSplitOptions = [];
      listContractSplit({
        id: this.schemeId,
      }).then((res) => {
        this.contractSplitOptions = res?.data;
        console.log('%c🏀 新增合同弹窗-采购方案列表数据-this.contractSplitOptions \n', `font-size: 14px;background-color: #fe0;`, this.contractSplitOptions );
      });
    },
    handleSelectionChange(val) {
      console.log(val, "选中的");
      this.selectedRow = val;
    },
    handleSelectionChangeYl(val) {
      console.log(val, "选中的");
      this.selectedRowYl = val;
    },
    /**
     * 查询采购任务列表
     */
    getList_procurement() {
      listSignAgreementScheme(this.queryParams_procurement).then((res) => {
        this.loading_procurement = false;
        this.procurementTableList = res?.data?.rows;
        this.total_procurement = res?.data?.total;
      });
    },

    /**
     * 弹出选择采购任务
     */
    openSelectProcurement() {
      this.queryParams_procurement.pageNumber = 1;
      this.queryParams_procurement.pageSize = 10;

      this.getList_procurement();
      this.dialogVisible = true;
      this.loading_procurement = true;
    },
     /**
     * 查询易料列表
     */
     getList_yl() {
      listMarketMaterialContract(this.queryParams_procurementYl).then((res) => {
        this.loading_procurement_yl = false;
        this.procurementTableListYl = res?.data?.rows;
        this.total_procurement_yl = res?.data?.total;
      });
    },
    /**
     * 弹出易料选择
     */
     openSelectYl() {
      this.queryParams_procurementYl.pageNumber = 1;
      this.queryParams_procurementYl.pageSize = 10;
      this.getList_yl();
      this.contractVisibleYl = true;
      this.loading_procurement_yl = true;
    },

    /** 查询定时任务列表 */
    getList() {
      this.loading = true;
      const query = {
        ...this.queryParams,
        expenditureBusinessType:
          this.queryParams.expenditureBusinessType === "all"
            ? undefined
            : this.queryParams.expenditureBusinessType,
      };
      listAgreement(query).then((res) => {
        this.loading = false;
        this.planList = res.data.rows;
        this.total = res.data.total;
      });
      this.getList_procurement();
    },
    handleQuery_procurement() {
      this.queryParams_procurement.pageNumber = 1;
      this.selectedRow = {};
      this.getList_procurement();
    },
    searchYl(){
      this.queryParams_procurementYl.pageNumber = 1;
      this.getList_yl();
    },
    /** 重置按钮操作 */
    resetQuery_procurement() {
      this.resetForm("queryForm_procurement");
      this.handleQuery_procurement();
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNumber = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    reset() {
      this.form = {};
      this.resetForm("form");
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加任务";
    },
    /** 修改 */
    async goEdit(id, agreementName, type,procurementTypeText) {
      const res = await checkAgreementUpdate(id);
      // if(res.data){
      this.$router.push({
        path: "/procurement/edit-contract",
        query: {
          id: id,
          procurementTypeText:procurementTypeText?true:false,//编码没有值表示易料
        },
      });
      // }
    },
    //撤回
    revokeProcess(id, agreementName) {
      this.$confirm("是否确定撤回合同：" + agreementName, "提示", {
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

          revokeAgreement(id)
            .then((res) => {
              if (res.code == 200) {
                this.$message.success("撤回成功");
              }
              this.revokeLoding.close();
              this.getList();
            })
            .catch((e) => this.revokeLoding.close());
        } catch (error) {}
      });
    },
    setTag(tag) {
      this.selectedTag = tag;
      this.reviewText = tag; // 将选中的标签文本填入文本框
    },
    /** 提交 */
    goSubmit(id, type) {
      this.submitDialogVisible = true;
      this.contractId = id;
      this.contractType = type;
      // this.$confirm("是否确定提交合同：" + agreementName, "提示", {
      //   confirmButtonText: "确定",
      //   cancelButtonText: "取消",
      //   type: "warning",
      // }).then(async () => {
      //   let param = Base64.encode(JSON.stringify({id,type}));
      //   param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
      //   const detailUrl = `/procurement/contract-detail/${param}`
      //   try {
      //     await submitAgreement(id, detailUrl);
      //     this.$message.success("提交成功");
      //     this.getList();
      //   } catch (error) {}
      // });
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
        let param = Base64.encode(
          JSON.stringify({ id: this.contractId, type: this.contractType })
        );
        param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
        const detailUrl = `/procurement/contract-detail/${param}`;
        await submitAgreement({
          id: this.contractId,
          detailUrl,
          operateComment: this.reviewText,
        });
        this.$message.success("提交成功");
        this.getList();
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
    goCancellation(id, agreementName) {
      this.$confirm("是否确定作废合同：" + agreementName, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          await cancellationAgreement(id);
          this.$message.success("作废成功");
          this.getList();
        } catch (error) {}
      });
    },
    countDecimalPlaces(num) {
      // 将数字转换为字符串
      const numStr = num.toString();
      // 查找小数点的位置
      const decimalIndex = numStr.indexOf(".");
      // 如果小数点存在，计算小数位数
      if (decimalIndex !== -1) {
        return numStr.length - decimalIndex - 1;
      }
      // 如果没有小数点，返回 0
      return 0;
    },
    //千分位
    formatNumberWithThousandsSeparator(number) {
      // 将数字转为字符串
      let [integerPart, decimalPart] = number.toString().split(".");

      // 使用正则表达式在整数部分插入千分位分隔符
      let formattedIntegerPart = integerPart.replace(
        /\B(?=(\d{3})+(?!\d))/g,
        ","
      );

      // 如果有小数部分，将其附加到格式化后的整数部分
      return decimalPart !== undefined
        ? `${formattedIntegerPart}.${decimalPart}`
        : formattedIntegerPart;
    },
    /** 推送给供应商 */
    pushToVendor(id, agreementName, partyBName) {
      this.$confirm(
        "是否确定推送合同：" + agreementName + "至供应商:" + partyBName,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      ).then(async () => {
        try {
          await pushAgreementToVendor(id);
          this.$message.success("推送成功");
          this.getList();
        } catch (error) {}
      });
    },
    /** 推送合同至电子签章平台弹窗 */
    async pushToSignPlatform(id, agreementName, partyADeptId) {
      const res = await getPartyAUserList(partyADeptId);
      this.partyAUserList = res.data;
      this.pushSignFormData.id = id;
      this.pushSignTitle =
        "您确定推送合同：" + agreementName + "，至电子签章平台?";
      this.pushSignDialog = true;
    },
    /** 推送合同至电子签章平台 */
    toPushSignPlatform() {
      this.$refs["pushSignForm"].validate(async (valid) => {
        if (valid) {
          this.pushSignFormSubBtn = true;
          await pushAgreementToSignPlatform(this.pushSignFormData);
          this.$message({
            message: "推送成功",
            type: "success",
          });
          this.pushSignDialog = false;
          this.handleQuery();
          this.clearPushSignFormData();
        } else {
          return false;
        }
      });
    },
    clearPushSignFormData() {
      this.partyAUserList = [];
      this.pushSignFormData = {};
      this.pushSignFormSubBtn = false;
      this.$refs["pushSignForm"].clearValidate();
    },
    async toSignAgreement(id) {
      const rest = await signAgreement(id);
      this.signAgreementDialog = true;
      this.signAgreementUrl = rest.data;
    },
    toCancelledSignAgreementDialog(id, agreementName) {
      this.cancelledSignDialog = true;
      this.cancelledSignTitle =
        "你确定要作废已完成签署的合同：" + agreementName + "?";
      this.cancelledSignFormData.id = id;
    },
    toCancelledSignAgreement() {
      this.$refs["cancelledSignForm"].validate(async (valid) => {
        if (valid) {
          this.cancelledSignSubBtn = true;
          await cancelledSignAgreement(this.cancelledSignFormData);
          this.$message({
            message: "作废签署合同成功",
            type: "success",
          });

          this.cancelledSignDialog = false;
          this.handleQuery();
          this.clearCancelledSignFormData();
        } else {
          return false;
        }
      });
    },
    clearCancelledSignFormData() {
      this.cancelledSignFormData = {};
      this.cancelledSignSubBtn = false;
      this.$refs["cancelledSignForm"].clearValidate();
    },
    //获取易料订单
    openSelectMarket() {
      this.queryParams_market.pageNumber = 1;
      this.queryParams_market.pageSize = 10;

      this.getList_market();
      this.marketDialogVisible = true;
      // this.loading_market = true;
    },
    handleQuery_market() {
      this.queryParams_market.pageNumber = 1;
      this.selectedRowMarket = {};
      this.getList_market();
    },
    handleSelectionChangeMarket(val) {
      console.log(val, "选中的");
      this.selectedRowMarket = val;
    },
    //易料
    getList_market() {
      console.log("调接口");
      getMarketOrder(this.queryParams_market).then((res) => {
        this.loading_market = false;
        this.marketTableList = res?.data?.rows;
        this.total_market = res?.data?.total;
      });
    },
    submitMarket() {
      this.marketDialogVisible = false;
    },
  },
  computed: {
    ...mapGetters(["project"]),
    countComputed() {
      return (row, taxUnitPrice, taxRate, count, type) => {
        if (!taxUnitPrice || !taxRate || !count) return "0.00";

        function isNumber(str) {
          const numberPattern = /^-?\d+(\.\d+)?$/;
          return numberPattern.test(str);
        }

        if (!isNumber(taxUnitPrice) || !isNumber(count) || !isNumber(taxRate))
          return "0.00";

        const { add, divide, multiply, bignumber, format, subtract } =
          this.mathjs;

        const taxUnitPriceBig = bignumber(taxUnitPrice);
        const taxRateBig = bignumber(taxRate);
        const countBig = bignumber(count);

        // 计算税率百分比
        const taxRatePercent = divide(taxRateBig, 100);

        // 计算 (1 + 税率百分比)
        const onePlusTaxRate = add(1, taxRatePercent);

        // 计算不含税价格
        const notTaxedPrice = divide(taxUnitPriceBig, onePlusTaxRate);

        // 计算含税价格
        // const taxedPrice = multiply(notTaxedPrice, onePlusTaxRate);

        // 计算含税总价
        const taxedTotal = (
          Math.floor(multiply(taxUnitPriceBig, countBig) * 100) / 100
        ).toFixed(2);
        this.$set(row, "taxIncludedTotal", taxedTotal);
        // row.taxIncludedTotal = taxedTotal;

        //计算不合税总价

        const notTaxedTotal = (
          Math.floor(divide(taxedTotal, onePlusTaxRate) * 100) / 100
        ).toFixed(2);
        // row.notTaxedTotal = notTaxedTotal;
        this.$set(row, "notTaxedTotal", notTaxedTotal);

        // 根据 type 选择不同的结果
        let result;

        switch (type) {
          case "taxIncludedTotal":
            result = taxedTotal;
            break;
          case "excludingTax":
            result = notTaxedPrice;
            break;
          default:
            result = notTaxedTotal;
            break;
        }

        let formattedResult;

        let newRes = (taxUnitPrice.toString()).replace(/\.?0+$/, "");

        if (type === "excludingTax") {
          if (this.countDecimalPlaces(newRes) <= 2) {
            formattedResult = format(result, {
              notation: "fixed",
              precision: 2,
            });
          } else if (this.countDecimalPlaces(newRes) >= 3) {
            let resultLength = this.countDecimalPlaces(result.toString());
            if (resultLength === 3) {
              formattedResult = format(result, {
                notation: "fixed",
                precision: 3,
              });
            } else {
              formattedResult = format(result, {
                notation: "fixed",
                precision: 4,
              });
            }
          }
        } else {
          formattedResult = result
            .toString()
            .slice(0, result.toString().indexOf(".") + 3);
        }

        // formattedResult = parseFloat(formattedResult).toLocaleString("zh", {
        //   minimumFractionDigits: this.countDecimalPlaces(formattedResult) <= 2 && 2 || 4,
        //   maximumFractionDigits: 4,
        // });

        return this.formatNumberWithThousandsSeparator(formattedResult);
      };
    },
    totalTaxPriceTotal() {
      if (this.form.vendorBiddingListQuotationList?.length === 0) return 0.0;
      const { add, bignumber } = this.mathjs;
      let count = bignumber(0.0);
      this.form.vendorBiddingListQuotationList?.forEach((item) => {
        if (item.taxIncludedTotal) {
          count = add(count, bignumber(item.taxIncludedTotal));
        }
      });
      const newCount = (Math.floor(count * 100) / 100).toFixed(2);
      return newCount.toString().slice(0, newCount.toString().indexOf(".") + 3);
    },
    /* 计算本次不含税总计 */
    totalNotTaxPriceTotal() {
      if (this.form.vendorBiddingListQuotationList?.length === 0) return 0.0;
      const { add, bignumber } = this.mathjs;
      let count = bignumber(0.0);
      this.form.vendorBiddingListQuotationList?.forEach((item) => {
        if (item.notTaxedTotal) {
          count = add(count, bignumber(item.notTaxedTotal));
        }
      });
      return count.toString().slice(0, count.toString().indexOf(".") + 3);
    },
  },
};
</script>
<style>
/* 取消虚拟列表的偏移 */
.translateYisZero div{
  transform: translateY(0px) !important;
}
</style>
<style lang="scss" scoped>
.el-table .el-form-item {
  margin-bottom: 0;
}
.required {
  color: rgb(245, 108, 108);
  margin-right: 4px;
}
</style>
