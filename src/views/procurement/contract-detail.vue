<template>
  <div
    class="app-container"
    v-loading="fullLoading"
    element-loading-text="正在处理中..."
  >
    <BackButton path="/procurement/sign-contract" title="合同签订详情">
      <div class="button-container">
        <div
          v-if="
            isOperate === 1 &&
            (Number(agreementState) === 0 || Number(agreementState) === 5)
          "
        >
          <el-button
            type="primary"
            size="mini"
            @click="goSave"
            v-hasPermi="['procurement:contract:edit']"
            :disabled="isSubmit"
            :loading="isSubmit"
          >
            修改
          </el-button>
          <el-button
            type="primary"
            size="mini"
            @click="goSubmit"
            :disabled="isSubmit"
            :loading="isSubmit"
          >
            {{ isSubmit ? "提交中..." : "提交" }}
          </el-button>
          <el-button type="primary" size="mini" @click="goCancellation"
            >作废</el-button
          >
        </div>
        <div v-if="isOperate === 1 && Number(agreementState) === 1">
          <el-button type="primary" size="mini" @click="revokeProcess()"
            >撤回</el-button
          >
        </div>

        <div v-else-if="isOperate === 1 && Number(agreementState) === 3">
          <el-button type="primary" size="mini" @click="pushToVendor()"
            >推送至供应商</el-button
          >
        </div>
        <div v-else-if="isOperate === 1 && Number(agreementState) === 7">
          <el-button type="primary" size="mini" @click="pushToSignPlatform()"
            >推送至电子签章平台</el-button
          >
        </div>
        <div v-else-if="isOperate === 1 && Number(agreementState) === 9">
          <el-button type="primary" size="mini" @click="toSignAgreement()"
            >签署</el-button
          >
        </div>
        <div v-else-if="isOperate === 1 && Number(agreementState) === 10">
          <el-button
            type="primary"
            size="mini"
            @click="toCancelledSignAgreementDialog()"
            >作废签署合同</el-button
          >
        </div>

        <div class="contractApprovalButton">
          <el-button
            type="primary"
            size="mini"
            v-if="isShowButton"
            @click="handelSanction"
          >
            审批
          </el-button>
          <el-button
            type="primary"
            size="mini"
            v-if="isShowApprovalDetails"
            @click="handelCalibrationApproval"
          >
            审批详情
          </el-button>
        </div>
      </div>
    </BackButton>
    <div class="context">
      <el-tabs v-model="activeName">
        <el-tab-pane label="基本信息" name="first">
          <commonTitle> 基本信息 </commonTitle>
          <el-form :model="showInfo" class="form-container">
            <el-row
              v-for="(row, index) in chunkedBaseItemList"
              :key="index"
              class="custom-row"
            >
