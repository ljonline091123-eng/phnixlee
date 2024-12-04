<template>
  <div class="app-container">
    <BackButton
      path="/procurement/scheme"
      :title="isEdit ? '修改采购方案' : '新增采购方案'"
    >
      <div>
        <el-button
          type="primary"
          plain
          size="mini"
          @click="$router.replace('/procurement/scheme')"
          >取消</el-button
        >
        <el-button
          type="primary"
          size="mini"
          @click="submitForm('form')"
          :disabled="isSubmit"
          :loading="isSubmit"
          >{{ isSubmit ? "提交中..." : "保存" }}</el-button
        >
      </div>
    </BackButton>
    <div
      style="
        background: #fff;
        min-height: calc(100vh - 50px - 34px - 16px - 16px);
        padding: 16px;
        box-sizing: border-box;
      "
    >
      <el-form
        :model="formData"
        ref="form"
        :rules="rules"
        label-position="right"
        label-width="150px"
        size="medium"
        label-suffix=":"
      >
        <div class="form-box">
          <el-tabs v-model="activeTabs" @tab-click="handleTypeClick">
            <el-tab-pane label="基本信息" name="base">
              <div class="pd20">
                <el-row :gutter="40">
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label=" 编号"
                      prop="procurementSchemeCode"
                      class="required label-right-align"
                    >
                      <el-input
                        type="text"
                        clearable
                        disabled
                        v-model="formData.procurementSchemeCode"
                        placeholder="系统自动为您生成"
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label=" 方案名称"
                      prop="procurementSchemeName"
                      class="required label-right-align"
                    >
                      <el-input
                        v-model="formData.procurementSchemeName"
                        type="text"
                        clearable
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label="采购人"
                      prop="procurementOfficerName"
                      class="required label-right-align"
                    >
                      <el-input
                        type="text"
                        clearable
                        :readonly="true"
                        disabled
                        v-model="formData.procurementOfficerName"
                        placeholder="系统自动为您生成"
                      ></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="40">
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label=" 采购方式"
                      prop="procurementType"
                      class="required label-right-align"
                    >
                      <!-- <el-input v-model="formData.procurementPlanName" type="text" clearable></el-input> -->
                      <el-select
                        v-model="formData.procurementType"
                        placeholder="请选择采购方式"
                        style="width: 100%"
                      >
                        <el-option
                          v-for="dict in dict.type.procurement_type"
                          :key="dict.value"
                          :label="dict.label"
                          :value="dict.value"
                        ></el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label="上限价(元)"
                      prop="ceilingPrice"
                      disabled
                      class="required label-right-align"
                    >
                      <el-input
                        v-model="formData.ceilingPrice"
                        disabled
                        type="text"
                        clearable
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item label="指导价格(元)" prop="guidance_price">
                      <el-input
                        v-model="formData.guidance_price"
                        type="text"
                        placeholder="对接易料市集"
                        clearable
                        disabled
                      ></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="40">
                  <el-col :span="8" class="grid-cell">
                    <el-form-item label="保证金" prop="isReceiveDeposit">
                      <el-radio-group
                        v-model="formData.isReceiveDeposit"
                        @input="marginInput"
                      >
                        <el-radio label="1">收取</el-radio>
                        <el-radio label="2">不收取</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell" v-if="isShow">
                    <el-form-item label="保证金金额(元)" prop="securityDeposit">
                      <el-input
                        v-model="formData.securityDeposit"
                        type="text"
                        clearable
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell" v-if="isShow">
                    <el-form-item
                      label="选择财务确认人员"
                      prop="financeConfirmId"
                    >
                      <el-select
                        v-model="formData.financeConfirmId"
                        placeholder="请选择财务人员"
                        filterable
                        style="width: 100%"
                        @change="changeFinance"
                      >
                        <el-option
                          v-for="item in financeList"
                          :key="item.userId"
                          :label="item.nickName"
                          :value="item.userId"
                        ></el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="40">
                  <el-col :span="8" class="grid-cell">
                    <el-form-item label="交易标的物" prop="subjectMatterName">
                      <el-input
                        v-model="formData.subjectMatterName"
                        type="text"
                        clearable
                        disabled
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label="价格类型"
                      v-if="
                        formData.subjectMatterType == 1 ||
                        formData.subjectMatterType == 2
                      "
                      prop="priceTypeText"
                      class="required label-right-align"
                    >
                      <el-input
                        v-model="formData.priceTypeText"
                        type="text"
                        clearable
                        disabled
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col
                    :span="8"
                    class="grid-cell"
                    v-if="formData.subjectMatterType == 1"
                  >
                    <el-form-item label="计数方式" prop="countingTypeText">
                      <el-input
                        v-model="formData.countingTypeText"
                        type="text"
                        clearable
                        disabled
                      ></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="40">
                  <el-col
                    :span="8"
                    class="grid-cell"
                    v-if="formData.subjectMatterType == 1"
                  >
                    <el-form-item
                      label=" 付款方式"
                      prop="paymentTypeText"
                      class="required label-right-align"
                    >
                      <el-input
                        v-model="formData.paymentTypeText"
                        type="text"
                        clearable
                        disabled
                      ></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>
              <div class="page-title">
                <span>清单</span>
              </div>
              <div class="pd20">
                <el-table
                  v-loading="loading"
                  :data="contractList"
                  stripe
                  border
                  size="small"
                >
                  <el-table-column
                    label="序号"
                    type="index"
                    width="50"
                    align="center"
                  />
                  <el-table-column
                    label="合约规划名称"
                    prop="contractPlanningName"
                    min-width="200"
                    show-overflow-tooltip
                  />
                  <el-table-column
                    label="规划金额(含税)"
                    width="200"
                    align="right"
                    prop="plannedAmountInclTaxText"
                  />
                  <el-table-column
                    label="已发生规划金额(含税)"
                    width="200"
                    align="right"
                    prop="incurredPlannedAmountText"
                  />
                  <el-table-column
                    label="规划余量(元)"
                    width="200"
                    align="right"
                    prop="planningBalanceText"
                  />
                  <el-table-column
                    label="拟定招标方式"
                    width="200"
                    align="center"
                    prop="biddingMethod"
                  />
                  <el-table-column label="清单" align="center" width="120">
                    <template slot-scope="{ row }">
                      <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-view"
                        @click="handelInventory(row)"
                        >查看清单</el-button
                      >
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
            <el-tab-pane label="招标文件" name="file">
              <div class="pd20">
                <el-row :gutter="40">
                  <el-col :span="8" class="grid-cell">
                    <!-- <el-form-item label=" 计划投标截止时间" prop="bidDeadline" class="required label-right-align">
                      <el-date-picker v-model="formData.bidDeadline" type="date" style="width:100%" placeholder="选择日期"
                        :picker-options="expireTimeOption" format="yyyy年MM月dd日" value-format="yyyy-MM-dd" />
                    </el-form-item> -->

                    <el-form-item
                      label="计划投标截止时间"
                      prop="bidDeadline"
                      class="required label-right-align"
                    >
                      <el-date-picker
                        v-model="formData.bidDeadline"
                        type="datetime"
                        style="width: 100%"
                        placeholder="选择日期"
                         popper-class="date-clear"
                        :picker-options="endTimeOptions"
                        value-format="yyyy-MM-dd HH:mm:ss"
                        @change="handleChange"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="40">
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label="联系人"
                      prop="bidContactPerson"
                      class="required label-right-align"
                    >
                      <el-input
                        v-model="formData.bidContactPerson"
                        type="text"
                        clearable
                        placeholder="请输入联系人"
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label="联系电话"
                      prop="bidContactPhone"
                      class="required label-right-align"
                    >
                      <el-input
                        type="text"
                        clearable
                        v-model="formData.bidContactPhone"
                        :maxlength="11"
                        placeholder="请输入联系电话"
                      ></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item
                      label="联系邮箱"
                      prop="bidContactEmail"
                      class="required label-right-align"
                    >
                      <el-input
                        v-model="formData.bidContactEmail"
                        type="text"
                        clearable
                      ></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="40">
                  <el-col :span="8" class="grid-cell">
                    <el-form-item label="评分模板" prop="templateName">
                      <template v-if="formData.templateName">
                        <a href="javascript:;" @click="getTemplateList">{{
                          formData.templateName
                        }}</a>
                      </template>
                      <el-button
                        v-else
                        size="small"
                        type="primary"
                        @click="getTemplateList"
                        >选择模板</el-button
                      >
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" class="grid-cell">

                    <el-form-item
                      label=" 招标文件模板"
                      prop="biddingTemplateName"
                    >
                      <template v-if="formData.biddingTemplateName">
                        <a href="javascript:;" @click="getBcTemplateList(2)">{{
                          formData.biddingTemplateName
                        }}</a>
                      </template>
                      <el-button
                        v-if="formData.biddingTemplateName"
                        size="mini"
                        @click="modifyTempFile(2)"
                      >修改附件</el-button>

                      <el-button
                        v-else
                        size="small"
                        type="primary"
                        @click="getBcTemplateList(2)"
                        >选择模板</el-button
                      >

                      <br>
                      <div style="margin-left: -90px;width: 300px;">
                        <!--  先选择模板后再去手动上传附件模板，优先保证系统数据能拥有tempId的值吧，然后判断审批状态是否可上传 -->
                        <el-button size="mini" type="primary" v-show="formData.biddingTemplateName && (state === null || (state !== 1 && state !== 2 && state !== 3))" @click="uploadBiddingClick">手动上传</el-button>
                        <el-upload
                          style="margin-left: 90px;margin-top: -75px;"
                          :action="uploadFileUrl"
                          :limit="1"
                          :on-success="fileSuccessBidding"
                          :file-list="formData.fileListBidding"
                          :on-remove="fileRemoveBidding"
                          ref="uploadBidding"
                        >
                        </el-upload>
                      </div>
                    </el-form-item>


                  </el-col>
                  <el-col :span="8" class="grid-cell">
                    <el-form-item label=" 合同模板" prop="contractTemplateName">
                      <template v-if="formData.contractTemplateName">
                        <a href="javascript:;" @click="getBcTemplateList(1)">{{
                          formData.contractTemplateName
                        }}</a>
                      </template>
                      <el-button
                        v-if="formData.contractTemplateName"
                        size="mini"
                        @click="modifyTempFile(1)"
                      >修改附件</el-button>
                      <el-button
                        v-else
                        size="small"
                        type="primary"
                        @click="getBcTemplateList(1)"
                        >选择模板</el-button
                      >
                      <br>
                      <div style="margin-left: -90px;width: 300px;">
                        <!--  先选择模板后再去手动上传附件模板，优先保证系统数据能拥有tempId的值吧，然后判断审批状态是否可上传 -->
                        <el-button size="mini" type="primary" v-show="formData.contractTemplateName && (state === null || (state !== 1 && state !== 2 && state !== 3))" @click="uploadContractClick">手动上传</el-button>
                        <el-upload
                          style="margin-left: 90px;margin-top: -75px;"
                          :action="uploadFileUrl"
                          :limit="1"
                          :on-success="fileSuccessContract"
                          :file-list="formData.fileListContract"
                          :on-remove="fileRemoveContract"
                          ref="uploadContract"
                        >
                        </el-upload>
                      </div>
                    </el-form-item>
                  </el-col>


                </el-row>
                <!-- 在线预览 -->
                <div class="previewFile">
                  <div class="page-title">
                    <span>文件预览</span>
                  </div>
                  <!-- <FileModule
                    v-if="viewAttachmentId"
                    :attachmentId="viewAttachmentId"
                    height= "95%"
                    type="edit"
                  /> -->
                  <iframe
                    v-if="viewAttachmentId"
                    :src= this.editFileUrl
                    width="100%"
                    height="500px"
                    frameborder="0"
                  ></iframe>

                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-form>
      <!-- 查看清单 -->
      <!-- 选择项目合约规划 -->
      <el-dialog title="清单" :visible.sync="inventoryVisible" width="70%">
        <el-table v-loading="loading" :data="inventoryList" border size="small">
          <el-table-column
            label="序号"
            type="index"
            width="50"
            align="center"
          />
          <el-table-column
            label="拆分合约规划名称"
            width="200"
            prop="splitContractName"
            show-overflow-tooltip
          />
          <el-table-column
            label="拟签约合同拆包范围"
            width="200"
            prop="contractScope"
            show-overflow-tooltip
          />
          <el-table-column label="清单" align="center">
            <template slot-scope="inventory">
              <el-table
                size="small"
                :data="inventory.row.materialsLists"
                border
              >
                <el-table-column
                  label="序号"
                  type="index"
                  width="50"
                  align="center"
                />
                <el-table-column
                  label="清单编码"
                  width="150"
                  align="center"
                  prop="materialsCode"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="清单名称"
                  width="200"
                  prop="materialsName"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="交易标的物"
                  width="200"
                  prop="subjectMatterName"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="规格型号"
                  prop="specification"
                  show-overflow-tooltip
                />
                <el-table-column
                  label="计量单位"
                  align="center"
                  prop="unitMeasurement"
                />
                <el-table-column
                  v-if="isLease"
                  label="租赁方式"
                  align="right"
                  prop="rentModeText"
                />
                <el-table-column
                  :label="isLease ? '工作量' : '清单数量'"
                  align="right"
                  prop="countText"
                />
                <el-table-column
                  label="基价(元)"
                  v-if="formData.priceType == 2"
                  align="right"
                  prop="basePriceText"
                />
                <el-table-column
                  label="单价(含税)"
                  v-else
                  align="right"
                  prop="unitPriceInclTaxText"
                  width="100"
                />
                <el-table-column
                  label="浮动价(元)"
                  v-if="formData.priceType == 2"
                  align="right"
                  prop="floatingPriceText"
                />
                <el-table-column
                  label="装卸费(元)"
                  v-if="formData.priceType == 2"
                  align="right"
                  prop="unloadingFeeText"
                />
                <el-table-column
                  v-if="isLease"
                  label="租赁时间"
                  align="right"
                  prop="rentTimeText"
                >
                  <template slot-scope="{ row }">
                    {{ row.rentMode == 3 ? "-" : row.rentTimeText }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isLease"
                  label="租赁数量"
                  align="right"
                  prop="rentQuantityText"
                >
                  <template slot-scope="{ row }">
                    {{ row.rentMode == 3 ? "-" : row.rentQuantityText }}
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>
      <!-- 选择评分模板 -->
      <el-dialog
        title="选择评分模板"
        class="dialogClass"
        :visible.sync="evaluateVisable"
        width="60%"
      >
        <el-tabs v-model="currentTab" @tab-click="onTabClick">
          <el-tab-pane label="通用模板" name="generalScoreTemplate">
            <el-form
              @submit.native.prevent
              :model="templateQuery"
              ref="queryForm"
              :inline="true"
            >
              <el-form-item label="模板名称" prop="name" label-width="100px">
                <el-input
                  v-model="templateQuery.name"
                  placeholder="请输入模板名称"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  icon="el-icon-search"
                  size="small"
                  @click="searchGeneralTemplates"
                  >查询</el-button
                >
              </el-form-item>
            </el-form>
            <el-table
              v-loading="generalTemplateLoading"
              :data="generalScoreTemplateList"
              @row-click="onTemplateSelect"
              size="small"
              border
              stripe
            >
              <el-table-column label="" width="30" align="center">
                <template slot-scope="scope">
                  <el-radio
                    class="table_radio"
                    v-model="selectedTemplateId"
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
              <el-table-column label="模板名称" prop="name" width="200" />
              <el-table-column label="维护人" align="center" prop="createBy" />
              <el-table-column
                label="创建日期"
                align="center"
                prop="createTime"
              />
              <el-table-column label="使用单位" prop="useUnitName" />
            </el-table>
            <pagination
              v-show="generalScoreTemplateListTotal > 0"
              :total="generalScoreTemplateListTotal"
              :page.sync="templateQuery.pageNumber"
              :limit.sync="templateQuery.pageSize"
              @pagination="getGeneralScoreTemplateList"
            />
          </el-tab-pane>
          <el-tab-pane label="复用模板" name="reusableScoreTemplate">
            <el-form :model="templateQuery" ref="queryForm" :inline="true">
              <el-form-item label="模板名称" prop="name" label-width="100px">
                <el-input
                  v-model="templateQuery.name"
                  placeholder="请输入模板名称"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  icon="el-icon-search"
                  size="small"
                  @click="searchReusableTemplates"
                  >查询</el-button
                >
              </el-form-item>
            </el-form>
            <el-table
              v-loading="generalTemplateLoading"
              :data="generalReuScoreTemplateList"
              @row-click="onTemplateSelect"
              size="small"
              border
              stripe
            >
              <el-table-column label="" width="30" align="center">
                <template slot-scope="scope">
                  <el-radio
                    class="table_radio"
                    v-model="selectedTemplateId"
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
              <el-table-column label="模板名称" prop="name" width="200" />
              <el-table-column label="维护人" align="center" prop="createBy" />
              <el-table-column
                label="创建日期"
                align="center"
                prop="createTime"
              />
              <el-table-column label="使用单位" prop="useUnitName" />
            </el-table>
            <pagination
              v-show="generalReuScoreTemplateListTotal > 0"
              :total="generalReuScoreTemplateListTotal"
              :page.sync="templateQuery.pageNumber"
              :limit.sync="templateQuery.pageSize"
              @pagination="getReusableScoreTemplateList"
            />
          </el-tab-pane>
        </el-tabs>

        <div slot="footer" class="dialog-footer">
          <el-button
            @click="evaluateVisable = false"
            style="width: 100px"
            size="small"
            >取 消</el-button
          >
          <el-button
            type="primary"
            @click="confirmTemplate"
            style="width: 100px"
            size="small"
            >确 定</el-button
          >
        </div>
      </el-dialog>

      <!-- 选择招标文件模板 -->
      <el-dialog
        :title="bcTemplateTitle"
        class="dialogClass"
        :visible.sync="bcTemplateVisable"
        width="60%"
      >
        <el-tabs v-model="activeTab" @tab-click="handleTabClick">
          <el-tab-pane label="通用模板" name="generalTemplate">
            <el-form
              @submit.native.prevent
              :model="bcTemplateQuery"
              ref="queryForm"
              :inline="true"
            >
              <el-form-item
                label="模板名称"
                prop="templateName"
                label-width="100px"
              >
                <el-input
                  v-model="bcTemplateQuery.templateName"
                  placeholder="请输入模板名称"
                />
              </el-form-item>
              <el-form-item
                v-if="!isScoreMOdel"
                label="合同类型："
                prop="contractType"
              >
                <el-select
                  style="width: 100%"
                  v-model="bcTemplateQuery.contractType"
                  placeholder="请选择"
                >
                  <el-option
                    v-for="dict in contractTypeList"
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
              </el-form-item>
            </el-form>
            <el-table
              v-loading="bcTemplateVisableLoading"
              :data="generalTemplateList"
              @row-click="selectBcTemplate"
              size="small"
              border
              stripe
            >
              <el-table-column label="" width="30" align="center">
                <template slot-scope="scope">
                  <el-radio
                    class="table_radio"
                    v-model="templateId"
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
                label="模板名称"
                prop="templateName"
                width="200"
              />
              <el-table-column label="维护人" align="center" prop="createBy" />
              <el-table-column
                label="创建日期"
                align="center"
                prop="createTime"
              />
              <el-table-column label="使用单位" prop="usingUnitName" />
              <el-table-column
                v-if="!isScoreMOdel"
                label="合同类型"
                prop="contractName"
              />
            </el-table>
            <pagination
              :total="generalTemplateTotal"
              :page.sync="bcTemplateQuery.pageNumber"
              :limit.sync="bcTemplateQuery.pageSize"
              @pagination="getGeneralTemplateList"
            />
          </el-tab-pane>

          <el-tab-pane label="复用模板" name="reusableTemplate">
            <el-form :model="bcTemplateQuery" ref="queryForm" :inline="true">
              <el-form-item
                label="模板名称"
                prop="templateName"
                label-width="100px"
              >
                <el-input
                  v-model="bcTemplateQuery.templateName"
                  placeholder="请输入模板名称"
                />
              </el-form-item>
              <el-form-item
                v-if="!isScoreMOdel"
                label="合同类型："
                prop="contractType"
              >
                <el-select
                  style="width: 100%"
                  v-model="bcTemplateQuery.contractType"
                  placeholder="请选择"
                >
                  <el-option
                    v-for="dict in contractTypeList"
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
                  @click="handleQueryReusable"
                  >查询</el-button
                >
              </el-form-item>
            </el-form>
            <el-table
              v-loading="bcTemplateVisableLoading"
              :data="reusableTemplateList"
              @row-click="selectBcTemplate"
              size="small"
              border
              stripe
            >
              <el-table-column label="" width="30" align="center">
                <template slot-scope="scope">
                  <el-radio
                    class="table_radio"
                    v-model="templateId"
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
                label="模板名称"
                prop="templateName"
                width="200"
              />
              <el-table-column label="维护人" align="center" prop="createBy" />
              <el-table-column
                label="创建日期"
                align="center"
                prop="createTime"
              />
              <el-table-column label="使用单位" prop="usingUnitName" />
              <el-table-column
                v-if="!isScoreMOdel"
                label="合同类型"
                prop="contractName"
              />
            </el-table>
            <pagination
              :total="reusableTemplateTotal"
              :page.sync="bcTemplateQuery.pageNumber"
              :limit.sync="bcTemplateQuery.pageSize"
              @pagination="getReusableTemplateList"
            />
          </el-tab-pane>
        </el-tabs>

        <div slot="footer" class="dialog-footer">
          <el-button
            @click="bcTemplateVisable = false"
            style="width: 100px"
            size="small"
            >取 消</el-button
          >
          <el-button
            type="primary"
            @click="confirmBcTemplate"
            style="width: 100px"
            size="small"
            >确 定</el-button
          >
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { Base64 } from "js-base64";
import {
  saveProcurementScheme,
  getListMaterials,
  getContractPlan,
  getFinanceList,
  getTemplateSwitchList,
  getProcurementSchemeCreateInfo,
  getSchemeDetail,
} from "@/api/procurement/scheme";
import { getSwitchPageList } from "@/api/procurement/manage";
import { getContractTypeList } from "@/api/template/file";
import FileModule from "@/components/FileModule/index.vue";
import { isvalidatemobile, validEmail, validatenum } from "@/utils/validate";
import BackButton from "@/components/BackButton/index.vue";
import { addAttachment , getEditFileUrlByID} from "@/api/template/file";
import {showSecretRelatedTips} from "@/utils/MyUtils";
import {offerRepo, offerService, uploadFileUrl} from "@/utils/const";

export default {
  name: "add-scheme",
  dicts: [
    "procurement_type",
    "procurement_counting_type",
    "procurement_payment_type",
  ],
  components: {
    FileModule,
    BackButton,
  },
  data() {
    const validatePhone = (rule, value, callback) => {
      if (isvalidatemobile(value)[0]) {
        callback(new Error(isvalidatemobile(value)[1]));
      } else {
        callback();
      }
    };
    const validateContactName = (rule, value, callback) => {
      const chineseNamePattern = /^[\u4e00-\u9fa5]+$/;

      if (!chineseNamePattern.test(value)) {
        callback(new Error("请输入正确的中文姓名"));
      } else {
        callback();
      }
    };

    const validateEmail = (rule, value, callback) => {
      if (!validEmail(value)) {
        callback(new Error("请输入正确的邮箱地址"));
      } else {
        callback();
      }
    };
    const validaNumber = (rule, value, callback) => {
      if (!validatenum(value, 1)) {
        callback(new Error("请输入正确的金额"));
      } else {
        callback();
      }
    };
    return {
      /*方案审批状态
        DRAFT(0,"自由态"),
        IN_APPROVAL(1,"审批中"),
        CANCELLATION(2,"已作废"),
        APPROVE(3,"已完成"),
        REJECT(4,"已驳回"),
        REVOKED(5,"已撤回"), */
      state: null,
      /* 文件上传 */
      offerService,
      offerRepo,
      uploadFileUrl,
      /* 采购方案文件，通过getSchemeDetail方法请求procurementScheme/detail?id=获取的数据 */
      procurementSchemeTempObject: null,
      fileList: [],
      fileList2: [],
      formData: {}, //form表单数据
      planList: [],
      inventoryList: [],
      contractList: [],
      rules: {
        procurementSchemeName: [
          {
            required: true,
            message: "任务名称不能为空",
          },
        ],
        procurementType: [
          {
            required: true,
            message: "请选择采购方式",
          },
        ],
        isReceiveDeposit: [
          {
            required: true,
            message: "请选择是否收取保证金",
          },
        ],
        bidDeadline: [
          {
            required: true,
            message: "请选择计划投标截止时间",
          },
        ],
        bidContactPerson: [
          {
            required: true,
            message: "联系人不能为空",
          },
          {
            validator: validateContactName,
            trigger: "blur",
          },
        ],
        bidContactPhone: [
          {
            required: true,
            message: "联系电话不能为空",
          },
          {
            validator: validatePhone,
            trigger: "blur",
          },
        ],
        bidContactEmail: [
          {
            required: true,
            message: "联系邮箱不能为空",
          },
          {
            validator: validateEmail,
            trigger: "blur",
          },
        ],
        securityDeposit: [
          {
            required: true,
            message: "请输入保证金",
          },
          {
            validator: validaNumber,
            trigger: "blur",
          },
        ],
        financeConfirmId: [
          {
            required: true,
            message: "请选择财务确认人员",
          },
        ],
        templateName: [
          {
            required: true,
            message: "请选择模板",
          },
        ],
        biddingTemplateName: [
          {
            required: true,
            message: "请选择招标文件模板",
          },
        ],
        contractTemplateName: [
          {
            required: true,
            message: "请选择合同模板",
          },
        ],
      },
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
        state: 1,
        procurementPlanCode: undefined,
        procurementPlanName: undefined,
        projectName: undefined,
        operator: undefined,
        procurementPlanType: "all",
      },
      templateQuery: {
        pageNumber: 1,
        pageSize: 10,
        state: 1,
        switchTemplateType: 1,
      },
      bcTemplateQuery: {
        pageNumber: 1,
        pageSize: 10,
        state: 1,
        templateType: "",
        templateName: "",
        switchTemplateType: "",
        contractType: "",
      },
      isShow: false,
      activeTab: "generalTemplate",
      currentTab: "generalScoreTemplate",
      generalTemplateList: [],
      reusableTemplateList: [],
      generalTemplateTotal: 0,
      reusableTemplateTotal: 0,
      procurementPlanIds: [], //计划id
      expireTimeOption: {
        // 设置日期时间显示格式，只显示年月日时分
        format: "yyyy-MM-dd HH:mm:ss",
        // 设置可选的时间范围
        // selectableRange: "00:00:00 - 23:59:59",
        selectableRange :new Date().getHours() + ':' + (new Date().getMinutes() + 1) + ':00 - 23:59:00',
        disabledDate(time) {
          console.log(new Date().getDate()+5)
          return time.getTime() < Date.now() + (4 * 24 * 3600 * 1000); // 禁用小于当前日期的日期
        }

        // disabledDate(time) {
        //   // 获取今天的时间戳
        //   const today = new Date();
        //   today.setHours(0, 0, 0, 0); // 设置为当天的零点

        //   // 明天的时间戳
        //   const tomorrow = new Date(today);
        //   tomorrow.setDate(today.getDate() + 1);

        //   // 将传入的时间戳转为日期对象
        //   const date = new Date(time);

        //   // 只能选择明天及之后的日期
        //   return date <= today || date < tomorrow;
        // },
      },
      activeTabs: "base",
      inventoryVisible: false,
      financeList: [],
      evaluateVisable: false, //评分弹出
      evaluateTemplateList: [], //评分模板列表
      evaluateTemplateLoading: false,
      templateId: "", //模板id
      templateTotal: 0,
      bcTemplateTitle: "",
      bcTemplateVisableLoading: false,
      generalTemplateLoading: false,
      bcTemplateVisable: false,
      bcTemplateList: [],
      bcTemplatetType: 0,
      bcTemplateTotal: 0,
      biddingAttachmentId: "",
      contractAttachmentId: "",
      deptId: "",
      attachmentId: "",
      viewAttachmentId: "", //预览ID
      isEdit: false,
      isSubmit: false,
      generalScoreTemplateListTotal: 0,
      generalReuScoreTemplateListTotal: 0,
      generalScoreTemplateList: [],
      generalReuScoreTemplateList: [],
      selectedTemplateId: "",
      isScoreMOdel: false,
      contractTypeList: [],
      editFileUrl:"", //编辑文档URL
    };
  },
  created() {
    const param = JSON.parse(Base64.decode(this.$route.params.params));
    this.getContractTypeList();
    console.log(param, "param--param--param");
    if (param?.type === "update") {
      this.isEdit = true;
      this.getSchemeDetail(param.id);
    } else {
      this.procurementPlanIds = param.ids;
      this.getProcurementSchemeCreateInfo();
      this.getContractPlan();
    }
    this.$set(this.formData, "procurementType", param.procurementType + "");
  },
  computed: {
    endTimeOptions() {
      //这里判断是不是今天
      let newVal = new Date(this.formData.bidDeadline)
      let    selectableRange =new Date().getHours() + ':' + (new Date().getMinutes() + 1) + ':00 - 23:59:00'
      console.log( newVal.getDate()+"---"+new Date().getDate()+5)
      if (
        newVal &&
        newVal.getDate() == new Date().getDate()+5
      ) {
       selectableRange =new Date().getHours() + ':' + (new Date().getMinutes() + 1) + ':00 - 23:59:00'
      }
      else if(newVal.getDate() > new Date().getDate()+5){
        selectableRange = '00:00:00 - 23:59:00' //默认的时间范围
      }
      return {
        selectableRange,
        disabledDate(time) {
          // 只能选大于当前截止时间的
            return time.getTime() < Date.now() + (4 * 24 * 3600 * 1000); // 禁用小于当前日期的日期

        }
      }
    },

    ...mapGetters(["project"]),
    isLease() {
      return (
        this.formData.procurementPlanType == 2 ||
        this.formData.procurementPlanType == 3
      );
    },
  },
  methods: {
    /* 计划投标截止时间监听 */
    handleChange(value) {
      let newVal = new Date(value);
      let currentDate = Date.now(); // 获取当前时间戳
      // 比较当前日期是否小于5天后的日期
      if (newVal && newVal < currentDate + 5 * 24 * 60 * 60 * 1000) {
        this.formData.bidDeadline = null; // 设置为null
      }
    },
    handleTabClick(tab) {
      // 处理标签页点击事件，根据标签页切换表格数据
      this.activeTab = tab.name;

      if (tab.name === "generalTemplate") {
        this.getGeneralTemplateList();
      } else if (tab.name === "reusableTemplate") {
        this.getReusableTemplateList();
      }
    },
    onTabClick(tab) {
      if (tab.name === "generalScoreTemplate") {
        this.getGeneralScoreTemplateList();
      } else if (tab.name === "reusableScoreTemplate") {
        this.getReusableScoreTemplateList();
      }
    },
    getContractTypeList() {
      getContractTypeList().then((res) => {
        this.contractTypeList = res.data;
      });
    },
    async getContractModelList() {
      // 获取通用模板列表数据
      this.bcTemplateVisableLoading = true;
      // API 调用获取数据
      this.bcTemplateQuery.switchTemplateType = "1";
      this.bcTemplateQuery.templateType = "1";
      const res = await getSwitchPageList(this.bcTemplateQuery);
      this.generalTemplateList = res.data.rows;
      this.bcTemplateVisableLoading = false;
    },

    async getGeneralTemplateList() {
      // 获取通用模板列表数据
      this.bcTemplateVisableLoading = true;
      // API 调用获取数据
      this.bcTemplateQuery.switchTemplateType = "1";
      const res = await getSwitchPageList(this.bcTemplateQuery);
      this.generalTemplateList = res.data.rows;
      this.bcTemplateVisableLoading = false;
    },
    async getReusableTemplateList() {
      // 获取复用模板列表数据
      this.bcTemplateVisableLoading = true;
      // API 调用获取数据
      this.bcTemplateQuery.switchTemplateType = "2";
      const res = await getSwitchPageList(this.bcTemplateQuery);
      this.reusableTemplateList = res.data.rows;
      this.bcTemplateVisableLoading = false;
    },
    /* 评分模板标签页 通用 */
    async getGeneralScoreTemplateList() {
      // 获取通用模板列表数据
      this.generalTemplateLoading = true;
      // API 调用获取数据
      this.templateQuery.switchTemplateType = "1";
      const res = await getTemplateSwitchList(this.templateQuery);
      this.generalScoreTemplateList = res.data.rows;
      this.generalScoreTemplateListTotal = res.data.total;
      this.generalTemplateLoading = false;
    },
    /* 评分模板标签页 复用 */
    async getReusableScoreTemplateList() {
      // 获取通用模板列表数据
      this.generalTemplateLoading = true;
      // API 调用获取数据
      this.templateQuery.switchTemplateType = "2";
      const res = await getTemplateSwitchList(this.templateQuery);
      this.generalReuScoreTemplateList = res.data.rows;
      this.generalReuScoreTemplateListTotal = res.data.total;
      this.generalTemplateLoading = false;
    },
    //提交
    submitForm(formName) {
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid, object) => {
        console.log(valid, "valid--valid--valid");
        console.log(object, "object--object--object");
        if (valid) {
          const loading = this.$loading({
            lock: true,
            text: "数据提交中...",
            background: "rgba(0, 0, 0, 0.7)",
          });
          console.log(
            this.formData,
            "formData-formData-formData-formData-formData-formData"
          );
          const {
            procurementSchemeName,
            procurementType,
            isReceiveDeposit,
            securityDeposit,
            bidDeadline,
            bidContactPerson,
            bidContactPhone,
            bidContactEmail,
            financeConfirmId,
            financeConfirmName,
            evaluationTemplateId,
            biddingAttachmentId,
            contractAttachmentId,
            contractTemplateId,
            biddingTemplateId,
            procurementSchemeId,
            procurementSchemeBiddingId,
          } = this.formData;
          const formData = {
            procurementScheme: {
              procurementSchemeName,
              procurementType,
              isReceiveDeposit,
              securityDeposit,
              financeConfirmId,
              financeConfirmName,
              id: procurementSchemeId || "",
            },
            procurementSchemeBidding: {
              bidDeadline,
              bidContactPerson,
              bidContactPhone,
              bidContactEmail,
              evaluationTemplateId,
              biddingAttachmentId,
              contractAttachmentId,
              contractTemplateId,
              biddingTemplateId,
              id: procurementSchemeBiddingId || "",
            },
            contractSplitIds: this.procurementPlanIds,
          };
          console.log(formData, "formData--formData--formData");
          try {
            const res = await saveProcurementScheme(formData);
            console.log(res, "成功");
            loading.close();
            this.$message({
              message: "保存成功",
              type: "success",
            });
            this.isSubmit = false;
            this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              let param = Base64.encode(JSON.stringify(res.data));
              this.$router.replace(`/procurement/scheme-detail/${param}`);
            });
          } catch (err) {
            this.isSubmit = false;
            loading.close();
            console.log(err);
          }
        } else {
          let msgName = [
            "bidDeadline",
            "bidContactPerson",
            "bidContactPhone",
            "bidContactEmail",
            "templateName",
            "biddingTemplateName",
            "contractTemplateName",
          ];
          const [[firstKey]] = Object.entries(object);
          const messageName = object[firstKey][0].field;
          this.isSubmit = false;
          if (msgName.includes(messageName)) {
            this.$message.error("招标文件内容未填写完成");
          }
          return false;
        }
      });
    },
    /** 查询任务列表 */
    async getContractPlan() {
      this.loading = false;
      try {
        const res = await getContractPlan(this.procurementPlanIds);
        if (res.data) {
          this.contractList = res.data;
        }
      } catch (err) {
        console.log(err);
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.bcTemplateQuery.pageNum = 1;
      this.getGeneralTemplateList();
    },
    handleQueryReusable() {
      this.bcTemplateQuery.pageNum = 1;
      this.getReusableTemplateList();
    },
    searchGeneralTemplates() {
      this.templateQuery.pageNum = 1;
      this.getGeneralScoreTemplateList();
    },
    searchReusableTemplates() {
      this.templateQuery.pageNum = 1;
      this.getReusableScoreTemplateList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    marginInput(val) {
      if (val === "1") {
        this.isShow = true;
      } else if (val === "2") {
        this.isShow = false;
      }
    },
    handleTypeClick(tab) {
      this.activeTabs = tab.name;
    },
    async handelInventory(row) {
      this.inventoryVisible = true;
      const formData = {
        planId: row.planId,
        contractSpiltIdList: this.procurementPlanIds,
      };
      try {
        const res = await getListMaterials(formData);
        this.inventoryList = res.data;
        console.log(res, "清单");
      } catch (err) {
        console.log(err);
      }
    },
    async getFinanceList() {
      try {
        const res = await getFinanceList(this.deptId);
        this.financeList = res.data;
      } catch (err) {
        console.log(err);
      }
    },
    changeFinance(val) {
      this.formData.financeConfirmName = this.financeList.find(
        (item) => item.userId === val
      ).nickName;
    },
    bidSuccess(res) {
      const { url, name } = res.data;
      console.log(url, name, "a");
      this.fileList = [{ name, url }];
      this.formData.biddingTemplate = { fileUrl: url, fileName: name };
      this.$refs.form.clearValidate("biddingTemplate");
    },
    contractSuccess(res, file, fileList) {
      const { url, name } = res.data;
      //this.fileList2 = [{ name, url }];
      console.log(url, name, "a");
      this.formData.contractTemplate = { fileUrl: url, fileName: name };
      this.$refs.form.clearValidate("contractTemplate");
    },
    //选择评分模板
    async getTemplateList() {
      showSecretRelatedTips(()=>{
        this.evaluateVisable = true;
        (this.currentTab = "generalScoreTemplate"),
          this.getGeneralScoreTemplateList();
        try {
          // const res = await getTemplateSwitchList(this.templateQuery);
          // this.evaluateTemplateList = res.data.rows;
          // this.templateTotal = res.data.total;
          // console.log(res, "评分模板");
        } catch (err) {
          console.log(err);
        }
      })
    },
    //获取经办人和上限价
    async getProcurementSchemeCreateInfo() {
      try {
        const res = await getProcurementSchemeCreateInfo(
          this.procurementPlanIds
        );
        const {
          ceilingPrice,
          procurementOfficerName,
          countingTypeText,
          paymentTypeText,
          priceType,
          subjectMatterName,
          subjectMatterType,
          priceTypeText,
          procurementPlanType,
          procurementSchemeName,
        } = res.data;
        this.deptId = res.data.projectDeptId;
        this.$set(this.formData, "ceilingPrice", ceilingPrice);
        this.$set(
          this.formData,
          "procurementOfficerName",
          procurementOfficerName
        );
        this.formData.countingTypeText = countingTypeText;
        this.$set(
          this.formData,
          "procurementSchemeName",
          procurementSchemeName
        );
        this.formData.procurementPlanType = procurementPlanType;
        this.formData.paymentTypeText = paymentTypeText;
        this.formData.subjectMatterName = subjectMatterName;
        this.formData.subjectMatterType = subjectMatterType;
        this.formData.priceType = priceType;
        this.formData.priceTypeText = priceTypeText;
        this.getFinanceList();
      } catch (err) {
        console.log(err);
      }
    },
    //选择模板
    /** 选择专家 */
    selectTemplate(row) {
      this.templateId = row.id;
    },
    //确认模板
    confirmTemplate() {
      const templateId = this.selectedTemplateId;
      if (!templateId) this.$message.error("请先选择一个评分模板");
      this.evaluateTemplateList = [
        ...this.generalScoreTemplateList,
        ...this.generalReuScoreTemplateList,
      ];
      const name = this.evaluateTemplateList.find(
        (item) => item.id === templateId
      ).name;
      this.$set(this.formData, "templateName", name);
      this.$set(this.formData, "evaluationTemplateId", templateId);
      this.$refs.form.clearValidate("templateName");
      this.evaluateVisable = false;
    },
    /* 修改不同的模板 2 招标文件模板 ，1 合同模板 联想文档绑定文件id对象名称viewAttachmentId  */
    async modifyTempFile(type){
      console.log('%c modifyTempFile(2 招标文件模板 ，1 合同模板)', `font-size: 20px;background-color: #f00;`, type);
      console.log('%c procurementSchemeTempObject', `font-size: 20px;background-color: #f00;`, this.procurementSchemeTempObject);
      console.log('%c formData', `font-size: 20px;background-color: #f00;`, this.formData);
      /* 2 招标模板 */
      if(type === 2){
        let {attachmentId , fileName , fileUrl , templateName , templateId} = this.procurementSchemeTempObject.biddingTemplate;
        try {
          let res = {};
          if(this.formData.biddingAttachmentId === this.formData.biddingTemplateId){
            /* 生成新的附件 */
            res = await addAttachment({
              fileName: fileName,
              fileUrl: fileUrl,
            });
          }else{
            res.data = this.formData.biddingAttachmentId;
          }
          /* 设置新的附件返回的附件id */
          this.$set(this.formData, "biddingAttachmentId", res.data);
          /* 同步更新页面的模板附件对象(附件修改按钮) */
          if (!this.procurementSchemeTempObject.biddingTemplate) {
            this.$set(this.procurementSchemeTempObject, 'biddingTemplate', {});
          }
          this.$set(this.procurementSchemeTempObject.biddingTemplate, "attachmentId", res.data);
          this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileName", fileName);
          this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileUrl", fileUrl);
          this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateName", templateName);
          this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateId", templateId);
          /* 调起联想文档 */
          this.viewAttachmentId = res.data;

          //据viewAttachmentId获取文件的文档中台的编辑URL
          if (this.viewAttachmentId) {
            console.log('Attachment ID:', this.viewAttachmentId);
            //获取文档中台的文档编辑URL
            try {
              const res = await getEditFileUrlByID({attachmentId: this.viewAttachmentId});
              this.editFileUrl = res.data;
              console.log("editFileUrl:", this.editFileUrl);
            } catch (err) {
              console.log(err);
            }
          } else {
            console.warn('attachmentId 数据未正确加载');
          }

        } catch (err) {
          console.log(err);
        }
      }else if(type === 1){
        /* 1 合同模板 */
        let {attachmentId , fileName , fileUrl , templateName , templateId} = this.procurementSchemeTempObject.contractTemplate;
        try {
          let res = {};
          if(this.formData.contractAttachmentId === this.formData.contractTemplateId){
            /* 生成新的附件 */
            res = await addAttachment({
              fileName: fileName,
              fileUrl: fileUrl,
            });
          }else{
            res.data = this.formData.contractAttachmentId;
          }
          /* 设置新的附件返回的附件id */
          this.$set(this.formData, "contractAttachmentId", res.data);
          /* 同步更新页面的模板附件对象(附件修改按钮) */
          if (!this.procurementSchemeTempObject.contractTemplate) {
            this.$set(this.procurementSchemeTempObject, 'contractTemplate', {});
          }
          this.$set(this.procurementSchemeTempObject.contractTemplate, "attachmentId", res.data);
          this.$set(this.procurementSchemeTempObject.contractTemplate, "fileName", fileName);
          this.$set(this.procurementSchemeTempObject.contractTemplate, "fileUrl", fileUrl);
          this.$set(this.procurementSchemeTempObject.contractTemplate, "templateName", templateName);
          this.$set(this.procurementSchemeTempObject.contractTemplate, "templateId", templateId);
          /* 调起联想文档 */
          this.viewAttachmentId = res.data;

          //据viewAttachmentId获取文件的文档中台的编辑URL
          if (this.viewAttachmentId) {
            console.log('Attachment ID:', this.viewAttachmentId);
            //获取文档中台的文档编辑URL
            try {
              const res = await getEditFileUrlByID({attachmentId: this.viewAttachmentId});
              this.editFileUrl = res.data;
              console.log("editFileUrl:", this.editFileUrl);
            } catch (err) {
              console.log(err);
            }
          } else {
            console.warn('attachmentId 数据未正确加载');
          }
        } catch (err) {
          console.log(err);
        }
      }
    },
    /* 点击显示选择模板列表 2 招标文件模板 ，1 合同模板 */
    async getBcTemplateList(type) {
      showSecretRelatedTips(async ()=>{
        this.bcTemplateQuery.pageNumber = 1;
        this.bcTemplateQuery.pageSize = 10;
        this.bcTemplateTitle = type === 2 ? "选择招标文件模板" : "选择合同模板";
        this.isScoreMOdel = type === 2 ? true : false;
        this.activeTab = "generalTemplate";
        this.bcTemplateVisable = true;
        this.bcTemplatetType = type;
        this.bcTemplateQuery.templateType = type;
        this.bcTemplateQuery.contractType = "";
        // 根据模板类型调用相应的方法
        if (type === 2) {
          this.getGeneralTemplateList();
        } else {
          this.getContractModelList();
        }
        // 设置 switchTemplateType 和 templateType
        this.bcTemplateQuery.switchTemplateType = "1";
        this.bcTemplateQuery.templateType = type === 2 ? "2" : "1";

        // 获取模板列表
        const res = await getSwitchPageList(this.bcTemplateQuery);
        this.bcTemplateList = res.data.rows;
        /* 最后再获取分页数据，区分了通用和复用模板。 */
        await this.activeTabListen(this.activeTab);
      })

    },

    async confirmBcTemplate() {
      /* this.templateId是模板列表弹窗单选的双向绑定，意思就是模板文件id */
      const templateId = this.templateId;
      if (!templateId) {
        this.$message.error("请先选择一个模板");
        return;
      }
      try {
        const {templateName, fileUrl, fileName} = this.bcTemplateList.find(
          (item) => item.id === templateId
        )
        /* 创建新文件 */
        const res = await addAttachment({
          fileName: fileName,
          fileUrl: fileUrl,
        });

          /* 2 招标文件模板 ，1 合同模板 */
          if (this.bcTemplatetType === 2) {
            this.$refs.uploadBidding.clearFiles();
            this.$set(this.formData, "biddingAttachmentId", res.data);
            this.$set(this.formData, "biddingTemplateName", templateName);
            this.$set(this.formData, "biddingTemplateId", templateId);
            this.$refs.form.clearValidate("biddingTemplateName");
            /* 同步更新页面的模板附件对象(附件修改按钮) */
            if (!this.procurementSchemeTempObject.biddingTemplate) {
              this.$set(this.procurementSchemeTempObject, 'biddingTemplate', {});
            }
            this.$set(this.procurementSchemeTempObject.biddingTemplate, "attachmentId", res.data);
            this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileName", fileName);
            this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileUrl", fileUrl);
            this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateName", templateName);
            this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateId", templateId);
            this.viewAttachmentId = res.data;

            //据viewAttachmentId获取文件的文档中台的编辑URL
            if (this.viewAttachmentId) {
              console.log('Attachment ID:', this.viewAttachmentId);
              //获取文档中台的文档编辑URL
              try {
                const res = await getEditFileUrlByID({attachmentId: this.viewAttachmentId});
                this.editFileUrl = res.data;
                console.log("editFileUrl:", this.editFileUrl);
              } catch (err) {
                console.log(err);
              }
            } else {
              console.warn('attachmentId 数据未正确加载');
            }
          } else {
            this.$refs.uploadContract.clearFiles();
            this.$set(this.formData, "contractAttachmentId", res.data);
            this.$set(this.formData, "contractTemplateName", templateName);
            this.$set(this.formData, "contractTemplateId", templateId);
            this.$refs.form.clearValidate("contractTemplateName");
            /* 同步更新页面的模板附件对象(附件修改按钮) */
            if (!this.procurementSchemeTempObject.contractTemplate) {
              this.$set(this.procurementSchemeTempObject, 'contractTemplate', {});
            }
            this.$set(this.procurementSchemeTempObject.contractTemplate, "attachmentId", res.data);
            this.$set(this.procurementSchemeTempObject.contractTemplate, "fileName", fileName);
            this.$set(this.procurementSchemeTempObject.contractTemplate, "fileUrl", fileUrl);
            this.$set(this.procurementSchemeTempObject.contractTemplate, "templateName", templateName);
            this.$set(this.procurementSchemeTempObject.contractTemplate, "templateId", templateId);
            this.viewAttachmentId = res.data;

            //据viewAttachmentId获取文件的文档中台的编辑URL
            if (this.viewAttachmentId) {
              console.log('Attachment ID:', this.viewAttachmentId);
              //获取文档中台的文档编辑URL
              try {
                const res = await getEditFileUrlByID({attachmentId: this.viewAttachmentId});
                this.editFileUrl = res.data;
                console.log("editFileUrl:", this.editFileUrl);
              } catch (err) {
                console.log(err);
              }
            } else {
              console.warn('attachmentId 数据未正确加载');
            }

          }
      } catch (err) {
        console.log(err);
        // 加载文件框
        let fileName = this.bcTemplateList.find(
          (item) => item.id === templateId
        ).fileName;
        const fileUrl = this.bcTemplateList.find(
          (item) => item.id === templateId
        ).fileUrl;
        try {
          const res = await addAttachment({
            fileName: fileName,
            fileUrl: fileUrl,
          });
          this.$set(this.formData, "biddingAttachmentId", res.data);
          this.viewAttachmentId = res.data;
          console.log("viewAttachmentId:", this.viewAttachmentId);
        } catch (err) {
          console.log(err);
        }

        //据viewAttachmentId获取文件的文档中台的编辑URL
        if (this.viewAttachmentId) {
          console.log('Attachment ID:', this.viewAttachmentId);
          //获取文档中台的文档编辑URL
          try {
            const res = await getEditFileUrlByID({attachmentId: this.viewAttachmentId});
            this.editFileUrl = res.data;
            console.log("editFileUrl:", this.editFileUrl);
          } catch (err) {
            console.log(err);
          }
        } else {
          console.warn('attachmentId 数据未正确加载');
        }
      }
      this.bcTemplateVisable = false;
    },

    selectBcTemplate(row) {
      this.templateId = row.templateId;
      this.attachmentId = row.attachmentId;
      console.log(row, "rrr");
    },
    onTemplateSelect(row) {
      this.selectedTemplateId = row.id;
      //  this.attachmentId = row.attachmentId;
    },


    /* 手动合同模板附件上传 */
    uploadBiddingClick() {
      showSecretRelatedTips(()=>{
        this.$refs['uploadBidding'].$refs['upload-inner'].handleClick()
      })
    },
    /* 手动合同模板附件上传 */
    uploadContractClick() {
      showSecretRelatedTips(()=>{
        this.$refs['uploadContract'].$refs['upload-inner'].handleClick()
      })
    },
    /* 招标文件手动上传成功 */
    async fileSuccessBidding(res) {
      const { url, name } = res.data;
      try {
        /* 保存到文件表获取返回id */
        const res = await addAttachment({ fileName: name, fileUrl: url });
        /* 设置新的附件返回的附件id */
        this.$set(this.formData, "biddingAttachmentId", res.data);
        /* 同步更新页面的模板附件对象(附件修改按钮) */
        if (!this.procurementSchemeTempObject.biddingTemplate) {
          this.$set(this.procurementSchemeTempObject, 'biddingTemplate', {});
        }
        this.$set(this.procurementSchemeTempObject.biddingTemplate, "attachmentId", res.data);
        this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileName", name);
        this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileUrl", url);
        this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateName", this.formData.biddingTemplateName);
        this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateId", this.formData.biddingTemplateId);
        /* 调起联想文档 */
        this.viewAttachmentId = res.data;

        //据viewAttachmentId获取文件的文档中台的编辑URL
        if (this.viewAttachmentId) {
          console.log('Attachment ID:', this.viewAttachmentId);
          //获取文档中台的文档编辑URL
          try {
            const res = await getEditFileUrlByID({attachmentId: this.viewAttachmentId});
            this.editFileUrl = res.data;
            console.log("editFileUrl:", this.editFileUrl);
          } catch (err) {
            console.log(err);
          }
        } else {
          console.warn('attachmentId 数据未正确加载');
        }
      } catch (err) {
        console.log(err);
      }
    },
    /* 招标文件手动上传文件删除 */
    fileRemoveBidding() {
      this.$set(this.formData, "biddingAttachmentId", null);
      this.$set(this.formData, "biddingTemplateName", null);
      this.$set(this.formData, "biddingTemplateId", null);
      this.$set(this.procurementSchemeTempObject.biddingTemplate, "attachmentId", null);
      this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileName", null);
      this.$set(this.procurementSchemeTempObject.biddingTemplate, "fileUrl", null);
      this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateName", null);
      this.$set(this.procurementSchemeTempObject.biddingTemplate, "templateId", null);
      this.$forceUpdate();
      /* 调起联想文档 */
      this.viewAttachmentId = null;
      this.editFileUrl = ""; //删除文档后，文档中台的文档编辑URL设为空
    },
    /* 合同模板文件手动上传成功 */
    async fileSuccessContract(res) {
      const { url, name } = res.data;
      try {
        /* 保存到文件表获取返回id */
        const res = await addAttachment({ fileName: name, fileUrl: url });
        /* 设置新的附件返回的附件id */
        this.$set(this.formData, "contractAttachmentId", res.data);
        /* 同步更新页面的模板附件对象(附件修改按钮) */
        if (!this.procurementSchemeTempObject.contractTemplate) {
          this.$set(this.procurementSchemeTempObject, 'contractTemplate', {});
        }
        this.$set(this.procurementSchemeTempObject.contractTemplate, "attachmentId", res.data);
        this.$set(this.procurementSchemeTempObject.contractTemplate, "fileName", name);
        this.$set(this.procurementSchemeTempObject.contractTemplate, "fileUrl", url);
        this.$set(this.procurementSchemeTempObject.contractTemplate, "templateName", this.formData.contractTemplateName);
        this.$set(this.procurementSchemeTempObject.contractTemplate, "templateId", this.formData.contractTemplateId);
        /* 调起联想文档 */
        this.viewAttachmentId = res.data;

        //据viewAttachmentId获取文件的文档中台的编辑URL
        if (this.viewAttachmentId) {
          console.log('Attachment ID:', this.viewAttachmentId);
          //获取文档中台的文档编辑URL
          try {
            const res = await getEditFileUrlByID({attachmentId: this.viewAttachmentId});
            this.editFileUrl = res.data;
            console.log("editFileUrl:", this.editFileUrl);
          } catch (err) {
            console.log(err);
          }
        } else {
          console.warn('attachmentId 数据未正确加载');
        }
      } catch (err) {
        console.log(err);
      }
    },
    /* 合同模板手动上传文件删除 */
    fileRemoveContract() {
      this.$set(this.formData, "contractAttachmentId", null);
      this.$set(this.formData, "contractTemplateName", null);
      this.$set(this.formData, "contractTemplateId", null);
      this.$set(this.procurementSchemeTempObject.contractTemplate, "attachmentId", null);
      this.$set(this.procurementSchemeTempObject.contractTemplate, "fileName", null);
      this.$set(this.procurementSchemeTempObject.contractTemplate, "fileUrl", null);
      this.$set(this.procurementSchemeTempObject.contractTemplate, "templateName", null);
      this.$set(this.procurementSchemeTempObject.contractTemplate, "templateId", null);
      this.$forceUpdate();
      /* 调起联想文档 */
      this.viewAttachmentId = null;
      this.editFileUrl="";
    },

    handleKeydown(event) {
      if (event.key === "Enter") {
        this.handleQuery();
      }
    },
    //获取详情
    async getSchemeDetail(id) {
      try {
        const res = await getSchemeDetail(id);
        this.skeletonLoading = false;
        console.log(res, "详情");
        const {
          procurementScheme,
          procurementSchemeBidding,
          contractPlanList,
          approveNodeInfos,
          approveLists,
          contractSplitIdList,
        } = res.data;
        /* 采购方案文件，通过getSchemeDetail方法请求procurementScheme/detail?id=获取的数据 */
        this.procurementSchemeTempObject = procurementSchemeBidding;
        this.contractList = contractPlanList;
        const {
          procurementSchemeName,
          procurementSchemeCode,
          ceilingPrice,
          isReceiveDeposit,
          securityDeposit,
          financeConfirmId,
          financeConfirmName,
          id: procurementSchemeId,
          procurementOfficerName,
          procurementOfficer,
          countingTypeText,
          procurementPlanType,
          paymentTypeText,
          subjectMatterName,
          subjectMatterType,
          priceType,
          priceTypeText,
          projectDeptId,
          state,
        } = procurementScheme;
        /* 本方案审批状态 */
        this.state = state;
        const {
          bidDeadline,
          bidContactPerson,
          bidContactPhone,
          bidContactEmail,
          biddingTemplate,
          contractTemplate,
          evaluationTemplate,
          id: procurementSchemeBiddingId,
        } = procurementSchemeBidding;
        this.deptId = projectDeptId;
        this.getFinanceList();
        this.$set(
          this.formData,
          "procurementSchemeName",
          procurementSchemeName
        );
        this.$set(this.formData, "isReceiveDeposit", isReceiveDeposit + "");
        this.marginInput(this.formData.isReceiveDeposit);
        this.$set(this.formData, "securityDeposit", securityDeposit);
        this.$set(this.formData, "bidDeadline", bidDeadline);
        this.$set(this.formData, "bidContactPerson", bidContactPerson);
        this.$set(this.formData, "bidContactPhone", bidContactPhone);
        this.$set(this.formData, "bidContactEmail", bidContactEmail);
        this.$set(this.formData, "financeConfirmId", financeConfirmId);
        this.$set(this.formData, "financeConfirmName", financeConfirmName);

        this.formData.fileListBidding = [{
          name: procurementSchemeBidding.biddingTemplate.fileName,  // 文件名
          url: procurementSchemeBidding.biddingTemplate.fileUrl,  // 文件的 URL（如果是已上传的文件）
          status: 'success',  // 上传状态，可以是 'success' | 'failure' | 'uploading'
          uid: Date.now()  // 文件的唯一标识符
        }];
        this.formData.fileListContract = [{
          name: procurementSchemeBidding.contractTemplate.fileName,  // 文件名
          url: procurementSchemeBidding.contractTemplate.fileUrl,  // 文件的 URL（如果是已上传的文件）
          status: 'success',  // 上传状态，可以是 'success' | 'failure' | 'uploading'
          uid: Date.now()  // 文件的唯一标识符
        }];

        this.formData.countingTypeText = countingTypeText;
        this.formData.procurementPlanType = procurementPlanType;
        this.formData.paymentTypeText = paymentTypeText;
        this.formData.subjectMatterName = subjectMatterName;
        this.formData.subjectMatterType = subjectMatterType;
        this.formData.priceType = priceType;
        this.formData.priceTypeText = priceTypeText;

        this.formData.evaluationTemplateId = evaluationTemplate.templateId;
        this.formData.templateName = evaluationTemplate.templateName;
        this.formData.biddingAttachmentId = !biddingTemplate?null:biddingTemplate.attachmentId;
        this.formData.biddingTemplateName = !biddingTemplate?null:biddingTemplate.templateName;
        this.formData.biddingTemplateId = !biddingTemplate?null:biddingTemplate.templateId;
        this.formData.contractAttachmentId = !contractTemplate?null:contractTemplate.attachmentId;
        this.formData.contractTemplateId = !contractTemplate?null:contractTemplate.templateId;
        this.formData.contractTemplateName = !contractTemplate?null:contractTemplate.templateName;
        this.formData.procurementSchemeId = procurementSchemeId;
        this.formData.procurementSchemeBiddingId = procurementSchemeBiddingId;
        this.formData.procurementSchemeCode = procurementSchemeCode;
        this.formData.ceilingPrice = ceilingPrice;
        this.formData.procurementOfficerName = procurementOfficerName;
        this.formData.procurementOfficer = procurementOfficer;
        this.procurementPlanIds = contractSplitIdList;
        console.log('%c👽 this.procurementPlanIds', `font-size: 20px;background-color: #f00;`, this.procurementPlanIds);

        console.log(this.formData, "this.formData-this.formData~");
      } catch (err) {
        console.log(err);
      }
    },
    async activeTabListen(newTab) {
      if (newTab === "generalTemplate") {
        this.bcTemplateQuery.switchTemplateType = "1";
      } else {
        this.bcTemplateQuery.switchTemplateType = "2";
      }
      const res = await getSwitchPageList(this.bcTemplateQuery);
      if (newTab === "generalTemplate") {
        this.bcTemplateList = res.data.rows;
        this.generalTemplateTotal = res.data.total;
      } else {
        this.bcTemplateList = res.data.rows;
        this.reusableTemplateTotal = res.data.total;
      }
    },
  },
  watch: {
    project: {
      handler(newVal, oldVal) {
        if (oldVal === undefined || newVal.id !== oldVal.id) {
          this.$router.replace("/procurement/scheme");
        }
      },
    },
    /* 招标文件合同模板弹窗显示隐藏监听 */
    bcTemplateVisable(val) {
      /* 如果弹窗隐藏，重置分页数据 */
      if(!val){
        this.bcTemplateQuery.pageNumber = 1;
        this.bcTemplateQuery.pageSize = 10;
        this.reusableTemplateTotal = 0;
        this.generalTemplateTotal = 0;
      }
    },
    async activeTab(newTab) {
      if (newTab === "generalTemplate") {
        this.bcTemplateQuery.switchTemplateType = "1";
      } else {
        this.bcTemplateQuery.switchTemplateType = "2";
      }
      const res = await getSwitchPageList(this.bcTemplateQuery);
      if (newTab === "generalTemplate") {
        this.bcTemplateList = res.data.rows;
        this.generalTemplateTotal = res.data.total;
      } else {
        this.bcTemplateList = res.data.rows;
        this.reusableTemplateTotal = res.data.total;
      }
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

  &::before {
    content: "";
    height: 20px;
    width: 5px;
    background-color: rgba(41, 65, 137, 1);
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}

.plan-title {
  width: 100%;
  font-size: 16px;
  line-height: 48px;
}

::v-deep.app-container .dialogClass .el-dialog__body {
  height: initial;
}
.previewFile {
  width: 100%;
  height: 500px;
}
</style>