<!--              工程范围及工作内容特殊处理-->
              <el-col v-for="item in row" :key="item.id" :span="item.prop==='scopeOfWork'?24:8">
                <el-form-item
                  :label="item.label"
                  label-width="180px"
                  class="custom-form-item"
                >
                  <template v-if="item.type === 'link'">
                    <a
                      style="text-decoration: underline"
                      :href="computeHref(item)"
                      >{{ showInfo[item.prop] }}</a
                    >
                  </template>
                  <template v-else>
                    <span>{{
                      showInfo[item.prop]
                        ? showInfo[item.prop] + (item.unit || "")
                        : ""
                    }}</span>
                  </template>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
          <commonTitle style="margin-top: 20px"> 款项信息 </commonTitle>
          <el-form :model="showInfo" label-width="120px" class="form-container">
            <el-row
              v-for="(row, index) in chunkedPaymentItem"
              :key="index"
              class="custom-row"
            >
              <el-col v-for="item in row" :key="item.id" :span="8">
                <el-form-item
                  :label="item.label"
                  label-width="220px"
                  class="custom-form-item"
                >
                  <template v-if="item.type === 'link'">
                    <a
                      style="text-decoration: underline"
                      :href="computeHref(item)"
                      >{{ showInfo[item.prop] }}</a
                    >
                  </template>
                  <template v-else>
                    <span>{{
                      showInfo[item.prop]
                        ? showInfo[item.prop] + (item.unit || "")
                        : "-"
                    }}</span>
                  </template>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
          <commonTitle style="margin-top: 20px"> 结算与付款节点 </commonTitle>
          <el-table
            :data="agreementPaymentLists"
            highlight-current-row
            :header-cell-style="{ background: '#F3F2F8' }"
            style="width: 100%"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              v-for="(item, index) in settlementHeaderList"
              :key="index"
              :label="item.label"
              :prop="item.prop"
            >
            </el-table-column>
          </el-table>
          <commonTitle style="margin-top: 20px"> 合同清单 </commonTitle>
          <!-- 购买材料 -->
          <el-table
            :data="agreementMaterialsLists"
            highlight-current-row
            :header-cell-style="{ background: '#F3F2F8' }"
            style="width: 100%"
            v-if="Number(param.type) === 1"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              prop="materialsCode"
              label="物资编码"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="materialsName"
              label="物资名称"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="subjectMatterName"
              label="交易标的物"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="specification"
              label="规格型号"
              width="100"
              show-overflow-tooltip
            />
            <el-table-column prop="unitMeasurement" label="计量单位" />
            <el-table-column
              prop="brand"
              label="品牌"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="costAccount"
              label="成本科目"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="paymentTypeText"
              label="价款类型"
              width="100"
            />
            <el-table-column
              prop="countText"
              label="投标总量"
              width="100"
              align="right"
            />
            <el-table-column
              header-align="center"
              align="center"
              label="合同价"
            >
              <el-table-column
                prop="signTaxRateText"
                label="签订税率(%)"
                width="140"
                align="right"
              />
              <el-table-column
                prop="signCountText"
                label="签订量"
                width="100"
                align="right"
              />
              <el-table-column
                prop="signUnitPriceInclTaxText"
                label="签订含税单价(元)"
                width="180"
                align="right"
              />
              <el-table-column
                prop="signUnitPriceExclTaxText"
                label="签订不含税单价(元)"
                width="180"
                align="right"
              />
              <el-table-column
                prop="signAmountInclTaxText"
                label="签订含税总价(元)"
                width="180"
                align="right"
              />
              <el-table-column
                prop="signAmountExclTaxText"
                label="签订不含税总价(元)"
                width="180"
                align="right"
              />
              <el-table-column
                prop="signTaxAmountText"
                label="签订税额"
                width="140"
                align="right"
              />
            </el-table-column>
            <el-table-column
              header-align="center"
              align="center"
              label="中标价"
            >
              <el-table-column
                prop="notTaxUnitPriceText"
                label="不含税单价(元)"
                width="120"
                align="right"
                v-if="showInfo.priceType == 1"
              />
              <el-table-column
                prop="taxUnitPriceText"
                label="含税单价(元)"
                width="120"
                align="right"
                v-if="showInfo.priceType == 1"
              />
              <el-table-column
                prop="basePriceText"
                label="基价(元)"
                width="120"
                align="right"
                v-if="showInfo.priceType == 2"
              />
              <el-table-column
                prop="floatingPriceText"
                label="浮动价(元)"
                width="120"
                align="right"
                v-if="showInfo.priceType == 2"
              />
              <el-table-column
                prop="unloadingFeeText"
                label="装卸费"
                width="120"
                align="right"
                v-if="showInfo.priceType == 2"
              />
              <el-table-column
                prop="notTaxPriceText"
                label="不含税总价(元)"
                width="120"
                align="right"
              />
              <el-table-column
                prop="taxPriceText"
                label="含税总价(元)"
                width="120"
                align="right"
              />
              <el-table-column
                prop="taxRateText"
                label="税率(%)"
                width="100"
                align="center"
              />
              <el-table-column
                prop="taxAmountText"
                label="税额"
                width="120"
                align="right"
              />
            </el-table-column>

            <el-table-column prop="remark" label="备注" width="120" />
            <el-table-column prop="skuId" align="center" width="180" label="易料商品编码"/>
            <el-table-column prop="goodsName" align="center" width="180" label="易料商品名称"/>
              <el-table-column prop="offerBrand" align="center" width="180" label="易料品牌"/>
             <el-table-column prop="offerPrice" align="center" width="180" label="易料初始报价"/>
          </el-table>
          <!-- 租赁材料、租赁机械 -->
          <el-table
            :data="agreementMaterialsLists"
            highlight-current-row
            :header-cell-style="{ background: '#F3F2F8' }"
            style="width: 100%"
            v-if="Number(param.type) === 2 || Number(param.type) === 3"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              prop="materialsCode"
              label="物资编码"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="materialsName"
              label="物资名称"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="subjectMatterName"
              label="交易标的物"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="specification"
              label="规格型号"
              width="100"
              show-overflow-tooltip
            />
            <el-table-column prop="unitMeasurement" label="计量单位" />
            <el-table-column
              prop="brand"
              label="品牌"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column prop="rentModeText" label="租赁方式" width="100" />
            <el-table-column
              prop="rentalUnitText"
              label="计租单位"
              width="100"
            />
            <el-table-column
              prop="costAccount"
              label="成本科目"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="countText"
              label="工作量"
              width="100"
              align="right"
            />
            <el-table-column
              prop="rentTimeText"
              label="租赁时间"
              width="100"
              align="right"
            >
              <template slot-scope="scope">
                {{ scope.row.rentMode == 3 ? "-" : scope.row.rentTimeText }}
              </template>
            </el-table-column>
            <el-table-column
              prop="rentQuantityText"
              label="租赁数量"
              width="100"
              align="right"
            >
              <template slot-scope="scope">
                {{ scope.row.rentMode == 3 ? "-" : scope.row.rentQuantityText }}
              </template>
            </el-table-column>
            <el-table-column
              prop="signTaxRateText"
              label="本次签订税率(%)"
              width="140"
              align="right"
            />
            <el-table-column
              prop="signCountText"
              label="本次签订量"
              width="100"
              align="right"
            />
            <el-table-column
              prop="signUnitPriceInclTaxText"
              label="本次签订含税单价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signUnitPriceExclTaxText"
              label="本次签订不含税单价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signAmountInclTaxText"
              label="本次签订含税总价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signAmountExclTaxText"
              label="本次签订不含税总价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signTaxAmountText"
              label="本次签订税额"
              width="140"
              align="right"
            />
            <el-table-column
              prop="notTaxUnitPriceText"
              label="不含税单价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxUnitPriceText"
              label="含税单价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="notTaxPriceText"
              label="不含税总价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxPriceText"
              label="含税总价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxRate"
              label="税率(%)"
              width="100"
              align="center"
            />
            <el-table-column
              prop="taxAmountText"
              label="税额"
              width="120"
              align="right"
            />
            <el-table-column prop="remark" label="备注" width="120" />
          </el-table>
          <!-- 专业分包类、劳务分包类 -->
          <el-table
            :data="agreementMaterialsLists"
            highlight-current-row
            :header-cell-style="{ background: '#F3F2F8' }"
            style="width: 100%"
            v-if="Number(param.type) === 4 || Number(param.type) === 5"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              prop="materialsCode"
              label="物资编码"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="materialsName"
              label="物资名称"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="subjectMatterName"
              label="交易标的物"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="specification"
              label="特征值及特征项"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="unitMeasurement"
              label="计量单位"
              width="100"
              show-overflow-tooltip
            />
            <el-table-column
              prop="measurementRules"
              label="计量规则"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="workContent"
              label="基本工作内容"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="costAccount"
              label="成本科目"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="countText"
              label="数量"
              width="100"
              align="right"
            />
            <el-table-column
              prop="signTaxRateText"
              label="本次签订税率(%)"
              width="140"
              align="right"
            />
            <el-table-column
              prop="signCountText"
              label="本次签订量"
              width="100"
              align="right"
            />
            <el-table-column
              prop="signUnitPriceInclTaxText"
              label="本次签订含税单价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signUnitPriceExclTaxText"
              label="本次签订不含税单价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signAmountInclTaxText"
              label="本次签订含税总价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signAmountExclTaxText"
              label="本次签订不含税总价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signTaxAmountText"
              label="本次签订税额"
              width="140"
              align="right"
            />
            <el-table-column
              prop="notTaxUnitPriceText"
              label="不含税单价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxUnitPriceText"
              label="含税单价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="notTaxPriceText"
              label="不含税总价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxPriceText"
              label="含税总价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxRateText"
              label="税率(%)"
              width="100"
              align="center"
            />
            <el-table-column
              prop="taxAmountText"
              label="税额"
              width="120"
              align="right"
            />
            <el-table-column prop="remark" label="备注" width="120" />
          </el-table>
          <!-- 其它 -->
          <el-table
            :data="agreementMaterialsLists"
            highlight-current-row
            :header-cell-style="{ background: '#F3F2F8' }"
            style="width: 100%"
            v-if="Number(param.type) === 6"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              prop="materialsCode"
              label="物资编码"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="materialsName"
              label="物资名称"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="subjectMatterName"
              label="交易标的物"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="specification"
              label="特征值及特征项"
              width="100"
              show-overflow-tooltip
            />
            <el-table-column
              prop="unitMeasurement"
              label="计量单位"
              width="100"
            />
            <el-table-column
              prop="costAccount"
              label="成本科目"
              width="150"
              show-overflow-tooltip
            />
            <el-table-column
              prop="countText"
              label="数量"
              width="100"
              align="right"
            />
            <el-table-column
              prop="signTaxRateText"
              label="本次签订税率(%)"
              width="140"
              align="right"
            />
            <el-table-column
              prop="signCountText"
              label="本次签订量"
              width="100"
              align="right"
            />
            <el-table-column
              prop="signUnitPriceInclTaxText"
              label="本次签订含税单价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signUnitPriceExclTaxText"
              label="本次签订不含税单价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signAmountInclTaxText"
              label="本次签订含税总价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signAmountExclTaxText"
              label="本次签订不含税总价(元)"
              width="180"
              align="right"
            />
            <el-table-column
              prop="signTaxAmountText"
              label="本次签订税额"
              width="140"
              align="right"
            />
            <el-table-column
              prop="notTaxUnitPriceText"
              label="不含税单价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxUnitPriceText"
              label="含税单价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="notTaxPriceText"
              label="不含税总价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxPriceText"
              label="含税总价(元)"
              width="120"
              align="right"
            />
            <el-table-column
              prop="taxRateText"
              label="税率(%)"
              width="100"
              align="center"
            />
            <el-table-column
              prop="taxAmountText"
              label="税额"
              width="120"
              align="right"
            />
            <el-table-column prop="remark" label="备注" width="120" />
          </el-table>
          <!--专业分包/劳务分包类独有开始 -->
          <template v-if="Number(param.type) === 4 || Number(param.type) === 5">
            <commonTitle style="margin-top: 20px">
              合同外工程签证清单
            </commonTitle>
            <div class="item-title">计日工</div>
            <el-table
              :data="agreementDailyWageList"
              highlight-current-row
              :header-cell-style="{ background: '#F3F2F8' }"
              style="width: 100%"
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column
                v-for="(item, index) in dayHeaderList"
                :key="index"
                :label="item.label"
                :prop="item.prop"
              >
              </el-table-column>
            </el-table>
            <div class="item-title">机械台班</div>
            <el-table
              :data="agreementMachineShifts"
              highlight-current-row
              :header-cell-style="{ background: '#F3F2F8' }"
              style="width: 100%"
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column
                v-for="(item, index) in MachineShiftsHeaderList"
                :key="index"
                :label="item.label"
                :prop="item.prop"
              >
              </el-table-column>
            </el-table>
          </template>
          <template v-if="Number(param.type) === 4">
            <commonTitle style="margin-top: 20px"> 甲供设备清单 </commonTitle>
            <el-table
              :data="agreementEquipmentSupplies"
              highlight-current-row
              :header-cell-style="{ background: '#F3F2F8' }"
              style="width: 100%"
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column
                v-for="(item, index) in EquipmentSuppliesHeaderList"
                :key="index"
                :label="item.label"
                :prop="item.prop"
              >
              </el-table-column>
            </el-table>
            <commonTitle style="margin-top: 20px"> 甲供材料清单 </commonTitle>
            <el-table
              :data="agreementMaterialSupplies"
              highlight-current-row
              :header-cell-style="{ background: '#F3F2F8' }"
              style="width: 100%"
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
              />
              <el-table-column
                v-for="(item, index) in materialSuppliesHeaderList"
                :key="index"
                :label="item.label"
                :prop="item.prop"
              >
              </el-table-column>
            </el-table>
          </template>
          <!--专业分包/劳务分包类独有结束 -->
          <commonTitle style="margin-top: 20px"> 押金、保证金信息 </commonTitle>
          <el-table
            :data="agreementDeposits"
            highlight-current-row
            :header-cell-style="{ background: '#F3F2F8' }"
            style="width: 100%"
          >
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              v-for="(item, index) in depositHeaderList"
              :key="index"
              :label="item.label"
              :prop="item.prop"
            >
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="合同附件" name="second">
          <FileModule
            v-if="activeName === 'second' && this.attachmentId"
            :attachmentId="this.attachmentId"
            height="600px"
          />

          <div v-else style="font-size: 14px; text-align: center">
            <span>{{ this.attachmentMessage }}</span>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <ApprovalForm
      :visible.sync="sanctionVisible"
      :title="'合同签订审批流程'"
      :formModel="sanctionForm"
      :rejectNodeList="rejectNodeList"
      :nextCandidateList="nextCandidateList"
      :nextAppointable="nextAppointable"
      @update:visible="sanctionVisible = $event"
      @submit="handleSubmit"
    />
    <ApprovalDetailsDialog
      :visible.sync="calibrateVisible"
      title="合同签订审批流程详情"
      :activeStep="calibrateActive"
      :processInformationList="processInformationList"
      :approveLists="approveArr"
      :loading="calibrateLoading"
      @update:visible="calibrateVisible = $event"
    />

    <el-dialog
      title="推送至电子签章平台"
      :visible.sync="pushSignDialog"
      width="600px"
      @closed="clearPushSignFormData"
    >
      <span
        style="
          font-size: 16px;
          line-height: 30px;
          text-align: center;
          margin-bottom: 25px;
          display: block;
        "
        >{{ pushSignTitle }}</span
      >
      <el-form
        :model="pushSignFormData"
        ref="pushSignForm"
        :rules="pushSignFormRules"
      >
        <el-form-item label="签署人：" prop="partyAUserId">
          <el-select
            v-model="pushSignFormData.partyAUserId"
            placeholder="请选择"
            filterable
          >
            <el-option
              v-for="item in partyAUserList"
              :key="item.userId"
              :label="item.nickName"
              :value="item.userId"
            >
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="pushSignDialog = false">取 消</el-button>
        <el-button
          type="primary"
          @click="toPushSignPlatform"
          :loading="pushSignFormSubBtn"
          >{{ pushSignFormSubBtn ? "推送中..." : "推 送" }}</el-button
        >
      </div>
    </el-dialog>
    <el-dialog
      title="签署合同"
      :visible.sync="signAgreementDialog"
      width="1500px"
      @closed="closeSignAgreementDialog"
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
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import commonTitle from "@/views/procurement/components/common-title.vue";
import {
  getAgreementDetail,
  getAgreementAttachmentId,
  revokeAgreement,
  submitAgreement,
  cancellationAgreement,
  checkAgreementUpdate,
  getPartyAUserList,
  pushAgreementToSignPlatform,
  signAgreement,
  cancelledSignAgreement,
  pushAgreementToVendor,
} from "@/api/procurement/contract";
import { offerRepo, offerService } from "@/utils/const";
import FileModule from "@/components/FileModule/index.vue";
import BackButton from "@/components/BackButton/index.vue";
import Roam from "@/components/Roam";
import ApprovalForm from "@/components/Approval/approvalForm.vue";
import ApprovalDetailsDialog from "@/components/Approval/approvalDetailsDialog.vue";
import {
  getPermissionButton,
  postAuditProcess,
  getLoadTaskDef,
  getProcessLogList,
} from "@/api/procurement/manage";
export default {
  components: {
    commonTitle,
    FileModule,
    BackButton,
    Roam,
    ApprovalForm,
    ApprovalDetailsDialog,
  },

  data() {
    return {
      activeName: "first",
      offerService,
      isSave: false,
      offerRepo,
      attachmentId: "",
      agreementState: "",
      agreementName: "",
      attachmentMessage: "合同附件正在生成中，请稍后",
      approveLists: [], //审批信息
      approveNodeInfos: [], //审批人信息
      intervalId: "",
      sanctionVisible: false,
      sanctionForm: {
        pass: true,
        rejectTaskKey: "",
        operateComment: "",
      },
      rejectNodeList: [],
      /* 下一步审批人列表 */
      nextCandidateList: [],
      /* 下一步审批人 */
      nextAppointable: false,
      purchaserId: "",
      exampleId: "",
      taskPresentId: "",
      calibrateVisible: false,
      calibrateActive: 1,
      processInformationList: [],
      approveArr: [],
      calibrateLoading: false,
      isShowButton: false,
      isShowApprovalDetails: false,
      isOperate: 0,
      contractHeader: {
        1: [
          {
            id: 1,
            label: "物资编码",
            prop: "materialsCode",
            idth: "100",
            overflow: true,
          },
          {
            id: 2,
            label: "物资名称",
            prop: "materialsName",
            width: "150",
            overflow: true,
          },
          {
            id: 3,
            label: "规格型号",
            prop: "specification",
            width: "100",
            overflow: true,
          },
          {
            id: 4,
            label: "计量单位",
            prop: "unitMeasurement",
          },
          {
            id: 5,
            label: "品牌",
            prop: "brand",
            width: "150",
            overflow: true,
          },
          {
            id: 6,
            label: "成本科目",
            prop: "costAccount",
            width: "150",
            overflow: true,
          },
          {
            id: 7,
            label: "价款类型",
            prop: "paymentTypeText",
          },
          {
            id: 8,
            label: "数量",
            prop: "countText",
            width: "120",
            align: "right",
          },
          {
            id: 9,
            label: "税率(%)",
            prop: "taxRate",
            align: "center",
          },
          {
            id: 10,
            label: "不含税单价(元)",
            prop: "notTaxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 11,
            label: "含税单价(元)",
            prop: "taxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 12,
            label: "不含税总价(元)",
            prop: "notTaxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 13,
            label: "含税总价(元)",
            prop: "taxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 14,
            label: "税额",
            prop: "taxAmountText",
            width: "120",
            align: "right",
          },
          {
            id: 15,
            label: "备注",
            prop: "remark",
          },
        ],
        2: [
          {
            id: 1,
            label: "物资编码",
            prop: "materialsCode",
            width: "100",
            overflow: true,
          },
          {
            id: 2,
            label: "物资名称",
            prop: "materialsName",
            width: "150",
            overflow: true,
          },
          {
            id: 3,
            label: "规格型号",
            prop: "specification",
            width: "100",
            overflow: true,
          },
          {
            id: 4,
            label: "计量单位",
            prop: "unitMeasurement",
          },
          {
            id: 5,
            label: "品牌",
            prop: "brand",
            width: "150",
            overflow: true,
          },
          {
            id: 6,
            label: "租赁方式",
            prop: "rentModeText",
            width: "120",
          },
          {
            id: 7,
            label: "计租单位",
            prop: "rentalUnitText",
            width: "120",
          },
          {
            id: 8,
            label: "成本科目",
            prop: "costAccount",
            width: "150",
            overflow: true,
          },
          {
            id: 9,
            label: "工作量",
            prop: "countText",
            width: "120",
            align: "right",
          },
          {
            id: 10,
            label: "租赁时间",
            prop: "rentTimeText",
            width: "120",
            align: "right",
          },
          {
            id: 11,
            label: "租赁数量",
            prop: "rentQuantityText",
            width: "120",
            align: "right",
          },
          {
            id: 12,
            label: "税率(%)",
            prop: "taxRate",
            align: "center",
          },
          {
            id: 13,
            label: "不含税单价(元)",
            prop: "notTaxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 14,
            label: "含税单价(元)",
            prop: "taxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 15,
            label: "不含税总价(元)",
            prop: "notTaxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 16,
            label: "含税总价(元)",
            prop: "taxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 17,
            label: "税额",
            prop: "taxAmountText",
            width: "120",
            align: "right",
          },
          {
            id: 18,
            label: "备注",
            prop: "remark",
          },
        ],
        3: [
          {
            id: 1,
            label: "物资编码",
            prop: "materialsCode",
            width: "100",
            overflow: true,
          },
          {
            id: 2,
            label: "物资名称",
            prop: "materialsName",
            width: "150",
            overflow: true,
          },
          {
            id: 3,
            label: "规格型号",
            prop: "specification",
            width: "100",
            overflow: true,
          },
          {
            id: 4,
            label: "计量单位",
            prop: "unitMeasurement",
          },
          {
            id: 5,
            label: "品牌",
            prop: "brand",
            width: "150",
            overflow: true,
          },
          {
            id: 6,
            label: "租赁方式",
            prop: "rentModeText",
            width: "120",
          },
          {
            id: 7,
            label: "计租单位",
            prop: "rentalUnitText",
            width: "120",
          },
          {
            id: 8,
            label: "成本科目",
            prop: "costAccount",
            width: "150",
            overflow: true,
          },
          {
            id: 9,
            label: "工作量",
            prop: "countText",
            width: "120",
            align: "right",
          },
          {
            id: 10,
            label: "租赁时间",
            prop: "rentTimeText",
            width: "120",
            align: "right",
          },
          {
            id: 11,
            label: "租赁数量",
            prop: "rentQuantityText",
            width: "120",
            align: "right",
          },
          {
            id: 12,
            label: "税率(%)",
            prop: "taxRateText",
            align: "center",
          },
          {
            id: 13,
            label: "不含税单价(元)",
            prop: "notTaxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 14,
            label: "含税单价(元)",
            prop: "taxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 15,
            label: "不含税总价(元)",
            prop: "notTaxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 16,
            label: "含税总价(元)",
            prop: "taxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 17,
            label: "税额",
            prop: "taxAmountText",
            width: "120",
            align: "right",
          },
          {
            id: 18,
            label: "备注",
            prop: "remark",
          },
        ],
        4: [
          {
            id: 1,
            label: "物资编码",
            prop: "materialsCode",
            width: "100",
            overflow: true,
          },
          {
            id: 2,
            label: "物资名称",
            prop: "materialsName",
            width: "150",
            overflow: true,
          },
          {
            id: 3,
            label: "特征值及特征项",
            prop: "specification",
            width: "150",
            overflow: true,
          },
          {
            id: 4,
            label: "计量单位",
            prop: "unitMeasurement",
          },
          {
            id: 5,
            label: "计量规则",
            prop: "measurementRules",
          },
          {
            id: 6,
            label: "基本工作内容",
            prop: "workContent",
            width: "150",
            overflow: true,
          },
          {
            id: 7,
            label: "成本科目",
            prop: "costAccount",
            width: "150",
            overflow: true,
          },
          {
            id: 8,
            label: "数量",
            prop: "countText",
            width: "120",
            align: "right",
          },
          {
            id: 9,
            label: "税率(%)",
            prop: "taxRateText",
            align: "center",
          },
          {
            id: 10,
            label: "不含税单价(元)",
            prop: "notTaxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 11,
            label: "含税单价(元)",
            prop: "taxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 12,
            label: "不含税总价(元)",
            prop: "notTaxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 13,
            label: "含税总价(元)",
            prop: "taxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 14,
            label: "税额",
            prop: "taxAmountText",
            width: "120",
            align: "right",
          },
          {
            id: 15,
            label: "备注",
            prop: "remark",
          },
        ],
        5: [
          {
            id: 1,
            label: "物资编码",
            prop: "materialsCode",
            width: "100",
            overflow: true,
          },
          {
            id: 2,
            label: "物资名称",
            prop: "materialsName",
            width: "150",
            overflow: true,
          },
          {
            id: 3,
            label: "特征值及特征项",
            prop: "specification",
            width: "150",
            overflow: true,
          },
          {
            id: 4,
            label: "计量单位",
            prop: "unitMeasurement",
          },
          {
            id: 5,
            label: "计量规则",
            prop: "measurementRules",
          },
          {
            id: 6,
            label: "基本工作内容",
            prop: "workContent",
            width: "150",
            overflow: true,
          },
          {
            id: 7,
            label: "成本科目",
            prop: "costAccount",
            width: "150",
            overflow: true,
          },
          {
            id: 8,
            label: "数量",
            prop: "countText",
            width: "120",
            align: "right",
          },
          {
            id: 9,
            label: "税率(%)",
            prop: "taxRateText",
            align: "center",
          },
          {
            id: 10,
            label: "不含税单价(元)",
            prop: "notTaxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 11,
            label: "含税单价(元)",
            prop: "taxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 12,
            label: "不含税总价(元)",
            prop: "notTaxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 13,
            label: "含税总价(元)",
            prop: "taxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 14,
            label: "税额",
            prop: "taxAmountText",
            width: "120",
            align: "right",
          },
          {
            id: 15,
            label: "备注",
            prop: "remark",
          },
        ],
        6: [
          {
            id: 1,
            label: "物资编码",
            prop: "materialsCode",
            width: "100",
            overflow: true,
          },
          {
            id: 2,
            label: "物资名称",
            prop: "materialsName",
            width: "150",
            overflow: true,
          },
          {
            id: 3,
            label: "特征值及特征项",
            prop: "specification",
            width: "150",
            overflow: true,
          },
          {
            id: 4,
            label: "计量单位",
            prop: "unitMeasurement",
          },
          {
            id: 5,
            label: "成本科目",
            prop: "costAccount",
            width: "150",
            overflow: true,
          },
          {
            id: 6,
            label: "数量",
            prop: "countText",
            width: "120",
            align: "right",
          },
          {
            id: 7,
            label: "税率(%)",
            prop: "taxRateText",
            align: "center",
          },
          {
            id: 8,
            label: "不含税单价(元)",
            prop: "notTaxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 9,
            label: "含税单价(元)",
            prop: "taxUnitPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 10,
            label: "不含税总价(元)",
            prop: "notTaxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 11,
            label: "含税总价(元)",
            prop: "taxPriceText",
            width: "120",
            align: "right",
          },
          {
            id: 12,
            label: "税额",
            prop: "taxAmountText",
            width: "120",
            align: "right",
          },
          {
            id: 13,
            label: "备注",
            prop: "remark",
          },
        ],
      },
      settlementHeaderList: [
        {
          id: 1,
          label: "付款条件/结算与付款节点",
          prop: "paymentName",
        },
        {
          id: 2,
          label: "付款基数",
          prop: "paymentBasisText",
        },
        {
          id: 3,
          label: "约定付款比例(%)",
          prop: "paymentRatioText",
        },
        {
          id: 4,
          label: "约定付款金额",
          prop: "paymentAmountText",
        },
        {
          id: 5,
          label: "付款说明",
          prop: "paymentRemark",
        },
        {
          id: 6,
          label: "当前付款节点",
          prop: "currentPaymentPointText",
        },
      ],
      depositHeaderList: [
        {
          id: 1,
          label: "押金/保证金类型",
          prop: "depositTypeText",
        },
        {
          id: 2,
          label: "押金/保证金方式",
          prop: "depositWayText",
        },
        {
          id: 3,
          label: "押金/保证金基数",
          prop: "depositBaseAmountText",
        },
        {
          id: 4,
          label: "约定押金/保证金比例(%)",
          prop: "depositRatioText",
        },
        {
          id: 5,
          label: "约定押金/保证金金额",
          prop: "depositAmountText",
        },
        {
          id: 6,
          label: "返还条件",
          prop: "returnCondition",
        },
        {
          id: 7,
          label: "备注",
          prop: "remark",
        },
      ],
      dayHeaderList: [
        {
          id: 1,
          label: "工种名称",
          prop: "jobTitleName",
        },
        {
          id: 2,
          label: "计量单位",
          prop: "unitMeasurement",
        },
        {
          id: 3,
          label: "税率(%)",
          prop: "taxRateText",
        },
        {
          id: 4,
          label: "单价(不含税)",
          prop: "unitPriceExcTaxText",
        },
        {
          id: 5,
          label: "单价(含税)",
          prop: "unitPriceIncTaxText",
        },
        {
          id: 6,
          label: "备注",
          prop: "remark",
        },
      ], //计日工表头
      agreementDailyWageList: [], //计日工数据
      //机械台班表头
      MachineShiftsHeaderList: [
        {
          id: 1,
          label: "设备名称",
          prop: "equipmentName",
        },
        {
          id: 2,
          label: "规格型号",
          prop: "specification",
        },
        {
          id: 3,
          label: "计量单位",
          prop: "unitMeasurement",
        },
        {
          id: 4,
          label: "税率(%)",
          prop: "taxRateText",
        },
        {
          id: 5,
          label: "单价(不含税)",
          prop: "unitPriceExcTaxText",
        },
        {
          id: 6,
          label: "单价(含税)",
          prop: "unitPriceIncTaxText",
        },
        {
          id: 7,
          label: "备注",
          prop: "remark",
        },
      ],
      agreementMachineShifts: [], // 机械台班
      // 甲供设备清单表头
      EquipmentSuppliesHeaderList: [
        {
          id: 1,
          label: "设备名称",
          prop: "equipmentName",
        },
        {
          id: 2,
          label: "规格型号",
          prop: "specification",
        },
        {
          id: 3,
          label: "计量单位",
          prop: "unitMeasurement",
        },
        {
          id: 4,
          label: "预估数量",
          prop: "estimatedCountText",
        },
        {
          id: 5,
          label: "预估税率(%)",
          prop: "estimatedTaxRateText",
        },
        {
          id: 6,
          label: "预估单价(不含税)",
          prop: "estimatedUnitPriceExcTaxText",
        },
        {
          id: 7,
          label: "预估单价(含税)",
          prop: "estimatedUnitPriceIncTaxText",
        },
        {
          id: 8,
          label: "预估金额(不含税)",
          prop: "estimatedAmountExcTaxText",
        },
        {
          id: 9,
          label: "预估金额(含税)",
          prop: "estimatedAmountIncTaxText",
        },
        {
          id: 10,
          label: "预估税额",
          prop: "estimatedTaxAmountText",
        },
        {
          id: 11,
          label: "备注",
          prop: "remark",
        },
      ],
      agreementEquipmentSupplies: [], //甲供设备清单对象
      // 甲供材料清单表头
      materialSuppliesHeaderList: [
        {
          id: 1,
          label: "物资名称",
          prop: "materialName",
        },
        {
          id: 2,
          label: "规格型号",
          prop: "specification",
        },
        {
          id: 3,
          label: "计量单位",
          prop: "unitMeasurement",
        },
        {
          id: 4,
          label: "预估数量",
          prop: "estimatedCountText",
        },
        {
          id: 5,
          label: "预估税率(%)",
          prop: "estimatedTaxRateText",
        },
        {
          id: 6,
          label: "预估单价(不含税)",
          prop: "estimatedUnitPriceExcTaxText",
        },
        {
          id: 7,
          label: "预估单价(含税)",
          prop: "estimatedUnitPriceIncTaxText",
        },
        {
          id: 8,
          label: "预估金额(不含税)",
          prop: "estimatedAmountExcTaxText",
        },
        {
          id: 9,
          label: "预估金额(含税)",
          prop: "estimatedAmountIncTaxText",
        },
        {
          id: 10,
          label: "预估税额",
          prop: "estimatedTaxAmountText",
        },
        {
          id: 11,
          label: "备注",
          prop: "remark",
        },
      ],
      agreementMaterialSupplies: [], //甲供材料清单对象
      //* 合同清单
      agreementMaterialsLists: [],
      agreementPaymentLists: [], //结算与付款节点
      agreementDeposits: [], // 押金、保证金信息
      // * 基本信息
      showInfo: {},
      baseItemList: {
        1: [
          {
            id: 1,
            label: "归属本级组织：",
            prop: "belongOrganizationName",
          },
          {
            id: 2,
            label: "归属最小核算项目：",
            prop: "belongAccountingItem",
            span: 1,
          },
          {
            id: 3,
            label: "归属最小核算项目编码：",
            prop: "belongAccountingItemCode",
            span: 1,
          },
          {
            id: 4,
            label: "合同名称：",
            prop: "agreementName",
            span: 1,
          },
          {
            id: 5,
            label: "合同编码：",
            prop: "agreementCode",
            span: 1,
          },
          {
            id: 6,
            label: "单位内部合同管理编码：",
            prop: "innerAgreementCode",
            span: 1,
          },
          {
            id: 7,
            label: "甲方名称：",
            prop: "partyAName",
            span: 1,
          },
          {
            id: 8,
            label: "乙方名称：",
            prop: "partyBName",
            span: 1,
          },
          {
            id: 9,
            label: "支出业务分类：",
            prop: "expenditureBusinessTypeText",
            span: 1,
          },
          {
            id: 10,
            label: "交易标的物：",
            prop: "subjectMatterName",
            span: 1,
          },
          {
            id: 11,
            label: "支付周期：",
            prop: "paymentCycleText",
            span: 1,
          },
          {
            id: 12,
            label: "支付方式：",
            prop: "paymentWayText",
            span: 1,
          },
          {
            id: 13,
            label: "乙方法人代表：",
            prop: "partyBLegalName",
            span: 1,
          },
          {
            id: 14,
            label: "身份证：",
            prop: "partyBLegalIdCard",
            span: 1,
          },
          {
            id: 15,
            label: "联系方式：",
            prop: "partyBLegalPhone",
            span: 1,
          },
          {
            id: 16,
            label: "乙方现场实际履职负责人：",
            prop: "partyBResponsibleName",
            span: 1,
          },
          {
            id: 17,
            label: "身份证：",
            prop: "partyBResponsibleIdCard",
            span: 1,
          },
          {
            id: 18,
            label: "联系方式：",
            prop: "partyBResponsiblePhone",
            span: 1,
          },
          {
            id: 19,
            label: "国家地区代码(履行地)：",
            prop: "agreementPerformCountry",
            span: 1,
          },
          {
            id: 20,
            label: "行政区划代码(履行地)：",
            prop: "agreementPerformDistrict",
            span: 1,
          },
          {
            id: 21,
            label: "合同履行地：",
            prop: "agreementPerformAddress",
            span: 1,
          },
        ],
        2: [
          {
            id: 1,
            label: "归属本级组织：",
            prop: "belongOrganizationName",
          },
          {
            id: 2,
            label: "归属最小核算项目：",
            prop: "belongAccountingItem",
            span: 1,
          },
          {
            id: 3,
            label: "归属最小核算项目编码：",
            prop: "belongAccountingItemCode",
            span: 1,
          },
          {
            id: 4,
            label: "合同名称：",
            prop: "agreementName",
            span: 1,
          },
          {
            id: 5,
            label: "合同编码：",
            prop: "agreementCode",
            span: 1,
          },
          {
            id: 6,
            label: "单位内部合同管理编码：",
            prop: "innerAgreementCode",
            span: 1,
          },
          {
            id: 7,
            label: "甲方名称：",
            prop: "partyAName",
            span: 1,
          },
          {
            id: 8,
            label: "乙方名称：",
            prop: "partyBName",
            span: 1,
          },
          {
            id: 9,
            label: "支出业务分类：",
            prop: "expenditureBusinessTypeText",
            span: 1,
          },
          {
            id: 10,
            label: "交易标的物：",
            prop: "subjectMatterName",
            span: 1,
          },
          {
            id: 11,
            label: "支付周期：",
            prop: "paymentCycleText",
            span: 1,
          },
          {
            id: 12,
            label: "支付方式：",
            prop: "paymentWayText",
            span: 1,
          },
          {
            id: 13,
            label: "乙方法人代表：",
            prop: "partyBLegalName",
            span: 1,
          },
          {
            id: 14,
            label: "身份证：",
            prop: "partyBLegalIdCard",
            span: 1,
          },
          {
            id: 15,
            label: "联系方式：",
            prop: "partyBLegalPhone",
            span: 1,
          },
          {
            id: 16,
            label: "乙方现场实际履职负责人：",
            prop: "partyBResponsibleName",
            span: 1,
          },
          {
            id: 17,
            label: "身份证：",
            prop: "partyBResponsibleIdCard",
            span: 1,
          },
          {
            id: 18,
            label: "联系方式：",
            prop: "partyBResponsiblePhone",
            span: 1,
          },
          {
            id: 19,
            label: "国家地区代码(履行地)：",
            prop: "agreementPerformCountry",
            span: 1,
          },
          {
            id: 20,
            label: "行政区划代码(履行地)：",
            prop: "agreementPerformDistrict",
            span: 1,
          },
          {
            id: 21,
            label: "合同履行地：",
            prop: "agreementPerformAddress",
            span: 1,
          },
          {
            id: 22,
            label: "计租方式：",
            prop: "rentalMethodText",
            span: 1,
          },
        ],
        3: [
          {
            id: 1,
            label: "归属本级组织：",
            prop: "belongOrganizationName",
          },
          {
            id: 2,
            label: "归属最小核算项目：",
            prop: "belongAccountingItem",
            span: 1,
          },
          {
            id: 3,
            label: "归属最小核算项目编码：",
            prop: "belongAccountingItemCode",
            span: 1,
          },
          {
            id: 4,
            label: "合同名称：",
            prop: "agreementName",
            span: 1,
          },
          {
            id: 5,
            label: "合同编码：",
            prop: "agreementCode",
            span: 1,
          },
          {
            id: 6,
            label: "单位内部合同管理编码：",
            prop: "innerAgreementCode",
            span: 1,
          },
          {
            id: 7,
            label: "甲方名称：",
            prop: "partyAName",
            span: 1,
          },
          {
            id: 8,
            label: "乙方名称：",
            prop: "partyBName",
            span: 1,
          },
          {
            id: 9,
            label: "支出业务分类：",
            prop: "expenditureBusinessTypeText",
            span: 1,
          },
          {
            id: 10,
            label: "交易标的物：",
            prop: "subjectMatterName",
            span: 1,
          },
          {
            id: 11,
            label: "支付周期：",
            prop: "paymentCycleText",
            span: 1,
          },
          {
            id: 12,
            label: "支付方式：",
            prop: "paymentWayText",
            span: 1,
          },
          {
            id: 13,
            label: "乙方法人代表：",
            prop: "partyBLegalName",
            span: 1,
          },
          {
            id: 14,
            label: "身份证：",
            prop: "partyBLegalIdCard",
            span: 1,
          },
          {
            id: 15,
            label: "联系方式：",
            prop: "partyBLegalPhone",
            span: 1,
          },
          {
            id: 16,
            label: "乙方现场实际履职负责人：",
            prop: "partyBResponsibleName",
            span: 1,
          },
          {
            id: 17,
            label: "身份证：",
            prop: "partyBResponsibleIdCard",
            span: 1,
          },
          {
            id: 18,
            label: "联系方式：",
            prop: "partyBResponsiblePhone",
            span: 1,
          },
          {
            id: 19,
            label: "国家地区代码(履行地)：",
            prop: "agreementPerformCountry",
            span: 1,
          },
          {
            id: 20,
            label: "行政区划代码(履行地)：",
            prop: "agreementPerformDistrict",
            span: 1,
          },
          {
            id: 21,
            label: "合同履行地：",
            prop: "agreementPerformAddress",
            span: 1,
          },
          {
            id: 22,
            label: "计租方式：",
            prop: "rentalMethodText",
            span: 1,
          },
        ],
        4: [
          {
            id: 1,
            label: "归属本级组织：",
            prop: "belongOrganizationName",
          },
          {
            id: 2,
            label: "归属最小核算项目：",
            prop: "belongAccountingItem",
            span: 1,
          },
          {
            id: 3,
            label: "归属最小核算项目编码：",
            prop: "belongAccountingItemCode",
            span: 1,
          },
          {
            id: 4,
            label: "合同名称：",
            prop: "agreementName",
            span: 1,
          },
          {
            id: 5,
            label: "合同编码：",
            prop: "agreementCode",
            span: 1,
          },
          {
            id: 6,
            label: "单位内部合同管理编码：",
            prop: "innerAgreementCode",
            span: 1,
          },
          {
            id: 7,
            label: "甲方名称：",
            prop: "partyAName",
            span: 1,
          },
          {
            id: 8,
            label: "乙方名称：",
            prop: "partyBName",
            span: 1,
          },
          {
            id: 9,
            label: "支出业务分类：",
            prop: "expenditureBusinessTypeText",
            span: 1,
          },
          {
            id: 10,
            label: "交易标的物：",
            prop: "subjectMatterName",
            span: 1,
          },
          {
            id: 11,
            label: "支付周期：",
            prop: "paymentCycleText",
            span: 1,
          },
          {
            id: 12,
            label: "支付方式：",
            prop: "paymentWayText",
            span: 1,
          },
          {
            id: 13,
            label: "乙方法人代表：",
            prop: "partyBLegalName",
            span: 1,
          },
          {
            id: 14,
            label: "身份证：",
            prop: "partyBLegalIdCard",
            span: 1,
          },
          {
            id: 15,
            label: "联系方式：",
            prop: "partyBLegalPhone",
            span: 1,
          },
          {
            id: 16,
            label: "乙方现场实际履职负责人：",
            prop: "partyBResponsibleName",
            span: 1,
          },
          {
            id: 17,
            label: "身份证：",
            prop: "partyBResponsibleIdCard",
            span: 1,
          },
          {
            id: 18,
            label: "联系方式：",
            prop: "partyBResponsiblePhone",
            span: 1,
          },
          {
            id: 19,
            label: "进场日期：",
            prop: "entryDate",
            span: 1,
          },
          {
            id: 20,
            label: "完工日期：",
            prop: "finishDate",
            span: 1,
          },
          {
            id: 21,
            label: "工期(天)：",
            prop: "duration",
            span: 1,
          },
          {
            id: 22,
            label: "国家地区代码(履行地)：",
            prop: "agreementPerformCountry",
            span: 1,
          },
          {
            id: 23,
            label: "行政区划代码(履行地)：",
            prop: "agreementPerformDistrict",
            span: 1,
          },
          {
            id: 24,
            label: "合同履行地：",
            prop: "agreementPerformAddress",
            span: 1,
          },
          {
            id: 25,
            label: "价格形式：",
            prop: "priceFormText",
            span: 1,
          },
          {
            id: 26,
            label: "下浮比例(%)：",
            prop: "discountRatio",
            span: 2,
          },
          {
            id: 27,
            label: "工程范围及工作内容：",
            prop: "scopeOfWork",
            span: 3,
          },
        ],
        5: [
          {
            id: 1,
            label: "归属本级组织：",
            prop: "belongOrganizationName",
          },
          {
            id: 2,
            label: "归属最小核算项目：",
            prop: "belongAccountingItem",
            span: 1,
          },
          {
            id: 3,
            label: "归属最小核算项目编码：",
            prop: "belongAccountingItemCode",
            span: 1,
          },
          {
            id: 4,
            label: "合同名称：",
            prop: "agreementName",
            span: 1,
          },
          {
            id: 5,
            label: "合同编码：",
            prop: "agreementCode",
            span: 1,
          },
          {
            id: 6,
            label: "单位内部合同管理编码：",
            prop: "innerAgreementCode",
            span: 1,
          },
          {
            id: 7,
            label: "甲方名称：",
            prop: "partyAName",
            span: 1,
          },
          {
            id: 8,
            label: "乙方名称：",
            prop: "partyBName",
            span: 1,
          },
          {
            id: 9,
            label: "支出业务分类：",
            prop: "expenditureBusinessTypeText",
            span: 1,
          },
          {
            id: 10,
            label: "交易标的物：",
            prop: "subjectMatterName",
            span: 1,
          },
          {
            id: 11,
            label: "支付周期：",
            prop: "paymentCycleText",
            span: 1,
          },
          {
            id: 12,
            label: "支付方式：",
            prop: "paymentWayText",
            span: 1,
          },
          {
            id: 13,
            label: "乙方法人代表：",
            prop: "partyBLegalName",
            span: 1,
          },
          {
            id: 14,
            label: "身份证：",
            prop: "partyBLegalIdCard",
            span: 1,
          },
          {
            id: 15,
            label: "联系方式：",
            prop: "partyBLegalPhone",
            span: 1,
          },
          {
            id: 16,
            label: "乙方现场实际履职负责人：",
            prop: "partyBResponsibleName",
            span: 1,
          },
          {
            id: 17,
            label: "身份证：",
            prop: "partyBResponsibleIdCard",
            span: 1,
          },
          {
            id: 18,
            label: "联系方式：",
            prop: "partyBResponsiblePhone",
            span: 1,
          },
          {
            id: 19,
            label: "进场日期：",
            prop: "entryDate",
            span: 1,
          },
          {
            id: 20,
            label: "完工日期：",
            prop: "finishDate",
            span: 1,
          },
          {
            id: 21,
            label: "工期(天)：",
            prop: "duration",
            span: 1,
          },
          {
            id: 22,
            label: "国家地区代码(履行地)：",
            prop: "agreementPerformCountry",
            span: 1,
          },
          {
            id: 23,
            label: "行政区划代码(履行地)：",
            prop: "agreementPerformDistrict",
            span: 1,
          },
          {
            id: 24,
            label: "合同履行地：",
            prop: "agreementPerformAddress",
            span: 1,
          },
          {
            id: 25,
            label: "工程范围及工作内容：",
            prop: "scopeOfWork",
            span: 3,
          },
        ],
        6: [
          {
            id: 1,
            label: "归属本级组织：",
            prop: "belongOrganizationName",
          },
          {
            id: 2,
            label: "归属最小核算项目：",
            prop: "belongAccountingItem",
            span: 1,
          },
          {
            id: 3,
            label: "归属最小核算项目编码：",
            prop: "belongAccountingItemCode",
            span: 1,
          },
          {
            id: 4,
            label: "合同名称：",
            prop: "agreementName",
            span: 1,
          },
          {
            id: 5,
            label: "合同编码：",
            prop: "agreementCode",
            span: 1,
          },
          {
            id: 6,
            label: "单位内部合同管理编码：",
            prop: "innerAgreementCode",
            span: 1,
          },
          {
            id: 7,
            label: "甲方名称：",
            prop: "partyAName",
            span: 1,
          },
          {
            id: 8,
            label: "乙方名称：",
            prop: "partyBName",
            span: 1,
          },
          {
            id: 9,
            label: "支出业务分类：",
            prop: "expenditureBusinessTypeText",
            span: 1,
          },
          {
            id: 10,
            label: "交易标的物：",
            prop: "subjectMatterName",
            span: 1,
          },
          {
            id: 11,
            label: "支付周期：",
            prop: "paymentCycleText",
            span: 1,
          },
          {
            id: 12,
            label: "支付方式：",
            prop: "paymentWayText",
            span: 1,
          },
          {
            id: 13,
            label: "乙方法人代表：",
            prop: "partyBLegalName",
            span: 1,
          },
          {
            id: 14,
            label: "身份证：",
            prop: "partyBLegalIdCard",
            span: 1,
          },
          {
            id: 15,
            label: "联系方式：",
            prop: "partyBLegalPhone",
            span: 1,
          },
          {
            id: 16,
            label: "乙方现场实际履职负责人：",
            prop: "partyBResponsibleName",
            span: 1,
          },
          {
            id: 17,
            label: "身份证：",
            prop: "partyBResponsibleIdCard",
            span: 1,
          },
          {
            id: 18,
            label: "联系方式：",
            prop: "partyBResponsiblePhone",
            span: 1,
          },
          {
            id: 19,
            label: "合同履行开始日期：",
            prop: "contractStartDate",
            span: 1,
          },
          {
            id: 20,
            label: "合同履行结束日期：",
            prop: "contractEndDate",
            span: 1,
          },
          {
            id: 21,
            label: "工期(天)：",
            prop: "duration",
            span: 1,
          },
          {
            id: 22,
            label: "国家地区代码(履行地)：",
            prop: "agreementPerformCountry",
            span: 1,
          },
          {
            id: 23,
            label: "行政区划代码(履行地)：",
            prop: "agreementPerformDistrict",
            span: 1,
          },
          {
            id: 24,
            label: "合同履行地：",
            prop: "agreementPerformAddress",
            span: 1,
          },
        ],
      },
      //款项信息
      paymentItem: {
        1: [
          {
            id: 1,
            label: "币种：",
            prop: "currencyText",
          },
          {
            id: 2,
            label: "发票类型：",
            prop: "invoiceTypeText",
          },
          {
            id: 3,
            label: "合同税率(%)：",
            prop: "contractTaxRateText",
          },
          {
            id: 4,
            label: "约定预付款比例(%)：",
            prop: "prepaymentRatioText",
          },
          {
            id: 5,
            label: "约定预付款金额(元)：",
            prop: "prepaymentAmountText",
          },
          {
            id: 6,
            label: "预付款扣回条件：",
            prop: "prepaymentDeductionConditions",
          },
          {
            id: 7,
            label: "预付款全部扣回截止点(%)：",
            prop: "prepaymentDeductionDeadlineText",
          },
          {
            id: 8,
            label: "允许合同外结算占合同比例(%)：",
            prop: "outOfSettlementRatioText",
          },
          {
            id: 9,
            label: "是否关联我的钢铁网价格：",
            prop: "isRelatedMySteelText",
          },
          {
            id: 10,
            label: "我的钢铁网价格浮动值：",
            prop: "mySteelPriceFluctuationText",
          },
          {
            id: 11,
            label: "合同签订金额(含税)：",
            prop: "totalAmountIncTaxText",
          },
          {
            id: 12,
            label: "合同签订金额(不含税)：",
            prop: "totalAmountExcTaxText",
          },
        ],
        2: [
          {
            id: 1,
            label: "币种：",
            prop: "currencyText",
          },
          {
            id: 2,
            label: "发票类型：",
            prop: "invoiceTypeText",
          },
          {
            id: 3,
            label: "合同税率(%)：",
            prop: "contractTaxRateText",
          },
          {
            id: 4,
            label: "约定预付款比例(%)：",
            prop: "prepaymentRatioText",
          },
          {
            id: 5,
            label: "约定预付款金额(元)",
            prop: "prepaymentAmountText",
          },
          {
            id: 6,
            label: "预付款扣回条件：",
            prop: "prepaymentDeductionConditions",
          },
          {
            id: 7,
            label: "预付款全部扣回截止点(%)：",
            prop: "prepaymentDeductionDeadlineText",
          },
          {
            id: 8,
            label: "允许合同外结算占合同比例(%)：",
            prop: "outOfSettlementRatioText",
          },
          {
            id: 9,
            label: "合同签订金额(含税)：",
            prop: "totalAmountIncTaxText",
          },
          {
            id: 10,
            label: "合同签订金额(不含税)：",
            prop: "totalAmountExcTaxText",
          },
        ],
        3: [
          {
            id: 1,
            label: "币种：",
            prop: "currencyText",
          },
          {
            id: 2,
            label: "发票类型：",
            prop: "invoiceTypeText",
          },
          {
            id: 3,
            label: "合同税率(%)：",
            prop: "contractTaxRateText",
          },
          {
            id: 4,
            label: "约定预付款比例(%)：",
            prop: "prepaymentRatioText",
          },
          {
            id: 5,
            label: "约定预付款金额(元)",
            prop: "prepaymentAmountText",
          },
          {
            id: 6,
            label: "预付款扣回条件：",
            prop: "prepaymentDeductionConditions",
          },
          {
            id: 7,
            label: "预付款全部扣回截止点(%)：",
            prop: "prepaymentDeductionDeadlineText",
          },
          {
            id: 8,
            label: "允许合同外结算占合同比例(%)：",
            prop: "outOfSettlementRatioText",
          },
          {
            id: 9,
            label: "停滞台班结算比例(%)：",
            prop: "stagnationRatioText",
          },
          {
            id: 10,
            label: "合同签订金额(含税)：",
            prop: "totalAmountIncTaxText",
          },
          {
            id: 11,
            label: "合同签订金额(不含税)：",
            prop: "totalAmountExcTaxText",
          },
        ],
        4: [
          {
            id: 1,
            label: "币种：",
            prop: "currencyText",
          },
          {
            id: 2,
            label: "发票类型：",
            prop: "invoiceTypeText",
          },
          {
            id: 3,
            label: "合同税率(%)：",
            prop: "contractTaxRateText",
          },
          {
            id: 4,
            label: "约定预付款比例(%)：",
            prop: "prepaymentRatioText",
          },
          {
            id: 5,
            label: "约定预付款金额(元)",
            prop: "prepaymentAmountText",
          },
          {
            id: 6,
            label: "预付款扣回条件：",
            prop: "prepaymentDeductionConditions",
          },
          {
            id: 7,
            label: "预付款全部扣回截止点(%)：",
            prop: "prepaymentDeductionDeadlineText",
          },
          {
            id: 8,
            label: "允许合同外结算占合同比例(%)：",
            prop: "outOfSettlementRatioText",
          },
          {
            id: 9,
            label: "合同签订金额(含税)：",
            prop: "totalAmountIncTaxText",
          },
          {
            id: 10,
            label: "合同签订金额(不含税)：",
            prop: "totalAmountExcTaxText",
          },
        ],
        5: [
          {
            id: 1,
            label: "币种：",
            prop: "currencyText",
          },
          {
            id: 2,
            label: "发票类型：",
            prop: "invoiceTypeText",
          },
          {
            id: 3,
            label: "合同税率(%)：",
            prop: "contractTaxRateText",
          },
          {
            id: 4,
            label: "约定预付款比例(%)：",
            prop: "prepaymentRatioText",
          },
          {
            id: 5,
            label: "约定预付款金额(元)",
            prop: "prepaymentAmountText",
          },
          {
            id: 6,
            label: "预付款扣回条件：",
            prop: "prepaymentDeductionConditions",
          },
          {
            id: 7,
            label: "预付款全部扣回截止点(%)：",
            prop: "prepaymentDeductionDeadlineText",
          },
          {
            id: 8,
            label: "允许合同外结算占合同比例(%)：",
            prop: "outOfSettlementRatioText",
          },
          {
            id: 9,
            label: "合同签订金额(含税)：",
            prop: "totalAmountIncTaxText",
          },
          {
            id: 10,
            label: "合同签订金额(不含税)：",
            prop: "totalAmountExcTaxText",
          },
        ],
        6: [
          {
            id: 1,
            label: "币种：",
            prop: "currencyText",
          },
          {
            id: 2,
            label: "发票类型：",
            prop: "invoiceTypeText",
          },
          {
            id: 3,
            label: "合同税率(%)：",
            prop: "contractTaxRateText",
          },
          {
            id: 4,
            label: "约定预付款比例(%)：",
            prop: "prepaymentRatioText",
          },
          {
            id: 5,
            label: "约定预付款金额(元)",
            prop: "prepaymentAmountText",
          },
          {
            id: 6,
            label: "预付款扣回条件：",
            prop: "prepaymentDeductionConditions",
          },
          {
            id: 7,
            label: "预付款全部扣回截止点(%)：",
            prop: "prepaymentDeductionDeadlineText",
          },
          {
            id: 8,
            label: "允许合同外结算占合同比例(%)：",
            prop: "outOfSettlementRatioText",
          },
          {
            id: 9,
            label: "合同签订金额(含税)：",
            prop: "totalAmountIncTaxText",
          },
          {
            id: 10,
            label: "合同签订金额(不含税)：",
            prop: "totalAmountExcTaxText",
          },
        ],
      },
      isSubmit: false,
      param: {},
      partyADeptId: "",
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
      fullLoading: false,
      submitDialogVisible: false,
      contractId: "",
      contractType: "",
      reviewText: "",
      selectedTag: null,
      tags: ["拟同意", "同意", "请修改, 再传至我处理", "阅"],
      partyBName: "",
    };
  },
  computed: {
    computeHref() {
      return (item) =>
        `javascript:POBrowser.openWindow('/pageoffice','width=1800px;height=900px;','${
          this.showInfo[item.src]
        }/Y')`;
    },
    active() {
      return this.approveNodeInfos.reduce(
        (pre, cur) => (cur.state === 1 ? pre + 1 : pre),
        0
      );
    },
    chunkedBaseItemList() {
      const items = this.baseItemList[Number(this.param.type)];
      return this.chunkArray(items, 3);
    },
    chunkedPaymentItem() {
      const items = this.paymentItem[Number(this.param.type)];
      return this.chunkArray(items, 3);
    },
  },
  mounted() {
    const param = JSON.parse(Base64.decode(this.$route.params.params));
    this.param = param;
    this.getContractDetail();
    this.intervalId = setInterval(this.loadAgreementAttachmentId, 3000);
  },
  methods: {
    getContractDetail() {
      this.fullLoading = true;
      getAgreementDetail({
        id: this.param.id,
      })
        .then((res) => {
        this.showInfo = {
            ...res.data.agreement,
            ...res.data.agreementPaymentItem,
          };
          this.exampleId = res.data.agreement.wfProcessId;
          this.isShowApprovalDetails = res.data.agreement.wfProcessId
            ? true
            : false;
          this.agreementMaterialsLists = res.data.materialsList;
          this.agreementPaymentLists = res.data.agreementPaymentLists;
          this.agreementDeposits = res.data.agreementDeposits;
          this.approveNodeInfos = res.data.approveNodeInfos;
          this.approveLists = res.data.approveLists;
          this.attachmentId = res.data.agreement.agreementAttachmentId;
          this.agreementState = res.data.agreement.agreementState;
          this.agreementName = res.data.agreement.agreementName;
          this.isOperate = res.data.agreement.isOperate;
          this.partyADeptId = res.data.agreement.partyADeptId;
          this.partyBName = res.data.agreement.partyBName;
          this.fullLoading = false;
          (this.agreementDailyWageList =
            res.data?.agreementDailyWageList || []),
            (this.agreementMachineShifts =
              res.data?.agreementMachineShifts || []),
            (this.agreementEquipmentSupplies =
              res.data?.agreementEquipmentSupplies || []);
          this.agreementMaterialSupplies =
            res.data?.agreementMaterialSupplies || [];
        })
        .then(() => {
          this.getPermissionButton();
        });
    },
    async loadAgreementAttachmentId() {
      const agreementId = this.param.id;
      if (agreementId) {
        const res = await getAgreementAttachmentId(agreementId);
        if (res.data) {
          if (res.data.attachmentId) {
            this.attachmentId = res.data.attachmentId;
            clearInterval(this.intervalId);
          } else {
            this.attachmentMessage = res.data.message;
          }
        }
      }
    },
    async getPermissionButton() {
      try {
        this.purchaserId = this.param.id;
        if (this.purchaserId && this.exampleId) {
          const res = await getPermissionButton({
            businessId: this.purchaserId,
            processId: this.exampleId,
          });
          this.rejectNodeList = res.data.completedTaskList;
          /* 下一步审批人列表 */
          this.nextCandidateList = res.data.nextCandidateList;
          /* 下一步审批人是否可选 */
          this.nextAppointable = res.data.nextAppointable;
          this.taskPresentId = res.data.curTaskId;
          this.isShowButton = res.data.auditable;
        }
      } catch (error) {}
    },
    handelSanction() {
      this.sanctionVisible = true;
      this.getPermissionButton();
    },
    handleSubmit() {
      const params = {
        ...this.sanctionForm,
        businessId: this.purchaserId,
        processId: this.exampleId,
        curTaskId: this.taskPresentId,
        processKey: "jiantou-zhaocai:{org}:ZHAOCAI_AGREEMENT_SIGN",
      };
      const loading = this.$loading({
        lock: true,
        text: "正在提交...",
        background: "rgba(0, 0, 0, 0.7)",
      });
      postAuditProcess(params).then(() => {
        this.$message.success("提交成功");
        this.sanctionVisible = false;
        this.getContractDetail();
      }).catch(error => {
        /* 关闭遮罩层 */
        this.$modal.closeLoading();
      }).finally(() => {
        // 关闭加载遮罩层
        loading.close();
      });
    },
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        const params = {
          businessId: this.purchaserId,
          processId: this.exampleId,
        };
        if (this.purchaserId && this.exampleId) {
          const res = await getLoadTaskDef(params);
          this.processInformationList = res.data;
          function getActive(nodes) {
            let allFalse = true;
            for (let i = 0; i < nodes.length; i++) {
              if (!nodes[i].completed) {
                if (i === 0) {
                  return 0;
                } else {
                  return i;
                }
              }
              allFalse = false;
            }
            return nodes.length;
          }
          this.calibrateActive = getActive(this.processInformationList);
          const response = await getProcessLogList(params);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
    chunkArray(arr, chunkSize) {
      if (!Array.isArray(arr) || arr.length === 0) return;
      const result = [];
      for (let i = 0; i < arr.length; i += chunkSize) {
        result.push(arr.slice(i, i + chunkSize));
      }
      return result;
    },

    // 保存
    async goSave() {
      try {
        await checkAgreementUpdate(this.param.id);
        this.$router.push({
          path: "/procurement/edit-contract",
          query: {
            id: this.param.id,
          },
        });
      } catch (err) {
        console.log(err);
      }
    },
    setTag(tag) {
      this.selectedTag = tag;
      this.reviewText = tag; // 将选中的标签文本填入文本框
    },
    /** 提交 */
    goSubmit() {
      this.submitDialogVisible = true;
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
        this.fullLoading = true;
        const detailUrl = this.$route.fullPath;
        await submitAgreement({
          id: this.param.id,
          detailUrl,
          operateComment: this.reviewText,
        });
        this.$message.success("提交成功");
        this.getContractDetail();
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
    //撤回
    revokeProcess() {
      let this_ = this;
      this.$confirm("确定是否撤回合同：" + this.agreementName, "提示", {
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
          revokeAgreement(this_.param.id)
            .then((res) => {
              if (res.code == 200) {
                this.$message.success("撤回成功");
              }
              this.revokeLoding.close();
              this.getContractDetail();
            })
            .catch((e) => this.revokeLoding.close());
        } catch (error) {}
      });
    },
    /** 作废 **/
    goCancellation() {
      this.$confirm("确定是否提交合同：" + this.agreementName, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        try {
          await cancellationAgreement(this.param.id);
          this.$message.success("作废成功");
          this.getContractDetail();
        } catch (error) {}
      });
    },
    async pushToSignPlatform() {
      const res = await getPartyAUserList(this.partyADeptId);
      this.partyAUserList = res.data;
      this.pushSignFormData.id = this.param.id;
      this.pushSignTitle =
        "您确定推送合同：" + this.agreementName + "，至电子签章平台?";
      this.pushSignDialog = true;
    },
    clearPushSignFormData() {
      this.partyAUserList = [];
      this.pushSignFormData = {};
      this.pushSignFormSubBtn = false;
      this.$refs["pushSignForm"].clearValidate();
    },
    pushToVendor() {
      this.$confirm(
        "是否确定推送合同：" +
          this.agreementName +
          "至供应商:" +
          this.partyBName,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      ).then(async () => {
        try {
          await pushAgreementToVendor(this.param.id);
          this.$message.success("推送成功");
          this.getContractDetail();
        } catch (error) {}
      });
    },
    toPushSignPlatform() {
      this.$refs["pushSignForm"].validate(async (valid) => {
        if (valid) {
          this.pushSignFormSubBtn = true;
          try {
            await pushAgreementToSignPlatform(this.pushSignFormData);
            this.$message({
              message: "推送成功",
              type: "success",
            });
            this.pushSignDialog = false;
            this.getContractDetail();
          } catch (ex) {
            this.pushSignFormSubBtn = false;
            this.$refs["pushSignForm"].clearValidate();
          }
        } else {
          return false;
        }
      });
    },
    async toSignAgreement() {
      const rest = await signAgreement(this.param.id);
      this.signAgreementDialog = true;
      this.signAgreementUrl = rest.data;
    },
    closeSignAgreementDialog() {
      this.getContractDetail();
    },
    toCancelledSignAgreementDialog() {
      this.cancelledSignDialog = true;
      this.cancelledSignTitle =
        "你确定要作废已完成签署的合同：" + this.agreementName + "?";
      this.cancelledSignFormData.id = this.param.id;
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
          this.getContractDetail();
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
  },
  destroyed() {
    clearInterval(this.intervalId);
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
  font-size: 13px;
  /* 修改字体大小 */
  font-weight: bolder;
  /* 修改字体粗细 */
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

.step_item {
  font-size: 14px;
  padding-bottom: 32px;
  margin-top: 32px;
}

.item-title {
  padding: 10px 0;
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

::v-deep .step_item {
  margin: 10px 0;
}
.button-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.button-container > div {
  display: flex;
  align-items: center;
}

.contractApprovalButton {
  margin-left: 10px;
}
.custom-row {
  display: flex;
  flex-wrap: wrap;
  line-height: 36px; /* 设置行高 */
}

.custom-col {
  display: flex;
  align-items: center; /* 垂直居中对齐内容 */
  height: 36px; /* 确保每列的高度与行高一致 */
}

.custom-form-item {
  margin-bottom: 0; /* 删除底部间距 */
}

.custom-form-item > .el-form-item__content {
  line-height: 36px; /* 设置内容的行高 */
  height: 36px; /* 确保内容高度与行高一致 */
  display: flex;
  align-items: center; /* 垂直居中对齐内容 */
}
.required {
  color: rgb(245, 108, 108);
  margin-right: 4px;
}
</style>
